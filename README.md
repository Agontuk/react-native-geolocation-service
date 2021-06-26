# react-native-geolocation-service
React native geolocation service for iOS and android.

# Why ?
This library is created in an attempt to fix the location timeout issue on android with the react-native's current implementation of Geolocation API. This library tries to solve the issue by using Google Play Service's new `FusedLocationProviderClient` API, which Google strongly recommends over android's default framework location API. It automatically decides which provider to use based on your request configuration and also prompts you to change the location mode if it doesn't satisfy your current request configuration.

> NOTE: Location request can still timeout since many android devices have GPS issue in the hardware/system level. Check the [FAQ](#faq) for more details.

# Installation
yarn
```bash
yarn add react-native-geolocation-service
```

npm
```bash
npm install react-native-geolocation-service
```

# Compatibility
| RN Version | Package Version |
| ---------- | --------------- |
| >=0.60     | >=3.0.0         |
| <0.60      | 2.0.0           |
| <0.57      | 1.1.0           |

# Setup
 - See [docs/setup.md](docs/setup.md)
 - Check out example project

# Usage
Since this library was meant to be a drop-in replacement for the RN's Geolocation API, the usage is pretty straight forward, with some extra error cases to handle.

> One thing to note, for android this library assumes that location permission is already granted by the user, so you have to use `PermissionsAndroid` to request for permission before making the location request.

```js
...
import Geolocation from 'react-native-geolocation-service';
...

componentDidMount() {
  if (hasLocationPermission) {
    Geolocation.getCurrentPosition(
        (position) => {
          console.log(position);
        },
        (error) => {
          // See error code charts below.
          console.log(error.code, error.message);
        },
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 10000 }
    );
  }
}
```

# API
#### `async requestAuthorization(authorizationLevel) (iOS only)`
Request location permission based on the authorizationLevel parameter. Can be either `"whenInUse"` or `"always"`. You have to configure the plist keys during setup.

When promise resolves, returns the status of the authorization.
- `disabled` - Location service is disabled
- `granted` - Permission granted
- `denied` - Permission denied
- `restricted` - Permission restricted

#### `getCurrentPosition(successCallback, ?errorCallback, ?options)`
 - **successCallback**: Invoked with latest location info.
 - **errorCallback**: Invoked whenever an error is encountered.
 - **options**:

    | Name | Type | Default | Description |
    | -- | -- | -- | -- |
    | timeout | `ms` | `INFINITY` | Request timeout |
    | maximumAge | `ms` | `INFINITY` | How long previous location will be cached |
    | accuracy | `object` | `--` | {<br/>&nbsp;&nbsp;&nbsp;android: [Link](docs/accuracy.md#android),<br/>&nbsp;&nbsp;&nbsp;ios: [Link](docs/accuracy.md#ios)<br/>}<br /><br /> If not provided or provided with invalid value, falls back to use `enableHighAccuracy` |
    | enableHighAccuracy | `bool` | `false` | Use high accuracy mode
    | distanceFilter | `m` | `100` | Minimum displacement in meters
    | showLocationDialog | `bool` | `true` | Whether to ask to enable location in Android (android only)
    | forceRequestLocation | `bool` | `false` | Force request location even after denying improve accuracy dialog (android only)
    | forceLocationManager | `bool` | `false` | If set to `true`, will use android's default LocationManager API (android only)

#### `watchPosition(successCallback, ?errorCallback, ?options)`
 - **successCallback**: Invoked with latest location info.
 - **errorCallback**: Invoked whenever an error is encountered.
 - **options**:

    | Name | Type | Default | Description |
    | -- | -- | -- | -- |
    | accuracy | `object` | `--` | {<br/>&nbsp;&nbsp;&nbsp;android: [Link](docs/accuracy.md#android),<br/>&nbsp;&nbsp;&nbsp;ios: [Link](docs/accuracy.md#ios)<br/>}<br /><br /> If not provided or provided with invalid value, falls back to use `enableHighAccuracy` |
    | enableHighAccuracy | `bool` | `false` | Use high accuracy mode
    | distanceFilter | `m` | `100` | Minimum displacement between location updates in meters
    | interval | `ms` | `10000` |  Interval for active location updates (android only)
    | fastestInterval | `ms` | `5000` | Fastest rate at which your application will receive location updates, which might be faster than `interval` in some situations (for example, if other applications are triggering location updates) (android only)
    | showLocationDialog | `bool` | `true` | whether to ask to enable location in Android (android only)
    | forceRequestLocation | `bool` | `false` | Force request location even after denying improve accuracy dialog (android only)
    | forceLocationManager | `bool` | `false` | If set to `true`, will use android's default LocationManager API (android only)
    | useSignificantChanges | `bool` | false | Uses the battery-efficient native significant changes APIs to return locations. Locations will only be returned when the device detects a significant distance has been breached (iOS only)
    | showsBackgroundLocationIndicator | `bool` | false | This setting enables a blue bar or a blue pill in the status bar on iOS. When the app moves to the background, the system uses this property to determine whether to change the status bar appearance to indicate that location services are in use. Users can tap the indicator to return to your app. (iOS only)

#### `clearWatch(watchId)`
 - watchId (id returned by `watchPosition`)

#### `stopObserving()`
Stops observing for device location changes. In addition, it removes all listeners previously registered.

# Error Codes
| Name | Code | Description |
| --- | --- | --- |
| PERMISSION_DENIED | 1 | Location permission is not granted |
| POSITION_UNAVAILABLE | 2 | Location provider not available |
| TIMEOUT | 3 | Location request timed out |
| PLAY_SERVICE_NOT_AVAILABLE | 4 | Google play service is not installed or has an older version (android only) |
| SETTINGS_NOT_SATISFIED | 5 | Location service is not enabled or location mode is not appropriate for the current request (android only) |
| INTERNAL_ERROR | -1 | Library crashed for some reason or the `getCurrentActivity()` returned null (android only) |

# FAQ
1. **Location timeout still happening ?**

    Try the following steps: (Taken from [here](https://support.strava.com/hc/en-us/articles/216918967-Troubleshooting-GPS-Issues))
    - Turn phone off/on
    - Turn GPS off/on
    - Disable any battery saver settings, including Power Saving Mode, Battery Management or any third party apps
    - Perform an "AGPS reset": Install the App GPS Status & Toolbox, then in that app, go to Menu > Tools > Manage A-GPS State > Reset

    Adjusting battery saver settings on different devices:

    - HTC: Access your phone settings > battery > power saving mode > battery optimization > select your app > don't optimize > save
    - Huawei: Turn Energy Settings to Normal and add your app to "Protected Apps"
    - LG If you're running Android 6 or higher: Settings > battery & power saving > battery usage > ignore optimizations > turn ON for your app
    - Motorola If you're running Android 6 or higher: Battery > select the menu in the upper right-hand corner > battery optimization > not optimized > all apps > select your app > don't optimize
    - OnePlus (using OxygenOS Settings): Battery > battery optimization > switch to 'all apps' > select your app > don't optimize
    - Samsung: Access battery settings > app power saving > details > your app > disabled
    - Sony If you're running Android 6 or higher: Battery > from the menu in the upper right-hand corner > battery optimization > apps > your app
    - Xiaomi (MIUI OS) If you're running Android 6 or higher: Access your phone settings > additional settings > battery and performance > manage battery usage > apps > your app
