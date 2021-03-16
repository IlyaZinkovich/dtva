package io.github.ilyazinkovich.dvta.dynamic;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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
  }

  private static Instant getGlobalTime(List<Request> requests) {
    return requests.stream()
        .map(request -> request.requestTime)
        .min(Comparator.naturalOrder())
        .orElseGet(Instant::now);
  }
}
