package io.github.ilyazinkovich.dvta;

import java.util.Objects;
import java.util.Set;

public class Assignment {

  public final Vehicle vehicle;
  public final Set<Request> trip;

  public Assignment(Vehicle vehicle, Set<Request> trip) {
    this.vehicle = vehicle;
    this.trip = trip;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Assignment that = (Assignment) o;
    return Objects.equals(vehicle, that.vehicle) && Objects
        .equals(trip, that.trip);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vehicle, trip);
  }

  @Override
  public String toString() {
    return "Assignment{" +
        "vehicle=" + vehicle +
        ", trip=" + trip +
        '}';
  }
}
