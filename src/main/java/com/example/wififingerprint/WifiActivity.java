package com.example.wififingerprint;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class WifiActivity extends Activity {

    private static final int REQUEST_CODE_PERMISSION = 1;
    String mPermission = Manifest.permission.CHANGE_WIFI_STATE;
    String wPermission = Manifest.permission.WAKE_LOCK;

    private void getPermission() {
        Log.d("WifiActivity", "Requestion permission");
        if (checkSelfPermission(mPermission) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(wPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.d("WifiActivity", "No permission, requesting");
            ActivityCompat.requestPermissions(this,
                    new String[]{mPermission, wPermission},
                    REQUEST_CODE_PERMISSION);
            return;
        }

        else {
            Log.d("WifiActivity", "Permission granted already");
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e("Req Code", "" + requestCode);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            getPackageManager();
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return;
            }
            else{
                this.getPermission();
            }
        }
    }

    private static final String TAG = WifiActivity.class.getSimpleName();

    private static final long WIFI_SCAN_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private WifiManager wifiManager;
    private WifiScanBroadcastReceiver wifiScanBroadcastReceiver = new WifiScanBroadcastReceiver();
    private WifiLock wifiLock;

    private volatile boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.getPermission();

        Log.d("WifiActivity", "onCreate");

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, WifiActivity.class.getName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        Log.d("WifiActivity", "onResume");


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED) {
            wifiLock.acquire();

            registerReceiver(wifiScanBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 12);
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 24);
            }

            final Handler wifiScanHandler = new Handler();
            Runnable wifiScanRunnable = new Runnable() {

                @Override
                public void run() {
                    if (!running) {
                        return;
                    }

                    if (!wifiManager.startScan()) {
                        Log.w(TAG, "Couldn't start Wi-fi scan!");
                    }

                    Log.d("WifiActivity", "Started Wifi Scan");

                    wifiScanHandler.postDelayed(this, WIFI_SCAN_DELAY_MILLIS);
                }

            };
            wifiScanHandler.post(wifiScanRunnable);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WAKE_LOCK}, 44);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        Log.d("WifiActivity", "onPause");

        unregisterReceiver(wifiScanBroadcastReceiver);

        wifiLock.release();
    }

    private final class WifiScanBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("WifiActivity", "WifiScanBroadcastReceiver onReceive");
            if (!running || !WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                return;
            }

            List<ScanResult> scanResults = wifiManager.getScanResults();

            for (ScanResult scan : scanResults) {
                writeToFile(scan.toString(), getBaseContext());
            }





            // Do something with your scanResults
        }

    }

    private void writeToFile(String data,Context context) {
        try {
            Log.d("WifiActivity", "WriteToFile");
            String separator = System.getProperty("line.separator");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_APPEND));
            outputStreamWriter.append(data);
            outputStreamWriter.append(separator);
            outputStreamWriter.flush();
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

}