package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.Routing.drivingTime;

import io.github.ilyazinkovich.dvta.dynamic.Capacity.Unit;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

class RequestsReader {

  static List<Request> read(Duration maxDispatchTime, Duration deliveryTimeBuffer, Random random) {
    List<Request> requests = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    try (Scanner scanner = new Scanner(Paths.get("data.csv"))) {
      scanner.nextLine();
      while (scanner.hasNext()) {
        String line = scanner.nextLine();
        String[] data = line.split(",");
        String id = data[0];
        LatLng pickUpLocation = new LatLng(Double.parseDouble(data[3]),
            Double.parseDouble(data[2]));
        Integer pickUpLocationId = pickUpLocation.hashCode();
        LatLng dropOffLocation = new LatLng(Double.parseDouble(data[5]),
            Double.parseDouble(data[4]));
        Integer dropOffLocationId = dropOffLocation.hashCode();
        Instant requestTime = LocalDateTime.parse(data[1], formatter).toInstant(ZoneOffset.UTC);
        Instant dispatchTimeout = requestTime.plus(maxDispatchTime);
        Instant pickUpTimeWindowStart = requestTime.plus(Duration.ofMinutes(random.nextInt(15)));
        Instant pickUpTimeWindowEnd = null;
        Duration pickUpQueueTime = null;
        Duration pickUpServiceTime = Duration.ofMinutes(random.nextInt(3) + 2L);
        Duration dropOffServiceTime = Duration.ofMinutes(random.nextInt(3) + 2L);
        Instant idealDropOffTime = pickUpTimeWindowStart
            .plus(pickUpServiceTime)
            .plus(drivingTime(pickUpLocation, dropOffLocation))
            .plus(dropOffServiceTime);
        Instant dropOffTimeTarget = idealDropOffTime.plus(deliveryTimeBuffer);
        Instant dropOffTimeWindowStart = idealDropOffTime;
        Instant dropOffTimeWindowEnd = idealDropOffTime.plus(deliveryTimeBuffer.multipliedBy(2));
        List<Capacity> requiredCapacities = List.of(new Capacity(1, Unit.SEAT));
        requests.add(new Request(id, pickUpLocation, pickUpLocationId, dropOffLocation,
            dropOffLocationId, requestTime, dispatchTimeout, pickUpTimeWindowStart,
            pickUpTimeWindowEnd, pickUpQueueTime, pickUpServiceTime, dropOffTimeWindowStart,
            dropOffTimeWindowEnd, dropOffServiceTime, dropOffTimeTarget, requiredCapacities));
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read file.", e);
    }
    return requests;
  }
}
