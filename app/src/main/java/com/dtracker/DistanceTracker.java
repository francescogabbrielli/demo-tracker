package com.dtracker;

import android.app.Application;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;

import com.google.android.gms.location.LocationListener;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Francesco Gabbrielli on 7/03/2017
 * Edited on 10/04/2017
 */
public class DistanceTracker extends Application implements LocationListener {

    public static final String PREF_PATH_DISTANCE = "pref_distance";
    public static final String PREF_PATH_ID = "pref_id";

    public static final String PREF_LOCATION_TIME = "pref_time";
    public static final String PREF_LOCATION_LAT = "pref_lat";
    public static final String PREF_LOCATION_LNG = "pref_lng";
    public static final String PREF_LOCATION_AUTH_LAT = "pref_lat";
    public static final String PREF_LOCATION_AUTH_LNG = "pref_lng";

    public final static String PREF_TRACKING = "pref_tracking";

    /** Sequential executor */
    private Executor exec;

    //------------------------------ Temporary flags ------------------------------
    //
    private boolean flagAddingPath;
    //
    //------------------------ TODO: evolve with ThreadLocal? ---------------------


    @Override
    public void onCreate() {
        super.onCreate();
        exec = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onLocationChanged(Location location) {

        if (location==null || flagAddingPath)
            return;

        // retrieve configuration
        TypedValue typedValue = new TypedValue();
        getResources().getValue(R.dimen.max_speed, typedValue, true);
        float maxSpeed = typedValue.getFloat();
        getResources().getValue(R.dimen.min_speed, typedValue, true);
        float minSpeed = typedValue.getFloat();
        getResources().getValue(R.dimen.min_distance, typedValue, true);
        float minDistance = typedValue.getFloat();

        // new values
        double newlat = location.getLatitude();
        double newlng = location.getLongitude();
        float newtime = SystemClock.elapsedRealtime()*0.001f;//or location.getTime()

        // stored values
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        long id = pref.getLong(PREF_PATH_ID, 0);
        float lat = pref.getFloat(PREF_LOCATION_LAT, 0);
        float lng = pref.getFloat(PREF_LOCATION_LNG, 0);

        // if there is at least one location available
        if (lat!=0 && lng!=0) {

            float time = pref.getLong(PREF_LOCATION_TIME, 0);

            float d = computeDistanceAndBearing(lat, lng, newlat, newlng, null);
            float speed = d / (newtime - time);
            Log.d("dTracker", "Distance: "+d+"m; Speed: "+speed+"m/s");
            if (speed>maxSpeed)
                return;

            float authlat = pref.getFloat(PREF_LOCATION_AUTH_LAT, 0);
            float authlng = pref.getFloat(PREF_LOCATION_AUTH_LNG, 0);
            float authd = computeDistanceAndBearing(authlat, authlng, lat, lng, null);
            boolean authOk = (authlat!=0 && authlng!=0) && authd>minDistance;

            // update distance
            if (authOk || speed>minSpeed) {
                float distance = pref.getFloat(PREF_PATH_DISTANCE, 0) + (authOk ? authd : d);
                addPoint(id, location, distance);
                pref.edit().putFloat(PREF_PATH_DISTANCE, distance).commit();
            }
        }

        if (id==0)
            addPath(location);

        pref.edit().putFloat(PREF_LOCATION_LAT, lat).putFloat(PREF_LOCATION_LNG, lng).commit();
        Log.d("dTracker", location.toString());
    }

    private void addPath(final Location firstPoint) {
        flagAddingPath = true;
        exec.execute(new Runnable() {
            @Override
            public void run() {
                PathContract.DbHelper helper = PathContract.createHelper(DistanceTracker.this);
                long id = helper.addPath();
                helper.addPoint(id, firstPoint.getLatitude(), firstPoint.getLongitude(), 0);
                PreferenceManager.getDefaultSharedPreferences(DistanceTracker.this).edit()
                        .putLong(PREF_PATH_ID, id)
                        .commit();
                flagAddingPath = false;
            }
        });
    }

    private void addPoint(final long id, final Location location, final float distance) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                PathContract.createHelper(DistanceTracker.this)
                        .addPoint(id, location.getLatitude(), location.getLongitude(), distance);
                PreferenceManager.getDefaultSharedPreferences(DistanceTracker.this).edit()
                        .putFloat(PREF_LOCATION_AUTH_LAT, (float) location.getLatitude())
                        .putFloat(PREF_LOCATION_AUTH_LNG, (float) location.getLongitude())
                        .commit();
            }
        });
    }

    public void reset() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .remove(PREF_PATH_ID)
                .remove(PREF_PATH_DISTANCE)
                .remove(PREF_LOCATION_LAT)
                .remove(PREF_LOCATION_LNG)
                .remove(PREF_LOCATION_AUTH_LAT)
                .remove(PREF_LOCATION_AUTH_LNG)
                .commit();
    }

    //...not used for simplicity
    private static class BearingDistanceCache {
        private double mLat1 = 0.0;
        private double mLon1 = 0.0;
        private double mLat2 = 0.0;
        private double mLon2 = 0.0;
        private float mDistance = 0.0f;
        private float mInitialBearing = 0.0f;
        private float mFinalBearing = 0.0f;
    }

    private static float computeDistanceAndBearing(double lat1, double lon1,
                                                  double lat2, double lon2, BearingDistanceCache results) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
        // using the "Inverse Formula" (section 4)

        int MAXITERS = 20;
        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double L = lon2 - lon1;
        double A = 0.0;
        double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha = 0.0;
        double cos2SM = 0.0;
        double cosSigma = 0.0;
        double sinSigma = 0.0;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = L; // initial guess
        for (int iter = 0; iter < MAXITERS; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2; // (14)
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
            sigma = Math.atan2(sinSigma, cosSigma); // (16)
            double sinAlpha = (sinSigma == 0) ? 0.0 :
                    cosU1cosU2 * sinLambda / sinSigma; // (17)
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 :
                    cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
            A = 1 + (uSquared / 16384.0) * // (3)
                    (4096.0 + uSquared *
                            (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double B = (uSquared / 1024.0) * // (4)
                    (256.0 + uSquared *
                            (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double C = (f / 16.0) *
                    cosSqAlpha *
                    (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * // (6)
                    (cos2SM + (B / 4.0) *
                            (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                                    (B / 6.0) * cos2SM *
                                            (-3.0 + 4.0 * sinSigma * sinSigma) *
                                            (-3.0 + 4.0 * cos2SMSq)));

            lambda = L +
                    (1.0 - C) * f * sinAlpha *
                            (sigma + C * sinSigma *
                                    (cos2SM + C * cosSigma *
                                            (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }

        float distance = (float) (b * A * (sigma - deltaSigma));

        if (results!=null) {
            results.mDistance = distance;
            float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
                    cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
            initialBearing *= 180.0 / Math.PI;
            results.mInitialBearing = initialBearing;
            float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
                    -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
            finalBearing *= 180.0 / Math.PI;
            results.mFinalBearing = finalBearing;
            results.mLat1 = lat1;
            results.mLat2 = lat2;
            results.mLon1 = lon1;
            results.mLon2 = lon2;
        }

        return distance;
    }

}