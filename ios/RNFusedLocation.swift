import Foundation
import CoreLocation

@objc(RNFusedLocation)
class RNFusedLocation: RCTEventEmitter {
  // MARK: RCTBridgeModule
  override static func requiresMainQueueSetup() -> Bool {
    return false
  }

  // MARK: RCTEventEmitter
  override func supportedEvents() -> [String]! {
    return ["geolocationDidChange", "geolocationError"]
  }

  // MARK: Bridge Method
  @objc func requestAuthorization(_ level: String) -> Void {
    NSLog("requestAuthorization")
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
}
