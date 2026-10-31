package com.qboix.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public final class WorkoutStore {
 private static final String PREFS="qboix_workout_store", KEY_FARTLEK="fartlek_sessions", KEY_RUNS="gps_run_sessions";
 private WorkoutStore(){}
 private static JSONArray read(SharedPreferences p,String key){try{return new JSONArray(p.getString(key,"[]"));}catch(JSONException e){return new JSONArray();}}
 public static synchronized void saveFartlek(Context c,int sprint,int recovery,int reps,int elapsed){
  SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE); JSONArray a=read(p,KEY_FARTLEK); JSONObject o=new JSONObject();
  try{o.put("type","FARTLEK");o.put("savedAt",System.currentTimeMillis());o.put("sprintSeconds",sprint);o.put("recoverySeconds",recovery);o.put("repetitions",reps);o.put("elapsedSeconds",elapsed);a.put(o);p.edit().putString(KEY_FARTLEK,a.toString()).apply();}catch(JSONException ignored){}
 }
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route){
  SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);JSONArray a=read(p,KEY_RUNS);JSONObject o=new JSONObject();
  try{o.put("type","RUN");o.put("savedAt",System.currentTimeMillis());o.put("distanceMeters",metres);o.put("elapsedSeconds",elapsed);o.put("averagePaceSecondsPerKm",pace);o.put("route",route==null?new JSONArray():route);a.put(o);p.edit().putString(KEY_RUNS,a.toString()).apply();}catch(JSONException ignored){}
 }
 public static JSONArray getFartlekSessions(Context c){return read(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),KEY_FARTLEK);}
 public static JSONArray getRunSessions(Context c){return read(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),KEY_RUNS);}
}