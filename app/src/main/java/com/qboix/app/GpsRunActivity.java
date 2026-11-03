package com.qboix.app;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
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
    private Spinner intervalChoice,modeChoice,trackLengthChoice,lapAlertChoice;
    private EditText customMetres;
    private Switch voiceSwitch,alertsSwitch;
    private boolean active,paused,registered,voiceEnabled=true,alertsEnabled=true,settingsOpen;
    private String activityType="RUN";
    private WebView mapWeb;
    private RouteCanvas fallback;
    private FrameLayout mapFrame;
    private final double[] intervals={100,200,400,500,1000,804.672,1609.344,0};
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LocationManager previewLocationManager;
    private boolean previewUpdatesRequested;
    private Location currentPhoneLocation;
    private final LocationListener previewLocationListener = new LocationListener() {
        @Override public void onLocationChanged(Location location) {
            if (location == null) return;
            currentPhoneLocation = new Location(location);
            if (fallback != null) fallback.setCurrentLocation(currentPhoneLocation);
            if (mapWeb != null && mapWeb.getVisibility() == View.VISIBLE) {
                mapWeb.evaluateJavascript("setUserLocation(" + location.getLatitude() + "," + location.getLongitude() + ")", null);
            }
            if (status != null && !active) {
                status.setText(String.format(Locale.getDefault(),
                    "GPS READY • ±%.0f m accuracy", location.getAccuracy()));
            }
        }
        @Override public void onStatusChanged(String provider, int state, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {
            if (status != null && !active) status.setText("GPS enabled • finding your position…");
        }
        @Override public void onProviderDisabled(String provider) {
            if (status != null && !active) status.setText("Turn on phone Location / GPS");
        }
    };
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

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        activityType=getIntent().getStringExtra("activity_type");
        if(activityType==null)activityType="RUN";
        buildScreen();
        prepareAutomaticLocation();
    }

    private void prepareAutomaticLocation() {
        previewLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Allow precise location so Q-BOIX can use the phone's built-in GPS");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 40);
            return;
        }
        startPreviewLocationUpdates();
    }

    // Offline positioning: use the phone's GNSS/GPS provider only.
    private void startPreviewLocationUpdates() {
        if (previewLocationManager == null || previewUpdatesRequested) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Precise location permission is required for GPS");
            return;
        }
        if (!previewLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            status.setText("Turn on Location / GPS on your phone");
            return;
        }
        try {
            Location last = previewLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) previewLocationListener.onLocationChanged(last);
            previewLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 0,
                    previewLocationListener, Looper.getMainLooper());
            previewUpdatesRequested = true;
            if (status != null && currentPhoneLocation == null) {
                status.setText("Finding satellites • GPS only • no internet required");
            }
        } catch (SecurityException e) {
            status.setText("Precise location permission is needed for GPS");
        }
    }
    private void buildScreen(){
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);

        // The map fills the entire screen; floating controls sit above it.
        mapFrame=new FrameLayout(this);
        mapFrame.setBackgroundColor(Color.WHITE);
        fallback=new RouteCanvas(this);
        mapFrame.addView(fallback,new FrameLayout.LayoutParams(-1,-1));
        mapWeb=new WebView(this);
        mapWeb.setBackgroundColor(Color.WHITE);
        mapWeb.getSettings().setJavaScriptEnabled(true);
        mapWeb.getSettings().setDomStorageEnabled(true);
        mapWeb.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String u){
                mapWeb.evaluateJavascript("typeof L !== 'undefined' ? 'ready' : 'offline'", result -> {
                    boolean ready = result != null && result.contains("ready");
                    mapWeb.setVisibility(ready ? View.VISIBLE : View.GONE);
                    fallback.setVisibility(ready ? View.GONE : View.VISIBLE);
                    if (ready && currentPhoneLocation != null) {
                        mapWeb.evaluateJavascript("setUserLocation(" + currentPhoneLocation.getLatitude() + "," + currentPhoneLocation.getLongitude() + ")", null);
                    }
                    if(status!=null && !active)status.setText(ready ? "Online map • GPS location active" : "Offline mode • GPS route display active");
                });
            }
            @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError err){
                if(req.isForMainFrame()){
                    mapWeb.setVisibility(View.GONE);
                    fallback.setVisibility(View.VISIBLE);
                    if(status!=null)status.setText("Offline map • drawing GPS route");
                }
            }
        });
        mapFrame.addView(mapWeb,new FrameLayout.LayoutParams(-1,-1));
        // Keep the on-device route canvas visible; never load remote map scripts or tiles.
        mapWeb.setVisibility(View.GONE);
        fallback.setVisibility(View.VISIBLE);
        root.addView(mapFrame,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16),dp(8),dp(16),dp(8));
        header.setBackgroundColor(0xEE09090D);
        TextView brand=label("Q-BOIX",GOLD,20,true);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        activityTitle=label(activityHeading(),Color.WHITE,18,true);
        header.addView(activityTitle);
        FrameLayout.LayoutParams headerParams=new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP);
        root.addView(header,headerParams);

        LinearLayout live=new LinearLayout(this);
        live.setOrientation(LinearLayout.VERTICAL);
        live.setPadding(dp(12),dp(10),dp(12),dp(8));
        GradientDrawable livePanel=new GradientDrawable();
        livePanel.setColor(0xF209090D);
        livePanel.setCornerRadius(dp(26));
        livePanel.setStroke(dp(1),0x337F6A35);
        live.setBackground(livePanel);
        live.setElevation(dp(12));
        status=label("Waiting for GPS",GREY,11,false);
        live.addView(status);
        LinearLayout row1=new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        distance=metric(row1,"0.00","KM");
        time=metric(row1,"00:00","TIME");
        pace=metric(row1,"--:--","AVG PACE / KM");
        live.addView(row1,new LinearLayout.LayoutParams(-1,dp(76)));
        LinearLayout row2=new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        speed=metric(row2,"0.0","KM/H");
        calories=metric(row2,"0","KCAL");
        lapStatus=metric(row2,"0","LAPS");
        live.addView(row2,new LinearLayout.LayoutParams(-1,dp(62)));
        LinearLayout controls=new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        start=button("START "+activityType,Color.rgb(55,190,125));
        pause=button("PAUSE",GOLD);
        finish=button("FINISH",Color.rgb(220,100,85));
        pause.setEnabled(false);
        finish.setEnabled(false);
        controls.addView(start,new LinearLayout.LayoutParams(0,dp(48),1));
        controls.addView(pause,new LinearLayout.LayoutParams(0,dp(48),1));
        controls.addView(finish,new LinearLayout.LayoutParams(0,dp(48),1));
        live.addView(controls);
        FrameLayout.LayoutParams liveParams=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        liveParams.setMargins(dp(14),0,dp(14),dp(48));
        root.addView(live,liveParams);

        TextView swipe=label("⌃  SWIPE UP FOR WORKOUT SETTINGS",GOLD,11,true);
        swipe.setGravity(Gravity.CENTER);
        swipe.setPadding(0,dp(5),0,dp(5));
        FrameLayout.LayoutParams swipeParams=new FrameLayout.LayoutParams(-1,dp(38),Gravity.BOTTOM);
        swipeParams.setMargins(dp(14),0,dp(14),dp(5));
        root.addView(swipe,swipeParams);

        LinearLayout drawer=buildSettings();
        drawer.setVisibility(View.GONE);
        FrameLayout.LayoutParams drawerParams=new FrameLayout.LayoutParams(-1,dp(400),Gravity.BOTTOM);
        drawerParams.setMargins(dp(14),0,dp(14),dp(14));
        drawer.setElevation(dp(12));
        root.addView(drawer,drawerParams);
        View dim=new View(this);
        dim.setBackgroundColor(0x99000000);
        dim.setVisibility(View.GONE);
        root.addView(dim,new FrameLayout.LayoutParams(-1,-1));
        dim.setOnClickListener(v->{
            drawer.setVisibility(View.GONE);
            dim.setVisibility(View.GONE);
            settingsOpen=false;
        });
        swipe.setOnClickListener(v->toggleSettings(drawer,dim));
        View.OnTouchListener swipeGesture=new View.OnTouchListener(){
            float y;
            public boolean onTouch(View v,android.view.MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){y=e.getRawY();return true;}
                if(e.getAction()==MotionEvent.ACTION_UP){
                    float dy=e.getRawY()-y;
                    if(dy < -dp(35))toggleSettings(drawer,dim);
                    else if(dy>dp(35)&&settingsOpen)toggleSettings(drawer,dim);
                    return true;
                }
                return true;
            }
        };
        live.setOnTouchListener(swipeGesture);
        status.setOnTouchListener(swipeGesture);
        swipe.setOnTouchListener(swipeGesture);
        start.setOnClickListener(v->requestStart());
        pause.setOnClickListener(v->command(paused?GpsTrackingService.RESUME:GpsTrackingService.PAUSE));
        finish.setOnClickListener(v->command(GpsTrackingService.FINISH));
        setContentView(root);
    }

    private LinearLayout buildSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(15),dp(18),dp(18));Bitmap settingsBitmap=BitmapFactory.decodeResource(getResources(),R.drawable.workout_settings_bg);
        BitmapDrawable settingsPhoto=new BitmapDrawable(getResources(),settingsBitmap);
        settingsPhoto.setGravity(Gravity.FILL);
        GradientDrawable darkOverlay=new GradientDrawable();
        darkOverlay.setColor(0xB809090D);
        darkOverlay.setCornerRadius(dp(26));
        LayerDrawable settingsBackground=new LayerDrawable(new android.graphics.drawable.Drawable[]{settingsPhoto,darkOverlay});
        box.setBackground(settingsBackground);
        box.setClipToOutline(true);
        box.setElevation(dp(12));
        TextView title=label("WORKOUT SETTINGS",Color.WHITE,17,true);
        title.setPaintFlags(title.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
        box.addView(title);
        modeChoice=new Spinner(this);modeChoice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Road / distance","Track workout","Cross-country"}));
        if ("TRACK & FIELD".equals(activityType)) modeChoice.setSelection(1);
        else if ("CROSS-COUNTRY".equals(activityType)) modeChoice.setSelection(2);
        else modeChoice.setSelection(0);
        addSetting(box,"WORKOUT MODE",modeChoice);
        trackLengthChoice=new Spinner(this);trackLengthChoice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"100 m","200 m","300 m","400 m","Custom track"}));addSetting(box,"TRACK LENGTH",trackLengthChoice);
        lapAlertChoice=new Spinner(this);lapAlertChoice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Every lap","Every 2 laps","Every 3 laps","Every 4 laps","Every 5 laps","Every 8 laps","Every 9 laps"}));addSetting(box,"LAP VOICE ALERT FREQUENCY",lapAlertChoice);
        intervalChoice=new Spinner(this);intervalChoice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"100 m","200 m","400 m","500 m","1 km","Half mile","1 mile","Custom metres"}));intervalChoice.setSelection(4);addSetting(box,"VOICE ALERT DISTANCE",intervalChoice);
        customMetres=new EditText(this);
        customMetres.setSingleLine(true);
        customMetres.setInputType(2);
        customMetres.setHint("Custom distance in metres");
        customMetres.setTextColor(GOLD);
        customMetres.setHintTextColor(0x99D7B55B);
        customMetres.setTextSize(19);
        customMetres.setPadding(dp(8),dp(10),dp(8),dp(10));
        customMetres.setBackgroundColor(Color.TRANSPARENT);
        customMetres.setSelectAllOnFocus(false);
        customMetres.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        box.addView(customMetres,new LinearLayout.LayoutParams(-1,dp(48)));
        voiceSwitch=new Switch(this);voiceSwitch.setText("Voice assistant");voiceSwitch.setTextColor(Color.WHITE);voiceSwitch.setChecked(true);box.addView(voiceSwitch);
        alertsSwitch=new Switch(this);alertsSwitch.setText("Distance voice alerts");alertsSwitch.setTextColor(Color.WHITE);alertsSwitch.setChecked(true);box.addView(alertsSwitch);
        TextView note=label("Settings apply when the next activity starts.",GREY,10,false);box.addView(note);
        return box;
    }
    private void addSetting(LinearLayout box,String title,Spinner spinner){
        TextView heading=label(title,Color.WHITE,11,true);
        heading.setPaintFlags(heading.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
        box.addView(heading);
        spinner.setVisibility(View.GONE);
        TextView choice=label(String.valueOf(spinner.getSelectedItem())+"     ▾",GOLD,19,true);
        choice.setPadding(dp(8),dp(9),dp(8),dp(9));
        choice.setOnClickListener(v->showChoiceDialog(title,spinner,choice));
        box.addView(choice,new LinearLayout.LayoutParams(-1,dp(48)));
        View line=new View(this);line.setBackgroundColor(0x557F6A35);
        box.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private void showChoiceDialog(String title,Spinner spinner,TextView display){
        Dialog dialog=new Dialog(this);
        FrameLayout frame=new FrameLayout(this);
        Bitmap popupBitmap=BitmapFactory.decodeResource(getResources(),R.drawable.workout_mode_popup_bg);
        android.graphics.drawable.Drawable popupPhoto;
        if(popupBitmap!=null){
            BitmapDrawable photo=new BitmapDrawable(getResources(),popupBitmap);
            photo.setGravity(Gravity.FILL);
            GradientDrawable darkOverlay=new GradientDrawable();
            darkOverlay.setColor(0xA609090D);
            darkOverlay.setCornerRadius(dp(24));
            GradientDrawable goldEdge=new GradientDrawable();
            goldEdge.setColor(Color.TRANSPARENT);
            goldEdge.setCornerRadius(dp(24));
            goldEdge.setStroke(dp(1),0x667F6A35);
            popupPhoto=new LayerDrawable(new android.graphics.drawable.Drawable[]{photo,darkOverlay,goldEdge});
        }else{
            GradientDrawable fallbackBackground=new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF171923,0xFF09090D,0xFF211B10});
            fallbackBackground.setCornerRadius(dp(24));
            popupPhoto=fallbackBackground;
        }
        frame.setBackground(popupPhoto);
        frame.setClipToOutline(true);
        LinearLayout options=new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(dp(14),dp(12),dp(14),dp(12));
        TextView heading=label(title.toUpperCase(Locale.ROOT),Color.WHITE,15,true);
        heading.setPaintFlags(heading.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
        heading.setGravity(Gravity.CENTER);
        options.addView(heading,new LinearLayout.LayoutParams(-1,dp(42)));
        for(int n=0;n<spinner.getCount();n++){
            final int index=n;
            TextView item=label(String.valueOf(spinner.getItemAtPosition(n)),GOLD,18,true);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(8),dp(12),dp(8),dp(12));
            if(n==spinner.getSelectedItemPosition())item.setBackground(panel(0x447F6A35,12));
            item.setOnClickListener(v->{spinner.setSelection(index);display.setText(String.valueOf(spinner.getItemAtPosition(index))+"     ▾");dialog.dismiss();});
            options.addView(item,new LinearLayout.LayoutParams(-1,dp(54)));
            if(n<spinner.getCount()-1){View divider=new View(this);divider.setBackgroundColor(0x337F6A35);options.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}
        }
        frame.addView(options,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));
        dialog.setContentView(frame);
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88),-2);}
        dialog.show();
        if(dialog.getWindow()!=null)dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88),-2);
    }
    private void toggleSettings(View drawer,View dim){settingsOpen=!settingsOpen;drawer.setVisibility(settingsOpen?View.VISIBLE:View.GONE);dim.setVisibility(settingsOpen?View.VISIBLE:View.GONE);drawer.bringToFront();dim.bringToFront();if(settingsOpen)drawer.bringToFront();}
    private String mapHtml(){return "";}
    private void updateRoute(String json){try{JSONArray a=new JSONArray(json);fallback.setRoute(a);if(mapWeb.getVisibility()==View.VISIBLE)mapWeb.evaluateJavascript("setRoute("+json+")",null);}catch(Exception ignored){}}
    private String activityHeading(){
        if("MARATHON".equals(activityType))return "MARATHON TRAINING";
        if("CUSTOM".equals(activityType))return "CUSTOM GPS WORKOUT";
        return activityType+" GPS";
    }
    private void requestStart(){double interval=selectedInterval();if(interval<0)return;if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},41);return;}command(GpsTrackingService.START,interval);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);
        boolean granted=false;
        for(int result:g) if(result==PackageManager.PERMISSION_GRANTED) granted=true;
        if(r==40){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                startPreviewLocationUpdates();
            } else {
                status.setText("Precise location permission denied • GPS unavailable");
            }
        } else if(r==41){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                command(GpsTrackingService.START,selectedInterval());
            } else {
                status.setText("Precise GPS permission is required to record.");
            }
        }
    }
    private double selectedInterval(){int pos=intervalChoice.getSelectedItemPosition();if(pos==7){try{double n=Double.parseDouble(customMetres.getText().toString());if(n>=50&&n<=100000)return n;}catch(Exception ignored){}Toast.makeText(this,"Enter a distance from 50 to 100000 metres.",Toast.LENGTH_LONG).show();return -1;}return intervals[pos];}
    private double selectedTrack(){int p=trackLengthChoice.getSelectedItemPosition();if(p<4)return new double[]{100,200,300,400}[p];try{return Double.parseDouble(customMetres.getText().toString());}catch(Exception e){return 400;}}
    private void command(String action){command(action,1000);}
    private void command(String action,double interval){Intent i=new Intent(this,GpsTrackingService.class);i.setAction(action);if(GpsTrackingService.START.equals(action)){i.putExtra(GpsTrackingService.EXTRA_INTERVAL,interval);i.putExtra(GpsTrackingService.EXTRA_ACTIVITY_TYPE,activityType);i.putExtra(GpsTrackingService.EXTRA_TRACK_MODE,modeChoice.getSelectedItemPosition()==1 || "TRACK & FIELD".equals(activityType));i.putExtra(GpsTrackingService.EXTRA_TRACK_LENGTH,selectedTrack());i.putExtra(GpsTrackingService.EXTRA_LAP_ALERT_EVERY,new int[]{1,2,3,4,5,8,9}[lapAlertChoice.getSelectedItemPosition()]);i.putExtra(GpsTrackingService.EXTRA_VOICE_ENABLED,voiceSwitch.isChecked());i.putExtra(GpsTrackingService.EXTRA_ALERTS_ENABLED,alertsSwitch.isChecked());}if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
    @Override protected void onStart(){super.onStart();IntentFilter f=new IntentFilter(GpsTrackingService.ACTION);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);registered=true;}
    @Override protected void onStop(){
        if(registered){unregisterReceiver(receiver);registered=false;}
        if(previewLocationManager!=null && previewUpdatesRequested){
            previewLocationManager.removeUpdates(previewLocationListener);
            previewUpdatesRequested=false;
        }
        super.onStop();
    }
    @Override protected void onResume(){
        super.onResume();
        if(previewLocationManager!=null
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            startPreviewLocationUpdates();
        }
    }
    @Override protected void onDestroy(){if(mapWeb!=null)mapWeb.destroy();super.onDestroy();}
    private TextView metric(LinearLayout row,String value,String caption){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(6),dp(5),dp(6),dp(5));
        GradientDrawable glass=new GradientDrawable();
        glass.setColor(0x40FFFFFF);
        glass.setCornerRadius(dp(16));
        glass.setStroke(dp(1),0x22FFFFFF);
        card.setBackground(glass);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);
        params.setMargins(dp(4),dp(4),dp(4),dp(4));
        TextView v=label(value,GOLD,19,true);
        TextView t=label(caption,GREY,9,true);
        card.addView(v);
        card.addView(t);
        row.addView(card,params);
        return v;
    }
    private Button button(String s,int color){
        Button b=new Button(this);
        b.setText(s);
        b.setTextSize(10);
        b.setTextColor(Color.BLACK);
        b.setTypeface(null,1);
        b.setAllCaps(false);
        b.setBackground(fadeButton(color));
        return b;
    }
    private android.graphics.drawable.Drawable fadeButton(int color){
        int transparent=color & 0x00FFFFFF;
        GradientDrawable glow=new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{transparent,color,color,transparent});
        glow.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        glow.setCornerRadius(dp(28));
        return glow;
    }
    private TextView label(String s,int c,float size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(c);t.setTextSize(size);if(bold)t.setTypeface(null,1);return t;}
    private GradientDrawable panel(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String clock(int s){return String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60);}

    private class RouteCanvas extends View{
        private JSONArray route=new JSONArray();private final Paint line=new Paint(3),dot=new Paint(3);
        private Location userLocation;
        RouteCanvas(Context c){
            super(c);
            setBackgroundColor(Color.WHITE);
            line.setColor(BLUE);
            line.setStrokeWidth(dp(4));
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            dot.setColor(BLUE);
        }
        void setCurrentLocation(Location location){
            userLocation=new Location(location);
            postInvalidate();
        }
        void setRoute(JSONArray a){route=a;postInvalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(route.length()==0){Paint p=new Paint(3);p.setColor(Color.GRAY);p.setTextSize(dp(14));c.drawText(userLocation==null?"Waiting for GPS location…":"GPS position acquired",dp(24),dp(40),p);if(userLocation!=null){Paint userPaint=new Paint(3);userPaint.setColor(Color.rgb(22,166,106));c.drawCircle(getWidth()/2f,getHeight()/2f,dp(10),userPaint);userPaint.setColor(Color.WHITE);userPaint.setTextSize(dp(12));c.drawText(String.format(Locale.getDefault(),"%.5f, %.5f",userLocation.getLatitude(),userLocation.getLongitude()),dp(18),getHeight()/2f+dp(35),userPaint);}return;}double minLat=90,maxLat=-90,minLon=180,maxLon=-180;for(int i=0;i<route.length();i++){JSONObject o=route.optJSONObject(i);if(o==null)continue;double la=o.optDouble("lat"),lo=o.optDouble("lon");minLat=Math.min(minLat,la);maxLat=Math.max(maxLat,la);minLon=Math.min(minLon,lo);maxLon=Math.max(maxLon,lo);}float pad=dp(35),w=getWidth()-2*pad,h=getHeight()-2*pad;Path path=new Path();for(int i=0;i<route.length();i++){JSONObject o=route.optJSONObject(i);if(o==null)continue;float x=pad+(float)((o.optDouble("lon")-minLon)/Math.max(0.000001,maxLon-minLon))*w;float y=getHeight()-pad-(float)((o.optDouble("lat")-minLat)/Math.max(0.000001,maxLat-minLat))*h;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}c.drawPath(path,line);JSONObject last=route.optJSONObject(route.length()-1);if(last!=null){float x=pad+(float)((last.optDouble("lon")-minLon)/Math.max(0.000001,maxLon-minLon))*w;float y=getHeight()-pad-(float)((last.optDouble("lat")-minLat)/Math.max(0.000001,maxLat-minLat))*h;c.drawCircle(x,y,dp(7),dot);}}
    }
}