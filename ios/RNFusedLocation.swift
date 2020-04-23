import Foundation
import CoreLocation

enum AuthorizationStatus: String {
  case disabled, granted, denied, restricted
}

@objc(RNFusedLocation)
class RNFusedLocation: RCTEventEmitter {
  private let locationManager: CLLocationManager = CLLocationManager()
  private var resolveAuthorizationStatus: RCTPromiseResolveBlock? = nil

  override init() {
    super.init()
    locationManager.delegate = self
  }

  deinit {
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
    NSLog("startLocationUpdate")
  }

  // MARK: Bridge Method
  @objc func stopLocationUpdate() -> Void {
    NSLog("stopLocationUpdate")
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
}
