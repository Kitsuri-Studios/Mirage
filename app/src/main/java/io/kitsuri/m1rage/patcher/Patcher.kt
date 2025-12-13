package io.kitsuri.m1rage.patcher

import android.content.Context
import android.net.Uri
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

            val apkFile = File(workDir, "input.apk")
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                FileOutputStream(apkFile).use { output -> input.copyTo(output) }
            }

            val extractDir = File(workDir, "extracted")
            extractDir.mkdirs()
            APKInstallUtils.unzip(apkFile.absolutePath, extractDir.absolutePath)

            val manifestFile = File(extractDir, "AndroidManifest.xml")
            if (!manifestFile.exists()) {
                return@withContext null
            }

            val launcherActivity = ManifestParser.findLauncherActivity(manifestFile)
                ?: return@withContext null

            val dexFiles = extractDir.listFiles()
                ?.filter { it.name.endsWith(".dex") }
                ?.sortedBy { it.name }

            dexFiles?.forEach { dexFile ->
                val smaliDirName = when (dexFile.name) {
                    "classes.dex" -> "smali"
                    else -> "smali_${dexFile.nameWithoutExtension}"
                }
                val outputSmali = File(extractDir, smaliDirName)
                outputSmali.mkdirs()

                try {
                    DexToSmali(true, dexFile, outputSmali, 30, dexFile.name).execute()
                } catch (e: Exception) {
                    // Decompilation failure for this DEX is non-fatal; continue with others
                }
            }

            val smaliFile = locateSmali(extractDir, launcherActivity)
                ?: return@withContext null

            val injected = if (injectionMode == "constructor") {
                injectConstructor(smaliFile)
            } else {
                injectOnCreate(smaliFile)
            }

            if (!injected) return@withContext null

            addNativeLibs(context, extractDir)

            extractDir // Return modified extracted directory
        } catch (e: Exception) {
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
            extractDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("smali")) {
                    val dexName = if (file.name == "smali") "classes.dex"
                    else file.name.replace("smali_", "") + ".dex"

                    val dexFile = File(buildDir, dexName)
                    SmaliUtils.smaliToDex(file, dexFile, 30)
                }
            }

            // Remove old signatures
            File(buildDir, "META-INF").deleteRecursively()

            // Create unsigned APK
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
            val alignedApk = File(outputDir, "aligned.apk")
            try {
                RandomAccessFile(unsignedApk, "r").use { raf ->
                    FileOutputStream(alignedApk).use { out ->
                        ZipAlign.alignZip(raf, out, 4, 4096)
                    }
                }
            } catch (e: Exception) {
                unsignedApk.copyTo(alignedApk, overwrite = true)
            }

            // Sign APK
            val signedApk = File(outputDir, "modded_signed.apk")
            try {
                APKData.signApks(alignedApk, signedApk, context)
            } catch (e: Exception) {
                return@withContext alignedApk
            }

            buildDir.deleteRecursively()
            signedApk.takeIf { it.exists() && it.length() > 0 } ?: alignedApk
        } catch (e: Exception) {
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
                true
            } else false
        } catch (e: Exception) {
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
                    }
                } catch (e: Exception) {
                    // Skip if no libs for this ABI
                    // Exception will be handled in future
                }
            }
        } catch (e: Exception) {
            // Silent fail – native libs are optional
            // Exception will be handled in future

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