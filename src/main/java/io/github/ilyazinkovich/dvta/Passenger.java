package io.github.ilyazinkovich.dvta;

import java.time.Instant;
import java.util.Objects;

class Passenger {

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
