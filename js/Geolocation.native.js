import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

const { RNFusedLocation } = NativeModules;
const LocationEventEmitter = new NativeEventEmitter(RNFusedLocation);

const noop = () => {};
let subscriptions = [];
let updatesEnabled = false;

const Geolocation = {
  setRNConfiguration: (config) => {}, // eslint-disable-line no-unused-vars

  requestAuthorization: async (authorizationLevel) => {
    if (Platform.OS !== 'ios') {
      return Promise.reject('requestAuthorization is only for iOS');
    }

    if (!authorizationLevel) {
      // eslint-disable-next-line no-console
      console.error('authorizationLevel must be provided');
    }

    return RNFusedLocation.requestAuthorization(authorizationLevel);
  },

  getCurrentPosition: (success, error = noop, options = {}) => {
    if (!success) {
      // eslint-disable-next-line no-console
      console.error('Must provide a success callback');
    }

    // Right now, we're assuming user already granted location permission.
    RNFusedLocation.getCurrentPosition(options, success, error);
  },

  watchPosition: (success, error = null, options = {}) => {
    if (!success) {
      // eslint-disable-next-line no-console
      console.error('Must provide a success callback');
    }

    if (!updatesEnabled) {
      RNFusedLocation.startObserving(options);
      updatesEnabled = true;
    }

    const watchID = subscriptions.length;

    subscriptions.push([
      LocationEventEmitter.addListener('geolocationDidChange', success),
      error ? LocationEventEmitter.addListener('geolocationError', error) : null
    ]);

    return watchID;
  },

  clearWatch: (watchID) => {
    const sub = subscriptions[watchID];

    if (!sub) {
      // Silently exit when the watchID is invalid or already cleared
      // This is consistent with timers
      return;
    }

    sub[0].remove();

    const sub1 = sub[1];

    if (sub1) {
      sub1.remove();
    }

    subscriptions[watchID] = undefined;

    let noWatchers = true;

    for (let ii = 0; ii < subscriptions.length; ii += 1) {
      if (subscriptions[ii]) {
        noWatchers = false; // still valid subscriptions
      }
    }

    if (noWatchers) {
      Geolocation.stopObserving();
    }
  },

  stopObserving: () => {
    if (updatesEnabled) {
      RNFusedLocation.stopObserving();
      updatesEnabled = false;

      for (let ii = 0; ii < subscriptions.length; ii += 1) {
        const sub = subscriptions[ii];
        if (sub) {
          // eslint-disable-next-line no-console
          console.warn('Called stopObserving with existing subscriptions.');
          sub[0].remove();

          const sub1 = sub[1];

          if (sub1) {
            sub1.remove();
          }
        }
      }

      subscriptions = [];
    }
  }
};

export default Geolocation;
