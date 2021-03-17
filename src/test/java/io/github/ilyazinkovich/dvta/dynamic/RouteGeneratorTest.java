package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.NO_PICK_UP_FOR_DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.PICK_UP_AFTER_TIME_WINDOW_END;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.PICK_UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteGeneratorTest {

  private static final DrivingTimeMatrix EMPTY_DRIVING_TIME_MATRIX =
      (origin, destination) -> Duration.ZERO;

  @Test
  public void oneDropOff() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, Type.DROP_OFF));
    assertTrue(routeGenerator.failed());
    assertEquals(NO_PICK_UP_FOR_DROP_OFF, routeGenerator.failureReason());
  }

  @Test
  public void failureAfterFailure() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
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
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Duration delay = Duration.ofMinutes(1L);
    Instant pickUpTimeWindowEnd = time.minus(delay);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        null, pickUpTimeWindowEnd, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertTrue(routeGenerator.failed());
    assertEquals(PICK_UP_AFTER_TIME_WINDOW_END, routeGenerator.failureReason());
  }

  @Test
  public void onePickUpAtTimeWindowStart() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Instant pickUpTimeWindowStart = time;
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        pickUpTimeWindowStart, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO), routeGenerator.pickUpExtraWait());
    assertEquals(time, routeGenerator.time());
  }

  @Test
  public void onePickUpBeforeTimeWindowStart() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Duration extraWait = Duration.ofMinutes(1L);
    Instant pickUpTimeWindowStart = time.plus(extraWait);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        pickUpTimeWindowStart, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(extraWait), routeGenerator.pickUpExtraWait());
    assertEquals(pickUpTimeWindowStart, routeGenerator.time());
  }

  @Test
  public void onePickUpAfterTimeWindowStart() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Duration extraDelay = Duration.ofMinutes(1L);
    Instant pickUpTimeWindowStart = time.minus(extraDelay);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        pickUpTimeWindowStart, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO), routeGenerator.pickUpExtraWait());
    assertEquals(time, routeGenerator.time());
  }

  @Test
  public void onePickUpWithServiceTime() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Duration serviceTime = Duration.ofMinutes(10);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        null, null, null, serviceTime, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(serviceTime, routeGenerator.serviceTime());
    assertEquals(time, routeGenerator.time());
  }

  @Test
  public void twoPickUpsAtSameLocationAtTimeWindowStart() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Integer pickUpLocationId = 1;
    Instant pickUpTimeWindowStart = time;
    Request request1 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart, null, null, null, null, null, null, null, null);
    Request request2 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart, null, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.pickUpExtraWait());
    assertEquals(time, routeGenerator.time());
  }

  @Test
  public void twoPickUpsAtSameLocationAtTimeWindowStartFirstServiceTimeDominates() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Integer pickUpLocationId = 1;
    Instant pickUpTimeWindowStart = time;
    Duration serviceTime1 = Duration.ofMinutes(5);
    Request request1 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart, null, null, serviceTime1, null, null, null, null, null);
    Duration serviceTime2 = serviceTime1.minus(Duration.ofMinutes(2));
    Request request2 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart, null, null, serviceTime2, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.pickUpExtraWait());
    assertEquals(serviceTime1, routeGenerator.serviceTime());
    assertEquals(time, routeGenerator.time());
  }

  @Test
  public void twoPickUpsAtSameLocationAtTimeWindowStartSecondServiceTimeDominates() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Integer pickUpLocationId = 1;
    Instant pickUpTimeWindowStart = time;
    Duration serviceTime1 = Duration.ofMinutes(5);
    Request request1 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart, null, null, serviceTime1, null, null, null, null, null);
    Duration serviceTime2 = serviceTime1.plus(Duration.ofMinutes(2));
    Request request2 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart, null, null, serviceTime2, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.pickUpExtraWait());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
    assertEquals(time, routeGenerator.time());
  }

  @Test
  public void twoPickUpsAtSameLocationSecondTimeWindowStartBeforeFirstProjectedDeparture() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Integer pickUpLocationId = 1;
    Instant pickUpTimeWindowStart1 = time;
    Duration serviceTime1 = Duration.ofMinutes(5);
    Request request1 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart1, null, null, serviceTime1, null, null, null, null, null);
    Duration commonWait = Duration.ofMinutes(2);
    Instant pickUpTimeWindowStart2 = pickUpTimeWindowStart1.plus(serviceTime1).minus(commonWait);
    Duration serviceTime2 = Duration.ofMinutes(3);
    Request request2 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart2, null, null, serviceTime2, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.pickUpExtraWait());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
    assertEquals(pickUpTimeWindowStart2, routeGenerator.time());
  }

  @Test
  public void twoPickUpsAtSameLocationSecondTimeWindowStartAfterFirstProjectedDeparture() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Integer pickUpLocationId = 1;
    Instant pickUpTimeWindowStart1 = time;
    Duration serviceTime1 = Duration.ofMinutes(5);
    Request request1 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart1, null, null, serviceTime1, null, null, null, null, null);
    Duration extraWait = Duration.ofMinutes(2);
    Instant pickUpTimeWindowStart2 = pickUpTimeWindowStart1.plus(serviceTime1).plus(extraWait);
    Duration serviceTime2 = Duration.ofMinutes(3);
    Request request2 = new Request(UUID.randomUUID().toString(), null, pickUpLocationId, null, null,
        null, null, pickUpTimeWindowStart2, null, null, serviceTime2, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, extraWait), routeGenerator.pickUpExtraWait());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
    assertEquals(pickUpTimeWindowStart2, routeGenerator.time());
  }

  @Test
  public void twoPickUpsAtDifferentLocationsArriveAtSecondAfterWindowEnd() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    Integer pickUpLocationId1 = 1;
    LatLng pickUpLocation1 = new LatLng(40.699161529541016, -73.985969543457031);
    Integer pickUpLocationId2 = 2;
    LatLng pickUpLocation2 = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation1), eq(pickUpLocation2)))
        .thenReturn(drivingTime);
    Instant pickUpTimeWindowStart1 = time;
    Duration serviceTime1 = Duration.ofMinutes(5);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation1, pickUpLocationId1,
        null, null, null, null, pickUpTimeWindowStart1, null, null, serviceTime1, null, null, null,
        null, null);
    Duration late = Duration.ofMinutes(2);
    Instant pickUpTimeWindowEnd =
        pickUpTimeWindowStart1.plus(serviceTime1).plus(drivingTime).minus(late);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation2, pickUpLocationId2,
        null, null, null, null, null, pickUpTimeWindowEnd, null, null, null, null, null,
        null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertTrue(routeGenerator.failed());
    assertEquals(PICK_UP_AFTER_TIME_WINDOW_END, routeGenerator.failureReason());
  }

  @Test
  public void twoPickUpsAtDifferentLocationsArriveAtSecondBeforeWindowStart() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    Integer pickUpLocationId1 = 1;
    LatLng pickUpLocation1 = new LatLng(40.699161529541016, -73.985969543457031);
    Integer pickUpLocationId2 = 2;
    LatLng pickUpLocation2 = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation1), eq(pickUpLocation2)))
        .thenReturn(drivingTime);
    Instant pickUpTimeWindowStart1 = time;
    Duration serviceTime1 = Duration.ofMinutes(5);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation1, pickUpLocationId1,
        null, null, null, null, pickUpTimeWindowStart1, null, null, serviceTime1, null, null, null,
        null, null);
    Duration extraWait = Duration.ofMinutes(2);
    Instant pickUpTimeWindowStart2 =
        pickUpTimeWindowStart1.plus(serviceTime1).plus(drivingTime).plus(extraWait);
    Duration serviceTime2 = Duration.ofMinutes(3);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation2, pickUpLocationId2,
        null, null, null, null, pickUpTimeWindowStart2, null, null, serviceTime2, null, null, null,
        null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, extraWait), routeGenerator.pickUpExtraWait());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
    assertEquals(pickUpTimeWindowStart2, routeGenerator.time());
  }

  @Test
  public void twoPickUpsAtDifferentLocationsArriveAtSecondAfterWindowStart() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    Integer pickUpLocationId1 = 1;
    LatLng pickUpLocation1 = new LatLng(40.699161529541016, -73.985969543457031);
    Integer pickUpLocationId2 = 2;
    LatLng pickUpLocation2 = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation1), eq(pickUpLocation2)))
        .thenReturn(drivingTime);
    Instant pickUpTimeWindowStart1 = time;
    Duration serviceTime1 = Duration.ofMinutes(5);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation1, pickUpLocationId1,
        null, null, null, null, pickUpTimeWindowStart1, null, null, serviceTime1, null, null, null,
        null, null);
    Duration extraDelay = Duration.ofMinutes(2);
    Instant arrivalTimeAtPickUpLocation2 =
        pickUpTimeWindowStart1.plus(serviceTime1).plus(drivingTime);
    Instant pickUpTimeWindowStart2 = arrivalTimeAtPickUpLocation2.minus(extraDelay);
    Duration serviceTime2 = Duration.ofMinutes(3);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation2, pickUpLocationId2,
        null, null, null, null, pickUpTimeWindowStart2, null, null, serviceTime2, null, null, null,
        null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.pickUpExtraWait());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
    assertEquals(arrivalTimeAtPickUpLocation2, routeGenerator.time());
  }
}
