package com.agontuk.RNFusedLocation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class RNFusedLocationModule extends ReactContextBaseJavaModule implements
        OnCompleteListener<LocationSettingsResponse> {

    private static final String TAG = "RNFusedLocation";
    private static final int REQUEST_CHECK_SETTINGS = 11403;
    private int mLocationPriority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    private long mUpdateInterval = 10 * 1000;  /* 10 secs */
    private long mFastestInterval = 5 * 1000; /* 5 sec */

    private Callback mSuccessCallback;
    private Callback mErrorCallback;
    private FusedLocationProviderClient mFusedProviderClient;
    private LocationRequest mLocationRequest;

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            if (requestCode == REQUEST_CHECK_SETTINGS) {
                if (resultCode == Activity.RESULT_CANCELED) {
                    // User cancelled the request.
                    mErrorCallback.invoke(LocationUtils.buildError(
                        LocationError.SETTINGS_NOT_SATISFIED.getValue(),
                        "Location settings are not satisfied."
                    ));
                    clearCallbacks();
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
        reactContext.addActivityEventListener(mActivityEventListener);

        Log.i(TAG, TAG + " initialized");
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void onComplete(Task<LocationSettingsResponse> task) {
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
                        // Cast to a resolvable exception.
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        resolvable.startResolutionForResult(
                            getActivity(),
                            REQUEST_CHECK_SETTINGS
                        );
                    } catch (SendIntentException e) {
                        // Ignore the error.
                    } catch (ClassCastException e) {
                        // Ignore, should be an impossible error.
                    }

                    break;
                default:
                    // TODO: we may have to handle other use case here.
                    // For now just say that settings are not ok.
                    mErrorCallback.invoke(LocationUtils.buildError(
                        LocationError.SETTINGS_NOT_SATISFIED.getValue(),
                        "Location settings are not satisfied."
                    ));
                    clearCallbacks();
                    break;
            }
        }
    }

    /**
     * Get the current position. This can return almost immediately if the location is cached or
     * request an update, which might take a while.
     *
     * @param options map containing optional arguments: timeout (millis), maximumAge (millis) and
     *        highAccuracy (boolean)
     * @param success success callback
     * @param error error callback
     */
    @ReactMethod
    public void getCurrentPosition(ReadableMap options, final Callback success, final Callback error) {
        Context context = getContext();

        if (!LocationUtils.hasLocationPermission(context)) {
            error.invoke(LocationUtils.buildError(
                LocationError.PERMISSION_DENIED.getValue(),
                "Location permission not granted."
            ));

            return;
        }

        if (!LocationUtils.isGooglePlayServicesAvailable(context)) {
            error.invoke(LocationUtils.buildError(
                LocationError.PLAY_SERVICE_NOT_AVAILABLE.getValue(),
                "Google play service is not available."
            ));

            return;
        }

        mSuccessCallback = success;
        mErrorCallback = error;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(mLocationPriority);
        mLocationRequest.setInterval(mUpdateInterval);
        mLocationRequest.setFastestInterval(mFastestInterval);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        LocationServices.getSettingsClient(context)
            .checkLocationSettings(locationSettingsRequest)
            .addOnCompleteListener(this);
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
                    if (location == null) {
                        // Last location is not available, request location update.
                        requestLocationUpdates();
                    } else {
                        mSuccessCallback.invoke(LocationUtils.locationToMap(location));
                        clearCallbacks();
                    }
                }
            });
        }
    }

    /**
     * Request new location update for one time and remove updates after result is returned.
     */
    private void requestLocationUpdates() {
        if (mFusedProviderClient != null) {
            mFusedProviderClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    // Remove further location update.
                    mFusedProviderClient.removeLocationUpdates(this);

                    Location location = locationResult.getLastLocation();
                    mSuccessCallback.invoke(LocationUtils.locationToMap(location));
                    clearCallbacks();
                }
            }, Looper.myLooper());
        }
    }

    /**
     * Get react context
     */
    private Context getContext() {
        return getReactApplicationContext();
    }

    /**
     * Get the current activity
     */
    private Activity getActivity() {
        return getCurrentActivity();
    }

    /**
     * Clear the JS callbacks
     */
    private void clearCallbacks() {
        mSuccessCallback = null;
        mErrorCallback = null;
    }
}
