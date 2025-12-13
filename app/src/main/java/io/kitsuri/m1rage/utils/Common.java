package io.kitsuri.m1rage.utils;

import android.content.Context;

import io.kitsuri.m1rage.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import in.sunilpaulmathew.sCommon.CommonUtils.sCommonUtils;


public class Common {

    private static final List<String> mErrorList = new ArrayList<>();
    private static int mError = 0, mSuccess = 0;
    private static String mFileToReplace = null;

    public static boolean isCancelled(Context context) {
        return sCommonUtils.getBoolean("cancelled", false, context);
    }

    public static boolean isFinished(Context context) {
        return Objects.equals(sCommonUtils.getString("exploringStatus", null, context), "finished");
    }

    public static boolean isTextMatched(String searchText, String searchWord) {
        for (int a = 0; a < searchText.length() - searchWord.length() + 1; a++) {
            if (searchWord.equalsIgnoreCase(searchText.substring(a, a + searchWord.length()))) {
                return true;
            }
        }
        return false;
    }

    public static int getError() {
        return mError;
    }

    public static int getSuccess() {
        return mSuccess;
    }

    public static List<String> getErrorList() {
        return mErrorList;
    }

    public static String getFileToReplace() {
        return mFileToReplace;
    }

    public static String getPackageName(Context context) {
        return sCommonUtils.getString("packageName", null, context);
    }

    public static String getStatus(Context context) {
        return isCancelled(context) ? "canceling..." : sCommonUtils.getString("exploringStatus", null, context);
    }

    public static void isCancelled(boolean b, Context context) {
        sCommonUtils.saveBoolean("cancelled", b, context);
    }

    public static void setFinishStatus(Context context) {
        if (!isFinished(context)) {
            sCommonUtils.saveString("exploringStatus", "finished", context);
        }
    }

    public static void setError(int i) {
        mError = i;
    }

    public static void setFileToReplace(String fileToReplace) {
        mFileToReplace = fileToReplace;
    }

    public static void setStatus(String status, Context context) {
        sCommonUtils.saveString("exploringStatus", status, context);
    }

    public static void setSuccess(int i) {
        mSuccess = i;
    }

}