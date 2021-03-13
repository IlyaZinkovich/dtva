package io.github.ilyazinkovich.dvta;

import java.time.Duration;

public class Routing {

  private static final int EARTH_RADIUS = 6371; // KM

  static Duration drivingTime(LatLng origin, LatLng destination) {
    double avgSpeedInKmH = 25.0;
    double distanceKm =
        straightLineDistance(origin.lat, origin.lng, destination.lat, destination.lng);
    return Duration.ofSeconds(Math.round(distanceKm * 1000 / (avgSpeedInKmH * 1000 / 3600)));
  }

  private static double straightLineDistance(
      double startLat, double startLong,
      double endLat, double endLong
  ) {

    double dLat = Math.toRadians((endLat - startLat));
    double dLong = Math.toRadians((endLong - startLong));

    startLat = Math.toRadians(startLat);
    endLat = Math.toRadians(endLat);

    double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS * c;
  }

  private static double haversin(double val) {
    return Math.pow(Math.sin(val / 2), 2);
  }
}
