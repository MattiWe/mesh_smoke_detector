package com.mmbuw.bachelor.meshsmokedetector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.IntentFilter;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Leaves on 7/1/2016.
 */
public class BleScanResults {
    private static final String TAG = "BleScanResults";

    public static final int SCAN_RESULT_SUCCESS = 1;
    public static final int SCAN_RESULT_DUPLICATE = 0;
    public static final int SCAN_RESULT_FAILED = 0;
    public static final int SCAN_RESULT_ALARM_DETECTED = 2;
    public static final int SCAN_RESULT_SIZE_CHANGED = 3;
    public static final int SCAN_RESULT_BATTERY_WARNING = 4;

    private static BleScanResults sBleScanResults;
    private ArrayList<ScanResult> mScanResults;
    private Integer[] mMeshStatus = new Integer[7];

    private BleScanResults(){
        this.clear();
    }

    public static BleScanResults getInstance(){
        if(sBleScanResults == null) sBleScanResults = new BleScanResults();
        return sBleScanResults;
    }

    public void clear(){
        mScanResults = new ArrayList<>();
    }

    public int addScanResult(ScanResult scanResult){
        // confirm exclusivity of entries
        try {
            ArrayList<ScanResult> newList = new ArrayList<>();
            if(scanResult.getScanRecord().getDeviceName().startsWith("rbc_mesh")){
                newList.add(scanResult);
            }
            for(ScanResult s : mScanResults){
                if (s.getScanRecord().getDeviceName()
                        .equals(scanResult.getScanRecord().getDeviceName())) {
                    // don't add duplicates, only most recent scan
                }else{
                    newList.add(s);
                }
            }
            mScanResults = newList;
            mMeshStatus = getAdvertisementData(scanResult);
            return SCAN_RESULT_SUCCESS;
        }catch(NullPointerException e){
            Log.e("BleScanResults", "Nullptr reading scanResult: " + e.toString());
            return SCAN_RESULT_FAILED;
        }
    }

    public static String getAdvertisementDataAsString(ScanResult current) throws NullPointerException{
        StringBuilder s = new StringBuilder();
        Integer[] i = getAdvertisementData(current);
        for(int entry : i){
            s.append(entry);
        }
        return s.toString();
    }

    public static Integer[] getAdvertisementData(ScanResult current) throws NullPointerException{
        Integer[] returnValue = new Integer[7];
        for(int i = 0; i < 7; i++ ){
            Log.d(TAG, String.format("%02x", current.getScanRecord().getBytes()[i+24]));
            try{
                returnValue[i] = Integer.valueOf(String.format("%02x", current.getScanRecord().getBytes()[i+24]));
            }catch (NumberFormatException e){
                returnValue[i] = 00;
            }
        }
        return returnValue;
    }

    public ArrayList<ScanResult> getScanResultList(){
        return mScanResults;
    }
    public Integer[] getMeshStatus(){ return mMeshStatus; }
    public BluetoothDevice getLatestResult() throws ArrayIndexOutOfBoundsException{
        return mScanResults.get(mScanResults.size()-1).getDevice();
    }

    public void printDebug(){
        if(mScanResults == null) mScanResults = new ArrayList<>();
        if(mScanResults.size() == 0 ) {
            Log.d(TAG, "no scan results in storage");
            return;
        }
        for(ScanResult s : mScanResults){
            StringBuilder hexString = new StringBuilder();
            for(byte b : s.getScanRecord().getBytes()) hexString.append(String.format("%02x", b));
            Log.d(TAG, "Store: " + s.getScanRecord().getDeviceName() + " --  " + hexString.toString());
        }
    }
}
