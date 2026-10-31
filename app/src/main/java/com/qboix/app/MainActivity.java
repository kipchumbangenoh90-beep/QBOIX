package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private FrameLayout root;
    private VideoView video;

    private FirebaseAuth auth;

    private boolean navigationStarted = false;
    private boolean activityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        setupFullscreen();
        setupVideoScreen();

        playStage("video1.mp4", () ->
                playStage("video2.mp4", this::openNextScreen)
        );
    }

    private void setupFullscreen() {

        Window window = getWindow();

        window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void setupVideoScreen() {

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#020204"));

        video = new VideoView(this);

        FrameLayout.LayoutParams videoParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                );

        root.addView(video, videoParams);

        setContentView(root);
    }

    private void playStage(String assetName, Runnable onDone) {

        if (activityDestroyed || navigationStarted) {
            return;
        }

        String videoPath = copyAssetToCache(assetName);

        if (videoPath == null) {
            onDone.run();
            return;
        }

        try {

            video.setOnPreparedListener(mediaPlayer -> {

                if (!activityDestroyed && !navigationStarted) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    video.start();
                }
            });

            video.setOnCompletionListener(mediaPlayer -> {
                if (!activityDestroyed && !navigationStarted) {
                    onDone.run();
                }
            });

            video.setOnErrorListener((mediaPlayer, what, extra) -> {

                if (!activityDestroyed && !navigationStarted) {
                    onDone.run();
                }

                return true;
            });

            video.setVideoPath(videoPath);

        } catch (Exception e) {

            if (!activityDestroyed && !navigationStarted) {
                onDone.run();
            }
        }
    }

    private void openNextScreen() {

        if (navigationStarted || activityDestroyed) {
            return;
        }

        navigationStarted = true;

        if (video != null) {
            video.stopPlayback();
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        user.reload().addOnCompleteListener(this, task -> {

            if (isFinishing() || isDestroyed()) {
                return;
            }

            FirebaseUser refreshedUser = auth.getCurrentUser();

            if (task.isSuccessful()
                    && refreshedUser != null
                    && refreshedUser.isEmailVerified()) {

                openDashboard();

            } else {

                auth.signOut();
                openLogin();
            }
        });
    }

    private void openLogin() {

        Intent intent = new Intent(
                MainActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void openDashboard() {

        Intent intent = new Intent(
                MainActivity.this,
                DashboardActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private String copyAssetToCache(String assetName) {

        File outputFile = new File(
                getCacheDir(),
                assetName
        );

        try (
                InputStream input = getAssets().open(assetName);
                OutputStream output = new FileOutputStream(outputFile)
        ) {

            byte[] buffer = new byte[8192];

            int length;

            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }

            output.flush();

            return outputFile.getAbsolutePath();

        } catch (Exception e) {

            return null;
        }
    }

    @Override
    protected void onDestroy() {

        activityDestroyed = true;

        if (video != null) {
            video.stopPlayback();
        }

        super.onDestroy();
    }
}
