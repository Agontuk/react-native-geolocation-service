package com.agontuk.RNFusedLocation;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

public class RNFusedLocationModule extends ReactContextBaseJavaModule {
    public RNFusedLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNFusedLocation";
    }
}
