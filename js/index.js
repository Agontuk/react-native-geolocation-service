import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

// eslint-disable-next-line import/no-mutable-exports
let Geolocation = null;

const noop = () => {};
let subscriptions = [];
let updatesEnabled = false;

export const PositionError = Object.freeze({
    PERMISSION_DENIED: 1,
    POSITION_UNAVAILABLE: 2,
    TIMEOUT: 3,
    PLAY_SERVICE_NOT_AVAILABLE: 4,
    SETTINGS_NOT_SATISFIED: 5,
    INTERNAL_ERROR: -1
});

if (Platform.OS === 'ios') {
    // eslint-disable-next-line global-require
    Geolocation = require('@react-native-community/geolocation');

    const { getCurrentPosition: iosGetCurrentPosition } = Geolocation;

    const currentPosition = (configOptions = {}) =>
    new Promise((resolve, reject) => {
        iosGetCurrentPosition(
            configOptions,
            position => resolve(position),
            error => reject(error)
        );
    }),

    const getCurrentPosition = function(
        param1,
        errorCallback = noop,
        options = {}
    ) {
        if (arguments.length === 1 && typeof arguments[0] === 'object') {
            return currentPosition(arguments[0]);
        } else if (arguments.length && typeof arguments[0] === 'function') {
            const successCallback = param1;
            // Right now, we're assuming user already granted location permission.
            iosGetCurrentPosition(options, successCallback, errorCallback);
        } else {
            return currentPosition();
        }
    };

    Geolocation.currentPosition = currentPosition;
    Geolocation.getCurrentPosition = getCurrentPosition;
} else if (Platform.OS === 'android') {
    const { RNFusedLocation } = NativeModules;
    const LocationEventEmitter = new NativeEventEmitter(RNFusedLocation);

    Geolocation = {
        setRNConfiguration: config => {}, // eslint-disable-line no-unused-vars

        requestAuthorization: () => {},

        currentPosition: (configOptions = {}) =>
            new Promise((resolve, reject) => {
                RNFusedLocation.getCurrentPosition(
                    configOptions,
                    position => resolve(position),
                    error => reject(error)
                );
            }),

        getCurrentPosition: function(
            param1,
            errorCallback = noop,
            options = {}
        ) {
            if (arguments.length === 1 && typeof arguments[0] === 'object') {
                return this.currentPosition(arguments[0]);
            } else if (arguments.length && typeof arguments[0] === 'function') {
                const successCallback = param1;
                // Right now, we're assuming user already granted location permission.
                RNFusedLocation.getCurrentPosition(
                    options,
                    successCallback,
                    errorCallback
                );
            } else {
                return this.currentPosition();
            }
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
                LocationEventEmitter.addListener(
                    'geolocationDidChange',
                    success
                ),
                error
                    ? LocationEventEmitter.addListener(
                          'geolocationError',
                          error
                      )
                    : null
            ]);

            return watchID;
        },

        clearWatch: watchID => {
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
                        console.warn(
                            'Called stopObserving with existing subscriptions.'
                        );
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
}

export default Geolocation;
