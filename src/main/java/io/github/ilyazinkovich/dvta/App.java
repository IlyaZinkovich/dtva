package io.github.ilyazinkovich.dvta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {

  private static final ExecutorService executor =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  public static void main(String[] args) {
    Random random = new Random(12345);
    Duration maxWaitTime = Duration.ofMinutes(10);
    Duration maxToleratedDelay = Duration.ofMinutes(15);
    List<Request> requests = RequestsReader.read(maxWaitTime, maxToleratedDelay);
    int vehiclesCount = 20;
    int vehicleCapacity = 2;
    List<Vehicle> vehicles =
        VehiclesGenerator.generate(requests, vehiclesCount, vehicleCapacity, random);
    RV rv = RV.create(requests, vehicles);
    RTV rtv = createRTV(rv);
    Map<Vehicle, Set<Request>> greedyAssignment = GreedyAssignmentSolver.solve(rtv);
    double greedyCost = assignmentsCost(rtv, greedyAssignment);
    System.out.println("Greedy cost: " + greedyCost);
    Map<Vehicle, Set<Request>> optimalAssignments =
        OptimalAssignmentSolver.solve(requests, rtv, greedyAssignment);
    double optimalCost = assignmentsCost(rtv, optimalAssignments);
    System.out.println("Optimal cost: " + optimalCost);
    executor.shutdown();
  }

  private static double assignmentsCost(RTV rtv, Map<Vehicle, Set<Request>> assignments) {
    return assignments.entrySet().stream().mapToDouble(assignment ->
        rtv.vehicleToTripCost.get(assignment.getKey()).get(assignment.getValue())).sum();
  }

  private static RTV createRTV(RV rv) {
    RTV rtv = new RTV(new ConcurrentHashMap<>());
    Set<Vehicle> rvVehicles = rv.vehicleToRequestCost.keySet();
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (Vehicle vehicle : rvVehicles) {
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        addTripsOfSizeOne(rv, rtv, vehicle);
        for (int k = 2; k <= vehicle.capacity; k++) {
          addTripsOfSizeK(rv, rtv, vehicle, k);
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

  private static void addTripsOfSizeK(RV rv, RTV rtv, Vehicle vehicle, int k) {
    Set<Set<Request>> tripsOfSizeLessThanK =
        new HashSet<>(rtv.vehicleToTripCost.get(vehicle).keySet());
    for (Set<Request> t1 : tripsOfSizeLessThanK) {
      for (Set<Request> t2 : tripsOfSizeLessThanK) {
        if (!t1.equals(t2) && t1.size() + t2.size() == k && r1r2exist(rv, t1, t2)) {
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

  private static boolean r1r2exist(RV rv, Set<Request> t1, Set<Request> t2) {
    for (Request r1 : t1) {
      for (Request r2 : t2) {
        if (rv.requestToRequestCost.containsKey(r1)
            && !rv.requestToRequestCost.get(r1).containsKey(r2)) {
          return false;
        }
      }
    }
    return true;
  }
}
