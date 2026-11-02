package com.qboix.app;

import android.app.*;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/**
 * Keeps the QX11 BLE owner alive while the user navigates between Q-BOIX screens.
 * The Activity remains strongly referenced for the lifetime of the connected session,
 * so its BluetoothGattCallback and decoder continue receiving watch notifications.
 */
public class Qx11KeepAliveService extends Service {
    private static final String CHANNEL = "qboix_watch_connection";
    private static WatchActivity retainedOwner;
    private static BluetoothGatt retainedGatt;

    public static synchronized boolean hasActiveConnection() { return retainedOwner != null && retainedGatt != null; }

    public static synchronized void retain(WatchActivity owner, BluetoothGatt gatt) {
        retainedOwner = owner;
        retainedGatt = gatt;
        Intent intent = new Intent(owner, Qx11KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= 26) owner.startForegroundService(intent);
        else owner.startService(intent);
    }

    public static synchronized void release(WatchActivity owner, BluetoothGatt gatt) {
        if (retainedOwner == owner && (gatt == null || retainedGatt == gatt)) {
            retainedOwner = null;
            retainedGatt = null;
            owner.stopService(new Intent(owner, Qx11KeepAliveService.class));
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL, "Q-BOIX watch connection", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the QX11 smartwatch session active.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification notification = builder
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Q-BOIX • QX11 watch connected")
            .setContentText("Keeping your smartwatch session active")
            .setOngoing(true).build();
        startForeground(811, notification);
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}