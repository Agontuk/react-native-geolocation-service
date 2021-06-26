package com.agontuk.RNFusedLocation;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

public class LocationOptions {
  private static final float DEFAULT_DISTANCE_FILTER = 100;
  private static final long DEFAULT_INTERVAL = 10 * 1000;  /* 10 secs */
  private static final long DEFAULT_FASTEST_INTERVAL = 5 * 1000; /* 5 sec */

  private final LocationAccuracy accuracy;
  private final long interval;
  private final long fastestInterval;
  private final float distanceFilter;
  private final long timeout;
  private final double maximumAge;
  private final boolean showLocationDialog;
  private final boolean forceRequestLocation;
  private final boolean forceLocationManager;

  private LocationOptions(
    LocationAccuracy accuracy,
    long interval,
    long fastestInterval,
    float distanceFilter,
    long timeout,
    double maximumAge,
    boolean showLocationDialog,
    boolean forceRequestLocation,
    boolean forceLocationManager
  ) {
    this.accuracy = accuracy;
    this.interval = interval;
    this.fastestInterval = fastestInterval;
    this.distanceFilter = distanceFilter;
    this.timeout = timeout;
    this.maximumAge = maximumAge;
    this.showLocationDialog = showLocationDialog;
    this.forceRequestLocation = forceRequestLocation;
    this.forceLocationManager = forceLocationManager;
  }

  public static LocationOptions fromReadableMap(ReadableMap map) {
    LocationAccuracy accuracy = getAccuracy(map);
    long interval = map.hasKey("interval")
      ? (long) map.getDouble("interval")
      : DEFAULT_INTERVAL;
    long fastestInterval = map.hasKey("fastestInterval")
      ? (long) map.getDouble("fastestInterval")
      : DEFAULT_FASTEST_INTERVAL;
    float distanceFilter = map.hasKey("distanceFilter")
      ? (float) map.getDouble("distanceFilter")
      : DEFAULT_DISTANCE_FILTER;
    long timeout = map.hasKey("timeout")
      ? (long) map.getDouble("timeout")
      : Long.MAX_VALUE;
    double maximumAge = map.hasKey("maximumAge")
      ? map.getDouble("maximumAge")
      : Double.POSITIVE_INFINITY;
    boolean showLocationDialog =
      !map.hasKey("showLocationDialog") || map.getBoolean("showLocationDialog");
    boolean forceRequestLocation =
      map.hasKey("forceRequestLocation") && map.getBoolean("forceRequestLocation");
    boolean forceLocationManager =
      map.hasKey("forceLocationManager") && map.getBoolean("forceLocationManager");

    return new LocationOptions(
      accuracy,
      interval,
      fastestInterval,
      distanceFilter,
      timeout,
      maximumAge,
      showLocationDialog,
      forceRequestLocation,
      forceLocationManager
    );
  }

  public LocationAccuracy getAccuracy() {
    return accuracy;
  }

  public long getInterval() {
    return interval;
  }

  public long getFastestInterval() {
    return fastestInterval;
  }

  public float getDistanceFilter() {
    return distanceFilter;
  }

  public long getTimeout() {
    return timeout;
  }

  public double getMaximumAge() {
    return maximumAge;
  }

  public boolean isShowLocationDialog() {
    return showLocationDialog;
  }

  public boolean isForceRequestLocation() {
    return forceRequestLocation;
  }

  public boolean isForceLocationManager() {
    return forceLocationManager;
  }

  /**
   * Determine location priority from user provided accuracy level
   */
  private static LocationAccuracy getAccuracy(@NonNull ReadableMap options) {
    String accuracy = "";
    boolean highAccuracy = options.hasKey("enableHighAccuracy")
      && options.getBoolean("enableHighAccuracy");

    if (options.hasKey("accuracy") && options.getType("accuracy") == ReadableType.Map) {
      ReadableMap accuracyMap = options.getMap("accuracy");

      if (accuracyMap != null &&
        accuracyMap.hasKey("android") &&
        accuracyMap.getType("android") == ReadableType.String
      ) {
        String value = accuracyMap.getString("android");

        if (value != null) {
          accuracy = value;
        }
      }
    }

    switch (accuracy) {
      case "high":
        return LocationAccuracy.high;
      case "balanced":
        return LocationAccuracy.balanced;
      case "low":
        return LocationAccuracy.low;
      case "passive":
        return LocationAccuracy.passive;
      default:
        return highAccuracy ? LocationAccuracy.high : LocationAccuracy.balanced;
    }
  }
}
