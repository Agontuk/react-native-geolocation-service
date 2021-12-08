import Foundation
import CoreLocation

@objc(RNFusedLocation)
class RNFusedLocation: RCTEventEmitter {
  private var continuousLocationProvider: LocationProvider? = nil
  private var permissionProvider: LocationProvider? = nil
  private var permissionHandler: RCTPromiseResolveBlock? = nil
  private var pendingRequests: [LocationProvider: LocationRequest] = [:]
  private var hasListeners: Bool = false

  deinit {
    continuousLocationProvider = nil
    permissionProvider = nil
    permissionHandler = nil
    pendingRequests.removeAll()
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

    let currentStatus = CLLocationManager.authorizationStatus()

    if currentStatus != .notDetermined {
      resolve(toPermissionStatus(currentStatus).rawValue)
      return
    }

    if permissionProvider == nil {
      permissionProvider = LocationProvider()
      permissionProvider!.delegate = self
    }

    permissionHandler = resolve
    permissionProvider!.requestPermission(level)
  }

  // MARK: Bridge Method
  @objc func getCurrentPosition(
    _ options: [String: Any],
    successCallback: @escaping RCTResponseSenderBlock,
    errorCallback: @escaping RCTResponseSenderBlock
  ) -> Void {
    let locationOptions = LocationOptions(options)
    let locationProvider = LocationProvider()
    locationProvider.delegate = self

    pendingRequests[locationProvider] = LocationRequest(successCallback, errorCallback)
    locationProvider.getCurrentLocation(locationOptions)
  }

  // MARK: Bridge Method
  @objc func startLocationUpdate(_ options: [String: Any]) -> Void {
    let locationOptions = LocationOptions(options)

    if continuousLocationProvider == nil {
      continuousLocationProvider = LocationProvider()
      continuousLocationProvider!.delegate = self
    }

    continuousLocationProvider!.requestLocationUpdates(locationOptions)
  }

  // MARK: Bridge Method
  @objc func stopLocationUpdate() -> Void {
    continuousLocationProvider?.removeLocationUpdates()
    continuousLocationProvider?.delegate = nil
    continuousLocationProvider = nil
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

extension RNFusedLocation: LocationProviderDelegate {
  func onPermissionChange(_ provider: LocationProvider, status: CLAuthorizationStatus) {
    permissionHandler?(toPermissionStatus(status).rawValue)
    permissionHandler = nil
    permissionProvider?.delegate = nil
    permissionProvider = nil
  }

  func onLocationChange(_ provider: LocationProvider, location: CLLocation) {
    let locationData = locationToDict(location)

    if provider == continuousLocationProvider && hasListeners {
      sendEvent(withName: "geolocationDidChange", body: locationData)
      return
    }

    guard let locationRequest: LocationRequest = pendingRequests[provider] else { return }
    locationRequest.onSuccess([locationData])
    pendingRequests.removeValue(forKey: provider)
  }

  func onLocationError(_ provider: LocationProvider, err: LocationError, message: String?) {
    let errData = buildError(err, message ?? "")

    if provider == continuousLocationProvider && hasListeners {
      sendEvent(withName: "geolocationError", body: errData)
      return
    }

    guard let locationRequest: LocationRequest = pendingRequests[provider] else { return }
    locationRequest.onError([errData])
    pendingRequests.removeValue(forKey: provider)
  }
}
