package com.mmbuw.bachelor.meshsmokedetector;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Leaves on 7/1/2016.
 */
public class StatusViewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private static final String TAG = StatusViewFragment.class.getName();

    static private View mRootView;
    private TextView mAlarmStatusView;
    //private TextView mNodeCountView;
    //private TextView mBatteryLowSensorView;
    //private TextView mBatteryLowNodeView;

    private ArrayAdapter<ScanResult> adapter;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState){
        mRootView = inflater.inflate(R.layout.status_view_fragment, container, false);
        ((SwipeRefreshLayout)mRootView.findViewById(R.id.swipe_refresh_layout)).setOnRefreshListener(this);

        registerUpdateReceiver();

        mAlarmStatusView = (TextView)mRootView.findViewById(R.id.textview_alarm_staus);
        //mNodeCountView = (TextView)mRootView.findViewById(R.id.textview_node_nr);
        //mBatteryLowSensorView = (TextView)mRootView.findViewById(R.id.textview_battery_warning_detector);
        //mBatteryLowNodeView = (TextView)mRootView.findViewById(R.id.textview_battery_warning_node);

        adapter = new ResultAdapter(getContext(), R.layout.list_view_entries, new ArrayList<ScanResult>());
        ListView listView = (ListView)mRootView.findViewById(R.id.listView);
        listView.setAdapter(adapter);

        Button muteMeshButton = (Button) mRootView.findViewById(R.id.button_mesh_alarm_mute);
        muteMeshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent("GATT_MUTE");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(in);
            }
        });

        Button testAlarmButton = (Button) mRootView.findViewById(R.id.button_start_test_alarm);
        testAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent("GATT_ALARM");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(in);
            }
        });

        return mRootView;

    }

    private void handleMeshAdvChange(){
        Log.d(TAG, "meshAdvChange called");
        Integer[] meshStatus = BleScanResults.getInstance().getMeshStatus();
        Log.d(TAG, meshStatus.toString());
        if(meshStatus[0]==01){ // set Alarm Status
            mAlarmStatusView.setText("Alarm");
            mAlarmStatusView.setTextColor(getResources().getColor(R.color.red));
        }else{
            mAlarmStatusView.setText("Quiet");
            mAlarmStatusView.setTextColor(getResources().getColor(R.color.green));
        }
        /*
        mNodeCountView.setText(String.valueOf(meshStatus[2]));
        if(meshStatus[3] == 00) mBatteryLowSensorView.setText("None");
        else mBatteryLowSensorView.setText("THIS SHOULD BE AN ID ");
        if(meshStatus[4] == 00) mBatteryLowNodeView.setText("None");
        else mBatteryLowNodeView.setText("THIS SHOULD BE AN ID ");
        */
    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("STORAGE_CHANGED")) {
                handleMeshAdvChange();
                updateView();
            } else {
                Log.e(TAG, "unknown intent: " + intent.toString());
            }
            Log.v(TAG, "intent accepted: " + intent.toString());
        }
    };

    private class ResultAdapter extends ArrayAdapter<ScanResult> {
        private List<ScanResult> resultList;

        public ResultAdapter(Context context, int resID, List<ScanResult> results){
            super(context, resID);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent){
            View v = view;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.list_view_entries, null);
            }

            ScanResult result = getItem(position);

            if(result != null){
                TextView textView1 = (TextView) v.findViewById(R.id.textview_list_item_name);
                textView1.setText(result.getScanRecord().getDeviceName());

                TextView textView2 = (TextView) v.findViewById(R.id.textview_list_item_advdata);
                textView2.setText(BleScanResults.getAdvertisementDataAsString(result));
            }

            return v;
        }
    }

    private void updateView() {
        if (adapter != null) {
            adapter.clear();
            ArrayList<ScanResult> sr = BleScanResults.getInstance().getScanResultList();
            for (ScanResult scanResult : sr) {
                adapter.add(scanResult);
            }
        }
    }


    @Override
    public void onPause(){
        Intent in = new Intent("SCAN_EVENT_STOP");
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(in);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(updateReceiver);
        getContext().stopService(new Intent(getContext(), BleGattConnectService.class));
        super.onPause();
    }

    @Override
    public void onResume(){
        registerUpdateReceiver();
        Intent in = new Intent("SCAN_EVENT_START");
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(in);
        getContext().startService(new Intent(getContext(), BleGattConnectService.class));
        super.onResume();
    }

    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(updateReceiver);
        super.onDestroy();
    }

    @Override
    public void onRefresh() {
        BleScanResults.getInstance().printDebug();
        BleScanResults.getInstance().clear();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ((SwipeRefreshLayout)mRootView.findViewById(R.id.swipe_refresh_layout)).setRefreshing(false);
            }
        }, 1000);
    }

    public void registerUpdateReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction("STORAGE_CHANGED");
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(updateReceiver, filter);
    }
}
