package com.agontuk.RNFusedLocation;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;

public class RNFusedLocationModule extends ReactContextBaseJavaModule implements ActivityEventListener {
  public static final String TAG = "RNFusedLocation";
  private final LocationManager locationManager;

  public RNFusedLocationModule(ReactApplicationContext reactContext) {
    super(reactContext);

    reactContext.addActivityEventListener(this);
    this.locationManager = createLocationManager();

    Log.i(TAG, TAG + " initialized");
  }

  @NonNull
  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    locationManager.onActivityResult(requestCode, resultCode);
  }

  @Override
  public void onNewIntent(Intent intent) {
    //
  }

  @ReactMethod
  public void getCurrentPosition(ReadableMap options, final Callback success, final Callback error) {
    ReactApplicationContext context = getContext();

    if (!LocationUtils.hasLocationPermission(context)) {
      error.invoke(LocationUtils.buildError(LocationError.PERMISSION_DENIED, null));
      return;
    }

    LocationOptions locationOptions = LocationOptions.fromReadableMap(options);

    locationManager.getCurrentLocation(locationOptions, new LocationListener() {
      @Override
      public void onLocationChange(Location location) {
        success.invoke(LocationUtils.locationToMap(location));
      }

      @Override
      public void onLocationError(LocationError locationError, @Nullable String message) {
        error.invoke(LocationUtils.buildError(locationError, message));
      }
    });
  }

  @ReactMethod
  public void startObserving(ReadableMap options) {
    ReactApplicationContext context = getContext();

    if (!LocationUtils.hasLocationPermission(context)) {
      emitEvent(
        "geolocationError",
        LocationUtils.buildError(LocationError.PERMISSION_DENIED, null)
      );
      return;
    }

    LocationOptions locationOptions = LocationOptions.fromReadableMap(options);

    locationManager.requestLocationUpdates(locationOptions, new LocationListener() {
      @Override
      public void onLocationChange(Location location) {
        emitEvent("geolocationDidChange", LocationUtils.locationToMap(location));
      }

      @Override
      public void onLocationError(LocationError error, @Nullable String message) {
        emitEvent("geolocationError", LocationUtils.buildError(error, message));
      }
    });
  }

  @ReactMethod
  public void stopObserving() {
    locationManager.removeLocationUpdates();
  }

  private LocationManager createLocationManager() {
    ReactApplicationContext context = getContext();

    if (LocationUtils.isGooglePlayServicesAvailable(context)) {
      return new FusedLocationManager(getContext());
    }

    return new AndroidLocationManager();
  }

  private void emitEvent(String eventName, WritableMap data) {
    getContext().getJSModule(RCTDeviceEventEmitter.class).emit(eventName, data);
  }

  private ReactApplicationContext getContext() {
    return getReactApplicationContext();
  }
}
