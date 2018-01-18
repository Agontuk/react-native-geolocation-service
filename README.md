# react-native-geolocation-service
React native geolocation service for iOS and android.

# Why ?
This library is created in an attempt to fix the location timeout issue on android with the react-native's current implementation of Geolocation API. This library tries to solve the issue by using Google Play Service's new `FusedLocationProviderClient` API, which Google strongly recommends over android's default framework location API. It automatically decides which provider to use based on your request configuration and also prompts you to change the location mode if it doesn't satisfy your current request configuration.

# Installation
```bash
yarn add react-native-geolocation-service
```

# Setup

## iOS
No additional setup is required, since it uses the React Native's default Geolocation API. Just follow the [React Native documentation](https://facebook.github.io/react-native/docs/geolocation.html#ios) to modify the `.plist` file.

## Android
1. In `android/app/build.gradle`

    ```gradle
    ...
    dependencies {
        ...
        compile project(':react-native-geolocation-service')
    }
    ```

    If you have a different play service version than the one included in this library, use the following instead. But play service version should be `11+` or the library won't work.

    ```gradle
    ...
    dependencies {
        ...
        compile(project(':react-native-geolocation-service')) {
            exclude group: 'com.google.android.gms', module: 'play-services-location'
        }
        compile 'com.google.android.gms:play-services-location:11.0.4'
    }
    ```

2. In `android/setting.gradle`

    ```gradle
    ...
    include ':react-native-geolocation-service'
    project(':react-native-geolocation-service').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-geolocation-service/android')
    ```

3. In `MainApplication.java`

    ```java
    ...
    import com.agontuk.RNFusedLocation.RNFusedLocationPackage;

    public class MainActivity extends ReactActivity {
        ...
        @Override
        protected List<ReactPackage> getPackages() {
            return Arrays.<ReactPackage>asList(
                ...
                new RNFusedLocationPackage()
            );
        }
    }
    ```

# Usage
Since this library was meant to be a drop-in replacement for the RN's Geolocation API, the usage is pretty straight forward, with some extra error cases to handle.

> One thing to note, this library assumes that location permission is already granted by the user, so you have to use `PermissionsAndroid` to request for permission before making the location request.

```js
...
import Geolocation from 'react-native-geolocation-service';
...

componentDidMount() {
    // Instead of navigator.geolocation, just use Geolocation.
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
Checkout [React Native documentation](https://facebook.github.io/react-native/docs/geolocation.html#reference) to see the list of available methods.


# Error Codes
| Name | Code | Description |
| --- | --- | --- |
| PERMISSION_DENIED | 1 | Location permission is not granted |
| POSITION_UNAVAILABLE | 2 | Unable to determine position (not used yet) |
| TIMEOUT | 3 | Location request timed out |
| PLAY_SERVICE_NOT_AVAILABLE | 4 | Google play service is not installed or has an older version |
| SETTINGS_NOT_SATISFIED | 5 | Location service is not enabled or location mode is not appropriate for the current request |
| INTERNAL_ERROR | -1 | Library crashed for some reason |

# TODO
- [ ] Implement `watchPosition` & `clearWatch` methods for android
- [ ] Implement `stopObserving` method for android
