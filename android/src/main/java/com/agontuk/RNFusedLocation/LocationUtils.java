package com.agontuk.RNFusedLocation;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class LocationUtils {
    /**
     * Check if location permissions are granted.
     */
    public static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if google play service is available on device.
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        int result =  GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);

        // TODO: Handle other possible success types.
        return result == ConnectionResult.SUCCESS || result == ConnectionResult.SERVICE_UPDATING;
    }

    /**
     * Build error response for error callback.
     */
    public static WritableMap buildError(int code, String message) {
        WritableMap error = Arguments.createMap();
        error.putInt("code", code);

        if (message != null) {
            error.putString("message", message);
        }

        return error;
    }

    public static WritableMap locationToMap(Location location) {
        WritableMap map = Arguments.createMap();
        WritableMap coords = Arguments.createMap();

        coords.putDouble("latitude", location.getLatitude());
        coords.putDouble("longitude", location.getLongitude());
        coords.putDouble("altitude", location.getAltitude());
        coords.putDouble("accuracy", location.getAccuracy());
        coords.putDouble("heading", location.getBearing());
        coords.putDouble("speed", location.getSpeed());
        map.putMap("coords", coords);
        map.putDouble("timestamp", location.getTime());

        if (Build.VERSION.SDK_INT >= 18) {
            map.putBoolean("mocked", location.isFromMockProvider());
        }

        return map;
    }
}
