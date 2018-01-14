import { NativeModules, Platform } from 'react-native';

const { RNFusedLocation } = NativeModules;

const noop = () => {};

// TODO: add static type checker
const Geolocation = Platform.OS === 'ios' ? global.navigator.geolocation : {
    setRNConfiguration: (config) => {}, // eslint-disable-line no-unused-vars

    requestAuthorization: () => {},

    getCurrentPosition: async (success, error = noop, options = {}) => {
        // Right now, we're assuming user already granted location permission.
        RNFusedLocation.getCurrentPosition(options, success, error);
    },

    watchPosition: (success, error = noop, options = {}) => { // eslint-disable-line no-unused-vars
        // eslint-disable-next-line no-console
        console.warn('watchPosition is not yet implemented');
    },

    clearWatch: (watchID) => { // eslint-disable-line no-unused-vars
        // eslint-disable-next-line no-console
        console.warn('clearWatch is not yet implemented');
    }
};

export default Geolocation;
