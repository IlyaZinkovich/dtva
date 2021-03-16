package io.github.ilyazinkovich.dvta.dynamic;

import java.util.List;
import java.util.Objects;

class Vehicle {

  public final String id;
  public final LatLng currentLocation;
  public final List<RouteStop> route;
  public final List<Capacity> capacities;

  /**
   * @param id unique vehicle identifier
   * @param currentLocation current vehicle location
   * @param route route that the vehicle follows
   * @param capacities capacities available in the vehicle
   */
  Vehicle(String id,
      LatLng currentLocation,
      List<RouteStop> route,
      List<Capacity> capacities) {
    this.id = id;
    this.currentLocation = currentLocation;
    this.route = route;
    this.capacities = capacities;
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
    return Objects.equals(id, vehicle.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Vehicle{" +
        "id='" + id + '\'' +
        ", currentLocation=" + currentLocation +
        ", route=" + route +
        ", capacities=" + capacities +
        '}';
  }
}
