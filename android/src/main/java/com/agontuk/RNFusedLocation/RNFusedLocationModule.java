package com.agontuk.RNFusedLocation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            Log.d(TAG, "Request code: " + requestCode + ", Result code: " + resultCode);
            if (requestCode == REQUEST_CHECK_SETTINGS) {
                if (resultCode == Activity.RESULT_CANCELED) {
                    //
                } else if (resultCode == Activity.RESULT_OK) {
                    //
                }
            }
        }
    };

    public RNFusedLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);

        // mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(reactContext);
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    @Override
    public String getName() {
        return "RNFusedLocation";
    }

    @Override
    public void onComplete(Task<LocationSettingsResponse> task) {
        try {
            LocationSettingsResponse response = task.getResult(ApiException.class);
            // All location settings are satisfied, start location request.
            // LocationServices.getFusedLocationProviderClient(getContext())
            //     .getLastLocation()
            //     .addOnCompleteListener(new OnCompleteListener<Location>() {
            //         @Override
            //         public void onComplete(Task<Location> task) {
            //             Log.d(TAG, "Location: " + task.getResult());
            //         }
            //     });
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
                            getCurrentActivity(),
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
                    mErrorCallback.invoke(buildError(
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
     */
    @ReactMethod
    public void getCurrentPosition(ReadableMap options, final Callback success, final Callback error) {
        if (!hasLocationPermission()) {
            error.invoke(buildError(
                LocationError.PERMISSION_DENIED.getValue(),
                "Location permission not granted."
            ));

            return;
        }

        if (!isGooglePlayServicesAvailable()) {
            error.invoke(buildError(
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

        LocationServices.getSettingsClient(getContext())
            .checkLocationSettings(locationSettingsRequest)
            .addOnCompleteListener(this);
    }

    private Context getContext() {
        return getReactApplicationContext();
    }

    private void clearCallbacks() {
        mSuccessCallback = null;
        mErrorCallback = null;
    }

    /**
     * Check if location permissions are granted.
     */
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if google play service is available on device.
     */
    private boolean isGooglePlayServicesAvailable() {
        int result =  GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());

        // TODO: Handle other possible success types.
        return result == ConnectionResult.SUCCESS || result == ConnectionResult.SERVICE_UPDATING;
    }

    /**
     * Build error response for error callback.
     */
    private WritableMap buildError(int code, String message) {
        WritableMap error = Arguments.createMap();
        error.putInt("code", code);

        if (message != null) {
            error.putString("message", message);
        }

        return error;
    }
}
