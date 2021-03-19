package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.PICK_UP;

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
            RouteGenerator permutation = permute(route, points, trip.route);
            if (!permutation.failed()) {
              tripsPerRequestsCount.get(permutation.requests().size())
                  .add(new Trip(permutation.requests(), permutation.stops()));
            }
          }
        }
      }
    }
  }

  private RouteGenerator permute(
      LinkedList<RouteStop> addedStops, int[] insertionPoints, LinkedList<RouteStop> route) {
    RouteGenerator generator =
        new RouteGenerator(route.getFirst().request.pickUpTimeWindowStart, drivingTimeMatrix);
    int i = 0;
    Iterator<RouteStop> iterator = route.iterator();
    int size = addedStops.size() + route.size();
    while (i < size) {
      if (i < insertionPoints[0]) {
        generator.add(iterator.next());
      } else if (i == insertionPoints[0]) {
        generator.add(addedStops.getFirst());
      } else if (i < insertionPoints[1]) {
        generator.add(iterator.next());
      } else if (i == insertionPoints[1]) {
        generator.add(addedStops.getLast());
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
