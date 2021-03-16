package io.github.ilyazinkovich.dvta.dynamic;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

class Request {

  public final String id;
  public final LatLng origin;
  public final LatLng destination;
  public final Instant requestTime;
  public final Instant dispatchTimeout;
  public final Instant pickUpTimeWindowStart;
  public final Instant pickUpTimeWindowEnd;
  public final Duration pickUpQueueTime;
  public final Duration pickUpServiceTime;
  public final Instant dropOffTimeWindowStart;
  public final Instant dropOffTimeWindowEnd;
  public final Duration dropOffServiceTime;
  public final List<Capacity> requiredCapacities;

  Request(String id,
      LatLng origin,
      LatLng destination,
      Instant requestTime,
      Instant dispatchTimeout,
      Instant pickUpTimeWindowStart,
      Instant pickUpTimeWindowEnd,
      Duration pickUpQueueTime,
      Duration pickUpServiceTime,
      Instant dropOffTimeWindowStart,
      Instant dropOffTimeWindowEnd,
      Duration dropOffServiceTime,
      List<Capacity> requiredCapacities) {
    this.id = id;
    this.origin = origin;
    this.destination = destination;
    this.requestTime = requestTime;
    this.dispatchTimeout = dispatchTimeout;
    this.pickUpTimeWindowStart = pickUpTimeWindowStart;
    this.pickUpTimeWindowEnd = pickUpTimeWindowEnd;
    this.pickUpQueueTime = pickUpQueueTime;
    this.pickUpServiceTime = pickUpServiceTime;
    this.dropOffTimeWindowStart = dropOffTimeWindowStart;
    this.dropOffTimeWindowEnd = dropOffTimeWindowEnd;
    this.dropOffServiceTime = dropOffServiceTime;
    this.requiredCapacities = requiredCapacities;
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
    return Objects.equals(id, request.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Request{" +
        "id=" + id +
        '}';
  }
}
