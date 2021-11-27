import Foundation
import CoreLocation

@objc(RNFusedLocation)
class RNFusedLocation: RCTEventEmitter {
  private let locationProvider: LocationProvider
  private var permissionProvider: LocationProvider? = nil
  private var hasListeners: Bool = false

  override init() {
    locationProvider = LocationProvider()
    super.init()
  }

  // MARK: Bridge Method
  @objc func requestAuthorization(
    _ level: String,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) -> Void {
    if !CLLocationManager.locationServicesEnabled() {
      resolve(PermissionStatus.disabled.rawValue)
      return
    }

    if permissionProvider == nil {
      permissionProvider = LocationProvider()
    }

    permissionProvider!.requestPermission(level, handler: { [self] (status) -> Void in
      var permissionStatus = PermissionStatus.denied

      switch status {
        case .authorizedAlways, .authorizedWhenInUse:
          permissionStatus = PermissionStatus.granted
        case .restricted:
          permissionStatus = PermissionStatus.restricted
        default:
          break
      }

      resolve(permissionStatus.rawValue)
      permissionProvider = nil
    })
  }

  // MARK: Bridge Method
  @objc func getCurrentPosition(
    _ options: [String: Any],
    successCallback: @escaping RCTResponseSenderBlock,
    errorCallback: @escaping RCTResponseSenderBlock
  ) -> Void {
    let locationOptions = LocationOptions(options)

    locationProvider.getCurrentLocation(
      locationOptions,
      successHandler: { (location) -> Void in
        successCallback([locationToDict(location)])
      },
      errorHandler: { (err, message) -> Void in
        errorCallback([buildError(err, message ?? "")])
      }
    )
  }

  // MARK: Bridge Method
  @objc func startLocationUpdate(_ options: [String: Any]) -> Void {
    let locationOptions = LocationOptions(options)

    locationProvider.requestLocationUpdates(
      locationOptions,
      successHandler: { [self] (location) -> Void in
        if (hasListeners) {
          sendEvent(withName: "geolocationDidChange", body: locationToDict(location))
        }
      },
      errorHandler: { [self] (err, message) -> Void in
        if (hasListeners) {
          sendEvent(withName: "geolocationError", body: buildError(err, message ?? ""))
        }
      }
    )
  }

  // MARK: Bridge Method
  @objc func stopLocationUpdate() -> Void {
    locationProvider.removeLocationUpdates()
  }
}

// MARK: RCTBridgeModule, RCTEventEmitter overrides
extension RNFusedLocation {
  override var methodQueue: DispatchQueue {
    get {
      return DispatchQueue.main
    }
  }

  override static func requiresMainQueueSetup() -> Bool {
    return false
  }

  override func supportedEvents() -> [String]! {
    return ["geolocationDidChange", "geolocationError"]
  }

  override func startObserving() -> Void {
    hasListeners = true
  }

  override func stopObserving() -> Void {
    hasListeners = false
  }
}
