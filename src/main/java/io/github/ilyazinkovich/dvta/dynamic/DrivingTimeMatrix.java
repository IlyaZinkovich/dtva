package io.github.ilyazinkovich.dvta.dynamic;

import java.time.Duration;

public interface DrivingTimeMatrix {

  Duration drivingTime(LatLng origin, LatLng destination);
}
