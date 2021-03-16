package io.github.ilyazinkovich.dvta.dynamic;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public class DynamicApp {

  public static void main(String[] args) {
    Duration maxDispatchTime = Duration.ofMinutes(10);
    Duration deliveryTimeBuffer = Duration.ofMinutes(7);
    Random random = new Random(12345);
    List<Request> requests = RequestsReader.read(maxDispatchTime, deliveryTimeBuffer, random);
    System.out.println(requests);
  }
}
