package io.kitsuri.m1rage.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;

import java.io.File;

public class ZipUtils {

    public static void zip(File sourceDir, File zipFile) {
        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipParameters params = new ZipParameters();
            for (File f : sourceDir.listFiles()) {
                if (f.isDirectory()) {
                    if (isStoredFolder(f.getName())) {
                        params.setCompressionMethod(CompressionMethod.STORE);
                        zf.addFolder(f, params);
                        params.setCompressionMethod(CompressionMethod.DEFLATE);
                    } else {
                        zf.addFolder(f);
                    }
                } else {
                    if (isStoredFile(f.getName())) {
                        params.setCompressionMethod(CompressionMethod.STORE);
                        zf.addFile(f, params);
                        params.setCompressionMethod(CompressionMethod.DEFLATE);
                    } else {
                        zf.addFile(f);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ZipUtils", "Zip failed", e);
        }
    }

    private static boolean isStoredFolder(String name) {
        return "assets".equals(name) || "lib".equals(name) || "res".equals(name);
    }

    private static boolean isStoredFile(String name) {
        return "resources.arsc".equalsIgnoreCase(name) ||
                (name.startsWith("classes") && name.endsWith(".dex"));
    }

    public static void unzip(String zipPath, String destPath) {
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            zipFile.extractAll(destPath);
        } catch (Exception e) {
            android.util.Log.e("ZipUtils", "Unzip failed", e);
        }
    }
}
