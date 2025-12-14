package io.kitsuri.m1rage.patcher

import android.content.Context
import android.net.Uri
import android.util.Log
import io.kitsuri.m1rage.model.PatcherViewModel
import io.kitsuri.m1rage.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

object Patcher {

    private var viewModel: PatcherViewModel? = null

    /**
     * Set the ViewModel to enable logging to UI
     */
    fun setViewModel(vm: PatcherViewModel) {
        viewModel = vm
        // Set for all Java utilities
        SmaliUtils.setViewModel(vm)
        DexToSmali.setViewModel(vm)
        APKSigner.setViewModel(vm)
        ZipAlign.setViewModel(vm)
    }

    private fun addLog(level: Int, message: String) {
        viewModel?.addLog(level, message)
    }

    /**
     * Main entry point to modify an APK:
     * - Copies the input APK to a working directory
     * - Extracts the APK contents
     * - Decompiles all DEX files to Smali code
     * - Parses the manifest to find the launcher activity
     * - Locates the corresponding Smali file
     * - Injects System.loadLibrary("hxo") into either the constructor or onCreate method
     * - Copies native .so libraries from assets into the lib folder
     * Returns the extracted directory (with modifications) on success, or null on failure.
     */
    suspend fun patchApk(
        context: Context,
        apkUri: Uri,
        injectionMode: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val workDir = File(context.getExternalFilesDir(null), "apk_workspace/$timestamp")
            workDir.mkdirs()

            addLog(Log.INFO, "Creating workspace directory")
            val apkFile = File(workDir, "input.apk")
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                FileOutputStream(apkFile).use { output -> input.copyTo(output) }
            }

            addLog(Log.INFO, "Extracting APK contents...")
            val extractDir = File(workDir, "extracted")
            extractDir.mkdirs()
            APKInstallUtils.unzip(apkFile.absolutePath, extractDir.absolutePath)

            val manifestFile = File(extractDir, "AndroidManifest.xml")
            if (!manifestFile.exists()) {
                addLog(Log.ERROR, "AndroidManifest.xml not found")
                return@withContext null
            }

            val launcherActivity = ManifestParser.findLauncherActivity(manifestFile)
            if (launcherActivity == null) {
                addLog(Log.ERROR, "Failed to find launcher activity")
                return@withContext null
            }

            addLog(Log.INFO, "Launcher activity: $launcherActivity")

            val dexFiles = extractDir.listFiles()
                ?.filter { it.name.endsWith(".dex") }
                ?.sortedBy { it.name }

            addLog(Log.INFO, "Found ${dexFiles?.size ?: 0} DEX files")

            dexFiles?.forEachIndexed { index, dexFile ->
                val smaliDirName = when (dexFile.name) {
                    "classes.dex" -> "smali"
                    else -> "smali_${dexFile.nameWithoutExtension}"
                }
                val outputSmali = File(extractDir, smaliDirName)
                outputSmali.mkdirs()

                try {
                    addLog(Log.INFO, "Decompiling ${dexFile.name}...")
                    DexToSmali(true, dexFile, outputSmali, 30, dexFile.name).execute()
                } catch (e: Exception) {
                    addLog(Log.WARN, "Failed to decompile ${dexFile.name}: ${e.message}")
                }
            }

            addLog(Log.INFO, "Locating launcher activity smali file...")
            val smaliFile = locateSmali(extractDir, launcherActivity)
            if (smaliFile == null) {
                addLog(Log.ERROR, "Could not find smali file for $launcherActivity")
                return@withContext null
            }

            addLog(Log.INFO, "Found: ${smaliFile.name}")

            val injected = if (injectionMode == "constructor") {
                addLog(Log.INFO, "Injecting into constructor...")
                injectConstructor(smaliFile)
            } else {
                addLog(Log.INFO, "Injecting into onCreate...")
                injectOnCreate(smaliFile)
            }

            if (!injected) {
                addLog(Log.ERROR, "Failed to inject library loader")
                return@withContext null
            }

            addLog(Log.INFO, "Library loader injected successfully")

            addLog(Log.INFO, "Copying native libraries...")
            addNativeLibs(context, extractDir)

            addLog(Log.INFO, "Patch preparation complete")
            extractDir // Return modified extracted directory
        } catch (e: Exception) {
            addLog(Log.ERROR, "Patch failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Recompiles the modified extracted directory back into a signed APK:
     * - Copies all non-Smali files/folders to a build directory
     * - Compiles each Smali directory back to its corresponding DEX file
     * - Removes old META-INF signatures
     * - Zips everything into an unsigned APK (using STORE compression for resources/libs)
     * - Aligns the APK (zipalign)
     * - Signs the APK
     * Returns the final signed APK (falls back to aligned unsigned if signing fails).
     */
    suspend fun rebuildApk(context: Context, extractDir: File): File? = withContext(Dispatchers.IO) {
        try {
            val workDir = extractDir.parentFile!!
            val outputDir = File(workDir.parentFile, "output").apply { mkdirs() }
            val buildDir = File(workDir.parentFile, "build_temp").apply {
                deleteRecursively()
                mkdirs()
            }

            addLog(Log.INFO, "Preparing build directory...")

            // Copy everything except Smali directories
            extractDir.listFiles()?.forEach { file ->
                if (!file.name.startsWith("smali")) {
                    if (file.isDirectory) {
                        file.copyRecursively(File(buildDir, file.name), overwrite = true)
                    } else {
                        file.copyTo(File(buildDir, file.name), overwrite = true)
                    }
                }
            }

            // Recompile Smali to DEX
            val smaliDirs = extractDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("smali") }
            addLog(Log.INFO, "Recompiling ${smaliDirs?.size ?: 0} smali directories...")

            smaliDirs?.forEach { file ->
                val dexName = if (file.name == "smali") "classes.dex"
                else file.name.replace("smali_", "") + ".dex"

                addLog(Log.INFO, "Compiling ${file.name} to $dexName...")
                val dexFile = File(buildDir, dexName)
                SmaliUtils.smaliToDex(file, dexFile, 30)
            }

            // Remove old signatures
            addLog(Log.INFO, "Removing old signatures...")
            File(buildDir, "META-INF").deleteRecursively()

            // Create unsigned APK
            addLog(Log.INFO, "Creating unsigned APK...")
            val unsignedApk = File(outputDir, "unsigned.apk").apply { delete() }
            ZipFile(unsignedApk).use { zipFile ->
                val params = ZipParameters()
                buildDir.listFiles()?.forEach { file ->
                    when {
                        file.isDirectory && noCompressFolder(file.name) -> {
                            params.compressionMethod = CompressionMethod.STORE
                            zipFile.addFolder(file, params)
                            params.compressionMethod = CompressionMethod.DEFLATE
                        }
                        file.isDirectory -> zipFile.addFolder(file)
                        noCompressFile(file.name) -> {
                            params.compressionMethod = CompressionMethod.STORE
                            zipFile.addFile(file, params)
                            params.compressionMethod = CompressionMethod.DEFLATE
                        }
                        else -> zipFile.addFile(file)
                    }
                }
            }

            // Align APK
            addLog(Log.INFO, "Aligning APK...")
            val alignedApk = File(outputDir, "aligned.apk")
            try {
                RandomAccessFile(unsignedApk, "r").use { raf ->
                    FileOutputStream(alignedApk).use { out ->
                        ZipAlign.alignZip(raf, out, 4, 4096)
                    }
                }
            } catch (e: Exception) {
                addLog(Log.WARN, "Alignment failed, using unaligned APK")
                unsignedApk.copyTo(alignedApk, overwrite = true)
            }

            // Sign APK
            addLog(Log.INFO, "Signing APK...")
            val signedApk = File(outputDir, "modded_signed.apk")
            try {
                APKData.signApks(alignedApk, signedApk, context)
            } catch (e: Exception) {
                addLog(Log.WARN, "Signing failed, returning aligned APK")
                return@withContext alignedApk
            }

            addLog(Log.INFO, "Cleaning up temporary files...")
            buildDir.deleteRecursively()

            val result = signedApk.takeIf { it.exists() && it.length() > 0 } ?: alignedApk
            addLog(Log.INFO, "Build complete: ${result.name}")
            result
        } catch (e: Exception) {
            addLog(Log.ERROR, "Build failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Searches across all smali directories for the .smali file corresponding
     * to the fully qualified launcher activity class name.
     */
    private fun locateSmali(extractDir: File, activityName: String): File? {
        val smaliPath = activityName.replace('.', '/') + ".smali"
        val smaliDirs = extractDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("smali") }
            ?.sortedBy { it.name } ?: return null

        smaliDirs.forEach { smaliDir ->
            val direct = File(smaliDir, smaliPath)
            if (direct.exists()) return direct

            smaliDir.walkTopDown().forEach { file ->
                if (file.isFile && file.name == smaliPath.substringAfterLast('/')) {
                    if (file.path.removePrefix(smaliDir.path).replace('\\', '/').drop(1) == smaliPath) {
                        return file
                    }
                }
            }
        }
        return null
    }

    private fun injectOnCreate(smaliFile: File): Boolean = injectLibraryLoad(smaliFile, "onCreate(")

    private fun injectConstructor(smaliFile: File): Boolean = injectLibraryLoad(smaliFile, "<init>")

    /**
     * Injects the System.loadLibrary("hxo") call into the specified method
     * (either onCreate or constructor) right after the .locals directive.
     */
    private fun injectLibraryLoad(smaliFile: File, methodSignatureContains: String): Boolean {
        return try {
            val lines = smaliFile.readLines().toMutableList()
            var inTargetMethod = false
            var insertIndex = -1

            for (i in lines.indices) {
                val trimmed = lines[i].trim()
                if (trimmed.startsWith(".method") && trimmed.contains(methodSignatureContains)) {
                    inTargetMethod = true
                }
                if (inTargetMethod && trimmed.startsWith(".locals")) {
                    insertIndex = i + 1
                    break
                }
            }

            if (insertIndex > 0) {
                val injection = listOf(
                    "",
                    "    # Hxo Loader's Loader",
                    "    const-string v0, \"hxo\"",
                    "",
                    "    invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V",
                    ""
                )
                repeat(injection.size) { lines.add(insertIndex++, injection[it]) }
                smaliFile.writeText(lines.joinToString("\n"))
                addLog(Log.INFO, "Injected library loader into $methodSignatureContains")
                true
            } else {
                addLog(Log.WARN, "Could not find injection point in $methodSignatureContains")
                false
            }
        } catch (e: Exception) {
            addLog(Log.ERROR, "Injection failed: ${e.message}")
            false
        }
    }

    /**
     * Copies native .so libraries from app assets (organized by ABI)
     * into the extracted APK's lib/<abi>/ directory.
     */
    private fun addNativeLibs(context: Context, extractDir: File) {
        try {
            val targetLibDir = File(extractDir, "lib").apply { mkdirs() }
            val abis = android.os.Build.SUPPORTED_ABIS

            var copiedCount = 0
            for (abi in abis) {
                try {
                    val assetPath = "libs/$abi"
                    val libs = context.assets.list(assetPath) ?: continue
                    if (libs.isEmpty()) continue

                    val targetAbiDir = File(targetLibDir, abi).apply { mkdirs() }

                    for (lib in libs.filter { it.endsWith(".so") }) {
                        val target = File(targetAbiDir, lib)
                        context.assets.open("$assetPath/$lib").use { input ->
                            FileOutputStream(target).use { output ->
                                input.copyTo(output)
                            }
                        }
                        copiedCount++
                    }
                } catch (e: Exception) {
                    // Skip if no libs for this ABI
                }
            }

            if (copiedCount > 0) {
                addLog(Log.INFO, "Copied $copiedCount native libraries")
            } else {
                addLog(Log.WARN, "No native libraries found in assets")
            }
        } catch (e: Exception) {
            addLog(Log.WARN, "Failed to copy native libraries: ${e.message}")
        }
    }

    /**
     * Determines which folders should be stored without compression
     * in the final APK.
     */
    private fun noCompressFolder(name: String): Boolean =
        name in setOf("assets", "lib", "res")

    /**
     * Determines which files should be stored without compression
     * in the final APK.
     */
    private fun noCompressFile(name: String): Boolean =
        name.equals("resources.arsc", ignoreCase = true) ||
                (name.startsWith("classes") && name.endsWith(".dex"))
}