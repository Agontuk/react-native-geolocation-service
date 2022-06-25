# Changelog

### 5.3.0 (June 25, 2022)
 - Android: set `distanceFilter` to `0` in `getCurrentPosition` API
 - Android: update default version of play services location to 18.0.0
 - Android: rewrite location change listener logic to fix `Illegal callback invocation` error

### 5.3.0-beta.4 (December 08, 2021)
 - iOS: rewrite implementation
 - iOS: revert back to use `requestLocation` API to avoid cached location issue
 - iOS: fix `getCurrentPosition` not working issue without timeout
 - iOS: remove `distanceFilter` support in `getCurrentPosition` API

### 5.3.0-beta.3 (September 25, 2021)
 - Android: fix NativeEventEmitter warnings in RN 0.65

### 5.3.0-beta.2 (August 28, 2021)
 - Android: expose option to use LocationManager API

### 5.3.0-beta.1 (April 23, 2021)
 - Android: update default build tools & sdk version
 - Android: add vertical accuracy & provider name in location data if available
 - Android: rewrite implementation & add support for LocationManager API
 - Android: fallback to use LocationManager if Google Play Service is not available

### 5.2.0 (February 2, 2021)
 - Android: fix location request on airplane mode
 - iOS: add ability to configure showsBackgroundLocationIndicator
 - iOS: fix location delay in getCurrentPosition method
 - iOS: add `reduced` as an accuracy option

### 5.1.1 (November 28, 2020)
 - Fix iOS `pod install` issue.

### 5.1.0 (November 26, 2020)
 - Android/iOS: add support for specifying accuracy level
 - Android: only trigger error in watchPosition if location service is off

### 5.0.0 (May 30, 2020)
#### Breaking
 - iOS: new iOS module implementation. Follow this [issue](https://github.com/Agontuk/react-native-geolocation-service/issues/173) for details.
 - iOS: Removed `setRNConfiguration` usage & updated `requestAuthorization` to return promise.
 - Android: removed permissions from manifest. You have to declare location permission in your main `AndroidManifest.xml`.
 - Android: update play-services-location version to `17.0.0`.

#### Fixes
 - Android: emit error if location is unavailable during `watchPosition` call.
 - Only start timer if timeout is valid.

### 4.0.2 (May 15, 2020)
 - Fix ios module warning

### 4.0.1 (April 29, 2020)
 - Removed premature check of isLocationEnabled in startObserving

### 4.0.0 (February 3, 2020)
 - Android: update android support library to androidx
 - Android: fix ApiException handling in getLastLocation method

### 3.1.0 (August 30, 2019)
 - Add support for web
 - Update location request flow ([described here](https://github.com/Agontuk/react-native-geolocation-service/issues/108#issuecomment-524217651))

### 3.0.0 (July 23, 2019)
 - Fix typescript definition
 - __BREAKING__: Switch to `@react-native-community/geolocation` library for iOS implementation (follow iOS setup instruction)
 - Export `PositionError` constants
 - Added `forceRequestLocation` flag to request location with only GPS enabled

### 2.0.1 (May 5, 2019)
 - Add typescript definition

### 2.0.0 (December 2, 2018)
 - Support RN 0.57+

### 1.1.0 (July 2, 2018)
 - Added support for project wide gradle properties.
 - Added support for tracking location update.

### 1.0.4 (April 20, 2018)
 - Fix crash due to illegal callback invocation.
 - Added `showLocationDialog` option to control whether to show location dialog if it is disabled.
