package com.jaswant.dispatch.domain;

import com.jaswant.dispatch.geo.Haversine;

public record Location(double latitude, double longitude) {

    public double distanceKmTo(Location other) {
        return Haversine.distanceKm(latitude, longitude, other.latitude, other.longitude);
    }
}