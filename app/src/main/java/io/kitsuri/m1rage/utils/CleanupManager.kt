// utils/CleanupManager.kt
package io.kitsuri.m1rage.utils

import android.content.Context
import android.util.Log
import java.io.File

object CleanupManager {

    private const val TAG = "CleanupManager"
    private const val WORKSPACE_DIR = "apk_workspace"

    fun hasOldWorkspaces(context: Context): Boolean {
        val externalFilesDir = context.getExternalFilesDir(null) ?: return false
        val workspaceRoot = File(externalFilesDir, WORKSPACE_DIR)
        return workspaceRoot.exists() && (workspaceRoot.listFiles()?.any { it.isDirectory } == true)
    }

    fun hasTempCacheFiles(context: Context): Boolean {
        val cacheDir = context.cacheDir
        if (!cacheDir.exists()) return false
        return cacheDir.listFiles()?.any { file ->
            file.isFile && file.name.startsWith("temp_") &&
                    (file.name.endsWith(".apk") || file.name.endsWith(".apks"))
        } == true
    }

    fun cleanupWorkspaces(context: Context, onLog: (String) -> Unit = {}) {
        val externalFilesDir = context.getExternalFilesDir(null) ?: return
        val workspaceRoot = File(externalFilesDir, WORKSPACE_DIR)

        if (!workspaceRoot.exists() || !workspaceRoot.isDirectory) return

        val children = workspaceRoot.listFiles() ?: return
        var deletedCount = 0

        for (child in children) {
            if (child.isDirectory) {
                try {
                    child.deleteRecursively()
                    deletedCount++
                    onLog("Deleted: apk_workspace/${child.name}")
                } catch (e: Exception) {
                    onLog("Failed to delete: ${child.name}")
                    Log.w(TAG, "Failed to delete old workspace: ${child.name}", e)
                }
            }
        }

        if (deletedCount > 0) {
            onLog("Removed $deletedCount old workspace(s)")
        }
    }

    fun cleanupTempCache(context: Context, onLog: (String) -> Unit = {}) {
        val cacheDir = context.cacheDir
        if (!cacheDir.exists()) return

        val tempFiles = cacheDir.listFiles { file ->
            file.isFile && file.name.startsWith("temp_") &&
                    (file.name.endsWith(".apk") || file.name.endsWith(".apks"))
        } ?: return

        var deletedCount = 0
        for (file in tempFiles) {
            try {
                file.delete()
                deletedCount++
                onLog("Deleted temp: ${file.name}")
            } catch (e: Exception) {
                onLog("Failed: ${file.name}")
            }
        }

        if (deletedCount > 0) {
            onLog("Cleaned $deletedCount temporary file(s)")
        }
    }

    fun deleteWorkspace(workspaceDir: File) {
        if (!workspaceDir.exists()) return
        try {
            workspaceDir.deleteRecursively()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete workspace: ${workspaceDir.absolutePath}", e)
        }
    }

    fun clearAppCache(context: Context): Boolean {
        return try {
            context.cacheDir?.deleteRecursively()
            context.codeCacheDir?.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }
}