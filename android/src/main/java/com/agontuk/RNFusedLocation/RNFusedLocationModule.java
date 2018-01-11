package com.agontuk.RNFusedLocation;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

public class RNFusedLocationModule extends ReactContextBaseJavaModule {
    private static final String TAG = "RNFusedLocation";

    public RNFusedLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
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
        success.invoke("Success");
    }
}
