package com.agontuk.RNFusedLocation;

public interface LocationProvider {
  void getCurrentLocation(LocationOptions locationOptions, LocationChangeListener locationChangeListener);

  boolean onActivityResult(int requestCode, int resultCode);

  void requestLocationUpdates(LocationOptions locationOptions, LocationChangeListener locationChangeListener);

  void removeLocationUpdates();
}
