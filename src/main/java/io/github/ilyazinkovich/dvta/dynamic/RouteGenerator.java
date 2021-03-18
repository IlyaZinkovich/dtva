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
  private Duration waitTime;
  private Duration serviceTime;
  private final LinkedList<Duration> extraWait;
  private final LinkedList<Duration> dropOffDelays;
  private final DrivingTimeMatrix drivingTimeMatrix;
  private State state;

  RouteGenerator(Instant time, DrivingTimeMatrix drivingTimeMatrix) {
    this.stops = new LinkedList<>();
    this.pickUps = new HashSet<>();
    this.time = time;
    this.waitTime = Duration.ZERO;
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
      initialState(stop);
    } else if (state == PICK) {
      pickState(stop);
    } else if (state == DROP) {
      dropState(stop);
    }
    stops.add(stop);
  }

  private void initialState(RouteStop stop) {
    if (stop.type == PICK_UP) {
      initialPick(stop);
    } else if (stop.type == DROP_OFF) {
      state = FAILED;
      failureReason = NO_PICK_UP_FOR_DROP_OFF;
    }
  }

  private void initialPick(RouteStop stop) {
    if (stop.request.pickUpTimeWindowEnd != null
        && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
      state = FAILED;
      failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
    } else if (stop.request.pickUpTimeWindowStart != null) {
      if (time.isBefore(stop.request.pickUpTimeWindowStart)) {
        waitTime = Duration.between(time, stop.request.pickUpTimeWindowStart);
      } else {
        waitTime = Duration.ZERO;
      }
      extraWait.add(waitTime);
      serviceTime = stop.request.pickUpServiceTime;
      state = PICK;
      pickUps.add(stop.request);
    }
  }

  private void pickState(RouteStop stop) {
    if (stop.type == PICK_UP) {
      if (sameLocation(stops.getLast().request.pickUpLocationId, stop.request.pickUpLocationId)) {
        pickSameLocation(stop);
      } else {
        pickDifferentLocation(stop);
      }
    } else if (stop.type == DROP_OFF) {
      dropDifferentLocation(stop);
    }
  }

  private void pickSameLocation(RouteStop stop) {
    if (stop.request.pickUpTimeWindowEnd != null
        && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
      state = FAILED;
      failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
    } else if (stop.request.pickUpTimeWindowStart != null) {
      Instant firstProjectedDeparture = time.plus(waitTime).plus(serviceTime);
      if (firstProjectedDeparture.isBefore(stop.request.pickUpTimeWindowStart)) {
        time = firstProjectedDeparture;
        waitTime = Duration.between(firstProjectedDeparture, stop.request.pickUpTimeWindowStart);
        extraWait.add(waitTime);
        serviceTime = stop.request.pickUpServiceTime;
      } else {
        Instant secondProjectedDeparture =
            stop.request.pickUpTimeWindowStart.plus(stop.request.pickUpServiceTime);
        if (secondProjectedDeparture.isAfter(firstProjectedDeparture)) {
          Duration extraServiceTime =
              Duration.between(firstProjectedDeparture, secondProjectedDeparture);
          serviceTime = serviceTime.plus(extraServiceTime);
        }
        extraWait.add(Duration.ZERO);
      }
      state = PICK;
      pickUps.add(stop.request);
    }
  }

  private void pickDifferentLocation(RouteStop stop) {
    time = time.plus(waitTime).plus(serviceTime)
        .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
    if (stop.request.pickUpTimeWindowEnd != null
        && time.isAfter(stop.request.pickUpTimeWindowEnd)) {
      state = FAILED;
      failureReason = PICK_UP_AFTER_TIME_WINDOW_END;
    } else if (stop.request.pickUpTimeWindowStart != null) {
      if (time.isBefore(stop.request.pickUpTimeWindowStart)) {
        waitTime = Duration.between(time, stop.request.pickUpTimeWindowStart);
      } else {
        waitTime = Duration.ZERO;
      }
      extraWait.add(waitTime);
      serviceTime = stop.request.pickUpServiceTime;
      state = PICK;
      pickUps.add(stop.request);
    }
  }

  private void dropState(RouteStop stop) {
    if (stop.type == PICK_UP) {
      pickDifferentLocation(stop);
    } else if (stop.type == DROP_OFF) {
      if (sameLocation(stops.getLast().request.dropOffLocationId, stop.request.dropOffLocationId)) {
        dropSameLocation(stop);
      } else {
        dropDifferentLocation(stop);
      }
    }
  }

  private void dropSameLocation(RouteStop stop) {
    if (!pickUps.contains(stop.request)) {
      state = FAILED;
      failureReason = NO_PICK_UP_FOR_DROP_OFF;
    } else if (stop.request.dropOffTimeWindowEnd != null
        && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
      state = FAILED;
      failureReason = DROP_OFF_AFTER_TIME_WINDOW_END;
    } else if (stop.request.dropOffTimeWindowStart != null) {
      Instant firstProjectedDeparture = time.plus(waitTime).plus(serviceTime);
      if (firstProjectedDeparture.isBefore(stop.request.dropOffTimeWindowStart)) {
        time = firstProjectedDeparture;
        waitTime = Duration.between(firstProjectedDeparture, stop.request.dropOffTimeWindowStart);
        extraWait.add(waitTime);
      } else {
        time = stop.request.dropOffTimeWindowStart;
        Instant secondProjectedDeparture =
            stop.request.dropOffTimeWindowStart.plus(stop.request.dropOffServiceTime);
        if (secondProjectedDeparture.isAfter(firstProjectedDeparture)) {
          Duration extraServiceTime =
              Duration.between(firstProjectedDeparture, secondProjectedDeparture);
          serviceTime = serviceTime.plus(extraServiceTime);
        }
        extraWait.add(Duration.ZERO);
      }
      serviceTime = stop.request.dropOffServiceTime;
      state = DROP;
      calculateDropOffDelay(stop);
    }
  }

  private void dropDifferentLocation(RouteStop stop) {
    time = time.plus(waitTime).plus(serviceTime)
        .plus(drivingTimeMatrix.drivingTime(stops.getLast().location(), stop.location()));
    if (!pickUps.contains(stop.request)) {
      state = FAILED;
      failureReason = NO_PICK_UP_FOR_DROP_OFF;
    } else if (stop.request.dropOffTimeWindowEnd != null
        && time.isAfter(stop.request.dropOffTimeWindowEnd)) {
      state = FAILED;
      failureReason = DROP_OFF_AFTER_TIME_WINDOW_END;
    } else if (stop.request.dropOffTimeWindowStart != null) {
      if (time.isBefore(stop.request.dropOffTimeWindowStart)) {
        waitTime = Duration.between(time, stop.request.dropOffTimeWindowStart);
      } else {
        waitTime = Duration.ZERO;
      }
      extraWait.add(waitTime);
      serviceTime = stop.request.dropOffServiceTime;
      state = DROP;
      calculateDropOffDelay(stop);
    }
  }

  private void calculateDropOffDelay(RouteStop stop) {
    Instant departure = time.plus(waitTime).plus(serviceTime);
    if (stop.request.dropOffTimeTarget != null
        && departure.isAfter(stop.request.dropOffTimeTarget)) {
      dropOffDelays.add(Duration.between(stop.request.dropOffTimeTarget, departure));
    } else {
      dropOffDelays.add(Duration.ZERO);
    }
  }

  private boolean sameLocation(Integer left, Integer right) {
    if (left == null) {
      return false;
    }
    return Objects.equals(left, right);
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
