package com.dtracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private DistanceTracker app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // update UI from a previous launch
        if(PreferenceManager.getDefaultSharedPreferences(
                getApplication()).getBoolean(DistanceTracker.PREF_TRACKING,false)) {

            ToggleButton bTracking=(ToggleButton)findViewById(R.id.buttonTracking);
            bTracking.setChecked(true);

        }

        app = (DistanceTracker) getApplication();

    }

    public void toggleTracking(View v) {
        final ToggleButton toggleButton = (ToggleButton) v;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        boolean allowed = prefs.getBoolean(DistanceTracker.PREF_TRACKING, false);

        if (!toggleButton.isChecked()) {
            stopTracking();
            return;
        }

        if (allowed) {

            startTracking();

        } else {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            prefs.edit().putBoolean(DistanceTracker.PREF_TRACKING, true).apply();
                            startTracking();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            toggleButton.setChecked(false);
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

    }

    private void startTracking() {
        app.resetTracking();
        app.startTracking();
    }

    private void stopTracking() {
        app.stopTracking();
    }

    public void showDistance(View v) {
        startActivity(new Intent(this, DistanceActivity.class));
    }

    public void showMaps(View v) {
        startActivity(new Intent(this, MapsActivity.class));
    }

}
