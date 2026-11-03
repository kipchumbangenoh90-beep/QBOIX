package com.qboix.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppUpdateManager {
    private static final String RELEASE_API =
            "https://api.github.com/repos/kipchumbangenoh90-beep/QBOIX/releases/latest";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppUpdateManager() {}

    public static final class UpdateInfo {
        public final int versionCode;
        public final String versionName;
        public final String apkUrl;
        public final String sha256;
        public final String releaseNotes;

        UpdateInfo(int code, String name, String url, String hash, String notes) {
            versionCode = code;
            versionName = name;
            apkUrl = url;
            sha256 = hash;
            releaseNotes = notes;
        }
    }

    public interface CheckCallback {
        void onUpToDate(String installedVersion);
        void onUpdateAvailable(UpdateInfo update);
        void onError(String message);
    }

    public interface DownloadCallback {
        void onDownloaded(File apk);
        void onError(String message);
    }

    public static void checkForUpdate(Context context, CheckCallback callback) {
        final Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                JSONObject release = new JSONObject(readUrl(RELEASE_API, true));
                JSONArray assets = release.optJSONArray("assets");
                String metadataUrl = null;
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        if ("update.json".equals(asset.optString("name"))) {
                            metadataUrl = asset.optString("browser_download_url", null);
                            break;
                        }
                    }
                }
                if (metadataUrl == null || metadataUrl.trim().isEmpty()) {
                    throw new IllegalStateException("The latest GitHub release has no update.json metadata yet.");
                }

                JSONObject metadata = new JSONObject(readUrl(metadataUrl, false));
                int latestCode = metadata.getInt("versionCode");
                String latestName = metadata.optString("versionName", "Unknown");
                String apkUrl = metadata.getString("apkUrl");
                String hash = metadata.getString("sha256").toLowerCase(Locale.US);
                String notes = metadata.optString("releaseNotes", release.optString("body", "Q-BOIX update"));

                if (!apkUrl.startsWith("https://github.com/")) {
                    throw new IllegalStateException("The release APK URL is not a trusted GitHub download.");
                }
                if (!hash.matches("[a-f0-9]{64}")) {
                    throw new IllegalStateException("The release SHA-256 checksum is missing or invalid.");
                }

                int installedCode = getInstalledVersionCode(app);
                String installedName = getInstalledVersionName(app);
                if (latestCode <= installedCode) {
                    MAIN.post(() -> callback.onUpToDate(installedName));
                } else {
                    UpdateInfo info = new UpdateInfo(latestCode, latestName, apkUrl, hash, notes);
                    MAIN.post(() -> callback.onUpdateAvailable(info));
                }
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Could not check for updates." : e.getMessage();
                MAIN.post(() -> callback.onError(message));
            }
        });
    }

    public static void downloadUpdate(Context context, UpdateInfo update, DownloadCallback callback) {
        final Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            File output = null;
            try {
                File directory = new File(app.getFilesDir(), "updates");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("Could not create the update download folder.");
                }
                output = new File(directory, "QBOIX-update-" + update.versionCode + ".apk");
                downloadToFile(update.apkUrl, output);
                String actualHash = sha256(output);
                if (!actualHash.equalsIgnoreCase(update.sha256)) {
                    // Never install a damaged or modified APK.
                    //noinspection ResultOfMethodCallIgnored
                    output.delete();
                    throw new SecurityException("APK verification failed. The download was not installed.");
                }
                File verified = output;
                MAIN.post(() -> callback.onDownloaded(verified));
            } catch (Exception e) {
                if (output != null) {
                    //noinspection ResultOfMethodCallIgnored
                    output.delete();
                }
                String message = e.getMessage() == null ? "Could not download the update." : e.getMessage();
                MAIN.post(() -> callback.onError(message));
            }
        });
    }

    public static void installApk(Activity activity, File apk) throws Exception {
        if (apk == null || !apk.isFile() || apk.length() == 0) {
            throw new IllegalStateException("The verified update file is missing.");
        }
        Uri uri = FileProvider.getUriForFile(
                activity, activity.getPackageName() + ".fileprovider", apk);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(install);
    }

    private static int getInstalledVersionCode(Context context) throws Exception {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        if (Build.VERSION.SDK_INT >= 28) return (int) info.getLongVersionCode();
        //noinspection deprecation
        return info.versionCode;
    }

    private static String getInstalledVersionName(Context context) throws Exception {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return info.versionName == null ? "unknown" : info.versionName;
    }

    private static String readUrl(String address, boolean api) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "Q-BOIX-Android-Updater");
        connection.setRequestProperty("Accept", api ? "application/vnd.github+json" : "application/json");
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return result.toString();
        } finally {
            connection.disconnect();
        }
    }

    private static void downloadToFile(String address, File output) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "Q-BOIX-Android-Updater");
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream stream = new FileOutputStream(output)) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) stream.write(buffer, 0, count);
            stream.getFD().sync();
        } finally {
            connection.disconnect();
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format(Locale.US, "%02x", value));
        return result.toString();
    }
}
