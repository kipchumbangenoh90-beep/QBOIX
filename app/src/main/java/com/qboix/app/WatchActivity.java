package com.qboix.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Q-BOIX BLE diagnostic and live-data screen.
 *
 * Important: Bluetooth connection is not treated as proof that health data arrived.
 * The screen only displays values received from a GATT read or notification.
 * Unknown manufacturer packets are shown as raw hexadecimal data until the exact
 * QX11 firmware protocol is verified.
 */
public class WatchActivity extends Activity {
    private static final int GOLD = Color.rgb(215, 181, 91);
    private static final int GREEN = Color.rgb(55, 190, 125);
    private static final int BG = Color.rgb(9, 12, 20);
    private static final int PANEL = Color.rgb(25, 29, 38);

    private static final UUID HR_SERVICE =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_MEASURE =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_SERVICE =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OX_SERVICE =
            UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OX_CONTINUOUS =
            UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OX_SPOT =
            UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Address recorded for the user's QX11 device. Manual selection remains available.
    private static final String QX11_MAC = "78:02:B7:14:AE:96";

    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;

    private LinearLayout root;
    private LinearLayout deviceList;
    private TextView status;
    private TextView watchName;
    private TextView heart;
    private TextView oxygen;
    private TextView steps;
    private TextView calories;
    private TextView battery;
    private TextView diagnostic;
    private Button scanButton;

    private final Map<String, BluetoothDevice> found = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Queue<GattOperation> operations = new ArrayDeque<>();
    private final StringBuilder diagnosticLog = new StringBuilder();

    private boolean scanning = false;
    private boolean gattBusy = false;
    private int operationToken = 0;
    private int serviceCount = 0;
    private int notificationCount = 0;
    private int readCount = 0;
    private int receivedPacketCount = 0;
    private long lastBatteryReadAt = 0L;

    private static class GattOperation {
        final BluetoothGattCharacteristic characteristic;
        final boolean subscribe;

        GattOperation(BluetoothGattCharacteristic characteristic, boolean subscribe) {
            this.characteristic = characteristic;
            this.subscribe = subscribe;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();

        BluetoothManager manager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();

        if (adapter == null) {
            setStatus("Bluetooth LE is not available on this phone.");
            return;
        }
        requestBlePermissions();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(30));
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(label("Q-BOIX  /  SMARTWATCH", GOLD, 22, true));
        TextView subtitle = label(
                "GLORYFIT LIVE HEALTH CONNECTION", Color.WHITE, 13, true);
        LinearLayout.LayoutParams sp = params(-1, -2);
        sp.topMargin = dp(6);
        root.addView(subtitle, sp);

        LinearLayout connection = panel();
        watchName = label("QX11 WATCH", Color.WHITE, 17, true);
        connection.addView(watchName);
        status = label("Not connected", Color.LTGRAY, 13, false);
        LinearLayout.LayoutParams st = params(-1, -2);
        st.topMargin = dp(5);
        connection.addView(status, st);
        root.addView(connection);

        scanButton = new Button(this);
        scanButton.setText("SCAN FOR MY WATCH");
        scanButton.setAllCaps(false);
        scanButton.setTextColor(Color.BLACK);
        scanButton.setBackground(shape(GOLD, dp(13)));
        LinearLayout.LayoutParams bp = params(-1, dp(50));
        bp.topMargin = dp(12);
        root.addView(scanButton, bp);
        scanButton.setOnClickListener(v -> startScan());

        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams dl = params(-1, -2);
        dl.topMargin = dp(6);
        root.addView(deviceList, dl);

        TextView heading = label("LIVE HEALTH DATA", GOLD, 15, true);
        LinearLayout.LayoutParams hp = params(-1, -2);
        hp.topMargin = dp(22);
        root.addView(heading, hp);
        root.addView(label(
                "Only values actually received from the watch will appear.",
                Color.LTGRAY, 11, false));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(metric("♥", "HEART RATE", "—", "BPM"),
                new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(metric("◉", "BLOOD OXYGEN", "—", "SpO₂"),
                new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams rp = params(-1, -2);
        rp.topMargin = dp(8);
        root.addView(row1, rp);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(metric("ST", "STEPS", "—", "steps"),
                new LinearLayout.LayoutParams(0, -2, 1));
        row2.addView(metric("KCAL", "CALORIES", "—", "kcal"),
                new LinearLayout.LayoutParams(0, -2, 1));
        rp = params(-1, -2);
        rp.topMargin = dp(8);
        root.addView(row2, rp);

        battery = label("BATTERY  •  Not read yet", Color.LTGRAY, 12, false);
        LinearLayout.LayoutParams bat = params(-1, -2);
        bat.topMargin = dp(15);
        root.addView(battery, bat);

        diagnostic = label("Waiting for watch connection.", Color.LTGRAY, 11, false);
        diagnostic.setPadding(0, dp(14), 0, 0);
        root.addView(diagnostic);
    }

    private LinearLayout metric(String icon, String title, String value, String unit) {
        LinearLayout card = panel();
        card.setMinimumHeight(dp(112));
        card.addView(label(icon, GREEN, 16, true));
        TextView t = label(title, Color.LTGRAY, 10, true);
        LinearLayout.LayoutParams tp = params(-1, -2);
        tp.topMargin = dp(5);
        card.addView(t, tp);
        TextView v = label(value, Color.WHITE, 27, true);
        LinearLayout.LayoutParams vp = params(-1, -2);
        vp.topMargin = dp(4);
        card.addView(v, vp);
        card.addView(label(unit, GREEN, 10, false));

        if ("HEART RATE".equals(title)) heart = v;
        if ("BLOOD OXYGEN".equals(title)) oxygen = v;
        if ("STEPS".equals(title)) steps = v;
        if ("CALORIES".equals(title)) calories = v;

        LinearLayout.LayoutParams p = params(-1, -2);
        p.setMargins(dp(4), dp(3), dp(4), dp(3));
        card.setLayoutParams(p);
        return card;
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(13), dp(14), dp(13));
        box.setBackground(shape(PANEL, dp(16)));
        return box;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView label(String value, int color, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private boolean hasPermission(String permission) {
        return Build.VERSION.SDK_INT < 23
                || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            ArrayList<String> missing = new ArrayList<>();
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                missing.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                missing.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!missing.isEmpty()) {
                requestPermissions(missing.toArray(new String[0]), 10);
                return;
            }
        } else if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            return;
        }

        setStatus(adapter.isEnabled()
                ? "Bluetooth ready. Tap scan to find your QX11 watch."
                : "Turn on Bluetooth, then scan.");
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                granted &= result == PackageManager.PERMISSION_GRANTED;
            }
            if (granted) setStatus("Permission granted. Tap scan.");
            else setStatus("Bluetooth permission denied. Allow it in Android settings.");
        }
    }

    private void startScan() {
        if (adapter == null || !adapter.isEnabled()) {
            setStatus("Turn on Bluetooth on your phone first.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31
                && (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)
                || !hasPermission(Manifest.permission.BLUETOOTH_CONNECT))) {
            requestBlePermissions();
            return;
        }

        try {
            scanner = adapter.getBluetoothLeScanner();
            if (scanner == null) {
                setStatus("Bluetooth LE scanner is unavailable.");
                return;
            }

            found.clear();
            deviceList.removeAllViews();
            scanning = true;
            scanButton.setEnabled(false);
            setStatus("Searching nearby for your QX11 watch…");
            scanner.startScan(scanCallback);
            handler.postDelayed(
                    () -> stopScan("Scan complete. Choose your watch if it appears."),
                    12000);
        } catch (SecurityException e) {
            setStatus("Bluetooth permission is required.");
            scanButton.setEnabled(true);
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String address = safeAddress(device);
            if (!found.containsKey(address)) {
                found.put(address, device);
                runOnUiThread(() -> addDevice(device));
            }
            if (QX11_MAC.equalsIgnoreCase(address)) {
                runOnUiThread(() -> {
                    stopScan("Your QX11 watch was found. Connecting…");
                    connect(device);
                });
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() -> {
                setStatus("Bluetooth scan failed (code " + errorCode + ").");
                scanButton.setEnabled(true);
            });
        }
    };

    private void stopScan(String message) {
        if (scanning) {
            scanning = false;
            try {
                if (scanner != null) scanner.stopScan(scanCallback);
            } catch (SecurityException ignored) {
            }
        }
        if (message != null && !message.isEmpty()) setStatus(message);
        if (scanButton != null) scanButton.setEnabled(true);
    }

    private String safeAddress(BluetoothDevice device) {
        try {
            return device.getAddress();
        } catch (SecurityException e) {
            return "";
        }
    }

    private void addDevice(BluetoothDevice device) {
        String name = "Bluetooth device";
        try {
            if (device.getName() != null && !device.getName().trim().isEmpty()) {
                name = device.getName();
            }
        } catch (SecurityException ignored) {
        }

        boolean target = QX11_MAC.equalsIgnoreCase(safeAddress(device));
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setTextColor(target ? Color.BLACK : Color.WHITE);
        button.setText((target ? "YOUR QX11 WATCH  •  " : "") + name);
        button.setBackground(shape(target ? GOLD : PANEL, dp(12)));
        LinearLayout.LayoutParams p = params(-1, dp(48));
        p.topMargin = dp(5);
        deviceList.addView(button, p);
        button.setOnClickListener(v -> connect(device));
    }

    private void connect(BluetoothDevice device) {
        stopScan("");
        closeGatt();
        resetMeasurements();
        operations.clear();
        gattBusy = false;
        operationToken = 0;
        serviceCount = 0;
        notificationCount = 0;
        readCount = 0;
        receivedPacketCount = 0;
        diagnosticLog.setLength(0);

        boolean target = QX11_MAC.equalsIgnoreCase(safeAddress(device));
        watchName.setText(target ? "QX11 • GLORYFIT WATCH" : "SELECTED BLUETOOTH WATCH");
        setStatus("Connecting…");

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                gatt = device.connectGatt(this, false, gattCallback,
                        BluetoothDevice.TRANSPORT_LE);
            } else {
                gatt = device.connectGatt(this, false, gattCallback);
            }
        } catch (SecurityException e) {
            setStatus("Android denied Bluetooth connection permission.");
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(
                BluetoothGatt source, int statusCode, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt = source;
                runOnUiThread(() -> setStatus(
                        "Bluetooth link connected. Discovering services; no health data confirmed yet."));
                try {
                    source.discoverServices();
                } catch (SecurityException e) {
                    runOnUiThread(() ->
                            setStatus("Connected, but service discovery permission failed."));
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    setStatus("Watch disconnected.");
                    resetMeasurements();
                });
                try {
                    source.close();
                } catch (Exception ignored) {
                }
                if (gatt == source) gatt = null;
            } else if (statusCode != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> setStatus(
                        "Bluetooth connection error: " + statusCode));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt source, int result) {
            if (result != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> setStatus(
                        "Bluetooth connected, but service discovery failed: " + result));
                return;
            }

            serviceCount = source.getServices().size();
            StringBuilder inventory = new StringBuilder();
            inventory.append("GATT INVENTORY • ")
                    .append(serviceCount).append(" services\n");

            operations.clear();
            for (BluetoothGattService service : source.getServices()) {
                inventory.append("SERVICE ").append(service.getUuid()).append("\n");
                for (BluetoothGattCharacteristic characteristic
                        : service.getCharacteristics()) {
                    int properties = characteristic.getProperties();
                    inventory.append("  CHAR ")
                            .append(characteristic.getUuid())
                            .append(" [").append(propertyNames(properties)).append("]\n");

                    boolean canNotify =
                            (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                    boolean canIndicate =
                            (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
                    boolean canRead =
                            (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0;

                    // Subscribe to every advertised notification/indication characteristic.
                    // This discovers the actual device channels instead of guessing UUIDs.
                    if ((canNotify || canIndicate)
                            && characteristic.getDescriptor(CCCD) != null) {
                        operations.add(new GattOperation(characteristic, true));
                    }
                    // Read every characteristic explicitly marked readable. No write/command
                    // is sent to unknown vendor characteristics.
                    if (canRead) {
                        operations.add(new GattOperation(characteristic, false));
                    }
                }
            }

            String inventoryText = inventory.toString();
            runOnUiThread(() -> {
                setStatus("CONNECTED • " + serviceCount
                        + " services discovered. Fetching readable data and enabling live updates…");
                appendDiagnostic(inventoryText);
                if (source.getService(BATTERY_SERVICE) == null
                        || source.getService(BATTERY_SERVICE)
                        .getCharacteristic(BATTERY_LEVEL) == null) {
                    battery.setText("BATTERY  •  NOT PROVIDED THROUGH STANDARD GATT");
                } else {
                    battery.setText("BATTERY  •  READ QUEUED — WAITING FOR WATCH");
                }
                if (source.getService(HR_SERVICE) == null
                        || source.getService(HR_SERVICE).getCharacteristic(HR_MEASURE) == null) {
                    heart.setText("N/A");
                }
                if (source.getService(PULSE_OX_SERVICE) == null) {
                    oxygen.setText("N/A");
                }
                steps.setText("WAITING");
                calories.setText("WAITING");
            });

            startNextGattOperation();
        }

        @Override
        public void onDescriptorWrite(
                BluetoothGatt source, BluetoothGattDescriptor descriptor, int statusCode) {
            if (statusCode == BluetoothGatt.GATT_SUCCESS) {
                notificationCount++;
                runOnUiThread(() -> setStatus(
                        "Services discovered • " + notificationCount
                                + " live channels enabled • waiting for actual data."));
            } else {
                appendDiagnostic("SUBSCRIPTION FAILED "
                        + descriptor.getCharacteristic().getUuid()
                        + " • GATT " + statusCode);
            }
            finishGattOperation();
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGatt source, BluetoothGattCharacteristic characteristic, int result) {
            if (result == BluetoothGatt.GATT_SUCCESS) {
                handleMeasurement(characteristic, characteristic.getValue(), "GATT READ");
            } else {
                appendDiagnostic("READ FAILED " + characteristic.getUuid()
                        + " • GATT " + result);
            }
            finishGattOperation();
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGatt source, BluetoothGattCharacteristic characteristic,
                byte[] value, int result) {
            if (result == BluetoothGatt.GATT_SUCCESS) {
                handleMeasurement(characteristic, value, "GATT READ");
            } else {
                appendDiagnostic("READ FAILED " + characteristic.getUuid()
                        + " • GATT " + result);
            }
            finishGattOperation();
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt source, BluetoothGattCharacteristic characteristic) {
            handleMeasurement(characteristic, characteristic.getValue(), "LIVE NOTIFICATION");
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt source, BluetoothGattCharacteristic characteristic, byte[] value) {
            handleMeasurement(characteristic, value, "LIVE NOTIFICATION");
        }
    };

    private String propertyNames(int properties) {
        ArrayList<String> names = new ArrayList<>();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0) names.add("READ");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) names.add("WRITE");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            names.add("WRITE_NO_RESPONSE");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) names.add("NOTIFY");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) names.add("INDICATE");
        return names.isEmpty() ? "NONE" : android.text.TextUtils.join(",", names);
    }

    /**
     * Android GATT permits one request at a time. Queue reads and CCCD writes so
     * subscription/read requests do not collide and silently fail.
     */
    private void startNextGattOperation() {
        if (gatt == null || gattBusy || operations.isEmpty()) {
            if (!gattBusy && operations.isEmpty()) {
                runOnUiThread(() -> {
                    setStatus("CONNECTED • " + serviceCount + " services • "
                            + notificationCount + " live channels enabled • "
                            + readCount + " readable values returned.");
                    appendDiagnostic("GATT discovery/read queue finished. Waiting for live watch updates. "
                            + "A connected link alone is not a measurement.");
                });
            }
            return;
        }

        GattOperation operation = operations.poll();
        if (operation == null) return;

        gattBusy = true;
        final int token = ++operationToken;
        BluetoothGattCharacteristic characteristic = operation.characteristic;

        try {
            boolean started;
            if (operation.subscribe) {
                boolean localEnabled =
                        gatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
                if (!localEnabled || descriptor == null) {
                    appendDiagnostic("COULD NOT ENABLE NOTIFICATION "
                            + characteristic.getUuid());
                    gattBusy = false;
                    startNextGattOperation();
                    return;
                }

                int properties = characteristic.getProperties();
                byte[] value = (properties
                        & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                        ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                descriptor.setValue(value);
                started = gatt.writeDescriptor(descriptor);
            } else {
                started = gatt.readCharacteristic(characteristic);
            }

            if (!started) {
                appendDiagnostic((operation.subscribe ? "SUBSCRIBE" : "READ")
                        + " REQUEST NOT STARTED: " + characteristic.getUuid());
                gattBusy = false;
                startNextGattOperation();
                return;
            }

            handler.postDelayed(() -> {
                if (gattBusy && operationToken == token) {
                    gattBusy = false;
                    appendDiagnostic("GATT OPERATION TIMEOUT: "
                            + characteristic.getUuid() + " — moving to next item.");
                    operationToken++;
                    startNextGattOperation();
                }
            }, 6000);

        } catch (SecurityException e) {
            gattBusy = false;
            appendDiagnostic("BLUETOOTH PERMISSION BLOCKED OPERATION: "
                    + characteristic.getUuid());
            startNextGattOperation();
        }
    }

    private void finishGattOperation() {
        gattBusy = false;
        operationToken++;
        if (gatt != null) handler.post(this::startNextGattOperation);
    }

    private void handleMeasurement(
            BluetoothGattCharacteristic characteristic, byte[] data, String source) {
        if (data == null || data.length == 0) {
            appendDiagnostic(source + " returned an empty value for "
                    + characteristic.getUuid());
            return;
        }

        UUID id = characteristic.getUuid();
        receivedPacketCount++;
        if ("GATT READ".equals(source)) readCount++;

        String hex = toHex(data);
        String time = now();
        appendDiagnostic(source + " • " + time + " • "
                + id + " • " + hex);

        if (BATTERY_LEVEL.equals(id)) {
            int level = data[0] & 0xFF;
            if (level <= 100) {
                lastBatteryReadAt = System.currentTimeMillis();
                runOnUiThread(() -> battery.setText(
                        "BATTERY  •  " + level + "%  •  RECEIVED FROM WATCH • " + time));
            } else {
                runOnUiThread(() -> battery.setText(
                        "BATTERY  •  INVALID VALUE (" + level + ") — NOT TRUSTED"));
            }
        } else if (HR_MEASURE.equals(id)) {
            parseHeartRate(data, time);
        } else if (PULSE_OX_CONTINUOUS.equals(id)
                || PULSE_OX_SPOT.equals(id)) {
            parseOxygen(data, time);
        } else {
            // Unknown manufacturer-specific data is retained as raw bytes.
            // Do not guess whether a byte represents steps, calories, BP or SpO2.
            runOnUiThread(() -> setStatus(
                    "CONNECTED • REAL DATA PACKET RECEIVED • "
                            + receivedPacketCount + " packets • " + time));
        }
    }

    private void parseHeartRate(byte[] data, String time) {
        if (data.length < 2) return;
        int flags = data[0] & 0xFF;
        if ((flags & 0x01) != 0 && data.length < 3) return;

        int bpm = (flags & 0x01) == 0
                ? (data[1] & 0xFF)
                : ((data[2] & 0xFF) << 8) | (data[1] & 0xFF);

        if (bpm > 0 && bpm < 260) {
            runOnUiThread(() -> {
                heart.setText(String.valueOf(bpm));
                setStatus("LIVE HEART RATE RECEIVED • " + time);
            });
        }
    }

    private void parseOxygen(byte[] data, String time) {
        // Standard Pulse Oximeter Measurement: flags followed by SpO2 SFLOAT.
        if (data.length < 3) return;
        int flags = data[0] & 0xFF;
        int raw = (data[1] & 0xFF) | ((data[2] & 0xFF) << 8);
        double spo2 = decodeSfloat(raw);

        if ((flags & 0x01) == 0 && spo2 >= 0 && spo2 <= 100) {
            runOnUiThread(() -> {
                oxygen.setText(String.format(Locale.US, "%.0f%%", spo2));
                setStatus("LIVE BLOOD OXYGEN VALUE RECEIVED • " + time);
            });
        } else {
            appendDiagnostic("PULSE OXIMETER PACKET RECEIVED BUT NOT DECODED AS SpO2: "
                    + toHex(data));
        }
    }

    private double decodeSfloat(int raw) {
        int mantissa = raw & 0x0FFF;
        if ((mantissa & 0x0800) != 0) mantissa |= 0xFFFFF000;
        int exponent = (raw >> 12) & 0x0F;
        if ((exponent & 0x08) != 0) exponent |= 0xFFFFFFF0;

        if (mantissa == 0x07FE || mantissa == 0x07FF
                || mantissa == 0x0800 || mantissa == 0x0801) return -1;
        return mantissa * Math.pow(10, exponent);
    }

    private String toHex(byte[] data) {
        StringBuilder result = new StringBuilder();
        for (byte b : data) {
            if (result.length() > 0) result.append(' ');
            result.append(String.format(Locale.US, "%02X", b & 0xFF));
        }
        return result.toString();
    }

    private String now() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date());
    }

    private void appendDiagnostic(String line) {
        handler.post(() -> {
            diagnosticLog.append(line).append("\n");
            if (diagnosticLog.length() > 7000) {
                diagnosticLog.delete(0, diagnosticLog.length() - 6000);
            }
            if (diagnostic != null) diagnostic.setText(diagnosticLog.toString());
        });
    }

    private void resetMeasurements() {
        if (heart != null) heart.setText("—");
        if (oxygen != null) oxygen.setText("—");
        if (steps != null) steps.setText("—");
        if (calories != null) calories.setText("—");
        if (battery != null) battery.setText("BATTERY  •  Not read yet");
        operations.clear();
        gattBusy = false;
        operationToken++;
    }

    private void setStatus(String message) {
        if (status != null) status.setText(message);
    }

    private void closeGatt() {
        if (gatt != null) {
            try {
                gatt.disconnect();
                gatt.close();
            } catch (SecurityException ignored) {
            }
            gatt = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopScan("");
        closeGatt();
        super.onDestroy();
    }
}
