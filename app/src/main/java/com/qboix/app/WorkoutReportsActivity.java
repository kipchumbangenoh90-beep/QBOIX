package com.qboix.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.content.*;
import android.view.Gravity;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkoutReportsActivity extends Activity {
    private final int BG=Color.rgb(7,17,31), CARD=Color.rgb(18,48,74), GOLD=Color.rgb(233,185,73),
            BLUE=Color.rgb(22,139,255), TEAL=Color.rgb(0,214,201), GREEN=Color.rgb(64,216,120),
            WHITE=Color.WHITE, MUTED=Color.rgb(177,194,210);
    private final ArrayList<JSONObject> runs=new ArrayList<>();
    private LinearLayout list;

    @Override public void onCreate(Bundle state){super.onCreate(state);JSONArray a=WorkoutStore.getRunSessions(this);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)runs.add(o);}Collections.sort(runs,(x,y)->Long.compare(y.optLong("savedAt"),x.optLong("savedAt")));render();}
    private void render(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(24),dp(18),dp(30));root.setBackgroundColor(BG);scroll.addView(root);
        TextView brand=txt("Q-BOIX",GOLD,28,true);root.addView(brand);root.addView(txt("PERFORMANCE REPORTS",WHITE,20,true));root.addView(txt("Portrait 9:16 • Original saved workout data • PDF export",MUTED,12,false));
        TextView note=txt("Your reports are generated from saved sessions. Exported PDFs are separate copies; your workout records remain unchanged.",TEAL,12,false);note.setPadding(dp(12),dp(12),dp(12),dp(12));note.setBackground(panel());LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(16);root.addView(note,np);
        list=new LinearLayout(this);list.setOrientation(1);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(18);root.addView(list,lp);
        if(runs.isEmpty()){list.addView(txt("NO GPS REPORTS YET",GOLD,16,true));list.addView(txt("Complete and save a GPS run to create your first report.",MUTED,13,false));}
        for(int i=0;i<runs.size();i++){final JSONObject run=runs.get(i);LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(15),dp(15),dp(15),dp(15));card.setBackground(panel());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(12);list.addView(card,cp);
            long stamp=run.optLong("savedAt");String date=stamp>0?new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm",Locale.getDefault()).format(new Date(stamp)):"Date unavailable";
            card.addView(txt(date,GOLD,12,true));card.addView(txt(String.format(Locale.getDefault(),"%.2f km",run.optDouble("distanceMeters")/1000),WHITE,24,true));
            card.addView(txt("Time "+time(run.optInt("elapsedSeconds"))+"  •  Pace "+pace(run.optDouble("averagePaceSecondsPerKm")),MUTED,12,false));
            card.addView(txt(WorkoutStore.verifyRun(run)?"RECORD CHECK: VERIFIED":"RECORD CHECK: UNVERIFIED / LEGACY",WorkoutStore.verifyRun(run)?GREEN:MUTED,11,true));
            LinearLayout actions=new LinearLayout(this);Button preview=btn("CREATE PDF",BLUE);Button share=btn("SHARE PDF",GREEN);actions.addView(preview,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(48),1);bp.leftMargin=dp(8);actions.addView(share,bp);card.addView(actions);preview.setOnClickListener(v->savePdf(run));share.setOnClickListener(v->sharePdf(run));
        }
        setContentView(scroll);
    }
    private void savePdf(JSONObject run){File f=makePdf(run);if(f==null)return;try{Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");intent.putExtra(Intent.EXTRA_TITLE,fileName(run));savePending=f;startActivityForResult(intent,41);}catch(Exception e){message("Could not open the save dialog: "+e.getMessage());}}
    private File savePending;
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==41&&result==RESULT_OK&&data!=null&&data.getData()!=null&&savePending!=null){try(InputStream in=new FileInputStream(savePending);OutputStream out=getContentResolver().openOutputStream(data.getData())){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);message("PDF saved successfully.");}catch(Exception e){message("Could not save PDF: "+e.getMessage());}}}
    private void sharePdf(JSONObject run){File f=makePdf(run);if(f==null)return;try{Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/pdf");send.putExtra(Intent.EXTRA_STREAM,uri);send.putExtra(Intent.EXTRA_SUBJECT,"Q-BOIX Running Report");send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(send,"Share Q-BOIX report"));}catch(Exception e){message("Could not share PDF: "+e.getMessage());}}
    private File makePdf(JSONObject run){
        PdfDocument pdf=new PdfDocument();int w=595,h=1058;PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(w,h,1).create());Canvas c=page.getCanvas();Paint p=new Paint(3);c.drawColor(BG);
        p.setColor(GOLD);p.setTextSize(34);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText("Q-BOIX",42,68,p);
        p.setColor(TEAL);p.setTextSize(13);c.drawText("ATHLETE PERFORMANCE REPORT",42,96,p);
        p.setColor(BLUE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);Path routePath=routePath(run.optJSONArray("route"),w-90,210);c.save();c.translate(45,120);c.drawPath(routePath,p);c.restore();p.setStyle(Paint.Style.FILL);
        p.setColor(WHITE);p.setTextSize(27);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("RUN PERFORMANCE",42,375,p);
        long stamp=run.optLong("savedAt");p.setColor(MUTED);p.setTextSize(13);c.drawText(stamp>0?new SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm",Locale.getDefault()).format(new Date(stamp)):"Date unavailable",42,402,p);
        double metres=run.optDouble("distanceMeters"),km=metres/1000;int elapsed=run.optInt("elapsedSeconds");double pace=run.optDouble("averagePaceSecondsPerKm");
        String[] labels={"DISTANCE","TIME","AVERAGE PACE","CALORIES"};String[] values={String.format(Locale.getDefault(),"%.2f km",km),time(elapsed),pace(pace),"Not recorded"};
        for(int i=0;i<4;i++){int x=42+(i%2)*260,y=435+(i/2)*105;p.setColor(CARD);c.drawRoundRect(x,y,x+235,y+82,14,14,p);p.setColor(GOLD);p.setTextSize(22);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(values[i],x+14,y+35,p);p.setColor(MUTED);p.setTextSize(11);c.drawText(labels[i],x+14,y+61,p);}
        p.setColor(TEAL);p.setTextSize(16);c.drawText("PACE / SPLIT PERFORMANCE",42,665,p);
        JSONArray splits=run.optJSONArray("kilometreSplits"),laps=run.optJSONArray("lapSplits");JSONArray items=splits!=null&&splits.length()>0?splits:laps;
        p.setColor(MUTED);p.setTextSize(12);int y=696;
        if(items!=null&&items.length()>0){for(int i=0;i<items.length()&&i<8;i++){JSONObject s=items.optJSONObject(i);if(s==null)continue;String label=splits==items?"KM "+s.optInt("kilometre",i+1):"LAP "+s.optInt("lap",i+1);int sec=s.optInt("splitSeconds");c.drawText(label+"     "+time(sec),48,y,p);float width=Math.min(380,sec*2.0f);p.setColor(BLUE);c.drawRoundRect(190,y-12,190+width,y-3,5,5,p);p.setColor(MUTED);y+=28;}}
        else c.drawText("No split or lap data was saved for this session.",48,y,p);
        p.setColor(GOLD);p.setTextSize(15);c.drawText("SESSION DETAILS",42,950,p);p.setColor(MUTED);p.setTextSize(11);
        c.drawText((run.optBoolean("trackMode")?"Track session • "+run.optInt("lapCount")+" laps • "+(int)run.optDouble("trackLengthMeters",400)+"m":"GPS running session"),42,975,p);
        c.drawText("Record check: "+(WorkoutStore.verifyRun(run)?"VERIFIED":"UNVERIFIED / LEGACY"),42,997,p);
        c.drawText("Q-BOIX • First-hand workout data • No values estimated",42,1030,p);
        pdf.finishPage(page);
        File dir=new File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),"Q-BOIX Reports");if(!dir.exists())dir.mkdirs();File f=new File(dir,fileName(run));try(FileOutputStream out=new FileOutputStream(f)){pdf.writeTo(out);pdf.close();return f;}catch(Exception e){pdf.close();message("PDF generation failed: "+e.getMessage());return null;}
    }
    private Path routePath(JSONArray a,int width,int height){Path path=new Path();if(a==null||a.length()<2)return path;double minLat=90,maxLat=-90,minLon=180,maxLon=-180;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null){double la=o.optDouble("lat"),lo=o.optDouble("lon");minLat=Math.min(minLat,la);maxLat=Math.max(maxLat,la);minLon=Math.min(minLon,lo);maxLon=Math.max(maxLon,lo);}}if(maxLat==minLat||maxLon==minLon)return path;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;float x=12+(float)((o.optDouble("lon")-minLon)/(maxLon-minLon))*(width-24),y=height-12-(float)((o.optDouble("lat")-minLat)/(maxLat-minLat))*(height-24);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}return path;}
    private String fileName(JSONObject o){return "Q-BOIX-Run-"+o.optLong("savedAt")+".pdf";}
    private String time(int s){return String.format(Locale.getDefault(),"%02d:%02d:%02d",s/3600,(s%3600)/60,s%60);}
    private String pace(double s){if(s<=0)return "Not recorded";return String.format(Locale.getDefault(),"%d:%02d /km",(int)s,(int)Math.round(s)%60);}
    private TextView txt(String s,int color,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,int color){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTextColor(WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));return b;}
    private android.graphics.drawable.GradientDrawable panel(){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(CARD);d.setCornerRadius(dp(14));return d;}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void message(String s){new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK",null).show();}
}
