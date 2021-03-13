package io.github.ilyazinkovich.dvta;

import java.util.Objects;
import java.util.Set;

public class Assignment {

  public final Vehicle vehicle;
  public final Set<Request> trip;
  public final Double cost;

  public Assignment(Vehicle vehicle, Set<Request> trip, Double cost) {
    this.vehicle = vehicle;
    this.trip = trip;
    this.cost = cost;
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
        .equals(trip, that.trip) && Objects.equals(cost, that.cost);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vehicle, trip, cost);
  }

  @Override
  public String toString() {
    return "GreedyCandidate{" +
        "vehicle=" + vehicle +
        ", trip=" + trip +
        ", cost=" + cost +
        '}';
  }
}
