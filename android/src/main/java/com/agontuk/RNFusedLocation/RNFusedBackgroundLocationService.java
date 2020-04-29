package com.agontuk.RNFusedLocation;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.android.gms.location.LocationResult;

public class RNFusedBackgroundLocationService extends Service {

    static RNFusedLocationModule.RNFusedLocationModuleBridge rNFusedLocationModuleBridge = null;

    public RNFusedBackgroundLocationService() {
    }

    public RNFusedBackgroundLocationService(RNFusedLocationModule.RNFusedLocationModuleBridge parent) {
        rNFusedLocationModuleBridge = parent;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_REDELIVER_INTENT;
    }

    public void handleIntent(Intent intent) {
        if (null == intent) {
            return;
        }

        if (!LocationResult.hasResult(intent)) {
            return;
        }

        Location location =  LocationResult.extractResult(intent).getLastLocation();;

        if (null == location) {
            return;
        }

        if (null != location && rNFusedLocationModuleBridge != null) {
            rNFusedLocationModuleBridge.onLocationUpdate(location);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
