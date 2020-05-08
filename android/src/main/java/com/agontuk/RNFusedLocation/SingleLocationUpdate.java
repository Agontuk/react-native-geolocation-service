package com.agontuk.RNFusedLocation;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.lang.RuntimeException;

public class SingleLocationUpdate {
  private final FusedLocationProviderClient mFusedProviderClient;
  private final LocationRequest mLocationRequest;
  private final long mTimeout;
  private final Callback mSuccessCallback;
  private final Callback mErrorCallback;

  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private final Runnable mTimeoutRunnable = new Runnable() {
    @Override
    public void run() {
      synchronized (SingleLocationUpdate.this) {
        invokeError(LocationError.TIMEOUT.getValue(), "Location request timed out.");

        // Remove further location update.
        if (mFusedProviderClient != null && mLocationCallback != null) {
          mFusedProviderClient.removeLocationUpdates(mLocationCallback);
        }

        Log.i(RNFusedLocationModule.TAG, "Location request timed out");
      }
    }
  };

  private final LocationCallback mLocationCallback = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult locationResult) {
      synchronized (SingleLocationUpdate.this) {
        Location location = locationResult.getLastLocation();
        invokeSuccess(LocationUtils.locationToMap(location));

        mHandler.removeCallbacks(mTimeoutRunnable);

        // Remove further location update.
        if (mFusedProviderClient != null && mLocationCallback != null) {
          mFusedProviderClient.removeLocationUpdates(mLocationCallback);
        }
      }
    }
  };

  public SingleLocationUpdate(
    FusedLocationProviderClient fusedLocationProviderClient,
    LocationRequest locationRequest,
    long timeout,
    Callback success,
    Callback error
  ) {
    mFusedProviderClient = fusedLocationProviderClient;
    mLocationRequest = locationRequest;
    mTimeout = timeout;
    mSuccessCallback = success;
    mErrorCallback = error;
  }

  /**
   * Request one time location update
   */
  public void getLocation() {
    if (mFusedProviderClient != null) {
      mFusedProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());

      if (mTimeout > 0 && mTimeout != Long.MAX_VALUE) {
        mHandler.postDelayed(mTimeoutRunnable, mTimeout);
      }
    }
  }

  /**
   * Helper method to invoke success callback
   */
  private void invokeSuccess(WritableMap data) {
    try {
      if (mSuccessCallback != null) {
        mSuccessCallback.invoke(data);
      }
    } catch (RuntimeException e) {
      // Illegal callback invocation
      Log.w(RNFusedLocationModule.TAG, e.getMessage());
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
    } catch (RuntimeException e) {
      // Illegal callback invocation
      Log.w(RNFusedLocationModule.TAG, e.getMessage());
    }
  }
}
