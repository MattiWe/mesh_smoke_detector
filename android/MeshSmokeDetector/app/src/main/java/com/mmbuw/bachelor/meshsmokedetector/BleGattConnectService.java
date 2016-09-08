package com.mmbuw.bachelor.meshsmokedetector;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import android.os.Handler;

/**
 * Created by Leaves on 7/3/2016.
 */
public class BleGattConnectService extends Service {

    private static final String TAG = BleScanService.class.getName();
    private static boolean isGattConnected = false;
    private static Context mContext;
    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothGatt mBluetoothGatt;
    private static int mGattAction;

    BroadcastReceiver mBroadcastReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){

        // Has the device BLE?
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "BLE not supported");
            return;
        }
        // Init BluetoothAdapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Adapter initialized and BT enabled?
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "BLE not enabled");
            return;
        }

        mContext = getApplicationContext();

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("GATT_ALARM")) {
                    if(isGattConnected) return;
                    isGattConnected = true;
                    connectToLatestGattDevice();
                    mGattAction = 1;
                }else if (intent.getAction().equals("GATT_MUTE")) {
                    if(isGattConnected) return;
                    isGattConnected = true;
                    connectToLatestGattDevice();
                    mGattAction = 0;
                }else if (intent.getAction().equals("GATT_DISCONNECT")) {
                    if(!isGattConnected) return;
                    disconnectGatt();
                } else {
                    Log.e(TAG, "unknown intent: " + intent.toString());
                }

                Log.v(TAG, "intent accepted: " + intent.toString());
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("GATT_ALARM");
        intentFilter.addAction("GATT_MUTE");
        intentFilter.addAction("DISCONNECT");
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private static void writeAlarmOnToMesh(int action){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter not initialized");
            disconnectGatt();
            return;
        }

        BluetoothGattService meshService = mBluetoothGatt.getService(UUID.fromString("0000fee4-0000-1000-8000-00805f9b34fb"));
        if(meshService == null){
            Log.d(TAG, "Custom BLE Service not found");
            disconnectGatt();
            return;
        }

        BluetoothGattCharacteristic writeCharacteristic = meshService.getCharacteristic(UUID.fromString("2a1e0005-fd51-d882-8ba8-b98c0000cd1e"));
        byte[] value;
        if(action==1) value = hexStringToByteArray("0001000101");
        else value = hexStringToByteArray("0001000100");
        writeCharacteristic.setValue(value);
        if(mBluetoothGatt.writeCharacteristic(writeCharacteristic) == false){
            Log.d(TAG, "Failed to write characteristic");
            disconnectGatt();
        }


    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static void connectToLatestGattDevice(){
        try {
            mBluetoothGatt = BleScanResults.getInstance().getLatestResult().connectGatt(mContext, false, mGattCallback);
        }catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            Log.d(TAG, "no current scan results");
        }

    }

    private static final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(isGattConnected){
                Log.d(TAG, "ConnectionStateChanged");
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "Services Discovered");
            List<BluetoothGattService> list =  gatt.getServices();
            for(BluetoothGattService s : list){
                Log.d(TAG, s.getUuid().toString());
            }
            if(mGattAction==1) writeAlarmOnToMesh(1);
            else writeAlarmOnToMesh(0);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "Characteristic changed");
            disconnectGatt();
        }
    };

    private static void disconnectGatt(){
        if (mBluetoothGatt == null || !isGattConnected) {
            return;
        }
        Looper.prepare();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isGattConnected = false;
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
                Log.v(TAG, "GATT disconnected");
            }
        }, 1000);
        Looper.loop();
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        disconnectGatt();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        Log.v(TAG, "GATT Service Destroyed");
        super.onDestroy();
    }
}
