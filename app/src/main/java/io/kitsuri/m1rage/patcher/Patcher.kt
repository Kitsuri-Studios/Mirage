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

    fun setViewModel(vm: PatcherViewModel) {
        viewModel = vm
        APKSigner.setViewModel(vm)
        ZipAlign.setViewModel(vm)
    }

    private fun addLog(level: Int, msg: String) {
        viewModel?.addLog(level, msg)
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    private fun isSplitApksBundle(context: Context, uri: Uri): Boolean {
        val fileName = getFileName(context, uri)?.lowercase() ?: return false
        return fileName.endsWith(".apks") || fileName.endsWith(".xapk")
    }

    suspend fun patchApk(
        context: Context,
        apkUri: Uri
    ): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val workDir = File(context.getExternalFilesDir(null), "apk_workspace/$timestamp")
            workDir.mkdirs()

            addLog(Log.INFO, "Creating workspace")

            // Check if it's a split APKs bundle
            val isSplitBundle = isSplitApksBundle(context, apkUri)

            if (isSplitBundle) {
                addLog(Log.INFO, "Detected split APKs bundle")
                return@withContext patchSplitApksBundle(context, apkUri, workDir)
            }

            // Regular single APK flow
            val apkFile = File(workDir, "input.apk")
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                FileOutputStream(apkFile).use { output -> input.copyTo(output) }
            }

            addLog(Log.INFO, "Extracting APK...")
            val extractDir = File(workDir, "extracted").apply { mkdirs() }
            APKInstallUtils.unzip(apkFile.absolutePath, extractDir.absolutePath)

            val manifestFile = File(extractDir, "AndroidManifest.xml")
            if (!manifestFile.exists()) {
                addLog(Log.ERROR, "AndroidManifest.xml not found")
                return@withContext null
            }

            val pkgName = ManifestParser.findPackageName(manifestFile)
            if (pkgName == null) {
                addLog(Log.ERROR, "Failed to read package name")
                return@withContext null
            }

            addLog(Log.INFO, "Package: $pkgName")

            // Apply patches
            injectLoaderDex(context, extractDir)
            ManifestEditor.addProvider(context, manifestFile, pkgName)
            addLog(Log.INFO, "Provider injected")
            injectNativeLibs(context, extractDir)

            addLog(Log.INFO, "Patch preparation complete")
            extractDir

        } catch (e: Exception) {
            addLog(Log.ERROR, "Patch failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun patchSplitApksBundle(
        context: Context,
        bundleUri: Uri,
        workDir: File
    ): File {
        // Copy bundle to workspace
        val bundleFile = File(workDir, "bundle.apks")
        context.contentResolver.openInputStream(bundleUri)?.use { input ->
            FileOutputStream(bundleFile).use { output -> input.copyTo(output) }
        }

        // Extract the bundle
        addLog(Log.INFO, "Extracting split APKs bundle...")
        val bundleExtractDir = File(workDir, "bundle_extracted").apply { mkdirs() }
        APKInstallUtils.unzip(bundleFile.absolutePath, bundleExtractDir.absolutePath)

        // Find all APK files
        val apkFiles = bundleExtractDir.walkTopDown()
            .filter { it.isFile && it.extension == "apk" }
            .toList()

        if (apkFiles.isEmpty()) {
            addLog(Log.ERROR, "No APK files found in bundle")
            throw Exception("No APK files found in bundle")
        }

        addLog(Log.INFO, "Found ${apkFiles.size} APK file(s)")

        // Find base APK
        val baseApk = apkFiles.firstOrNull {
            it.nameWithoutExtension.contains("base", ignoreCase = true)
        } ?: apkFiles.first()

        addLog(Log.INFO, "Base APK: ${baseApk.name}")

        // Extract and patch base APK
        val extractDir = File(workDir, "extracted_base").apply { mkdirs() }
        APKInstallUtils.unzip(baseApk.absolutePath, extractDir.absolutePath)

        val manifestFile = File(extractDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) {
            addLog(Log.ERROR, "AndroidManifest.xml not found in base APK")
            throw Exception("AndroidManifest.xml not found in base APK")
        }

        val pkgName = ManifestParser.findPackageName(manifestFile)
        if (pkgName == null) {
            addLog(Log.ERROR, "Failed to read package name")
            throw Exception("Failed to read package name")
        }

        addLog(Log.INFO, "Package: $pkgName")

        // Apply patches to base APK
        injectLoaderDex(context, extractDir)
        ManifestEditor.addProvider(context, manifestFile, pkgName)
        injectNativeLibs(context, extractDir)

        // Store split APKs for rebuild
        val splitsDir = File(workDir, "splits").apply { mkdirs() }
        apkFiles.forEach { apk ->
            if (apk != baseApk) {
                val targetFile = File(splitsDir, apk.name)
                apk.copyTo(targetFile, overwrite = true)
            }
        }

        addLog(Log.INFO, "Split APKs prepared for signing")
        return extractDir
    }

    suspend fun rebuildApk(context: Context, extractDir: File): File? =
        withContext(Dispatchers.IO) {
            try {
                val workDir = extractDir.parentFile!!
                val outputDir = File(workDir.parentFile, "output").apply { mkdirs() }

                // Check if we have split APKs
                val splitsDir = File(workDir, "splits")
                val hasSplits = splitsDir.exists() && splitsDir.listFiles()?.isNotEmpty() == true

                if (hasSplits) {
                    addLog(Log.INFO, "Rebuilding split APKs bundle")
                    return@withContext rebuildSplitApksBundle(context, extractDir, outputDir, splitsDir)
                }

                // Regular single APK rebuild
                rebuildSingleApk(context, extractDir, outputDir)

            } catch (e: Exception) {
                addLog(Log.ERROR, "Build failed: ${e.message}")
                e.printStackTrace()
                null
            }
        }

    private fun rebuildSingleApk(
        context: Context,
        extractDir: File,
        outputDir: File
    ): File {
        val workDir = extractDir.parentFile!!
        val buildDir = File(workDir.parentFile, "build_temp").apply {
            deleteRecursively()
            mkdirs()
        }

        addLog(Log.INFO, "Preparing build directory")

        // Copy everything
        extractDir.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                f.copyRecursively(File(buildDir, f.name), overwrite = true)
            } else {
                f.copyTo(File(buildDir, f.name), overwrite = true)
            }
        }

        // Remove old signatures
        File(buildDir, "META-INF").deleteRecursively()

        // Build unsigned APK
        addLog(Log.INFO, "Creating unsigned APK")
        val unsignedApk = File(outputDir, "unsigned.apk").apply { delete() }
        zipDirectory(buildDir, unsignedApk)

        // Align
        addLog(Log.INFO, "Aligning APK")
        val alignedApk = File(outputDir, "aligned.apk")
        alignApk(unsignedApk, alignedApk)

        // Sign
        addLog(Log.INFO, "Signing APK")
        val signedApk = File(outputDir, "modded_signed.apk")
        APKData.signApks(alignedApk, signedApk, context)

        buildDir.deleteRecursively()
        addLog(Log.INFO, "Build complete: ${signedApk.name}")
        return signedApk
    }

    private fun rebuildSplitApksBundle(
        context: Context,
        extractDir: File,
        outputDir: File,
        splitsDir: File
    ): File {
        val workDir = extractDir.parentFile!!
        val buildDir = File(workDir.parentFile, "build_temp").apply {
            deleteRecursively()
            mkdirs()
        }


        addLog(Log.INFO, "Rebuilding base APK")
        extractDir.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                f.copyRecursively(File(buildDir, f.name), overwrite = true)
            } else {
                f.copyTo(File(buildDir, f.name), overwrite = true)
            }
        }

        File(buildDir, "META-INF").deleteRecursively()

        val unsignedBase = File(outputDir, "base_unsigned.apk").apply { delete() }
        zipDirectory(buildDir, unsignedBase)


        addLog(Log.INFO, "Aligning base APK")
        val alignedBase = File(outputDir, "base_aligned.apk")
        alignApk(unsignedBase, alignedBase)


        addLog(Log.INFO, "Signing base APK")
        val signedBase = File(outputDir, "base.apk")
        APKData.signApks(alignedBase, signedBase, context)


        val splitFiles = splitsDir.listFiles() ?: emptyArray()
        addLog(Log.INFO, "Processing ${splitFiles.size} split APK(s)")

        val signedSplitFiles = mutableListOf<File>()
        splitFiles.forEach { splitApk ->
            addLog(Log.INFO, "Processing: ${splitApk.name}")

            // Align
            val alignedSplit = File(outputDir, "temp_aligned_${splitApk.name}")
            alignApk(splitApk, alignedSplit)

            // Sign
            val signedSplit = File(outputDir, splitApk.name)
            APKData.signApks(alignedSplit, signedSplit, context)
            alignedSplit.delete()

            signedSplitFiles.add(signedSplit)
            addLog(Log.INFO, "Signed: ${splitApk.name}")
        }

        addLog(Log.INFO, "Creating split APKs bundle")
        val finalBundle = File(outputDir, "modded_signed.apks")
        ZipFile(finalBundle).use { zip ->
            zip.addFile(signedBase)
            signedSplitFiles.forEach { signedSplit ->
                zip.addFile(signedSplit)
            }
        }

        buildDir.deleteRecursively()
        addLog(Log.INFO, "Split APKs bundle complete: ${finalBundle.name}")
        addLog(Log.INFO, "Total APKs: ${splitFiles.size + 1}")

        return finalBundle
    }

    private fun zipDirectory(buildDir: File, outputFile: File) {
        ZipFile(outputFile).use { zip ->
            val params = ZipParameters()
            buildDir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory && noCompressFolder(file.name) -> {
                        params.compressionMethod = CompressionMethod.STORE
                        zip.addFolder(file, params)
                        params.compressionMethod = CompressionMethod.DEFLATE
                    }
                    file.isDirectory -> zip.addFolder(file)
                    noCompressFile(file.name) -> {
                        params.compressionMethod = CompressionMethod.STORE
                        zip.addFile(file, params)
                        params.compressionMethod = CompressionMethod.DEFLATE
                    }
                    else -> zip.addFile(file)
                }
            }
        }
    }

    private fun alignApk(inputApk: File, outputApk: File) {
        try {
            RandomAccessFile(inputApk, "r").use { raf ->
                FileOutputStream(outputApk).use { out ->
                    ZipAlign.alignZip(raf, out, 4, 4096)
                }
            }
        } catch (e: Exception) {
            addLog(Log.WARN, "Alignment failed, copying as-is: ${e.message}")
            inputApk.copyTo(outputApk, overwrite = true)
        }
    }

    /**
     * Inject prebuilt loader dex (exposed for ViewModel)
     */
    fun injectLoaderDex(context: Context, extractDir: File) {
        val dexFiles = extractDir.listFiles { file ->
            file.isFile && file.name.startsWith("classes") && file.name.endsWith(".dex")
        }.orEmpty()

        var maxIndex = 0

        for (dex in dexFiles) {
            val name = dex.name
            val index = when {
                name == "classes.dex" -> 1
                name.matches(Regex("""classes\d+\.dex""")) ->
                    name.removePrefix("classes")
                        .removeSuffix(".dex")
                        .toIntOrNull() ?: 0
                else -> 0
            }
            if (index > maxIndex) maxIndex = index
        }

        val nextIndex = maxIndex + 1
        val targetName = if (nextIndex == 1) {
            "classes.dex"
        } else {
            "classes$nextIndex.dex"
        }

        val target = File(extractDir, targetName)

        context.assets.open("loader/hxo.dex").use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }

        addLog(Log.INFO, "Injected loader dex: $targetName")
    }

    /**
     * Add native libraries (exposed for ViewModel)
     */
    fun injectNativeLibs(context: Context, extractDir: File) {
        try {
            val targetLibDir = File(extractDir, "lib").apply { mkdirs() }
            val abis = android.os.Build.SUPPORTED_ABIS
            var copied = 0

            for (abi in abis) {
                try {
                    val assetPath = "libs/$abi"
                    val libs = context.assets.list(assetPath) ?: continue
                    val abiDir = File(targetLibDir, abi).apply { mkdirs() }

                    for (lib in libs.filter { it.endsWith(".so") }) {
                        context.assets.open("$assetPath/$lib").use { input ->
                            FileOutputStream(File(abiDir, lib)).use { out ->
                                input.copyTo(out)
                            }
                        }
                        copied++
                    }
                } catch (_: Exception) {}
            }

            addLog(
                if (copied > 0) Log.INFO else Log.WARN,
                "Copied $copied native libraries"
            )

        } catch (e: Exception) {
            addLog(Log.WARN, "Failed to copy native libs: ${e.message}")
        }
    }

    private fun noCompressFolder(name: String) =
        name in setOf("assets", "lib", "res")

    private fun noCompressFile(name: String) =
        name.equals("resources.arsc", true) ||
                (name.startsWith("classes") && name.endsWith(".dex"))
}