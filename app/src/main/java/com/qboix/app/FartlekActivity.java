package com.qboix.app;

import android.app.Activity;
import android.Manifest;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.CheckBox;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import org.json.JSONArray;
import org.json.JSONObject;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FartlekActivity extends Activity {
    private TextToSpeech speech;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean audioFocusHeld = false;
    private boolean speechReady = false;
    private boolean voiceEnabled = true;
    private boolean vibrationEnabled = true;
    private Vibrator vibrator;
    private final int GOLD = Color.rgb(215,181,91);
    private final int BLACK = Color.rgb(9,9,13);
    private final int CARD = Color.rgb(27,27,34);
    private final int WHITE = Color.WHITE;
    private final int GREY = Color.rgb(165,165,175);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText sprintDurationInput, recoveryDurationInput;
    private TextView phaseText, timerText, progressText, elapsedText;
    private Button startButton, pauseButton, stopButton, compactPause, compactFinish;
    private LinearLayout expandedPanel;
    private FrameLayout compactPanel;
    private WebView mapWeb;
    private RouteCanvas routeCanvas;
    private TextView compactPhase, compactTimer, compactProgress;
    private boolean minimized=false, gpsReceiverRegistered=false;
    private double gpsDistance=0, sprintStartDistance=0;
    private int gpsElapsed=0, sprintStartElapsed=0;
    private int sprintSeconds, recoverySeconds, targetRepetitions;
    private int completedRepetitions, remainingSeconds, elapsedSeconds, phaseElapsedSeconds;
    private final org.json.JSONArray intervalSplits = new org.json.JSONArray();
    private long workoutStartedAt;
    private boolean running = false, paused = false, recoveryPhase = false;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running || paused) return;
            elapsedSeconds++;
            phaseElapsedSeconds++;
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                recordIntervalSplit();
                if (!recoveryPhase) {
                    announceSprintSummary();
                    recoveryPhase = true;
                    gpsCommand("PHASE_RECOVERY");
                    remainingSeconds = recoverySeconds;
                    sprintStartDistance=gpsDistance; sprintStartElapsed=gpsElapsed;
                    phaseText.setText("RECOVERY • EASY JOG");
                    announce("Cooldown now. Easy jog.");
                    cueVibration();
                } else {
                    completedRepetitions++;
                    recoveryPhase = false;
                    gpsCommand("PHASE_SPRINT");
                    remainingSeconds = sprintSeconds;
                    sprintStartDistance=gpsDistance; sprintStartElapsed=gpsElapsed;
                    phaseText.setText("SPRINT • RUN FAST");
                    announce("Sprint now. Run fast.");
                    cueVibration();
                }
            }
            if (remainingSeconds > 0 && remainingSeconds <= 10) {
                announce(Integer.toString(remainingSeconds));
            }
            updateDisplay();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        voiceEnabled = getPreferences(MODE_PRIVATE).getBoolean("voice_enabled", true);
        vibrationEnabled = getPreferences(MODE_PRIVATE).getBoolean("vibration_enabled", true);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        speech = new TextToSpeech(this, status -> {
            speechReady = status == TextToSpeech.SUCCESS;
            if (speechReady) { int language=speech.setLanguage(java.util.Locale.getDefault()); if(language==TextToSpeech.LANG_MISSING_DATA||language==TextToSpeech.LANG_NOT_SUPPORTED)speech.setLanguage(java.util.Locale.US); speech.setSpeechRate(0.95f); speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                private void releaseWhenFinished() { handler.postDelayed(() -> { if (speech == null || !speech.isSpeaking()) releaseSpeechAudioFocus(); }, 250); }
                @Override public void onStart(String id) {}
                @Override public void onDone(String id) { releaseWhenFinished(); }
                @Override public void onError(String id) { releaseWhenFinished(); }
                @Override public void onStop(String id, boolean interrupted) { releaseWhenFinished(); }
            }); }
        });
        buildScreen();
    }

    private void buildScreen() {
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(BLACK);

        expandedPanel=new LinearLayout(this);
        expandedPanel.setOrientation(LinearLayout.VERTICAL);
        expandedPanel.setPadding(dp(18),dp(16),dp(18),dp(14));
        expandedPanel.setBackgroundColor(BLACK);

        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=label("Q-BOIX  •  FARTLEK",GOLD,21,true);
        top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button minimize=actionButton("—",CARD,WHITE);
        minimize.setTextSize(22); minimize.setContentDescription("Minimize workout");
        top.addView(minimize,new LinearLayout.LayoutParams(dp(48),dp(44)));
        minimize.setOnClickListener(v->setMinimized(true));
        expandedPanel.addView(top);
        TextView heading=label("BUILD YOUR INTERVALS",WHITE,16,true);
        LinearLayout.LayoutParams headingParams=new LinearLayout.LayoutParams(-1,-2);headingParams.topMargin=dp(12);expandedPanel.addView(heading,headingParams);
        expandedPanel.addView(label("Set your fast-run and easy-jog durations.",GREY,12,false));

        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout fast=new LinearLayout(this);fast.setOrientation(LinearLayout.VERTICAL);
        fast.addView(label("SPRINT • MINUTES.SECONDS",GOLD,11,true));
        sprintDurationInput=durationField("3.00");fast.addView(sprintDurationInput,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout easy=new LinearLayout(this);easy.setOrientation(LinearLayout.VERTICAL);
        easy.addView(label("COOLDOWN • MINUTES.SECONDS",GOLD,11,true));
        recoveryDurationInput=durationField("1.00");easy.addView(recoveryDurationInput,new LinearLayout.LayoutParams(-1,dp(54)));
        fields.addView(fast,new LinearLayout.LayoutParams(0,-2,1));fields.addView(spaceWidth(10));fields.addView(easy,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.topMargin=dp(14);expandedPanel.addView(fields,fp);
        expandedPanel.addView(label("Example: 3.00 = 3 minutes; 1.30 = 1 minute 30 seconds.",GREY,11,false));

        LinearLayout display=new LinearLayout(this);display.setOrientation(LinearLayout.VERTICAL);display.setGravity(Gravity.CENTER);
        display.setPadding(dp(14),dp(12),dp(14),dp(12));display.setBackground(panel());
        LinearLayout.LayoutParams displayParams=new LinearLayout.LayoutParams(-1,0,1);displayParams.topMargin=dp(12);displayParams.bottomMargin=dp(10);expandedPanel.addView(display,displayParams);
        phaseText=label("READY",GOLD,18,true);phaseText.setGravity(Gravity.CENTER);display.addView(phaseText);
        timerText=label("03:00",WHITE,52,true);timerText.setGravity(Gravity.CENTER);display.addView(timerText);
        progressText=label("Open session | 0 rounds complete",GREY,14,false);progressText.setGravity(Gravity.CENTER);display.addView(progressText);
        elapsedText=label("Elapsed 00:00",GREY,12,false);elapsedText.setGravity(Gravity.CENTER);display.addView(elapsedText);

        startButton=actionButton("START FARTLEK",GOLD,BLACK);expandedPanel.addView(startButton,new LinearLayout.LayoutParams(-1,dp(48)));startButton.setOnClickListener(v->startOrResume());
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setPadding(0,dp(8),0,0);
        pauseButton=actionButton("PAUSE",CARD,WHITE);stopButton=actionButton("FINISH & SAVE",Color.rgb(55,190,125),BLACK);
        controls.addView(pauseButton,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(spaceWidth(10));controls.addView(stopButton,new LinearLayout.LayoutParams(0,dp(48),1));expandedPanel.addView(controls);
        pauseButton.setOnClickListener(v->pauseWorkout());stopButton.setOnClickListener(v->confirmStop());

        LinearLayout options=new LinearLayout(this);options.setOrientation(LinearLayout.HORIZONTAL);
        CheckBox voiceToggle=new CheckBox(this);voiceToggle.setText("Voice countdown");voiceToggle.setTextColor(WHITE);voiceToggle.setButtonTintList(android.content.res.ColorStateList.valueOf(GOLD));voiceToggle.setChecked(voiceEnabled);
        voiceToggle.setOnCheckedChangeListener((button,checked)->{voiceEnabled=checked;getPreferences(MODE_PRIVATE).edit().putBoolean("voice_enabled",checked).apply();});
        CheckBox vibrationToggle=new CheckBox(this);vibrationToggle.setText("Vibration");vibrationToggle.setTextColor(WHITE);vibrationToggle.setButtonTintList(android.content.res.ColorStateList.valueOf(GOLD));vibrationToggle.setChecked(vibrationEnabled);
        vibrationToggle.setOnCheckedChangeListener((button,checked)->{vibrationEnabled=checked;getPreferences(MODE_PRIVATE).edit().putBoolean("vibration_enabled",checked).apply();});
        options.addView(voiceToggle,new LinearLayout.LayoutParams(0,dp(42),1));options.addView(vibrationToggle,new LinearLayout.LayoutParams(0,dp(42),1));expandedPanel.addView(options);
        Button testVoice=actionButton("TEST VOICE",CARD,GOLD);expandedPanel.addView(testVoice,new LinearLayout.LayoutParams(-1,dp(38)));
        testVoice.setOnClickListener(v->{if(speechReady&&speech!=null)speech.speak("Q BOIX voice assistant is ready.",TextToSpeech.QUEUE_FLUSH,null,"qboix_fartlek_test");});

        root.addView(expandedPanel,new FrameLayout.LayoutParams(-1,-1));
        compactPanel=new FrameLayout(this);compactPanel.setVisibility(View.GONE);
        mapWeb=new WebView(this);mapWeb.setBackgroundColor(Color.rgb(235,239,242));mapWeb.getSettings().setJavaScriptEnabled(true);mapWeb.getSettings().setDomStorageEnabled(true);
        mapWeb.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String url){mapWeb.evaluateJavascript("typeof L !== 'undefined' ? 'ready' : 'offline'",result->{boolean ready=result!=null&&result.contains("ready");mapWeb.setVisibility(ready?View.VISIBLE:View.GONE);routeCanvas.setVisibility(ready?View.GONE:View.VISIBLE);});}
            @Override public void onReceivedError(WebView v,android.webkit.WebResourceRequest req,android.webkit.WebResourceError err){if(req.isForMainFrame()){mapWeb.setVisibility(View.GONE);routeCanvas.setVisibility(View.VISIBLE);}}
        });
        compactPanel.addView(mapWeb,new FrameLayout.LayoutParams(-1,-1));
        routeCanvas=new RouteCanvas(this);compactPanel.addView(routeCanvas,new FrameLayout.LayoutParams(-1,-1));mapWeb.setVisibility(View.INVISIBLE);
        mapWeb.loadDataWithBaseURL("https://qboix.local/",mapHtml(),"text/html","UTF-8",null);

        LinearLayout liveInfo=new LinearLayout(this);liveInfo.setOrientation(LinearLayout.VERTICAL);liveInfo.setPadding(dp(14),dp(10),dp(14),dp(10));liveInfo.setBackground(panel(0xE609090D));
        compactPhase=label("SPRINT",GOLD,14,true);compactTimer=label("03:00",WHITE,34,true);compactProgress=label("0 rounds • GPS waiting",WHITE,12,false);
        liveInfo.addView(compactPhase);liveInfo.addView(compactTimer);liveInfo.addView(compactProgress);
        FrameLayout.LayoutParams infoParams=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);infoParams.setMargins(dp(12),dp(12),dp(68),0);compactPanel.addView(liveInfo,infoParams);
        Button restore=actionButton("↗",CARD,WHITE);restore.setTextSize(20);restore.setContentDescription("Restore full workout");
        FrameLayout.LayoutParams restoreParams=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.RIGHT);restoreParams.setMargins(0,dp(12),dp(12),0);compactPanel.addView(restore,restoreParams);restore.setOnClickListener(v->setMinimized(false));
        LinearLayout liveControls=new LinearLayout(this);liveControls.setOrientation(LinearLayout.HORIZONTAL);liveControls.setPadding(dp(10),dp(10),dp(10),dp(10));liveControls.setBackground(panel(0xE609090D));
        compactPause=actionButton("PAUSE",GOLD,BLACK);compactFinish=actionButton("FINISH & SAVE",Color.rgb(55,190,125),BLACK);
        liveControls.addView(compactPause,new LinearLayout.LayoutParams(0,dp(52),1));liveControls.addView(spaceWidth(10));liveControls.addView(compactFinish,new LinearLayout.LayoutParams(0,dp(52),1));
        FrameLayout.LayoutParams liveParams=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);liveParams.setMargins(dp(12),0,dp(12),dp(14));compactPanel.addView(liveControls,liveParams);
        compactPause.setOnClickListener(v->pauseWorkout());compactFinish.setOnClickListener(v->confirmStop());
        root.addView(compactPanel,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);updateDisplay();
    }

    private String mapHtml(){
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#map{height:100%;margin:0;background:#e8edf0}</style></head><body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>var map=L.map('map').setView([-1.2864,36.8172],15);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);var blue=L.polyline([],{color:'#2878ff',weight:6}).addTo(map),red=L.polyline([],{color:'#e34242',weight:6}).addTo(map),marker=null;function setRoute(a){var s=[],r=[];a.forEach(function(p){(p.recovery?r:s).push([p.lat,p.lon]);});blue.setLatLngs(s);red.setLatLngs(r);if(a.length){var p=a[a.length-1],ll=[p.lat,p.lon];if(marker)marker.setLatLng(ll);else marker=L.circleMarker(ll,{radius:7,color:'#fff',weight:2,fillColor:'#16a66a',fillOpacity:1}).addTo(map);map.panTo(ll);}}</script></body></html>";
    }
    private void setMinimized(boolean value){minimized=value;expandedPanel.setVisibility(value?View.GONE:View.VISIBLE);compactPanel.setVisibility(value?View.VISIBLE:View.GONE);updateDisplay();}
    private void updateRoute(String json){try{JSONArray a=new JSONArray(json);routeCanvas.setRoute(a);if(mapWeb.getVisibility()==View.VISIBLE)mapWeb.evaluateJavascript("setRoute("+json+")",null);}catch(Exception ignored){}}
    private final BroadcastReceiver gpsReceiver=new BroadcastReceiver(){@Override public void onReceive(android.content.Context c,Intent i){gpsDistance=i.getDoubleExtra("distance",gpsDistance);gpsElapsed=i.getIntExtra("seconds",gpsElapsed);String route=i.getStringExtra("route_json");if(route!=null)updateRoute(route);updateDisplay();}};
    private class RouteCanvas extends View{
        private JSONArray route=new JSONArray();private final Paint blue=new Paint(3),red=new Paint(3),dot=new Paint(3);
        RouteCanvas(android.content.Context c){super(c);setBackgroundColor(Color.rgb(235,239,242));blue.setColor(Color.rgb(40,120,255));red.setColor(Color.rgb(227,66,66));for(Paint p:new Paint[]{blue,red}){p.setStrokeWidth(dp(5));p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}dot.setColor(Color.rgb(22,166,106));}
        void setRoute(JSONArray a){route=a;postInvalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(route.length()==0){blue.setStyle(Paint.Style.FILL);blue.setTextSize(dp(14));c.drawText("Waiting for GPS route…",dp(20),dp(40),blue);blue.setStyle(Paint.Style.STROKE);return;}double minLat=90,maxLat=-90,minLon=180,maxLon=-180;for(int i=0;i<route.length();i++){JSONObject o=route.optJSONObject(i);if(o==null)continue;minLat=Math.min(minLat,o.optDouble("lat"));maxLat=Math.max(maxLat,o.optDouble("lat"));minLon=Math.min(minLon,o.optDouble("lon"));maxLon=Math.max(maxLon,o.optDouble("lon"));}float pad=dp(30),w=getWidth()-2*pad,h=getHeight()-2*pad;for(int i=1;i<route.length();i++){JSONObject a=route.optJSONObject(i-1),b=route.optJSONObject(i);if(a==null||b==null)continue;float x1=pad+(float)((a.optDouble("lon")-minLon)/Math.max(.000001,maxLon-minLon))*w,y1=getHeight()-pad-(float)((a.optDouble("lat")-minLat)/Math.max(.000001,maxLat-minLat))*h,x2=pad+(float)((b.optDouble("lon")-minLon)/Math.max(.000001,maxLon-minLon))*w,y2=getHeight()-pad-(float)((b.optDouble("lat")-minLat)/Math.max(.000001,maxLat-minLat))*h;c.drawLine(x1,y1,x2,y2,b.optBoolean("recovery")?red:blue);}JSONObject last=route.optJSONObject(route.length()-1);if(last!=null){float x=pad+(float)((last.optDouble("lon")-minLon)/Math.max(.000001,maxLon-minLon))*w,y=getHeight()-pad-(float)((last.optDouble("lat")-minLat)/Math.max(.000001,maxLat-minLat))*h;c.drawCircle(x,y,dp(7),dot);}}
    }

    private EditText durationField(String value) {
        EditText input=new EditText(this); input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(value); input.setHint("Minutes.Seconds"); input.setHintTextColor(GREY);
        input.setTextColor(WHITE); input.setTextSize(18); input.setPadding(dp(14),0,dp(14),0); input.setBackground(panel());
        return input;
    }

    private int parseDuration(EditText field) {
        String value=field.getText().toString().trim();
        if(value.isEmpty())throw new IllegalArgumentException();
        String[] parts=value.split("\\.",-1);
        if(parts.length>2 || parts[0].isEmpty())throw new IllegalArgumentException();
        int minutes=Integer.parseInt(parts[0]);
        int seconds=parts.length==1 || parts[1].isEmpty()?0:Integer.parseInt(parts[1]);
        if(minutes<0 || seconds<0 || seconds>59)throw new IllegalArgumentException();
        return minutes*60+seconds;
    }
    private void startOrResume() {
        if (running && paused) {
            paused=false; gpsCommand(GpsTrackingService.RESUME); startButton.setText("WORKOUT RUNNING");
            handler.postDelayed(ticker,1000);
            return;
        }
        if (running) return;
        try {
            sprintSeconds=parseDuration(sprintDurationInput);
            recoverySeconds=parseDuration(recoveryDurationInput);
            targetRepetitions=0;
        } catch(Exception e) {
            Toast.makeText(this,"Use minutes.seconds, for example 3.00 or 1.30. Seconds must be 00–59.",Toast.LENGTH_LONG).show();
            return;
        }
        if(sprintSeconds<1||sprintSeconds>21600||recoverySeconds<1||recoverySeconds>21600) {
            Toast.makeText(this,"Each interval must be 1 second to 6 hours; repetitions 1–100.",Toast.LENGTH_LONG).show();
            return;
        }
        completedRepetitions=0; elapsedSeconds=0; phaseElapsedSeconds=0; gpsDistance=0;gpsElapsed=0;sprintStartDistance=0;sprintStartElapsed=0;while(intervalSplits.length()>0)intervalSplits.remove(0); workoutStartedAt=System.currentTimeMillis(); recoveryPhase=false;
        remainingSeconds=sprintSeconds; running=true; paused=false;
        startGpsTracking();
        sprintStartDistance=gpsDistance;sprintStartElapsed=gpsElapsed;
        setIntervalInputsEnabled(false);
        phaseText.setText("SPRINT • RUN FAST");
        startButton.setText("WORKOUT RUNNING");
        handler.postDelayed(ticker,1000);
        updateDisplay();
    }

    private void pauseWorkout() {
        if(!running||paused) return;
        paused=true; handler.removeCallbacks(ticker);
        gpsCommand(GpsTrackingService.PAUSE);
        phaseText.setText("PAUSED");
        startButton.setText("RESUME WORKOUT");
    }

    private void confirmStop() {
        if(!running) { finish(); return; }
        new AlertDialog.Builder(this).setTitle("Finish this fartlek session?")
            .setMessage("Save this session with "+completedRepetitions+" completed fast/easy rounds?")
            .setNegativeButton("KEEP GOING",null)
            .setPositiveButton("FINISH & SAVE",(d,w)->finishWorkout()).show();
    }
    private void resetWorkout() {
        handler.removeCallbacks(ticker);
        running=false; paused=false; completedRepetitions=0; elapsedSeconds=0;
        setIntervalInputsEnabled(true);
        phaseText.setText("READY"); startButton.setText("START WORKOUT");
        updateDisplay();
    }

    private void finishWorkout() {
        handler.removeCallbacks(ticker);
        running=false; paused=false;
        gpsCommand(GpsTrackingService.FINISH);
        recordIntervalSplit();
        WorkoutStore.saveFartlek(this,sprintSeconds,recoverySeconds,completedRepetitions,elapsedSeconds,workoutStartedAt,System.currentTimeMillis(),intervalSplits);
        phaseText.setText("WORKOUT COMPLETE");
        timerText.setText("DONE");
        progressText.setText(completedRepetitions+" rounds completed");
        elapsedText.setText("Elapsed "+formatTime(elapsedSeconds));
        startButton.setText("START AGAIN");
        setIntervalInputsEnabled(true);
        Toast.makeText(this,"Fartlek workout saved on this device.",Toast.LENGTH_LONG).show();
    }

    private void startGpsTracking() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},72);
            return;
        }
        Intent i=new Intent(this,GpsTrackingService.class);
        i.setAction(GpsTrackingService.START);
        i.putExtra(GpsTrackingService.EXTRA_ACTIVITY_TYPE,"FARTLEK");
        i.putExtra(GpsTrackingService.EXTRA_INTERVAL,1000.0);
        i.putExtra(GpsTrackingService.EXTRA_TRACK_MODE,false);
        i.putExtra(GpsTrackingService.EXTRA_VOICE_ENABLED,false);
        i.putExtra(GpsTrackingService.EXTRA_ALERTS_ENABLED,false);
        if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
    }
    private void gpsCommand(String action) {
        Intent i=new Intent(this,GpsTrackingService.class);i.setAction(action);
        if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==72) {
            boolean granted=false;for(int result:results)if(result==PackageManager.PERMISSION_GRANTED)granted=true;
            if(granted)startGpsTracking();
            else Toast.makeText(this,"GPS permission denied. Fartlek timer will continue without route tracking.",Toast.LENGTH_LONG).show();
        }
    }

    private void recordIntervalSplit() {
        try { org.json.JSONObject split=new org.json.JSONObject(); split.put("phase",recoveryPhase?"RECOVERY":"SPRINT"); split.put("durationSeconds",phaseElapsedSeconds); split.put("completedAtElapsedSeconds",elapsedSeconds); intervalSplits.put(split); } catch(org.json.JSONException ignored) {}
        phaseElapsedSeconds=0;
    }

    private void announceSprintSummary(){double metres=Math.max(0,gpsDistance-sprintStartDistance);int seconds=Math.max(0,gpsElapsed-sprintStartElapsed);double pace=metres>=20?seconds/(metres/1000.0):0;int p=(int)Math.round(pace);String paceText=pace>0?(p/60)+" minutes "+(p%60)+" seconds per kilometre":"pace unavailable";announce("Sprint complete. You ran "+String.format(java.util.Locale.US,"%.0f",metres)+" metres in "+(seconds/60)+" minutes "+(seconds%60)+" seconds. Sprint pace "+paceText+". Cooldown now.");}

    private void announce(String words) {
        if (voiceEnabled && speech != null && speechReady) {
            speech.speak(words, TextToSpeech.QUEUE_FLUSH, null, "qboix_fartlek_voice");
        }
    }

    private void cueVibration() {
        if (!vibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(250);
        }
    }

    private void updateDisplay() {
        if(!running && completedRepetitions==0) {
            int s=previewDuration(sprintDurationInput,180);
            timerText.setText(formatTime(s));
            progressText.setText("Open session | 0 rounds complete");
            elapsedText.setText("Elapsed "+formatTime(elapsedSeconds));
            return;
        }
        timerText.setText(formatTime(remainingSeconds));
        progressText.setText(completedRepetitions+" rounds complete | open session");
        elapsedText.setText("Elapsed "+formatTime(elapsedSeconds));
        if(compactPhase!=null){compactPhase.setText(recoveryPhase?"COOLDOWN • EASY JOG":"SPRINT • RUN FAST");compactTimer.setText(formatTime(remainingSeconds));compactProgress.setText(completedRepetitions+" rounds • "+String.format(java.util.Locale.US,"%.2f km",gpsDistance/1000.0));compactPause.setText(paused?"RESUME":"PAUSE");}
    }

    private int previewDuration(EditText field,int fallback) {
        try { return parseDuration(field); } catch(Exception e) { return fallback; }
    }
    private void setIntervalInputsEnabled(boolean enabled) {
        sprintDurationInput.setEnabled(enabled); recoveryDurationInput.setEnabled(enabled);
    }
    private String formatTime(int seconds) {
        return String.format(java.util.Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60);
    }

    private TextView label(String value,int color,float size,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private Button actionButton(String value,int bg,int fg) {
        Button b=new Button(this); b.setText(value); b.setTextColor(fg); b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setAllCaps(false); b.setBackground(panel(bg)); return b;
    }
    private GradientDrawable panel() { return panel(CARD); }
    private GradientDrawable panel(int color) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(14)); return d;
    }
    private ViewSpace spaceWidth(int width) { return new ViewSpace(width); }
    private class ViewSpace extends android.view.View {
        ViewSpace(int width) { super(FartlekActivity.this); setLayoutParams(new LinearLayout.LayoutParams(dp(width),1)); }
    }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }

    @Override protected void onStart(){super.onStart();IntentFilter filter=new IntentFilter(GpsTrackingService.ACTION);if(Build.VERSION.SDK_INT>=33)registerReceiver(gpsReceiver,filter,android.content.Context.RECEIVER_NOT_EXPORTED);else registerReceiver(gpsReceiver,filter);gpsReceiverRegistered=true;}
    @Override protected void onStop(){if(gpsReceiverRegistered){unregisterReceiver(gpsReceiver);gpsReceiverRegistered=false;}super.onStop();}
    @Override public void onBackPressed() {if(minimized){setMinimized(false);return;}if(running)confirmStop();else super.onBackPressed();}
    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        if(mapWeb!=null)mapWeb.destroy();
        if (speech != null) {
            speech.stop();
            speech.shutdown();
        }
        super.onDestroy();
    }
}
