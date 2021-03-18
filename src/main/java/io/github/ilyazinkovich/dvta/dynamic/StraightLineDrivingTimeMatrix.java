package io.github.ilyazinkovich.dvta.dynamic;

import java.time.Duration;

class StraightLineDrivingTimeMatrix implements DrivingTimeMatrix {

  @Override
  public Duration drivingTime(LatLng origin, LatLng destination) {
    return Routing.drivingTime(origin, destination);
  }
}
