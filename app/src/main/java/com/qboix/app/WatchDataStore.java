package com.qboix.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class WatchDataStore {
    private static final String PREFS = "qboix_watch_live";
    private WatchDataStore() {}
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static void setConnected(Context c, boolean value) { prefs(c).edit().putBoolean("connected", value).putLong("updated", System.currentTimeMillis()).apply(); }
    public static boolean isConnected(Context c) { return prefs(c).getBoolean("connected", false); }
    public static void put(Context c, String metric, int value, String source) { prefs(c).edit().putInt(metric, value).putString(metric+"_source", source).putLong(metric+"_time", System.currentTimeMillis()).apply(); }
    public static int get(Context c, String metric) { return prefs(c).getInt(metric, -1); }
    public static String source(Context c, String metric) { return prefs(c).getString(metric+"_source", ""); }
}
