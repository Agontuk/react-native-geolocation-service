package com.agontuk.RNFusedLocation;

public interface LocationProvider {
  void getCurrentLocation(LocationOptions locationOptions);

  boolean onActivityResult(int requestCode, int resultCode);

  void requestLocationUpdates(LocationOptions locationOptions);

  void removeLocationUpdates();
}
