package com.qboix.app;

import android.app.Activity;
import android.Manifest;
import android.content.Intent;
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
import android.widget.CheckBox;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FartlekActivity extends Activity {
    private TextToSpeech speech;
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
    private Button startButton, pauseButton, stopButton;
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
                    recoveryPhase = true;
                    remainingSeconds = recoverySeconds;
                    phaseText.setText("RECOVERY • EASY JOG");
                    announce("Cool down! Cool down!");
                    cueVibration();
                } else {
                    completedRepetitions++;
                    recoveryPhase = false;
                    remainingSeconds = sprintSeconds;
                    phaseText.setText("SPRINT • RUN FAST");
                    announce("Up!");
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
            if (speechReady) { int language=speech.setLanguage(java.util.Locale.getDefault()); if(language==TextToSpeech.LANG_MISSING_DATA||language==TextToSpeech.LANG_NOT_SUPPORTED)speech.setLanguage(java.util.Locale.US); speech.setSpeechRate(0.95f); }
        });
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(24),dp(22),dp(24));
        root.setBackgroundColor(BLACK);

        TextView brand = label("Q-BOIX", GOLD, 26, true);
        root.addView(brand);
        TextView heading = label("FARTLEK WORKOUT", WHITE, 20, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1,-2);
        hp.topMargin=dp(18); root.addView(heading,hp);
        root.addView(label("Set your fast and easy running intervals.",GREY,13,false));

        TextView ratioLabel = label("DEFAULT RATIO 3:1 • EDIT BOTH INTERVALS", GOLD, 12, true);
        LinearLayout.LayoutParams ratioParams = new LinearLayout.LayoutParams(-1,-2);
        ratioParams.topMargin = dp(18);
        root.addView(ratioLabel, ratioParams);

        TextView sprintLabel = label("FAST RUN DURATION (MINUTES.SECONDS)", WHITE, 12, true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1,-2); slp.topMargin=dp(10); root.addView(sprintLabel,slp);
        sprintDurationInput = durationField("3.00");
        root.addView(sprintDurationInput,new LinearLayout.LayoutParams(-1,dp(54)));

        TextView recoveryLabel = label("EASY JOG DURATION (MINUTES.SECONDS)", WHITE, 12, true);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1,-2); rlp.topMargin=dp(12); root.addView(recoveryLabel,rlp);
        recoveryDurationInput = durationField("1.00");
        root.addView(recoveryDurationInput,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView formatHint=label("Example: 3.00 = 3 minutes | 1.30 = 1 minute 30 seconds",GREY,11,false);
        LinearLayout.LayoutParams fhp=new LinearLayout.LayoutParams(-1,-2);fhp.topMargin=dp(6);root.addView(formatHint,fhp);
        TextView finishHint=label("No fixed 8-round limit. Tap FINISH & SAVE whenever you are done.",Color.rgb(55,190,125),11,true);
        LinearLayout.LayoutParams fip=new LinearLayout.LayoutParams(-1,-2);fip.topMargin=dp(10);root.addView(finishHint,fip);
        LinearLayout display = new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setGravity(Gravity.CENTER);
        display.setPadding(dp(18),dp(28),dp(18),dp(28));
        display.setBackground(panel());
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(-1,0,1);
        displayParams.topMargin=dp(14); displayParams.bottomMargin=dp(12);
        root.addView(display,displayParams);

        phaseText = label("READY",GOLD,18,true);
        phaseText.setGravity(Gravity.CENTER);
        display.addView(phaseText);
        timerText = label("03:00",WHITE,56,true);
        timerText.setGravity(Gravity.CENTER);
        display.addView(timerText);
        progressText = label("Open session | 0 rounds complete",GREY,14,false);
        progressText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);
        pp.topMargin=dp(14); display.addView(progressText,pp);
        elapsedText = label("Elapsed 00:00",GREY,13,false);
        elapsedText.setGravity(Gravity.CENTER);
        display.addView(elapsedText);

        startButton = actionButton("START WORKOUT",GOLD,BLACK);
        root.addView(startButton,new LinearLayout.LayoutParams(-1,dp(52)));
        startButton.setOnClickListener(v -> startOrResume());

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(0,dp(10),0,0);
        pauseButton = actionButton("PAUSE",CARD,WHITE);
        stopButton = actionButton("FINISH & SAVE",Color.rgb(55,190,125),BLACK);
        controls.addView(pauseButton,new LinearLayout.LayoutParams(0,dp(48),1));
        controls.addView(spaceWidth(10));
        controls.addView(stopButton,new LinearLayout.LayoutParams(0,dp(48),1));
        root.addView(controls);
        pauseButton.setOnClickListener(v -> pauseWorkout());
        stopButton.setOnClickListener(v -> confirmStop());

        LinearLayout feedback = new LinearLayout(this);
        feedback.setOrientation(LinearLayout.HORIZONTAL);
        CheckBox voiceToggle = new CheckBox(this);
        voiceToggle.setText("Voice countdown");
        voiceToggle.setTextColor(WHITE);
        voiceToggle.setButtonTintList(android.content.res.ColorStateList.valueOf(GOLD));
        voiceToggle.setChecked(voiceEnabled);
        voiceToggle.setOnCheckedChangeListener((button, checked) -> {
            voiceEnabled = checked;
            getPreferences(MODE_PRIVATE).edit().putBoolean("voice_enabled", checked).apply();
        });
        CheckBox vibrationToggle = new CheckBox(this);
        vibrationToggle.setText("Vibration");
        vibrationToggle.setTextColor(WHITE);
        vibrationToggle.setButtonTintList(android.content.res.ColorStateList.valueOf(GOLD));
        vibrationToggle.setChecked(vibrationEnabled);
        vibrationToggle.setOnCheckedChangeListener((button, checked) -> {
            vibrationEnabled = checked;
            getPreferences(MODE_PRIVATE).edit().putBoolean("vibration_enabled", checked).apply();
        });
        feedback.addView(voiceToggle, new LinearLayout.LayoutParams(0, dp(44), 1));
        feedback.addView(vibrationToggle, new LinearLayout.LayoutParams(0, dp(44), 1));
        root.addView(feedback);
        Button testVoice=actionButton("TEST VOICE",CARD,GOLD);LinearLayout.LayoutParams testVoiceParams=new LinearLayout.LayoutParams(-1,dp(42));testVoiceParams.topMargin=dp(4);root.addView(testVoice,testVoiceParams);testVoice.setOnClickListener(v->{if(speechReady&&speech!=null)speech.speak("Q BOIX voice assistant is ready. Sprint. Easy jog. Keep moving.",TextToSpeech.QUEUE_FLUSH,null,"qboix_fartlek_test");else Toast.makeText(this,"Voice engine is still starting. Try again in a moment.",Toast.LENGTH_SHORT).show();});

        TextView note=label("Fartlek interval timer | GPS map tracking is available from the other training modes.",GREY,11,false);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);
        np.topMargin=dp(14); root.addView(note,np);

        setContentView(root);
        updateDisplay();
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
        completedRepetitions=0; elapsedSeconds=0; phaseElapsedSeconds=0; while(intervalSplits.length()>0)intervalSplits.remove(0); workoutStartedAt=System.currentTimeMillis(); recoveryPhase=false;
        remainingSeconds=sprintSeconds; running=true; paused=false;
        startGpsTracking();
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

    @Override public void onBackPressed() {
        if(running) confirmStop(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        if (speech != null) {
            speech.stop();
            speech.shutdown();
        }
        super.onDestroy();
    }
}
