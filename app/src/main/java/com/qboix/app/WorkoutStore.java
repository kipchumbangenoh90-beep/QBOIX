package com.qboix.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;\nimport java.security.MessageDigest;\nimport java.nio.charset.StandardCharsets;
public final class WorkoutStore {
 private static final String PREFS="qboix_workout_store", KEY_FARTLEK="fartlek_sessions", KEY_RUNS="gps_run_sessions";
 private WorkoutStore(){}
 private static JSONArray read(SharedPreferences p,String key){try{return new JSONArray(p.getString(key,"[]"));}catch(JSONException e){return new JSONArray();}}
 public static synchronized void saveFartlek(Context c,int sprint,int recovery,int reps,int elapsed){
  SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE); JSONArray a=read(p,KEY_FARTLEK); JSONObject o=new JSONObject();
  try{o.put("type","FARTLEK");o.put("savedAt",System.currentTimeMillis());o.put("sprintSeconds",sprint);o.put("recoverySeconds",recovery);o.put("repetitions",reps);o.put("elapsedSeconds",elapsed);a.put(o);p.edit().putString(KEY_FARTLEK,a.toString()).apply();}catch(JSONException ignored){}
 }
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route){saveRun(c,metres,elapsed,pace,route,new JSONArray(),false,400,0);}
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route,JSONArray laps,boolean trackMode,double trackLength,int lapCount){
  SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);JSONArray a=read(p,KEY_RUNS);JSONObject o=new JSONObject();
  try{o.put("type","RUN");o.put("savedAt",System.currentTimeMillis());o.put("distanceMeters",metres);o.put("elapsedSeconds",elapsed);o.put("averagePaceSecondsPerKm",pace);o.put("route",route==null?new JSONArray():route);o.put("trackMode",trackMode);o.put("trackLengthMeters",trackLength);o.put("lapCount",lapCount);o.put("lapSplits",laps==null?new JSONArray():laps);o.put("integrityVersion",1);o.put("integrityHash",hash(o));a.put(o);p.edit().putString(KEY_RUNS,a.toString()).apply();}catch(JSONException ignored){}
 }
 public static synchronized boolean verifyRun(JSONObject record){if(record==null)return false;String saved=record.optString("integrityHash","");if(saved.isEmpty())return false;JSONObject copy=new JSONObject(record.toString());copy.remove("integrityHash");return saved.equals(hash(copy));}\n private static String hash(JSONObject record){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] bytes=md.digest(record.toString().getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b));return out.toString();}catch(Exception e){return "";}}\n public static JSONArray getFartlekSessions(Context c){return read(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),KEY_FARTLEK);}
 public static JSONArray getRunSessions(Context c){return read(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),KEY_RUNS);}
}