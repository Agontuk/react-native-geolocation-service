package com.agontuk.RNFusedLocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

public class RNFusedLocationModule extends ReactContextBaseJavaModule {

    public static final String TAG = "RNFusedLocation";
    private static final int REQUEST_SETTINGS_SINGLE_UPDATE = 11403;
    private static final int REQUEST_SETTINGS_CONTINUOUS_UPDATE = 11404;
    private static final float DEFAULT_DISTANCE_FILTER = 100;
    private static final int DEFAULT_ACCURACY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    private static final long DEFAULT_INTERVAL = 10 * 1000;  /* 10 secs */
    private static final long DEFAULT_FASTEST_INTERVAL = 5 * 1000; /* 5 sec */

    private boolean mShowLocationDialog = true;
    private int mLocationPriority = DEFAULT_ACCURACY;
    private long mUpdateInterval = DEFAULT_INTERVAL;
    private long mFastestInterval = DEFAULT_FASTEST_INTERVAL;
    private double mMaximumAge = Double.POSITIVE_INFINITY;
    private long mTimeout = Long.MAX_VALUE;
    private float mDistanceFilter = DEFAULT_DISTANCE_FILTER;

    private Callback mSuccessCallback;
    private Callback mErrorCallback;
    private FusedLocationProviderClient mFusedProviderClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            if (requestCode == REQUEST_SETTINGS_SINGLE_UPDATE) {
                if (resultCode == Activity.RESULT_CANCELED) {
                    // User cancelled the request.
                    // TODO: allow user to ignore this & request location.
                    invokeError(
                            LocationError.SETTINGS_NOT_SATISFIED.getValue(),
                            "Location settings are not satisfied.",
                            true
                    );
                } else if (resultCode == Activity.RESULT_OK) {
                    // Location settings changed successfully, request user location.
                    getUserLocation();
                }
            } else if (requestCode == REQUEST_SETTINGS_CONTINUOUS_UPDATE) {
                if (resultCode == Activity.RESULT_CANCELED) {
                    // User cancelled the request.
                    // TODO: allow user to ignore this & request location.
                    invokeError(
                            LocationError.SETTINGS_NOT_SATISFIED.getValue(),
                            "Location settings are not satisfied.",
                            false
                    );
                } else if (resultCode == Activity.RESULT_OK) {
                    // Location settings changed successfully, request user location.
                    getLocationUpdates();
                }
            }
        }
    };
    private Callback mSuccessSettingsCallback;
    private Callback mErrorSettingsCallback;

    public RNFusedLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mFusedProviderClient = LocationServices.getFusedLocationProviderClient(reactContext);
        mSettingsClient = LocationServices.getSettingsClient(reactContext);
        reactContext.addActivityEventListener(mActivityEventListener);

        Log.i(TAG, TAG + " initialized");
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(LocationError.PERMISSION_DENIED.name(), LocationError.PERMISSION_DENIED.getValue());
        constants.put(LocationError.POSITION_UNAVAILABLE.name(), LocationError.POSITION_UNAVAILABLE.getValue());
        constants.put(LocationError.TIMEOUT.name(), LocationError.TIMEOUT.getValue());
        constants.put(LocationError.PLAY_SERVICE_NOT_AVAILABLE.name(), LocationError.PLAY_SERVICE_NOT_AVAILABLE.getValue());
        constants.put(LocationError.SETTINGS_NOT_SATISFIED.name(), LocationError.SETTINGS_NOT_SATISFIED.getValue());
        constants.put(LocationError.INTERNAL_ERROR.name(), LocationError.INTERNAL_ERROR.getValue());
        return constants;
    }

    /**
     * Get the current location settings. This return the current state of locations providers in
     * the device.
     *
     * @param success success callback
     * @param error   error callback
     */
    @ReactMethod
    public void getCurrentLocationSettings(final Callback success, final Callback error) {
        ReactApplicationContext context = getContext();

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION)) {

            int locationMode = LocationUtils.LOCATION_MODE_UNDEFINED;

            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }


            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            LocationProvider gpsProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            LocationProvider networkProvider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            boolean locationEnabled = gpsEnabled || networkEnabled;

            mSuccessSettingsCallback = success;
            mErrorSettingsCallback = error;

            if (!LocationUtils.hasLocationPermission(context)) {
                invokeSettingsError(
                        LocationError.PERMISSION_DENIED.getValue(),
                        "Location permission not granted."
                );
                return;
            }

            if (!LocationUtils.isGooglePlayServicesAvailable(context)) {
                invokeSettingsError(
                        LocationError.PLAY_SERVICE_NOT_AVAILABLE.getValue(),
                        "Google play service is not available."
                );
                return;
            }

            invokeSettingsSuccess(locationMode, locationEnabled, gpsProvider, gpsEnabled, networkProvider, networkEnabled);
        }

        invokeSettingsError(
                LocationError.INTERNAL_ERROR.getValue(),
                "Internal error occurred."
        );
        return;

    }

    /**
     * Get the current position. This can return almost immediately if the location is cached or
     * request an update, which might take a while.
     *
     * @param options map containing optional arguments: timeout (millis), maximumAge (millis),
     *                highAccuracy (boolean), distanceFilter (double) and showLocationDialog (boolean)
     * @param success success callback
     * @param error   error callback
     */
    @ReactMethod
    public void getCurrentPosition(ReadableMap options, final Callback success, final Callback error) {
        ReactApplicationContext context = getContext();

        mSuccessCallback = success;
        mErrorCallback = error;

        if (!LocationUtils.hasLocationPermission(context)) {
            invokeError(
                    LocationError.PERMISSION_DENIED.getValue(),
                    "Location permission not granted.",
                    true
            );
            return;
        }

        if (!LocationUtils.isGooglePlayServicesAvailable(context)) {
            invokeError(
                    LocationError.PLAY_SERVICE_NOT_AVAILABLE.getValue(),
                    "Google play service is not available.",
                    true
            );
            return;
        }

        boolean highAccuracy = options.hasKey("enableHighAccuracy") &&
                options.getBoolean("enableHighAccuracy");

        // TODO: Make other PRIORITY_* constants availabe to the user
        mLocationPriority = highAccuracy ? LocationRequest.PRIORITY_HIGH_ACCURACY : DEFAULT_ACCURACY;

        mTimeout = options.hasKey("timeout") ? (long) options.getDouble("timeout") : Long.MAX_VALUE;
        mMaximumAge = options.hasKey("maximumAge")
                ? options.getDouble("maximumAge")
                : Double.POSITIVE_INFINITY;
        mDistanceFilter = options.hasKey("distanceFilter")
                ? (float) options.getDouble("distanceFilter")
                : 0;
        mShowLocationDialog = options.hasKey("showLocationDialog")
                ? options.getBoolean("showLocationDialog")
                : true;

        LocationSettingsRequest locationSettingsRequest = buildLocationSettingsRequest();

        if (mSettingsClient != null) {
            mSettingsClient.checkLocationSettings(locationSettingsRequest)
                    .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                        @Override
                        public void onComplete(Task<LocationSettingsResponse> task) {
                            onLocationSettingsResponse(task, true);
                        }
                    });
        }
    }

    /**
     * Start listening for location updates. These will be emitted via the
     * {@link RCTDeviceEventEmitter} as {@code geolocationDidChange} events.
     *
     * @param options map containing optional arguments: highAccuracy (boolean), distanceFilter (double),
     *                interval (millis), fastestInterval (millis)
     */
    @ReactMethod
    public void startObserving(ReadableMap options) {
        ReactApplicationContext context = getContext();

        if (!LocationUtils.hasLocationPermission(context)) {
            invokeError(
                    LocationError.PERMISSION_DENIED.getValue(),
                    "Location permission not granted.",
                    false
            );
            return;
        }

        if (!LocationUtils.isGooglePlayServicesAvailable(context)) {
            invokeError(
                    LocationError.PLAY_SERVICE_NOT_AVAILABLE.getValue(),
                    "Google play service is not available.",
                    false
            );
            return;
        }

        boolean highAccuracy = options.hasKey("enableHighAccuracy")
                && options.getBoolean("enableHighAccuracy");

        // TODO: Make other PRIORITY_* constants availabe to the user
        mLocationPriority = highAccuracy ? LocationRequest.PRIORITY_HIGH_ACCURACY : DEFAULT_ACCURACY;
        mDistanceFilter = options.hasKey("distanceFilter")
                ? (float) options.getDouble("distanceFilter")
                : DEFAULT_DISTANCE_FILTER;
        mUpdateInterval = options.hasKey("interval")
                ? (long) options.getDouble("interval")
                : DEFAULT_INTERVAL;
        mFastestInterval = options.hasKey("fastestInterval")
                ? (long) options.getDouble("fastestInterval")
                : DEFAULT_INTERVAL;
        mShowLocationDialog = options.hasKey("showLocationDialog")
                ? options.getBoolean("showLocationDialog")
                : true;

        LocationSettingsRequest locationSettingsRequest = buildLocationSettingsRequest();

        if (mSettingsClient != null) {
            mSettingsClient.checkLocationSettings(locationSettingsRequest)
                    .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                        @Override
                        public void onComplete(Task<LocationSettingsResponse> task) {
                            onLocationSettingsResponse(task, false);
                        }
                    });
        }
    }

    /**
     * Stop listening for location updates.
     */
    @ReactMethod
    public void stopObserving() {
        if (mFusedProviderClient != null && mLocationCallback != null) {
            mFusedProviderClient.removeLocationUpdates(mLocationCallback);
            mLocationCallback = null;
        }
    }

    /**
     * Build location setting request using current configuration
     */
    private LocationSettingsRequest buildLocationSettingsRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(mLocationPriority)
                .setInterval(mUpdateInterval)
                .setFastestInterval(mFastestInterval)
                .setSmallestDisplacement(mDistanceFilter);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        return locationSettingsRequest;
    }

    /**
     * Check location setting response and decide whether to proceed with
     * location request or not.
     */
    private void onLocationSettingsResponse(
            Task<LocationSettingsResponse> task,
            boolean isSingleUpdate
    ) {
        try {
            LocationSettingsResponse response = task.getResult(ApiException.class);
            
            // All location settings are satisfied, start location request.
            if (isSingleUpdate) {
                getUserLocation();
            } else {
                getLocationUpdates();
            }
        } catch (ApiException exception) {
            switch (exception.getStatusCode()) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    /**
                     * Location settings are not satisfied. But could be fixed by showing the
                     * user a dialog. It means either location serivce is not enabled or
                     * default location mode is not enough to perform the request.
                     */
                    if (!mShowLocationDialog) {
                        invokeError(
                                LocationError.SETTINGS_NOT_SATISFIED.getValue(),
                                "Location settings are not satisfied.",
                                isSingleUpdate
                        );
                        break;
                    }

                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        Activity activity = getCurrentActivity();

                        if (activity == null) {
                            invokeError(
                                    LocationError.INTERNAL_ERROR.getValue(),
                                    "Tried to open location dialog while not attached to an Activity",
                                    isSingleUpdate
                            );
                            break;
                        }

                        resolvable.startResolutionForResult(
                                activity,
                                isSingleUpdate ? REQUEST_SETTINGS_SINGLE_UPDATE : REQUEST_SETTINGS_CONTINUOUS_UPDATE
                        );
                    } catch (SendIntentException e) {
                        invokeError(
                                LocationError.INTERNAL_ERROR.getValue(),
                                "Internal error occurred",
                                isSingleUpdate
                        );
                    } catch (ClassCastException e) {
                        invokeError(
                                LocationError.INTERNAL_ERROR.getValue(),
                                "Internal error occurred",
                                isSingleUpdate
                        );
                    }

                    break;
                default:
                    // TODO: we may have to handle other use case here.
                    // For now just say that settings are not ok.
                    invokeError(
                            LocationError.SETTINGS_NOT_SATISFIED.getValue(),
                            "Location settings are not satisfied.",
                            isSingleUpdate
                    );

                    break;
            }
        }
    }

    /**
     * Get last known location if it exists, otherwise request a new update.
     */
    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        if (mFusedProviderClient != null) {
            mFusedProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(Task<Location> task) {
                    Location location = task.getResult();

                    if (location != null &&
                            (SystemClock.currentTimeMillis() - location.getTime()) < mMaximumAge) {
                        invokeSuccess(location, true);
                        return;
                    }

                    // Last location is not available, request location update.
                    new SingleLocationUpdate(
                            mFusedProviderClient,
                            mLocationRequest,
                            mTimeout,
                            mSuccessCallback,
                            mErrorCallback
                    ).getLocation();
                }
            });
        }
    }

    /**
     * Get periodic location updates based on the current location request.
     */
    @SuppressLint("MissingPermission")
    private void getLocationUpdates() {
        if (mFusedProviderClient != null && mLocationRequest != null) {
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    invokeSuccess(location, false);
                }
            };

            mFusedProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    /**
     * Get react context
     */
    private ReactApplicationContext getContext() {
        return getReactApplicationContext();
    }

    /**
     * Clear the JS callbacks
     */
    private void clearCallbacks() {
        mSuccessCallback = null;
        mErrorCallback = null;
    }

    private void emitEvent(String event, Object data) {
        getContext().getJSModule(RCTDeviceEventEmitter.class)
                .emit(event, data);
    }


    /**
     * Helper method to invoke success callback
     */
    private void invokeSuccess(Location location, boolean isSingleUpdate) {
        WritableMap data = LocationUtils.buildSuccess(location);

        if (!isSingleUpdate) {
            emitEvent("geolocationDidChange", data);

            return;
        }

        try {
            if (mSuccessCallback != null) {
                mSuccessCallback.invoke(data);
            }

            clearCallbacks();
        } catch (RuntimeException e) {
            // Illegal callback invocation
            Log.w(TAG, e.getMessage());
        }
    }

    /**
     * Helper method to invoke error callback
     */
    private void invokeError(int code, String message, boolean isSingleUpdate) {
        WritableMap data = LocationUtils.buildError(code, message);

        if (!isSingleUpdate) {
            emitEvent("geolocationError", data);

            return;
        }

        try {
            if (mErrorCallback != null) {
                mErrorCallback.invoke(data);
            }

            clearCallbacks();
        } catch (RuntimeException e) {
            // Illegal callback invocation
            Log.w(TAG, e.getMessage());
        }
    }


    /**
     * Clear the JS settings callbacks
     */
    private void clearSettingsCallbacks() {
        mSuccessSettingsCallback = null;
        mErrorSettingsCallback = null;
    }


    /**
     * Helper method to invoke success callback
     */
    private void invokeSettingsSuccess(int locationMode, boolean isLocationEnabled,
                                       LocationProvider gpsProvider, boolean isGpsEnabled,
                                       LocationProvider networkProvider, boolean isNetworkEnabled) {
        WritableMap data = LocationUtils.buildLocationSettingsSuccess(locationMode, isLocationEnabled,
                gpsProvider, isGpsEnabled, networkProvider, isNetworkEnabled);
        try {
            if (mSuccessSettingsCallback != null) {
                mSuccessSettingsCallback.invoke(data);
            }

            clearSettingsCallbacks();
        } catch (RuntimeException e) {
            // Illegal callback invocation
            Log.w(TAG, e.getMessage());
        }
    }

    /**
     * Helper method to invoke error callback
     */
    private void invokeSettingsError(int code, String message) {
        WritableMap data = LocationUtils.buildError(code, message);

        try {
            if (mErrorSettingsCallback != null) {
                mErrorSettingsCallback.invoke(data);
            }

            clearSettingsCallbacks();
        } catch (RuntimeException e) {
            // Illegal callback invocation
            Log.w(TAG, e.getMessage());
        }
    }
}
