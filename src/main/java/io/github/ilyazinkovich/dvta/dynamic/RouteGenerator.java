package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.DROP_OFF_AFTER_TIME_WINDOW_END;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.NO_PICK_UP_FOR_DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.PICK_UP_AFTER_TIME_WINDOW_END;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.State.DROP;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.State.FAILED;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.State.INITIAL;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.State.PICK;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.PICK_UP;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

public class RouteGenerator {

  private FailureReason failureReason;
  private final LinkedList<RouteStop> stops;
  private final Set<Request> pickUps;
  private Instant time;
  private Duration serviceTime;
  private final LinkedList<Duration> extraWait;
  private final LinkedList<Duration> dropOffDelays;
  private final DrivingTimeMatrix drivingTimeMatrix;
  private State state;

  RouteGenerator(Instant time, DrivingTimeMatrix drivingTimeMatrix) {
    this.stops = new LinkedList<>();
    this.pickUps = new HashSet<>();
    this.time = time;
    this.serviceTime = Duration.ZERO;
    this.extraWait = new LinkedList<>();
    this.dropOffDelays = new LinkedList<>();
    this.drivingTimeMatrix = drivingTimeMatrix;
    this.state = INITIAL;
  }

  public void add(RouteStop stop) {
    if (state == FAILED) {
      return;
    } else if (state == INITIAL) {
      initial(stop);
    } else if (state == PICK) {
      if (stop.type == PICK_UP) {
        if (sameLocation(stops.getLast().request.pickUpLocationId, stop.request.pickUpLocationId)) {
          pickSameLocation(stop);
        } else {
          pickDifferentLocation(stop);
        }
      } else if (stop.type == DROP_OFF) {
        drop(stop);
      }
    } else if (state == DROP) {
      if (stop.type == PICK_UP) {
        pick(stop);
      } else if (stop.type == DROP_OFF) {
        if (sameLocation(stops.getLast().request.dropOffLocationId,
            stop.request.dropOffLocationId)) {
          dropSameLocation(stop);
        } else {
          dropDifferentLocation(stop);
        }
        calculateDropOffDelay(stop);
      }
    }
    stops.add(stop);
  }

  private void initial(RouteStop stop) {
    if (stop.type == PICK_UP) {
      serviceTime = stop.request.pickUpServiceTime;
      if (stop.request.pickUpTimeWindowEnd != null
          && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
        state = FAILED;
        failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
      } else if (stop.request.pickUpTimeWindowStart != null) {
        if (time.isBefore(stop.request.pickUpTimeWindowStart)) {
          extraWait.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
        } else {
          extraWait.add(Duration.ZERO);
        }
        if (stop.request.pickUpTimeWindowStart.isAfter(time)) {
          time = stop.request.pickUpTimeWindowStart;
        }
        state = PICK;
        pickUps.add(stop.request);
      }
    } else if (stop.type == DROP_OFF) {
      state = FAILED;
      failureReason = NO_PICK_UP_FOR_DROP_OFF;
    }
  }

  private void pickSameLocation(RouteStop stop) {
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
      state = PICK;
      pickUps.add(stop.request);
    }
  }

  private void pickDifferentLocation(RouteStop stop) {
    time = time.plus(serviceTime)
        .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
    if (stop.request.pickUpTimeWindowEnd != null
        && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
      state = FAILED;
      failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
    } else {
      if (stop.request.pickUpTimeWindowStart != null) {
        serviceTime = stop.request.pickUpServiceTime;
        if (time.isBefore(stop.request.pickUpTimeWindowStart)) {
          extraWait.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
        } else {
          extraWait.add(Duration.ZERO);
        }
        if (stop.request.pickUpTimeWindowStart.isAfter(time)) {
          time = stop.request.pickUpTimeWindowStart;
        }
        state = PICK;
        pickUps.add(stop.request);
      }
    }
  }

  private void drop(RouteStop stop) {
    if (!pickUps.contains(stop.request)) {
      state = FAILED;
      failureReason = NO_PICK_UP_FOR_DROP_OFF;
    } else {
      time = time.plus(serviceTime)
          .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
      if (stop.request.dropOffTimeWindowEnd != null
          && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
        state = FAILED;
        failureReason = DROP_OFF_AFTER_TIME_WINDOW_END;
      } else if (stop.request.dropOffTimeWindowStart != null) {
        if (time.isBefore(stop.request.dropOffTimeWindowStart)) {
          extraWait.add(Duration.between(time, stop.request.dropOffTimeWindowStart));
        } else {
          extraWait.add(Duration.ZERO);
        }
        if (stop.request.dropOffTimeWindowStart.isAfter(time)) {
          time = stop.request.dropOffTimeWindowStart;
        }
        serviceTime = stop.request.dropOffServiceTime;
        state = DROP;
        calculateDropOffDelay(stop);
      }
    }
  }

  private void dropSameLocation(RouteStop stop) {
    if (stop.request.dropOffTimeWindowEnd != null
        && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
      state = FAILED;
      failureReason = DROP_OFF_AFTER_TIME_WINDOW_END;
    } else if (stop.request.dropOffTimeWindowStart != null) {
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
      state = DROP;
    }
  }

  private void dropDifferentLocation(RouteStop stop) {
    time = time.plus(serviceTime)
        .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
    if (stop.request.dropOffTimeWindowEnd != null
        && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
      state = FAILED;
      failureReason = DROP_OFF_AFTER_TIME_WINDOW_END;
    } else if (stop.request.dropOffTimeWindowStart != null) {
      if (time.isBefore(stop.request.dropOffTimeWindowStart)) {
        extraWait.add(Duration.between(time, stop.request.dropOffTimeWindowStart));
      } else {
        extraWait.add(Duration.ZERO);
      }
      if (stop.request.dropOffTimeWindowStart.isAfter(time)) {
        time = stop.request.dropOffTimeWindowStart;
      }
      serviceTime = stop.request.dropOffServiceTime;
      state = DROP;
    }
  }

  private void calculateDropOffDelay(RouteStop stop) {
    if (stop.request.dropOffTimeTarget != null
        && time.plus(serviceTime).isAfter(stop.request.dropOffTimeTarget)) {
      dropOffDelays.add(
          Duration.between(stop.request.dropOffTimeTarget, time.plus(serviceTime)));
    } else {
      dropOffDelays.add(Duration.ZERO);
    }
  }

  private void pick(RouteStop stop) {
    time = time.plus(serviceTime)
        .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
    if (stop.request.pickUpTimeWindowEnd != null
        && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
      state = FAILED;
      failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
    } else if (stop.request.pickUpTimeWindowStart != null) {
      if (time.isBefore(stop.request.pickUpTimeWindowStart)) {
        extraWait.add(Duration.between(time, stop.request.pickUpTimeWindowStart));
      } else {
        extraWait.add(Duration.ZERO);
      }
      if (stop.request.pickUpTimeWindowStart.isAfter(time)) {
        time = stop.request.pickUpTimeWindowStart;
      }
      serviceTime = stop.request.pickUpServiceTime;
      state = PICK;
      pickUps.add(stop.request);
    }
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

  boolean failed() {
    return state == FAILED;
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

  enum State {
    INITIAL, FAILED, PICK, DROP
  }
}
