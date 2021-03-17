package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.DROP_OFF_AFTER_TIME_WINDOW_END;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.NO_PICK_UP_FOR_DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.PICK_UP_AFTER_TIME_WINDOW_END;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.PICK_UP;

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
  private Duration serviceTime;
  private final LinkedList<Duration> extraWait;
  private final LinkedList<Duration> dropOffDelays;
  private final DrivingTimeMatrix drivingTimeMatrix;

  RouteGenerator(Instant time, DrivingTimeMatrix drivingTimeMatrix) {
    this.failed = false;
    this.stops = new LinkedList<>();
    this.pickUps = new HashSet<>();
    this.time = time;
    this.serviceTime = Duration.ZERO;
    this.extraWait = new LinkedList<>();
    this.dropOffDelays = new LinkedList<>();
    this.drivingTimeMatrix = drivingTimeMatrix;
  }

  public void add(RouteStop stop) {
    if (failed) {
      return;
    }
    if (stop.type == PICK_UP) {
      if (stops.isEmpty()) {
        serviceTime = stop.request.pickUpServiceTime;
        if (stop.request.pickUpTimeWindowEnd != null
            && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
          failed = true;
          failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
          return;
        }
        if (stop.request.pickUpTimeWindowStart != null) {
          if (time.isBefore(stop.request.pickUpTimeWindowStart)) {
            extraWait.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
          } else {
            extraWait.add(Duration.ZERO);
          }
          if (stop.request.pickUpTimeWindowStart.isAfter(time)) {
            time = stop.request.pickUpTimeWindowStart;
          }
        }
      } else {
        boolean samePickUpLocation =
            sameLocation(stops.getLast().request.pickUpLocationId, stop.request.pickUpLocationId);
        if (stops.getLast().type == PICK_UP && samePickUpLocation) {
          if (stop.request.pickUpTimeWindowStart != null) {
            Instant firstProjectedDeparture = time.plus(serviceTime);
            if (firstProjectedDeparture.isBefore(stop.request.pickUpTimeWindowStart)) {
              extraWait.add(
                  Duration.between(firstProjectedDeparture, stop.request.pickUpTimeWindowStart));
            } else {
              extraWait.add(Duration.ZERO);
            }
            Instant secondProjectedDeparture = max(time, stop.request.pickUpTimeWindowStart)
                .plus(stop.request.pickUpServiceTime);
            if (secondProjectedDeparture.isAfter(firstProjectedDeparture)) {
              time = max(time, stop.request.pickUpTimeWindowStart);
              serviceTime = stop.request.pickUpServiceTime;
            }
          }
        } else if (stops.getLast().type == PICK_UP && !samePickUpLocation) {
          time = time.plus(serviceTime)
              .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.pickUpTimeWindowEnd != null
              && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
            failed = true;
            failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
            return;
          }
          serviceTime = stop.request.pickUpServiceTime;
          if (stop.request.pickUpTimeWindowStart != null) {
            if (time.isBefore(stop.request.pickUpTimeWindowStart)) {
              extraWait.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
            } else {
              extraWait.add(Duration.ZERO);
            }
            if (stop.request.pickUpTimeWindowStart.isAfter(time)) {
              time = stop.request.pickUpTimeWindowStart;
            }
          }
        } else if (stops.getLast().type == DROP_OFF) {
          time = time.plus(serviceTime)
              .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.pickUpTimeWindowEnd != null
              && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
            failed = true;
            failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
            return;
          }
          if (stop.request.pickUpTimeWindowStart != null) {
            if (time.isBefore(stop.request.pickUpTimeWindowStart)) {
              extraWait.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
            } else {
              extraWait.add(Duration.ZERO);
            }
            if (stop.request.pickUpTimeWindowStart.isAfter(time)) {
              time = stop.request.pickUpTimeWindowStart;
            }
          }
          serviceTime = stop.request.pickUpServiceTime;
        }
      }
      pickUps.add(stop.request);
    } else if (stop.type == DROP_OFF) {
      if (!pickUps.contains(stop.request)) {
        failed = true;
        failureReason = NO_PICK_UP_FOR_DROP_OFF;
        return;
      } else {
        boolean sameDropOffLocation =
            sameLocation(stops.getLast().request.dropOffLocationId, stop.request.dropOffLocationId);
        if (stops.getLast().type == DROP_OFF && sameDropOffLocation) {
          if (stop.request.dropOffTimeWindowEnd != null
              && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
            failed = true;
            failureReason = DROP_OFF_AFTER_TIME_WINDOW_END;
            return;
          }
          if (stop.request.dropOffTimeWindowStart != null) {
            Instant firstProjectedDeparture = time.plus(serviceTime);
            if (firstProjectedDeparture.isBefore(stop.request.dropOffTimeWindowStart)) {
              extraWait.add(
                  Duration.between(firstProjectedDeparture, stop.request.dropOffTimeWindowStart));
            } else {
              extraWait.add(Duration.ZERO);
            }
            Instant secondProjectedDeparture = max(time, stop.request.dropOffTimeWindowStart)
                .plus(stop.request.dropOffServiceTime);
            if (secondProjectedDeparture.isAfter(firstProjectedDeparture)) {
              time = max(time, stop.request.dropOffTimeWindowStart);
              serviceTime = stop.request.dropOffServiceTime;
            }
          }
        } else if (stops.getLast().type == DROP_OFF && !sameDropOffLocation) {
          time = time.plus(serviceTime)
              .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.dropOffTimeWindowEnd != null
              && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
            failed = true;
            failureReason = DROP_OFF_AFTER_TIME_WINDOW_END;
            return;
          }
          if (stop.request.dropOffTimeWindowStart != null) {
            if (time.isBefore(stop.request.dropOffTimeWindowStart)) {
              extraWait.add(Duration.between(time, stop.request.dropOffTimeWindowStart));
            } else {
              extraWait.add(Duration.ZERO);
            }
            if (stop.request.dropOffTimeWindowStart.isAfter(time)) {
              time = stop.request.dropOffTimeWindowStart;
            }
          }
          serviceTime = stop.request.dropOffServiceTime;
        } else if (stops.getLast().type == PICK_UP) {
          time = time.plus(serviceTime)
              .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
          if (stop.request.dropOffTimeWindowEnd != null
              && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
            failed = true;
            failureReason = DROP_OFF_AFTER_TIME_WINDOW_END;
            return;
          }
          if (stop.request.dropOffTimeWindowStart != null) {
            if (time.isBefore(stop.request.dropOffTimeWindowStart)) {
              extraWait.add(Duration.between(time, stop.request.dropOffTimeWindowStart));
            } else {
              extraWait.add(Duration.ZERO);
            }
            if (stop.request.dropOffTimeWindowStart.isAfter(time)) {
              time = stop.request.dropOffTimeWindowStart;
            }
          }
          serviceTime = stop.request.dropOffServiceTime;
        }
        if (stop.request.dropOffTimeTarget != null
            && time.plus(serviceTime).isAfter(stop.request.dropOffTimeTarget)) {
          dropOffDelays.add(
                Duration.between(stop.request.dropOffTimeTarget, time.plus(serviceTime)));
        } else {
          dropOffDelays.add(Duration.ZERO);
        }
      }
    }
    stops.add(stop);
  }

  private boolean sameLocation(Integer left, Integer right) {
    if (left == null) {
      return false;
    }
    return Objects.equals(left, right);
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

  LinkedList<Duration> extraWait() {
    return extraWait;
  }

  Duration serviceTime() {
    return serviceTime;
  }

  Instant time() {
    return time;
  }

  LinkedList<Duration> dropOffDelays() {
    return dropOffDelays;
  }

  enum FailureReason {
    NO_PICK_UP_FOR_DROP_OFF, PICK_UP_AFTER_TIME_WINDOW_END, DROP_OFF_AFTER_TIME_WINDOW_END
  }
}
