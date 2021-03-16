package io.github.ilyazinkovich.dvta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RV {

  public final Map<Vehicle, Map<Request, Double>> vehicleToRequestCost;

  public RV(Map<Vehicle, Map<Request, Double>> vehicleToRequestCost) {
    this.vehicleToRequestCost = vehicleToRequestCost;
  }

  public static RV create(List<Request> requests, List<Vehicle> vehicles) {
    RV rv = new RV(new HashMap<>());
    for (Request r1 : requests) {
      for (Vehicle vehicle : vehicles) {
        Double cost = TSP.travel(vehicle, Set.of(r1));
        if (cost != null) {
          rv.addVehicleToRequest(vehicle, r1, cost);
        }
      }
    }
    return rv;
  }

  private void addVehicleToRequest(Vehicle v, Request r, Double cost) {
    if (!vehicleToRequestCost.containsKey(v)) {
      vehicleToRequestCost.put(v, new HashMap<>());
    }
    vehicleToRequestCost.get(v).put(r, cost);
  }
}
