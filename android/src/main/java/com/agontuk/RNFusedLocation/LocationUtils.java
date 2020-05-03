package com.agontuk.RNFusedLocation;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;
import android.os.Build;
import android.text.TextUtils;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;

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
   * Check if location is enabled on the device.
   */
  public static boolean isLocationEnabled(Context context) {
    int locationMode = 0;
    String locationProviders;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      try {
        locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
      } catch (Settings.SettingNotFoundException e) {
        return false;
      }

      return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    } else {
      locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
      return !TextUtils.isEmpty(locationProviders);
    }
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      map.putBoolean("mocked", location.isFromMockProvider());
    }

    return map;
  }
}
