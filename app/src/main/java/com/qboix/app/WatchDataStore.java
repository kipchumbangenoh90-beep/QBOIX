package com.qboix.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class WatchDataStore {

    private static final String PREFS = "qboix_watch_live";

    private WatchDataStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void setConnected(Context c, boolean value) {
        prefs(c).edit()
                .putBoolean("connected", value)
                .putLong("updated", System.currentTimeMillis())
                .apply();
    }

    public static boolean isConnected(Context c) {
        return prefs(c).getBoolean("connected", false);
    }

    public static void put(Context c, String metric, int value, String source) {
        prefs(c).edit()
                .putInt(metric, value)
                .putString(metric + "_source", source)
                .putLong(metric + "_time", System.currentTimeMillis())
                .apply();
    }

    /** Returns the stored value, or -1 if none exists for the current session. */
    public static int get(Context c, String metric) {
        return prefs(c).getInt(metric, -1);
    }

    public static String source(Context c, String metric) {
        return prefs(c).getString(metric + "_source", "");
    }

    /** Removes every metric so values from an earlier session are never shown as current. */
    public static void clearMetrics(Context c) {
        SharedPreferences.Editor e = prefs(c).edit();
        for (String m : new String[]{"heart", "spo2", "steps", "calories"}) {
            e.remove(m).remove(m + "_source").remove(m + "_time");
        }
        e.apply();
    }
}