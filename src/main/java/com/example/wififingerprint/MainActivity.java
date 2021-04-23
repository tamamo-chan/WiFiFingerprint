package com.example.wififingerprint;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void handleButton(View view) {
        Button myButton = findViewById(R.id.scanWifi);
        myButton.setEnabled(false);
        myButton.postDelayed(new Runnable() {
            Button myButton = findViewById(R.id.scanWifi);
            @Override
            public void run() {
                myButton.setEnabled(true);
            }
        }, 30000);

        new CountDownTimer(30000, 1000) {
            TextView text = findViewById(R.id.countdown);

            public void onTick(long millisUntilFinished) {
                text.setText("seconds remaining: " + millisUntilFinished / 1000);
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                text.setText("done!");
            }

        }.start();

        Intent intent = new Intent(this, WifiActivity.class);
        this.startActivity(intent);

    }


}