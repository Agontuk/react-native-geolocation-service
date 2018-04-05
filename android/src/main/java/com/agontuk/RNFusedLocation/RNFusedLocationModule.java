package com.agontuk.RNFusedLocation;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
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

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.lang.RuntimeException;

public class RNFusedLocationModule extends ReactContextBaseJavaModule {
    public static final String TAG = "RNFusedLocation";
    private static final int REQUEST_CHECK_SETTINGS = 11403;
    private static final float DEFAULT_DISTANCE_FILTER = 100;
    private static final int DEFAULT_ACCURACY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

    private int mLocationPriority = DEFAULT_ACCURACY;
    private long mUpdateInterval = 10 * 1000;  /* 10 secs */
    private long mFastestInterval = 5 * 1000; /* 5 sec */
    private double mMaximumAge = Double.POSITIVE_INFINITY;
    private long mTimeout = Long.MAX_VALUE;
    private float mDistanceFilter = DEFAULT_DISTANCE_FILTER;

    private Callback mSuccessCallback;
    private Callback mErrorCallback;
    private FusedLocationProviderClient mFusedProviderClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            if (requestCode == REQUEST_CHECK_SETTINGS) {
                if (resultCode == Activity.RESULT_CANCELED) {
                    // User cancelled the request.
                    // TODO: allow user to ignore this & request location.
                    invokeError(LocationError.SETTINGS_NOT_SATISFIED.getValue(),
                        "Location settings are not satisfied.");
                } else if (resultCode == Activity.RESULT_OK) {
                    // Location settings changed successfully, request user location.
                    getUserLocation();
                }
            }
        }
    };

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

    /**
     * Get the current position. This can return almost immediately if the location is cached or
     * request an update, which might take a while.
     *
     * @param options map containing optional arguments: timeout (millis), maximumAge (millis),
     *        highAccuracy (boolean) and distanceFilter
     * @param success success callback
     * @param error error callback
     */
    @ReactMethod
    public void getCurrentPosition(ReadableMap options, final Callback success, final Callback error) {
        ReactApplicationContext context = getContext();

        mSuccessCallback = success;
        mErrorCallback = error;

        if (!LocationUtils.hasLocationPermission(context)) {
            invokeError(LocationError.PERMISSION_DENIED.getValue(), "Location permission not granted.");
            return;
        }

        if (!LocationUtils.isGooglePlayServicesAvailable(context)) {
            invokeError(LocationError.PLAY_SERVICE_NOT_AVAILABLE.getValue(),
                "Google play service is not available.");
            return;
        }

        boolean highAccuracy = options.hasKey("enableHighAccuracy") && options.getBoolean("enableHighAccuracy");

        // TODO: Make other PRIORITY_* constants availabe to the user
        mLocationPriority = highAccuracy ? LocationRequest.PRIORITY_HIGH_ACCURACY : DEFAULT_ACCURACY;

        mTimeout = options.hasKey("timeout") ? (long) options.getDouble("timeout") : Long.MAX_VALUE;
        mMaximumAge = options.hasKey("maximumAge") ?
            options.getDouble("maximumAge") : Double.POSITIVE_INFINITY;
        mDistanceFilter = options.hasKey("distanceFilter") ? (float) options.getDouble("distanceFilter") : 0;

        LocationSettingsRequest locationSettingsRequest = buildLocationSettingsRequest();

        if (mSettingsClient != null) {
            mSettingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(Task<LocationSettingsResponse> task) {
                        onLocationSettingsResponse(task);
                    }
                });
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
    private void onLocationSettingsResponse(Task<LocationSettingsResponse> task) {
        try {
            LocationSettingsResponse response = task.getResult(ApiException.class);
            // All location settings are satisfied, start location request.
            getUserLocation();
        } catch (ApiException exception) {
            switch (exception.getStatusCode()) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    /**
                     * Location settings are not satisfied. But could be fixed by showing the
                     * user a dialog. It means either location serivce is not enabled or
                     * default location mode is not enough to perform the request.
                     *
                     * TODO: we may want to make it optional & just say that settings are not ok.
                     */
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        Activity activity = getCurrentActivity();

                        if (activity == null) {
                            invokeError(
                                LocationError.INTERNAL_ERROR.getValue(),
                                "Tried to open location dialog while not attached to an Activity"
                            );
                            return;
                        }

                        resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                    } catch (SendIntentException e) {
                        invokeError(LocationError.INTERNAL_ERROR.getValue(), "Internal error occurred");
                    } catch (ClassCastException e) {
                        invokeError(LocationError.INTERNAL_ERROR.getValue(), "Internal error occurred");
                    }

                    break;
                default:
                    // TODO: we may have to handle other use case here.
                    // For now just say that settings are not ok.
                    invokeError(LocationError.SETTINGS_NOT_SATISFIED.getValue(),
                        "Location settings are not satisfied.");

                    break;
            }
        }
    }

    /**
     * Get last known location if it exists, otherwise request a new update.
     */
    private void getUserLocation() {
        if (mFusedProviderClient != null) {
            mFusedProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(Task<Location> task) {
                    Location location = task.getResult();

                    if (location != null &&
                            (SystemClock.currentTimeMillis() - location.getTime()) < mMaximumAge) {
                        invokeSuccess(LocationUtils.locationToMap(location));
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

    /**
     * Helper method to invoke success callback
     */
    private void invokeSuccess(WritableMap data) {
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
    private void invokeError(int code, String message) {
        try {
            if (mErrorCallback != null) {
                mErrorCallback.invoke(LocationUtils.buildError(code, message));
            }

            clearCallbacks();
        } catch (RuntimeException e) {
            // Illegal callback invocation
            Log.w(TAG, e.getMessage());
        }
    }
}
