package com.qboix.app;

import android.app.Activity;
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

    private EditText sprintInput, recoveryInput, repetitionsInput;
    private TextView phaseText, timerText, progressText, elapsedText;
    private Button startButton, pauseButton, stopButton;
    private int sprintSeconds, recoverySeconds, targetRepetitions;
    private int completedRepetitions, remainingSeconds, elapsedSeconds;
    private boolean running = false, paused = false, recoveryPhase = false;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running || paused) return;
            elapsedSeconds++;
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                if (!recoveryPhase) {
                    recoveryPhase = true;
                    remainingSeconds = recoverySeconds;
                    phaseText.setText("RECOVERY • EASY JOG");
                    announce("Cool down! Cool down!");
                    cueVibration();
                } else {
                    completedRepetitions++;
                    if (completedRepetitions >= targetRepetitions) {
                        finishWorkout();
                        return;
                    }
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
            if (speechReady) speech.setLanguage(java.util.Locale.US);
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

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.HORIZONTAL);
        fields.setPadding(0,dp(20),0,dp(8));
        sprintInput = numberField("Sprint seconds","45");
        recoveryInput = numberField("Jog seconds","15");
        fields.addView(sprintInput,new LinearLayout.LayoutParams(0,dp(58),1));
        fields.addView(spaceWidth(10));
        fields.addView(recoveryInput,new LinearLayout.LayoutParams(0,dp(58),1));
        root.addView(fields);

        repetitionsInput = numberField("Repeats","8");
        root.addView(repetitionsInput,new LinearLayout.LayoutParams(-1,dp(58)));

        LinearLayout display = new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setGravity(Gravity.CENTER);
        display.setPadding(dp(18),dp(28),dp(18),dp(28));
        display.setBackground(panel());
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1,0,1);
        dp.topMargin=dp(22); dp.bottomMargin=dp(18);
        root.addView(display,dp);

        phaseText = label("READY",GOLD,18,true);
        phaseText.setGravity(Gravity.CENTER);
        display.addView(phaseText);
        timerText = label("00:45",WHITE,56,true);
        timerText.setGravity(Gravity.CENTER);
        display.addView(timerText);
        progressText = label("0 / 8 intervals",GREY,14,false);
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
        stopButton = actionButton("STOP",CARD,WHITE);
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

        TextView note=label("Timer only • GPS distance and pace tracking will be added separately.",GREY,11,false);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);
        np.topMargin=dp(14); root.addView(note,np);

        setContentView(root);
        updateDisplay();
    }

    private EditText numberField(String hint,String value) {
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setInputType(2);
        input.setText(value);
        input.setHint(hint);
        input.setHintTextColor(GREY);
        input.setTextColor(WHITE);
        input.setTextSize(14);
        input.setPadding(dp(12),0,dp(12),0);
        input.setBackground(panel());
        return input;
    }

    private void startOrResume() {
        if (running && paused) {
            paused=false; startButton.setText("WORKOUT RUNNING");
            handler.postDelayed(ticker,1000);
            return;
        }
        if (running) return;
        try {
            sprintSeconds=Integer.parseInt(sprintInput.getText().toString().trim());
            recoverySeconds=Integer.parseInt(recoveryInput.getText().toString().trim());
            targetRepetitions=Integer.parseInt(repetitionsInput.getText().toString().trim());
        } catch(Exception e) {
            Toast.makeText(this,"Enter valid numbers for all fields.",Toast.LENGTH_SHORT).show();
            return;
        }
        if(sprintSeconds<1||sprintSeconds>3600||recoverySeconds<1||recoverySeconds>3600||targetRepetitions<1||targetRepetitions>100) {
            Toast.makeText(this,"Use 1–3600 seconds and 1–100 repetitions.",Toast.LENGTH_LONG).show();
            return;
        }
        completedRepetitions=0; elapsedSeconds=0; recoveryPhase=false;
        remainingSeconds=sprintSeconds; running=true; paused=false;
        sprintInput.setEnabled(false); recoveryInput.setEnabled(false); repetitionsInput.setEnabled(false);
        phaseText.setText("SPRINT • RUN FAST");
        startButton.setText("WORKOUT RUNNING");
        handler.postDelayed(ticker,1000);
        updateDisplay();
    }

    private void pauseWorkout() {
        if(!running||paused) return;
        paused=true; handler.removeCallbacks(ticker);
        phaseText.setText("PAUSED");
        startButton.setText("RESUME WORKOUT");
    }

    private void confirmStop() {
        if(!running) { finish(); return; }
        new AlertDialog.Builder(this).setTitle("Stop workout?")
            .setMessage("This interval session will not be saved as completed.")
            .setNegativeButton("Continue",null)
            .setPositiveButton("Stop",(d,w)->resetWorkout()).show();
    }

    private void resetWorkout() {
        handler.removeCallbacks(ticker);
        running=false; paused=false; completedRepetitions=0; elapsedSeconds=0;
        sprintInput.setEnabled(true); recoveryInput.setEnabled(true); repetitionsInput.setEnabled(true);
        phaseText.setText("READY"); startButton.setText("START WORKOUT");
        updateDisplay();
    }

    private void finishWorkout() {
        handler.removeCallbacks(ticker);
        running=false; paused=false;
        WorkoutStore.saveFartlek(this,sprintSeconds,recoverySeconds,targetRepetitions,elapsedSeconds);
        phaseText.setText("WORKOUT COMPLETE");
        timerText.setText("DONE");
        progressText.setText(targetRepetitions+" / "+targetRepetitions+" intervals");
        elapsedText.setText("Elapsed "+formatTime(elapsedSeconds));
        startButton.setText("START AGAIN");
        sprintInput.setEnabled(true); recoveryInput.setEnabled(true); repetitionsInput.setEnabled(true);
        Toast.makeText(this,"Fartlek workout saved on this device.",Toast.LENGTH_LONG).show();
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
            int s=readPreview(sprintInput,45);
            timerText.setText(formatTime(s));
            progressText.setText("0 / "+safePreviewReps()+" intervals");
            elapsedText.setText("Elapsed "+formatTime(elapsedSeconds));
            return;
        }
        timerText.setText(formatTime(remainingSeconds));
        progressText.setText(completedRepetitions+" / "+targetRepetitions+" intervals");
        elapsedText.setText("Elapsed "+formatTime(elapsedSeconds));
    }

    private int readPreview(EditText e,int fallback) {
        try { return Math.max(0,Integer.parseInt(e.getText().toString())); }
        catch(Exception ex) { return fallback; }
    }
    private int safePreviewReps() { return readPreview(repetitionsInput,8); }
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
