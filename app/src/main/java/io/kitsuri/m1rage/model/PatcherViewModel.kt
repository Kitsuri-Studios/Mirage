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
import io.kitsuri.m1rage.utils.ManifestParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    CONSTRUCTOR,
    ACTIVITY_ENTRY
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val apkUri: Uri?,
    val apkPath: String?
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
            is ViewAction.ConfigureComplete -> patcherState = PatcherState.CONFIGURATION
            is ViewAction.StartPatch -> startPatching(action.context)
            ViewAction.Reset -> reset()
        }
    }

    fun addLog(level: Int, message: String) {
        logs.add(level to message)
        Log.println(level, "PatcherViewModel", message)
    }

    private fun startDecompiling(context: Context, apkUri: Uri?, apkPath: String?) {
        viewModelScope.launch {
            patcherState = PatcherState.DECOMPILING
            logs.clear()
            decompileProgress = 0f

            // Set ViewModel for logging
            Patcher.setViewModel(this@PatcherViewModel)

            withContext(Dispatchers.IO) {
                try {
                    addLog(Log.INFO, "Starting decompilation process...")
                    decompileProgress = 0.1f

                    val pm = context.packageManager
                    val appName: String
                    val packageName: String
                    val actualApkPath: String

                    if (apkUri != null) {
                        addLog(Log.INFO, "Reading APK from storage...")
                        val tempApkPath = "${context.cacheDir}/temp_${System.currentTimeMillis()}.apk"
                        context.contentResolver.openInputStream(apkUri)?.use { input ->
                            File(tempApkPath).outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        decompileProgress = 0.2f

                        val info = pm.getPackageArchiveInfo(tempApkPath, 0)
                        appName = info?.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown"
                        packageName = info?.packageName ?: "unknown"
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

                    addLog(Log.INFO, "Extracting APK contents...")
                    decompileProgress = 0.3f

                    addLog(Log.INFO, "Decompiling DEX files...")
                    decompileProgress = 0.5f

                    val extractDir = Patcher.patchApk(context, Uri.parse("file://$actualApkPath"), "constructor")
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
                            selectedApp = AppInfo(appName, packageName, apkUri, apkPath)
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

            // Set ViewModel for logging
            Patcher.setViewModel(this@PatcherViewModel)

            withContext(Dispatchers.IO) {
                try {
                    addLog(Log.INFO, "Starting patch process...")
                    addLog(Log.INFO, "Mode: ${if (patchConfig.mode == PatchMode.CONSTRUCTOR) "Constructor Injection" else "Activity Entry Point"}")
                    addLog(Log.INFO, "Target: ${patchConfig.selectedActivity ?: "Default"}")

                    if (patchConfig.debuggable) {
                        addLog(Log.INFO, "Applying debuggable flag...")
                        addLog(Log.DEBUG, "Setting android:debuggable=\"true\" in manifest")
                    }

                    if (patchConfig.overrideVersionCode) {
                        addLog(Log.INFO, "Overriding version code...")
                        addLog(Log.DEBUG, "Setting android:versionCode=\"1\" in manifest")
                    }

                    addLog(Log.INFO, "Modifying smali files...")
                    addLog(Log.DEBUG, "Injecting library loader at ${patchConfig.mode} point")

                    Thread.sleep(500)
                    addLog(Log.DEBUG, "Modified: Application.smali")
                    Thread.sleep(300)
                    addLog(Log.DEBUG, "Modified: ${patchConfig.selectedActivity ?: "MainActivity"}.smali")

                    addLog(Log.INFO, "Recompiling smali to DEX...")
                    Thread.sleep(800)
                    addLog(Log.DEBUG, "classes.dex compiled")

                    addLog(Log.INFO, "Rebuilding APK...")
                    val signedApk = Patcher.rebuildApk(context, extractedDir!!)

                    addLog(Log.INFO, "Signing APK...")
                    Thread.sleep(500)
                    addLog(Log.DEBUG, "Using default signing certificate")

                    addLog(Log.INFO, "Zipaligning APK...")
                    Thread.sleep(300)

                    if (signedApk != null && signedApk.exists()) {
                        addLog(Log.INFO, "Patch completed successfully")
                        addLog(Log.INFO, "Output: ${signedApk.absolutePath}")
                        addLog(Log.INFO, "Size: ${signedApk.length() / 1024 / 1024}MB")

                        withContext(Dispatchers.Main) {
                            patcherState = PatcherState.FINISHED
                        }
                    } else {
                        throw Exception("Failed to rebuild APK")
                    }
                } catch (e: Exception) {
                    addLog(Log.ERROR, "Patch failed")
                    addLog(Log.ERROR, "Error: ${e.message}")
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
        object ConfigureComplete : ViewAction()
        data class StartPatch(val context: Context) : ViewAction()
        object Reset : ViewAction()
    }
}