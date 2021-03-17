package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.NO_PICK_UP_FOR_DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.PICK_UP_AFTER_TIME_WINDOW_END;

import io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

public class RouteGenerator {

  private boolean failed;
  private FailureReason failureReason;
  private final LinkedList<RouteStop> stops;
  private final Set<Request> pickUps;
  private Instant time;
  private Duration queueTime;
  private Duration serviceTime;
  private final LinkedList<Duration> pickUpDeviations;
  private final LinkedList<Duration> dropOffDeviations;

  RouteGenerator(Instant time) {
    this.failed = false;
    this.stops = new LinkedList<>();
    this.pickUps = new HashSet<>();
    this.time = time;
    this.queueTime = Duration.ZERO;
    this.serviceTime = Duration.ZERO;
    this.pickUpDeviations = new LinkedList<>();
    this.dropOffDeviations = new LinkedList<>();
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
          failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
          return;
        }
        if (stop.request.pickUpTimeWindowStart != null) {
          pickUpDeviations.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
          if (stop.request.pickUpTimeWindowStart.isAfter(time)) {
            time = stop.request.pickUpTimeWindowStart;
          }
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
            Instant secondProjectedDeparture = max(time, stop.request.pickUpTimeWindowStart)
                .plus(stop.request.pickUpServiceTime);
            if (secondProjectedDeparture.isAfter(firstProjectedDeparture)) {
              time = max(time, stop.request.pickUpTimeWindowStart);
              serviceTime = stop.request.pickUpServiceTime;
            }
            if (firstProjectedDeparture.isBefore(stop.request.pickUpTimeWindowStart)) {
              pickUpDeviations.add(
                  Duration.between(firstProjectedDeparture, stop.request.pickUpTimeWindowStart));
            }
          } else if (stop.request.pickUpQueueTime != null) {
            if (queueTime.isZero()) {
              pickUpDeviations.add(queueTime);
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
          if (stop.request.pickUpTimeWindowStart != null
              && stop.request.pickUpTimeWindowStart.isAfter(time)) {
            pickUpDeviations.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
            time = stop.request.pickUpTimeWindowStart;
          } else if (stop.request.pickUpQueueTime != null) {
            queueTime = stop.request.pickUpQueueTime;
          }
          serviceTime = stop.request.pickUpServiceTime;
        } else if (stops.getLast().type == Type.DROP_OFF) {
          time = time.plus(queueTime).plus(serviceTime)
              .plus(Routing.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.dropOffTimeWindowEnd != null
              && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
            failed = true;
            return;
          }
          if (stop.request.pickUpTimeWindowStart != null
              && stop.request.pickUpTimeWindowStart.isAfter(time)) {
            pickUpDeviations.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
            time = stop.request.pickUpTimeWindowStart;
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
        failureReason = NO_PICK_UP_FOR_DROP_OFF;
        return;
      } else {
        boolean sameDropOffLocation =
            Objects.equals(stops.getLast().request.dropOffLocation, stop.request.dropOffLocation);
        if (stops.getLast().type == Type.DROP_OFF && sameDropOffLocation) {
          if (stop.request.dropOffTimeWindowEnd != null
              && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
            failed = true;
            return;
          }
          if (stop.request.dropOffTimeWindowStart != null) {
            Instant firstProjectedDeparture = time.plus(serviceTime);
            Instant secondProjectedDeparture = max(time, stop.request.dropOffTimeWindowStart)
                .plus(stop.request.dropOffServiceTime);
            if (secondProjectedDeparture.isAfter(firstProjectedDeparture)) {
              time = max(time, stop.request.dropOffTimeWindowStart);
              serviceTime = stop.request.dropOffServiceTime;
            }
            if (firstProjectedDeparture.isBefore(stop.request.dropOffTimeWindowStart)) {
              pickUpDeviations.add(
                  Duration.between(firstProjectedDeparture, stop.request.dropOffTimeWindowStart));
            }
          }
          time = max(time, stop.request.dropOffTimeWindowStart);
          serviceTime = max(serviceTime, stop.request.dropOffServiceTime);
        } else if (stops.getLast().type == Type.DROP_OFF && !sameDropOffLocation) {
          time = time.plus(queueTime).plus(serviceTime)
              .plus(Routing.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.dropOffTimeWindowEnd != null
              && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
            failed = true;
            return;
          }
          if (stop.request.dropOffTimeWindowStart != null
              && stop.request.dropOffTimeWindowStart.isAfter(time)) {
            pickUpDeviations.add(Duration.between(time, stop.request.dropOffTimeWindowStart));
            time = stop.request.dropOffTimeWindowStart;
          }
          queueTime = Duration.ZERO;
          serviceTime = stop.request.dropOffServiceTime;
        } else if (stops.getLast().type == Type.PICK_UP) {
          time = time.plus(queueTime).plus(serviceTime)
              .plus(Routing.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.dropOffTimeWindowEnd != null
              && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
            failed = true;
            return;
          }
          if (stop.request.dropOffTimeWindowStart != null
              && stop.request.dropOffTimeWindowStart.isAfter(time)) {
            pickUpDeviations.add(Duration.between(time, stop.request.dropOffTimeWindowStart));
            time = stop.request.dropOffTimeWindowStart;
          }
          serviceTime = stop.request.dropOffServiceTime;
        }
        dropOffDeviations.add(Duration.between(stop.request.dropOffTimeTarget, time));
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

  boolean failed() {
    return failed;
  }

  FailureReason failureReason() {
    return failureReason;
  }

  LinkedList<Duration> pickUpDeviations() {
    return pickUpDeviations;
  }

  Duration queueTime() {
    return queueTime;
  }

  Duration serviceTime() {
    return serviceTime;
  }

  enum FailureReason {
    NO_PICK_UP_FOR_DROP_OFF, PICK_UP_AFTER_TIME_WINDOW_END
  }
}
