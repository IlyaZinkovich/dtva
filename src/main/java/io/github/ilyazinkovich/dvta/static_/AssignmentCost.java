package io.github.ilyazinkovich.dvta.static_;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

class AssignmentCost {

  static double calculate(RTV rtv, Map<Vehicle, Set<Request>> assignment, int requestsCount) {
    double penalty = penalty(rtv);
    int assignedRequestsCount = assignment.values().stream().mapToInt(Set::size).sum();
    return assignment.entrySet().stream().mapToDouble(vehicleToTrip ->
        rtv.vehicleToTripCost.get(vehicleToTrip.getKey()).get(vehicleToTrip.getValue())).sum()
        + penalty * (requestsCount - assignedRequestsCount);
  }

  static double penalty(RTV rtv) {
    double maxCost = rtv.vehicleToTripCost.values().stream()
        .flatMap(tripCosts -> tripCosts.values().stream())
        .max(Comparator.naturalOrder())
        .orElse(0.0D);
    return maxCost * 2;
  }
}
