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
  private final LocationProvider locationProvider;

  public RNFusedLocationModule(ReactApplicationContext reactContext) {
    super(reactContext);

    reactContext.addActivityEventListener(this);
    this.locationProvider = createLocationProvider();

    Log.i(TAG, TAG + " initialized");
  }

  @NonNull
  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    locationProvider.onActivityResult(requestCode, resultCode);
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

    locationProvider.getCurrentLocation(locationOptions, new LocationChangeListener() {
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

    locationProvider.requestLocationUpdates(locationOptions, new LocationChangeListener() {
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
    locationProvider.removeLocationUpdates();
  }

  private LocationProvider createLocationProvider() {
    ReactApplicationContext context = getContext();

    if (LocationUtils.isGooglePlayServicesAvailable(context)) {
      return new FusedLocationProvider(getContext());
    }

    return new LocationManagerProvider();
  }

  private void emitEvent(String eventName, WritableMap data) {
    getContext().getJSModule(RCTDeviceEventEmitter.class).emit(eventName, data);
  }

  private ReactApplicationContext getContext() {
    return getReactApplicationContext();
  }
}
