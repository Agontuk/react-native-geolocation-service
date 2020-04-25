import Geolocation from './Geolocation';

export const PositionError = Object.freeze({
  PERMISSION_DENIED: 1,
  POSITION_UNAVAILABLE: 2,
  TIMEOUT: 3,
  PLAY_SERVICE_NOT_AVAILABLE: 4,
  SETTINGS_NOT_SATISFIED: 5,
  INTERNAL_ERROR: -1
});

export default Geolocation;
