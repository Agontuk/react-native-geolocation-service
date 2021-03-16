package com.agontuk.RNFusedLocation;

import android.location.Location;

import androidx.annotation.Nullable;

public interface LocationChangeListener {
  void onLocationChange(Location location);

  void onLocationError(LocationError error, @Nullable String message);
}
