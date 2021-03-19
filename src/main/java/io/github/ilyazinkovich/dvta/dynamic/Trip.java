package io.github.ilyazinkovich.dvta.dynamic;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

class Trip {

  final Set<Request> requests;
  final LinkedList<RouteStop> route;

  Trip(Set<Request> requests, LinkedList<RouteStop> route) {
    this.requests = requests;
    this.route = route;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Trip trip = (Trip) o;
    return Objects.equals(requests, trip.requests) && Objects
        .equals(route, trip.route);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requests, route);
  }

  @Override
  public String toString() {
    return "Trip{" +
        "requests=" + requests +
        ", route=" + route +
        '}';
  }
}
