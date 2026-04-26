package com.example.shelter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private String emergencyNumber;
    private int tapCount = 0;
    private long lastTapTime = 0;
    private String alertText = "I NEED HELP, DON'T CALL ME BACK!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // PERSISTENCE: Load the saved number. Defaults to yours if empty.
        SharedPreferences prefs = getSharedPreferences("SHElterPrefs", MODE_PRIVATE);
        emergencyNumber = prefs.getString("sos_number", "9400630923");

        // SETUP: Long press the Title to change the number (Hidden Setting)
        findViewById(R.id.quoteTitle).setOnLongClickListener(v -> {
            showNumberInputDialog();
            return true;
        });

        // TRIGGER: Triple tap to start SOS
        findViewById(R.id.secretTrigger).setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTapTime < 1500) {
                tapCount++;
            } else {
                tapCount = 1;
            }
            lastTapTime = currentTime;

            if (tapCount >= 3) {
                startEmergencySystem();
                tapCount = 0;
            } else {
                Toast.makeText(this, "Quote Updated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNumberInputDialog() {
        EditText input = new EditText(this);
        input.setHint("Ex: 9400630923");
        input.setText(emergencyNumber);
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("Set Emergency Contact")
                .setMessage("The SOS calls and SMS will be sent to this number.")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String num = input.getText().toString();
                    if (!num.isEmpty()) {
                        emergencyNumber = num;
                        getSharedPreferences("SHElterPrefs", MODE_PRIVATE).edit()
                                .putString("sos_number", num).apply();
                        Toast.makeText(this, "Contact Saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startEmergencySystem() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setStreamMute(AudioManager.STREAM_RING, true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {

            SmsManager.getDefault().sendTextMessage(emergencyNumber, null, alertText + " Tracking started.", null, null);

            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location loc) {
                    String mapLink = "http://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
                    String msg = alertText + " LIVE LOCATION: " + mapLink;
                    try {
                        SmsManager.getDefault().sendTextMessage(emergencyNumber, null, msg, null, null);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            };

            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1, listener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 1, listener);
            } catch (Exception e) { e.printStackTrace(); }

            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + emergencyNumber)));
            new Handler().postDelayed(this::applyFakePowerOff, 1500);

        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void applyFakePowerOff() {
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.BLACK);
        TextView tv = new TextView(this);
        tv.setText(alertText + "\n\n(Tracking Active for " + emergencyNumber + ")");
        tv.setTextColor(Color.DKGRAY);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(16);
        layout.addView(tv);
        setContentView(layout);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0.01f;
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}