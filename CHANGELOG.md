# Changelog

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
