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
    private static final UUID GF_MAIN_SERVICE=UUID.fromString("000055ff-0000-1000-8000-00805f9b34fb");
    private static final UUID GF_MAIN_WRITE=UUID.fromString("000033f1-0000-1000-8000-00805f9b34fb");
    private static final UUID GF_HR_NOTIFY=UUID.fromString("000033f2-0000-1000-8000-00805f9b34fb");
    private static final UUID GF_DATA_SERVICE=UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb");
    private static final UUID GF_DATA_WRITE=UUID.fromString("000034f1-0000-1000-8000-00805f9b34fb");
    private static final UUID GF_DATA_NOTIFY=UUID.fromString("000034f2-0000-1000-8000-00805f9b34fb");
    // Address copied from the user's connected-watch screenshot.
    private static final String QXII_MAC="78:02:B7:14:AE:96";

    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic gfMainWrite,gfDataWrite;
    private LinearLayout root,deviceList;
    private TextView status,watchName,heart,oxygen,steps,calories,battery,diagnostic;
    private TextView heartState,oxygenState,stepsState,caloriesState;
    private Button scanButton;
    private final Map<String,BluetoothDevice> found=new ConcurrentHashMap<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean scanning=false,connected=false;
    private final Runnable liveUiRefresh = new Runnable(){ @Override public void run(){ refreshStoredMetrics(); handler.postDelayed(this,1000); } };
    private int customPackets=0;
    private long batteryReadStartedAt=0;
    private final ArrayDeque<Runnable> gattOperations=new ArrayDeque<>();
    private boolean gattOperationInFlight=false;
    private Runnable activeGattTimeout;
    private long gattOperationSequence=0;
    private final Set<UUID> enabledCharacteristics=Collections.synchronizedSet(new HashSet<>());
    private final ArrayDeque<String> packetHistory=new ArrayDeque<>();
    // Independent raw capture: records every incoming GATT read/notification before decoding.
    private int rawPacketCount=0;
    private String diagnosticsText="";
    // B1/B2 are hourly totals; retain one value per date/hour to avoid duplicates.
    private final Map<String,Integer> hourlySteps=new LinkedHashMap<>();
    private boolean spo2SpotPending=false;
    private boolean spo2EarlyFailureSeen=false;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        buildUi();
        BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        adapter=manager==null?null:manager.getAdapter();
        if(adapter==null){setStatus("Bluetooth LE is not available on this phone.");return;}
        if(WatchDataStore.isConnected(this)){ setStatus("CONNECTED • QX11_AE96 • Reusing the existing watch session"); scanButton.setText("WATCH ALREADY CONNECTED"); scanButton.setEnabled(false); refreshStoredMetrics(); } else { requestBlePermissions(); }
        handler.post(liveUiRefresh);
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
        TextView rawHeading=label("RAW BLUETOOTH DIAGNOSTICS",GOLD,14,true);
        LinearLayout.LayoutParams rawHp=params(-1,-2);rawHp.topMargin=dp(20);root.addView(rawHeading,rawHp);
        root.addView(label("Every incoming GATT read/notification is captured before decoding, including unknown packets.",Color.LTGRAY,11,false));
        Button copyButton=new Button(this);copyButton.setText("COPY RAW DIAGNOSTICS");
        copyButton.setAllCaps(false);copyButton.setTextColor(Color.BLACK);copyButton.setBackground(shape(GOLD,dp(11)));
        LinearLayout.LayoutParams cp=params(-1,dp(44));cp.topMargin=dp(8);root.addView(copyButton,cp);
        copyButton.setOnClickListener(v->copyDiagnostics());
        diagnostic=label("Waiting for watch connection. Raw packet capture is ready.",Color.LTGRAY,11,false);
        diagnostic.setTextIsSelectable(true);diagnostic.setPadding(0,dp(12),0,0);root.addView(diagnostic);
    }

    private LinearLayout metric(String icon,String title,String value,String unit){
        LinearLayout card=panel();card.setMinimumHeight(dp(112));
        TextView i=label(icon,GREEN,20,true);card.addView(i);
        TextView t=label(title,Color.LTGRAY,10,true);
        LinearLayout.LayoutParams tp=params(-1,-2);tp.topMargin=dp(5);card.addView(t,tp);
        TextView v=label(value,Color.WHITE,27,true);
        LinearLayout.LayoutParams vp=params(-1,-2);vp.topMargin=dp(4);card.addView(v,vp);
        TextView u=label(unit,GREEN,10,false);card.addView(u);
        TextView state=label("WAITING FOR WATCH DATA",Color.LTGRAY,9,false);
        LinearLayout.LayoutParams stateParams=params(-1,-2);stateParams.topMargin=dp(5);card.addView(state,stateParams);
        if("HEART RATE".equals(title)){heart=v;heartState=state;}
        if("BLOOD OXYGEN".equals(title)){oxygen=v;oxygenState=state;}
        if("STEPS".equals(title)){steps=v;stepsState=state;}
        if("CALORIES".equals(title)){calories=v;caloriesState=state;}
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
        if(WatchDataStore.isConnected(this) || Qx11KeepAliveService.hasActiveConnection()){ setStatus("Your QX11 is already connected. Reusing the live session; no second connection is needed."); refreshStoredMetrics(); return; }
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
        if(WatchDataStore.isConnected(this) || Qx11KeepAliveService.hasActiveConnection()){ setStatus("QX11 is already connected. Keeping the existing live session."); refreshStoredMetrics(); return; }
        stopScan("Connecting to "+(QXII_MAC.equalsIgnoreCase(safeAddress(device))?"your QXII watch":"selected device")+"…");
        if(gatt!=null){Qx11KeepAliveService.release(this,gatt);try{gatt.disconnect();gatt.close();}catch(Exception ignored){}gatt=null;}
        connected=false;gattOperations.clear();gattOperationInFlight=false;enabledCharacteristics.clear();resetMeasurements();
        try{
            watchName.setText(QXII_MAC.equalsIgnoreCase(safeAddress(device))
                ?"QX11_AE96 • HELLO_WATCH • V006927"
                :"SELECTED BLUETOOTH WATCH");
            WatchDataStore.setConnected(this,false);
            gatt=device.connectGatt(this,false,gattCallback);
        }catch(SecurityException e){setStatus("Android denied Bluetooth connection permission.");}
    }

    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt source,int statusCode,int newState){
            if(newState==BluetoothProfile.STATE_CONNECTED){
                gatt=source;connected=true;WatchDataStore.setConnected(WatchActivity.this,true);Qx11KeepAliveService.retain(WatchActivity.this,source);runOnUiThread(()->setStatus("Connected. Keeping the QX11 session active while you use Q-BOIX…"));
                try{source.discoverServices();}catch(SecurityException e){runOnUiThread(()->setStatus("Connected, but service discovery permission failed."));}
            }else if(newState==BluetoothProfile.STATE_DISCONNECTED){
                connected=false;WatchDataStore.setConnected(WatchActivity.this,false);Qx11KeepAliveService.release(WatchActivity.this,source);runOnUiThread(()->{setStatus("Watch disconnected. Tap scan to reconnect.");if(scanButton!=null){scanButton.setText("SCAN FOR MY WATCH");scanButton.setEnabled(true);}resetMeasurements();});
                try{source.close();}catch(Exception ignored){}
            }
        }
        @Override public void onServicesDiscovered(BluetoothGatt source,int result){
            if(result!=BluetoothGatt.GATT_SUCCESS){runOnUiThread(()->diagnostic.setText("Could not read watch services (error "+result+")."));return;}
            int serviceCount=source.getServices().size();
            StringBuilder inventory=new StringBuilder("GATT INVENTORY • "+serviceCount+" services\n");
            for(BluetoothGattService svc:source.getServices()){
                inventory.append(svc.getUuid()).append("\n");
                for(BluetoothGattCharacteristic ch:svc.getCharacteristics()){
                    inventory.append("  ").append(ch.getUuid()).append(" [")
                        .append(ch.getProperties()).append("]\n");
                }
            }
            runOnUiThread(()->{
                setStatus("CONNECTED • QX11_AE96 • "+serviceCount+" Bluetooth services discovered");
                diagnostic.setText("WATCH INFO (from device Info screen) • NAME: QX11_AE96 • MODEL: HELLO_WATCH • FIRMWARE: V006927\n"+
                    inventory.toString()+"\nReading live values and available history. Old-dated records stay separate from today's totals.");
            });
            // QX11 V006927 battery is read through its GloryFit A2 reply, not the generic 2A19 value.
            enableHeartRate(source);
            enableOxygen(source);
            subscribeGloryFitChannels(source);
            markUnavailableIfMissing(source);
        }
        @Override public void onCharacteristicRead(BluetoothGatt source,BluetoothGattCharacteristic c,int result){
            if(result==BluetoothGatt.GATT_SUCCESS)handleMeasurement(c,c.getValue());
            finishGattOperation();
        }
        @Override public void onCharacteristicRead(BluetoothGatt source,BluetoothGattCharacteristic c,byte[] value,int result){
            if(result==BluetoothGatt.GATT_SUCCESS)handleMeasurement(c,value);
            finishGattOperation();
        }
        @Override public void onCharacteristicWrite(BluetoothGatt source,BluetoothGattCharacteristic c,int result){
            if(result!=BluetoothGatt.GATT_SUCCESS)runOnUiThread(()->diagnostic.setText("Watch command write failed ("+result+") for "+c.getUuid()));
            finishGattOperation();
        }
        @Override public void onCharacteristicChanged(BluetoothGatt source,BluetoothGattCharacteristic c){
            handleMeasurement(c,c.getValue());
        }
        @Override public void onCharacteristicChanged(BluetoothGatt source,BluetoothGattCharacteristic c,byte[] value){
            handleMeasurement(c,value);
        }
        @Override public void onDescriptorWrite(BluetoothGatt source,BluetoothGattDescriptor descriptor,int statusCode){
            UUID characteristicUuid=descriptor.getCharacteristic().getUuid();
            if(statusCode==BluetoothGatt.GATT_SUCCESS){
                enabledCharacteristics.add(characteristicUuid);
                runOnUiThread(()->recordDiagnosticEvent("NOTIFICATIONS ENABLED • "+characteristicUuid));
            }else{
                enabledCharacteristics.remove(characteristicUuid);
                runOnUiThread(()->diagnostic.setText("Watch rejected notification subscription for "+characteristicUuid+" (status "+statusCode+")."));
            }
            finishGattOperation();
        }
    };

    private void subscribeGloryFitChannels(BluetoothGatt source){
        int candidates=0;
        BluetoothGattService main=source.getService(GF_MAIN_SERVICE);
        BluetoothGattService data=source.getService(GF_DATA_SERVICE);
        gfMainWrite=main==null?null:main.getCharacteristic(GF_MAIN_WRITE);
        gfDataWrite=data==null?null:data.getCharacteristic(GF_DATA_WRITE);
        if(main!=null){
            BluetoothGattCharacteristic notify=main.getCharacteristic(GF_HR_NOTIFY);
            if(notify!=null){candidates++;subscribe(source,notify);}
        }
        if(data!=null){
            BluetoothGattCharacteristic notify=data.getCharacteristic(GF_DATA_NOTIFY);
            if(notify!=null){candidates++;subscribe(source,notify);}
        }
        final int count=candidates;
        runOnUiThread(()->diagnostic.setText("GloryFit channels: 55FF="+(main!=null?"FOUND":"MISSING")+
            ", 56FF="+(data!=null?"FOUND":"MISSING")+
            ". Notification channels found: "+count+". Enabling subscriptions, then requesting watch data."));

        if(gfMainWrite!=null){
            // Skip READs on command-write characteristics: they are not fitness
            // records. Removing these two unnecessary GATT operations speeds sync.
            // Read-only requests: no watch settings are changed by these commands.
            sendGloryFitCommand(source,0xA1);                 // firmware/version
            // A2 was repeatedly captured as constant 0x47 on this QX11; do not treat it as battery.
            // A2 returned the same 0x47 value in repeated captures; it is NOT a verified battery reading.
            // Experimental read-only daily aggregate query documented on a related
            // GloryFit/Moyoung watch. QX11 V006927 support is NOT yet confirmed.
            // Keep its reply in raw diagnostics until its exact response is verified.
            sendGloryFitCommand(source,0x26,0x01);              // candidate daily activity summary
            sendGloryFitCommand(source,0xB2,0xFA);             // step history
            sendGloryFitCommand(source,0xF7,0xFA);             // heart-rate history
            sendGloryFitCommand(source,0x34,0xFA);             // SpO2 history
            sendGloryFitCommand(source,0x31,0x01);             // sleep history
            sendGloryFitCommand(source,0xF7,0x01);             // enable continuous HR sampling
            handler.postDelayed(()->{
                if(connected && gfMainWrite!=null){
                    sendGloryFitCommand(source,0xD6,0x02);
                    handler.postDelayed(()->{
                        if(connected && gfMainWrite!=null)sendGloryFitCommand(source,0xE5,0x11);
                    },1500);
                }
            },700);
            handler.postDelayed(()->{
                if(connected && gfMainWrite!=null){
                    spo2SpotPending=true;spo2EarlyFailureSeen=false;
                    sendGloryFitCommand(source,0x34,0x11);
                    handler.postDelayed(()->{
                        if(connected&&spo2SpotPending){
                            spo2SpotPending=false;
                            runOnUiThread(()->setMetricState(oxygenState,
                                spo2EarlyFailureSeen?"NO FINAL SpO2 RESULT AFTER MEASUREMENT WINDOW":"MEASUREMENT NOT RECEIVED"));
                        }
                    },70000);
                }
            },2600);
        }else{
            runOnUiThread(()->diagnostic.setText("GloryFit command channel 33F1 is missing. Standard Bluetooth measurements may still work; custom history cannot be requested."));
        }
    }

    private void sendGloryFitCommand(BluetoothGatt source,int... bytes){
        byte[] packet=new byte[bytes.length];
        for(int i=0;i<bytes.length;i++)packet[i]=(byte)(bytes[i]&0xff);
        enqueueGattOperation(()->{
            try{
                if(gfMainWrite==null){finishGattOperation();return;}
                int props=gfMainWrite.getProperties();
                if((props&BluetoothGattCharacteristic.PROPERTY_WRITE)!=0)
                    gfMainWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                else if((props&BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)!=0)
                    gfMainWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                else{runOnUiThread(()->diagnostic.setText("GloryFit command characteristic is not writable."));finishGattOperation();return;}
                gfMainWrite.setValue(packet);
                if(!source.writeCharacteristic(gfMainWrite)){
                    runOnUiThread(()->diagnostic.setText("Watch rejected command start: "+hex(packet)));
                    finishGattOperation();
                }else{
                    runOnUiThread(()->diagnostic.setText("Requested watch data: "+hex(packet)+" • waiting for a real response."));
                }
            }catch(SecurityException e){
                runOnUiThread(()->diagnostic.setText("Bluetooth permission blocked a GloryFit data request."));
                finishGattOperation();
            }
        });
    }

    private String hex(byte[] data){
        StringBuilder b=new StringBuilder();
        for(byte value:data)b.append(String.format(Locale.US,"%02X ",value&255));
        return b.toString().trim();
    }

    private void enqueueGattOperation(Runnable operation){
        handler.post(()->{
            if(gatt==null||!connected)return;
            gattOperations.addLast(operation);
            runNextGattOperation();
        });
    }
    private void runNextGattOperation(){
        if(gattOperationInFlight||gattOperations.isEmpty()||gatt==null||!connected)return;
        Runnable next=gattOperations.removeFirst();
        gattOperationInFlight=true;
        final long operationId=++gattOperationSequence;
        activeGattTimeout=()->{
            if(gattOperationInFlight&&operationId==gattOperationSequence){
                gattOperationInFlight=false;
                activeGattTimeout=null;
                runOnUiThread(()->diagnostic.setText("A Bluetooth operation timed out. Continuing with the next queued request."));
                handler.postDelayed(this::runNextGattOperation,100);
            }
        };
        handler.postDelayed(activeGattTimeout,8000);
        next.run();
    }
    private void finishGattOperation(){
        handler.post(()->{
            if(!gattOperationInFlight)return;
            if(activeGattTimeout!=null){handler.removeCallbacks(activeGattTimeout);activeGattTimeout=null;}
            gattOperationInFlight=false;
            handler.postDelayed(this::runNextGattOperation,80);
        });
    }

    private void readBattery(BluetoothGatt source){
        BluetoothGattService service=source.getService(BATTERY_SERVICE);
        BluetoothGattCharacteristic c=service==null?null:service.getCharacteristic(BATTERY_LEVEL);
        if(c==null){runOnUiThread(()->battery.setText("BATTERY  •  Not provided by watch"));return;}
        // QX11 exposes Battery Level as READ + NOTIFY. Subscribe and read through the same queue.
        subscribe(source,c);
        enqueueGattOperation(()->{
            batteryReadStartedAt=System.currentTimeMillis();
            battery.setText("BATTERY  •  READ REQUEST SENT — WAITING");
            try{
                boolean started=source.readCharacteristic(c);
                if(!started){battery.setText("BATTERY  •  READ REQUEST NOT STARTED");finishGattOperation();}
            }catch(SecurityException e){
                battery.setText("BATTERY  •  READ BLOCKED BY PERMISSION");
                finishGattOperation();
            }
        });
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
        int props=c.getProperties();
        boolean notify=(props&BluetoothGattCharacteristic.PROPERTY_NOTIFY)!=0;
        boolean indicate=(props&BluetoothGattCharacteristic.PROPERTY_INDICATE)!=0;
        if(!notify&&!indicate){runOnUiThread(()->setUnsupported(c.getUuid()));return;}
        BluetoothGattDescriptor d=c.getDescriptor(CCCD);
        if(d==null){runOnUiThread(()->setUnsupported(c.getUuid()));return;}
        enqueueGattOperation(()->{
            try{
                // Local routing and remote CCCD configuration are both required.
                if(!source.setCharacteristicNotification(c,true)){
                    runOnUiThread(()->diagnostic.setText("Android could not enable local notifications for "+c.getUuid()));
                    finishGattOperation();return;
                }
                byte[] value=notify?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                boolean started;
                if(Build.VERSION.SDK_INT>=33){
                    started=source.writeDescriptor(d,value)==BluetoothStatusCodes.SUCCESS;
                }else{
                    d.setValue(value);
                    started=source.writeDescriptor(d);
                }
                if(!started){
                    runOnUiThread(()->diagnostic.setText("Could not start notification subscription for "+c.getUuid()));
                    finishGattOperation();
                }
                // Do NOT mark enabled here. onDescriptorWrite confirms the remote CCCD write.
            }catch(SecurityException e){
                runOnUiThread(()->diagnostic.setText("Bluetooth permission blocked a notification subscription."));
                finishGattOperation();
            }
        });
    }
    private void markUnavailableIfMissing(BluetoothGatt source){
        if(source.getService(HR_SERVICE)==null)runOnUiThread(()->heart.setText("N/A"));
        if(source.getService(PULSE_OX_SERVICE)==null)runOnUiThread(()->oxygen.setText("N/A"));
        // Steps and calories use manufacturer-specific GloryFit data on many watches.
        // Never display a fabricated value; wait for a supported decoder.
        runOnUiThread(()->{steps.setText("N/A");calories.setText("N/A");setMetricState(stepsState,"WAITING FOR DAILY STEP HISTORY");setMetricState(caloriesState,"NOT SENT IN DAILY SYNC • WORKOUT DATA ONLY IF PROVIDED");});
    }

    private void handleMeasurement(BluetoothGattCharacteristic c,byte[] data){
        // Capture before decoding: unknown formats and empty responses remain visible.
        recordRawPacket(c,data);
        if(data==null||data.length==0)return;
        UUID id=c.getUuid();
        if(HR_MEASURE.equals(id)) {
            Qx11DataEngine.Candidate candidate=Qx11DataEngine.standardHeartRate(data);
            if(candidate!=null) publishHeart(candidate.value,candidate.engine);
        }
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
            String channel=GF_HR_NOTIFY.equals(id)?"55FF / 33F2":"56FF / 34F2";
            String receivedAt=new java.text.SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date());
            String packet=channel+" • PACKET "+customPackets+" • "+receivedAt+" • "+hex(data);
            // A2 was constant 0x47 in repeated QX11 captures; it is not trusted as battery.
            if(GF_HR_NOTIFY.equals(id)&&data.length>=3&&(data[0]&255)==0xA1){
                String info=new String(data,1,data.length-1,java.nio.charset.StandardCharsets.US_ASCII).replace("\u0000","").trim();
                java.util.regex.Matcher firmware=java.util.regex.Pattern.compile("V[0-9]{4,}").matcher(info);
                String version=firmware.find()?firmware.group():"INFO RECEIVED";
                runOnUiThread(()->watchName.setText("QX11_AE96 • HELLO_WATCH • "+version));
            }
            if(GF_HR_NOTIFY.equals(id)&&data.length>=4&&(data[0]&255)==0xE5&&(data[1]&255)==0x11){
                Qx11DataEngine.Candidate x=Qx11DataEngine.gloryFitE5(data);
                if(x!=null)publishHeart(x.value,x.engine);
            }
            boolean stepsDecoded=false;
            if(GF_HR_NOTIFY.equals(id)&&data.length>0&&
                    ((data[0]&255)==0xB1||(data[0]&255)==0xB2))
                stepsDecoded=parseGloryFitSteps(data);
            if(GF_HR_NOTIFY.equals(id)&&(data[0]&255)==0xFD&&data.length==14){
                // FD workout metrics are accepted only when the packet carries valid ranges.
                Qx11DataEngine.Candidate[] decoded=Qx11DataEngine.gloryFitWorkout(data);
                int workoutSteps=((data[7]&255)<<16)|((data[8]&255)<<8)|(data[9]&255);
                for(Qx11DataEngine.Candidate candidate:decoded){
                    if("heart".equals(candidate.metric))publishHeart(candidate.value,candidate.engine);
                    if("calories".equals(candidate.metric)){
                        WatchDataStore.put(this,"calories",candidate.value,candidate.engine);
                        runOnUiThread(()->{calories.setText(String.valueOf(candidate.value));setMetricState(caloriesState,"WORKOUT CALORIES RECEIVED");});
                    }
                }
                if(workoutSteps>0){
                    WatchDataStore.put(this,"steps",workoutSteps,"QX11 FD live workout");
                    runOnUiThread(()->{steps.setText(String.valueOf(workoutSteps));setMetricState(stepsState,"LIVE WORKOUT STEP COUNT");});
                }
            }
            if(GF_HR_NOTIFY.equals(id)&&(data[0]&255)==0x34){
                int spo2=GloryFitDecoder.decodeSpO2(data);
                if(spo2>=70&&spo2<=100){
                    WatchDataStore.put(this,"spo2",spo2,"QX11 34 spot result");
                    spo2SpotPending=false;spo2EarlyFailureSeen=false;
                    runOnUiThread(()->{oxygen.setText(spo2+"%");setMetricState(oxygenState,"SPOT MEASUREMENT RECEIVED • GLORYFIT");});
                } else if(data.length>=4&&(data[3]&255)==0xFF){
                    spo2EarlyFailureSeen=true;
                    runOnUiThread(()->setMetricState(oxygenState,"MEASURING • IGNORING EARLY EMPTY RESPONSE"));
                }
            }
            if(GF_HR_NOTIFY.equals(id)&&data.length>0){
                int opcode=data[0]&255;
                if((opcode==0xB1||opcode==0xB2)&&!stepsDecoded)
                    runOnUiThread(()->setMetricState(stepsState,"STEP PACKET RECEIVED • FORMAT/DATE NOT CONFIRMED"));
                if(opcode==0xF7)runOnUiThread(()->{
                    if("—".contentEquals(heart.getText())||"N/A".contentEquals(heart.getText()))
                        setMetricState(heartState,"HEART HISTORY PACKET RECEIVED • VALUE NOT DECODED");
                });
            }
            if(GF_DATA_NOTIFY.equals(id)&&data.length>0){
                int opcode=data[0]&255;
                if(opcode==0x34)runOnUiThread(()->setMetricState(oxygenState,"OXYGEN PACKET RECEIVED • DECODER PENDING"));
                if(opcode==0x32)runOnUiThread(()->diagnostic.setText(packet+"\nSleep history packet received on the 56FF data channel."));
            }
            runOnUiThread(()->appendPacketLog(packet));
        }
        else if(PULSE_OX_CONTINUOUS.equals(id)||PULSE_OX_SPOT.equals(id)){
            // IEEE-11073 SFLOAT is used by the standard Pulse Oximeter service.
            // Do not mislabel unsupported/invalid values as a saturation percentage.
            if(data.length>=3){int flags=data[0]&255;int raw=(data[1]&255)|((data[2]&255)<<8);double spo2=decodeSfloat(raw);
                if((flags&0x01)==0&&spo2>=0&&spo2<=100)runOnUiThread(()->{oxygen.setText(String.format(Locale.US,"%.0f%%",spo2));setMetricState(oxygenState,"DATA RECEIVED FROM WATCH");});}
        }
    }
    private boolean parseGloryFitSteps(byte[] data){
        // QX11 capture confirms the 18-byte B1 record date order is
        // YEAR (big-endian), MONTH, DAY, HOUR, followed by a 16-bit
        // hourly step total (big-endian). Example: 07 EA 0B 02 0A = 2026-11-02 10:00.
        GloryFitDecoder.StepRecord record=GloryFitDecoder.decodeStepRecord(data);
        if(record==null)return false;
        String date=record.date;
        int hour=record.hour;
        int hourlyTotal=record.steps;
        String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        String key=date+"-"+String.format(Locale.US,"%02d",hour);
        // B1 is observed repeatedly as the same hour's counter increases.
        // Replace that hour's previous value; never add each notification.
        hourlySteps.put(key,hourlyTotal);
        if(!date.equals(today)){
            final String recordLabel=date+" "+String.format(Locale.US,"%02d:00",hour);
            runOnUiThread(()->setMetricState(stepsState,
                "HISTORY RECEIVED • "+recordLabel+" • "+hourlyTotal+" STEPS IN THIS HOUR (NOT TODAY)"));
            return true;
        }
        int dailyTotal=0;
        for(Map.Entry<String,Integer> entry:hourlySteps.entrySet())
            if(entry.getKey().startsWith(today+"-"))dailyTotal+=entry.getValue();
        final int displayTotal=dailyTotal;
        final int hours=countTodayHours(today);
        WatchDataStore.put(this,"steps",displayTotal,"QX11 B1/B2 daily sum");
        runOnUiThread(()->{
            steps.setText(String.format(Locale.US,"%d",displayTotal));
            setMetricState(stepsState,"TODAY • "+hours+" HOURLY RECORD(S) • LATEST VALUE PER HOUR");
        });
        return true;
    }
    private int countTodayHours(String date){
        int count=0;for(String key:hourlySteps.keySet())if(key.startsWith(date+"-"))count++;return count;
    }

    private void parseHeartRate(byte[] data){
        if(data.length<2)return;int flags=data[0]&255;
        if((flags&1)!=0&&data.length<3)return;
        int bpm=(flags&1)==0?(data[1]&255):((data[2]&255)<<8)|(data[1]&255);
        if(bpm>0&&bpm<260)publishHeart(bpm,"BLE 2A37");
    }
    private void publishHeart(int bpm,String source){
        WatchDataStore.put(this,"heart",bpm,source);
        runOnUiThread(()->{heart.setText(String.valueOf(bpm));setMetricState(heartState,"LIVE • "+source);});
    }
    private double decodeSfloat(int raw){
        int mantissa=raw&0x0FFF;if((mantissa&0x0800)!=0)mantissa|=0xFFFFF000;
        int exponent=(raw>>12)&0x0F;if((exponent&0x08)!=0)exponent|=0xFFFFFFF0;
        if(mantissa==0x07FE||mantissa==0x07FF||mantissa==0x0800||mantissa==0x0801)return -1;
        return mantissa*Math.pow(10,exponent);
    }
    private void recordRawPacket(BluetoothGattCharacteristic c,byte[] data){
        rawPacketCount++;
        String time=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US).format(new Date());
        String service="unknown";
        try{if(c.getService()!=null)service=c.getService().getUuid().toString();}catch(Exception ignored){}
        String bytes=data==null?"<null>":hex(data);
        int length=data==null?0:data.length;
        String entry="#"+rawPacketCount+" • "+time+" • INCOMING GATT DATA\\n"+
            "SERVICE: "+service+"\\nCHARACTERISTIC: "+c.getUuid()+"\\nLENGTH: "+length+" bytes\\nRAW HEX: "+(bytes.isEmpty()?"<empty>":bytes);
        packetHistory.addFirst(entry);
        while(packetHistory.size()>40)packetHistory.removeLast();
        renderRawDiagnostics();
    }
    private void recordDiagnosticEvent(String event){
        String time=new java.text.SimpleDateFormat("HH:mm:ss.SSS",Locale.US).format(new Date());
        packetHistory.addFirst(time+" • EVENT • "+event);
        while(packetHistory.size()>40)packetHistory.removeLast();
        renderRawDiagnostics();
    }
    private void renderRawDiagnostics(){
        StringBuilder log=new StringBuilder("RAW RECEIVE CAPTURE • "+rawPacketCount+" PACKETS\\n");
        log.append("Each entry includes the GATT service, characteristic, byte length and original HEX bytes.\\n");
        log.append("Unknown packets are retained; a received packet does not automatically mean it was decoded.\\n\\n");
        for(String line:packetHistory)log.append(line).append("\\n\\n");
        diagnosticsText=log.toString();
        runOnUiThread(()->diagnostic.setText(diagnosticsText));
    }
    private void appendPacketLog(String packet){
        // Preserve decoder notes alongside the independent raw capture.
        recordDiagnosticEvent("DECODE NOTE • "+packet);
    }
    private void copyDiagnostics(){
        android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard==null){setStatus("Clipboard is unavailable.");return;}
        String text=diagnosticsText.isEmpty()?"No incoming packets captured yet. Connect the QX11 and wait for data.":diagnosticsText;
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Q-BOIX raw Bluetooth diagnostics",text));
        setStatus("Raw diagnostics copied. Paste them into this chat when ready.");
    }

    private void setUnsupported(UUID id){
        if(HR_MEASURE.equals(id))heart.setText("N/A");
        if(PULSE_OX_CONTINUOUS.equals(id)||PULSE_OX_SPOT.equals(id))oxygen.setText("N/A");
    }
    private void refreshStoredMetrics(){
        if(heart!=null){int v=WatchDataStore.get(this,"heart");heart.setText(v<0?"—":String.valueOf(v));setMetricState(heartState,v<0?"WAITING FOR WATCH DATA":"LATEST WATCH READING");}
        if(oxygen!=null){int v=WatchDataStore.get(this,"spo2");oxygen.setText(v<0?"—":v+"%");setMetricState(oxygenState,v<0?"WAITING FOR WATCH DATA":"LATEST SPOT MEASUREMENT");}
        if(steps!=null){int v=WatchDataStore.get(this,"steps");steps.setText(v<0?"—":String.valueOf(v));setMetricState(stepsState,v<0?"WAITING FOR WATCH DATA":"LATEST WATCH READING");}
        if(calories!=null){int v=WatchDataStore.get(this,"calories");calories.setText(v<0?"—":String.valueOf(v));setMetricState(caloriesState,v<0?"WAITING FOR WATCH DATA":"LATEST WATCH READING");}
        if(battery!=null && !WatchDataStore.isConnected(this)) battery.setText("BATTERY • Watch disconnected");
    }

    private void resetMeasurements(){
        if(heart!=null)heart.setText("—");if(oxygen!=null)oxygen.setText("—");
        if(steps!=null)steps.setText("—");if(calories!=null)calories.setText("—");
        if(battery!=null)battery.setText("BATTERY  •  Not connected"); customPackets=0;
        rawPacketCount=0;packetHistory.clear();diagnosticsText="";
    }
    private void setMetricState(TextView view,String message){if(view!=null)view.setText(message);}
    private void setStatus(String message){if(status!=null)status.setText(message);}

    @Override protected void onResume(){
        super.onResume();
        refreshStoredMetrics(); handler.removeCallbacks(liveUiRefresh); handler.post(liveUiRefresh);
        // The watch connection belongs to this Activity instance. Returning from
        // Dashboard must reuse the existing GATT session, not start a new one.
        if(gatt!=null && connected){
            setStatus("CONNECTED • QX11_AE96 • Live watch session retained");
        }else if(WatchDataStore.isConnected(this)){
            setStatus("Watch session is restoring. Check Bluetooth connection.");
        }
    }

    @Override protected void onDestroy(){
        // Do not disconnect or close GATT here. Android normally keeps this
        // Activity stopped in the back stack when the user opens Dashboard or
        // another Q-BOIX screen. Closing GATT here caused every navigation away
        // from Watch to drop the watch connection.
        stopScan(""); handler.removeCallbacks(liveUiRefresh);
        super.onDestroy();
    }
}
