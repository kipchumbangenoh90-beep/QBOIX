package com.qboix.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class WorkoutStore {
    private static final String PREFS = "qboix_workout_store";
    private static final String KEY_FARTLEK = "fartlek_sessions";

    private WorkoutStore() { }

    public static synchronized void saveFartlek(Context context, int sprintSeconds,
            int recoverySeconds, int repetitions, int elapsedSeconds) {
        SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        JSONArray sessions;
        try {
            sessions=new JSONArray(prefs.getString(KEY_FARTLEK,"[]"));
        } catch(JSONException e) {
            sessions=new JSONArray();
        }
        JSONObject item=new JSONObject();
        try {
            item.put("type","FARTLEK");
            item.put("savedAt",System.currentTimeMillis());
            item.put("sprintSeconds",sprintSeconds);
            item.put("recoverySeconds",recoverySeconds);
            item.put("repetitions",repetitions);
            item.put("elapsedSeconds",elapsedSeconds);
            sessions.put(item);
            prefs.edit().putString(KEY_FARTLEK,sessions.toString()).apply();
        } catch(JSONException ignored) { }
    }

    public static JSONArray getFartlekSessions(Context context) {
        SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        try {
            return new JSONArray(prefs.getString(KEY_FARTLEK,"[]"));
        } catch(JSONException e) {
            return new JSONArray();
        }
    }
}
