package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.NO_PICK_UP_FOR_DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.PICK_UP_AFTER_TIME_WINDOW_END;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.PICK_UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteGeneratorTest {

  @Test
  public void oneDropOff() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, Type.DROP_OFF));
    assertTrue(routeGenerator.failed());
    assertEquals(NO_PICK_UP_FOR_DROP_OFF, routeGenerator.failureReason());
  }

  @Test
  public void failureAfterFailure() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, Type.DROP_OFF));
    routeGenerator.add(new RouteStop(request, Type.DROP_OFF));
    assertTrue(routeGenerator.failed());
    assertEquals(NO_PICK_UP_FOR_DROP_OFF, routeGenerator.failureReason());
  }

  @Test
  public void onePickUpAfterTimeWindowEnd() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time);
    Duration delay = Duration.ofMinutes(1L);
    Instant pickUpTimeWindowEnd = time.minus(delay);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        null, pickUpTimeWindowEnd, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertTrue(routeGenerator.failed());
    assertEquals(PICK_UP_AFTER_TIME_WINDOW_END, routeGenerator.failureReason());
  }

  @Test
  public void onePickUpBeforePickUpTimeWindowStart() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time);
    Duration extraWait = Duration.ofMinutes(1L);
    Instant pickUpTimeWindowStart = time.plus(extraWait);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        pickUpTimeWindowStart, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertTrue(routeGenerator.pickUpDeviations().contains(extraWait));
  }

  @Test
  public void onePickUpAfterPickUpTimeWindowStart() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time);
    Duration extraDelay = Duration.ofMinutes(1L);
    Instant pickUpTimeWindowStart = time.minus(extraDelay);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        pickUpTimeWindowStart, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertTrue(routeGenerator.pickUpDeviations().contains(extraDelay.negated()));
  }

  @Test
  public void onePickUpWithQueueTime() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time);
    Duration queueTime = Duration.ofMinutes(10);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        null, null, queueTime, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(queueTime, routeGenerator.queueTime());
  }

  @Test
  public void onePickUpWithServiceTime() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time);
    Duration serviceTime = Duration.ofMinutes(10);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        null, null, null, serviceTime, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(serviceTime, routeGenerator.serviceTime());
  }
}