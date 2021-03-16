package io.github.ilyazinkovich.dvta.static_;

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
    List<Request> requests = RequestsReader.read(maxWaitTime, maxToleratedDelay).subList(0, 90);
    int vehiclesCount = 30;
    int vehicleCapacity = 3;
    List<Vehicle> vehicles =
        VehiclesGenerator.generate(requests, vehiclesCount, vehicleCapacity, random);
    RR rr = RR.create(requests);
    RV rv = RV.create(requests, vehicles);
    RTV rtv = RTV.create(rr, rv, executor);
    Map<Vehicle, Set<Request>> greedyAssignment = GreedyAssignmentSolver.solve(rtv);
    double greedyCost = AssignmentCost.calculate(rtv, greedyAssignment, requests.size());
    System.out.println("Greedy cost: " + greedyCost);
    Map<Vehicle, Set<Request>> optimalAssignment =
        OptimalAssignmentSolver.solve(requests, rtv, greedyAssignment);
    double optimalCost = AssignmentCost.calculate(rtv, optimalAssignment, requests.size());
    System.out.println("Optimal cost: " + optimalCost);
    executor.shutdown();
  }
}
