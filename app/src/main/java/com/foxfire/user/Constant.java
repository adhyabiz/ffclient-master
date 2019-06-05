package com.foxfire.user;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

class Constant {

    static final String GEOFENCE_ID_STAN_UNI = "STAN_UNI";
    static final float GEOFENCE_RADIUS_IN_METERS = 10000;

    /**
     * Map for storing information about stanford university in the Stanford.
     */
    static final HashMap<String, LatLng> AREA_LANDMARKS = new HashMap<>();

    static {
        // stanford university.
        AREA_LANDMARKS.put(GEOFENCE_ID_STAN_UNI, new LatLng(28.6139, 77.2090));
    }
}
