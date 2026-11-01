package com.qboix.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WatchActivity extends Activity {
    private static final UUID HR_SERVICE=UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_MEASURE=UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // User-confirmed GloryFit device identity. MAC is used only to recognize this paired watch during scans.
    private static final String QXII_MAC="78:02:B7:14:96";
    private static final String QXII_NAME="QXII (ID-AE96)";
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private LinearLayout root, devices;
    private TextView status, heart, steps, oxygen, calories, detail;
    private final Map<String,BluetoothDevice> found=new ConcurrentHashMap<>();
    private boolean scanning=false;
    private final Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b){super.onCreate(b); buildUi();
        BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        adapter=manager==null?null:manager.getAdapter();
        if(adapter==null){status.setText("Bluetooth LE is not available on this phone.");return;}
        requestBlePermissions();
    }
    private void buildUi(){
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(28,28,28,28);root.setBackgroundColor(Color.rgb(9,12,20));
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        add("Q-BOIX • SMARTWATCH",Color.rgb(215,181,91),24);
        add("GloryFit / BLE connection",Color.WHITE,18);
        status=add("Status: Not connected",Color.rgb(55,190,125),17);
        Button scan=new Button(this);scan.setText("SCAN FOR WATCHES");root.addView(scan);scan.setOnClickListener(v->startScan());
        devices=new LinearLayout(this);devices.setOrientation(1);root.addView(devices);
        add("LIVE WATCH DATA",Color.rgb(215,181,91),18);
        heart=add("Heart rate: — bpm",Color.WHITE,20);
        steps=add("Steps: Not yet available",Color.WHITE,16);
        oxygen=add("Blood oxygen (SpO₂): Not yet available",Color.WHITE,16);
        calories=add("Calories: Not yet available",Color.WHITE,16);
        detail=add("Connect a watch to inspect its Bluetooth services.",Color.LTGRAY,13);
        TextView note=add("Important: this screen currently reads standard Bluetooth heart-rate data when the watch exposes it. GloryFit models often use different private protocols for steps, SpO₂ and calories; those values will remain unavailable until this exact watch model/protocol is supported. No estimated values are shown.",Color.LTGRAY,13);
        note.setPadding(0,18,0,18);
    }
    private TextView add(String s,int color,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setPadding(0,12,0,12);root.addView(t);return t;}
    private boolean has(String p){return Build.VERSION.SDK_INT<23||checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;}
    private void requestBlePermissions(){
        if(Build.VERSION.SDK_INT>=31){
            ArrayList<String> p=new ArrayList<>();
            if(!has(Manifest.permission.BLUETOOTH_SCAN))p.add(Manifest.permission.BLUETOOTH_SCAN);
            if(!has(Manifest.permission.BLUETOOTH_CONNECT))p.add(Manifest.permission.BLUETOOTH_CONNECT);
            if(!p.isEmpty()){requestPermissions(p.toArray(new String[0]),10);return;}
        } else if(!has(Manifest.permission.ACCESS_FINE_LOCATION)){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},10);return;}
        status.setText(adapter.isEnabled()?"Bluetooth ready. Tap scan.":"Turn on Bluetooth, then scan.");
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==10){boolean ok=true;for(int x:g)ok&=x==PackageManager.PERMISSION_GRANTED;if(ok)status.setText("Permissions granted. Tap scan.");else status.setText("Bluetooth permission denied.");}}
    private void startScan(){
        if(adapter==null||!adapter.isEnabled()){status.setText("Turn on Bluetooth first.");return;}
        if(Build.VERSION.SDK_INT>=31&&(!has(Manifest.permission.BLUETOOTH_SCAN)||!has(Manifest.permission.BLUETOOTH_CONNECT))){requestBlePermissions();return;}
        try{
            scanner=adapter.getBluetoothLeScanner();if(scanner==null){status.setText("BLE scanner unavailable.");return;}
            found.clear();devices.removeAllViews();scanning=true;status.setText("Scanning nearby Bluetooth LE devices…");
            scanner.startScan(callback);handler.postDelayed(()->stopScan("Scan finished. Select your watch."),12000);
        }catch(SecurityException e){status.setText("Bluetooth permission is required.");}
    }
    private final ScanCallback callback=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult result){BluetoothDevice d=result.getDevice();String key=d.getAddress();if(found.putIfAbsent(key,d)==null)runOnUiThread(()->addDevice(d));
            if(QXII_MAC.equalsIgnoreCase(key))runOnUiThread(()->{stopScan("QXII detected. Connecting…");connect(d);});}
        @Override public void onScanFailed(int code){runOnUiThread(()->status.setText("Scan failed: "+code));}
    };
    private void stopScan(String message){if(!scanning)return;scanning=false;try{if(scanner!=null)scanner.stopScan(callback);}catch(SecurityException ignored){}status.setText(message);}
    private void addDevice(BluetoothDevice d){
        String name="Unknown watch";try{if(d.getName()!=null)name=d.getName();}catch(SecurityException ignored){}
        if(QXII_MAC.equalsIgnoreCase(safeAddress(d)))name=QXII_NAME+" • "+name;
        Button b=new Button(this);b.setText(name+"  •  "+d.getAddress());devices.addView(b);b.setOnClickListener(v->connect(d));
    }
    private String safeAddress(BluetoothDevice d){try{return d.getAddress();}catch(SecurityException e){return "";}}
    private void connect(BluetoothDevice d){
        stopScan("Connecting…");if(gatt!=null){try{gatt.close();}catch(Exception ignored){}gatt=null;}
        try{gatt=d.connectGatt(this,false,gattCallback);status.setText("Connecting to selected device…");}
        catch(SecurityException e){status.setText("Bluetooth connect permission missing.");}
    }
    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int s,int n){
            runOnUiThread(()->status.setText(n==BluetoothProfile.STATE_CONNECTED?"Connected • discovering watch services…": "Disconnected"));
            if(n==BluetoothProfile.STATE_CONNECTED){gatt=g;try{g.discoverServices();}catch(SecurityException ignored){}}
        }
        @Override public void onServicesDiscovered(BluetoothGatt g,int s){
            if(s!=BluetoothGatt.GATT_SUCCESS){runOnUiThread(()->detail.setText("Service discovery failed: "+s));return;}
            boolean hasHr=g.getService(HR_SERVICE)!=null;
            StringBuilder report=new StringBuilder("GATT services: ").append(g.getServices().size()).append("\\n");
            for(BluetoothGattService service:g.getServices()){
                report.append("SERVICE ").append(service.getUuid()).append("\\n");
                for(BluetoothGattCharacteristic ch:service.getCharacteristics()){
                    report.append("  CHARACTERISTIC ").append(ch.getUuid()).append(" properties=").append(ch.getProperties()).append("\\n");
                }
            }
            String serviceReport=report.toString();
            runOnUiThread(()->detail.setText("QXII protocol inspection\\n"+serviceReport+"Standard heart-rate service: "+(hasHr?"available":"not exposed by this watch")+"."));
            if(hasHr){BluetoothGattCharacteristic ch=g.getService(HR_SERVICE).getCharacteristic(HR_MEASURE);if(ch!=null)try{g.setCharacteristicNotification(ch,true);BluetoothGattDescriptor desc=ch.getDescriptor(CCCD);if(desc!=null){desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);g.writeDescriptor(desc);}}catch(SecurityException ignored){}}
        }
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){if(HR_MEASURE.equals(c.getUuid()))showHeart(c.getValue());}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){if(HR_MEASURE.equals(c.getUuid()))showHeart(value);}
    };
    private void showHeart(byte[] v){if(v==null||v.length<2)return;int flags=v[0]&255;if((flags&1)!=0&&v.length<3)return;int bpm=(flags&1)==0?(v[1]&255):((v[2]&255)<<8)|(v[1]&255);runOnUiThread(()->heart.setText("Heart rate: "+bpm+" bpm"));}
    @Override protected void onDestroy(){stopScan("");if(gatt!=null){try{gatt.disconnect();gatt.close();}catch(SecurityException ignored){}}super.onDestroy();}
}
