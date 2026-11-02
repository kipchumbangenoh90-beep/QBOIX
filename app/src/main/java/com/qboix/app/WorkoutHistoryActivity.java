package com.qboix.app;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkoutHistoryActivity extends Activity {
 private final int BLUE=Color.rgb(32,112,190),GREEN=Color.rgb(30,150,105),GOLD=Color.rgb(190,145,40),INK=Color.rgb(28,38,50),MUTED=Color.rgb(105,119,132),BG=Color.rgb(247,249,252);
 private Calendar month=Calendar.getInstance(),selected=Calendar.getInstance();
 private LinearLayout root,calendarGrid,records;
 private TextView monthLabel,dayLabel;
 private final ArrayList<JSONObject> all=new ArrayList<>();
 private final SimpleDateFormat dayFmt=new SimpleDateFormat("yyyyMMdd",Locale.US),dateFmt=new SimpleDateFormat("EEE, dd MMM yyyy",Locale.getDefault()),timeFmt=new SimpleDateFormat("HH:mm",Locale.getDefault()),monthFmt=new SimpleDateFormat("MMMM yyyy",Locale.getDefault());

 @Override protected void onCreate(Bundle b){super.onCreate(b);loadRecords();if(!all.isEmpty()){long t=all.get(all.size()-1).optLong("startedAt",all.get(all.size()-1).optLong("savedAt",System.currentTimeMillis()));selected.setTimeInMillis(t);month.setTimeInMillis(t);}render();}
 private void loadRecords(){JSONArray a=WorkoutStore.getFartlekSessions(this),b=WorkoutStore.getRunSessions(this);for(int i=0;i<a.length();i++)if(a.optJSONObject(i)!=null)all.add(a.optJSONObject(i));for(int i=0;i<b.length();i++)if(b.optJSONObject(i)!=null)all.add(b.optJSONObject(i));Collections.sort(all,(x,y)->Long.compare(startOf(x),startOf(y)));}
 private long startOf(JSONObject o){return o.optLong("startedAt",o.optLong("savedAt",0));}
 private void render(){
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(22),dp(18),dp(24));root.setBackgroundColor(BG);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);
  addText(root,"Q-BOIX",BLUE,24,true);addText(root,"WORKOUT HISTORY",INK,21,true).setPadding(0,dp(14),0,dp(3));
  addText(root,"Your training calendar and daily performance",MUTED,13,false);
  LinearLayout monthRow=new LinearLayout(this);monthRow.setGravity(Gravity.CENTER_VERTICAL);monthRow.setPadding(dp(12),dp(9),dp(12),dp(9));monthRow.setBackground(card());LinearLayout.LayoutParams mr=new LinearLayout.LayoutParams(-1,-2);mr.topMargin=dp(18);root.addView(monthRow,mr);
  Button prev=smallButton("‹");monthRow.addView(prev);prev.setOnClickListener(v->{month.add(Calendar.MONTH,-1);render();});
  monthLabel=new TextView(this);monthLabel.setText(monthFmt.format(month.getTime()));monthLabel.setTextColor(INK);monthLabel.setTextSize(16);monthLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);monthLabel.setGravity(Gravity.CENTER);monthRow.addView(monthLabel,new LinearLayout.LayoutParams(0,-2,1));
  Button next=smallButton("›");monthRow.addView(next);next.setOnClickListener(v->{month.add(Calendar.MONTH,1);render();});
  calendarGrid=new LinearLayout(this);calendarGrid.setOrientation(1);calendarGrid.setPadding(dp(8),dp(10),dp(8),dp(10));calendarGrid.setBackground(card());LinearLayout.LayoutParams cg=new LinearLayout.LayoutParams(-1,-2);cg.topMargin=dp(9);root.addView(calendarGrid,cg);
  LinearLayout names=new LinearLayout(this);String[] days={"S","M","T","W","T","F","S"};for(String d:days){TextView t=new TextView(this);t.setText(d);t.setTextColor(MUTED);t.setTextSize(12);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);names.addView(t,new LinearLayout.LayoutParams(0,dp(30),1));}calendarGrid.addView(names);
  Calendar first=(Calendar)month.clone();first.set(Calendar.DAY_OF_MONTH,1);int offset=first.get(Calendar.DAY_OF_WEEK)-1;int count=month.getActualMaximum(Calendar.DAY_OF_MONTH);
  for(int row=0;row<6;row++){LinearLayout line=new LinearLayout(this);for(int col=0;col<7;col++){int day=row*7+col-offset+1;TextView cell=new TextView(this);cell.setGravity(Gravity.CENTER);cell.setTextSize(13);if(day>0&&day<=count){Calendar d=(Calendar)month.clone();d.set(Calendar.DAY_OF_MONTH,day);boolean has=hasDay(d);boolean chosen=dayFmt.format(d.getTime()).equals(dayFmt.format(selected.getTime()));cell.setText((has?"•\n":"")+day);cell.setTextColor(chosen?Color.WHITE:has?BLUE:INK);if(chosen)cell.setBackground(round(BLUE));cell.setOnClickListener(v->{selected.setTimeInMillis(d.getTimeInMillis());render();});}line.addView(cell,new LinearLayout.LayoutParams(0,dp(42),1));}calendarGrid.addView(line);}
  dayLabel=addText(root,dateFmt.format(selected.getTime()).toUpperCase(Locale.getDefault()),INK,16,true);LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-1,-2);dl.topMargin=dp(18);dayLabel.setLayoutParams(dl);
  ArrayList<JSONObject> daily=new ArrayList<>();for(JSONObject o:all)if(sameDay(startOf(o),selected.getTimeInMillis()))daily.add(o);
  double km=0;int seconds=0;for(JSONObject o:daily){km+=o.optDouble("distanceMeters",0)/1000.0;seconds+=o.optInt("elapsedSeconds",0);}
  LinearLayout stats=new LinearLayout(this);stats.setPadding(dp(12),dp(13),dp(12),dp(13));stats.setBackground(card());LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(-1,-2);st.topMargin=dp(9);root.addView(stats,st);
  stat(stats,String.valueOf(daily.size()),"ACTIVITIES");stat(stats,String.format(Locale.getDefault(),"%.2f km",km),"DISTANCE");stat(stats,format(seconds),"DURATION");
  addText(root,"DAILY PERFORMANCE",BLUE,12,true).setPadding(0,dp(17),0,dp(7));
  LinearLayout graph=new LinearLayout(this);graph.setOrientation(1);graph.setPadding(dp(13),dp(12),dp(13),dp(12));graph.setBackground(card());root.addView(graph);
  addBar(graph,"Distance",km,"km");addBar(graph,"Duration",seconds/60.0,"min");
  addText(root,"ACTIVITY TIMELINE",BLUE,12,true).setPadding(0,dp(18),0,dp(7));
  records=new LinearLayout(this);records.setOrientation(1);root.addView(records);
  if(daily.isEmpty()){TextView empty=addText(records,"No activities recorded on this date. Choose a marked day to view its sessions.",MUTED,13,false);empty.setPadding(dp(14),dp(18),dp(14),dp(18));empty.setBackground(card());}
  for(int i=daily.size()-1;i>=0;i--)activityCard(daily.get(i));
  Button today=smallButton("TODAY");today.setOnClickListener(v->{selected=Calendar.getInstance();month=Calendar.getInstance();render();});LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(44));tp.topMargin=dp(16);root.addView(today,tp);
 }
 private boolean hasDay(Calendar d){for(JSONObject o:all)if(sameDay(startOf(o),d.getTimeInMillis()))return true;return false;}
 private boolean sameDay(long a,long b){return dayFmt.format(new Date(a)).equals(dayFmt.format(new Date(b)));}
 private void activityCard(JSONObject o){
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(13),dp(14),dp(13));card.setBackground(card());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(9);records.addView(card,cp);
  String type=o.optString("activityType",o.optString("type","WORKOUT"));addText(card,type.replace('_',' '),BLUE,14,true);
  long start=startOf(o),end=o.optLong("endedAt",o.optLong("savedAt",start));addText(card,timeFmt.format(new Date(start))+" – "+timeFmt.format(new Date(end)),MUTED,12,false).setPadding(0,dp(4),0,dp(8));
  int sec=o.optInt("elapsedSeconds",0);double metres=o.optDouble("distanceMeters",0);
  addText(card,String.format(Locale.getDefault(),"%.2f km   •   %.2f mi",metres/1000.0,metres/1609.344),INK,19,true);
  addText(card,"Duration "+format(sec)+"   •   "+(sec>0&&metres>0?String.format(Locale.getDefault(),"%.1f km/h",metres/1000.0/(sec/3600.0)):"Speed not recorded"),MUTED,12,false).setPadding(0,dp(5),0,dp(3));
  if("FARTLEK".equalsIgnoreCase(type)){addText(card,"Sprint "+format(o.optInt("sprintSeconds",0))+"  •  Recovery "+format(o.optInt("recoverySeconds",0))+"  •  "+o.optInt("repetitions",0)+" repetitions",MUTED,12,false);addText(card,"GPS, distance and interval pace can be added when watch integration is connected.",MUTED,11,false).setPadding(0,dp(5),0,0);}
  else {double pace=o.optDouble("averagePaceSecondsPerKm",0);addText(card,pace>0?"Average pace "+pace(pace)+" /km":"Pace not recorded",MUTED,12,false);JSONArray route=o.optJSONArray("route");if(route!=null&&route.length()>1){RouteView map=new RouteView(route);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(145));mp.topMargin=dp(10);card.addView(map,mp);addText(card,"RECORDED GPS ROUTE • "+route.length()+" points",GREEN,10,true);}else addText(card,"No GPS route points recorded.",MUTED,11,false);}
 }
 private void addBar(LinearLayout box,String label,double value,String unit){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);box.addView(row);TextView l=addText(row,label,INK,12,true);l.setLayoutParams(new LinearLayout.LayoutParams(dp(85),-2));addText(row,String.format(Locale.getDefault(),"%.1f %s",value,unit),MUTED,11,false);View gap=new View(this);box.addView(gap,new LinearLayout.LayoutParams(1,dp(7)));}
 private void stat(LinearLayout row,String value,String label){LinearLayout col=new LinearLayout(this);col.setOrientation(1);col.setGravity(Gravity.CENTER);row.addView(col,new LinearLayout.LayoutParams(0,-2,1));addText(col,value,INK,16,true);addText(col,label,MUTED,9,true);}
 private TextView addText(LinearLayout p,String s,int color,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);p.addView(t);return t;}
 private Button smallButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(BLUE);b.setTextSize(12);b.setAllCaps(false);b.setMinWidth(dp(42));return b;}
 private GradientDrawable card(){GradientDrawable d=new GradientDrawable();d.setColor(Color.WHITE);d.setCornerRadius(dp(15));d.setStroke(dp(1),Color.rgb(229,235,242));return d;}
 private GradientDrawable round(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(10));return d;}
 private String format(int s){return String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60);}
 private String pace(double p){int s=(int)p;return String.format(Locale.getDefault(),"%d:%02d",s/60,s%60);}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 private class RouteView extends View{
  JSONArray route;Paint paint=new Paint(3);RouteView(JSONArray a){super(WorkoutHistoryActivity.this);route=a;setBackground(round(Color.rgb(239,246,250)));}
  protected void onDraw(Canvas c){super.onDraw(c);if(route.length()<2)return;double minLat=90,maxLat=-90,minLon=180,maxLon=-180;for(int i=0;i<route.length();i++){JSONObject p=route.optJSONObject(i);if(p!=null){double lat=p.optDouble("lat"),lon=p.optDouble("lon");minLat=Math.min(minLat,lat);maxLat=Math.max(maxLat,lat);minLon=Math.min(minLon,lon);maxLon=Math.max(maxLon,lon);}}float pad=dp(15),w=getWidth()-2*pad,h=getHeight()-2*pad;Path path=new Path();for(int i=0;i<route.length();i++){JSONObject p=route.optJSONObject(i);if(p==null)continue;float x=pad+(float)((p.optDouble("lon")-minLon)/Math.max(0.000001,maxLon-minLon))*w;float y=pad+h-(float)((p.optDouble("lat")-minLat)/Math.max(0.000001,maxLat-minLat))*h;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}paint.setColor(BLUE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);c.drawPath(path,paint);}
 }
}