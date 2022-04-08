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

import java.util.HashMap;
import java.util.Set;

public class RNFusedLocationModule extends ReactContextBaseJavaModule implements ActivityEventListener, LocationChangeListener {
  public static final String TAG = "RNFusedLocation";
  private final HashMap<LocationProvider, PendingLocationRequest> pendingRequests;
  @Nullable private LocationProvider continuousLocationProvider;

  public RNFusedLocationModule(ReactApplicationContext reactContext) {
    super(reactContext);

    reactContext.addActivityEventListener(this);
    this.pendingRequests = new HashMap<>();

    Log.i(TAG, TAG + " initialized");
  }

  @NonNull
  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    if (continuousLocationProvider != null &&
      continuousLocationProvider.onActivityResult(requestCode, resultCode)
    ) {
      return;
    }

    Set<LocationProvider> providers = pendingRequests.keySet();

    for (LocationProvider locationProvider: providers) {
      if (locationProvider.onActivityResult(requestCode, resultCode)) {
        return;
      }
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    //
  }

  @Override
  public void onLocationChange(LocationProvider locationProvider, Location location) {
    WritableMap locationData = LocationUtils.locationToMap(location);

    if (locationProvider.equals(continuousLocationProvider)) {
      emitEvent("geolocationDidChange", locationData);
      return;
    }

    PendingLocationRequest request =  pendingRequests.get(locationProvider);

    if (request != null) {
      request.successCallback.invoke(locationData);
      pendingRequests.remove(locationProvider);
    }
  }

  @Override
  public void onLocationError(LocationProvider locationProvider, LocationError error, @Nullable String message) {
    WritableMap errorData = LocationUtils.buildError(error, message);

    if (locationProvider.equals(continuousLocationProvider)) {
      emitEvent("geolocationError", errorData);
      return;
    }

    PendingLocationRequest request =  pendingRequests.get(locationProvider);

    if (request != null) {
      request.errorCallback.invoke(errorData);
      pendingRequests.remove(locationProvider);
    }
  }

  @ReactMethod
  public void getCurrentPosition(ReadableMap options, final Callback success, final Callback error) {
    ReactApplicationContext context = getContext();

    if (!LocationUtils.hasLocationPermission(context)) {
      error.invoke(LocationUtils.buildError(LocationError.PERMISSION_DENIED, null));
      return;
    }

    LocationOptions locationOptions = LocationOptions.fromReadableMap(options);
    final LocationProvider locationProvider = createLocationProvider(locationOptions.isForceLocationManager());

    pendingRequests.put(locationProvider, new PendingLocationRequest(success, error));
    locationProvider.getCurrentLocation(locationOptions);
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

    if (continuousLocationProvider == null) {
      continuousLocationProvider = createLocationProvider(locationOptions.isForceLocationManager());
    }

    continuousLocationProvider.requestLocationUpdates(locationOptions);
  }

  @ReactMethod
  public void stopObserving() {
    if (continuousLocationProvider != null) {
      continuousLocationProvider.removeLocationUpdates();
      continuousLocationProvider = null;
    }
  }

  @ReactMethod
  public void addListener(String eventName) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  @ReactMethod
  public void removeListeners(Integer count) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  private LocationProvider createLocationProvider(boolean forceLocationManager) {
    ReactApplicationContext context = getContext();
    boolean playServicesAvailable = LocationUtils.isGooglePlayServicesAvailable(context);

    if (forceLocationManager || !playServicesAvailable) {
      return new LocationManagerProvider(context, this);
    }

    return new FusedLocationProvider(context, this);
  }

  private void emitEvent(String eventName, WritableMap data) {
    getContext().getJSModule(RCTDeviceEventEmitter.class).emit(eventName, data);
  }

  private ReactApplicationContext getContext() {
    return getReactApplicationContext();
  }

  private static class PendingLocationRequest {
    final Callback successCallback;
    final Callback errorCallback;

    public PendingLocationRequest(Callback success, Callback error) {
      this.successCallback = success;
      this.errorCallback = error;
    }
  }
}
