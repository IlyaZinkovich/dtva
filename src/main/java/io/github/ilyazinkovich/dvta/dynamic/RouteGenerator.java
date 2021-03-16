package io.github.ilyazinkovich.dvta.dynamic;

import io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

public class RouteGenerator {

  private boolean failed;
  private final LinkedList<RouteStop> stops;
  private final Set<Request> pickUps;
  private Instant time;
  private Duration queueTime;
  private Duration serviceTime;

  RouteGenerator(Instant time) {
    this.failed = false;
    this.stops = new LinkedList<>();
    this.pickUps = new HashSet<>();
    this.time = time;
    this.queueTime = Duration.ZERO;
    this.serviceTime = Duration.ZERO;
  }

  public void add(RouteStop stop) {
    if (failed) {
      return;
    }
    if (stop.type == Type.PICK_UP) {
      if (stops.isEmpty()) {
        if (stop.request.pickUpTimeWindowEnd != null
            && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
          failed = true;
          return;
        }
        if (stop.request.pickUpTimeWindowStart != null) {
          time = max(time, stop.request.pickUpTimeWindowStart);
        } else if (stop.request.pickUpQueueTime != null) {
          queueTime = stop.request.pickUpQueueTime;
        }
        serviceTime = stop.request.pickUpServiceTime;
      } else {
        boolean samePickUpLocation =
            Objects.equals(stops.getLast().request.pickUpLocationId, stop.request.pickUpLocationId);
        if (stops.getLast().type == Type.PICK_UP && samePickUpLocation) {
          if (stop.request.pickUpTimeWindowStart != null) {
            Instant firstProjectedDeparture = time.plus(serviceTime);
            Instant secondProjectedDeparture =
                max(time, stop.request.pickUpTimeWindowStart).plus(stop.request.pickUpServiceTime);
            if (secondProjectedDeparture.isAfter(firstProjectedDeparture)) {
              time = max(time, stop.request.pickUpTimeWindowStart);
              serviceTime = stop.request.pickUpServiceTime;
            }
          } else if (stop.request.pickUpQueueTime != null) {
            if (queueTime.isZero()) {
              queueTime = stop.request.pickUpQueueTime;
            }
            serviceTime = max(serviceTime, stop.request.pickUpServiceTime);
          }
        } else if (stops.getLast().type == Type.PICK_UP && !samePickUpLocation) {
          time = time.plus(queueTime).plus(serviceTime)
              .plus(Routing.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.pickUpTimeWindowEnd != null
              && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
            failed = true;
            return;
          }
          if (stop.request.pickUpTimeWindowStart != null) {
            time = max(time, stop.request.pickUpTimeWindowStart);
          } else if (stop.request.pickUpQueueTime != null) {
            queueTime = stop.request.pickUpQueueTime;
          }
          serviceTime = stop.request.pickUpServiceTime;
        } else if (stops.getLast().type == Type.DROP_OFF) {
          time = time.plus(queueTime).plus(serviceTime)
              .plus(Routing.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.pickUpTimeWindowEnd != null
              && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
            failed = true;
            return;
          }
          if (stop.request.pickUpTimeWindowStart != null) {
            time = max(time, stop.request.pickUpTimeWindowStart);
          } else if (stop.request.pickUpQueueTime != null) {
            queueTime = stop.request.pickUpQueueTime;
          }
          serviceTime = stop.request.pickUpServiceTime;
        }
      }
      pickUps.add(stop.request);
    } else if (stop.type == Type.DROP_OFF) {
      if (!pickUps.contains(stop.request)) {
        failed = true;
        return;
      } else {
        time = time.plus(queueTime).plus(serviceTime)
            .plus(Routing.drivingTime(stops.getLast().location(), stop.location()));
        if (stop.request.dropOffTimeWindowStart != null) {
          time = max(time, stop.request.dropOffTimeWindowStart);
        }
        queueTime = Duration.ZERO;
        serviceTime = stop.request.dropOffServiceTime;
      }
    }
    stops.add(stop);
  }

  private static Instant max(Instant left, Instant right) {
    if (left.isBefore(right)) {
      return right;
    } else {
      return left;
    }
  }

  private Duration max(Duration left, Duration right) {
    if (left.compareTo(right) < 0) {
      return right;
    } else {
      return left;
    }
  }
}
