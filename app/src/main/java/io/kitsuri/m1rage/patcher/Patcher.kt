package io.kitsuri.m1rage.patcher

import android.content.Context
import android.net.Uri
import android.util.Log
import io.kitsuri.m1rage.R
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
            val uniqueId = java.util.UUID.randomUUID().toString().take(8)
            val workDir = File(context.getExternalFilesDir(null), "apk_workspace/${timestamp}_${uniqueId}")

            if (workDir.exists()) {
                workDir.deleteRecursively()
            }
            workDir.mkdirs()

            addLog(Log.INFO, context.getString(R.string.patcher_creating_workspace, workDir.name))

            val isSplitBundle = isSplitApksBundle(context, apkUri)

            if (isSplitBundle) {
                addLog(Log.INFO, context.getString(R.string.patcher_detected_split_bundle))
                return@withContext patchSplitApksBundle(context, apkUri, workDir)
            }

            val apkFile = File(workDir, "input.apk")
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                FileOutputStream(apkFile).use { output -> input.copyTo(output) }
            }

            addLog(Log.INFO, context.getString(R.string.patcher_extracting_apk))
            val extractDir = File(workDir, "extracted").apply { mkdirs() }
            APKInstallUtils.unzip(apkFile.absolutePath, extractDir.absolutePath)

            val manifestFile = File(extractDir, "AndroidManifest.xml")
            if (!manifestFile.exists()) {
                addLog(Log.ERROR, context.getString(R.string.patcher_manifest_not_found))
                return@withContext null
            }

            val pkgName = ManifestParser.findPackageName(manifestFile)
            if (pkgName == null) {
                addLog(Log.ERROR, context.getString(R.string.patcher_failed_read_package))
                return@withContext null
            }

            addLog(Log.INFO, context.getString(R.string.patcher_package_info, pkgName))

            injectLoaderDex(context, extractDir)
            ManifestEditor.addProvider(context, manifestFile, pkgName)
            addLog(Log.INFO, context.getString(R.string.patcher_provider_injected))
            ManifestEditor.addMetaData(context, manifestFile, "io.kitsur.HXO_LOADED", "true")
            addLog(Log.INFO, context.getString(R.string.patcher_metadata_injected))
            injectNativeLibs(context, extractDir)

            addLog(Log.INFO, context.getString(R.string.patcher_patch_preparation_complete))
            extractDir

        } catch (e: Exception) {
            addLog(Log.ERROR, context.getString(R.string.patcher_patch_failed, e.message ?: "Unknown error"))
            e.printStackTrace()
            null
        }
    }

    private fun patchSplitApksBundle(
        context: Context,
        bundleUri: Uri,
        workDir: File
    ): File {
        val bundleFile = File(workDir, "bundle.apks")
        context.contentResolver.openInputStream(bundleUri)?.use { input ->
            FileOutputStream(bundleFile).use { output -> input.copyTo(output) }
        }

        addLog(Log.INFO, context.getString(R.string.patcher_extracting_bundle))
        val bundleExtractDir = File(workDir, "bundle_extracted").apply { mkdirs() }
        APKInstallUtils.unzip(bundleFile.absolutePath, bundleExtractDir.absolutePath)

        val apkFiles = bundleExtractDir.walkTopDown()
            .filter { it.isFile && it.extension == "apk" }
            .toList()

        if (apkFiles.isEmpty()) {
            addLog(Log.ERROR, context.getString(R.string.patcher_no_apks_in_bundle))
            throw Exception(context.getString(R.string.patcher_no_apks_in_bundle))
        }

        addLog(Log.INFO, context.getString(R.string.patcher_found_apks_count, apkFiles.size))

        val baseApk = apkFiles.firstOrNull {
            it.nameWithoutExtension.contains("base", ignoreCase = true)
        } ?: apkFiles.first()

        addLog(Log.INFO, context.getString(R.string.patcher_base_apk, baseApk.name))

        val extractDir = File(workDir, "extracted_base").apply { mkdirs() }
        APKInstallUtils.unzip(baseApk.absolutePath, extractDir.absolutePath)

        val manifestFile = File(extractDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) {
            addLog(Log.ERROR, context.getString(R.string.patcher_manifest_not_found_base))
            throw Exception(context.getString(R.string.patcher_manifest_not_found_base))
        }

        val pkgName = ManifestParser.findPackageName(manifestFile)
        if (pkgName == null) {
            addLog(Log.ERROR, context.getString(R.string.patcher_failed_read_package))
            throw Exception(context.getString(R.string.patcher_failed_read_package))
        }

        addLog(Log.INFO, context.getString(R.string.patcher_package_info, pkgName))

        injectLoaderDex(context, extractDir)
        ManifestEditor.addProvider(context, manifestFile, pkgName)
        addLog(Log.INFO, context.getString(R.string.patcher_provider_injected))
        ManifestEditor.addMetaData(context, manifestFile, "io.kitsur.HXO_LOADED", "true")
        addLog(Log.INFO, context.getString(R.string.patcher_metadata_injected))
        injectNativeLibs(context, extractDir)

        val splitsDir = File(workDir, "splits").apply { mkdirs() }
        apkFiles.forEach { apk ->
            if (apk != baseApk) {
                val targetFile = File(splitsDir, apk.name)
                apk.copyTo(targetFile, overwrite = true)
            }
        }

        addLog(Log.INFO, context.getString(R.string.patcher_split_apks_prepared))
        return extractDir
    }

    private fun patchSplitApkManifest(
        context: Context,
        splitApk: File,
        workDir: File,
        versionCode: Int? = null,
        debuggable: Boolean? = null
    ): File {
        try {
            val splitExtractDir = File(workDir, "temp_split_${splitApk.nameWithoutExtension}").apply {
                deleteRecursively()
                mkdirs()
            }

            addLog(Log.DEBUG, context.getString(R.string.patcher_extracting_split_for_patching, splitApk.name))
            APKInstallUtils.unzip(splitApk.absolutePath, splitExtractDir.absolutePath)

            val manifestFile = File(splitExtractDir, "AndroidManifest.xml")
            if (manifestFile.exists()) {
                versionCode?.let {
                    addLog(Log.DEBUG, context.getString(R.string.patcher_setting_version_code, it, splitApk.name))
                    ManifestEditor.setVersionCode(context, manifestFile, it)
                }
                debuggable?.let {
                    addLog(Log.DEBUG, context.getString(R.string.patcher_setting_debuggable, it.toString(), splitApk.name))
                    ManifestEditor.setDebuggable(context, manifestFile, it)
                }
            }

            File(splitExtractDir, "META-INF").deleteRecursively()

            val repackedSplit = File(workDir, "repacked_${splitApk.name}")
            zipDirectory(splitExtractDir, repackedSplit)

            splitExtractDir.deleteRecursively()

            return repackedSplit

        } catch (e: Exception) {
            addLog(Log.WARN, context.getString(R.string.patcher_failed_patch_split, splitApk.name, e.message ?: "Unknown"))
            return splitApk
        }
    }

    suspend fun rebuildApk(context: Context, extractDir: File): File? =
        withContext(Dispatchers.IO) {
            try {
                val workDir = extractDir.parentFile!!
                val outputDir = File(workDir.parentFile, "output").apply { mkdirs() }

                val splitsDir = File(workDir, "splits")
                val hasSplits = splitsDir.exists() && splitsDir.listFiles()?.isNotEmpty() == true

                if (hasSplits) {
                    addLog(Log.INFO, context.getString(R.string.patcher_rebuilding_split_bundle))
                    return@withContext rebuildSplitApksBundle(context, extractDir, outputDir, splitsDir)
                }

                rebuildSingleApk(context, extractDir, outputDir)

            } catch (e: Exception) {
                addLog(Log.ERROR, context.getString(R.string.patcher_build_failed, e.message ?: "Unknown error"))
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

        addLog(Log.INFO, context.getString(R.string.patcher_preparing_build_dir))

        extractDir.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                f.copyRecursively(File(buildDir, f.name), overwrite = true)
            } else {
                f.copyTo(File(buildDir, f.name), overwrite = true)
            }
        }

        File(buildDir, "META-INF").deleteRecursively()

        addLog(Log.INFO, context.getString(R.string.patcher_creating_unsigned_apk))
        val unsignedApk = File(outputDir, "unsigned.apk").apply { delete() }
        zipDirectory(buildDir, unsignedApk)

        addLog(Log.INFO, context.getString(R.string.patcher_aligning_apk))
        val alignedApk = File(outputDir, "aligned.apk")
        alignApk(unsignedApk, alignedApk)

        addLog(Log.INFO, context.getString(R.string.patcher_signing_apk))
        val signedApk = File(outputDir, "modded_signed.apk")
        APKData.signApks(alignedApk, signedApk, context)

        buildDir.deleteRecursively()
        addLog(Log.INFO, context.getString(R.string.patcher_build_complete, signedApk.name))
        return signedApk
    }

    private fun rebuildSplitApksBundle(
        context: Context,
        extractDir: File,
        outputDir: File,
        splitsDir: File
    ): File {
        val workDir = extractDir.parentFile!!

        if (!splitsDir.canonicalPath.startsWith(workDir.canonicalPath)) {
            addLog(Log.ERROR, context.getString(R.string.patcher_invalid_splits_dir))
            throw Exception(context.getString(R.string.patcher_invalid_splits_dir))
        }

        val buildDir = File(workDir.parentFile, "build_temp").apply {
            deleteRecursively()
            mkdirs()
        }

        addLog(Log.INFO, context.getString(R.string.patcher_rebuilding_base_apk))
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

        addLog(Log.INFO, context.getString(R.string.patcher_aligning_base))
        val alignedBase = File(outputDir, "base_aligned.apk")
        alignApk(unsignedBase, alignedBase)

        addLog(Log.INFO, context.getString(R.string.patcher_signing_base))
        val signedBase = File(outputDir, "base.apk")
        APKData.signApks(alignedBase, signedBase, context)

        val splitFiles = splitsDir.listFiles() ?: emptyArray()
        addLog(Log.INFO, context.getString(R.string.patcher_processing_split_count, splitFiles.size))

        val baseManifest = File(extractDir, "AndroidManifest.xml")
        val targetVersionCode = if (baseManifest.exists()) ManifestParser.findVersionCode(baseManifest) else null
        val isDebuggable = if (baseManifest.exists()) ManifestParser.isDebuggable(baseManifest) else null

        val signedSplitFiles = mutableListOf<File>()
        splitFiles.forEach { splitApk ->
            addLog(Log.INFO, context.getString(R.string.patcher_processing_split, splitApk.name))

            val patchedSplit = if (targetVersionCode != null || isDebuggable != null) {
                patchSplitApkManifest(context, splitApk, workDir, targetVersionCode, isDebuggable)
            } else {
                splitApk
            }

            val alignedSplit = File(outputDir, "temp_aligned_${patchedSplit.name}")
            alignApk(patchedSplit, alignedSplit)

            val signedSplit = File(outputDir, splitApk.name)
            APKData.signApks(alignedSplit, signedSplit, context)
            alignedSplit.delete()

            if (patchedSplit != splitApk) patchedSplit.delete()

            signedSplitFiles.add(signedSplit)
            addLog(Log.INFO, context.getString(R.string.patcher_signed_split, splitApk.name))
        }

        addLog(Log.INFO, context.getString(R.string.patcher_creating_bundle))
        val finalBundle = File(outputDir, "modded_signed.apks")
        ZipFile(finalBundle).use { zip ->
            zip.addFile(signedBase)
            signedSplitFiles.forEach { zip.addFile(it) }
        }

        buildDir.deleteRecursively()
        addLog(Log.INFO, context.getString(R.string.patcher_bundle_complete, finalBundle.name))
        addLog(Log.INFO, context.getString(R.string.patcher_total_apks, splitFiles.size + 1))

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
                    name.removePrefix("classes").removeSuffix(".dex").toIntOrNull() ?: 0
                else -> 0
            }
            if (index > maxIndex) maxIndex = index
        }

        val nextIndex = maxIndex + 1
        val targetName = if (nextIndex == 1) "classes.dex" else "classes$nextIndex.dex"
        val target = File(extractDir, targetName)

        context.assets.open("loader/hxo.dex").use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }

        addLog(Log.INFO, context.getString(R.string.patcher_injected_loader_dex, targetName))
    }

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

            val msg = if (copied > 0) {
                context.getString(R.string.patcher_copied_native_libs, copied)
            } else {
                context.getString(R.string.patcher_no_native_libs)
            }
            addLog(if (copied > 0) Log.INFO else Log.WARN, msg)

        } catch (e: Exception) {
            addLog(Log.WARN, context.getString(R.string.patcher_failed_copy_native_libs, e.message ?: "Unknown"))
        }
    }

    private fun noCompressFolder(name: String) =
        name in setOf("assets", "lib", "res")

    private fun noCompressFile(name: String) =
        name.equals("resources.arsc", true) ||
                (name.startsWith("classes") && name.endsWith(".dex"))
}