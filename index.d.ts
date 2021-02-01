declare module 'react-native-geolocation-service' {
  export type AuthorizationLevel = 'always' | 'whenInUse'

  export type AuthorizationResult = 'disabled' | 'granted' | 'denied' | 'restricted'

  export type AccuracyIOS =
    | 'bestForNavigation'
    | 'best'
    | 'nearestTenMeters'
    | 'hundredMeters'
    | 'kilometer'
    | 'threeKilometers'
    | 'reduced';

  export type AccuracyAndroid =
    | 'high'
    | 'balanced'
    | 'low'
    | 'passive';

  interface BaseOptions {
    accuracy?: {
      android?: AccuracyAndroid;
      ios?: AccuracyIOS;
    };
    enableHighAccuracy?: boolean
    distanceFilter?: number
    showLocationDialog?: boolean
    forceRequestLocation?: boolean
  }

  interface GeoOptions extends BaseOptions {
    timeout?: number
    maximumAge?: number
  }

  interface GeoWatchOptions extends BaseOptions {
    interval?: number
    fastestInterval?: number
    useSignificantChanges?: boolean
    showsBackgroundLocationIndicator?: boolean
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

  type SuccessCallback = (position: GeoPosition) => void

  type ErrorCallback = (error: GeoError) => void

  export function requestAuthorization(
    authorizationLevel: AuthorizationLevel
  ): Promise<AuthorizationResult>

  export function getCurrentPosition(
    successCallback: SuccessCallback,
    errorCallback?: ErrorCallback,
    options?: GeoOptions
  ): void

  export function watchPosition(
    successCallback: SuccessCallback,
    errorCallback?: ErrorCallback,
    options?: GeoWatchOptions
  ): number

  export function clearWatch(watchID: number): void

  export function stopObserving(): void
}

