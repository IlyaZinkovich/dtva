package io.github.ilyazinkovich.dvta.dynamic;

import static java.util.stream.Collectors.toList;

import io.github.ilyazinkovich.dvta.dynamic.Capacity.Unit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class VehiclesGenerator {

  private static final AtomicInteger IDS = new AtomicInteger();

  static List<Vehicle> generate(List<Request> requests, int vehiclesCount, Random random) {
    List<Request> copy = new ArrayList<>(requests);
    Collections.shuffle(copy, random);
    return copy.stream()
        .map(VehiclesGenerator::createVehicle)
        .limit(vehiclesCount).collect(toList());
  }

  private static Vehicle createVehicle(Request request) {
    List<RouteStop> route = new ArrayList<>();
    List<Capacity> capacities = List.of(new Capacity(3, Unit.SEAT));
    return new Vehicle(
        String.valueOf(IDS.incrementAndGet()), request.pickUpLocation, route, capacities);
  }
}
