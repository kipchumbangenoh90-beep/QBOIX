
package com.qboix.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class WorkoutStore {
 private static final String KEY_FARTLEK="fartlek_sessions.json";
 private static final String KEY_RUNS="gps_run_sessions.json";
 private WorkoutStore(){}

 private static File userFolder(Context c){
  FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
  if(c==null||user==null||user.getUid()==null||user.getUid().isEmpty())return null;
  File folder=new File(new File(c.getFilesDir(),"users"),user.getUid());
  if(!folder.exists()&&!folder.mkdirs())return null;
  return folder;
 }
 private static JSONArray read(Context c,String name){
  File folder=userFolder(c);
  if(folder==null)return new JSONArray();
  File file=new File(folder,name);
  if(!file.isFile())return new JSONArray();
  try(FileInputStream input=new FileInputStream(file);ByteArrayOutputStream output=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int count;
   while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
   return new JSONArray(new String(output.toByteArray(),StandardCharsets.UTF_8));
  }catch(IOException|JSONException e){return new JSONArray();}
 }
 private static void write(Context c,String name,JSONArray data){
  File folder=userFolder(c);
  if(folder==null)return;
  File file=new File(folder,name);
  File temp=new File(folder,name+".tmp");
  try(FileOutputStream output=new FileOutputStream(temp)){
   output.write(data.toString().getBytes(StandardCharsets.UTF_8));
   output.flush();
   if(file.exists()&&!file.delete())return;
   if(!temp.renameTo(file))temp.delete();
  }catch(IOException ignored){temp.delete();}
 }
 public static String getPrivateFolderTitle(FirebaseUser user){
  if(user==null)return "Sign in to open your private folder";
  String name=user.getDisplayName();
  if(name==null||name.trim().isEmpty()){
   String email=user.getEmail();
   name=(email==null||email.trim().isEmpty())?"Personal":email.split("@")[0];
  }
  return name.trim()+"'s private workout folder";
 }
 public static synchronized void saveFartlek(Context c,int sprint,int recovery,int reps,int elapsed){
  JSONArray a=read(c,KEY_FARTLEK);JSONObject o=new JSONObject();
  try{o.put("type","FARTLEK");o.put("savedAt",System.currentTimeMillis());o.put("sprintSeconds",sprint);o.put("recoverySeconds",recovery);o.put("repetitions",reps);o.put("elapsedSeconds",elapsed);o.put("integrityVersion",1);o.put("integrityHash",hash(o));a.put(o);write(c,KEY_FARTLEK,a);}catch(JSONException ignored){}
 }
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route){saveRun(c,metres,elapsed,pace,route,new JSONArray(),false,400,0,new JSONArray());}
 public static synchronized void saveRun(Context c,double metres,int elapsed,double pace,JSONArray route,JSONArray laps,boolean trackMode,double trackLength,int lapCount,JSONArray distanceSplits){
  JSONArray a=read(c,KEY_RUNS);JSONObject o=new JSONObject();
  try{o.put("type","RUN");o.put("savedAt",System.currentTimeMillis());o.put("distanceMeters",metres);o.put("elapsedSeconds",elapsed);o.put("averagePaceSecondsPerKm",pace);o.put("route",route==null?new JSONArray():route);o.put("trackMode",trackMode);o.put("trackLengthMeters",trackLength);o.put("lapCount",lapCount);o.put("lapSplits",laps==null?new JSONArray():laps);o.put("kilometreSplits",distanceSplits==null?new JSONArray():distanceSplits);o.put("integrityVersion",1);o.put("integrityHash",hash(o));a.put(o);write(c,KEY_RUNS,a);}catch(JSONException ignored){}
 }
 public static synchronized boolean verifyRun(JSONObject record){if(record==null)return false;String saved=record.optString("integrityHash","");if(saved.isEmpty())return false;try{JSONObject copy=new JSONObject(record.toString());copy.remove("integrityHash");return saved.equals(hash(copy));}catch(JSONException e){return false;}}
 private static String hash(JSONObject record){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] bytes=md.digest(record.toString().getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b));return out.toString();}catch(Exception e){return "";}}
 public static JSONArray getFartlekSessions(Context c){return read(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),KEY_FARTLEK);}
 public static JSONArray getRunSessions(Context c){return read(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),KEY_RUNS);}
} public static JSONArray getFartlekSessions(Context c){return read(c,KEY_FARTLEK);}
 public static JSONArray getRunSessions(Context c){return read(c,KEY_RUNS);}
}