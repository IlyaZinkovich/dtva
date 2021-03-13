package io.github.ilyazinkovich.dvta;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class VehiclesGenerator {

  private static final AtomicInteger IDS = new AtomicInteger();

  static List<Vehicle> generate(
      List<Request> requests, int vehiclesCount, int vehicleCapacity, Random random) {
    List<Request> copy = new ArrayList<>(requests);
    Collections.shuffle(copy, random);
    return copy.stream()
        .map(request -> createVehicle(vehicleCapacity, request))
        .limit(vehiclesCount).collect(toList());
  }

  private static Vehicle createVehicle(int vehicleCapacity, Request request) {
    return new Vehicle(String.valueOf(IDS.incrementAndGet()), request.origin, request.requestTime,
        new HashSet<>(), vehicleCapacity);
  }
}
