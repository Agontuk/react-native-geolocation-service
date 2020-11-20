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

![xcode-add](../screenshots/01-ios-add-to-library.png?raw=true)

#### Link `libRNFusedLocation.a` binary with libraries

Click on your main project file (the one that represents the `.xcodeproj`) select `Build Phases` and drag the static library from the `Products` folder inside the Library you are importing to `Link Binary With Libraries` (or use the `+` sign and choose library from the list):

![xcode-link](../screenshots/02-ios-add-to-build-phases.png?raw=true)
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
