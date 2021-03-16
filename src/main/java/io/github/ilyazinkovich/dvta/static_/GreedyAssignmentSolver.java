package io.github.ilyazinkovich.dvta.static_;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class GreedyAssignmentSolver {

  static Map<Vehicle, Set<Request>> solve(RTV rtv) {
    List<AssignmentCandidate> candidates = new ArrayList<>();
    rtv.vehicleToTripCost.forEach((vehicle, tripsWithCost) ->
        tripsWithCost.forEach((trip, cost) ->
            candidates.add(new AssignmentCandidate(vehicle, trip, cost))
        )
    );
    candidates.sort(Comparator.<AssignmentCandidate, Integer>comparing(
        assignmentCandidate -> assignmentCandidate.trip.size())
        .reversed()
        .thenComparing(assignmentCandidate -> assignmentCandidate.cost));
    Set<Request> assignedRequests = new HashSet<>();
    Set<Vehicle> assignedVehicles = new HashSet<>();
    Map<Vehicle, Set<Request>> assignment = new HashMap<>();
    for (AssignmentCandidate candidate : candidates) {
      if (!(requestsAreAssigned(assignedRequests, candidate)
          || vehicleIsAssigned(assignedVehicles, candidate))) {
        assignment.put(candidate.vehicle, candidate.trip);
        assignedRequests.addAll(candidate.trip);
        assignedVehicles.add(candidate.vehicle);
      }
    }
    return assignment;
  }

  private static boolean requestsAreAssigned(Set<Request> assignedRequests,
      AssignmentCandidate assignmentCandidate) {
    for (Request request : assignmentCandidate.trip) {
      if (assignedRequests.contains(request)) {
        return true;
      }
    }
    return false;
  }

  private static boolean vehicleIsAssigned(Set<Vehicle> assignedVehicles,
      AssignmentCandidate assignmentCandidate) {
    return assignedVehicles.contains(assignmentCandidate.vehicle);
  }
}
