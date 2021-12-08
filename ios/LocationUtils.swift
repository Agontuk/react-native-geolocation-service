import CoreLocation

enum LocationError: Int {
  case PERMISSION_DENIED = 1
  case POSITION_UNAVAILABLE
  case TIMEOUT
}

enum PermissionStatus: String {
  case disabled, granted, denied, restricted
}

struct LocationRequest {
  let onSuccess: RCTResponseSenderBlock
  let onError: RCTResponseSenderBlock

  init(_ successHandler: @escaping RCTResponseSenderBlock, _ errorHandler: @escaping RCTResponseSenderBlock) {
    self.onSuccess = successHandler
    self.onError = errorHandler
  }
}

func toPermissionStatus(_ status: CLAuthorizationStatus) -> PermissionStatus {
  var permissionStatus = PermissionStatus.denied

  switch status {
    case .authorizedAlways, .authorizedWhenInUse:
      permissionStatus = PermissionStatus.granted
    case .restricted:
      permissionStatus = PermissionStatus.restricted
    default:
      break
  }

  return permissionStatus
}

func locationToDict(_ location: CLLocation) -> [String: Any] {
  return [
    "coords": [
      "latitude": location.coordinate.latitude,
      "longitude": location.coordinate.longitude,
      "altitude": location.altitude,
      "accuracy": location.horizontalAccuracy,
      "altitudeAccuracy": location.verticalAccuracy,
      "heading": location.course,
      "speed": location.speed
    ],
    "timestamp": location.timestamp.timeIntervalSince1970 * 1000 // ms
  ]
}

func buildError(_ err: LocationError, _ message: String) -> [String: Any] {
  var msg: String = message

  if msg.isEmpty {
    switch err {
      case .PERMISSION_DENIED:
        msg = "Location permission denied"
      case .POSITION_UNAVAILABLE:
        msg = "Unable to retrieve location due to a network failure"
      case .TIMEOUT:
        msg = "Location request timed out"
    }
  }

  return [
    "code": err.rawValue,
    "message": msg
  ]
}

func checkPlistKeys(_ authorizationLevel: String) -> Void {
  #if DEBUG
    let key1 = Bundle.main.object(forInfoDictionaryKey: "NSLocationWhenInUseUsageDescription")
    let key2 = Bundle.main.object(forInfoDictionaryKey: "NSLocationAlwaysUsageDescription")
    let key3 = Bundle.main.object(forInfoDictionaryKey: "NSLocationAlwaysAndWhenInUseUsageDescription")

    switch authorizationLevel {
      case "whenInUse":
        if key1 == nil {
          RCTMakeAndLogError(
            "NSLocationWhenInUseUsageDescription key must be present in Info.plist",
            nil,
            nil
          )
        }
      case "always":
        if key1 == nil || key2 == nil || key3 == nil {
          RCTMakeAndLogError(
            "NSLocationWhenInUseUsageDescription, NSLocationAlwaysUsageDescription & NSLocationAlwaysAndWhenInUseUsageDescription key must be present in Info.plist",
            nil,
            nil
          )
        }
      default:
        RCTMakeAndLogError("Invalid authorization level provided", nil, nil)
    }
  #endif
}
