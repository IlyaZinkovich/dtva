package io.github.ilyazinkovich.dvta;

import java.util.List;

class Trip {

  public final List<RouteStop> routeStops;

  Trip(
      List<RouteStop> routeStops
  ) {
    this.routeStops = routeStops;
  }

  @Override
  public String toString() {
    return "Trip{" +
        "routeStops=" + routeStops +
        '}';
  }
}
