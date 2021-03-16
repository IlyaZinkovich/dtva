package io.github.ilyazinkovich.dvta.static_;

import java.util.Objects;
import java.util.Set;

public class AssignmentCandidate {

  public final Vehicle vehicle;
  public final Set<Request> trip;
  public final Double cost;

  public AssignmentCandidate(Vehicle vehicle, Set<Request> trip, Double cost) {
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
    final AssignmentCandidate that = (AssignmentCandidate) o;
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
