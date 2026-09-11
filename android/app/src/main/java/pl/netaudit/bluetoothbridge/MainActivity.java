package pl.netaudit.bluetoothbridge;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends Activity {
    private static final int REQUEST_BT = 41;
    private final Map<String, JSONObject> devices = new ConcurrentHashMap<>();
    private BluetoothAdapter adapter;
    private BluetoothLeScanner bleScanner;
    private TextView status;
    private volatile boolean scanning;
    private volatile boolean serverRunning;
    private ServerSocket server;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(32,48,32,32); box.setBackgroundColor(0xff08111b);
        TextView title=new TextView(this); title.setText("Net Audit Bluetooth Bridge"); title.setTextSize(24); title.setTextColor(0xffeaf2ff); box.addView(title);
        status=new TextView(this); status.setTextColor(0xffeaf2ff); status.setPadding(0,24,0,24); box.addView(status);
        Button grant=new Button(this); grant.setText("Nadaj uprawnienia"); grant.setOnClickListener(v->requestPermissionsNow()); box.addView(grant);
        Button start=new Button(this); start.setText("Uruchom skan BLE + Classic"); start.setOnClickListener(v->startScan()); box.addView(start);
        Button stop=new Button(this); stop.setText("Zatrzymaj skan"); stop.setOnClickListener(v->stopScan()); box.addView(stop);
        ScrollView scroll=new ScrollView(this); scroll.addView(box); setContentView(scroll);
        BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE); adapter=manager==null?null:manager.getAdapter();
        registerReceiver(classicReceiver,new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(classicReceiver,new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        startServer(); refreshStatus();
    }

    private boolean allowed(){ return Build.VERSION.SDK_INT<31 || (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED); }
    private void requestPermissionsNow(){
        if(Build.VERSION.SDK_INT>=31) requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},REQUEST_BT);
        else if(Build.VERSION.SDK_INT>=23) requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_BT);
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){ super.onRequestPermissionsResult(r,p,g); refreshStatus(); }
    private void refreshStatus(){ runOnUiThread(()->status.setText("API: http://127.0.0.1:8766\nBluetooth: "+(adapter==null?"BRAK":adapter.isEnabled()?"WŁĄCZONY":"WYŁĄCZONY")+"\nUprawnienia: "+(allowed()?"OK":"BRAK")+"\nSkanowanie: "+(scanning?"TAK":"NIE")+"\nUrządzenia: "+devices.size())); }

    private final ScanCallback bleCallback=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult result){ addDevice(result.getDevice(),"BLE",result.getRssi()); }
        @Override public void onScanFailed(int code){ scanning=false; refreshStatus(); }
    };
    private final BroadcastReceiver classicReceiver=new BroadcastReceiver(){ @Override public void onReceive(Context c,Intent i){
        if(BluetoothDevice.ACTION_FOUND.equals(i.getAction())){ BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); short r=i.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE); if(d!=null)addDevice(d,"CLASSIC",r); }
        if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(i.getAction())) refreshStatus();
    }};
    private void addDevice(BluetoothDevice d,String transport,int rssi){
        try{ String address=d.getAddress(); String key=transport+":"+address; JSONObject j=new JSONObject(); j.put("address",address); j.put("name",allowed()?JSONObject.wrap(d.getName()):JSONObject.NULL); j.put("transport",transport); j.put("rssi",rssi==Short.MIN_VALUE?JSONObject.NULL:rssi); j.put("bond_state",d.getBondState()); j.put("captured_at",System.currentTimeMillis()); j.put("source","REAL_ANDROID_API"); devices.put(key,j); refreshStatus(); }catch(Exception ignored){}
    }
    private synchronized boolean startScan(){
        if(adapter==null||!adapter.isEnabled()||!allowed()) return false;
        devices.clear(); scanning=true;
        try{ bleScanner=adapter.getBluetoothLeScanner(); if(bleScanner!=null)bleScanner.startScan(bleCallback); adapter.startDiscovery(); refreshStatus(); return true; }
        catch(SecurityException e){ scanning=false; refreshStatus(); return false; }
    }
    private synchronized void stopScan(){
        try{ if(bleScanner!=null&&allowed())bleScanner.stopScan(bleCallback); if(adapter!=null&&allowed())adapter.cancelDiscovery(); }catch(SecurityException ignored){}
        scanning=false; refreshStatus();
    }
    private JSONObject snapshot(){
        JSONObject root=new JSONObject(); JSONArray a=new JSONArray(); for(JSONObject j:devices.values())a.put(j);
        try{ root.put("status",allowed()?"OK":"PERMISSION_REQUIRED"); root.put("source","REAL_ANDROID_API"); root.put("scanning",scanning); root.put("devices",a); }catch(Exception ignored){} return root;
    }
    private void startServer(){
        if(serverRunning)return; serverRunning=true;
        new Thread(()->{ try{ server=new ServerSocket(8766,8,InetAddress.getByName("127.0.0.1")); while(serverRunning)handle(server.accept()); }catch(Exception e){ serverRunning=false; refreshStatus(); } },"bridge-http").start();
    }
    private void handle(Socket s){
        try(Socket socket=s; BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8)); OutputStream out=socket.getOutputStream()){
            String line=in.readLine(); String path=(line==null)?"/":line.split(" ")[1]; JSONObject body;
            if("/scan/start".equals(path)){ body=new JSONObject().put("started",startScan()).put("source","REAL_ANDROID_API"); }
            else if("/scan/stop".equals(path)){ stopScan(); body=new JSONObject().put("stopped",true).put("source","REAL_ANDROID_API"); }
            else if("/devices".equals(path)||"/status".equals(path)){ body=snapshot(); }
            else body=new JSONObject().put("status","NOT_FOUND");
            byte[] data=body.toString().getBytes(StandardCharsets.UTF_8); String h="HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: "+data.length+"\r\nConnection: close\r\n\r\n"; out.write(h.getBytes(StandardCharsets.UTF_8)); out.write(data);
        }catch(Exception ignored){}
    }
    @Override protected void onDestroy(){ stopScan(); serverRunning=false; try{if(server!=null)server.close();}catch(Exception ignored){} unregisterReceiver(classicReceiver); super.onDestroy(); }
}
