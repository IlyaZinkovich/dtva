package io.github.ilyazinkovich.dvta;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

class RouteStop {

  public final Request request;
  public final Type type;

  RouteStop(
      Request request,
      Type type
  ) {
    this.request = request;
    this.type = type;
  }

  public LatLng location() {
    if (type == Type.PICK_UP) {
      return request.origin;
    } else {
      return request.destination;
    }
  }

  public boolean isValid(Instant time) {
    if (type == Type.PICK_UP) {
      System.out.println("PICK_UP - " + Duration.between(time, request.latestAcceptablePickUpTime));
      return !time.isAfter(request.latestAcceptablePickUpTime);
    } else {
      System.out.println("DROP_OFF - " + Duration.between(time, request.earliestPossibleDropOffTime.plus(request.maxToleratedDelay)));
      return !time.isAfter(request.earliestPossibleDropOffTime.plus(request.maxToleratedDelay));
    }
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

  @Override
  public String toString() {
    return "RouteStop{" +
        "request=" + request +
        ", type=" + type +
        '}';
  }

  public enum Type {
    PICK_UP, DROP_OFF
  }
}
