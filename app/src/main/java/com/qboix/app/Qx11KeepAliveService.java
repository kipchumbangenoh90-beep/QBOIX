package com.qboix.app;
import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
public class Qx11KeepAliveService extends Service {
 private static final String CHANNEL="qboix_watch_connection";
 @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Q-BOIX watch connection",NotificationManager.IMPORTANCE_LOW);c.setDescription("Keeps the QX11 watch session active.");getSystemService(NotificationManager.class).createNotificationChannel(c);}}
 @Override public int onStartCommand(Intent intent,int flags,int startId){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);Notification n=b.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("Q-BOIX • QX11 watch session").setContentText("Keeping smartwatch connection active").setOngoing(true).build();startForeground(811,n);return START_STICKY;}
 @Override public IBinder onBind(Intent intent){return null;}
}