package io.github.ilyazinkovich.dvta.static_;

import static java.util.stream.Collectors.toMap;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolver.ResultStatus;
import com.google.ortools.linearsolver.MPVariable;
import com.skaggsm.ortools.OrToolsHelper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OptimalAssignmentSolver {

  public static final MPSolver mipSolver;

  static {
    OrToolsHelper.loadLibrary();
    mipSolver = MPSolver.createSolver("SCIP");
    if (mipSolver == null) {
      throw new RuntimeException("Could not create solver SCIP");
    }
  }

  static Map<Vehicle, Set<Request>> solve(List<Request> requests, RTV rtv,
      Map<Vehicle, Set<Request>> greedyAssignment) {
    double noAssignmentPenalty = AssignmentCost.penalty(rtv);
    MPObjective objective = mipSolver.objective();
    List<AssignmentOptimisationVariable> assignmentOptimisationVariables = new ArrayList<>();
    Map<Request, Set<MPVariable>> requestToAssignmentVariable = new HashMap<>();
    rtv.vehicleToTripCost.forEach((vehicle, tripsWithCost) -> {
      MPConstraint mpConstraint = mipSolver.makeConstraint();
      tripsWithCost.forEach((trip, cost) -> {
        MPVariable mpVariable = mipSolver.makeIntVar(0, 1, "ε-" + trip + "-" + vehicle);
        assignmentOptimisationVariables.add(
            new AssignmentOptimisationVariable(vehicle, trip, mpVariable));
        objective.setCoefficient(mpVariable, cost);
        mpConstraint.setCoefficient(mpVariable, 1);
        addRequestToAssignmentVariable(requestToAssignmentVariable, trip, mpVariable);
      });
      mpConstraint.setUb(1);
    });
    setInitialSolution(greedyAssignment, assignmentOptimisationVariables);
    for (Request request : requests) {
      MPVariable mpVariable = mipSolver.makeIntVar(0, 1, "χ-" + request.id);
      objective.setCoefficient(mpVariable, noAssignmentPenalty);
      MPConstraint mpConstraint = mipSolver.makeConstraint();
      mpConstraint.setCoefficient(mpVariable, 1);
      if (requestToAssignmentVariable.containsKey(request)) {
        requestToAssignmentVariable.get(request).forEach(assignmentVariable -> {
          mpConstraint.setCoefficient(assignmentVariable, 1);
        });
      }
      mpConstraint.setBounds(1, 1);
    }
    objective.setMinimization();
    mipSolver.setTimeLimit(Duration.ofSeconds(30).toMillis());
    MPSolver.ResultStatus resultStatus = mipSolver.solve();
    if (ResultStatus.OPTIMAL == resultStatus || ResultStatus.FEASIBLE == resultStatus) {
      return assignmentOptimisationVariables.stream()
          .filter(a -> a.variable.solutionValue() > 0)
          .collect(toMap(a -> a.vehicle, a -> a.trip, (left, right) -> left));
    } else {
      return greedyAssignment;
    }
  }

  private static void addRequestToAssignmentVariable(
      Map<Request, Set<MPVariable>> requestToAssignmentVariables, Set<Request> trip,
      MPVariable mpVariable) {
    for (Request request : trip) {
      if (!requestToAssignmentVariables.containsKey(request)) {
        requestToAssignmentVariables.put(request, new HashSet<>());
      }
      requestToAssignmentVariables.get(request).add(mpVariable);
    }
  }

  private static void setInitialSolution(Map<Vehicle, Set<Request>> greedyAssignment,
      List<AssignmentOptimisationVariable> assignmentOptimisationVariables) {
    MPVariable[] initialSolution = assignmentOptimisationVariables.stream()
        .filter(a -> a.trip.equals(greedyAssignment.get(a.vehicle)))
        .map(a -> a.variable)
        .toArray(MPVariable[]::new);
    double[] initialSolutionValues = new double[initialSolution.length];
    Arrays.fill(initialSolutionValues, 1.0D);
    mipSolver.setHint(initialSolution, initialSolutionValues);
  }
}
