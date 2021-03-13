package io.github.ilyazinkovich.dvta;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RequestsReader {

  static List<Request> read(Duration maxWaitTime, Duration maxToleratedDelay) {
    List<Request> requests = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    try (Scanner scanner = new Scanner(Paths.get("data.csv"))) {
      scanner.nextLine();
      while (scanner.hasNext()) {
        String line = scanner.nextLine();
        String[] data = line.split(",");
        String id = data[0];
        LatLng origin = new LatLng(Double.parseDouble(data[3]), Double.parseDouble(data[2]));
        LatLng destination = new LatLng(Double.parseDouble(data[5]), Double.parseDouble(data[4]));
        Instant requestTime = LocalDateTime.parse(data[1], formatter).toInstant(ZoneOffset.UTC);
        Instant latestAcceptablePickUpTime = requestTime.plus(maxWaitTime);
        Instant earliestPossibleDropOffTime = requestTime.plus(drivingTime(origin, destination));
        requests.add(new Request(id, origin, destination, requestTime, latestAcceptablePickUpTime,
            earliestPossibleDropOffTime, maxToleratedDelay));
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read file.", e);
    }
    return requests;
  }

  private static Duration drivingTime(LatLng origin, LatLng destination) {
    double avgSpeedInKmH = 25.0;
    double distanceKm =
        StraightLine.distance(origin.lat, origin.lng, destination.lat, destination.lng);
    return Duration.ofSeconds(Math.round(distanceKm * 1000 / (avgSpeedInKmH * 1000 / 3600)));
  }
}
