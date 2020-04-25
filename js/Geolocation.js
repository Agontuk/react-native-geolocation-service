/* eslint-disable no-undef */
/* eslint-disable func-names */
/* eslint-disable object-shorthand */
const Geolocation = {
  setRNConfiguration: function () {
    throw new Error('Method not supported by browser');
  },

  requestAuthorization: async function () {
    return Promise.reject('Method not supported by browser');
  },

  getCurrentPosition: function (success, error, options) {
    if (!success) {
      throw new Error('Must provide a success callback');
    } else if (!navigator || !navigator.geolocation) {
      throw new Error('Navigator is not defined');
    }

    navigator.geolocation.getCurrentPosition(success, error, options);
  },

  watchPosition: function (success, error, options) {
    if (!success) {
      throw new Error('Must provide a success callback');
    } else if (!navigator || !navigator.geolocation) {
      throw new Error('Navigator is not defined');
    }

    return navigator.geolocation.watchPosition(success, error, options);
  },

  clearWatch: function (watchID) {
    if (!navigator || !navigator.geolocation) {
      throw new Error('Navigator is not defined');
    }

    navigator.geolocation.clearWatch(watchID);
  },

  stopObserving: function () {
    throw new Error('Method not supported by browser');
  }
};

export default Geolocation;
