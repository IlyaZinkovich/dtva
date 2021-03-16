package io.github.ilyazinkovich.dvta.dynamic;

import io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RouteGenerator {

  private boolean failed;
  private List<RouteStop> route;
  private Set<Request> pickUps;
  private Instant time;
  private Duration queueTime;
  private Duration serviceTime;
  private RouteStop lastStop;

  public RouteGenerator(Instant time) {
    this.failed = false;
    this.route = new ArrayList<>();
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
      if (route.isEmpty()) {
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
        if (lastStop.type == Type.PICK_UP
            && Objects.equals(lastStop.request.pickUpLocationId, stop.request.pickUpLocationId)) {
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
        } else if (lastStop.type == Type.PICK_UP
            && !Objects.equals(lastStop.request.pickUpLocationId, stop.request.pickUpLocationId)) {
          time = time.plus(queueTime).plus(serviceTime)
              .plus(Routing.drivingTime(lastStop.location(), stop.location()));
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
        } else if (lastStop.type == Type.DROP_OFF) {
          time = time.plus(serviceTime)
              .plus(Routing.drivingTime(lastStop.location(), stop.location()));
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
        route.add(stop);
        pickUps.add(stop.request);
      }
    } else if (stop.type == Type.DROP_OFF) {
      if (!pickUps.contains(stop.request)) {
        failed = true;
      } else {
      }
    }
    lastStop = stop;
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
