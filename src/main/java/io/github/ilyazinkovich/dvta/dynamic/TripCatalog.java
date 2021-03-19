package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.PICK_UP;

import java.util.HashMap;
import java.util.HashSet;
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
    RouteGenerator generator = new RouteGenerator(request.pickUpTimeWindowStart, drivingTimeMatrix)
        .add(new RouteStop(request, PICK_UP))
        .add(new RouteStop(request, DROP_OFF));
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
          int size = trip.route.size();
          List<int[]> insertionPoints = InsertionPoints.generate(0, size);
          for (int[] points : insertionPoints) {
            RouteGenerator gen = new RouteGenerator(
                trip.route.getFirst().request.pickUpTimeWindowStart, drivingTimeMatrix);
            int i = 0;
            for (RouteStop stop : trip.route) {
              if (gen.failed()) {
                break;
              }
              if (i < points[0]) {
                gen.add(stop);
              } else if (i == points[0]) {
                gen.add(route.getFirst());
              } else if (i < points[1]) {
                gen.add(stop);
              } else if (i == points[1]) {
                gen.add(route.getLast());
              } else {
                gen.add(stop);
              }
              i++;
            }
            if (!gen.failed()) {
              int newSize = trip.requests.size() + 1;
              Set<Request> tripRequests = new HashSet<>();
              tripRequests.add(request);
              tripRequests.addAll(trip.requests);
              tripsPerRequestsCount.get(newSize).add(new Trip(tripRequests, gen.stops()));
            }
          }
        }
      }
    }
  }

  Set<Request> requests() {
    return requests;
  }

  Map<Integer, Set<Trip>> tripsPerRequestsCount() {
    return tripsPerRequestsCount;
  }
}
