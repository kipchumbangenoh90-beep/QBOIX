package com.qboix.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkoutHistoryActivity extends Activity {
    private final int GOLD=Color.rgb(215,181,91);
    private final int BLACK=Color.rgb(9,9,13);
    private final int CARD=Color.rgb(27,27,34);
    private final int WHITE=Color.WHITE;
    private final int GREY=Color.rgb(165,165,175);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        renderHistory();
    }

    private void renderHistory() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BLACK);
        root.setPadding(dp(20),dp(24),dp(20),dp(20));

        TextView brand=text("Q-BOIX",GOLD,25,true);
        root.addView(brand);
        TextView heading=text("WORKOUT HISTORY",WHITE,21,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
        hp.topMargin=dp(20); root.addView(heading,hp);
        TextView subtitle=text("Your completed Fartlek sessions saved on this device.",GREY,13,false);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
        sp.topMargin=dp(7); root.addView(subtitle,sp);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-1);
        lp.topMargin=dp(18);
        root.addView(scroll,lp);
        scroll.addView(list);

        JSONArray sessions=WorkoutStore.getFartlekSessions(this);
        if(sessions.length()==0) {
            LinearLayout empty=new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18),dp(35),dp(18),dp(35));
            empty.setBackground(panel());
            TextView title=text("NO WORKOUTS YET",GOLD,16,true);
            title.setGravity(Gravity.CENTER);
            empty.addView(title);
            TextView detail=text("Complete a Fartlek session and it will appear here.",GREY,13,false);
            detail.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);
            ep.topMargin=dp(9); empty.addView(detail,ep);
            list.addView(empty);
        } else {
            TextView count=text(sessions.length()+" COMPLETED SESSION"+(sessions.length()==1?"":"S"),GOLD,12,true);
            list.addView(count);
            for(int i=sessions.length()-1;i>=0;i--) {
                JSONObject item=sessions.optJSONObject(i);
                if(item==null) continue;
                long savedAt=item.optLong("savedAt",0);
                int sprint=item.optInt("sprintSeconds",0);
                int recovery=item.optInt("recoverySeconds",0);
                int reps=item.optInt("repetitions",0);
                int elapsed=item.optInt("elapsedSeconds",0);
                String date=savedAt>0
                        ?new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm",Locale.getDefault()).format(new Date(savedAt))
                        :"Date unavailable";
                LinearLayout card=new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(16),dp(15),dp(16),dp(15));
                card.setBackground(panel());
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
                cp.topMargin=dp(11); list.addView(card,cp);
                card.addView(text("FARTLEK SESSION",GOLD,14,true));
                TextView dateText=text(date,GREY,11,false);
                LinearLayout.LayoutParams dateP=new LinearLayout.LayoutParams(-1,-2);
                dateP.topMargin=dp(5); card.addView(dateText,dateP);
                TextView config=text("Sprint "+formatTime(sprint)+"  •  Recovery "+formatTime(recovery),WHITE,13,true);
                LinearLayout.LayoutParams configP=new LinearLayout.LayoutParams(-1,-2);
                configP.topMargin=dp(13); card.addView(config,configP);
                TextView result=text(reps+" repetitions  •  Total time "+formatTime(elapsed),GREY,12,false);
                LinearLayout.LayoutParams resultP=new LinearLayout.LayoutParams(-1,-2);
                resultP.topMargin=dp(5); card.addView(result,resultP);
            }
        }
        setContentView(root);
    }

    private String formatTime(int seconds) {
        return String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60);
    }
    private TextView text(String value,int color,float size,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private GradientDrawable panel() {
        GradientDrawable d=new GradientDrawable(); d.setColor(CARD); d.setCornerRadius(dp(14)); return d;
    }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
}
