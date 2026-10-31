package com.qboix.app;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class GpsRunActivity extends Activity {
    private final int GOLD=Color.rgb(215,181,91), BG=Color.rgb(9,9,13), CARD=Color.rgb(27,27,34), GREY=Color.rgb(165,165,175), BLUE=Color.rgb(52,136,255);
    private TextView distance,time,pace,speed,calories,lapStatus,status,activityTitle;
    private Button start,pause,finish;
    private Spinner intervalChoice,modeChoice,trackLengthChoice;
    private EditText customMetres;
    private Switch voiceSwitch,alertsSwitch;
    private boolean active,paused,registered,voiceEnabled=true,alertsEnabled=true,settingsOpen;
    private String activityType="RUN";
    private WebView mapWeb;
    private RouteCanvas fallback;
    private FrameLayout mapFrame;
    private final double[] intervals={100,200,400,500,1000,804.672,1609.344,0};
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            double metres=i.getDoubleExtra("distance",0),p=i.getDoubleExtra("pace",0);
            int sec=i.getIntExtra("seconds",0);
            active=i.getBooleanExtra("active",active); paused=i.getBooleanExtra("paused",paused);
            distance.setText(String.format(Locale.getDefault(),"%.2f",metres/1000));
            time.setText(clock(sec)); pace.setText(p>0?clock((int)p):"--:--");
            speed.setText(p>0?String.format(Locale.getDefault(),"%.1f",3600.0/p):"0.0");
            calories.setText(String.format(Locale.getDefault(),"%.0f",metres/1000.0*60.0));
            int laps=i.getIntExtra("lap_count",-1); if(laps>=0)lapStatus.setText("LAPS  "+laps);
            String msg=i.getStringExtra("message"); if(msg!=null)status.setText(msg);
            start.setEnabled(!active); start.setText(active?"RECORDING":"START "+activityType);
            pause.setEnabled(active); pause.setText(paused?"RESUME":"PAUSE");
            finish.setEnabled(active);
            String route=i.getStringExtra("route_json");
            if(route!=null) updateRoute(route);
            if(msg!=null&&msg.contains("saved"))Toast.makeText(GpsRunActivity.this,"Activity saved offline",Toast.LENGTH_LONG).show();
        }
    };

    @Override public void onCreate(Bundle b){super.onCreate(b);activityType=getIntent().getStringExtra("activity_type");if(activityType==null)activityType="RUN";buildScreen();}
    private void buildScreen(){
        FrameLayout root=new FrameLayout(this); root.setBackgroundColor(BG);
        LinearLayout base=new LinearLayout(this);base.setOrientation(LinearLayout.VERTICAL);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(16),dp(8),dp(16),dp(8));
        TextView brand=label("Q-BOIX",GOLD,20,true);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        activityTitle=label("GPS "+activityType,Color.WHITE,18,true);header.addView(activityTitle);
        base.addView(header,new LinearLayout.LayoutParams(-1,dp(48)));
        mapFrame=new FrameLayout(this);mapFrame.setBackgroundColor(Color.WHITE);
        fallback=new RouteCanvas(this);mapFrame.addView(fallback,new FrameLayout.LayoutParams(-1,-1));
        mapWeb=new WebView(this);mapWeb.setBackgroundColor(Color.WHITE);mapWeb.getSettings().setJavaScriptEnabled(true);mapWeb.getSettings().setDomStorageEnabled(true);
        mapWeb.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String u){mapWeb.setVisibility(View.VISIBLE);fallback.setVisibility(View.GONE);status.setText("Online map ready");}
            @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError err){if(req.isForMainFrame()){mapWeb.setVisibility(View.GONE);fallback.setVisibility(View.VISIBLE);status.setText("Offline map • drawing GPS route");}}
        });
        mapFrame.addView(mapWeb,new FrameLayout.LayoutParams(-1,-1));
        mapWeb.setVisibility(View.INVISIBLE);fallback.setVisibility(View.VISIBLE);
        mapWeb.loadDataWithBaseURL("https://qboix.local/",mapHtml(),"text/html","UTF-8",null);
        base.addView(mapFrame,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout live=new LinearLayout(this);live.setOrientation(LinearLayout.VERTICAL);live.setPadding(dp(12),dp(10),dp(12),dp(8));live.setBackground(panel(BG,18));
        status=label("Waiting for GPS",GREY,11,false);live.addView(status);
        LinearLayout row1=new LinearLayout(this);row1.setOrientation(LinearLayout.HORIZONTAL);
        distance=metric(row1,"0.00","KM");time=metric(row1,"00:00","TIME");pace=metric(row1,"--:--","AVG PACE / KM");
        live.addView(row1,new LinearLayout.LayoutParams(-1,dp(76)));
        LinearLayout row2=new LinearLayout(this);row2.setOrientation(LinearLayout.HORIZONTAL);
        speed=metric(row2,"0.0","KM/H");calories=metric(row2,"0","KCAL");lapStatus=metric(row2,"0","LAPS");
        live.addView(row2,new LinearLayout.LayoutParams(-1,dp(62)));
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setGravity(Gravity.CENTER);
        start=button("START "+activityType,Color.rgb(55,190,125));pause=button("PAUSE",GOLD);finish=button("FINISH",Color.rgb(220,100,85));
        pause.setEnabled(false);finish.setEnabled(false);
        controls.addView(start,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(pause,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(finish,new LinearLayout.LayoutParams(0,dp(48),1));
        live.addView(controls);base.addView(live,new LinearLayout.LayoutParams(-1,-2));
        TextView swipe=label("⌃  SWIPE UP FOR WORKOUT SETTINGS",GOLD,11,true);swipe.setGravity(Gravity.CENTER);swipe.setPadding(0,dp(9),0,dp(8));base.addView(swipe);
        root.addView(base,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout drawer=buildSettings();drawer.setVisibility(View.GONE);
        FrameLayout.LayoutParams dp=new FrameLayout.LayoutParams(-1,dp(380),Gravity.BOTTOM);root.addView(drawer,dp);
        View dim=new View(this);dim.setBackgroundColor(0x99000000);dim.setVisibility(View.GONE);
        root.addView(dim,new FrameLayout.LayoutParams(-1,-1));root.removeView(dim);root.addView(dim,new FrameLayout.LayoutParams(-1,-1));dim.setOnClickListener(v->{drawer.setVisibility(View.GONE);dim.setVisibility(View.GONE);settingsOpen=false;});
        swipe.setOnClickListener(v->toggleSettings(drawer,dim));
        root.setOnTouchListener(new View.OnTouchListener(){float y;public boolean onTouch(View v,android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){y=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){float dy=e.getY()-y;if(dy < -45)toggleSettings(drawer,dim);else if(dy>45&&settingsOpen)toggleSettings(drawer,dim);return true;}return true;}});
        start.setOnClickListener(v->requestStart());pause.setOnClickListener(v->command(paused?GpsTrackingService.RESUME:GpsTrackingService.PAUSE));finish.setOnClickListener(v->command(GpsTrackingService.FINISH));
        setContentView(root);
    }

    private LinearLayout buildSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(15),dp(18),dp(18));box.setBackground(panel(Color.rgb(20,21,28),22));
        TextView title=label("WORKOUT SETTINGS",GOLD,17,true);box.addView(title);
        modeChoice=new Spinner(this);modeChoice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Road / distance","Track workout","Cross-country"}));addSetting(box,"WORKOUT MODE",modeChoice);
        trackLengthChoice=new Spinner(this);trackLengthChoice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"100 m","200 m","300 m","400 m","Custom track"}));addSetting(box,"TRACK LENGTH",trackLengthChoice);
        intervalChoice=new Spinner(this);intervalChoice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"100 m","200 m","400 m","500 m","1 km","Half mile","1 mile","Custom metres"}));intervalChoice.setSelection(4);addSetting(box,"VOICE ALERT DISTANCE",intervalChoice);
        customMetres=new EditText(this);customMetres.setSingleLine(true);customMetres.setInputType(2);customMetres.setHint("Custom distance in metres");customMetres.setTextColor(Color.WHITE);customMetres.setHintTextColor(GREY);box.addView(customMetres);
        voiceSwitch=new Switch(this);voiceSwitch.setText("Voice assistant");voiceSwitch.setTextColor(Color.WHITE);voiceSwitch.setChecked(true);box.addView(voiceSwitch);
        alertsSwitch=new Switch(this);alertsSwitch.setText("Distance voice alerts");alertsSwitch.setTextColor(Color.WHITE);alertsSwitch.setChecked(true);box.addView(alertsSwitch);
        TextView note=label("Settings apply when the next activity starts.",GREY,10,false);box.addView(note);
        return box;
    }
    private void addSetting(LinearLayout box,String title,Spinner spinner){box.addView(label(title,GOLD,10,true));box.addView(spinner,new LinearLayout.LayoutParams(-1,dp(42)));}
    private void toggleSettings(View drawer,View dim){settingsOpen=!settingsOpen;drawer.setVisibility(settingsOpen?View.VISIBLE:View.GONE);dim.setVisibility(settingsOpen?View.VISIBLE:View.GONE);drawer.bringToFront();dim.bringToFront();if(settingsOpen)drawer.bringToFront();}
    private String mapHtml(){return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#map{height:100%;margin:0;background:#fff}</style></head><body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>var map=L.map('map').setView([0.5143,35.2698],15);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);var line=L.polyline([],{color:'#3488ff',weight:5}).addTo(map);var marker=null;function setRoute(a){var pts=a.map(p=>[p.lat,p.lon]);line.setLatLngs(pts);if(pts.length){if(marker)marker.setLatLng(pts[pts.length-1]);else marker=L.circleMarker(pts[pts.length-1],{radius:7,color:'#fff',weight:3,fillColor:'#3488ff',fillOpacity:1}).addTo(map);map.panTo(pts[pts.length-1]);}}</script></body></html>";}
    private void updateRoute(String json){try{JSONArray a=new JSONArray(json);fallback.setRoute(a);if(mapWeb.getVisibility()==View.VISIBLE)mapWeb.evaluateJavascript("setRoute("+json+")",null);}catch(Exception ignored){}}
    private void requestStart(){double interval=selectedInterval();if(interval<0)return;if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},41);return;}command(GpsTrackingService.START,interval);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==41){if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)command(GpsTrackingService.START,selectedInterval());else status.setText("Location permission is required.");}}
    private double selectedInterval(){int pos=intervalChoice.getSelectedItemPosition();if(pos==7){try{double n=Double.parseDouble(customMetres.getText().toString());if(n>=50&&n<=100000)return n;}catch(Exception ignored){}Toast.makeText(this,"Enter a distance from 50 to 100000 metres.",Toast.LENGTH_LONG).show();return -1;}return intervals[pos];}
    private double selectedTrack(){int p=trackLengthChoice.getSelectedItemPosition();if(p<4)return new double[]{100,200,300,400}[p];try{return Double.parseDouble(customMetres.getText().toString());}catch(Exception e){return 400;}}
    private void command(String action){command(action,1000);}
    private void command(String action,double interval){Intent i=new Intent(this,GpsTrackingService.class);i.setAction(action);if(GpsTrackingService.START.equals(action)){i.putExtra(GpsTrackingService.EXTRA_INTERVAL,interval);i.putExtra(GpsTrackingService.EXTRA_TRACK_MODE,modeChoice.getSelectedItemPosition()==1);i.putExtra(GpsTrackingService.EXTRA_TRACK_LENGTH,selectedTrack());i.putExtra(GpsTrackingService.EXTRA_VOICE_ENABLED,voiceSwitch.isChecked()&&alertsSwitch.isChecked());}if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
    @Override protected void onStart(){super.onStart();IntentFilter f=new IntentFilter(GpsTrackingService.ACTION);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);registered=true;}
    @Override protected void onStop(){if(registered){unregisterReceiver(receiver);registered=false;}super.onStop();}
    @Override protected void onDestroy(){if(mapWeb!=null)mapWeb.destroy();super.onDestroy();}
    private TextView metric(LinearLayout row,String value,String caption){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setGravity(Gravity.CENTER);TextView v=label(value,GOLD,19,true);TextView t=label(caption,GREY,9,true);c.addView(v);c.addView(t);row.addView(c,new LinearLayout.LayoutParams(0,-1,1));return v;}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setTextColor(Color.BLACK);b.setTypeface(null,1);b.setBackground(panel(color,14));return b;}
    private TextView label(String s,int c,float size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(c);t.setTextSize(size);if(bold)t.setTypeface(null,1);return t;}
    private GradientDrawable panel(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String clock(int s){return String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60);}

    private class RouteCanvas extends View{
        private JSONArray route=new JSONArray();private final Paint line=new Paint(3),dot=new Paint(3);
        RouteCanvas(Context c){super(c);setBackgroundColor(Color.WHITE);line.setColor(BLUE);line.setStrokeWidth(dp(4));line.setStyle(Paint.Style.STROKE);line.setStrokeCap(Paint.Cap.ROUND);line.setStrokeJoin(Paint.Join.ROUND);dot.setColor(BLUE);}
        void setRoute(JSONArray a){route=a;postInvalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(route.length()==0){Paint p=new Paint(3);p.setColor(Color.GRAY);p.setTextSize(dp(14));c.drawText("GPS route will appear here",dp(24),dp(40),p);return;}double minLat=90,maxLat=-90,minLon=180,maxLon=-180;for(int i=0;i<route.length();i++){JSONObject o=route.optJSONObject(i);if(o==null)continue;double la=o.optDouble("lat"),lo=o.optDouble("lon");minLat=Math.min(minLat,la);maxLat=Math.max(maxLat,la);minLon=Math.min(minLon,lo);maxLon=Math.max(maxLon,lo);}float pad=dp(35),w=getWidth()-2*pad,h=getHeight()-2*pad;Path path=new Path();for(int i=0;i<route.length();i++){JSONObject o=route.optJSONObject(i);if(o==null)continue;float x=pad+(float)((o.optDouble("lon")-minLon)/Math.max(0.000001,maxLon-minLon))*w;float y=getHeight()-pad-(float)((o.optDouble("lat")-minLat)/Math.max(0.000001,maxLat-minLat))*h;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}c.drawPath(path,line);JSONObject last=route.optJSONObject(route.length()-1);if(last!=null){float x=pad+(float)((last.optDouble("lon")-minLon)/Math.max(0.000001,maxLon-minLon))*w;float y=getHeight()-pad-(float)((last.optDouble("lat")-minLat)/Math.max(0.000001,maxLat-minLat))*h;c.drawCircle(x,y,dp(7),dot);}}
    }
}