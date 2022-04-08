package com.agontuk.RNFusedLocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Random;

public class FusedLocationProvider implements LocationProvider {
  private final ReactApplicationContext context;
  private final FusedLocationProviderClient fusedLocationProviderClient;
  private final LocationChangeListener locationChangeListener;
  private final SettingsClient settingsClient;

  private int activityRequestCode;
  private LocationOptions locationOptions;
  private LocationRequest locationRequest;

  private boolean isSingleUpdate = false;
  private final LocationCallback locationCallback = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult locationResult) {
      locationChangeListener.onLocationChange(
        FusedLocationProvider.this,
        locationResult.getLastLocation()
      );

      if (isSingleUpdate) {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
      }
    }

    @Override
    public void onLocationAvailability(LocationAvailability locationAvailability) {
      if (!locationAvailability.isLocationAvailable() &&
        !LocationUtils.isLocationEnabled(context)
      ) {
        locationChangeListener.onLocationError(
          FusedLocationProvider.this,
          LocationError.POSITION_UNAVAILABLE,
          "Unable to retrieve location."
        );
      }
    }
  };
  private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
  private final Runnable timeoutRunnable = new Runnable() {
    @Override
    public void run() {
      locationChangeListener.onLocationError(
        FusedLocationProvider.this,
        LocationError.TIMEOUT,
        null
      );
      fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
  };

  public FusedLocationProvider(ReactApplicationContext context, LocationChangeListener locationChangeListener) {
    this.context = context;
    this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    this.locationChangeListener = locationChangeListener;
    this.settingsClient = LocationServices.getSettingsClient(context);
  }

  @SuppressLint("MissingPermission")
  @Override
  public void getCurrentLocation(final LocationOptions locationOptions) {
    this.isSingleUpdate = true;
    this.locationOptions = locationOptions;
    this.locationRequest = buildLocationRequest(locationOptions);

    fusedLocationProviderClient.getLastLocation()
      .addOnSuccessListener(new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
          if (location != null &&
            LocationUtils.getLocationAge(location) < locationOptions.getMaximumAge()
          ) {
            Log.i(RNFusedLocationModule.TAG, "returning cached location.");
            locationChangeListener.onLocationChange(FusedLocationProvider.this, location);
            return;
          }

          checkLocationSettings();
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          checkLocationSettings();
        }
      });
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode) {
    if (requestCode != activityRequestCode) {
      return false;
    }

    if (resultCode == Activity.RESULT_OK) {
      startLocationUpdates();
      return true;
    }

    boolean forceRequestLocation = locationOptions.isForceRequestLocation();
    boolean locationEnabled = LocationUtils.isLocationEnabled(context);

    if (forceRequestLocation && locationEnabled) {
      startLocationUpdates();
    } else {
      locationChangeListener.onLocationError(
        FusedLocationProvider.this,
        locationEnabled ? LocationError.SETTINGS_NOT_SATISFIED : LocationError.POSITION_UNAVAILABLE,
        null
      );
    }

    return true;
  }

  @Override
  public void requestLocationUpdates(LocationOptions locationOptions) {
    this.isSingleUpdate = false;
    this.locationOptions = locationOptions;
    this.locationRequest = buildLocationRequest(locationOptions);
    checkLocationSettings();
  }

  @Override
  public void removeLocationUpdates() {
    fusedLocationProviderClient.removeLocationUpdates(locationCallback);
  }

  private LocationRequest buildLocationRequest(LocationOptions options) {
    LocationRequest locationRequest = new LocationRequest();
    int priority = getLocationPriority(options.getAccuracy());

    locationRequest.setPriority(priority)
      .setInterval(options.getInterval())
      .setFastestInterval(options.getFastestInterval())
      .setSmallestDisplacement(isSingleUpdate ? 0 : options.getDistanceFilter());

    return locationRequest;
  }

  private void checkLocationSettings() {
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(locationRequest);
    LocationSettingsRequest locationSettingsRequest = builder.build();

    settingsClient.checkLocationSettings(locationSettingsRequest)
      .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
        @Override
        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
          startLocationUpdates();
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          ApiException exception = (ApiException) e;

          switch (exception.getStatusCode()) {
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
              boolean showLocationDialog = locationOptions.isShowLocationDialog();
              boolean forceRequestLocation = locationOptions.isForceRequestLocation();
              boolean locationEnabled = LocationUtils.isLocationEnabled(context);

              if (!showLocationDialog) {
                if (forceRequestLocation && locationEnabled) {
                  startLocationUpdates();
                } else {
                  locationChangeListener.onLocationError(
                    FusedLocationProvider.this,
                    locationEnabled ? LocationError.SETTINGS_NOT_SATISFIED : LocationError.POSITION_UNAVAILABLE,
                    null
                  );
                }

                break;
              }

              try {
                ResolvableApiException resolvable = (ResolvableApiException) exception;
                Activity activity = context.getCurrentActivity();

                if (activity == null) {
                  locationChangeListener.onLocationError(
                    FusedLocationProvider.this,
                    LocationError.INTERNAL_ERROR,
                    "Tried to open location dialog while not attached to an Activity."
                  );
                  break;
                }

                activityRequestCode = getActivityRequestCode();
                resolvable.startResolutionForResult(activity, activityRequestCode);
              } catch (IntentSender.SendIntentException | ClassCastException sie) {
                locationChangeListener.onLocationError(
                  FusedLocationProvider.this,
                  LocationError.INTERNAL_ERROR,
                  null
                );
              }

              break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
              if (LocationUtils.isOnAirplaneMode(context) &&
                LocationUtils.isProviderEnabled(context, LocationManager.GPS_PROVIDER)
              ) {
                startLocationUpdates();
                break;
              }
            default:
              locationChangeListener.onLocationError(
                FusedLocationProvider.this,
                LocationError.SETTINGS_NOT_SATISFIED,
                null
              );
              break;
          }
        }
      });
  }

  private int getActivityRequestCode() {
    Random random = new Random();
    return random.nextInt(10000);
  }

  private int getLocationPriority(LocationAccuracy locationAccuracy) {
    switch (locationAccuracy) {
      case high:
        return LocationRequest.PRIORITY_HIGH_ACCURACY;
      case balanced:
        return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
      case low:
        return LocationRequest.PRIORITY_LOW_POWER;
      case passive:
        return LocationRequest.PRIORITY_NO_POWER;
      default:
        throw new IllegalStateException("Unexpected value: " + locationAccuracy);
    }
  }

  @SuppressLint("MissingPermission")
  private void startLocationUpdates() {
    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

    if (isSingleUpdate) {
      long timeout = locationOptions.getTimeout();

      if (timeout > 0 && timeout != Long.MAX_VALUE) {
        timeoutHandler.postDelayed(timeoutRunnable, timeout);
      }
    }
  }
}
