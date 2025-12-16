package io.kitsuri.m1rage.model

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.kitsuri.m1rage.patcher.Patcher
import io.kitsuri.m1rage.utils.ManifestEditor
import io.kitsuri.m1rage.utils.ManifestParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream

enum class PatcherState {
    EMPTY,
    DECOMPILING,
    CONFIGURATION,
    PATCHING,
    FINISHED,
    ERROR
}

enum class PatchMode {
    CONSTRUCTOR,
    ACTIVITY_ENTRY
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val apkUri: Uri?,
    val apkPath: String?,
    val isSplitApk: Boolean = false,
    val splitCount: Int = 0
)

data class PatchConfig(
    val mode: PatchMode = PatchMode.CONSTRUCTOR,
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
            ViewAction.Reset -> reset()
        }
    }

    fun addLog(level: Int, message: String) {
        logs.add(level to message)
        Log.println(level, "PatcherViewModel", message)
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

    private fun startDecompilingFromSplits(
        context: Context,
        packageName: String,
        appName: String,
        splitApkPaths: List<String>
    ) {
        viewModelScope.launch {
            patcherState = PatcherState.DECOMPILING
            logs.clear()
            decompileProgress = 0f

            Patcher.setViewModel(this@PatcherViewModel)

            withContext(Dispatchers.IO) {
                try {
                    addLog(Log.INFO, "Processing split APKs from installed app")
                    addLog(Log.INFO, "Package: $packageName")
                    addLog(Log.INFO, "Found ${splitApkPaths.size} APK file(s)")
                    decompileProgress = 0.1f

                    // Create workspace
                    val timestamp = System.currentTimeMillis()
                    val workDir = File(context.getExternalFilesDir(null), "apk_workspace/$timestamp")
                    workDir.mkdirs()

                    // Create splits directory to store non-base APKs
                    val splitsDir = File(workDir, "splits").apply { mkdirs() }

                    addLog(Log.INFO, "Copying split APKs...")

                    // First APK is always base, rest are splits
                    val baseApkPath = splitApkPaths[0]
                    val baseApk = File(baseApkPath)

                    // Extract base APK for patching
                    val extractDir = File(workDir, "extracted_base").apply { mkdirs() }
                    addLog(Log.INFO, "Extracting base APK...")
                    net.lingala.zip4j.ZipFile(baseApk).extractAll(extractDir.absolutePath)
                    decompileProgress = 0.3f

                    // Copy split APKs to splits directory (skip base at index 0)
                    splitApkPaths.drop(1).forEachIndexed { index, path ->
                        val splitFile = File(path)
                        if (splitFile.exists()) {
                            val targetFile = File(splitsDir, splitFile.name)
                            splitFile.copyTo(targetFile, overwrite = true)
                            addLog(Log.DEBUG, "Copied split: ${splitFile.name}")
                        }
                    }
                    decompileProgress = 0.4f

                    // Verify manifest exists
                    val manifestFile = File(extractDir, "AndroidManifest.xml")
                    if (!manifestFile.exists()) {
                        throw Exception("AndroidManifest.xml not found in base APK")
                    }

                    // Parse activities
                    addLog(Log.INFO, "Parsing AndroidManifest.xml...")
                    val launcherActivity = ManifestParser.findLauncherActivity(manifestFile)
                    val activities = listOfNotNull(launcherActivity)
                    decompileProgress = 0.5f

                    // Apply patches to base APK
                    addLog(Log.INFO, "Injecting loader DEX...")
                    Patcher.injectLoaderDex(context, extractDir)
                    decompileProgress = 0.7f

                    addLog(Log.INFO, "Injecting provider...")
                    ManifestEditor.addProvider(context, manifestFile, packageName)
                    decompileProgress = 0.8f

                    addLog(Log.INFO, "Adding native libraries...")
                    Patcher.injectNativeLibs(context, extractDir)
                    decompileProgress = 0.9f

                    addLog(Log.INFO, "Found ${activities.size} activities")
                    addLog(Log.INFO, "Decompilation completed successfully")
                    decompileProgress = 1f

                    withContext(Dispatchers.Main) {
                        selectedApp = AppInfo(
                            appName,
                            packageName,
                            null,
                            null,
                            isSplitApk = true,
                            splitCount = splitApkPaths.size
                        )
                        extractedDir = extractDir
                        availableActivities = activities
                        patchConfig = patchConfig.copy(selectedActivity = activities.firstOrNull())
                        patcherState = PatcherState.CONFIGURATION
                    }

                } catch (e: Exception) {
                    addLog(Log.ERROR, "Error: ${e.message}")
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        patcherState = PatcherState.ERROR
                    }
                }
            }
        }
    }

    private fun startDecompiling(context: Context, apkUri: Uri?, apkPath: String?) {
        viewModelScope.launch {
            patcherState = PatcherState.DECOMPILING
            logs.clear()
            decompileProgress = 0f

            Patcher.setViewModel(this@PatcherViewModel)

            withContext(Dispatchers.IO) {
                try {
                    addLog(Log.INFO, "Starting decompilation process...")
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
                            addLog(Log.INFO, "Detected split APKs bundle (.apks/.xapk)")
                        } else {
                            addLog(Log.INFO, "Reading APK from storage...")
                        }

                        val tempApkPath = "${context.cacheDir}/temp_${System.currentTimeMillis()}.${if (isSplit) "apks" else "apk"}"
                        context.contentResolver.openInputStream(apkUri)?.use { input ->
                            File(tempApkPath).outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        decompileProgress = 0.2f

                        if (isSplit) {
                            val bundleFile = File(tempApkPath)
                            val bundleExtractDir = File(context.cacheDir, "temp_bundle_${System.currentTimeMillis()}")
                            bundleExtractDir.mkdirs()

                            ZipFile(bundleFile).extractAll(bundleExtractDir.absolutePath)

                            val apkFiles = bundleExtractDir.walkTopDown()
                                .filter { it.isFile && it.extension == "apk" }
                                .toList()
                            splitCount = apkFiles.size
                            addLog(Log.INFO, "Found $splitCount APK file(s) in bundle")

                            val baseApk = apkFiles.firstOrNull {
                                it.nameWithoutExtension.contains("base", ignoreCase = true)
                            } ?: apkFiles.first()

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
                        addLog(Log.INFO, "Package: $packageName")
                    } else {
                        addLog(Log.INFO, "Reading APK from installed app...")
                        val info = pm.getPackageArchiveInfo(apkPath!!, 0)
                        appName = info?.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown"
                        packageName = info?.packageName ?: "unknown"
                        actualApkPath = apkPath
                        addLog(Log.INFO, "Package: $packageName")
                        decompileProgress = 0.2f
                    }

                    if (isSplit) {
                        addLog(Log.INFO, "Extracting split APKs bundle...")
                    } else {
                        addLog(Log.INFO, "Extracting APK contents...")
                    }
                    decompileProgress = 0.3f

                    addLog(Log.INFO, "Decompiling DEX files...")
                    decompileProgress = 0.5f

                    val extractDir = Patcher.patchApk(context, Uri.parse("file://$actualApkPath"))
                    decompileProgress = 0.8f

                    if (extractDir != null) {
                        addLog(Log.INFO, "Parsing AndroidManifest.xml...")
                        val manifestFile = File(extractDir, "AndroidManifest.xml")
                        val launcherActivity = ManifestParser.findLauncherActivity(manifestFile)

                        val activities = listOfNotNull(launcherActivity)
                        decompileProgress = 0.9f

                        addLog(Log.INFO, "Found ${activities.size} activities")
                        addLog(Log.INFO, "Decompilation completed successfully")
                        decompileProgress = 1f

                        withContext(Dispatchers.Main) {
                            selectedApp = AppInfo(
                                appName,
                                packageName,
                                apkUri,
                                apkPath,
                                isSplit,
                                splitCount
                            )
                            extractedDir = extractDir
                            availableActivities = activities
                            patchConfig = patchConfig.copy(selectedActivity = activities.firstOrNull())
                            patcherState = PatcherState.CONFIGURATION
                        }
                    } else {
                        throw Exception("Failed to extract APK")
                    }
                } catch (e: Exception) {
                    addLog(Log.ERROR, "Error: ${e.message}")
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        patcherState = PatcherState.ERROR
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
                    addLog(Log.INFO, "Starting patch process")

                    if (selectedApp?.isSplitApk == true) {
                        addLog(Log.INFO, "Processing split APKs (${selectedApp?.splitCount} files)")
                    }

                    addLog(
                        Log.INFO,
                        "Mode: ${
                            if (patchConfig.mode == PatchMode.CONSTRUCTOR)
                                "Constructor Injection"
                            else
                                "Activity Entry Point"
                        }"
                    )

                    patchConfig.selectedActivity?.let {
                        addLog(Log.INFO, "Target activity: $it")
                    }

                    if (patchConfig.debuggable) {
                        addLog(Log.INFO, "Applying debuggable flag")
                    }

                    if (patchConfig.overrideVersionCode) {
                        addLog(Log.INFO, "Overriding version code")
                    }

                    addLog(Log.INFO, "Preparing patched APK")

                    val signedApk = Patcher.rebuildApk(context, extractedDir!!)
                        ?: throw Exception("Failed to rebuild APK")

                    addLog(Log.INFO, "APK rebuilt successfully")
                    addLog(Log.INFO, "Output: ${signedApk.absolutePath}")
                    addLog(
                        Log.INFO,
                        "Size: ${signedApk.length() / 1024 / 1024} MB"
                    )

                    if (selectedApp?.isSplitApk == true) {
                        addLog(Log.INFO, "Split APKs bundle ready for installation")
                        addLog(Log.INFO, "Use 'adb install-multiple' or SAI app to install")
                    }

                    withContext(Dispatchers.Main) {
                        patcherState = PatcherState.FINISHED
                    }

                } catch (e: Exception) {
                    addLog(Log.ERROR, "Patch failed: ${e.message}")
                    e.printStackTrace()

                    withContext(Dispatchers.Main) {
                        patcherState = PatcherState.ERROR
                    }
                }
            }
        }
    }

    private fun reset() {
        patcherState = PatcherState.EMPTY
        selectedApp = null
        extractedDir = null
        logs.clear()
        patchConfig = PatchConfig()
        decompileProgress = 0f
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
        object Reset : ViewAction()
    }
}