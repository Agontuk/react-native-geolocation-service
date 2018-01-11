import { NativeModules, Platform } from 'react-native';

const { RNFusedLocation } = NativeModules;

// TODO: add static type checker
export default {
    /**
     * Sets configuration options that will be used in all location requests.
     *
     * ### Options
     *
     * #### iOS
     *
     * - `skipPermissionRequests` - defaults to `false`, if `true` you must request permissions
     * before using Geolocation APIs.
     *
     */
    setRNConfiguration: (config) => {
        if (Platform.OS === 'android') return;

        global.navigator.geolocation.setRNConfiguration(config);
    },

    /**
     * Request suitable Location permission based on the key configured on pList.
     * If NSLocationAlwaysUsageDescription is set, it will request Always authorization,
     * although if NSLocationWhenInUseUsageDescription is set, it will request InUse
     * authorization.
     */
    requestAuthorization: () => {
        if (Platform.OS === 'android') return;

        global.navigator.geolocation.requestAuthorization();
    },

    getCurrentPosition: async (success, error, options = {}) => {
        if (Platform.OS === 'ios') {
            // Use the react-native built in Geolocation service.
            global.navigator.geolocation.getCurrentPosition(success, error, options);
            return;
        }

        RNFusedLocation.getCurrentPosition(success, error, options);
    }
};
