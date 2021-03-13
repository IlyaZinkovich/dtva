package io.github.ilyazinkovich.dvta;

import io.github.ilyazinkovich.dvta.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class App {

  public static void main(String[] args) {
    Random random = new Random(12345);
    Duration maxWaitTime = Duration.ofMinutes(10);
    Duration maxToleratedDelay = Duration.ofMinutes(15);
    List<Request> requests = RequestsReader.read(maxWaitTime, maxToleratedDelay);
    int vehiclesCount = 10;
    int vehicleCapacity = 3;
    List<Vehicle> vehicles =
        VehiclesGenerator.generate(requests, vehiclesCount, vehicleCapacity, random);
    RV rv = new RV(new HashMap<>(), new HashMap<>());
    for (Request r1 : requests) {
      for (Vehicle vehicle : vehicles) {
        if (travel(vehicle, r1)) {
          rv.addVehicleToRequest(vehicle, r1);
        }
      }
      for (Request r2 : requests) {
        if (!r1.equals(r2)) {
          Double cost = match(r1, r2);
          if (cost != null) {
            rv.addRequestToRequest(r1, r2, cost);
          }
        }
      }
    }
  }

  private static boolean travel(Vehicle vehicle, Request request) {
    List<RouteStop> stops = routeStops(vehicle, request);
    Set<List<RouteStop>> permutations = Permutations.generate(stops, stops.size());
    permutations.removeIf(routeStops -> pickUpAndDropOffIsOutOfOrder(vehicle, routeStops));
    for (List<RouteStop> routeStops : permutations) {
      boolean valid = routeIsValid(vehicle, routeStops);
      if (valid) {
        return true;
      }
    }
    return false;
  }

  private static boolean routeIsValid(Vehicle vehicle, List<RouteStop> routeStops) {
    LatLng position = vehicle.currentPosition;
    Instant time = vehicle.currentTime;
    boolean valid = true;
    for (RouteStop stop : routeStops) {
      Duration drivingDuration = Routing.drivingTime(position, stop.location());
      time = time.plus(drivingDuration);
      position = stop.location();
      if (!stop.isValid(time)) {
        valid = false;
        break;
      }
    }
    return valid;
  }

  private static boolean pickUpAndDropOffIsOutOfOrder(Vehicle vehicle, List<RouteStop> routeStops) {
    Set<Request> pickUpRequests = new HashSet<>(vehicle.passengers);
    for (RouteStop stop : routeStops) {
      if (stop.type == Type.PICK_UP) {
        pickUpRequests.add(stop.request);
      } else {
        if (pickUpRequests.contains(stop.request)) {
          pickUpRequests.remove(stop.request);
        } else {
          return true;
        }
      }
    }
    return false;
  }

  private static List<RouteStop> routeStops(Vehicle vehicle, Request request) {
    List<RouteStop> stops = new ArrayList<>();
    for (Request passenger : vehicle.passengers) {
      stops.add(new RouteStop(passenger, Type.DROP_OFF));
    }
    stops.add(new RouteStop(request, Type.PICK_UP));
    stops.add(new RouteStop(request, Type.DROP_OFF));
    return stops;
  }

  private static Double match(Request r1, Request r2) {
    Instant pickUpTime2 = r1.requestTime
        .plus(Routing.drivingTime(r1.origin, r2.origin));
    if (pickUpTime2.isAfter(r2.latestAcceptablePickUpTime)
        || pickUpTime2.isBefore(r2.requestTime)) {
      return null;
    }
    Double cost = null;
    Instant firstDropOffTime1 = pickUpTime2
        .plus(Routing.drivingTime(r2.origin, r1.destination));
    Instant firstDropOffTime2 = firstDropOffTime1
        .plus(Routing.drivingTime(r1.destination, r2.destination));
    if (firstDropOffTime1.isBefore(r1.earliestPossibleDropOffTime.plus(r1.maxToleratedDelay))
        && firstDropOffTime2.isBefore(r2.earliestPossibleDropOffTime.plus(r2.maxToleratedDelay))) {
      cost = rrCost(r1, r2, firstDropOffTime1, firstDropOffTime2);
    }
    Instant secondDropOffTime2 = pickUpTime2
        .plus(Routing.drivingTime(r2.origin, r2.destination));
    Instant secondDropOffTime1 = secondDropOffTime2
        .plus(Routing.drivingTime(r2.destination, r1.destination));
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

  private static double rrCost(Request r1, Request r2, Instant dropOffTime1, Instant dropOffTime2) {
    return (double) Duration.between(r1.earliestPossibleDropOffTime, dropOffTime1)
        .plus(Duration.between(r2.earliestPossibleDropOffTime, dropOffTime2))
        .toMillis();
  }
}
