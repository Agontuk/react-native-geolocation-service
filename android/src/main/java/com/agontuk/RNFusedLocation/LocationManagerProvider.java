package com.agontuk.RNFusedLocation;

public class LocationManagerProvider implements LocationProvider {
  public LocationManagerProvider() {
    //
  }

  @Override
  public void getCurrentLocation(LocationOptions locationOptions, LocationChangeListener locationChangeListener) {
    //
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode) {
    return true;
  }

  @Override
  public void requestLocationUpdates(LocationOptions locationOptions, LocationChangeListener listener) {
    //
  }

  @Override
  public void removeLocationUpdates() {
    //
  }
}
