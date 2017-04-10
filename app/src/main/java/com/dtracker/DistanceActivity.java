package com.dtracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DistanceActivity extends AppCompatActivity implements Runnable {

    private ScheduledFuture scheduled;
    private ScheduledExecutorService scheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance);
        scheduler = Executors.newSingleThreadScheduledExecutor();
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
                TypedValue typedValue = new TypedValue();
                getResources().getValue(R.dimen.measure_factor, typedValue, true);
                float factor = typedValue.getFloat();
                SharedPreferences pref =
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                float distance = pref.getFloat(DistanceTracker.PREF_PATH_DISTANCE, 0);
                TextView view = (TextView) findViewById(R.id.distanceTextView);
                view.setText((distance*factor) + getString(R.string.measure_unit));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scheduler.shutdown();
    }
}
