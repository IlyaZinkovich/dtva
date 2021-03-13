package io.github.ilyazinkovich.dvta;

import java.time.Duration;

public class Routing {

  static Duration drivingTime(LatLng origin, LatLng destination) {
    double avgSpeedInKmH = 25.0;
    double distanceKm =
        StraightLine.distance(origin.lat, origin.lng, destination.lat, destination.lng);
    return Duration.ofSeconds(Math.round(distanceKm * 1000 / (avgSpeedInKmH * 1000 / 3600)));
  }
}
