package io.github.ilyazinkovich.dvta.static_;

import com.google.ortools.linearsolver.MPVariable;
import java.util.Objects;
import java.util.Set;

public class AssignmentOptimisationVariable {

  public final Vehicle vehicle;
  public final Set<Request> trip;
  public final MPVariable variable;

  public AssignmentOptimisationVariable(Vehicle vehicle, Set<Request> trip, MPVariable variable) {
    this.trip = trip;
    this.vehicle = vehicle;
    this.variable = variable;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final AssignmentOptimisationVariable that = (AssignmentOptimisationVariable) o;
    return Objects.equals(vehicle, that.vehicle)
        && Objects.equals(trip, that.trip);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vehicle, trip);
  }

  @Override
  public String toString() {
    return "AssignmentOptimisationVariable{" +
        "vehicle=" + vehicle +
        ", trip=" + trip +
        ", variable=" + variable.name() +
        '}';
  }
}
