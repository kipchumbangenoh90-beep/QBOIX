package com.qboix.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
public final class WorkoutStore {
 private static final String PREFS="qboix_workout_store", KEY_FARTLEK="fartlek_sessions", KEY_RUNS="gps_run_sessions";
 private WorkoutStore(){}
 private static JSONArray read(SharedPreferences p,String key){try{return new JSONArray(p.getString(key,"[]"));}catch(JSONException e){return new JSONArray();}}
 public static synchronized void saveFartlek(Context c,int sprint,int recovery,int reps,int elapsed){saveFartlek(c,sprint,recovery,reps,elapsed,System.currentTimeMillis()-elapsed*1000L,System.currentTimeMillis());}
 public static synchronized void saveFartlek(Context c,int sprint,int recovery,int reps,int elapsed,long startedAt,long endedAt){
  SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE); JSONArray a=read(p,KEY_FARTLEK); JSONObject o=new JSONObject();
  try{o.put("type","FARTLEK");o.put("activityType","FARTLEK");o.put("startedAt",startedAt);o.put("endedAt",endedAt);o.put("savedAt",endedAt);o.put("sprintSeconds",sprint);o.put("recoverySeconds",recovery);o.put("repetitions",reps);o.put("elapsedSeconds",elapsed);o.put("distanceMeters",JSONObject.NULL);o.put("calories",JSONObject.NULL);o.put("integrityVersion",1);o.put("integrityHash",hash(o));a.put(o);p.edit().putString(KEY_FARTLEK,a.toString()).apply();}catch(JSONException ignored){}
 }
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route){saveRun(c,metres,elapsed,pace,route,new JSONArray(),false,400,0,new JSONArray(),"RUN",System.currentTimeMillis()-elapsed*1000L,System.currentTimeMillis());}
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route,JSONArray laps,boolean trackMode,double trackLength,int lapCount,JSONArray distanceSplits){saveRun(c,metres,elapsed,pace,route,laps,trackMode,trackLength,lapCount,distanceSplits,"RUN",System.currentTimeMillis()-elapsed*1000L,System.currentTimeMillis());}
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route,JSONArray laps,boolean trackMode,double trackLength,int lapCount,JSONArray distanceSplits,String activityType,long startedAt,long endedAt){saveRun(c,metres,elapsed,pace,route,laps,trackMode,trackLength,lapCount,distanceSplits,activityType,startedAt,endedAt,1000);}
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route,JSONArray laps,boolean trackMode,double trackLength,int lapCount,JSONArray distanceSplits,String activityType,long startedAt,long endedAt,double alertDistanceMeters){
  SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);JSONArray a=read(p,KEY_RUNS);JSONObject o=new JSONObject();
  try{o.put("type",activityType);o.put("activityType",activityType);o.put("startedAt",startedAt);o.put("endedAt",endedAt);o.put("savedAt",endedAt);o.put("distanceMeters",metres);o.put("distanceKm",metres/1000.0);o.put("distanceMiles",metres/1609.344);o.put("elapsedSeconds",elapsed);o.put("averagePaceSecondsPerKm",pace);o.put("averageSpeedKmh",elapsed>0?(metres/1000.0)/(elapsed/3600.0):0);o.put("alertDistanceMeters",alertDistanceMeters);o.put("calories",JSONObject.NULL);o.put("route",route==null?new JSONArray():route);o.put("trackMode",trackMode);o.put("trackLengthMeters",trackLength);o.put("lapCount",lapCount);o.put("lapSplits",laps==null?new JSONArray():laps);o.put("kilometreSplits",distanceSplits==null?new JSONArray():distanceSplits);o.put("integrityVersion",1);o.put("integrityHash",hash(o));a.put(o);p.edit().putString(KEY_RUNS,a.toString()).apply();}catch(JSONException ignored){}
 }
 public static synchronized boolean verifyRun(JSONObject record){if(record==null)return false;String saved=record.optString("integrityHash","");if(saved.isEmpty())return false;try{JSONObject copy=new JSONObject(record.toString());copy.remove("integrityHash");return saved.equals(hash(copy));}catch(JSONException e){return false;}}
 private static String hash(JSONObject record){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] bytes=md.digest(record.toString().getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b));return out.toString();}catch(Exception e){return "";}}
 public static JSONArray getFartlekSessions(Context c){return read(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),KEY_FARTLEK);}
 public static JSONArray getRunSessions(Context c){return read(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),KEY_RUNS);}
}