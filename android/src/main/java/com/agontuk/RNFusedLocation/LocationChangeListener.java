package com.agontuk.RNFusedLocation;

import android.location.Location;

import androidx.annotation.Nullable;

public interface LocationChangeListener {
  void onLocationChange(LocationProvider locationProvider, Location location);

  void onLocationError(LocationProvider locationProvider, LocationError error, @Nullable String message);
}
