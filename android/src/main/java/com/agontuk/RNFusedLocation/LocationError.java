package com.agontuk.RNFusedLocation;

public enum LocationError {
    PERMISSION_DENIED(1),
    POSITION_UNAVAILABLE(2),
    TIMEOUT(3),
    PLAY_SERVICE_NOT_AVAILABLE(4);

    private int value;

    LocationError(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
