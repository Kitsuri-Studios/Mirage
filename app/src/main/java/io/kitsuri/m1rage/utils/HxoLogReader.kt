package io.kitsuri.m1rage.utils

import android.util.Log
import io.kitsuri.m1rage.model.AppLogInfo
import io.kitsuri.m1rage.model.PatchedAppInfo
import java.io.File

object HxoLogReader {

    private const val TAG = "HxoLogReader"
    private const val MEDIA_PATH = "/storage/emulated/0/Android/media"
    private const val LOG_FILE_NAME = "hxo_log.txt"

    fun getLogFilePath(packageName: String): File {
        return File(MEDIA_PATH, "$packageName/$LOG_FILE_NAME")
    }

    fun hasLogFile(packageName: String): Boolean {
        val logFile = getLogFilePath(packageName)
        return logFile.exists() && logFile.isFile
    }

    fun readLogFile(packageName: String): String? {
        val logFile = getLogFilePath(packageName)
        return try {
            if (logFile.exists() && logFile.isFile) {
                logFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log file for $packageName", e)
            null
        }
    }

    fun scanForLogs(patchedApps: List<PatchedAppInfo>): List<AppLogInfo> {
        val appsWithLogs = mutableListOf<AppLogInfo>()

        for (app in patchedApps) {
            val logFile = getLogFilePath(app.packageName)
            if (logFile.exists() && logFile.isFile) {
                appsWithLogs.add(
                    AppLogInfo(
                        packageName = app.packageName,
                        appName = app.appName,
                        icon = app.icon,
                        logFile = logFile
                    )
                )
            }
        }

        return appsWithLogs
    }
}