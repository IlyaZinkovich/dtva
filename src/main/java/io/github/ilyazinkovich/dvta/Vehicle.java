package io.github.ilyazinkovich.dvta;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

class Vehicle {

  public final LatLng currentPosition; // q
  public final Instant currentTime; // t
  public final Set<Passenger> passengers; // P
  public final int capacity; // v

  private Vehicle(
      LatLng currentPosition,
      Instant currentTime,
      Set<Passenger> passengers,
      int capacity
  ) {
    this.currentPosition = currentPosition;
    this.currentTime = currentTime;
    this.passengers = passengers;
    this.capacity = capacity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Vehicle vehicle = (Vehicle) o;
    return Objects.equals(currentPosition, vehicle.currentPosition) && Objects
        .equals(currentTime, vehicle.currentTime) && Objects
        .equals(passengers, vehicle.passengers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentPosition, currentTime, passengers);
  }
}
