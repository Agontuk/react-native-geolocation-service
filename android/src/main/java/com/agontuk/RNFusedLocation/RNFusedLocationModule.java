package com.agontuk.RNFusedLocation;

import android.content.pm.PackageManager;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class RNFusedLocationModule extends ReactContextBaseJavaModule {
    private static final String TAG = "RNFusedLocation";
    private ReactApplicationContext reactContext;

    public RNFusedLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNFusedLocation";
    }

    /**
     * Get the current position. This can return almost immediately if the location is cached or
     * request an update, which might take a while.
     *
     * @param options map containing optional arguments: timeout (millis), maximumAge (millis) and
     *        highAccuracy (boolean)
     */
    @ReactMethod
    public void getCurrentPosition(
        ReadableMap options,
        final Callback success,
        Callback error
    ) {
        Log.d(TAG, "Called");

        if (!hasLocationPermission()) {
            error.invoke(buildError(
                LocationError.PERMISSION_DENIED.getValue(),
                "Location permission not granted"
            ));

            return;
        }

        if (!isGooglePlayServicesAvailable()) {
            error.invoke(buildError(
                LocationError.PLAY_SERVICE_NOT_AVAILABLE.getValue(),
                "Google play service is not available"
            ));

            return;
        }

        success.invoke("Success");
    }

    /**
     * Check if location permissions are granted.
     */
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if google play service is available on device.
     */
    private boolean isGooglePlayServicesAvailable() {
        int result =  GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(reactContext);

        // TODO: Handle other possible success types.
        return result == ConnectionResult.SUCCESS || result == ConnectionResult.SERVICE_UPDATING;
    }

    /**
     * Build error response for error callback.
     */
    private WritableMap buildError(int code, String message) {
        WritableMap error = Arguments.createMap();
        error.putInt("code", code);

        if (message != null) {
            error.putString("message", message);
        }

        return error;
    }
}
