package io.github.ilyazinkovich.dvta.dynamic;

import java.time.Duration;

interface DrivingTimeMatrix {

  Duration drivingTime(LatLng origin, LatLng destination);
}
