package io.github.ilyazinkovich.dvta;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

class Vehicle {

  public final UUID id;
  public final LatLng currentPosition; // q
  public final Instant currentTime; // t
  public final Set<Passenger> passengers; // P
  public final int capacity; // v

  Vehicle(
      UUID id, LatLng currentPosition,
      Instant currentTime,
      Set<Passenger> passengers,
      int capacity
  ) {
    this.id = id;
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
    return capacity == vehicle.capacity && Objects.equals(id, vehicle.id)
        && Objects.equals(currentPosition, vehicle.currentPosition) && Objects
        .equals(currentTime, vehicle.currentTime) && Objects
        .equals(passengers, vehicle.passengers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, currentPosition, currentTime, passengers, capacity);
  }

  @Override
  public String toString() {
    return "Vehicle{" +
        "id=" + id +
        '}';
  }
}
