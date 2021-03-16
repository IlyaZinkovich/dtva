package io.github.ilyazinkovich.dvta.static_;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

class Request {

  public final String id;
  public final LatLng origin; // o
  public final LatLng destination; // d
  public final Instant requestTime; // tR
  public final Instant latestAcceptablePickUpTime; // tPL = TR + Ω -> max waiting time
  public final Instant pickUpTime; // tP
  public final Instant earliestPossibleDropOffTime; // t* = tR + τ(o, d) -> driving time
  public final Duration maxToleratedDelay; // ∆ = tD - t*

  Request(
      String id,
      LatLng origin,
      LatLng destination,
      Instant requestTime,
      Instant latestAcceptablePickUpTime,
      Instant earliestPossibleDropOffTime,
      Duration maxToleratedDelay
  ) {
    this.id = id;
    this.origin = origin;
    this.destination = destination;
    this.requestTime = requestTime;
    this.latestAcceptablePickUpTime = latestAcceptablePickUpTime;
    this.pickUpTime = null;
    this.earliestPossibleDropOffTime = earliestPossibleDropOffTime;
    this.maxToleratedDelay = maxToleratedDelay;
  }

  public Request(
      Request request,
      Instant pickUpTime
  ) {
    this.id = request.id;
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
    return Objects.equals(id, request.id) && Objects
        .equals(origin, request.origin) && Objects.equals(destination, request.destination)
        && Objects.equals(requestTime, request.requestTime) && Objects
        .equals(latestAcceptablePickUpTime, request.latestAcceptablePickUpTime) && Objects
        .equals(pickUpTime, request.pickUpTime) && Objects
        .equals(earliestPossibleDropOffTime, request.earliestPossibleDropOffTime) && Objects
        .equals(maxToleratedDelay, request.maxToleratedDelay);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, origin, destination, requestTime, latestAcceptablePickUpTime, pickUpTime,
            earliestPossibleDropOffTime, maxToleratedDelay);
  }

  @Override
  public String toString() {
    return "Request{" +
        "id=" + id +
        '}';
  }
}
