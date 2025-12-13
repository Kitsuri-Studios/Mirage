package io.kitsuri.m1rage.utils;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.sunilpaulmathew.sCommon.APKUtils.sAPKUtils;

/*
 * Modified: R + APKEditorUtils removed.
 */
public class APKItems implements Serializable {

    private final File mAPKFile;

    public APKItems(File apkFile) {
        this.mAPKFile = apkFile;
    }

    public boolean isDirectory() {
        return mAPKFile.isDirectory();
    }

    public CharSequence getAppName(Context context) {
        return sAPKUtils.getAPKName(isDirectory() ? getBaseAPKPath(context) : getPath(), context);
    }

    private Drawable getAPKIcon(Context context) {
        return sAPKUtils.getAPKIcon(isDirectory() ? getBaseAPKPath(context) : getPath(), context);
    }

    public File getAPKFile() {
        return mAPKFile;
    }

    public File getBaseAPK(Context context) {
        if (isDirectory()) {
            for (File file : Objects.requireNonNull(mAPKFile.listFiles())) {
                if (file.isFile()
                        && sAPKUtils.getPackageName(file.getAbsolutePath(), context) != null) {
                    return file;
                }
            }
        }
        return null;
    }

    public String getBaseAPKPath(Context context) {
        return getBaseAPK(context).getAbsolutePath();
    }

    public String getName() {
        return mAPKFile.getName();
    }

    public String getPackageName(Context context) {
        return sAPKUtils.getPackageName(
                isDirectory() ? getBaseAPKPath(context) : getPath(),
                context
        );
    }

    public String getPath() {
        return mAPKFile.getAbsolutePath();
    }

    @SuppressLint("StringFormatInvalid")
    public String getSize(Context context) {
        long length = 0;

        if (isDirectory()) {
            for (File file : Objects.requireNonNull(mAPKFile.listFiles())) {
                if (file.isFile() && sAPKUtils.getPackageName(file.getAbsolutePath(), context) != null) {
                    length += file.length();
                }
            }
        } else {
            length = getAPKFile().length();
        }

        // TODO: Replace with your own formatted string
        return "Size: " + sAPKUtils.getAPKSize(length);
    }

    public String getVersionName(Context context) {

        // TODO: Replace with your own formatted string
        return "Version: " + sAPKUtils.getVersionName(
                isDirectory() ? getBaseAPKPath(context) : getPath(),
                context
        );
    }

    public void loadAppIcon(ImageView view) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Drawable drawable = getAPKIcon(view.getContext());

            if (drawable == null) {
                // TODO: Replace fallback drawable with your own (no R references allowed)
                drawable = ContextCompat.getDrawable(view.getContext(), android.R.drawable.sym_def_app_icon);

                // TODO: Replace with your own accent-color logic if needed
                // view.setColorFilter(CustomThemeUtils.getAccentColor(view.getContext()));
            }

            Drawable finalDrawable = drawable;
            handler.post(() -> view.setImageDrawable(finalDrawable));
        });
    }
}

