package io.github.ilyazinkovich.dvta;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class App {

  public static void main(String[] args) {
    Set<Request> requests = requests();
    RV rv = new RV(new HashMap<>(), new HashMap<>());
    for (Request r1 : requests) {
      for (Request r2 : requests) {
        if (!r1.equals(r2)) {
          Double cost = match(r1, r2);
          if (cost != null) {
            rv.addRequestToRequest(r1, r2, cost);
          }
        }
      }
    }
    System.out.println(rv);
  }

  private static Set<Request> requests() {
    Duration maxToleratedDelay = Duration.ofMinutes(15);
    Duration maxWaitTime = Duration.ofMinutes(10);
    LatLng origin1 = new LatLng(40.767936706542969, -73.982154846191406);
    LatLng destination1 = new LatLng(40.765602111816406, -73.964630126953125);
    Instant requestTime1 = Instant.parse("2016-03-14T17:24:55.00Z");
    Request r1 = new Request(
        origin1,
        destination1,
        requestTime1,
        requestTime1.plus(maxWaitTime),
        requestTime1.plus(drivingDuration(origin1, destination1)),
        maxToleratedDelay);
    LatLng origin2 = new LatLng(origin1.lat - 0.001, origin1.lng - 0.001);
    LatLng destination2 = new LatLng(destination1.lat - 0.001, destination1.lng - 0.001);
    Instant requestTime2 = requestTime1.plus(Duration.ofSeconds(10));
    Request r2 = new Request(
        origin2,
        destination2,
        requestTime2,
        requestTime2.plus(maxWaitTime),
        requestTime2.plus(drivingDuration(origin2, destination2)),
        maxToleratedDelay);
    Set<Request> requests = new HashSet<>();
    requests.add(r1);
    requests.add(r2);
    return requests;
  }

  private static Double match(Request r1, Request r2) {
    Instant pickUpTime2 = r1.requestTime
        .plus(drivingDuration(r1.origin, r2.origin));
    if (pickUpTime2.isAfter(r2.latestAcceptablePickUpTime)) {
      return null;
    }
    Double cost = null;
    Instant firstDropOffTime1 = pickUpTime2
        .plus(drivingDuration(r2.origin, r1.destination));
    Instant firstDropOffTime2 = firstDropOffTime1
        .plus(drivingDuration(r1.destination, r2.destination));
    if (firstDropOffTime1.isBefore(r1.earliestPossibleDropOffTime.plus(r1.maxToleratedDelay))
        && firstDropOffTime2.isBefore(r2.earliestPossibleDropOffTime.plus(r2.maxToleratedDelay))) {
      cost = rrCost(r1, r2, firstDropOffTime1, firstDropOffTime2);
    }
    Instant secondDropOffTime2 = pickUpTime2
        .plus(drivingDuration(r2.origin, r2.destination));
    Instant secondDropOffTime1 = secondDropOffTime2
        .plus(drivingDuration(r2.destination, r1.destination));
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

  private static double rrCost(
      Request r1,
      Request r2,
      Instant dropOffTime1,
      Instant dropOffTime2
  ) {
    return (double) Duration.between(r1.earliestPossibleDropOffTime, dropOffTime1)
        .plus(Duration.between(r2.earliestPossibleDropOffTime, dropOffTime2))
        .toSeconds();
  }

  private static class RV {

    public final Map<Request, Map<Request, Double>> requestToRequestCost;
    public final Map<Vehicle, Map<Request, Double>> vehicleToRequestCost;

    public RV(
        Map<Request, Map<Request, Double>> requestToRequestCost,
        Map<Vehicle, Map<Request, Double>> vehicleToRequestCost
    ) {
      this.requestToRequestCost = requestToRequestCost;
      this.vehicleToRequestCost = vehicleToRequestCost;
    }

    public void addRequestToRequest(Request r1, Request r2, Double cost) {
      if (!requestToRequestCost.containsKey(r1)) {
        requestToRequestCost.put(r1, new HashMap<>());
      }
      Map<Request, Double> r1r2 = requestToRequestCost.get(r1);
      if (r1r2.containsKey(r2)) {
        r1r2.put(r2, Math.min(r1r2.get(r2), cost));
      } else {
        r1r2.put(r2, cost);
      }
      if (!requestToRequestCost.containsKey(r2)) {
        requestToRequestCost.put(r2, new HashMap<>());
      }
      Map<Request, Double> r2r1 = requestToRequestCost.get(r2);
      if (r2r1.containsKey(r2)) {
        r2r1.put(r1, Math.min(r2r1.get(r1), cost));
      } else {
        r2r1.put(r1, cost);
      }
    }

    public void addVehicleToRequest(Vehicle v, Request r, Double cost) {
      if (!vehicleToRequestCost.containsKey(v)) {
        vehicleToRequestCost.put(v, new HashMap<>());
      }
      vehicleToRequestCost.get(v).put(r, cost);
    }
  }

  private static Duration drivingDuration(LatLng origin, LatLng destination) {
    double avgSpeedInKmH = 25.0;
    double distanceKm = distance(origin.lat, origin.lng, destination.lat, destination.lng);
    return Duration.ofSeconds(Math.round(distanceKm * 1000 / (avgSpeedInKmH * 1000 / 3600)));
  }

  private static class LatLng {

    public final double lat;
    public final double lng;

    private LatLng(double lat, double lng) {
      this.lat = lat;
      this.lng = lng;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LatLng latLng = (LatLng) o;
      return Double.compare(latLng.lat, lat) == 0
          && Double.compare(latLng.lng, lng) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(lat, lng);
    }
  }

  private static class Request {

    public final LatLng origin; // o
    public final LatLng destination; // d
    public final Instant requestTime; // tR
    public final Instant latestAcceptablePickUpTime; // tPL = TR + Ω -> max waiting time
    public final Instant pickUpTime; // tP
    public final Instant earliestPossibleDropOffTime; // t* = tR + τ(o, d) -> driving time
    public final Duration maxToleratedDelay; // ∆ = tD - t*

    private Request(
        LatLng origin,
        LatLng destination,
        Instant requestTime,
        Instant latestAcceptablePickUpTime,
        Instant earliestPossibleDropOffTime,
        Duration maxToleratedDelay
    ) {
      this.origin = origin;
      this.destination = destination;
      this.requestTime = requestTime;
      this.latestAcceptablePickUpTime = latestAcceptablePickUpTime;
      this.pickUpTime = null;
      this.earliestPossibleDropOffTime = earliestPossibleDropOffTime;
      this.maxToleratedDelay = maxToleratedDelay;
    }

    public Request(Request request, Instant pickUpTime) {
      this.origin = request.origin;
      this.destination = request.destination;
      this.requestTime = request.requestTime;
      this.latestAcceptablePickUpTime = request.latestAcceptablePickUpTime;
      this.pickUpTime = pickUpTime;
      this.earliestPossibleDropOffTime = request.earliestPossibleDropOffTime;
      this.maxToleratedDelay = request.maxToleratedDelay;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Request request = (Request) o;
      return Objects.equals(origin, request.origin) && Objects
          .equals(destination, request.destination) && Objects
          .equals(requestTime, request.requestTime) && Objects
          .equals(latestAcceptablePickUpTime, request.latestAcceptablePickUpTime) && Objects
          .equals(pickUpTime, request.pickUpTime) && Objects
          .equals(earliestPossibleDropOffTime, request.earliestPossibleDropOffTime);
    }

    @Override
    public int hashCode() {
      return Objects.hash(origin, destination, requestTime, latestAcceptablePickUpTime, pickUpTime,
          earliestPossibleDropOffTime);
    }
  }

  private static class Passenger {

    public final Request request;

    public Passenger(Request request, Instant pickUpTime) {
      this.request = new Request(request, pickUpTime);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Passenger passenger = (Passenger) o;
      return Objects.equals(request, passenger.request);
    }

    @Override
    public int hashCode() {
      return Objects.hash(request);
    }
  }

  private static class Vehicle {

    public final LatLng currentPosition; // q
    public final Instant currentTime; // t
    public final Set<Passenger> passengers; // P
    public final int capacity; // v

    private Vehicle(
        LatLng currentPosition,
        Instant currentTime,
        Set<Passenger> passengers,
        int capacity
    ) {
      this.currentPosition = currentPosition;
      this.currentTime = currentTime;
      this.passengers = passengers;
      this.capacity = capacity;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Vehicle vehicle = (Vehicle) o;
      return Objects.equals(currentPosition, vehicle.currentPosition) && Objects
          .equals(currentTime, vehicle.currentTime) && Objects
          .equals(passengers, vehicle.passengers);
    }

    @Override
    public int hashCode() {
      return Objects.hash(currentPosition, currentTime, passengers);
    }
  }

  private static class RouteStop {

    public final Request request;
    public final Type type;

    private RouteStop(Request request, Type type) {
      this.request = request;
      this.type = type;
    }

    enum Type {
      PICK_UP, DROP_OFF
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RouteStop routeStop = (RouteStop) o;
      return Objects.equals(request, routeStop.request) && type == routeStop.type;
    }

    @Override
    public int hashCode() {
      return Objects.hash(request, type);
    }
  }

  private static class Trip {

    public final List<RouteStop> routeStops;

    private Trip(
        List<RouteStop> routeStops
    ) {
      this.routeStops = routeStops;
    }
  }

  private static final int EARTH_RADIUS = 6371; // KM

  public static double distance(
      double startLat, double startLong,
      double endLat, double endLong
  ) {

    double dLat = Math.toRadians((endLat - startLat));
    double dLong = Math.toRadians((endLong - startLong));

    startLat = Math.toRadians(startLat);
    endLat = Math.toRadians(endLat);

    double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS * c;
  }

  public static double haversin(double val) {
    return Math.pow(Math.sin(val / 2), 2);
  }
}
