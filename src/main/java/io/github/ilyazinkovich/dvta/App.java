package io.github.ilyazinkovich.dvta;

import io.github.ilyazinkovich.dvta.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class App {

  public static void main(String[] args) {
    Set<Request> requests = requests();
    RV rv = new RV(new HashMap<>(), new HashMap<>());
    for (Request r1 : requests) {
      for (Request r2 : requests) {
        if (!r1.equals(r2)) {
          Double cost = match(r1, r2);
          if (cost != null) {
            rv.addRequestToRequest(r1, r2, cost);
          }
        }
      }
    }
    System.out.println(rv);
  }

  public Trip travel(Vehicle vehicle, Request request) {
    List<RouteStop> stops = new ArrayList<>();
    for (Passenger passenger : vehicle.passengers) {
      stops.add(new RouteStop(passenger.request, Type.PICK_UP));
      stops.add(new RouteStop(passenger.request, Type.DROP_OFF));
    }
    stops.add(new RouteStop(request, Type.PICK_UP));
    stops.add(new RouteStop(request, Type.DROP_OFF));
    return new Trip(new ArrayList<>());
  }

  private static Set<Request> requests() {
    Duration maxToleratedDelay = Duration.ofMinutes(15);
    Duration maxWaitTime = Duration.ofMinutes(10);
    LatLng origin1 = new LatLng(40.767936706542969, -73.982154846191406);
    LatLng destination1 = new LatLng(40.765602111816406, -73.964630126953125);
    Instant requestTime1 = Instant.parse("2016-03-14T17:24:55.00Z");
    Request r1 = new Request(
        origin1,
        destination1,
        requestTime1,
        requestTime1.plus(maxWaitTime),
        requestTime1.plus(drivingDuration(origin1, destination1)),
        maxToleratedDelay);
    LatLng origin2 = new LatLng(origin1.lat - 0.001, origin1.lng - 0.001);
    LatLng destination2 = new LatLng(destination1.lat - 0.001, destination1.lng - 0.001);
    Instant requestTime2 = requestTime1.plus(Duration.ofSeconds(10));
    Request r2 = new Request(
        origin2,
        destination2,
        requestTime2,
        requestTime2.plus(maxWaitTime),
        requestTime2.plus(drivingDuration(origin2, destination2)),
        maxToleratedDelay);
    Set<Request> requests = new HashSet<>();
    requests.add(r1);
    requests.add(r2);
    return requests;
  }

  private static Double match(Request r1, Request r2) {
    Instant pickUpTime2 = r1.requestTime
        .plus(drivingDuration(r1.origin, r2.origin));
    if (pickUpTime2.isAfter(r2.latestAcceptablePickUpTime)) {
      return null;
    }
    Double cost = null;
    Instant firstDropOffTime1 = pickUpTime2
        .plus(drivingDuration(r2.origin, r1.destination));
    Instant firstDropOffTime2 = firstDropOffTime1
        .plus(drivingDuration(r1.destination, r2.destination));
    if (firstDropOffTime1.isBefore(r1.earliestPossibleDropOffTime.plus(r1.maxToleratedDelay))
        && firstDropOffTime2.isBefore(r2.earliestPossibleDropOffTime.plus(r2.maxToleratedDelay))) {
      cost = rrCost(r1, r2, firstDropOffTime1, firstDropOffTime2);
    }
    Instant secondDropOffTime2 = pickUpTime2
        .plus(drivingDuration(r2.origin, r2.destination));
    Instant secondDropOffTime1 = secondDropOffTime2
        .plus(drivingDuration(r2.destination, r1.destination));
    if (secondDropOffTime2.isBefore(r2.earliestPossibleDropOffTime.plus(r2.maxToleratedDelay))
        && secondDropOffTime1.isBefore(r1.earliestPossibleDropOffTime.plus(r1.maxToleratedDelay))) {
      double alternativeCost = rrCost(r1, r2, secondDropOffTime1, secondDropOffTime2);
      if (cost != null) {
        cost = Math.min(cost, alternativeCost);
      } else {
        cost = alternativeCost;
      }
    }
    return cost;
  }

  private static double rrCost(
      Request r1,
      Request r2,
      Instant dropOffTime1,
      Instant dropOffTime2
  ) {
    return (double) Duration.between(r1.earliestPossibleDropOffTime, dropOffTime1)
        .plus(Duration.between(r2.earliestPossibleDropOffTime, dropOffTime2))
        .toSeconds();
  }

  private static Duration drivingDuration(LatLng origin, LatLng destination) {
    double avgSpeedInKmH = 25.0;
    double distanceKm =
        StraightLine.distance(origin.lat, origin.lng, destination.lat, destination.lng);
    return Duration.ofSeconds(Math.round(distanceKm * 1000 / (avgSpeedInKmH * 1000 / 3600)));
  }
}
