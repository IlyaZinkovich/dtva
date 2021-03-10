package io.github.ilyazinkovich.dvta;

import java.util.HashMap;
import java.util.Map;

class RV {

  public final Map<Request, Map<Request, Double>> requestToRequestCost;
  public final Map<Vehicle, Map<Request, Double>> vehicleToRequestCost;

  public RV(
      Map<Request, Map<Request, Double>> requestToRequestCost,
      Map<Vehicle, Map<Request, Double>> vehicleToRequestCost
  ) {
    this.requestToRequestCost = requestToRequestCost;
    this.vehicleToRequestCost = vehicleToRequestCost;
  }

  public void addRequestToRequest(Request r1, Request r2, Double cost) {
    if (!requestToRequestCost.containsKey(r1)) {
      requestToRequestCost.put(r1, new HashMap<>());
    }
    Map<Request, Double> r1r2 = requestToRequestCost.get(r1);
    if (r1r2.containsKey(r2)) {
      r1r2.put(r2, Math.min(r1r2.get(r2), cost));
    } else {
      r1r2.put(r2, cost);
    }
    if (!requestToRequestCost.containsKey(r2)) {
      requestToRequestCost.put(r2, new HashMap<>());
    }
    Map<Request, Double> r2r1 = requestToRequestCost.get(r2);
    if (r2r1.containsKey(r2)) {
      r2r1.put(r1, Math.min(r2r1.get(r1), cost));
    } else {
      r2r1.put(r1, cost);
    }
  }

  public void addVehicleToRequest(Vehicle v, Request r, Double cost) {
    if (!vehicleToRequestCost.containsKey(v)) {
      vehicleToRequestCost.put(v, new HashMap<>());
    }
    vehicleToRequestCost.get(v).put(r, cost);
  }
}
