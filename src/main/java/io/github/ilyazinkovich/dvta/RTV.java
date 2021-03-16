package io.github.ilyazinkovich.dvta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class RTV {

  public final Map<Vehicle, Map<Set<Request>, Double>> vehicleToTripCost;

  public RTV(Map<Vehicle, Map<Set<Request>, Double>> vehicleToTripCost) {
    this.vehicleToTripCost = vehicleToTripCost;
  }

  static RTV create(RR rr, RV rv, Executor executor) {
    RTV rtv = new RTV(new ConcurrentHashMap<>());
    Set<Vehicle> rvVehicles = rv.vehicleToRequestCost.keySet();
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (Vehicle vehicle : rvVehicles) {
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        addTripsOfSizeOne(rv, rtv, vehicle);
        for (int k = 2; k <= vehicle.capacity; k++) {
          addTripsOfSizeK(rr, rtv, vehicle, k);
        }
      }, executor);
      futures.add(future);
    }
    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    return rtv;
  }

  private static void addTripsOfSizeOne(RV rv, RTV rtv, Vehicle vehicle) {
    Set<Request> rvVehicleRequests = rv.vehicleToRequestCost.get(vehicle).keySet();
    for (Request request : rvVehicleRequests) {
      Set<Request> trip = new HashSet<>();
      trip.add(request);
      rtv.addVehicleToTrip(vehicle, trip, rv.vehicleToRequestCost.get(vehicle).get(request));
    }
  }

  private static void addTripsOfSizeK(RR rr, RTV rtv, Vehicle vehicle, int k) {
    Set<Set<Request>> tripsOfSizeLessThanK =
        new HashSet<>(rtv.vehicleToTripCost.get(vehicle).keySet());
    for (Set<Request> t1 : tripsOfSizeLessThanK) {
      for (Set<Request> t2 : tripsOfSizeLessThanK) {
        if (!t1.equals(t2) && t1.size() + t2.size() == k && r1r2exist(rr, t1, t2)) {
          Set<Request> tripOfSizeK = new HashSet<>();
          tripOfSizeK.addAll(t1);
          tripOfSizeK.addAll(t2);
          Double cost = TSP.travel(vehicle, tripOfSizeK);
          if (cost != null) {
            rtv.addVehicleToTrip(vehicle, tripOfSizeK, cost);
          }
        }
      }
    }
  }

  private static boolean r1r2exist(RR rr, Set<Request> t1, Set<Request> t2) {
    for (Request r1 : t1) {
      for (Request r2 : t2) {
        if (rr.requestToRequestCost.containsKey(r1)
            && !rr.requestToRequestCost.get(r1).containsKey(r2)) {
          return false;
        }
      }
    }
    return true;
  }

  private void addVehicleToTrip(
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
