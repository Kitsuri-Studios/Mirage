package io.kitsuri.m1rage.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import io.kitsuri.m1rage.model.PatchedAppInfo

object PatchedAppScanner {

    private const val TAG = "PatchedAppScanner"
    private const val HXO_META_DATA_KEY = "io.kitsur.HXO_LOADED"

    fun scanPatchedApps(context: Context): List<PatchedAppInfo> {
        val packageManager = context.packageManager
        val patchedApps = mutableListOf<PatchedAppInfo>()

        try {
            val installedApps = packageManager.getInstalledApplications(
                PackageManager.GET_META_DATA
            )

            for (appInfo in installedApps) {
                val metaData = appInfo.metaData ?: continue
                val hxoLoaded = metaData.getBoolean(HXO_META_DATA_KEY, false)

                if (hxoLoaded) {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = try {
                        packageManager.getApplicationIcon(appInfo)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get icon for ${appInfo.packageName}", e)
                        packageManager.defaultActivityIcon
                    }

                    patchedApps.add(
                        PatchedAppInfo(
                            packageName = appInfo.packageName,
                            appName = appName,
                            icon = icon
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan installed apps", e)
        }

        return patchedApps
    }
}