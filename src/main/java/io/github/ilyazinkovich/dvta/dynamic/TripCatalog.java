package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.PICK_UP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TripCatalog {

  private final Set<Request> requests;
  private final DrivingTimeMatrix drivingTimeMatrix;
  private final int maxTripRequests;
  private final Map<Integer, Set<Trip>> tripsPerRequestsCount;

  TripCatalog(int maxTripRequests, DrivingTimeMatrix drivingTimeMatrix) {
    this.requests = new HashSet<>();
    this.drivingTimeMatrix = drivingTimeMatrix;
    this.maxTripRequests = maxTripRequests;
    this.tripsPerRequestsCount = new HashMap<>();
    for (int i = 1; i <= maxTripRequests; i++) {
      tripsPerRequestsCount.put(i, new HashSet<>());
    }
  }

  void add(Request request) {
    RouteStop pickUp = new RouteStop(request, PICK_UP);
    RouteStop dropOff = new RouteStop(request, DROP_OFF);
    RouteGenerator generator = new RouteGenerator(request.pickUpTimeWindowStart, drivingTimeMatrix)
        .add(pickUp).add(dropOff);
    if (generator.failed()) {
      System.err.println(
          "Failed to generate route - " + generator.failureReason() + " - " + request);
    } else {
      requests.add(request);
      LinkedList<RouteStop> route = generator.stops();
      tripsPerRequestsCount.get(1).add(new Trip(Set.of(request), route));
      Set<Trip> trips = tripsPerRequestsCount.get(1);
      for (Trip trip : trips) {
        if (!trip.requests.contains(request)) {
          List<Trip> permutations = tripPermutations(trip, pickUp, dropOff);
          tripsPerRequestsCount.get(trip.requests.size() + 1).addAll(permutations);
        }
      }
    }
  }

  private List<Trip> tripPermutations(Trip trip, RouteStop pickUp, RouteStop dropOff) {
    int start = 0;
    int end = trip.route.size();
    List<Trip> permutations = new ArrayList<>();
    for (int i = start; i <= end; i++) {
      for (int j = i + 1; j <= end + 1; j++) {
        RouteGenerator permutation = permute(pickUp, i, dropOff, j, trip.route);
        if (!permutation.failed()) {
          permutations.add(new Trip(permutation.requests(), permutation.stops()));
        }
      }
    }
    return permutations;
  }

  private RouteGenerator permute(
      RouteStop pickUp, int pickUpInsertionIndex,
      RouteStop dropOff, int dropOffInsertionIndex,
      LinkedList<RouteStop> route) {
    RouteGenerator generator =
        new RouteGenerator(route.getFirst().request.pickUpTimeWindowStart, drivingTimeMatrix);
    int i = 0;
    Iterator<RouteStop> iterator = route.iterator();
    int size = 2 + route.size();
    while (i < size) {
      if (i < pickUpInsertionIndex) {
        generator.add(iterator.next());
      } else if (i == pickUpInsertionIndex) {
        generator.add(pickUp);
      } else if (i < dropOffInsertionIndex) {
        generator.add(iterator.next());
      } else if (i == dropOffInsertionIndex) {
        generator.add(dropOff);
      } else {
        generator.add(iterator.next());
      }
      i++;
      if (generator.failed()) {
        break;
      }
    }
    return generator;
  }

  Set<Request> requests() {
    return requests;
  }

  Map<Integer, Set<Trip>> tripsPerRequestsCount() {
    return tripsPerRequestsCount;
  }
}
