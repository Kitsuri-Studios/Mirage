package io.kitsuri.m1rage.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;



import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import in.sunilpaulmathew.sCommon.APKUtils.sAPKUtils;
import in.sunilpaulmathew.sCommon.CommonUtils.sCommonUtils;
import in.sunilpaulmathew.sCommon.FileUtils.sFileUtils;
import io.kitsuri.m1rage.R;


public class APKData {

    public static List<APKItems> getData(String searchWord, Context context) {
        List<APKItems> mData = new CopyOnWriteArrayList<>();
        for (File mFile : getAPKList(context)) {
            if (sCommonUtils.getString("apkTypes", "apks", context).equals("bundles")) {
                if (mFile.isDirectory() && !mFile.getName().equals("APK") && isValidBundle(mFile, context)) {
                    if (searchWord == null) {
                        mData.add(new APKItems(mFile));
                    } else if (Common.isTextMatched(mFile.getAbsolutePath(), searchWord)) {
                        mData.add(new APKItems(mFile));
                    }
                }
            } else {
                if (mFile.exists() && mFile.getName().endsWith(".apk")) {
                    if (searchWord == null) {
                        mData.add(new APKItems(mFile));
                    } else if (sAPKUtils.getAPKName(mFile.getAbsolutePath(), context) != null && Common.isTextMatched(Objects.requireNonNull(
                            sAPKUtils.getAPKName(mFile.getAbsolutePath(), context)).toString(), searchWord)) {
                        mData.add(new APKItems(mFile));
                    } else if (Common.isTextMatched(mFile.getName(), searchWord)) {
                        mData.add(new APKItems(mFile));
                    }
                }
            }
        }
        Collections.sort(mData, (lhs, rhs) -> String.CASE_INSENSITIVE_ORDER.compare(lhs.getAPKFile().getName(), rhs.getAPKFile().getName()));
        if (!sCommonUtils.getBoolean("az_order", true, context)) {
            Collections.reverse(mData);
        }
        return mData;
    }

    private static File[] getAPKList(Context context) {
        return getExportPath(context).listFiles();
    }

    public static File getExportPath(Context context) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "decomp");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (file.exists() && file.isFile()) {
                sFileUtils.delete(file);
            }
            sFileUtils.mkdir(file);
        } else {
            if (!file.exists()) {
                sFileUtils.mkdir(file);
            }
        }
        return file;
    }

    public static void signApks(File apk, File signedAPK, Context context) {
        try {
            checkAndPrepareSigningEnvironment(context);

            APKSigner apkSigner = new APKSigner(context);
            apkSigner.sign(apk, signedAPK);
        } catch (Exception ignored) {}
    }

    private static void checkAndPrepareSigningEnvironment(Context context) {
        if (APKSigner.getPK8PrivateKey(context).exists()) {
            return;
        }

        sFileUtils.mkdir(new File(context.getFilesDir(), "signing"));
        sFileUtils.copyAssetFile("APKEditor.pk8", APKSigner.getPK8PrivateKey(context), context);
    }

    public static String findPackageName(List<String> apkList, Context context) {
        String name = null;
        for (String mAPKs : apkList) {
            if (sAPKUtils.getPackageName(mAPKs, context) != null) {
                name = Objects.requireNonNull(sAPKUtils.getPackageName(mAPKs, context));
                break;
            }
        }
        return name;
    }

    public static List<String> splitApks(File file) {
        List<String> list = new CopyOnWriteArrayList<>();
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File mFile : Objects.requireNonNull(file.listFiles())) {
                    if (mFile.getName().endsWith(".apk")) {
                        list.add(mFile.getAbsolutePath());
                    }
                }
            } else {
                return splitApks(Objects.requireNonNull(file.getParentFile()));
            }
        }
        return list;
    }

    public static List<String> splitApks(String apkPath) {
        return splitApks(new File(apkPath));
    }

    public static boolean isAppBundle(String path) {
        return splitApks(path).size() > 1;
    }

    private static boolean fileToExclude(File file) {
        return file.isDirectory() && file.getName().equals(".aeeBackup") || file.isDirectory() && file.getName().equals(".aeeBuild")
                || file.isDirectory() && file.getName().equals("META-INF") || file.isDirectory() && file.getName().startsWith("classes")
                && file.getName().endsWith(".dex");
    }

    private static boolean isValidBundle(File parentDir, Context context) {
        for (File files : Objects.requireNonNull(parentDir.listFiles())) {
            if (files.getName().endsWith(".apk") && sAPKUtils.getPackageName(files.getAbsolutePath(), context) != null) {
                return true;
            }
        }
        return false;
    }




}