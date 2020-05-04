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

## iOS
### 1) Linking
For RN 0.60 or higher, no manual linking is needed. After installing the package, just run `pod install` from inside ios directory. It'll automatically pickup the package and install it.

<details>
<summary>0.59 or below</summary>

### Using Cocoapods
- Update your `Podfile`
    ```
    pod 'react-native-geolocation-service', path: '../node_modules/react-native-geolocation-service'
    ```
 - Then run `pod install` from ios directory

### Manually linking

#### `Open project.xcodeproj in Xcode`

Drag `RNFusedLocation.xcodeproj` to your project on Xcode (usually under the Libraries group on Xcode):

![xcode-add](screenshots/01-ios-add-to-library.png?raw=true)

#### Link `libRNFusedLocation.a` binary with libraries

Click on your main project file (the one that represents the `.xcodeproj`) select `Build Phases` and drag the static library from the `Products` folder inside the Library you are importing to `Link Binary With Libraries` (or use the `+` sign and choose library from the list):

![xcode-link](screenshots/02-ios-add-to-build-phases.png?raw=true)
</details>

### 2) Enable Swift Support
Since the iOS implementation is written in swift, you need to add swift support in your project. It can be done just by adding an empty swift file and a bridging header in your project folder. You have to do it from xcode, otherwise swift compiler flag won't be updated.
- Select `File -> New -> File` from xcode
- Choose Swift file, name it anything
- Click `Next` and say yes when prompted if you’d like to generate a bridging header (important)

### 3) Update `info.plist`
There are three info.plist keys for location service
- NSLocationWhenInUseUsageDescription
- NSLocationAlwaysUsageDescription
- NSLocationAlwaysAndWhenInUseUsageDescription

Unless you need background location update, adding only the first key will be enough. To enable background location update, you need to add all the keys in `info.plist` and add location as a background mode in the `Signing & Capabilities -> Capability` tab in Xcode.

## Android

### 1) Linking
For RN 0.60 or higher, no manual linking is needed. You can override following gradle properties from your root build.gradle file.

```gradle
ext {
  compileSdkVersion   = 28
  buildToolsVersion   = "28.0.3"
  minSdkVersion       = 16
  targetSdkVersion    = 28

  // Any of the following will work
  googlePlayServicesVersion      = "17.0.0"
  // playServicesVersion         = "17.0.0"
  // playServicesLocationVersion = "17.0.0"
}
```

<details>
<summary>0.59 or below</summary>

1. In `android/app/build.gradle`

    ```gradle
    ...
    dependencies {
        ...
        implementation project(':react-native-geolocation-service')
    }
    ```

    If you've defined [project-wide properties](https://developer.android.com/studio/build/gradle-tips#configure-project-wide-properties) (recommended) in your root build.gradle, this library will detect the presence of the following properties:

    ```gradle
    buildscript {
        /**
         + Project-wide Gradle configuration properties
         */
        ext {
            compileSdkVersion   = 28
            minSdkVersion       = 16
            targetSdkVersion    = 28
            buildToolsVersion   = "28.0.3"
            googlePlayServicesVersion = "17.0.0"
        }
        repositories { ... }
        dependencies { ... }
    }
    ```

    If you do not have *project-wide properties* defined and have a different play-services version than the one included in this library, use the following instead. But play service version should be `11+` or the library won't work.

    ```gradle
    ...
    dependencies {
        ...
        implementation(project(':react-native-geolocation-service')) {
            exclude group: 'com.google.android.gms', module: 'play-services-location'
        }
        implementation 'com.google.android.gms:play-services-location:<insert your play service version here>'
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
</details>

### 2) Permissions
Add permission in your `AndroidManifest.xml` file based on your project requirement.
```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

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
    | enableHighAccuracy | `bool` | `false` | Use high accuracy mode
    | distanceFilter | `m` | `0` | Minimum displacement in meters
    | showLocationDialog | `bool` | `true` | Whether to ask to enable location in Android (android only)
    | forceRequestLocation | `bool` | `false` | Force request location even after denying improve accuracy dialog (android only)

#### `watchPosition(successCallback, ?errorCallback, ?options)`
 - **successCallback**: Invoked with latest location info.
 - **errorCallback**: Invoked whenever an error is encountered.
 - **options**:

    | Name | Type | Default | Description |
    | -- | -- | -- | -- |
    | enableHighAccuracy | `bool` | `false` | Use high accuracy mode
    | distanceFilter | `m` | `100` | Minimum displacement between location updates in meters
    | interval | `ms` | `10000` |  Interval for active location updates (android only)
    | fastestInterval | `ms` | `5000` | Fastest rate at which your application will receive location updates, which might be faster than `interval` in some situations (for example, if other applications are triggering location updates) (android only)
    | showLocationDialog | `bool` | `true` | whether to ask to enable location in Android (android only)
    | forceRequestLocation | `bool` | `false` | Force request location even after denying improve accuracy dialog (android only)
    | useSignificantChanges | `bool` | false | Uses the battery-efficient native significant changes APIs to return locations. Locations will only be returned when the device detects a significant distance has been breached (iOS only)

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
