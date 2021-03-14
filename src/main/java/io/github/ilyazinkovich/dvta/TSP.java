package io.github.ilyazinkovich.dvta;

import io.github.ilyazinkovich.dvta.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TSP {

  static Double travel(Vehicle vehicle, Set<Request> requests) {
    List<RouteStop> stops = routeStops(vehicle, requests);
    Set<List<RouteStop>> permutations = Permutations.generate(stops, stops.size());
    permutations.removeIf(routeStops -> pickUpAndDropOffIsOutOfOrder(vehicle, routeStops));
    Double cost = null;
    for (List<RouteStop> routeStops : permutations) {
      Double routeCost = routeCost(vehicle, routeStops);
      if (routeCost != null) {
        if (cost == null) {
          cost = routeCost;
        } else {
          cost = Math.min(cost, routeCost);
        }
      }
    }
    return cost;
  }

  private static List<RouteStop> routeStops(Vehicle vehicle, Set<Request> requests) {
    List<RouteStop> stops = new ArrayList<>();
    for (Request passenger : vehicle.passengers) {
      stops.add(new RouteStop(passenger, Type.DROP_OFF));
    }
    for (Request request : requests) {
      stops.add(new RouteStop(request, Type.PICK_UP));
      stops.add(new RouteStop(request, Type.DROP_OFF));
    }
    return stops;
  }

  private static Double routeCost(Vehicle vehicle, List<RouteStop> routeStops) {
    LatLng position = vehicle.currentPosition;
    Instant time = vehicle.currentTime;
    double cost = 0.0D;
    for (RouteStop stop : routeStops) {
      Duration drivingDuration = Routing.drivingTime(position, stop.location());
      time = time.plus(drivingDuration);
      position = stop.location();
      if (!stop.isValid(time)) {
        return null;
      } else {
        cost += stop.delay(time).toSeconds();
      }
    }
    return cost;
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
}
