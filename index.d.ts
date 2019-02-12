declare module 'react-native-geolocation-service' {
  interface GeoOptions {
    timeout?: number
    maximumAge?: number
    enableHighAccuracy?: boolean
    distanceFilter?: number
    showLocationDialog?: boolean
  }

  interface GeoWatchOptions {
    timeout?: number
    maximumAge?: number
    enableHighAccuracy?: boolean
    distanceFilter?: number
    useSignificantChanges?: boolean
    interval?: number
    fastestInterval?: number
    showLocationDialog?: boolean
  }

  export interface GeoError {
    code: number
    message: string
  }

  export interface GeoCoordinates {
    latitude?: number
    longitude?: number
    accuracy?: number
    altitude?: number
    heading?: number
    speed?: number
    altitudeAccuracy?: number
  }

  export interface GeoPosition {
    coords: GeoCoordinates
    timestamp: Date
  }

  export interface GeoConfig {
    skipPermissionRequests?: boolean
  }

  export function setRNConfiguration(config: GeoConfig): void
  export function requestAuthorization(): void

  export function getCurrentPosition(
    successCallback: SuccessCallback,
    errorCallback?: ErrorCallback,
    options?: GeoOptions
  ): Promise<GeoPosition>

  export function watchPosition(
    successCallback: SuccessCallback,
    errorCallback?: ErrorCallback,
    options?: GeoWatchOptions
  ): Promise<GeoPosition>

  type SuccessCallback = (position: GeoPosition) => void
  type ErrorCallback = (error: GeoError) => void

  export function clearWatch(watchID: number): void
  export function stopObserving(): void
}

