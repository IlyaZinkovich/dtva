package io.github.ilyazinkovich.dvta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RTV {

  public final Map<Vehicle, Map<Set<Request>, Double>> vehicleToTripCost;

  public RTV(Map<Vehicle, Map<Set<Request>, Double>> vehicleToTripCost) {
    this.vehicleToTripCost = vehicleToTripCost;
  }

  public void addVehicleToTrip(
      Vehicle vehicle, Set<Request> trip, Double cost) {
    if (!vehicleToTripCost.containsKey(vehicle)) {
      vehicleToTripCost.put(vehicle, new HashMap<>());
    }
    Double previousCost = vehicleToTripCost.get(vehicle).get(trip);
    Double newCost;
    if (previousCost == null) {
      newCost = cost;
    } else {
      newCost = Math.min(previousCost, cost);
    }
    vehicleToTripCost.get(vehicle).put(trip, newCost);
  }
}
