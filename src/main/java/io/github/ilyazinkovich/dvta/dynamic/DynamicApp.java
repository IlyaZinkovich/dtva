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
    List<Request> requests = RequestsReader.read(maxDispatchTime, deliveryTimeBuffer, random).subList(0, 100);
    GLOBAL_TIME = getGlobalTime(requests);
    int vehiclesCount = 100;
    List<Vehicle> vehicles = VehiclesGenerator.generate(requests, vehiclesCount, random);
    DrivingTimeMatrix drivingTimeMatrix = new StraightLineDrivingTimeMatrix();
    System.out.println("TC");
    for (int i = 0; i < 100; i++) {
      TripCatalog tripCatalog = new TripCatalog(3, drivingTimeMatrix);
      long start = System.currentTimeMillis();
      for (Request request : requests) {
        tripCatalog.add(request);
      }
      System.out.println(System.currentTimeMillis() - start);
    }
  }

  private static void rr(List<Request> requests, DrivingTimeMatrix drivingTimeMatrix) {
    System.out.println("RR");
    for (int i = 0; i < 100; i++) {
      RR rr = new RR(new HashSet<>());
      long start = System.currentTimeMillis();
      Set<Request> processedRequests = new HashSet<>();
      for (Request request : requests) {
        rr.add(request, processedRequests, drivingTimeMatrix);
        processedRequests.add(request);
      }
      System.out.println(System.currentTimeMillis() - start);
    }
  }

  private static Instant getGlobalTime(List<Request> requests) {
    return requests.stream()
        .map(request -> request.requestTime)
        .min(Comparator.naturalOrder())
        .orElseGet(Instant::now);
  }
}
