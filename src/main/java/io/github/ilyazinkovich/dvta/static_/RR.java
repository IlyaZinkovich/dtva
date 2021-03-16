package io.github.ilyazinkovich.dvta.static_;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RR {

  public final Map<Request, Map<Request, Double>> requestToRequestCost;

  public RR(Map<Request, Map<Request, Double>> requestToRequestCost) {
    this.requestToRequestCost = requestToRequestCost;
  }

  public static RR create(List<Request> requests) {
    RR rr = new RR(new HashMap<>());
    for (Request r1 : requests) {
      for (Request r2 : requests) {
        if (!r1.equals(r2)) {
          Double cost = match(r1, r2);
          if (cost != null) {
            rr.addRequestToRequest(r1, r2, cost);
          }
        }
      }
    }
    return rr;
  }

  private static Double match(Request r1, Request r2) {
    Instant pickUpTime2 = r1.requestTime
        .plus(Routing.drivingTime(r1.origin, r2.origin));
    if (pickUpTime2.isAfter(r2.latestAcceptablePickUpTime)
        || pickUpTime2.isBefore(r2.requestTime)) {
      return null;
    }
    Double cost = null;
    Instant firstDropOffTime1 = pickUpTime2
        .plus(Routing.drivingTime(r2.origin, r1.destination));
    Instant firstDropOffTime2 = firstDropOffTime1
        .plus(Routing.drivingTime(r1.destination, r2.destination));
    if (firstDropOffTime1.isBefore(r1.earliestPossibleDropOffTime.plus(r1.maxToleratedDelay))
        && firstDropOffTime2.isBefore(r2.earliestPossibleDropOffTime.plus(r2.maxToleratedDelay))) {
      cost = rrCost(r1, r2, firstDropOffTime1, firstDropOffTime2);
    }
    Instant secondDropOffTime2 = pickUpTime2
        .plus(Routing.drivingTime(r2.origin, r2.destination));
    Instant secondDropOffTime1 = secondDropOffTime2
        .plus(Routing.drivingTime(r2.destination, r1.destination));
    if (secondDropOffTime2.isBefore(r2.earliestPossibleDropOffTime.plus(r2.maxToleratedDelay))
        && secondDropOffTime1.isBefore(r1.earliestPossibleDropOffTime.plus(r1.maxToleratedDelay))) {
      double alternativeCost = rrCost(r1, r2, secondDropOffTime1, secondDropOffTime2);
      if (cost != null) {
        cost = Math.min(cost, alternativeCost);
      } else {
        cost = alternativeCost;
      }
    }
    return cost;
  }

  private static double rrCost(Request r1, Request r2, Instant dropOffTime1, Instant dropOffTime2) {
    return (double) Duration.between(r1.earliestPossibleDropOffTime, dropOffTime1)
        .plus(Duration.between(r2.earliestPossibleDropOffTime, dropOffTime2))
        .toMillis();
  }

  private void addRequestToRequest(Request r1, Request r2, Double cost) {
    if (!requestToRequestCost.containsKey(r1)) {
      requestToRequestCost.put(r1, new HashMap<>());
    }
    Map<Request, Double> r1r2 = requestToRequestCost.get(r1);
    if (r1r2.containsKey(r2)) {
      r1r2.put(r2, Math.min(r1r2.get(r2), cost));
    } else {
      r1r2.put(r2, cost);
    }
    if (!requestToRequestCost.containsKey(r2)) {
      requestToRequestCost.put(r2, new HashMap<>());
    }
    Map<Request, Double> r2r1 = requestToRequestCost.get(r2);
    if (r2r1.containsKey(r2)) {
      r2r1.put(r1, Math.min(r2r1.get(r1), cost));
    } else {
      r2r1.put(r1, cost);
    }
  }
}
