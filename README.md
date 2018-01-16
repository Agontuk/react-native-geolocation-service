# react-native-geolocation
React native geolocation service for iOS and android.

# Why ?
This library is created in an attempt to fix the location timeout issue on android with the react-native's current implementation of Geolocation API. This library tries to solve the issue using Google Play Service's new `FusedLocationProviderClient` API, which Google strongly recommends over android's default framework location API.

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
