package com.qboix.app;

import android.app.*;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import java.util.List;
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

    public static synchronized boolean hasActiveConnection() {
        return retainedOwner != null && retainedGatt != null;
    }

    /** Verify the GATT device is still connected; a saved flag is not proof. */
    public static synchronized boolean hasActiveConnection(Context context) {
        if (retainedOwner == null || retainedGatt == null) return false;
        try {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                clearStaleConnection(context);
                return false;
            }
            BluetoothDevice device = retainedGatt.getDevice();
            List<BluetoothDevice> connected = manager.getConnectedDevices(BluetoothProfile.GATT);
            boolean found = false;
            if (device != null && connected != null) {
                String address = device.getAddress();
                for (BluetoothDevice item : connected) {
                    if (item != null && address.equalsIgnoreCase(item.getAddress())) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) clearStaleConnection(context);
            return found;
        } catch (SecurityException | IllegalStateException e) {
            clearStaleConnection(context);
            return false;
        }
    }

    private static void clearStaleConnection(Context context) {
        WatchActivity owner = retainedOwner;
        BluetoothGatt gatt = retainedGatt;
        retainedOwner = null;
        retainedGatt = null;
        WatchDataStore.setConnected(context, false);
        if (gatt != null) {
            try { gatt.disconnect(); } catch (Exception ignored) {}
            try { gatt.close(); } catch (Exception ignored) {}
        }
        if (owner != null) {
            try { owner.runOnUiThread(owner::onWatchConnectionLost); } catch (Exception ignored) {}
            try { owner.stopService(new Intent(owner, Qx11KeepAliveService.class)); } catch (Exception ignored) {}
        }
    }

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