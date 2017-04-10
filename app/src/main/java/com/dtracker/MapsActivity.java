package com.dtracker;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Runnable {

    private GoogleMap map;

    ScheduledFuture scheduled;

    ScheduledExecutorService scheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted to access current location", Toast.LENGTH_SHORT).show();
            return;
        }
        map.setMyLocationEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduled = scheduler.scheduleAtFixedRate(this, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scheduled.cancel(false);
    }

    public void run() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences pref =
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                float lat = pref.getFloat(DistanceTracker.PREF_LOCATION_LAT, 0);
                float lng = pref.getFloat(DistanceTracker.PREF_LOCATION_LNG, 0);
                if (lat!=0 && lng!=0 && map != null)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scheduler.shutdown();
    }

}
