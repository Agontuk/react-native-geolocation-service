package com.agontuk.RNFusedLocation;

public interface LocationManager {
  void getCurrentLocation(LocationOptions locationOptions, LocationListener locationListener);

  boolean onActivityResult(int requestCode, int resultCode);

  void requestLocationUpdates(LocationOptions locationOptions, LocationListener locationListener);

  void removeLocationUpdates();
}
