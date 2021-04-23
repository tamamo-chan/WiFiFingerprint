package com.example.wififingerprint;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity  {

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

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final long WIFI_SCAN_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private WifiManager wifiManager;
    private WifiScanBroadcastReceiver wifiScanBroadcastReceiver = new WifiScanBroadcastReceiver();
    private WifiManager.WifiLock wifiLock;

    private volatile boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //this.getPermission();

        Log.d("WifiActivity", "onCreate");

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, this.getLocalClassName());
        /*EditText myEditText = findViewById(R.id.roomName);
        myEditText.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                EditText myEditText = findViewById(R.id.roomName);
                if (hasFocus) {
                    myEditText.setHint("");
                } else {
                    myEditText.setHint("Enter room");
                }
            }
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        Log.d("WifiActivity", "onResume");


    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        Log.d("WifiActivity", "onPause");

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
        }

    }

    private void writeToFile(String data,Context context) {
        try {
            Log.d("WifiActivity", "WriteToFile");
            String separator = System.getProperty("line.separator");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("wifi.txt", Context.MODE_APPEND));
            outputStreamWriter.append(data);
            outputStreamWriter.append(separator);
            outputStreamWriter.flush();
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private int cooldown_time = 3000;

    public void handleButton(View view) {
        EditText roomName = findViewById(R.id.roomName);
        if (TextUtils.isEmpty(roomName.getText()) ) {
            roomName.setError("Room name required.");
        } else {
            roomName.setEnabled(false);
            roomName.postDelayed(new Runnable() {
                EditText roomName = findViewById(R.id.roomName);
                @Override
                public void run() {
                    roomName.setEnabled(true);
                }
            }, cooldown_time);

            writeToFile(roomName.getText().toString(), this);
            Button myButton = findViewById(R.id.scanWifi);
            myButton.setEnabled(false);
            myButton.postDelayed(new Runnable() {
                Button myButton = findViewById(R.id.scanWifi);
                @Override
                public void run() {
                    myButton.setEnabled(true);
                }
            }, cooldown_time);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED) {
                wifiLock.acquire();

                registerReceiver(wifiScanBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 12);
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 24);
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WAKE_LOCK}, 44);
            }

            new CountDownTimer(cooldown_time, 1000) {
                TextView text = findViewById(R.id.countdown);

                public void onTick(long millisUntilFinished) {
                    text.setText("seconds remaining: " + millisUntilFinished / 1000);
                    //here you can have your logic to set text to edittext

                    if (!running) {
                        return;
                    }

                    if (!wifiManager.startScan()) {
                        Log.w(TAG, "Couldn't start Wi-fi scan!");
                    }

                }

                public void onFinish() {
                    text.setText("done!");
                    unregisterReceiver(wifiScanBroadcastReceiver);

                    wifiLock.release();
                }

            }.start();
        }

    }
}