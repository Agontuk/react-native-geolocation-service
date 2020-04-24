import Foundation
import CoreLocation

let DEFAULT_ACCURACY: CLLocationAccuracy = kCLLocationAccuracyHundredMeters
let DEFAULT_DISTANCE_FILTER: CLLocationDistance = 100

enum LocationError: Int {
  case PERMISSION_DENIED = 1
  case POSITION_UNAVAILABLE
  case TIMEOUT
}

enum AuthorizationStatus: String {
  case disabled, granted, denied, restricted
}

@objc(RNFusedLocation)
class RNFusedLocation: RCTEventEmitter {
  private let locationManager: CLLocationManager = CLLocationManager()
  private var hasListeners: Bool = false
  private var observing: Bool = false
  private var resolveAuthorizationStatus: RCTPromiseResolveBlock? = nil
  private var useSignificantChanges: Bool = false

  override init() {
    super.init()
    locationManager.delegate = self
  }

  deinit {
    if observing {
      useSignificantChanges
        ? locationManager.stopMonitoringSignificantLocationChanges()
        : locationManager.stopUpdatingLocation()

      observing = false
    }

    locationManager.delegate = nil;
  }

  // MARK: RCTBridgeModule
  override static func requiresMainQueueSetup() -> Bool {
    return false
  }

  // MARK: RCTEventEmitter
  override func supportedEvents() -> [String]! {
    return ["geolocationDidChange", "geolocationError"]
  }

  // MARK: RCTEventEmitter
  override func startObserving() -> Void {
    hasListeners = true
  }

  // MARK: RCTEventEmitter
  override func stopObserving() -> Void {
    hasListeners = false
  }

  // MARK: Bridge Method
  @objc func requestAuthorization(
    _ level: String,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) -> Void {
    checkPlistKeys(authorizationLevel: level)

    if !CLLocationManager.locationServicesEnabled() {
      resolve(AuthorizationStatus.disabled.rawValue)
      return
    }

    switch CLLocationManager.authorizationStatus() {
      case .authorizedWhenInUse, .authorizedAlways:
        resolve(AuthorizationStatus.granted.rawValue)
        return
      case .denied:
        resolve(AuthorizationStatus.denied.rawValue)
        return
      case .restricted:
        resolve(AuthorizationStatus.restricted.rawValue)
        return
      default:
        break
    }

    resolveAuthorizationStatus = resolve

    if level == "whileInUse" {
      locationManager.requestWhenInUseAuthorization()
    } else if level == "always" {
      locationManager.requestAlwaysAuthorization()
    }
  }

  // MARK: Bridge Method
  @objc func getCurrentPosition(
    _ options: [String: Any],
    successCallback: RCTResponseSenderBlock,
    errorCallback: RCTResponseSenderBlock
  ) -> Void {
    NSLog("getCurrentPosition")
  }

  // MARK: Bridge Method
  @objc func startLocationUpdate(_ options: [String: Any]) -> Void {
    let distanceFilter = options["distanceFilter"] as? Double ?? DEFAULT_DISTANCE_FILTER
    let highAccuracy = options["enableHighAccuracy"] as? Bool ?? false
    let significantChanges = options["useSignificantChanges"] as? Bool ?? false

    locationManager.desiredAccuracy = highAccuracy ? kCLLocationAccuracyBest : DEFAULT_ACCURACY
    locationManager.distanceFilter = distanceFilter

    significantChanges
      ? locationManager.startMonitoringSignificantLocationChanges()
      : locationManager.startUpdatingLocation()

    useSignificantChanges = significantChanges
    observing = true
  }

  // MARK: Bridge Method
  @objc func stopLocationUpdate() -> Void {
    useSignificantChanges
      ? locationManager.stopMonitoringSignificantLocationChanges()
      : locationManager.stopUpdatingLocation()

    observing = false
  }

  private func checkPlistKeys(authorizationLevel: String) -> Void {
    #if DEBUG
      let key1 = Bundle.main.object(forInfoDictionaryKey: "NSLocationWhenInUseUsageDescription")
      let key2 = Bundle.main.object(forInfoDictionaryKey: "NSLocationAlwaysUsageDescription")
      let key3 = Bundle.main.object(forInfoDictionaryKey: "NSLocationAlwaysAndWhenInUseUsageDescription")

      switch authorizationLevel {
        case "whileInUse":
          if key1 == nil {
            NSLog("NSLocationWhenInUseUsageDescription key must be present in Info.plist")
          }
        case "always":
          if key1 == nil || key2 == nil || key3 == nil {
            NSLog("NSLocationWhenInUseUsageDescription, NSLocationAlwaysUsageDescription & NSLocationAlwaysAndWhenInUseUsageDescription key must be present in Info.plist")
          }
        default:
          NSLog("Invalid authorization level provided")
      }
    #endif
  }
}

extension RNFusedLocation: CLLocationManagerDelegate {
  func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
    if status == .notDetermined || resolveAuthorizationStatus == nil {
      return
    }

    switch status {
      case .authorizedWhenInUse, .authorizedAlways:
        resolveAuthorizationStatus?(AuthorizationStatus.granted.rawValue)
      case .denied:
        resolveAuthorizationStatus?(AuthorizationStatus.denied.rawValue)
      case .restricted:
        resolveAuthorizationStatus?(AuthorizationStatus.restricted.rawValue)
      default:
        break
    }

    resolveAuthorizationStatus = nil
  }

  func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    guard let location: CLLocation = locations.last else { return }
    let locationData: [String: Any] = [
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

    if hasListeners && observing {
      sendEvent(withName: "geolocationDidChange", body: locationData)
    }
  }

  func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    let errorData: [String: Any]

    if let clErr = error as? CLError {
      switch clErr.code {
        case CLError.denied:
          if !CLLocationManager.locationServicesEnabled() {
            errorData = [
              "code": LocationError.POSITION_UNAVAILABLE.rawValue,
              "message": "Location service is turned off"
            ]
          } else {
            errorData = [
              "code": LocationError.PERMISSION_DENIED.rawValue,
              "message": "Location permission denied"
            ]
          }
        case CLError.network:
          errorData = [
            "code": LocationError.POSITION_UNAVAILABLE.rawValue,
            "message": "Unable to retrieve location due to a network failure"
          ]
        default:
          errorData = [
            "code": LocationError.POSITION_UNAVAILABLE.rawValue,
            "message": "Unable to retrieve location"
          ]
      }
    } else {
      errorData = [
        "code": LocationError.POSITION_UNAVAILABLE.rawValue,
        "message": "Unable to retrieve location"
      ]
    }

    if hasListeners && observing {
      sendEvent(withName: "geolocationError", body: errorData)
    }
  }
}
