package io.github.ilyazinkovich.dvta.dynamic;

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
      return request.pickUpLocation;
    } else {
      return request.dropOffLocation;
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
