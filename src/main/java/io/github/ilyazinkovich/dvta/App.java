package io.github.ilyazinkovich.dvta;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {

  public static void main(String[] args) {
    ExecutorService executor =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    Random random = new Random(12345);
    Duration maxWaitTime = Duration.ofMinutes(10);
    Duration maxToleratedDelay = Duration.ofMinutes(15);
    List<Request> requests = RequestsReader.read(maxWaitTime, maxToleratedDelay);
    int vehiclesCount = 20;
    int vehicleCapacity = 2;
    List<Vehicle> vehicles =
        VehiclesGenerator.generate(requests, vehiclesCount, vehicleCapacity, random);
    RV rv = RV.create(requests, vehicles);
    RTV rtv = RTV.create(rv, executor);
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
}
