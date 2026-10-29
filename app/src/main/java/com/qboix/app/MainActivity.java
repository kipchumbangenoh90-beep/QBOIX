package com.qboix.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setText("Q-BOIX\nRun. Track. Train. Improve.");
        t.setTextColor(Color.parseColor("#FFE4A0"));
        t.setTextSize(28);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(Color.parseColor("#020204"));
        setContentView(t);
    }
}
