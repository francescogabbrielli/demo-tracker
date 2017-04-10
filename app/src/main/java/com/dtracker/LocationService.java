package com.dtracker;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<LocationSettingsResult> {

    private final static String TAG = LocationService.class.getSimpleName();

    /** Broadcast action to request for permissions */
    public final static String ACTION_REQUEST_PERMISSION = "action_request_permission";

    /** Broadcast action to request for permissions */
    public final static String ACTION_REQUEST_RESOLUTION = "action_request_resolution";

    /** Notify when location updates can be performed */
    public final static String ACTION_LOCATION_STATUS_CHANGE = "action_location_status_changed";

    /** Start this service */
    public final static String ACTION_START_SERVICE = "action_start_service";

    /** Start location updates*/
    public final static String ACTION_START_UPDATES = "action_start_updates";

    /** Stop location updates*/
    public final static String ACTION_STOP_UPDATES = "action_stop_updates";

    public final static String KEY_LOCATION_STATUS = "key_location_status";

    public final static String KEY_PARAM = "key_param";

    /** GoogleAPI client */
    private GoogleApiClient googleApiClient;

    /** Current location request */
    private LocationRequest request;

    private boolean updating, autoStart;

    @Override
    public void onCreate() {
        // Create an instance of GoogleAPIClient.
        Log.i(TAG, "Creating Location Service");
        if (googleApiClient == null)
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Request: " + intent);
        if (intent==null || ACTION_START_SERVICE.equals(intent.getAction())) {
            Log.i(TAG, "request connect");
            connect();
        } else if (ACTION_START_UPDATES.equals(intent.getAction())) {
            Log.i(TAG, "request start updates");
            startUpdates();
        } else if (ACTION_STOP_UPDATES.equals(intent.getAction())) {
            Log.i(TAG, "request stop updates");
            stopUpdates();
        } else {
            Log.w(TAG, "unknown action: "+intent.getAction());
            //connect();???
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void connect() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            if (!googleApiClient.isConnected() && !googleApiClient.isConnecting())
                googleApiClient.connect();
    }

    @Override
    public synchronized void onConnected(@Nullable Bundle bundle) {

        Log.i(TAG, "CONNECTED!");

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleApiClient);

            //create request
            request = new LocationRequest();
            request.setInterval(10000);
            request.setFastestInterval(5000);
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            sendChange(true);

            if(autoStart)
                startUpdates();

        } else {

            sendAction(ACTION_REQUEST_PERMISSION);

        }

    }

    @Override
    public synchronized void onConnectionSuspended(int cause) {

        Log.i(TAG, "DISCONNECTED!");

        switch(cause) {
            case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST:
                //XXX: reconnect? or wait for network to activated?
                break;
            case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
                googleApiClient.connect();
                break;
        }

        request = null;
        sendChange(false);

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.w(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    private void sendChange(boolean status) {
        Intent intent = new Intent(ACTION_LOCATION_STATUS_CHANGE);
        intent.putExtra(KEY_LOCATION_STATUS, status);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void sendAction(String action, Parcelable... params) {
        Intent intent = new Intent(action);
        int n = 0;
        if (params.length>0)
            for (Parcelable param : params)
                intent.putExtra(KEY_PARAM+"_"+(++n), param);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private synchronized void startUpdates() {

        if (updating)
            return;

        if (request==null) {
            autoStart = true;
            connect();
            return;
        }

        autoStart = false;
        updating = true;

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient,
                        builder.build());

        result.setResultCallback(this);

    }

    private synchronized void stopUpdates() {
        updating = false;
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        final LocationSettingsStates states = result.getLocationSettingsStates();
        Log.i(TAG, status.getStatusCode()
                +"="+status.getStatusMessage()
                +": "+states.isGpsPresent()+"/"+states.isGpsUsable());
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                requestLocation();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                sendAction(ACTION_REQUEST_RESOLUTION, status);
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Toast.makeText(this, "Please change your settings to enable location management in this app", Toast.LENGTH_LONG).show();
                stopSelf();
                break;
        }

    }

    private void requestLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.i(TAG, "request location updates");
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, request, (DistanceTracker) getApplication());

        } else {
            sendAction(ACTION_REQUEST_PERMISSION);
        }

    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroying Location Service");
        request = null;
        sendChange(false);
        googleApiClient.disconnect();
        super.onDestroy();
    }

}
