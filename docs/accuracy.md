# Accuracy Level

## Android
[Source](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest#constants)

| Name | Description |
| -- | -- |
| high | This will return the finest location available. |
| balanced | Block level accuracy considered to be about 100 meter accuracy. |
| low | City level accuracy is considered to be about 10km accuracy. |
| passive | No locations will be returned unless a different client has requested location updates in which case this request will act as a passive listener to those locations. |

## iOS
[Source](https://developer.apple.com/documentation/corelocation/cllocationaccuracy)

| Name | Description |
| -- | -- |
| bestForNavigation | The highest possible accuracy that uses additional sensor data to facilitate navigation apps. |
| best | The best level of accuracy available. |
| nearestTenMeters | Accurate to within ten meters of the desired target. |
| hundredMeters | Accurate to within one hundred meters. |
| kilometer | Accurate to the nearest kilometer. |
| threeKilometers | Accurate to the nearest three kilometers. |
| reduced | Used when an app does not need accurate location data. |
