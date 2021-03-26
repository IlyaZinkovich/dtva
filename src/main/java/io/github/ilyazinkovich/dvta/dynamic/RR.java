package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.PICK_UP;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

class RR {

  final Set<Set<Request>> pairs;

  RR(Set<Set<Request>> pairs) {
    this.pairs = pairs;
  }

  void add(Request request, Collection<Request> requests, DrivingTimeMatrix drivingTimeMatrix) {
    for (Request r : requests) {
      if (match(r, request, drivingTimeMatrix)) {
        Set<Request> requestPair = new HashSet<>();
        requestPair.add(request);
        requestPair.add(r);
        pairs.add(requestPair);
      }
    }
  }

  private boolean match(Request r1, Request r2, DrivingTimeMatrix drivingTimeMatrix) {
    return Stream.of(
        new RouteGenerator(r1.pickUpTimeWindowStart, drivingTimeMatrix)
            .add(new RouteStop(r1, PICK_UP))
            .add(new RouteStop(r2, PICK_UP))
            .add(new RouteStop(r1, DROP_OFF))
            .add(new RouteStop(r2, DROP_OFF)),
        new RouteGenerator(r1.pickUpTimeWindowStart, drivingTimeMatrix)
            .add(new RouteStop(r1, PICK_UP))
            .add(new RouteStop(r2, PICK_UP))
            .add(new RouteStop(r2, DROP_OFF))
            .add(new RouteStop(r1, DROP_OFF)),
        new RouteGenerator(r1.pickUpTimeWindowStart, drivingTimeMatrix)
            .add(new RouteStop(r1, PICK_UP))
            .add(new RouteStop(r1, DROP_OFF))
            .add(new RouteStop(r2, PICK_UP))
            .add(new RouteStop(r2, DROP_OFF)),
        new RouteGenerator(r2.pickUpTimeWindowStart, drivingTimeMatrix)
            .add(new RouteStop(r2, PICK_UP))
            .add(new RouteStop(r1, PICK_UP))
            .add(new RouteStop(r1, DROP_OFF))
            .add(new RouteStop(r2, DROP_OFF)),
        new RouteGenerator(r2.pickUpTimeWindowStart, drivingTimeMatrix)
            .add(new RouteStop(r2, PICK_UP))
            .add(new RouteStop(r1, PICK_UP))
            .add(new RouteStop(r2, DROP_OFF))
            .add(new RouteStop(r1, DROP_OFF)),
        new RouteGenerator(r2.pickUpTimeWindowStart, drivingTimeMatrix)
            .add(new RouteStop(r2, PICK_UP))
            .add(new RouteStop(r2, DROP_OFF))
            .add(new RouteStop(r1, PICK_UP))
            .add(new RouteStop(r1, DROP_OFF)))
        .anyMatch(generator -> !generator.failed());
  }
}
