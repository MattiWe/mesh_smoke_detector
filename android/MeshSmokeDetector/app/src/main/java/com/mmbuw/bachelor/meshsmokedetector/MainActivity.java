package com.mmbuw.bachelor.meshsmokedetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_host, new StatusViewFragment(), "StatusViewFragment").commit();

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        startService(new Intent(this, BleScanService.class));
    }

    @Override
    protected void onResume(){
        super.onResume();
        startService(new Intent(this, BleScanService.class));
    }

    @Override
    protected void onPause(){
        stopService(new Intent(this, BleScanService.class));
        super.onPause();
    }

    @Override
    protected void onStop(){
        stopService(new Intent(this, BleScanService.class));
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if(requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Main Activity", "location permission granted");
        } else {
            Log.d("Main Activity", "location permission denied");
        }

    }
}
