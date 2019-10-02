declare module 'react-native-geolocation-service' {
  interface GeoOptions {
    timeout?: number
    maximumAge?: number
    enableHighAccuracy?: boolean
    distanceFilter?: number
    useSignificantChanges?: boolean
    showLocationDialog?: boolean,
    forceRequestLocation?: boolean
  }

  interface GeoWatchOptions {
    timeout?: number
    maximumAge?: number
    enableHighAccuracy?: boolean
    distanceFilter?: number
    useSignificantChanges?: boolean
    interval?: number
    fastestInterval?: number
    showLocationDialog?: boolean,
    forceRequestLocation?: boolean
  }

  export enum PositionError {
    PERMISSION_DENIED = 1,
    POSITION_UNAVAILABLE = 2,
    TIMEOUT = 3,
    PLAY_SERVICE_NOT_AVAILABLE = 4,
    SETTINGS_NOT_SATISFIED = 5,
    INTERNAL_ERROR = -1
  }

  export interface GeoError {
    code: PositionError
    message: string
  }

  export interface GeoCoordinates {
    latitude: number
    longitude: number
    accuracy: number
    altitude: number | null
    heading: number | null
    speed: number | null
    altitudeAccuracy: number | null
  }

  export interface GeoPosition {
    coords: GeoCoordinates
    timestamp: number
    mocked?: boolean;
  }

  export interface GeoConfig {
    skipPermissionRequests: boolean
    authorizationLevel: 'always' | 'whenInUse' | 'auto'
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
  ): number

  type SuccessCallback = (position: GeoPosition) => void
  type ErrorCallback = (error: GeoError) => void

  export function clearWatch(watchID: number): void
  export function stopObserving(): void
}

