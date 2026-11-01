package com.qboix.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WatchActivity extends Activity {
    private static final int GOLD=Color.rgb(215,181,91), GREEN=Color.rgb(55,190,125);
    private static final int BG=Color.rgb(9,12,20), PANEL=Color.rgb(25,29,38);
    private static final UUID HR_SERVICE=UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_MEASURE=UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_SERVICE=UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL=UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OX_SERVICE=UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OX_CONTINUOUS=UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OX_SPOT=UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // GloryFit-family data notifications. These are observed on this QX11 connection;
    // payloads are logged raw until the exact QX11 firmware packet format is verified.
    private static final UUID GF_HR_NOTIFY=UUID.fromString("000033f2-0000-1000-8000-00805f9b34fb");
    private static final UUID GF_DATA_NOTIFY=UUID.fromString("000034f2-0000-1000-8000-00805f9b34fb");
    // Address copied from the user's connected-watch screenshot.
    private static final String QXII_MAC="78:02:B7:14:AE:96";

    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private LinearLayout root,deviceList;
    private TextView status,watchName,heart,oxygen,steps,calories,battery,diagnostic;
    private TextView heartState,oxygenState,stepsState,caloriesState;
    private Button scanButton;
    private final Map<String,BluetoothDevice> found=new ConcurrentHashMap<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean scanning=false,connected=false;\n    private int customPackets=0;\n    private long batteryReadStartedAt=0;
    private final Set<UUID> enabledCharacteristics=Collections.synchronizedSet(new HashSet<>());

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        buildUi();
        BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        adapter=manager==null?null:manager.getAdapter();
        if(adapter==null){setStatus("Bluetooth LE is not available on this phone.");return;}
        requestBlePermissions();
    }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(BG);
        scroll.addView(root);setContentView(scroll);

        TextView brand=label("Q-BOIX  /  SMARTWATCH",GOLD,22,true);root.addView(brand);
        TextView subtitle=label("GLORYFIT LIVE HEALTH CONNECTION",Color.WHITE,13,true);
        LinearLayout.LayoutParams sp=params(-1,-2);sp.topMargin=dp(6);root.addView(subtitle,sp);

        LinearLayout connection=panel();
        watchName=label("QXII WATCH",Color.WHITE,17,true);connection.addView(watchName);
        status=label("Not connected",Color.rgb(190,190,198),13,false);
        LinearLayout.LayoutParams st=params(-1,-2);st.topMargin=dp(5);connection.addView(status,st);
        root.addView(connection,new LinearLayout.LayoutParams(-1,-2));

        scanButton=new Button(this);scanButton.setText("SCAN FOR MY WATCH");
        scanButton.setAllCaps(false);scanButton.setTextColor(Color.BLACK);
        scanButton.setBackground(shape(GOLD,dp(13)));
        LinearLayout.LayoutParams bp=params(-1,dp(50));bp.topMargin=dp(12);root.addView(scanButton,bp);
        scanButton.setOnClickListener(v->startScan());

        deviceList=new LinearLayout(this);deviceList.setOrientation(1);
        LinearLayout.LayoutParams dl=params(-1,-2);dl.topMargin=dp(6);root.addView(deviceList,dl);

        TextView heading=label("LIVE HEALTH DATA",GOLD,15,true);
        LinearLayout.LayoutParams hp=params(-1,-2);hp.topMargin=dp(22);root.addView(heading,hp);
        TextView hint=label("Only measurements actually sent by your watch will appear.",Color.LTGRAY,11,false);
        root.addView(hint);

        LinearLayout row1=new LinearLayout(this);row1.setOrientation(0);
        row1.addView(metric("♥","HEART RATE","—","BPM"),new LinearLayout.LayoutParams(0,-2,1));
        row1.addView(metric("◉","BLOOD OXYGEN","—","SpO₂"),new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams rp=params(-1,-2);rp.topMargin=dp(8);root.addView(row1,rp);

        LinearLayout row2=new LinearLayout(this);row2.setOrientation(0);
        row2.addView(metric("👣","STEPS","—","steps"),new LinearLayout.LayoutParams(0,-2,1));
        row2.addView(metric("🔥","CALORIES","—","kcal"),new LinearLayout.LayoutParams(0,-2,1));
        rp=params(-1,-2);rp.topMargin=dp(8);root.addView(row2,rp);

        battery=label("BATTERY  •  Not read yet",Color.LTGRAY,12,false);
        LinearLayout.LayoutParams bat=params(-1,-2);bat.topMargin=dp(15);root.addView(battery,bat);
        diagnostic=label("Waiting for watch connection.",Color.LTGRAY,11,false);
        diagnostic.setPadding(0,dp(14),0,0);root.addView(diagnostic);
    }

    private LinearLayout metric(String icon,String title,String value,String unit){
        LinearLayout card=panel();card.setMinimumHeight(dp(112));
        TextView i=label(icon,GREEN,20,true);card.addView(i);
        TextView t=label(title,Color.LTGRAY,10,true);
        LinearLayout.LayoutParams tp=params(-1,-2);tp.topMargin=dp(5);card.addView(t,tp);
        TextView v=label(value,Color.WHITE,27,true);
        LinearLayout.LayoutParams vp=params(-1,-2);vp.topMargin=dp(4);card.addView(v,vp);
        TextView u=label(unit,GREEN,10,false);card.addView(u);
        if("HEART RATE".equals(title))heart=v;
        if("BLOOD OXYGEN".equals(title))oxygen=v;
        if("STEPS".equals(title))steps=v;
        if("CALORIES".equals(title))calories=v;
        LinearLayout.LayoutParams p=params(-1,-2);p.setMargins(dp(4),dp(3),dp(4),dp(3));card.setLayoutParams(p);
        return card;
    }

    private LinearLayout panel(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);
        box.setPadding(dp(14),dp(13),dp(14),dp(13));box.setBackground(shape(PANEL,dp(16)));
        return box;
    }
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;
    }
    private TextView label(String value,int color,float size,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private boolean has(String p){return Build.VERSION.SDK_INT<23||checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;}

    private void requestBlePermissions(){
        if(Build.VERSION.SDK_INT>=31){
            ArrayList<String> missing=new ArrayList<>();
            if(!has(Manifest.permission.BLUETOOTH_SCAN))missing.add(Manifest.permission.BLUETOOTH_SCAN);
            if(!has(Manifest.permission.BLUETOOTH_CONNECT))missing.add(Manifest.permission.BLUETOOTH_CONNECT);
            if(!missing.isEmpty()){requestPermissions(missing.toArray(new String[0]),10);return;}
        }else if(!has(Manifest.permission.ACCESS_FINE_LOCATION)){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},10);return;
        }
        setStatus(adapter.isEnabled()?"Bluetooth ready. Tap scan to find your QXII watch.":"Turn on Bluetooth, then scan.");
    }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==10){boolean ok=grants.length>0;for(int g:grants)ok&=g==PackageManager.PERMISSION_GRANTED;
            if(ok)setStatus("Permission granted. Tap scan.");else setStatus("Bluetooth permission denied. Allow it in Android settings.");}
    }

    private void startScan(){
        if(adapter==null||!adapter.isEnabled()){setStatus("Turn on Bluetooth on your phone first.");return;}
        if(Build.VERSION.SDK_INT>=31&&(!has(Manifest.permission.BLUETOOTH_SCAN)||!has(Manifest.permission.BLUETOOTH_CONNECT))){requestBlePermissions();return;}
        try{
            scanner=adapter.getBluetoothLeScanner();if(scanner==null){setStatus("Bluetooth LE scanner is unavailable.");return;}
            found.clear();deviceList.removeAllViews();scanning=true;scanButton.setEnabled(false);
            setStatus("Searching nearby for your watch…");
            scanner.startScan(scanCallback);
            handler.postDelayed(()->stopScan("Scan complete. Choose QXII if it appears."),12000);
        }catch(SecurityException e){setStatus("Bluetooth permission is required.");}
    }

    private final ScanCallback scanCallback=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult result){
            BluetoothDevice d=result.getDevice();String address=safeAddress(d);
            if(found.putIfAbsent(address,d)==null)runOnUiThread(()->addDevice(d));
            if(QXII_MAC.equalsIgnoreCase(address))runOnUiThread(()->{stopScan("Your QXII watch was found. Connecting…");connect(d);});
        }
        @Override public void onScanFailed(int code){runOnUiThread(()->{setStatus("Bluetooth scan failed (code "+code+").");scanButton.setEnabled(true);});}
    };

    private void stopScan(String message){
        if(scanning){scanning=false;try{if(scanner!=null)scanner.stopScan(scanCallback);}catch(SecurityException ignored){}}
        setStatus(message);if(scanButton!=null)scanButton.setEnabled(true);
    }
    private String safeAddress(BluetoothDevice d){try{return d.getAddress();}catch(SecurityException e){return "";}}
    private void addDevice(BluetoothDevice d){
        String name="Bluetooth device";
        try{if(d.getName()!=null&&!d.getName().trim().isEmpty())name=d.getName();}catch(SecurityException ignored){}
        boolean target=QXII_MAC.equalsIgnoreCase(safeAddress(d));
        Button b=new Button(this);b.setAllCaps(false);b.setTextColor(target?Color.BLACK:Color.WHITE);
        b.setText((target?"✓  YOUR QXII WATCH  •  ":"")+name);
        b.setBackground(shape(target?GOLD:PANEL,dp(12)));
        LinearLayout.LayoutParams p=params(-1,dp(48));p.topMargin=dp(5);deviceList.addView(b,p);
        b.setOnClickListener(v->connect(d));
    }

    private void connect(BluetoothDevice device){
        stopScan("Connecting to "+(QXII_MAC.equalsIgnoreCase(safeAddress(device))?"your QXII watch":"selected device")+"…");
        if(gatt!=null){try{gatt.disconnect();gatt.close();}catch(Exception ignored){}gatt=null;}
        connected=false;enabledCharacteristics.clear();resetMeasurements();
        try{
            watchName.setText(QXII_MAC.equalsIgnoreCase(safeAddress(device))?"QXII • GLORYFIT WATCH":"SELECTED BLUETOOTH WATCH");
            gatt=device.connectGatt(this,false,gattCallback);
        }catch(SecurityException e){setStatus("Android denied Bluetooth connection permission.");}
    }

    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt source,int statusCode,int newState){
            if(newState==BluetoothProfile.STATE_CONNECTED){
                gatt=source;connected=true;runOnUiThread(()->setStatus("Connected. Reading available watch measurements…"));
                try{source.discoverServices();}catch(SecurityException e){runOnUiThread(()->setStatus("Connected, but service discovery permission failed."));}
            }else if(newState==BluetoothProfile.STATE_DISCONNECTED){
                connected=false;runOnUiThread(()->{setStatus("Watch disconnected.");resetMeasurements();});
                try{source.close();}catch(Exception ignored){}
            }
        }
        @Override public void onServicesDiscovered(BluetoothGatt source,int result){
            if(result!=BluetoothGatt.GATT_SUCCESS){runOnUiThread(()->diagnostic.setText("Could not read watch services (error "+result+")."));return;}
            int serviceCount=source.getServices().size();
            StringBuilder inventory=new StringBuilder("GATT INVENTORY • "+serviceCount+" services\\n");
            for(BluetoothGattService svc:source.getServices()){
                inventory.append(svc.getUuid()).append("\\n");
                for(BluetoothGattCharacteristic ch:svc.getCharacteristics()){
                    inventory.append("  ").append(ch.getUuid()).append(" [")
                        .append(ch.getProperties()).append("]\\n");
                }
            }
            runOnUiThread(()->{setStatus("CONNECTED • "+serviceCount+" Bluetooth services discovered. Health readings are checked separately.");diagnostic.setText(inventory.toString()+"\\nWaiting for real measurements and GloryFit packets. No health value is assumed.");});
            readBattery(source);
            enableHeartRate(source);
            enableOxygen(source);
            markUnavailableIfMissing(source);
        }
        @Override public void onCharacteristicRead(BluetoothGatt source,BluetoothGattCharacteristic c,int result){
            if(result==BluetoothGatt.GATT_SUCCESS)handleMeasurement(c,c.getValue());
        }
        @Override public void onCharacteristicRead(BluetoothGatt source,BluetoothGattCharacteristic c,byte[] value,int result){
            if(result==BluetoothGatt.GATT_SUCCESS)handleMeasurement(c,value);
        }
        @Override public void onCharacteristicChanged(BluetoothGatt source,BluetoothGattCharacteristic c){
            handleMeasurement(c,c.getValue());
        }
        @Override public void onCharacteristicChanged(BluetoothGatt source,BluetoothGattCharacteristic c,byte[] value){
            handleMeasurement(c,value);
        }
        @Override public void onDescriptorWrite(BluetoothGatt source,BluetoothGattDescriptor descriptor,int statusCode){
            if(statusCode!=BluetoothGatt.GATT_SUCCESS)runOnUiThread(()->diagnostic.setText("Watch connected, but a live measurement subscription was rejected."));
        }
    };

    private void subscribeGloryFitChannels(BluetoothGatt source){
        int candidates=0;
        for(BluetoothGattService service:source.getServices()){
            for(BluetoothGattCharacteristic ch:service.getCharacteristics()){
                UUID id=ch.getUuid();
                if(GF_HR_NOTIFY.equals(id)||GF_DATA_NOTIFY.equals(id)){
                    candidates++;
                    subscribe(source,ch);
                }
            }
        }
        final int count=candidates;
        runOnUiThread(()->diagnostic.setText("GloryFit notification characteristics found: "+count+
            ". Subscribing; raw packets will be displayed when received.\\n"+
            "This does not yet decode steps, calories, sleep, blood pressure or oxygen."));
    }

    private void readBattery(BluetoothGatt source){
        BluetoothGattService service=source.getService(BATTERY_SERVICE);
        BluetoothGattCharacteristic c=service==null?null:service.getCharacteristic(BATTERY_LEVEL);
        if(c==null){runOnUiThread(()->battery.setText("BATTERY  •  Not provided by watch"));return;}
        batteryReadStartedAt=System.currentTimeMillis();
        runOnUiThread(()->battery.setText("BATTERY  •  READ REQUEST SENT — WAITING"));
        try{boolean started=source.readCharacteristic(c);
            if(!started)runOnUiThread(()->battery.setText("BATTERY  •  READ REQUEST NOT STARTED"));
        }catch(SecurityException ignored){runOnUiThread(()->battery.setText("BATTERY  •  READ BLOCKED BY PERMISSION"));}
    }
    private void enableHeartRate(BluetoothGatt source){
        BluetoothGattService service=source.getService(HR_SERVICE);
        BluetoothGattCharacteristic c=service==null?null:service.getCharacteristic(HR_MEASURE);
        if(c==null){runOnUiThread(()->heart.setText("N/A"));return;}
        subscribe(source,c);
    }
    private void enableOxygen(BluetoothGatt source){
        BluetoothGattService service=source.getService(PULSE_OX_SERVICE);
        if(service==null){runOnUiThread(()->{oxygen.setText("N/A");setMetricState(oxygenState,"NOT PROVIDED BY WATCH");});return;}
        BluetoothGattCharacteristic continuous=service.getCharacteristic(PULSE_OX_CONTINUOUS);
        BluetoothGattCharacteristic spot=service.getCharacteristic(PULSE_OX_SPOT);
        BluetoothGattCharacteristic c=continuous!=null?continuous:spot;
        if(c==null){runOnUiThread(()->oxygen.setText("N/A"));return;}
        subscribe(source,c);
    }
    private void subscribe(BluetoothGatt source,BluetoothGattCharacteristic c){
        try{
            int props=c.getProperties();
            boolean notify=(props&BluetoothGattCharacteristic.PROPERTY_NOTIFY)!=0;
            boolean indicate=(props&BluetoothGattCharacteristic.PROPERTY_INDICATE)!=0;
            if(!notify&&!indicate){runOnUiThread(()->setUnsupported(c.getUuid()));return;}
            source.setCharacteristicNotification(c,true);
            BluetoothGattDescriptor d=c.getDescriptor(CCCD);
            if(d==null){runOnUiThread(()->setUnsupported(c.getUuid()));return;}
            d.setValue(notify?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            source.writeDescriptor(d);
            enabledCharacteristics.add(c.getUuid());
        }catch(SecurityException e){runOnUiThread(()->diagnostic.setText("Bluetooth permission prevented reading a watch measurement."));}
    }
    private void markUnavailableIfMissing(BluetoothGatt source){
        if(source.getService(HR_SERVICE)==null)runOnUiThread(()->heart.setText("N/A"));
        if(source.getService(PULSE_OX_SERVICE)==null)runOnUiThread(()->oxygen.setText("N/A"));
        // Steps and calories use manufacturer-specific GloryFit data on many watches.
        // Never display a fabricated value; wait for a supported decoder.
        runOnUiThread(()->{steps.setText("N/A");calories.setText("N/A");setMetricState(stepsState,"GLORYFIT DATA NOT DECODED");setMetricState(caloriesState,"GLORYFIT DATA NOT DECODED");});
    }

    private void handleMeasurement(BluetoothGattCharacteristic c,byte[] data){
        if(data==null||data.length==0)return;
        UUID id=c.getUuid();
        if(HR_MEASURE.equals(id))parseHeartRate(data);
        else if(BATTERY_LEVEL.equals(id)){
            int level=data[0]&255;
            if(level<=100){
                long age=System.currentTimeMillis()-batteryReadStartedAt;
                runOnUiThread(()->battery.setText("BATTERY  •  "+level+"%  •  GATT VALUE READ"+
                    (age>=0&&age<60000?" • JUST NOW":" • CHECK FRESHNESS")));
            }else{
                runOnUiThread(()->battery.setText("BATTERY  •  INVALID VALUE ("+level+") — NOT TRUSTED"));
            }
        } else if(GF_HR_NOTIFY.equals(id)||GF_DATA_NOTIFY.equals(id)){
            customPackets++;
            StringBuilder hex=new StringBuilder();
            for(byte b:data)hex.append(String.format(Locale.US,"%02X ",b&255));
            String channel=GF_HR_NOTIFY.equals(id)?"GLORYFIT HR CHANNEL":"GLORYFIT DATA CHANNEL";
            String packet=channel+" • PACKET "+customPackets+" • "+hex.toString().trim();
            runOnUiThread(()->diagnostic.setText(packet+"\\nRaw packet received; decoder not verified for QX11 firmware."));
        }
        else if(PULSE_OX_CONTINUOUS.equals(id)||PULSE_OX_SPOT.equals(id)){
            // IEEE-11073 SFLOAT is used by the standard Pulse Oximeter service.
            // Do not mislabel unsupported/invalid values as a saturation percentage.
            if(data.length>=3){int flags=data[0]&255;int raw=(data[1]&255)|((data[2]&255)<<8);double spo2=decodeSfloat(raw);
                if((flags&0x01)==0&&spo2>=0&&spo2<=100)runOnUiThread(()->{oxygen.setText(String.format(Locale.US,"%.0f%%",spo2));setMetricState(oxygenState,"DATA RECEIVED FROM WATCH");});}
        }
    }
    private void parseHeartRate(byte[] data){
        if(data.length<2)return;int flags=data[0]&255;
        if((flags&1)!=0&&data.length<3)return;
        int bpm=(flags&1)==0?(data[1]&255):((data[2]&255)<<8)|(data[1]&255);
        if(bpm>0&&bpm<260)runOnUiThread(()->{heart.setText(String.valueOf(bpm));setMetricState(heartState,"LIVE DATA RECEIVED • BPM");});
    }
    private double decodeSfloat(int raw){
        int mantissa=raw&0x0FFF;if((mantissa&0x0800)!=0)mantissa|=0xFFFFF000;
        int exponent=(raw>>12)&0x0F;if((exponent&0x08)!=0)exponent|=0xFFFFFFF0;
        if(mantissa==0x07FE||mantissa==0x07FF||mantissa==0x0800||mantissa==0x0801)return -1;
        return mantissa*Math.pow(10,exponent);
    }
    private void setUnsupported(UUID id){
        if(HR_MEASURE.equals(id))heart.setText("N/A");
        if(PULSE_OX_CONTINUOUS.equals(id)||PULSE_OX_SPOT.equals(id))oxygen.setText("N/A");
    }
    private void resetMeasurements(){
        if(heart!=null)heart.setText("—");if(oxygen!=null)oxygen.setText("—");
        if(steps!=null)steps.setText("—");if(calories!=null)calories.setText("—");
        if(battery!=null)battery.setText("BATTERY  •  Not connected"); customPackets=0;
    }
    private void setMetricState(TextView view,String message){if(view!=null)view.setText(message);}
    private void setStatus(String message){if(status!=null)status.setText(message);}

    @Override protected void onDestroy(){
        stopScan("");if(gatt!=null){try{gatt.disconnect();gatt.close();}catch(SecurityException ignored){}gatt=null;}
        super.onDestroy();
    }
}
