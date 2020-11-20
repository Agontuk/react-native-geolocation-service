import Foundation
import CoreLocation

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
  private var lastLocation: [String: Any] = [:]
  private var observing: Bool = false
  private var timeoutTimer: Timer? = nil
  private var useSignificantChanges: Bool = false
  private var resolveAuthorizationStatus: RCTPromiseResolveBlock? = nil
  private var successCallback: RCTResponseSenderBlock? = nil
  private var errorCallback: RCTResponseSenderBlock? = nil

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

    timeoutTimer?.invalidate()

    locationManager.delegate = nil;
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

    if level == "whenInUse" {
      locationManager.requestWhenInUseAuthorization()
    } else if level == "always" {
      locationManager.requestAlwaysAuthorization()
    }
  }

  // MARK: Bridge Method
  @objc func getCurrentPosition(
    _ options: [String: Any],
    successCallback: @escaping RCTResponseSenderBlock,
    errorCallback: @escaping RCTResponseSenderBlock
  ) -> Void {
    let distanceFilter = options["distanceFilter"] as? Double ?? kCLDistanceFilterNone
    let maximumAge = options["maximumAge"] as? Double ?? Double.infinity
    let timeout = options["timeout"] as? Double ?? Double.infinity

    if !lastLocation.isEmpty {
      let elapsedTime = (Date().timeIntervalSince1970 * 1000) - (lastLocation["timestamp"] as! Double)

      if elapsedTime < maximumAge {
        // Return cached location
        successCallback([lastLocation])
        return
      }
    }

    let locManager = CLLocationManager()
    locManager.delegate = self
    locManager.desiredAccuracy = getAccuracy(options)
    locManager.distanceFilter = distanceFilter
    locManager.requestLocation()

    self.successCallback = successCallback
    self.errorCallback = errorCallback

    if timeout > 0 && timeout != Double.infinity {
      timeoutTimer = Timer.scheduledTimer(
        timeInterval: timeout / 1000.0, // timeInterval is in seconds
        target: self,
        selector: #selector(timerFired),
        userInfo: [
          "errorCallback": errorCallback,
          "manager": locManager
        ],
        repeats: false
      )
    }
  }

  // MARK: Bridge Method
  @objc func startLocationUpdate(_ options: [String: Any]) -> Void {
    let distanceFilter = options["distanceFilter"] as? Double ?? DEFAULT_DISTANCE_FILTER
    let significantChanges = options["useSignificantChanges"] as? Bool ?? false

    locationManager.desiredAccuracy = getAccuracy(options)
    locationManager.distanceFilter = distanceFilter
    locationManager.allowsBackgroundLocationUpdates = shouldAllowBackgroundUpdate()
    locationManager.pausesLocationUpdatesAutomatically = false

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

  @objc func timerFired(timer: Timer) -> Void {
    let data = timer.userInfo as! [String: Any]
    let errorCallback = data["errorCallback"] as! RCTResponseSenderBlock
    let manager = data["manager"] as! CLLocationManager

    manager.stopUpdatingLocation()
    manager.delegate = nil
    errorCallback([generateErrorResponse(code: LocationError.TIMEOUT.rawValue)])
  }

  private func checkPlistKeys(authorizationLevel: String) -> Void {
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

  private func getAccuracy(_ options: [String: Any]) -> Double {
    let accuracyDict = options["accuracy"] as? [String: String] ?? [:]
    let accuracyLevel = accuracyDict["ios"] ?? ""
    let highAccuracy = options["enableHighAccuracy"] as? Bool ?? false

    switch accuracyLevel {
      case "bestForNavigation":
        return kCLLocationAccuracyBestForNavigation
      case "best":
        return kCLLocationAccuracyBest
      case "nearestTenMeters":
        return kCLLocationAccuracyNearestTenMeters
      case "hundredMeters":
        return kCLLocationAccuracyHundredMeters
      case "kilometer":
        return kCLLocationAccuracyKilometer
      case "threeKilometers":
        return kCLLocationAccuracyThreeKilometers
      default:
        return highAccuracy ? kCLLocationAccuracyBest : kCLLocationAccuracyHundredMeters
    }
  }

  private func shouldAllowBackgroundUpdate() -> Bool {
    let info = Bundle.main.object(forInfoDictionaryKey: "UIBackgroundModes") as? [String] ?? []

    if info.contains("location") {
      return true
    }

    return false
  }

  private func generateErrorResponse(code: Int, message: String = "") -> [String: Any] {
    var msg: String = message

    if msg.isEmpty {
      switch code {
        case LocationError.PERMISSION_DENIED.rawValue:
          msg = "Location permission denied"
        case LocationError.POSITION_UNAVAILABLE.rawValue:
          msg = "Unable to retrieve location due to a network failure"
        case LocationError.TIMEOUT.rawValue:
          msg = "Location request timed out"
        default:
          break
      }
    }

    return [
      "code": code,
      "message": msg
    ]
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

    if manager.isEqual(locationManager) && hasListeners && observing {
      sendEvent(withName: "geolocationDidChange", body: locationData)
      return
    }

    guard successCallback != nil else { return }

    lastLocation = locationData
    successCallback!([locationData])

    // Cleanup
    timeoutTimer?.invalidate()
    successCallback = nil
    errorCallback = nil
    manager.delegate = nil
  }

  func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    var errorData: [String: Any] = generateErrorResponse(
      code: LocationError.POSITION_UNAVAILABLE.rawValue,
      message: "Unable to retrieve location"
    )

    if let clErr = error as? CLError {
      switch clErr.code {
        case CLError.denied:
          if !CLLocationManager.locationServicesEnabled() {
            errorData = generateErrorResponse(
              code: LocationError.POSITION_UNAVAILABLE.rawValue,
              message: "Location service is turned off"
            )
          } else {
            errorData = generateErrorResponse(code: LocationError.PERMISSION_DENIED.rawValue)
          }
        case CLError.network:
          errorData = generateErrorResponse(code: LocationError.POSITION_UNAVAILABLE.rawValue)
        default:
          break
      }
    }

    if manager.isEqual(locationManager) && hasListeners && observing {
      sendEvent(withName: "geolocationError", body: errorData)
      return
    }

    guard errorCallback != nil else { return }

    errorCallback!([errorData])

    // Cleanup
    timeoutTimer?.invalidate()
    successCallback = nil
    errorCallback = nil
    manager.delegate = nil
  }
}
