package com.mmbuw.bachelor.meshsmokedetector;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Leaves on 7/1/2016.
 */
public class BleScanService extends Service{

    private static final String TAG = BleScanService.class.getName();
    public static final int STATUS_SCAN_ACTIVE = 0;
    public static final int STATUS_IDLE        = 1;
    public static final int STATUS_DISABLED    = 2;
    private static final long SCAN_DURATION_TIMEOUT      = 3000; // time in between scans
    private static final long SCAN_DURATION = 10000;  // how long a single scan lasts

    private IBinder mBinder;
    private boolean mAllowRebind;
    private BleIntentReceiver mBleIntentReceiver;
    Timer mTimer = new Timer();
    private static Context mContext;

    private int scanStatus = STATUS_DISABLED;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler handler;

    @Override
    public void onCreate(){
        handler = new Handler();

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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SCAN_EVENT_START");
        intentFilter.addAction("SCAN_EVENT_STOP");
        mBleIntentReceiver = new BleIntentReceiver();
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mBleIntentReceiver, intentFilter);

        Log.d(TAG, "BLE Service Started");
        // and start right away
        startScan();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopScan();
        return mAllowRebind;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBleIntentReceiver);
        stopScan();
        Log.v(TAG, "BLE Service Destroyed");
    }

    public void startScan(){
        if(scanStatus == STATUS_SCAN_ACTIVE) return;

        mTimer.cancel();
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "BLE Scan Start");
                scanStatus = BleScanService.STATUS_SCAN_ACTIVE;
                mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finishScan();
                    }
                }, SCAN_DURATION);
            }
        }, 0, SCAN_DURATION + SCAN_DURATION_TIMEOUT);
    }

    public void startSingleScan(){
        if(scanStatus == STATUS_SCAN_ACTIVE) return;

        Log.d(TAG, "BLE Scan Start");
        scanStatus = BleScanService.STATUS_SCAN_ACTIVE;
        mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finishScan();
            }
        }, SCAN_DURATION);
    }

    public void finishScan(){
        scanStatus = STATUS_IDLE;
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        Log.d(TAG, "BLE Scan Finished");
    }

    public void stopScan(){
        if (scanStatus == STATUS_DISABLED) return;
        finishScan();
        mTimer.cancel();
        mTimer = new Timer();
        scanStatus = STATUS_DISABLED;
    }

    private static ScanCallback mLeScanCallback = new ScanCallback(){
        @Override
        public void onScanResult(int callbackType, ScanResult result){
            int scanResultCode = BleScanResults.getInstance().addScanResult(result);
            if(scanResultCode >= 1){
                Intent in = new Intent("STORAGE_CHANGED");
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
            }
        }

        @Override
        public void onScanFailed(int errorCode){
            Log.e(TAG, "Scan failed with error code: "+errorCode);
        }
    };

    // BroadcastReceiver for Start/Stop Events
    class BleIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("SCAN_EVENT_START")) {
                startScan();
            } else if (intent.getAction().equals("SCAN_EVENT_STOP")) {
                stopScan();
            } else {
                Log.e(TAG, "unknown intent: " + intent.toString());
            }

            Log.v(TAG, "intent accepted: " + intent.toString());
        }
    }
}
