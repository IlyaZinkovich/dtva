package io.github.ilyazinkovich.dvta;

import static java.util.stream.Collectors.toList;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.skaggsm.ortools.OrToolsHelper;
import io.github.ilyazinkovich.dvta.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    OrToolsHelper.loadLibrary();
    Random random = new Random(12345);
    Duration maxWaitTime = Duration.ofMinutes(10);
    Duration maxToleratedDelay = Duration.ofMinutes(15);
    List<Request> requests = RequestsReader.read(maxWaitTime, maxToleratedDelay);
    int vehiclesCount = 20;
    int vehicleCapacity = 3;
    List<Vehicle> vehicles =
        VehiclesGenerator.generate(requests, vehiclesCount, vehicleCapacity, random);
    RV rv = createRV(requests, vehicles);
    RTV rtv = createRTV(rv);
    List<Assignment> greedyAssignment = greedyAssignment(rtv);
    double greedyCost = greedyAssignment.stream().mapToDouble(assignment ->
        rtv.vehicleToTripCost.get(assignment.vehicle).get(assignment.trip)).sum();
    System.out.println("Greedy cost: " + greedyCost);
    MPSolver solver = MPSolver.createSolver("SCIP");
    if (solver == null) {
      System.out.println("Could not create solver SCIP");
      return;
    }
    double maxCost = rtv.vehicleToTripCost.values().stream()
        .flatMap(tripCosts -> tripCosts.values().stream())
        .max(Comparator.naturalOrder())
        .orElse(0.0D);
    MPObjective objective = solver.objective();
    List<AssignmentOptimisationVariable> assignmentOptimisationVariables = new ArrayList<>();
    Map<Request, Set<MPVariable>> requestToAssignmentVariable = new HashMap<>();
    rtv.vehicleToTripCost.forEach((vehicle, tripsWithCost) -> {
      MPConstraint mpConstraint = solver.makeConstraint();
      tripsWithCost.forEach((trip, cost) -> {
        MPVariable mpVariable = solver.makeIntVar(0, 1, "ε-" + trip + "-" + vehicle);
        assignmentOptimisationVariables.add(
            new AssignmentOptimisationVariable(vehicle, trip, mpVariable));
        objective.setCoefficient(mpVariable, cost);
        mpConstraint.setCoefficient(mpVariable, 1);
        for (Request request : trip) {
          if (!requestToAssignmentVariable.containsKey(request)) {
            requestToAssignmentVariable.put(request, new HashSet<>());
          }
          requestToAssignmentVariable.get(request).add(mpVariable);
        }
      });
      mpConstraint.setUb(1);
    });
    for (Request request : requests) {
      MPVariable mpVariable = solver.makeIntVar(0, 1, "χ-" + request.id);
      objective.setCoefficient(mpVariable, maxCost * 2);
      MPConstraint mpConstraint = solver.makeConstraint();
      mpConstraint.setCoefficient(mpVariable, 1);
      if (requestToAssignmentVariable.containsKey(request)) {
        requestToAssignmentVariable.get(request).forEach(assignmentVariable -> {
          mpConstraint.setCoefficient(assignmentVariable, 1);
        });
      }
      mpConstraint.setBounds(1, 1);
    }
    objective.setMinimization();
    MPSolver.ResultStatus resultStatus = solver.solve();
    System.out.println(resultStatus);
    List<Assignment> assignments = assignmentOptimisationVariables.stream()
        .filter(a -> a.variable.solutionValue() > 0)
        .map(a -> new Assignment(a.vehicle, a.trip))
        .collect(toList());
    double optimalCost = assignments.stream().mapToDouble(assignment ->
        rtv.vehicleToTripCost.get(assignment.vehicle).get(assignment.trip)).sum();
    System.out.println("Optimal cost: " + optimalCost);
    executor.shutdown();
  }

  private static List<Assignment> greedyAssignment(RTV rtv) {
    List<AssignmentCandidate> candidates = new ArrayList<>();
    rtv.vehicleToTripCost.forEach((vehicle, tripsWithCost) ->
        tripsWithCost.forEach((trip, cost) ->
            candidates.add(new AssignmentCandidate(vehicle, trip, cost))
        )
    );
    candidates.sort(Comparator.<AssignmentCandidate, Integer>comparing(
        assignmentCandidate -> assignmentCandidate.trip.size())
        .reversed()
        .thenComparing(assignmentCandidate -> assignmentCandidate.cost));
    Set<Request> assignedRequests = new HashSet<>();
    Set<Vehicle> assignedVehicles = new HashSet<>();
    List<Assignment> assignmentCandidates = new ArrayList<>();
    for (AssignmentCandidate candidate : candidates) {
      if (!(requestsAreAssigned(assignedRequests, candidate)
          || vehicleIsAssigned(assignedVehicles, candidate))) {
        assignmentCandidates.add(new Assignment(candidate.vehicle, candidate.trip));
        assignedRequests.addAll(candidate.trip);
        assignedVehicles.add(candidate.vehicle);
      }
    }
    return assignmentCandidates;
  }

  private static boolean requestsAreAssigned(Set<Request> assignedRequests,
      AssignmentCandidate assignmentCandidate) {
    for (Request request : assignmentCandidate.trip) {
      if (assignedRequests.contains(request)) {
        return true;
      }
    }
    return false;
  }

  private static boolean vehicleIsAssigned(Set<Vehicle> assignedVehicles,
      AssignmentCandidate assignmentCandidate) {
    return assignedVehicles.contains(assignmentCandidate.vehicle);
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
          Double cost = travel(vehicle, tripOfSizeK);
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

  private static Double travel(Vehicle vehicle, Set<Request> requests) {
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

  private static RV createRV(List<Request> requests, List<Vehicle> vehicles) {
    RV rv = new RV(new HashMap<>(), new HashMap<>());
    for (Request r1 : requests) {
      for (Vehicle vehicle : vehicles) {
        Double cost = travel(vehicle, Set.of(r1));
        if (cost != null) {
          rv.addVehicleToRequest(vehicle, r1, cost);
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
    return rv;
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
