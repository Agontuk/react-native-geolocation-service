# react-native-geolocation
React native geolocation service for iOS and android.

# Why ?
This library is created in an attempt to fix the location timeout issue on android with the react-native's current implementation of Geolocation API. This library tries to solve the issue by using Google Play Service's new `FusedLocationProviderClient` API, which Google strongly recommends over android's default framework location API.

# Installation
```bash
yarn add react-native-geolocation
```

# Setup

## iOS
No setup required, since it uses the React Native's default Geolocation API.

## Android
1. In `android/app/build.gradle`

    ```gradle
    ...
    dependencies {
        ...
        compile project(':react-native-geolocation')
    }
    ```

    If you have a different play service version than the one included in this library, use the following instead. But play service version should be `11+` or the library won't work.

    ```gradle
    ...
    dependencies {
        ...
        compile(project(':react-native-geolocation')) {
            exclude group: 'com.google.android.gms', module: 'play-services-location'
        }
        compile 'com.google.android.gms:play-services-location:11.0.4'
    }
    ```

2. In `android/setting.gradle`

    ```gradle
    ...
    include ':react-native-geolocation'
    project(':react-native-geolocation').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-geolocation/android')
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
Since this library was meant to be a drop-in replacement for the RN's Geolocation API, the usage is pretty straight forward, with some extra error codes to handle.

> One thing to note, this library assumes that location permission is already granted by the user, so you have to use `PermissionsAndroid` to request for permission before making the location request.

```js
...
import Geolocation from 'react-native-geolocation';
...

componentDidMount() {
    // Instead of navigator.geolocation, just use Geolocation.
    if (hasLocationPermission) {
        Geolocation.getCurrentPosition(
            (position) => {
                this._handleGeoLocationSuccess(position);
            },
            (error) => {
                this._handleGeoLocationError(error);
            },
            { enableHighAccuracy: true, timeout: 15000, maximumAge: 10000 }
        );
    }
}
```
