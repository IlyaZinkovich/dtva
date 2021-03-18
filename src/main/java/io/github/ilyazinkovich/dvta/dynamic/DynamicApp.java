package io.github.ilyazinkovich.dvta.dynamic;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DynamicApp {

  static Instant GLOBAL_TIME;

  public static void main(String[] args) {
    Duration maxDispatchTime = Duration.ofMinutes(10);
    Duration deliveryTimeBuffer = Duration.ofMinutes(7);
    Random random = new Random(12345);
    List<Request> requests = RequestsReader.read(maxDispatchTime, deliveryTimeBuffer, random);
    System.out.println(requests);
    GLOBAL_TIME = getGlobalTime(requests);
    int vehiclesCount = 100;
    List<Vehicle> vehicles = VehiclesGenerator.generate(requests, vehiclesCount, random);
    System.out.println(vehicles);
    DrivingTimeMatrix drivingTimeMatrix = new StraightLineDrivingTimeMatrix();
    RR rr = new RR(new HashSet<>());
    Set<Request> processedRequests = new HashSet<>();
    for (Request request : requests) {
      rr.add(request, processedRequests, drivingTimeMatrix);
      processedRequests.add(request);
    }
    System.out.println((double) rr.pairs.size() / (requests.size() * requests.size()));
  }

  private static Instant getGlobalTime(List<Request> requests) {
    return requests.stream()
        .map(request -> request.requestTime)
        .min(Comparator.naturalOrder())
        .orElseGet(Instant::now);
  }
}
