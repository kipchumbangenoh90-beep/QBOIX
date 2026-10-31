package com.qboix.app;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;
public class GpsRunActivity extends Activity implements LocationListener {
 private final int GOLD=Color.rgb(215,181,91),BG=Color.rgb(9,9,13),CARD=Color.rgb(27,27,34),GREY=Color.rgb(165,165,175);
 private LocationManager manager; private final Handler handler=new Handler(Looper.getMainLooper()); private final ArrayList<Location> points=new ArrayList<>();
 private TextView distance,time,pace,status; private Button start,pause,finish; private boolean active=false,paused=false,saved=false; private long started,pausedMs,pauseAt; private double metres; private Location previous;
 private final Runnable ticker=new Runnable(){public void run(){refresh();if(active&&!paused)handler.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);manager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);ui();}
 private void ui(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(22),dp(25),dp(22),dp(22));root.setBackgroundColor(BG);
  root.addView(label("Q-BOIX",GOLD,26,true));TextView heading=label("GPS RUN",Color.WHITE,23,true);LinearLayout.LayoutParams h=new LinearLayout.LayoutParams(-1,-2);h.topMargin=dp(20);root.addView(heading,h);
  status=label("Ready. GPS recording works without mobile data.",GREY,13,false);LinearLayout.LayoutParams s=new LinearLayout.LayoutParams(-1,-2);s.topMargin=dp(8);root.addView(status,s);
  LinearLayout stats=new LinearLayout(this);stats.setOrientation(1);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1);sp.topMargin=dp(22);sp.bottomMargin=dp(15);root.addView(stats,sp);
  distance=metric(stats,"0.00","KILOMETRES");time=metric(stats,"00:00","ELAPSED TIME");pace=metric(stats,"--:--","AVG PACE / KM");
  start=button("START RUN",Color.rgb(55,190,125));root.addView(start,buttonParams());
  pause=button("PAUSE",GOLD);pause.setEnabled(false);LinearLayout.LayoutParams pp=buttonParams();pp.topMargin=dp(9);root.addView(pause,pp);
  finish=button("FINISH & SAVE",Color.rgb(220,100,85));finish.setEnabled(false);LinearLayout.LayoutParams fp=buttonParams();fp.topMargin=dp(9);root.addView(finish,fp);
  start.setOnClickListener(v->requestStart());pause.setOnClickListener(v->{if(paused)resume();else pause();});finish.setOnClickListener(v->finishRun());setContentView(root);
 }
 private TextView metric(LinearLayout parent,String value,String caption){
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setGravity(Gravity.CENTER);box.setBackground(panel(CARD));TextView n=label(value,GOLD,31,true);n.setGravity(Gravity.CENTER);box.addView(n);TextView c=label(caption,GREY,11,true);c.setGravity(Gravity.CENTER);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(5);box.addView(c,cp);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,1);p.bottomMargin=dp(8);parent.addView(box,p);return n;
 }
 private void requestStart(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},41);return;}startRun();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==41){if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startRun();else status.setText("Location permission is required to record your run.");}}
 private void startRun(){
  try{if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){status.setText("Turn on Location/GPS, then tap START RUN.");return;}
   active=true;paused=false;started=SystemClock.elapsedRealtime();start.setEnabled(false);pause.setEnabled(true);finish.setEnabled(true);status.setText("Searching for GPS. Move outdoors for a clear signal.");
   manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this,Looper.getMainLooper());Location last=manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);if(last!=null)onLocationChanged(last);handler.post(ticker);
  }catch(Exception e){status.setText("Could not start GPS. Check Location permission and settings.");}
 }
 private void pause(){if(!active||paused)return;paused=true;pauseAt=SystemClock.elapsedRealtime();manager.removeUpdates(this);handler.removeCallbacks(ticker);pause.setText("RESUME");status.setText("Paused. Distance is not being counted.");}
 private void resume(){if(!active||!paused)return;pausedMs+=SystemClock.elapsedRealtime()-pauseAt;paused=false;try{manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this,Looper.getMainLooper());pause.setText("PAUSE");status.setText("GPS recording.");handler.post(ticker);}catch(SecurityException e){status.setText("Location permission needed to resume.");}}
 private void finishRun(){
  if(!active||saved)return;active=false;saved=true;manager.removeUpdates(this);handler.removeCallbacks(ticker);long end=paused?pauseAt:SystemClock.elapsedRealtime();int seconds=(int)Math.max(1,(end-started-pausedMs)/1000);double avg=metres>0?seconds/(metres/1000):0;
  JSONArray route=new JSONArray();for(Location p:points){JSONObject o=new JSONObject();try{o.put("lat",p.getLatitude());o.put("lon",p.getLongitude());o.put("accuracy",p.getAccuracy());route.put(o);}catch(Exception ignored){}}
  WorkoutStore.saveRun(this,metres,seconds,avg,route);status.setText("Saved offline • "+points.size()+" GPS route points");start.setEnabled(true);start.setText("NEW RUN");pause.setEnabled(false);finish.setEnabled(false);Toast.makeText(this,"Q-BOIX run saved",Toast.LENGTH_LONG).show();
 }
 @Override public void onLocationChanged(Location l){
  if(!active||paused||saved||l==null)return;if(!l.hasAccuracy()||l.getAccuracy()>35){status.setText("GPS accuracy is weak. Waiting for a better signal.");return;}
  if(previous!=null){float d=previous.distanceTo(l);long sec=Math.max(1,(l.getTime()-previous.getTime())/1000);if(d<1.5)return;if(d/sec>8.5){status.setText("Unreliable GPS jump ignored.");return;}metres+=d;}
  previous=new Location(l);points.add(new Location(l));status.setText("GPS recording • accuracy ±"+Math.round(l.getAccuracy())+" m");refresh();
 }
 private void refresh(){long now=paused?pauseAt:SystemClock.elapsedRealtime();int sec=(int)Math.max(0,(now-started-pausedMs)/1000);distance.setText(String.format(Locale.getDefault(),"%.2f",metres/1000));time.setText(clock(sec));pace.setText(metres>=20?clock((int)(sec/(metres/1000))):"--:--");}
 private String clock(int s){return String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60);}
 @Override protected void onPause(){super.onPause();if(active&&!paused){manager.removeUpdates(this);handler.removeCallbacks(ticker);status.setText("GPS paused while you leave this screen in this version.");}}
 @Override protected void onResume(){super.onResume();if(active&&!paused&&!saved){try{manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this,Looper.getMainLooper());handler.post(ticker);}catch(SecurityException ignored){}}}
 @Override protected void onDestroy(){manager.removeUpdates(this);handler.removeCallbacks(ticker);super.onDestroy();}
 private Button button(String t,int color){Button b=new Button(this);b.setText(t);b.setTextColor(Color.BLACK);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(panel(color));return b;}
 private LinearLayout.LayoutParams buttonParams(){return new LinearLayout.LayoutParams(-1,dp(54));}
 private TextView label(String t,int c,float size,boolean bold){TextView v=new TextView(this);v.setText(t);v.setTextColor(c);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
 private GradientDrawable panel(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(14));return d;}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}