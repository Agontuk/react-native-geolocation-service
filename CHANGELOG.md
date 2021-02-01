# Changelog

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
