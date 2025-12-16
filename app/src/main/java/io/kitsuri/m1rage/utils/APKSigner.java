package io.kitsuri.m1rage.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.apksig.ApkSigner;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;

import in.sunilpaulmathew.sCommon.FileUtils.sFileUtils;
import io.kitsuri.m1rage.model.PatcherViewModel;

public class APKSigner {

    private static final String TAG = "APKSigner";
    private static PatcherViewModel viewModel;

    private final Context mContext;

    private static final String SIGN_DIR = "keystore";
    private static final String PK8_NAME = "HXOManager.pk8";
    private static final String CRD_NAME = "HXOManager_crd";

    public static void setViewModel(PatcherViewModel vm) {
        viewModel = vm;
    }

    private static void addLog(int level, String message) {
        if (viewModel != null) {
            viewModel.addLog(level, message);
        } else {
            Log.println(level, TAG, message);
        }
    }

    public APKSigner(Context context) {
        mContext = context;
        ensureDefaultSigningKeys();
    }

    public void sign(File apkFile, File output) throws Exception {
        addLog(Log.INFO, "Signing APK: " + apkFile.getName());

        File pk8 = getPK8PrivateKey(mContext);
        File crd = getSigningCredentials(mContext);

        if (!pk8.exists() || !crd.exists()) {
            addLog(Log.ERROR, "Signing keys missing");
            throw new IllegalStateException("Signing keys missing");
        }

        addLog(Log.DEBUG, "Loading signing certificate");
        ApkSigner.SignerConfig signerConfig =
                new ApkSigner.SignerConfig.Builder(
                        "CERT",
                        new PK8File(pk8).getPrivateKey(),
                        Collections.singletonList(getCertificate(mContext))
                ).build();

        ApkSigner.Builder builder =
                new ApkSigner.Builder(Collections.singletonList(signerConfig));

        builder.setInputApk(apkFile);
        builder.setOutputApk(output);
        builder.setCreatedBy("Kitsuri Studios");
        builder.setV1SigningEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setV2SigningEnabled(true);
            addLog(Log.DEBUG, "V2 signing enabled");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setV3SigningEnabled(true);
            addLog(Log.DEBUG, "V3 signing enabled");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setV4SigningEnabled(true);
            addLog(Log.DEBUG, "V4 signing enabled");
        }

        builder.setMinSdkVersion(-1);

        addLog(Log.INFO, "Applying signature...");
        builder.build().sign();
        addLog(Log.INFO, "APK signed successfully");
    }

    public static File getPK8PrivateKey(Context context) {
        return new File(context.getFilesDir(), SIGN_DIR + "/" + PK8_NAME);
    }

    public static File getSigningCredentials(Context context) {
        return new File(context.getFilesDir(), SIGN_DIR + "/" + CRD_NAME);
    }

    private static X509Certificate getCertificate(Context context) throws JSONException {
        JSONObject json =
                new JSONObject(sFileUtils.read(getSigningCredentials(context)));

        String cert = json.getString("x509Certificate");

        return encodeCertificate(
                new ByteArrayInputStream(cert.getBytes())
        );
    }

    public static X509Certificate encodeCertificate(InputStream in) {
        try {
            return (X509Certificate) CertificateFactory
                    .getInstance("X509")
                    .generateCertificate(in);
        } catch (CertificateException e) {
            addLog(Log.ERROR, "Invalid signing certificate: " + e.getMessage());
            throw new RuntimeException("Invalid signing certificate", e);
        }
    }

    private void ensureDefaultSigningKeys() {
        File dir = new File(mContext.getFilesDir(), SIGN_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            addLog(Log.DEBUG, "Created keystore directory");
        }

        copyAssetIfMissing(PK8_NAME);
        copyAssetIfMissing(CRD_NAME);
    }

    private void copyAssetIfMissing(String name) {
        File out = new File(mContext.getFilesDir(), SIGN_DIR + "/" + name);
        if (out.exists()) return;

        try (InputStream in = mContext.getAssets().open(SIGN_DIR + "/" + name);
             FileOutputStream fos = new FileOutputStream(out)) {

            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) {
                fos.write(buf, 0, r);
            }
            addLog(Log.DEBUG, "Copied signing key: " + name);
        } catch (Exception e) {
            addLog(Log.ERROR, "Missing signing asset: " + name);
            throw new RuntimeException("Missing signing asset: " + name, e);
        }
    }
}