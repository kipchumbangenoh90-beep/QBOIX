package com.qboix.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores only values actually received and decoded from the connected watch.
 * Unsupported metrics remain -1 and are displayed as unavailable.
 */
public final class WatchDataStore {
    private static final String PREFS = "qboix_watch_measurements";
    private static final String KEY_CONNECTED = "measurement_stream_active";

    private WatchDataStore() { }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int get(Context context, String metric) {
        return prefs(context).getInt(metric, -1);
    }

    public static void saveMeasurement(Context context, String metric, int value) {
        if (value < 0) return;
        if ("heart".equals(metric) && (value < 1 || value > 260)) return;
        if ("spo2".equals(metric) && (value < 0 || value > 100)) return;
        if (!"heart".equals(metric) && !"spo2".equals(metric)
                && !"steps".equals(metric) && !"calories".equals(metric)) return;
        prefs(context).edit().putInt(metric, value).apply();
    }

    public static boolean isConnected(Context context) {
        return prefs(context).getBoolean(KEY_CONNECTED, false);
    }

    public static void setConnected(Context context, boolean connected) {
        prefs(context).edit().putBoolean(KEY_CONNECTED, connected).apply();
    }
}
