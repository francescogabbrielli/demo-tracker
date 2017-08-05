package com.dtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public abstract class LocalActivity extends AppCompatActivity {

    public final static int REQUEST_RESOLUTION_GPS = 1;

    public final static int REQUEST_PERMISSION_LOCATION = 1;


    /** Receiver of location service messages */
    protected BroadcastReceiver receiver;

    protected IntentFilter filter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // receive messages from the background service
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(DistanceTracker.TAG, intent.getAction() + " " + intent.getExtras().toString());
                if (LocationService.ACTION_REQUEST_RESOLUTION.equals(intent.getAction())) {
                    Status status = intent.getParcelableExtra(LocationService.KEY_PARAM+"_1");
                    if (status.getStatusCode()== LocationSettingsStatusCodes.RESOLUTION_REQUIRED)
                        try {
                            status.startResolutionForResult(LocalActivity.this, REQUEST_RESOLUTION_GPS);
                        } catch(Exception e) {
                            onGpsResolved(false);
                            Toast.makeText(LocalActivity.this, R.string.cannot_activate_gps, Toast.LENGTH_LONG).show();
                            Log.e(DistanceTracker.TAG, getString(R.string.cannot_activate_gps), e);
                        }
                }
            }
        };

        filter = new IntentFilter();
        filter.addAction(LocationService.ACTION_LOCATION_STATUS_CHANGE);
//        filter.addAction(LocationService.ACTION_REQUEST_PERMISSION);
        filter.addAction(LocationService.ACTION_REQUEST_RESOLUTION);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_RESOLUTION_GPS:
                if (resultCode==RESULT_OK) {
                    onGpsResolved(true);
                } else {
                    onGpsResolved(false);
                    Toast.makeText(LocalActivity.this, R.string.cancel_activate_gps, Toast.LENGTH_LONG).show();
                    Log.w(DistanceTracker.TAG, getString(R.string.cancel_activate_gps));
                }
                break;
        }
    }

    /**
     * Implement to reflect the change in the current activity
     *
     * @param enabled
     *              GPS enabled status
     */
    protected void onGpsResolved(boolean enabled) {
        Log.d("GPS", enabled?"Enabled":"Disabled");
//        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
//                .edit().putBoolean(DistanceTracker.PREF_ALLOW_TRACKING, enabled).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
    }
}
