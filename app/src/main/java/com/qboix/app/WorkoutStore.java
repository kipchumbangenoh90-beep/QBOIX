package com.qboix.app;

import android.content.Context;
import android.util.AtomicFile;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class WorkoutStore {
    private static final String KEY_FARTLEK = "fartlek_sessions.json";
    private static final String KEY_RUNS = "gps_run_sessions.json";

    private WorkoutStore() {}

    private static File userFolder(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (context == null || user == null || user.getUid() == null
                || user.getUid().isEmpty()) {
            return null;
        }

        File folder = new File(
                new File(context.getFilesDir(), "users"),
                user.getUid()
        );
        if (!folder.exists() && !folder.mkdirs()) {
            return null;
        }
        return folder;
    }

    private static JSONArray read(Context context, String name) {
        File folder = userFolder(context);
        if (folder == null) {
            return new JSONArray();
        }

        File file = new File(folder, name);
        if (!file.isFile()) {
            return new JSONArray();
        }

        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new JSONArray(new String(
                    output.toByteArray(),
                    StandardCharsets.UTF_8
            ));
        } catch (IOException | JSONException e) {
            return new JSONArray();
        }
    }

    private static void write(Context context, String name, JSONArray data) {
        File folder = userFolder(context);
        if (folder == null) {
            return;
        }

        AtomicFile file = new AtomicFile(new File(folder, name));
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(data.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(output);
        } catch (IOException ignored) {
            if (output != null) {
                file.failWrite(output);
            }
        }
    }

    public static String getPrivateFolderTitle(FirebaseUser user) {
        if (user == null) {
            return "Sign in to open your private folder";
        }

        String name = user.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            String email = user.getEmail();
            name = email == null || email.trim().isEmpty()
                    ? "Personal"
                    : email.split("@")[0];
        }
        return name.trim() + "'s private workout folder";
    }

    public static synchronized void saveFartlek(
            Context context,
            int sprint,
            int recovery,
            int reps,
            int elapsed
    ) {
        JSONArray sessions = read(context, KEY_FARTLEK);
        JSONObject record = new JSONObject();
        try {
            record.put("type", "FARTLEK");
            record.put("savedAt", System.currentTimeMillis());
            record.put("sprintSeconds", sprint);
            record.put("recoverySeconds", recovery);
            record.put("repetitions", reps);
            record.put("elapsedSeconds", elapsed);
            record.put("integrityVersion", 1);
            record.put("integrityHash", hash(record));
            sessions.put(record);
            write(context, KEY_FARTLEK, sessions);
        } catch (JSONException ignored) {
        }
    }

    public static synchronized void saveRun(
            Context context,
            double metres,
            int elapsed,
            double pace,
            JSONArray route
    ) {
        saveRun(
                context,
                metres,
                elapsed,
                pace,
                route,
                new JSONArray(),
                false,
                400,
                0,
                new JSONArray()
        );
    }

    public static synchronized void saveRun(
            Context context,
            double metres,
            int elapsed,
            double pace,
            JSONArray route,
            JSONArray laps,
            boolean trackMode,
            double trackLength,
            int lapCount,
            JSONArray distanceSplits
    ) {
        JSONArray sessions = read(context, KEY_RUNS);
        JSONObject record = new JSONObject();
        try {
            record.put("type", "RUN");
            record.put("savedAt", System.currentTimeMillis());
            record.put("distanceMeters", metres);
            record.put("elapsedSeconds", elapsed);
            record.put("averagePaceSecondsPerKm", pace);
            record.put("route", route == null ? new JSONArray() : route);
            record.put("trackMode", trackMode);
            record.put("trackLengthMeters", trackLength);
            record.put("lapCount", lapCount);
            record.put("lapSplits", laps == null ? new JSONArray() : laps);
            record.put(
                    "kilometreSplits",
                    distanceSplits == null ? new JSONArray() : distanceSplits
            );
            record.put("integrityVersion", 1);
            record.put("integrityHash", hash(record));
            sessions.put(record);
            write(context, KEY_RUNS, sessions);
        } catch (JSONException ignored) {
        }
    }

    public static synchronized JSONArray getFartlekSessions(Context context) {
        return read(context, KEY_FARTLEK);
    }

    public static synchronized JSONArray getRunSessions(Context context) {
        return read(context, KEY_RUNS);
    }

    public static synchronized boolean verifyRun(JSONObject record) {
        if (record == null) {
            return false;
        }

        String saved = record.optString("integrityHash", "");
        if (saved.isEmpty()) {
            return false;
        }

        try {
            JSONObject copy = new JSONObject(record.toString());
            copy.remove("integrityHash");
            return saved.equals(hash(copy));
        } catch (JSONException e) {
            return false;
        }
    }

    private static String hash(JSONObject record) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(
                    record.toString().getBytes(StandardCharsets.UTF_8)
            );
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) {
                out.append(String.format(java.util.Locale.US, "%02x", b));
            }
            return out.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
