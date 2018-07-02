# react-native-geolocation-service
React native geolocation service for iOS and android.

# Why ?
This library is created in an attempt to fix the location timeout issue on android with the react-native's current implementation of Geolocation API. This library tries to solve the issue by using Google Play Service's new `FusedLocationProviderClient` API, which Google strongly recommends over android's default framework location API. It automatically decides which provider to use based on your request configuration and also prompts you to change the location mode if it doesn't satisfy your current request configuration.

> NOTE: Location request can still timeout since many android devices have GPS issue in the hardware level or in the system software level. Check the [FAQ](#faq) for more details.

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

    If you've defined [project-wide properties](https://developer.android.com/studio/build/gradle-tips#configure-project-wide-properties) (recommended) in your root build.gradle, this library will detect the presence of the following properties:

    ```gradle
    buildscript {...}
    allprojects {...}

    /**
     + Project-wide Gradle configuration properties
     */
    ext {
        compileSdkVersion   = 25
        targetSdkVersion    = 25
        buildToolsVersion   = "25.0.2"
        supportLibVersion   = "25.0.1"
        googlePlayServicesVersion = "11.0.0"
    }
    ```

    If you do not have *project-wide properties* defined and have a different play-services version than the one included in this library, use the following instead. But play service version should be `11+` or the library won't work.

    ```gradle
    ...
    dependencies {
        ...
        compile(project(':react-native-geolocation-service')) {
            exclude group: 'com.google.android.gms', module: 'play-services-location'
        }
        compile 'com.google.android.gms:play-services-location:<insert your play service version here>'
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

    public class MainApplication extends Application implements ReactApplication {
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

# API
#### `getCurrentPosition(successCallback, ?errorCallback, ?options)`
 - **successCallback**: Invoked with latest location info.
 - **errorCallback**: Invoked whenever an error is encountered.
 - **options**:
   - timeout (ms)
   - maximumAge (ms)
   - enableHighAccuracy (bool)
   - distanceFilter (double)
   - showLocationDialog (whether to ask to enable location in Android)

#### `watchPosition(successCallback, ?errorCallback, ?options)`
 - **successCallback**: Invoked with latest location info.
 - **errorCallback**: Invoked whenever an error is encountered.
 - **options**:
   - enableHighAccuracy (bool)
   - distanceFilter (double)
   - interval (millisecond)
   - fastestInterval (millisecond)
   - showLocationDialog (whether to ask to enable location in Android)

#### `clearWatch(watchId)`
 - watchId (id returned by `watchPosition`)

Checkout [React Native documentation](https://facebook.github.io/react-native/docs/geolocation.html#reference) to see the list of available methods.


# Error Codes
| Name | Code | Description |
| --- | --- | --- |
| PERMISSION_DENIED | 1 | Location permission is not granted |
| POSITION_UNAVAILABLE | 2 | Unable to determine position (not used yet) |
| TIMEOUT | 3 | Location request timed out |
| PLAY_SERVICE_NOT_AVAILABLE | 4 | Google play service is not installed or has an older version |
| SETTINGS_NOT_SATISFIED | 5 | Location service is not enabled or location mode is not appropriate for the current request |
| INTERNAL_ERROR | -1 | Library crashed for some reason or the `getCurrentActivity()` returned null |

# TODO
- [x] Implement `watchPosition` & `clearWatch` methods for android
- [x] Implement `stopObserving` method for android

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
    - Xiomi (MIUI OS) If you're running Android 6 or higher: Access your phone settings > additional settings > battery and performance > manage battery usage > apps > your app
