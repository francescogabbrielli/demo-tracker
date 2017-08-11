package com.dtracker;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ToggleButton;

public class MainActivity extends LocalActivity {

    private final static int REQUEST_PERMISSION_LOCATION = 1;

    private DistanceTracker app;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (DistanceTracker) getApplication();
        prefs = PreferenceManager.getDefaultSharedPreferences(app);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // resume a current tracking session
        if(prefs.getBoolean(DistanceTracker.PREF_TRACKING, false))
            toggleTracking(true);
    }

    @Override
    protected void onGpsResolved(boolean resolved) {
        super.onGpsResolved(resolved);
        if (resolved) {
            startTracking();
        } else {
            toggleTracking(false);
        }
    }

    private void toggleTracking(boolean track) {
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.buttonTracking);
        toggleButton.setChecked(track);
    }

    public void toggleTracking(View v) {

        ToggleButton toggleButton = (ToggleButton) v;
        boolean allowed = prefs.getBoolean(DistanceTracker.PREF_ALLOW_TRACKING, false);

        if (!toggleButton.isChecked())
            stopTracking();
        else if (verifyStartTrackingPermissions()) {
            if (!allowed)
                showStartTrackingDialog();
            else
                startTracking();
        }

    }

    private boolean verifyStartTrackingPermissions() {
        int check = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (check != PackageManager.PERMISSION_GRANTED) {
            prefs.edit().putBoolean(DistanceTracker.PREF_ALLOW_TRACKING, false).apply();
            toggleTracking(false);
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_LOCATION
            );
            return false;
        }
        return true;
    }

    private void showStartTrackingDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        prefs.edit()
                                .putBoolean(DistanceTracker.PREF_ALLOW_TRACKING, true)
                                .apply();
                        startTracking();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        toggleTracking(false);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_message)
                .setTitle(R.string.dialog_title)
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    if (!prefs.getBoolean(DistanceTracker.PREF_ALLOW_TRACKING, false))
                        showStartTrackingDialog();
                    else
                        startTracking();
                } else {
                    toggleTracking(false);
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startTracking() {
        if (!prefs.getBoolean(DistanceTracker.PREF_TRACKING, false)) {
            prefs.edit().putBoolean(DistanceTracker.PREF_TRACKING, true).apply();
            app.resetTracking();
        }
        app.startTracking();
    }

    private void stopTracking() {
        prefs.edit().putBoolean(DistanceTracker.PREF_TRACKING, false).apply();
        app.stopTracking();
    }

    public void showDistance(View v) {
        startActivity(new Intent(this, DistanceActivity.class));
    }

    public void showMaps(View v) {
        startActivity(new Intent(this, MapsActivity.class));
    }

}
