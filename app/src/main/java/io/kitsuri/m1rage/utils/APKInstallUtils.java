package io.kitsuri.m1rage.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class APKInstallUtils {

    private static final String TAG = "APKInstallUtils";

    public static List<File> extractApksFromBundle(String bundlePath, String extractDir) {
        List<File> apks = new ArrayList<>();
        File dir = new File(extractDir);
        if (dir.exists()) deleteDir(dir);

        try (ZipFile zipFile = new ZipFile(bundlePath)) {
            zipFile.extractAll(extractDir);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".apk")) {
                        apks.add(f);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract app bundle", e);
        }
        return apks;
    }

    public static void deleteDir(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDir(child);
                }
            }
        }
        file.delete();
    }

    // Simple unzip/zip helpers
    public static void unzip(String zipPath, String dest) {
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            zipFile.extractAll(dest);
        } catch (Exception e) {
            Log.e(TAG, "Unzip failed", e);
        }
    }

    public static void zipFolder(String sourceFolder, String zipPath) {
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            zipFile.addFolder(new File(sourceFolder));
        } catch (Exception e) {
            Log.e(TAG, "Zip failed", e);
        }
    }
}