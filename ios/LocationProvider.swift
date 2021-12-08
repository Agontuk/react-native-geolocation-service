import CoreLocation

protocol LocationProviderDelegate: AnyObject {
  func onPermissionChange(_ provider: LocationProvider, status: CLAuthorizationStatus)
  func onLocationChange(_ provider: LocationProvider, location: CLLocation)
  func onLocationError(_ provider: LocationProvider, err: LocationError, message: String?)
}

class LocationProvider: NSObject {
  private let locationManager: CLLocationManager
  private var locationOptions: LocationOptions? = nil
  private var isSingleUpdate: Bool = false
  private var timeoutTimer: Timer? = nil

  weak var delegate: LocationProviderDelegate?

  override init() {
    locationManager = CLLocationManager()
    super.init()
    locationManager.delegate = self
  }

  deinit {
    removeLocationUpdates()
    timeoutTimer?.invalidate()
    locationManager.delegate = nil;
  }

  func requestPermission(_ level: String) -> Void {
    checkPlistKeys(level)

    if level == "whenInUse" {
      locationManager.requestWhenInUseAuthorization()
    } else if level == "always" {
      locationManager.requestAlwaysAuthorization()
    }
  }

  func getCurrentLocation(_ options: LocationOptions) -> Void {
    if locationManager.location != nil {
      let elapsedTime = (Date().timeIntervalSince1970 - locationManager.location!.timestamp.timeIntervalSince1970) * 1000

      #if DEBUG
        NSLog("RNLocation: elapsedTime=\(elapsedTime)ms, maxAge=\(options.maximumAge)ms")
      #endif

      if elapsedTime < options.maximumAge {
        #if DEBUG
          NSLog("RNLocation: returning cached location")
        #endif
        delegate?.onLocationChange(self, location: locationManager.location!)
        return
      }
    }

    isSingleUpdate = true
    locationOptions = options

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

  func requestLocationUpdates(_ options: LocationOptions) -> Void {
    isSingleUpdate = false
    locationOptions = options

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

    delegate?.onLocationError(self, err: LocationError.TIMEOUT, message: nil)
    locationManager.stopUpdatingLocation()
  }
}

extension LocationProvider: CLLocationManagerDelegate {
  func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
    // When CLLocationManager instance is created, it'll trigger this method.
    // Status can be undetermined in that case.
    if status == .notDetermined {
      return
    }

    delegate?.onPermissionChange(self, status: status)
  }

  func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    guard let location: CLLocation = locations.last else { return }

    #if DEBUG
      NSLog("RNLocation: \(location.coordinate.latitude), \(location.coordinate.longitude)")
    #endif

    delegate?.onLocationChange(self, location: location)

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

    delegate?.onLocationError(self, err: err, message: message)

    if (isSingleUpdate) {
      timeoutTimer?.invalidate()
      locationManager.stopUpdatingLocation()
    }
  }
}
