package io.kitsuri.m1rage.model

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.kitsuri.m1rage.R
import io.kitsuri.m1rage.patcher.Patcher
import io.kitsuri.m1rage.utils.CleanupManager
import io.kitsuri.m1rage.utils.ManifestEditor
import io.kitsuri.m1rage.utils.ManifestParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

enum class PatcherState {
    EMPTY,
    DECOMPILING,
    CONFIGURATION,
    PATCHING,
    FINISHED,
    ERROR
}

enum class PatchMode {
    DEX,
    MAPI
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val apkUri: Uri? = null,
    val apkPath: String? = null,
    val isSplitApk: Boolean = false,
    val splitCount: Int = 0
)

data class PatchConfig(
    val mode: PatchMode = PatchMode.DEX,
    val selectedActivity: String? = null,
    val debuggable: Boolean = false,
    val overrideVersionCode: Boolean = false
)

class PatcherViewModel : ViewModel() {

    var patcherState by mutableStateOf(PatcherState.EMPTY)
        private set

    var selectedApp by mutableStateOf<AppInfo?>(null)
        private set

    var patchConfig by mutableStateOf(PatchConfig())

    var extractedDir by mutableStateOf<File?>(null)
        private set

    var outputApkFile by mutableStateOf<File?>(null)
        private set

    var savedToDownloads by mutableStateOf(false)
        private set

    var availableActivities by mutableStateOf<List<String>>(emptyList())
        private set

    val logs = mutableStateListOf<Pair<Int, String>>()

    var decompileProgress by mutableStateOf(0f)
        private set

    fun dispatch(action: ViewAction) {
        when (action) {
            is ViewAction.StartDecompile -> startDecompiling(action.context, action.apkUri, action.apkPath)
            is ViewAction.StartDecompileFromSplits -> startDecompilingFromSplits(
                action.context, action.packageName, action.appName, action.splitApkPaths
            )
            is ViewAction.ConfigureComplete -> patcherState = PatcherState.CONFIGURATION
            is ViewAction.StartPatch -> startPatching(action.context)
            is ViewAction.SaveToDownloads -> savePatchedApk(action.context)
            ViewAction.Reset -> reset()
        }
    }

    fun addLog(level: Int, message: String) {
        logs.add(level to message)
        Log.println(level, "PatcherViewModel", message)
    }

    private fun getString(context: Context, resId: Int): String = context.getString(resId)
    private fun getString(context: Context, resId: Int, vararg args: Any): String = context.getString(resId, *args)

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

    private fun startDecompilingFromSplits(
        context: Context,
        packageName: String,
        appName: String,
        splitApkPaths: List<String>
    ) {
        viewModelScope.launch {
            reset()
            patcherState = PatcherState.DECOMPILING
            logs.clear()
            decompileProgress = 0f
            Patcher.setViewModel(this@PatcherViewModel)

            withContext(Dispatchers.IO) {
                try {
                    addLog(Log.INFO, getString(context, R.string.vm_processing_split_installed))
                    addLog(Log.INFO, getString(context, R.string.vm_package_info, packageName))
                    addLog(Log.INFO, getString(context, R.string.vm_found_apks, splitApkPaths.size))
                    decompileProgress = 0.1f

                    val timestamp = System.currentTimeMillis()
                    val uniqueId = java.util.UUID.randomUUID().toString().take(8)
                    val workDir = File(context.getExternalFilesDir(null), "apk_workspace/${timestamp}_${uniqueId}")
                    workDir.mkdirs()

                    val splitsDir = File(workDir, "splits").apply { mkdirs() }
                    addLog(Log.INFO, getString(context, R.string.vm_copying_splits))

                    val baseApkPath = splitApkPaths[0]
                    val extractDir = File(workDir, "extracted_base").apply { mkdirs() }
                    addLog(Log.INFO, getString(context, R.string.vm_extracting_base))
                    ZipFile(File(baseApkPath)).extractAll(extractDir.absolutePath)
                    decompileProgress = 0.3f

                    splitApkPaths.drop(1).forEach { path ->
                        val splitFile = File(path)
                        if (splitFile.exists()) {
                            val target = File(splitsDir, splitFile.name)
                            splitFile.copyTo(target, overwrite = true)
                            addLog(Log.DEBUG, getString(context, R.string.vm_copied_split, splitFile.name))
                        }
                    }
                    decompileProgress = 0.4f

                    val manifestFile = File(extractDir, "AndroidManifest.xml")
                    if (!manifestFile.exists()) {
                        throw Exception(getString(context, R.string.vm_manifest_not_found_error))
                    }

                    addLog(Log.INFO, getString(context, R.string.vm_parsing_manifest))
                    val launcherActivity = ManifestParser.findLauncherActivity(manifestFile)
                    val activities = listOfNotNull(launcherActivity)
                    decompileProgress = 0.5f

                    addLog(Log.INFO, getString(context, R.string.vm_injecting_loader_dex))
                    Patcher.injectLoaderDex(context, extractDir)
                    decompileProgress = 0.7f

                    addLog(Log.INFO, getString(context, R.string.vm_injecting_provider))
                    ManifestEditor.addProvider(context, manifestFile, packageName)
                    decompileProgress = 0.8f

                    addLog(Log.INFO, getString(context, R.string.vm_adding_native_libs))
                    Patcher.injectNativeLibs(context, extractDir)
                    decompileProgress = 0.9f

                    addLog(Log.INFO, getString(context, R.string.vm_found_activities, activities.size))
                    addLog(Log.INFO, getString(context, R.string.vm_decompile_success))
                    decompileProgress = 1f

                    withContext(Dispatchers.Main) {
                        selectedApp = AppInfo(appName, packageName, isSplitApk = true, splitCount = splitApkPaths.size)
                        extractedDir = extractDir
                        availableActivities = activities
                        patchConfig = patchConfig.copy(selectedActivity = activities.firstOrNull())
                        patcherState = PatcherState.CONFIGURATION
                    }
                } catch (e: Exception) {
                    addLog(Log.ERROR, getString(context, R.string.vm_error_generic, e.message ?: "Unknown error"))
                    e.printStackTrace()
                    withContext(Dispatchers.Main) { patcherState = PatcherState.ERROR }
                }
            }
        }
    }

    private fun startDecompiling(context: Context, apkUri: Uri?, apkPath: String?) {
        viewModelScope.launch {
            reset()
            patcherState = PatcherState.DECOMPILING
            logs.clear()
            decompileProgress = 0f
            Patcher.setViewModel(this@PatcherViewModel)

            withContext(Dispatchers.IO) {
                try {
                    addLog(Log.INFO, getString(context, R.string.vm_starting_decompile))
                    decompileProgress = 0.1f

                    val pm = context.packageManager
                    val appName: String
                    val packageName: String
                    val actualApkPath: String
                    var isSplit = false
                    var splitCount = 0

                    if (apkUri != null) {
                        isSplit = isSplitApksBundle(context, apkUri)
                        if (isSplit) {
                            addLog(Log.INFO, getString(context, R.string.vm_detected_split_bundle))
                        } else {
                            addLog(Log.INFO, getString(context, R.string.vm_reading_apk_storage))
                        }

                        val tempApkPath = "${context.cacheDir}/temp_${System.currentTimeMillis()}.${if (isSplit) "apks" else "apk"}"
                        context.contentResolver.openInputStream(apkUri)?.use { input ->
                            File(tempApkPath).outputStream().use { output -> input.copyTo(output) }
                        }
                        decompileProgress = 0.2f

                        if (isSplit) {
                            val bundleFile = File(tempApkPath)
                            val bundleExtractDir = File(context.cacheDir, "temp_bundle_${System.currentTimeMillis()}").apply { mkdirs() }
                            ZipFile(bundleFile).extractAll(bundleExtractDir.absolutePath)

                            val apkFiles = bundleExtractDir.walkTopDown().filter { it.extension == "apk" }.toList()
                            splitCount = apkFiles.size
                            addLog(Log.INFO, getString(context, R.string.vm_found_in_bundle, splitCount))

                            val baseApk = apkFiles.firstOrNull { it.nameWithoutExtension.contains("base", ignoreCase = true) } ?: apkFiles.first()
                            val info = pm.getPackageArchiveInfo(baseApk.absolutePath, 0)
                            appName = info?.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown"
                            packageName = info?.packageName ?: "unknown"

                            bundleExtractDir.deleteRecursively()
                        } else {
                            val info = pm.getPackageArchiveInfo(tempApkPath, 0)
                            appName = info?.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown"
                            packageName = info?.packageName ?: "unknown"
                        }

                        actualApkPath = tempApkPath
                        addLog(Log.INFO, getString(context, R.string.vm_package_info, packageName))
                    } else {
                        addLog(Log.INFO, getString(context, R.string.vm_reading_installed_app))
                        val info = pm.getPackageArchiveInfo(apkPath!!, 0)
                        appName = info?.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown"
                        packageName = info?.packageName ?: "unknown"
                        actualApkPath = apkPath
                        addLog(Log.INFO, getString(context, R.string.vm_package_info, packageName))
                        decompileProgress = 0.2f
                    }

                    if (isSplit) {
                        addLog(Log.INFO, getString(context, R.string.vm_extracting_split_bundle))
                    } else {
                        addLog(Log.INFO, getString(context, R.string.vm_extracting_apk_contents))
                    }
                    decompileProgress = 0.3f

                    addLog(Log.INFO, getString(context, R.string.vm_decompiling_dex))
                    decompileProgress = 0.5f

                    val extractDir = Patcher.patchApk(context, Uri.parse("file://$actualApkPath"))
                    decompileProgress = 0.8f

                    if (extractDir != null) {
                        addLog(Log.INFO, getString(context, R.string.vm_parsing_manifest))
                        val manifestFile = File(extractDir, "AndroidManifest.xml")
                        val launcherActivity = ManifestParser.findLauncherActivity(manifestFile)
                        val activities = listOfNotNull(launcherActivity)
                        decompileProgress = 0.9f

                        addLog(Log.INFO, getString(context, R.string.vm_found_activities, activities.size))
                        addLog(Log.INFO, getString(context, R.string.vm_decompile_success))
                        decompileProgress = 1f

                        withContext(Dispatchers.Main) {
                            selectedApp = AppInfo(appName, packageName, apkUri, apkPath, isSplit, splitCount)
                            extractedDir = extractDir
                            availableActivities = activities
                            patchConfig = patchConfig.copy(selectedActivity = activities.firstOrNull())
                            patcherState = PatcherState.CONFIGURATION
                        }
                    } else {
                        throw Exception(getString(context, R.string.vm_failed_extract))
                    }
                } catch (e: Exception) {
                    addLog(Log.ERROR, getString(context, R.string.vm_error_generic, e.message ?: "Unknown error"))
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        patcherState = PatcherState.ERROR
                        extractedDir?.parentFile?.let { CleanupManager.deleteWorkspace(it) }
                        extractedDir = null
                    }
                }
            }
        }
    }

    private fun startPatching(context: Context) {
        viewModelScope.launch {
            patcherState = PatcherState.PATCHING
            logs.clear()
            Patcher.setViewModel(this@PatcherViewModel)

            withContext(Dispatchers.IO) {
                try {
                    addLog(Log.INFO, getString(context, R.string.vm_starting_patch))

                    selectedApp?.let {
                        if (it.isSplitApk) {
                            addLog(Log.INFO, getString(context, R.string.vm_processing_split_count, it.splitCount))
                        }
                    }

                    val modeText = if (patchConfig.mode == PatchMode.DEX)
                        getString(context, R.string.vm_mode_dex)
                    else
                        getString(context, R.string.vm_mode_mapi)
                    addLog(Log.INFO, getString(context, R.string.vm_mode_label, modeText))

                    patchConfig.selectedActivity?.let {
                        addLog(Log.INFO, getString(context, R.string.vm_target_activity, it))
                    }

                    val manifestFile = File(extractedDir!!, "AndroidManifest.xml")

                    if (patchConfig.debuggable) {
                        addLog(Log.INFO, getString(context, R.string.vm_applying_debuggable))
                        ManifestEditor.setDebuggable(context, manifestFile, true)
                    }

                    if (patchConfig.overrideVersionCode) {
                        addLog(Log.INFO, getString(context, R.string.vm_overriding_version_code))
                        ManifestEditor.setVersionCode(context, manifestFile, 1)
                    }

                    if (selectedApp?.isSplitApk == true) {
                        addLog(Log.INFO, getString(context, R.string.vm_split_will_be_patched))
                    }

                    addLog(Log.INFO, getString(context, R.string.vm_preparing_patched))

                    val signedApk = Patcher.rebuildApk(context, extractedDir!!)
                        ?: throw Exception("Failed to rebuild APK")

                    addLog(Log.INFO, getString(context, R.string.vm_rebuild_success))
                    addLog(Log.INFO, getString(context, R.string.vm_output_path, signedApk.absolutePath))
                    addLog(Log.INFO, getString(context, R.string.vm_output_size, (signedApk.length() / 1024 / 1024).toInt()))

                    if (selectedApp?.isSplitApk == true) {
                        addLog(Log.INFO, getString(context, R.string.vm_split_bundle_ready))
                        addLog(Log.INFO, getString(context, R.string.vm_matching_signatures))
                        addLog(Log.INFO, getString(context, R.string.vm_install_instructions))
                    }

                    withContext(Dispatchers.Main) {
                        outputApkFile = signedApk
                        patcherState = PatcherState.FINISHED
                        extractedDir?.parentFile?.let { CleanupManager.deleteWorkspace(it) }
                        extractedDir = null
                    }
                } catch (e: Exception) {
                    addLog(Log.ERROR, getString(context, R.string.vm_patch_failed, e.message ?: "Unknown error"))
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        patcherState = PatcherState.ERROR
                        extractedDir?.parentFile?.let { CleanupManager.deleteWorkspace(it) }
                        extractedDir = null
                    }
                }
            }
        }
    }

    private fun reset() {
        patcherState = PatcherState.EMPTY
        selectedApp = null
        extractedDir?.parentFile?.let { CleanupManager.deleteWorkspace(it) }
        extractedDir = null
        outputApkFile = null
        savedToDownloads = false
        logs.clear()
        patchConfig = PatchConfig()
        decompileProgress = 0f
    }

    private fun savePatchedApk(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val sourceFile = outputApkFile ?: return@withContext
                    val appName = selectedApp?.name?.replace(" ", "_") ?: "patched"
                    val fileName = "${appName}_modded.${sourceFile.extension}"

                    val settings = SettingsManager(context)
                    val resolver = context.contentResolver
                    val uriString = settings.getStringValue("save_directory_uri", "")

                    if (uriString.isNotEmpty()) {
                        try {
                            val treeUri = Uri.parse(uriString)
                            if (resolver.persistedUriPermissions.any { it.uri == treeUri && it.isWritePermission }) {
                                copyToUri(context, sourceFile, treeUri, fileName)
                                withContext(Dispatchers.Main) {
                                    addLog(Log.INFO, getString(context, R.string.vm_saved_to_folder, fileName))
                                    savedToDownloads = true
                                }
                                return@withContext
                            }
                        } catch (_: Exception) {}
                    }

                    val fallbackDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Mirage").apply { mkdirs() }
                    sourceFile.copyTo(File(fallbackDir, fileName), overwrite = true)

                    withContext(Dispatchers.Main) {
                        addLog(Log.INFO, getString(context, R.string.vm_saved_to_downloads, fileName))
                        savedToDownloads = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        addLog(Log.ERROR, getString(context, R.string.vm_save_failed, e.message ?: "Unknown error"))
                    }
                }
            }
        }
    }

    fun copyToUri(context: Context, source: File, treeUri: Uri, fileName: String) {
        val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val outFile = docTree.findFile(fileName) ?: docTree.createFile("application/vnd.android.package-archive", fileName) ?: return
        context.contentResolver.openOutputStream(outFile.uri)?.use { out ->
            source.inputStream().use { input -> input.copyTo(out) }
        }
    }

    sealed class ViewAction {
        data class StartDecompile(val context: Context, val apkUri: Uri?, val apkPath: String?) : ViewAction()
        data class StartDecompileFromSplits(
            val context: Context,
            val packageName: String,
            val appName: String,
            val splitApkPaths: List<String>
        ) : ViewAction()
        object ConfigureComplete : ViewAction()
        data class StartPatch(val context: Context) : ViewAction()
        data class SaveToDownloads(val context: Context) : ViewAction()
        object Reset : ViewAction()
    }
}