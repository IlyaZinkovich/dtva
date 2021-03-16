package io.github.ilyazinkovich.dvta.dynamic;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

class Request {

  public final String id;
  public final LatLng pickUpLocation;
  public final LatLng dropOffLocation;
  public final Instant requestTime;
  public final Instant dispatchTimeout;
  public final Instant pickUpTimeWindowStart;
  public final Instant pickUpTimeWindowEnd;
  public final Duration pickUpQueueTime;
  public final Duration pickUpServiceTime;
  public final Instant dropOffTimeWindowStart;
  public final Instant dropOffTimeWindowEnd;
  public final Duration dropOffServiceTime;
  public final Instant dropOffTimeTarget;
  public final List<Capacity> requiredCapacities;

  /**
   * @param id unique request identifier
   * @param pickUpLocation pick-up geolocation
   * @param dropOffLocation drop-off geolocation
   * @param requestTime time when the request was made
   * @param dispatchTimeout limit on dispatching time
   * @param pickUpTimeWindowStart time when the request becomes ready for pick-up
   * @param pickUpTimeWindowEnd upper-bound on the arrival time at the pick-up location
   * @param pickUpQueueTime queue time required to place the order at the pick-up location
   * @param pickUpServiceTime time required to receive the order at the pick-up location
   * @param dropOffTimeWindowStart lower-bound on the arrival time at the drop-off location
   * @param dropOffTimeWindowEnd lower-bound on the arrival time at the drop-off location
   * @param dropOffServiceTime time required to hand-over the order at the drop-off location
   * @param dropOffTimeTarget target for the arrival time at the drop-off location
   * @param requiredCapacities vehicle capacities required to handle the order
   */
  Request(String id,
      LatLng pickUpLocation,
      LatLng dropOffLocation,
      Instant requestTime,
      Instant dispatchTimeout,
      Instant pickUpTimeWindowStart,
      Instant pickUpTimeWindowEnd,
      Duration pickUpQueueTime,
      Duration pickUpServiceTime,
      Instant dropOffTimeWindowStart,
      Instant dropOffTimeWindowEnd,
      Duration dropOffServiceTime,
      Instant dropOffTimeTarget,
      List<Capacity> requiredCapacities) {
    this.id = id;
    this.pickUpLocation = pickUpLocation;
    this.dropOffLocation = dropOffLocation;
    this.requestTime = requestTime;
    this.dispatchTimeout = dispatchTimeout;
    this.pickUpTimeWindowStart = pickUpTimeWindowStart;
    this.pickUpTimeWindowEnd = pickUpTimeWindowEnd;
    this.pickUpQueueTime = pickUpQueueTime;
    this.pickUpServiceTime = pickUpServiceTime;
    this.dropOffTimeWindowStart = dropOffTimeWindowStart;
    this.dropOffTimeWindowEnd = dropOffTimeWindowEnd;
    this.dropOffTimeTarget = dropOffTimeTarget;
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
        "id='" + id + '\'' +
        ", pickUpLocation=" + pickUpLocation +
        ", dropOffLocation=" + dropOffLocation +
        ", requestTime=" + requestTime +
        ", dispatchTimeout=" + dispatchTimeout +
        ", pickUpTimeWindowStart=" + pickUpTimeWindowStart +
        ", pickUpTimeWindowEnd=" + pickUpTimeWindowEnd +
        ", pickUpQueueTime=" + pickUpQueueTime +
        ", pickUpServiceTime=" + pickUpServiceTime +
        ", dropOffTimeWindowStart=" + dropOffTimeWindowStart +
        ", dropOffTimeWindowEnd=" + dropOffTimeWindowEnd +
        ", dropOffServiceTime=" + dropOffServiceTime +
        ", dropOffTimeTarget=" + dropOffTimeTarget +
        ", requiredCapacities=" + requiredCapacities +
        '}';
  }
}
