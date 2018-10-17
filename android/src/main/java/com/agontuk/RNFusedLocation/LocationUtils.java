package com.agontuk.RNFusedLocation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class LocationUtils {

    public static int LOCATION_MODE_UNDEFINED = -1;

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
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);

        // TODO: Handle other possible success types.
        return result == ConnectionResult.SUCCESS || result == ConnectionResult.SERVICE_UPDATING;
    }


    /**
     * Get location mode name.
     *
     * @param locationMode
     * @return
     */
    private static String getLocationModeName(int locationMode) {
        switch (locationMode) {
            case Settings.Secure.LOCATION_MODE_OFF:
                return "LOCATION_MODE_OFF";
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                return "LOCATION_MODE_SENSORS_ONLY";
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                return "LOCATION_MODE_BATTERY_SAVING";
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                return "LOCATION_MODE_HIGH_ACCURACY";
            default:
                return "LOCATION_MODE_UNDEFINED";
        }
    }


    /**
     * Build information LocationProvider information.
     *
     * @param provider
     * @return
     */
    private static WritableMap buildProviderLocation(LocationProvider provider, boolean enabled) {
        WritableMap map = Arguments.createMap();
        map.putString("name", provider.getName());
        map.putBoolean("enabled", enabled);
        String accuracy = provider.getAccuracy() == Criteria.ACCURACY_FINE ? "ACCURACY_FINE" : "ACCURACY_COARSE";
        map.putString("accuracy", accuracy);
        map.putBoolean("requiresCell", provider.requiresCell());
        map.putBoolean("requiresNetwork", provider.requiresCell());
        map.putBoolean("requiresSatellite", provider.requiresCell());
        return map;
    }


    /**
     * Build success response for success callback.
     */
    public static WritableMap buildLocationSettingsSuccess(int locationMode, boolean isLocationEnabled,
                                                           LocationProvider gpsProvider, boolean isGpsEnabled,
                                                           LocationProvider networkProvider, boolean isNetworkEnabled) {
        WritableMap map = Arguments.createMap();


        map.putString("locationMode", getLocationModeName(locationMode));
        map.putBoolean("locationEnabled", isLocationEnabled);

        if (gpsProvider != null) {
            WritableMap gps = buildProviderLocation(gpsProvider, isGpsEnabled);
            map.putMap("gpsProvider", gps);
        }

        if (networkProvider != null) {
            WritableMap network = buildProviderLocation(networkProvider, isNetworkEnabled);
            map.putMap("networkProvider", network);
        }

        return map;
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

    /**
     * Build success response for success callback.
     */
    public static WritableMap buildSuccess(Location location) {
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
