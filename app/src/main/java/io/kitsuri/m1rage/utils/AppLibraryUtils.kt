package io.kitsuri.m1rage.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

object AppLibraryUtils {
    private const val TAG = "PerAppLibraryUtils"
    private const val MODULES_DIR = "modules"
    private const val HXO_INI = "hxo.ini"
    private const val MODULES_JSON = "modules.json"
    private const val PREFS_NAME = "app_mod_preferences"
    private const val PREFS_MOD_PREFIX = "mod_"

    private val prettyJson = Json {
        prettyPrint = true
        encodeDefaults = true
    }


    fun getAppDirectory(context: Context, packageName: String): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val externalStorage = Environment.getExternalStorageDirectory()
            val mediaDir = File(externalStorage, "Android/media/$packageName")

            if (!mediaDir.exists()) {
                if (!mediaDir.mkdirs()) {
                    Log.e(TAG, "Failed to create media directory for $packageName")
                    return null
                }
            }
            mediaDir
        } else {
            val externalStorageState = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED == externalStorageState) {
                val mediaDir = File(
                    Environment.getExternalStorageDirectory(),
                    "Android/media/$packageName"
                )
                if (!mediaDir.exists()) {
                    mediaDir.mkdirs()
                }
                mediaDir
            } else {
                null
            }
        }
    }


    private fun getModPreferences(context: Context, packageName: String): SharedPreferences {
        return context.getSharedPreferences("$PREFS_NAME.$packageName", Context.MODE_PRIVATE)
    }


    private fun saveModState(context: Context, packageName: String, fileName: String, enabled: Boolean) {
        val prefs = getModPreferences(context, packageName)
        prefs.edit().putBoolean(PREFS_MOD_PREFIX + fileName, enabled).apply()
        Log.d(TAG, "Saved mod state for $packageName: $fileName = $enabled")
    }

    private fun getModState(context: Context, packageName: String, fileName: String, defaultValue: Boolean = true): Boolean {
        val prefs = getModPreferences(context, packageName)
        return prefs.getBoolean(PREFS_MOD_PREFIX + fileName, defaultValue)
    }

    private fun removeModState(context: Context, packageName: String, fileName: String) {
        val prefs = getModPreferences(context, packageName)
        prefs.edit().remove(PREFS_MOD_PREFIX + fileName).apply()
        Log.d(TAG, "Removed mod state from preferences for $packageName: $fileName")
    }

    fun writeIniFile(context: Context, packageName: String, settingsManager: io.kitsuri.m1rage.model.AppSettingsManager): Boolean {
        return try {
            val appDir = getAppDirectory(context, packageName)
            if (appDir == null) {
                Log.e(TAG, "App directory is not available for $packageName")
                return false
            }

            val iniFile = File(appDir, HXO_INI)
            Log.d(TAG, "Writing hxo.ini to: ${iniFile.absolutePath}")

            val iniContent = buildString {
                appendLine(";")
                appendLine("; HXO Configuration for $packageName")
                appendLine(";")
                appendLine("")
                appendLine("[HXO]")
                appendLine("hxo=${if (settingsManager.getBooleanValue("hxo_enabled", true)) "1" else "0"}")
                appendLine("hxo_dir=$MODULES_DIR")
                appendLine("sleep=${settingsManager.getFloatValue("load_delay", 0f).toInt()}")
                appendLine("UnloadAfterExecution=${if (settingsManager.getBooleanValue("unload_after_execution", false)) "1" else "0"}")
                appendLine("")
                appendLine(";EXPERIMENTAL: DON'T MODIFY")
                appendLine(";unless you really need to")
                appendLine("[1337]")
                appendLine("lib=${settingsManager.getStringValue("lib_path", "/usr/lib/")}")
                appendLine("")
            }

            iniFile.writeText(iniContent)
            Log.d(TAG, "hxo.ini written successfully for $packageName")
            true
        } catch (e: IOException) {
            Log.e(TAG, "IOException while writing hxo.ini for $packageName", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while writing hxo.ini for $packageName", e)
            false
        }
    }

    fun initializeAppFiles(context: Context, packageName: String, settingsManager: io.kitsuri.m1rage.model.AppSettingsManager): Boolean {
        return try {
            val appDir = getAppDirectory(context, packageName)
            if (appDir == null) {
                Log.e(TAG, "App directory is not available for $packageName")
                return false
            }

            Log.d(TAG, "Using app directory: ${appDir.absolutePath}")

            // Create modules directory
            val modulesDir = File(appDir, MODULES_DIR)
            if (!modulesDir.exists()) {
                val created = modulesDir.mkdirs()
                Log.d(TAG, "Modules directory created for $packageName: $created")
                if (!created) {
                    Log.e(TAG, "Failed to create modules directory for $packageName")
                    return false
                }
            }

            if (!writeIniFile(context, packageName, settingsManager)) {
                Log.e(TAG, "Failed to write hxo.ini for $packageName")
                return false
            }

            val modulesJsonFile = File(appDir, MODULES_JSON)
            if (!modulesJsonFile.exists()) {
                try {
                    val emptyJson = prettyJson.encodeToString(emptyMap<String, Boolean>())
                    modulesJsonFile.writeText(emptyJson)
                    Log.d(TAG, "modules.json created successfully for $packageName")
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating modules.json for $packageName", e)
                    return false
                }
            }

            Log.d(TAG, "File initialization completed successfully for $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during file initialization for $packageName", e)
            false
        }
    }


    fun copyModFile(context: Context, packageName: String, sourceFile: File, fileName: String): File? {
        return try {
            val appDir = getAppDirectory(context, packageName)
            if (appDir == null) {
                Log.e(TAG, "App directory is not available for $packageName")
                return null
            }

            val modulesDir = File(appDir, MODULES_DIR)
            if (!modulesDir.exists()) {
                if (!modulesDir.mkdirs()) {
                    Log.e(TAG, "Failed to create modules directory for $packageName")
                    return null
                }
            }

            val destFile = File(modulesDir, fileName)
            Log.d(TAG, "Copying mod file for $packageName from ${sourceFile.absolutePath} to ${destFile.absolutePath}")

            sourceFile.copyTo(destFile, overwrite = true)

            if (!destFile.exists()) {
                Log.e(TAG, "Destination file was not created after copying for $packageName")
                return null
            }

            Log.d(TAG, "Mod file copied successfully for $packageName")
            destFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying mod file for $packageName", e)
            null
        }
    }


    fun updateModulesJson(context: Context, packageName: String, fileName: String): Boolean {
        return try {
            val appDir = getAppDirectory(context, packageName)
            if (appDir == null) {
                Log.e(TAG, "App directory is not available for $packageName")
                return false
            }

            val modulesJsonFile = File(appDir, MODULES_JSON)
            Log.d(TAG, "Updating modules.json for $packageName with file: $fileName")

            val modulesJson = if (modulesJsonFile.exists()) {
                try {
                    prettyJson.decodeFromString<Map<String, Boolean>>(modulesJsonFile.readText())
                } catch (e: Exception) {
                    Log.w(TAG, "Error reading existing modules.json for $packageName", e)
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val previousState = getModState(context, packageName, fileName, true)
            val updatedModules = modulesJson.toMutableMap().apply {
                put(fileName, previousState)
            }

            val jsonString = prettyJson.encodeToString(updatedModules)
            modulesJsonFile.writeText(jsonString)

            saveModState(context, packageName, fileName, previousState)
            Log.d(TAG, "modules.json updated successfully for $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating modules.json for $packageName", e)
            false
        }
    }


    fun toggleMod(context: Context, packageName: String, fileName: String, enabled: Boolean): Boolean {
        return try {
            val appDir = getAppDirectory(context, packageName)
            if (appDir == null) {
                Log.e(TAG, "App directory is not available for $packageName")
                return false
            }

            val modulesJsonFile = File(appDir, MODULES_JSON)
            val modulesJson = if (modulesJsonFile.exists()) {
                try {
                    prettyJson.decodeFromString<Map<String, Boolean>>(modulesJsonFile.readText())
                } catch (e: Exception) {
                    Log.w(TAG, "Error reading existing modules.json for $packageName", e)
                    return false
                }
            } else {
                return false
            }

            if (!modulesJson.containsKey(fileName)) {
                Log.w(TAG, "Mod $fileName not found in modules.json for $packageName")
                return false
            }

            val updatedModules = modulesJson.toMutableMap().apply {
                put(fileName, enabled)
            }

            val jsonString = prettyJson.encodeToString(updatedModules)
            modulesJsonFile.writeText(jsonString)

            saveModState(context, packageName, fileName, enabled)
            Log.d(TAG, "Mod $fileName ${if (enabled) "enabled" else "disabled"} for $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mod for $packageName", e)
            false
        }
    }


    fun removeMod(context: Context, packageName: String, fileName: String): Boolean {
        return try {
            val appDir = getAppDirectory(context, packageName)
            if (appDir == null) {
                Log.e(TAG, "App directory is not available for $packageName")
                return false
            }

            val modulesDir = File(appDir, MODULES_DIR)
            val modFile = File(modulesDir, fileName)
            if (modFile.exists()) {
                modFile.delete()
                Log.d(TAG, "Mod file $fileName deleted for $packageName")
            }

            val modulesJsonFile = File(appDir, MODULES_JSON)
            val modulesJson = if (modulesJsonFile.exists()) {
                try {
                    prettyJson.decodeFromString<Map<String, Boolean>>(modulesJsonFile.readText())
                } catch (e: Exception) {
                    return false
                }
            } else {
                emptyMap()
            }

            val updatedModules = modulesJson.toMutableMap().apply {
                remove(fileName)
            }

            val jsonString = prettyJson.encodeToString(updatedModules)
            modulesJsonFile.writeText(jsonString)

            removeModState(context, packageName, fileName)
            Log.d(TAG, "Mod $fileName removed for $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing mod for $packageName", e)
            false
        }
    }


    fun refreshMods(context: Context, packageName: String): Boolean {
        return try {
            val appDir = getAppDirectory(context, packageName)
            if (appDir == null) {
                Log.e(TAG, "App directory is not available for $packageName")
                return false
            }

            val modulesDir = File(appDir, MODULES_DIR)
            if (!modulesDir.exists()) {
                Log.w(TAG, "Modules directory doesn't exist for $packageName")
                return false
            }

            val actualModFiles = modulesDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".so") || file.name.endsWith(".hxo"))
            }?.map { it.name }?.toSet() ?: emptySet()

            val modulesJsonFile = File(appDir, MODULES_JSON)
            val currentModulesJson = if (modulesJsonFile.exists()) {
                try {
                    prettyJson.decodeFromString<Map<String, Boolean>>(modulesJsonFile.readText())
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val updatedModules = mutableMapOf<String, Boolean>()

            currentModulesJson.forEach { (fileName, enabled) ->
                if (actualModFiles.contains(fileName)) {
                    val savedState = getModState(context, packageName, fileName, enabled)
                    updatedModules[fileName] = savedState
                } else {
                    removeModState(context, packageName, fileName)
                }
            }

            actualModFiles.forEach { fileName ->
                if (!currentModulesJson.containsKey(fileName)) {
                    val savedState = getModState(context, packageName, fileName, true)
                    updatedModules[fileName] = savedState
                    saveModState(context, packageName, fileName, savedState)
                }
            }

            val jsonString = prettyJson.encodeToString(updatedModules)
            modulesJsonFile.writeText(jsonString)

            Log.d(TAG, "Mods refreshed successfully for $packageName. Total mods: ${updatedModules.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing mods for $packageName", e)
            false
        }
    }


    fun getAllMods(context: Context, packageName: String): Map<String, Boolean>? {
        return try {
            val appDir = getAppDirectory(context, packageName)
            if (appDir == null) {
                Log.e(TAG, "App directory is not available for $packageName")
                return null
            }

            val modulesJsonFile = File(appDir, MODULES_JSON)
            if (modulesJsonFile.exists()) {
                prettyJson.decodeFromString<Map<String, Boolean>>(modulesJsonFile.readText())
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all mods for $packageName", e)
            null
        }
    }


    fun getAppDirectoryPath(context: Context, packageName: String): String? {
        return getAppDirectory(context, packageName)?.absolutePath
    }
}