package com.qboix.app;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private FrameLayout root;
    private VideoView video;
    private TextView endCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen, black, no status bar distractions
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#020204"));

        video = new VideoView(this);
        FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        root.addView(video, vp);

        endCard = new TextView(this);
        endCard.setText("Q-BOIX\nDesire to Achieve\n\nBoot complete");
        endCard.setTextColor(Color.parseColor("#FFE4A0"));
        endCard.setTextSize(24);
        endCard.setGravity(Gravity.CENTER);
        endCard.setVisibility(View.GONE);
        root.addView(endCard, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        setContentView(root);

        playStage("video1.mp4", () -> playStage("video2.mp4", this::showEnd));
    }

    // Plays one video from the assets folder, then runs the next step
    private void playStage(String assetName, Runnable onDone) {
        String path = copyAssetToCache(assetName);
        if (path == null) {
            onDone.run();
            return;
        }

        video.setVideoPath(path);
        video.setOnPreparedListener(mp -> {
            mp.setVolume(1f, 1f);   // full volume, so video 2 has sound
            video.start();
        });
        video.setOnCompletionListener(mp -> onDone.run());
        video.setOnErrorListener((mp, what, extra) -> {
            onDone.run();
            return true;
        });
    }

    private void showEnd() {
        video.stopPlayback();
        video.setVisibility(View.GONE);
        endCard.setVisibility(View.VISIBLE);
    }

    // VideoView needs a real file, so copy the asset into the app's cache first
    private String copyAssetToCache(String name) {
        File out = new File(getCacheDir(), name);
        try (InputStream in = getAssets().open(name);
             OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
