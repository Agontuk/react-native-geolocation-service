import CoreLocation

typealias PermissionHandler = (_ status: CLAuthorizationStatus) -> Void
typealias SuccessHandler = (_ location: CLLocation) -> Void
typealias ErrorHandler = (_ err: LocationError, _ message: String?) -> Void

class LocationProvider: NSObject {
  private let locationManager: CLLocationManager
  private var locationOptions: LocationOptions? = nil
  private var isSingleUpdate: Bool = false
  private var onLocationChange: SuccessHandler? = nil
  private var onLocationError: ErrorHandler? = nil
  private var onPermissionChange: PermissionHandler? = nil
  private var timeoutTimer: Timer? = nil

  override init() {
    locationManager = CLLocationManager()
    super.init()
    locationManager.delegate = self
  }

  deinit {
    removeLocationUpdates()
    timeoutTimer?.invalidate()
    onLocationChange = nil
    onLocationError = nil
    onPermissionChange = nil
    locationManager.delegate = nil;
  }

  func requestPermission(_ level: String, handler: @escaping PermissionHandler) -> Void {
    checkPlistKeys(level)

    let currentStatus = CLLocationManager.authorizationStatus()

    if currentStatus != .notDetermined {
      handler(currentStatus)
      return
    }

    onPermissionChange = handler

    if level == "whenInUse" {
      locationManager.requestWhenInUseAuthorization()
    } else if level == "always" {
      locationManager.requestAlwaysAuthorization()
    }
  }

  func getCurrentLocation(
    _ options: LocationOptions,
    successHandler: @escaping SuccessHandler,
    errorHandler: @escaping ErrorHandler
  ) -> Void {
    if locationManager.location != nil {
      let elapsedTime = (Date().timeIntervalSince1970 - locationManager.location!.timestamp.timeIntervalSince1970) * 1000

      #if DEBUG
        NSLog("RNLocation: elapsedTime=\(elapsedTime)ms, maxAge=\(options.maximumAge)ms")
      #endif

      if elapsedTime < options.maximumAge {
        #if DEBUG
          NSLog("RNLocation: returning cached location")
        #endif
        successHandler(locationManager.location!)
        return
      }
    }

    isSingleUpdate = true
    locationOptions = options
    onLocationChange = successHandler
    onLocationError = errorHandler

    locationManager.desiredAccuracy = options.accuracy
    locationManager.distanceFilter = kCLDistanceFilterNone
    locationManager.requestLocation()

    let timeout = options.timeout

    if timeout > 0 && timeout != Double.infinity {
      timeoutTimer = Timer.scheduledTimer(
        timeInterval: timeout / 1000.0, // timeInterval is in seconds
        target: self,
        selector: #selector(timerFired),
        userInfo: nil,
        repeats: false
      )
    }
  }

  func requestLocationUpdates(
    _ options: LocationOptions,
    successHandler: @escaping SuccessHandler,
    errorHandler: @escaping ErrorHandler
  ) -> Void {
    isSingleUpdate = false
    locationOptions = options
    onLocationChange = successHandler
    onLocationError = errorHandler

    locationManager.desiredAccuracy = options.accuracy
    locationManager.distanceFilter = options.distanceFilter
    locationManager.allowsBackgroundLocationUpdates = options.backgroundUpdates
    locationManager.pausesLocationUpdatesAutomatically = options.pauseUpdatesAutomatically
    if #available(iOS 11.0, *) {
      locationManager.showsBackgroundLocationIndicator = options.backgroundIndicator
    }

    options.significantChanges
      ? locationManager.startMonitoringSignificantLocationChanges()
      : locationManager.startUpdatingLocation()
  }

  func removeLocationUpdates() -> Void {
    if locationOptions == nil {
      return
    }

    locationOptions!.significantChanges
      ? locationManager.stopMonitoringSignificantLocationChanges()
      : locationManager.stopUpdatingLocation()
  }

  @objc private func timerFired(timer: Timer) -> Void {
    #if DEBUG
      NSLog("RNLocation: request timed out")
    #endif

    onLocationError?(LocationError.TIMEOUT, nil)
    locationManager.stopUpdatingLocation()
  }
}

extension LocationProvider: CLLocationManagerDelegate {
  func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
    // When CLLocationManager instance is created, it'll trigger this method.
    // Status can be undetermined and permission handler will be nil in that case.
    if status == .notDetermined || onPermissionChange == nil {
      return
    }

    onPermissionChange!(status)
  }

  func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    guard let location: CLLocation = locations.last else { return }

    #if DEBUG
      NSLog("RNLocation: \(location.coordinate.latitude), \(location.coordinate.longitude)")
    #endif

    onLocationChange?(location)

    if (isSingleUpdate) {
      timeoutTimer?.invalidate()
      locationManager.stopUpdatingLocation()
    }
  }

  func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    var err = LocationError.POSITION_UNAVAILABLE
    var message: String? = nil

    if let clErr = error as? CLError {
      switch clErr.code {
        case CLError.denied:
          if !CLLocationManager.locationServicesEnabled() {
            message = "Location service is turned off"
          } else {
            err = LocationError.PERMISSION_DENIED
          }
        case CLError.network:
          message = "Unable to retrieve location due to a network failure"
        default:
          break
      }
    } else {
      NSLog("RNLocation: \(error.localizedDescription)")
    }

    onLocationError?(err, message)

    if (isSingleUpdate) {
      timeoutTimer?.invalidate()
      locationManager.stopUpdatingLocation()
    }
  }
}
