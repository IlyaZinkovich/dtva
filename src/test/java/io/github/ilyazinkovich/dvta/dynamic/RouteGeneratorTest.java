package io.github.ilyazinkovich.dvta.dynamic;

import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.DROP_OFF_AFTER_TIME_WINDOW_END;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.NO_PICK_UP_FOR_DROP_OFF;
import static io.github.ilyazinkovich.dvta.dynamic.RouteGenerator.FailureReason.PICK_UP_AFTER_TIME_WINDOW_END;
import static io.github.ilyazinkovich.dvta.dynamic.RouteStop.Type.DROP_OFF;
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
  public void onePickUpAfterWindowEnd() {
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
    Duration serviceTime = Duration.ofMinutes(5);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        pickUpTimeWindowStart, null, null, serviceTime, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO), routeGenerator.extraWait());
    assertEquals(time, routeGenerator.time());
    assertEquals(serviceTime, routeGenerator.serviceTime());
  }

  @Test
  public void onePickUpBeforeTimeWindowStart() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Duration extraWait = Duration.ofMinutes(1L);
    Instant pickUpTimeWindowStart = time.plus(extraWait);
    Duration serviceTime = Duration.ofMinutes(5);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        pickUpTimeWindowStart, null, null, serviceTime, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(extraWait), routeGenerator.extraWait());
    assertEquals(pickUpTimeWindowStart, routeGenerator.time());
    assertEquals(serviceTime, routeGenerator.serviceTime());
  }

  @Test
  public void onePickUpAfterTimeWindowStart() {
    Instant time = Instant.now();
    RouteGenerator routeGenerator = new RouteGenerator(time, EMPTY_DRIVING_TIME_MATRIX);
    Duration extraDelay = Duration.ofMinutes(1L);
    Instant pickUpTimeWindowStart = time.minus(extraDelay);
    Duration serviceTime = Duration.ofMinutes(5);
    Request request = new Request(UUID.randomUUID().toString(), null, null, null, null, null, null,
        pickUpTimeWindowStart, null, null, serviceTime, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO), routeGenerator.extraWait());
    assertEquals(time, routeGenerator.time());
    assertEquals(serviceTime, routeGenerator.serviceTime());
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
    assertEquals(time, routeGenerator.time());
    assertEquals(serviceTime, routeGenerator.serviceTime());
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
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.extraWait());
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
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.extraWait());
    assertEquals(time, routeGenerator.time());
    assertEquals(serviceTime1, routeGenerator.serviceTime());
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
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.extraWait());
    assertEquals(time, routeGenerator.time());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
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
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.extraWait());
    assertEquals(pickUpTimeWindowStart2, routeGenerator.time());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
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
    assertEquals(List.of(Duration.ZERO, extraWait), routeGenerator.extraWait());
    assertEquals(pickUpTimeWindowStart2, routeGenerator.time());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
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
    Duration delay = Duration.ofMinutes(2);
    Instant pickUpTimeWindowEnd =
        pickUpTimeWindowStart1.plus(serviceTime1).plus(drivingTime).minus(delay);
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
    assertEquals(List.of(Duration.ZERO, extraWait), routeGenerator.extraWait());
    assertEquals(pickUpTimeWindowStart2, routeGenerator.time());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
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
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.extraWait());
    assertEquals(arrivalTimeAtPickUpLocation2, routeGenerator.time());
    assertEquals(serviceTime2, routeGenerator.serviceTime());
  }

  @Test
  public void onePickUpOneDropOffAfterWindowEnd() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation = new LatLng(40.699161529541016, -73.985969543457031);
    LatLng dropOffLocation = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation), eq(dropOffLocation)))
        .thenReturn(drivingTime);
    Instant pickUpTimeStart = time;
    Duration pickUpServiceTime = Duration.ofMinutes(5);
    Duration delay = Duration.ofMinutes(1L);
    Instant dropOffTimeWindowEnd =
        pickUpTimeStart.plus(pickUpServiceTime).plus(drivingTime).minus(delay);
    Request request = new Request(UUID.randomUUID().toString(), pickUpLocation, null,
        dropOffLocation, null, null, null, pickUpTimeStart, null, null, pickUpServiceTime, null,
        dropOffTimeWindowEnd, null, null, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    routeGenerator.add(new RouteStop(request, DROP_OFF));
    assertTrue(routeGenerator.failed());
    assertEquals(DROP_OFF_AFTER_TIME_WINDOW_END, routeGenerator.failureReason());
  }

  @Test
  public void onePickUpOneDropOffBeforeWindowStart() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation = new LatLng(40.699161529541016, -73.985969543457031);
    LatLng dropOffLocation = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation), eq(dropOffLocation)))
        .thenReturn(drivingTime);
    Instant pickUpTimeStart = time;
    Duration pickUpServiceTime = Duration.ofMinutes(5);
    Duration extraWait = Duration.ofMinutes(1L);
    Instant arrivalTimeAtDropOff = pickUpTimeStart.plus(pickUpServiceTime).plus(drivingTime);
    Instant dropOffTimeWindowStart = arrivalTimeAtDropOff.plus(extraWait);
    Duration dropOffServiceTime = Duration.ofMinutes(3);
    Instant dropOffTimeTarget = dropOffTimeWindowStart.plus(dropOffServiceTime);
    Request request = new Request(UUID.randomUUID().toString(), pickUpLocation, null,
        dropOffLocation, null, null, null, pickUpTimeStart, null, null, pickUpServiceTime,
        dropOffTimeWindowStart, null, dropOffServiceTime, dropOffTimeTarget, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    routeGenerator.add(new RouteStop(request, DROP_OFF));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, extraWait), routeGenerator.extraWait());
    assertEquals(dropOffTimeWindowStart, routeGenerator.time());
    assertEquals(dropOffServiceTime, routeGenerator.serviceTime());
    assertEquals(List.of(Duration.ZERO), routeGenerator.dropOffDelays());
  }

  @Test
  public void onePickUpOneDropOffAfterWindowStart() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation = new LatLng(40.699161529541016, -73.985969543457031);
    LatLng dropOffLocation = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation), eq(dropOffLocation)))
        .thenReturn(drivingTime);
    Instant pickUpTimeStart = time;
    Duration pickUpServiceTime = Duration.ofMinutes(5);
    Duration extraDelay = Duration.ofMinutes(1L);
    Instant arrivalTimeAtDropOff = pickUpTimeStart.plus(pickUpServiceTime).plus(drivingTime);
    Instant dropOffTimeWindowStart = arrivalTimeAtDropOff.minus(extraDelay);
    Duration dropOffServiceTime = Duration.ofMinutes(3);
    Instant dropOffTimeTarget = dropOffTimeWindowStart.plus(dropOffServiceTime);
    Request request = new Request(UUID.randomUUID().toString(), pickUpLocation, null,
        dropOffLocation, null, null, null, pickUpTimeStart, null, null, pickUpServiceTime,
        dropOffTimeWindowStart, null, dropOffServiceTime, dropOffTimeTarget, null);
    routeGenerator.add(new RouteStop(request, PICK_UP));
    routeGenerator.add(new RouteStop(request, DROP_OFF));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.extraWait());
    assertEquals(arrivalTimeAtDropOff, routeGenerator.time());
    assertEquals(dropOffServiceTime, routeGenerator.serviceTime());
    assertEquals(List.of(extraDelay), routeGenerator.dropOffDelays());
  }

  @Test
  public void pickUpAfterDropOffAfterWindowEnd() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation1 = new LatLng(40.699161529541016, -73.985969543457031);
    LatLng dropOffLocation1 = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime1 = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation1), eq(dropOffLocation1)))
        .thenReturn(drivingTime1);
    LatLng pickUpLocation2 = new LatLng(40.719234466552734, -73.99432373046875);
    Duration drivingTime2 = Duration.ofMinutes(15);
    when(drivingTimeMatrix.drivingTime(eq(dropOffLocation1), eq(pickUpLocation2)))
        .thenReturn(drivingTime2);
    Instant pickUpTimeStart1 = time;
    Duration pickUpServiceTime1 = Duration.ofMinutes(5);
    Instant arrivalTimeAtDropOff1 = pickUpTimeStart1.plus(pickUpServiceTime1).plus(drivingTime1);
    Instant dropOffTimeWindowStart1 = arrivalTimeAtDropOff1;
    Duration dropOffServiceTime1 = Duration.ofMinutes(3);
    Instant dropOffTimeTarget1 = dropOffTimeWindowStart1.plus(dropOffServiceTime1);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation1, null,
        dropOffLocation1, null, null, null, pickUpTimeStart1, null, null, pickUpServiceTime1,
        dropOffTimeWindowStart1, null, dropOffServiceTime1, dropOffTimeTarget1, null);
    Duration delay = Duration.ofMinutes(1);
    Instant arrivalAtPickUp2 = dropOffTimeWindowStart1.plus(dropOffServiceTime1).plus(drivingTime2);
    Instant pickUpTimeWindowEnd2 = arrivalAtPickUp2.minus(delay);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation2, null, null, null,
        null, null, null, pickUpTimeWindowEnd2, null, null, null, null, null, null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request1, DROP_OFF));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertTrue(routeGenerator.failed());
    assertEquals(PICK_UP_AFTER_TIME_WINDOW_END, routeGenerator.failureReason());
  }

  @Test
  public void pickUpAfterDropOffBeforeWindowStart() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation1 = new LatLng(40.699161529541016, -73.985969543457031);
    LatLng dropOffLocation1 = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime1 = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation1), eq(dropOffLocation1)))
        .thenReturn(drivingTime1);
    LatLng pickUpLocation2 = new LatLng(40.719234466552734, -73.99432373046875);
    Duration drivingTime2 = Duration.ofMinutes(15);
    when(drivingTimeMatrix.drivingTime(eq(dropOffLocation1), eq(pickUpLocation2)))
        .thenReturn(drivingTime2);
    Instant pickUpTimeStart1 = time;
    Duration pickUpServiceTime1 = Duration.ofMinutes(5);
    Instant arrivalTimeAtDropOff1 = pickUpTimeStart1.plus(pickUpServiceTime1).plus(drivingTime1);
    Instant dropOffTimeWindowStart1 = arrivalTimeAtDropOff1;
    Duration dropOffServiceTime1 = Duration.ofMinutes(3);
    Instant dropOffTimeTarget1 = dropOffTimeWindowStart1.plus(dropOffServiceTime1);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation1, null,
        dropOffLocation1, null, null, null, pickUpTimeStart1, null, null, pickUpServiceTime1,
        dropOffTimeWindowStart1, null, dropOffServiceTime1, dropOffTimeTarget1, null);
    Duration extraWait = Duration.ofMinutes(2);
    Instant arrivalAtPickUp2 =
        dropOffTimeWindowStart1.plus(dropOffServiceTime1).plus(drivingTime2);
    Instant pickUpTimeWindowStart2 = arrivalAtPickUp2.plus(extraWait);
    Duration pickUpServiceTime2 = Duration.ofMinutes(7);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation2, null, null, null,
        null, null, pickUpTimeWindowStart2, null, null, pickUpServiceTime2, null, null, null, null,
        null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request1, DROP_OFF));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO, extraWait), routeGenerator.extraWait());
    assertEquals(pickUpTimeWindowStart2, routeGenerator.time());
    assertEquals(pickUpServiceTime2, routeGenerator.serviceTime());
    assertEquals(List.of(Duration.ZERO), routeGenerator.dropOffDelays());
  }

  @Test
  public void pickUpAfterDropOffAfterWindowStart() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation1 = new LatLng(40.699161529541016, -73.985969543457031);
    LatLng dropOffLocation1 = new LatLng(40.701595306396484, -74.012008666992188);
    Duration drivingTime1 = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation1), eq(dropOffLocation1)))
        .thenReturn(drivingTime1);
    LatLng pickUpLocation2 = new LatLng(40.719234466552734, -73.99432373046875);
    Duration drivingTime2 = Duration.ofMinutes(15);
    when(drivingTimeMatrix.drivingTime(eq(dropOffLocation1), eq(pickUpLocation2)))
        .thenReturn(drivingTime2);
    Instant pickUpTimeStart1 = time;
    Duration pickUpServiceTime1 = Duration.ofMinutes(5);
    Instant arrivalTimeAtDropOff1 = pickUpTimeStart1.plus(pickUpServiceTime1).plus(drivingTime1);
    Instant dropOffTimeWindowStart1 = arrivalTimeAtDropOff1;
    Duration dropOffServiceTime1 = Duration.ofMinutes(3);
    Instant dropOffTimeTarget1 = dropOffTimeWindowStart1.plus(dropOffServiceTime1);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation1, null,
        dropOffLocation1, null, null, null, pickUpTimeStart1, null, null, pickUpServiceTime1,
        dropOffTimeWindowStart1, null, dropOffServiceTime1, dropOffTimeTarget1, null);
    Duration delay = Duration.ofMinutes(2);
    Instant arrivalAtPickUp2 =
        dropOffTimeWindowStart1.plus(dropOffServiceTime1).plus(drivingTime2);
    Instant pickUpTimeWindowStart2 = arrivalAtPickUp2.minus(delay);
    Duration pickUpServiceTime2 = Duration.ofMinutes(7);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation2, null, null, null,
        null, null, pickUpTimeWindowStart2, null, null, pickUpServiceTime2, null, null, null, null,
        null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request1, DROP_OFF));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO, Duration.ZERO), routeGenerator.extraWait());
    assertEquals(arrivalAtPickUp2, routeGenerator.time());
    assertEquals(pickUpServiceTime2, routeGenerator.serviceTime());
    assertEquals(List.of(Duration.ZERO), routeGenerator.dropOffDelays());
  }

  @Test
  public void twoPickUpsTwoDropOffsAtSameLocationAfterWindowEnd() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation = new LatLng(40.699161529541016, -73.985969543457031);
    Integer pickUpLocationId = 1;
    LatLng dropOffLocation = new LatLng(40.701595306396484, -74.012008666992188);
    Integer dropOffLocationId = 2;
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation), eq(dropOffLocation)))
        .thenReturn(drivingTime);
    Instant pickUpTimeStart1 = time;
    Duration pickUpServiceTime1 = Duration.ofMinutes(5);
    Instant pickUpTimeStart2 = time;
    Duration pickUpServiceTime2 = Duration.ofMinutes(7);
    Instant arrivalTimeAtDropOff = time.plus(pickUpServiceTime2).plus(drivingTime);
    Instant dropOffTimeWindowStart1 = arrivalTimeAtDropOff;
    Duration dropOffServiceTime1 = Duration.ofMinutes(3);
    Instant dropOffTimeTarget1 = dropOffTimeWindowStart1.plus(dropOffServiceTime1);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation, pickUpLocationId,
        dropOffLocation, dropOffLocationId, null, null, pickUpTimeStart1, null, null,
        pickUpServiceTime1, dropOffTimeWindowStart1, null, dropOffServiceTime1, dropOffTimeTarget1,
        null);
    Duration delay = Duration.ofMinutes(2);
    Instant dropOffTimeWindowEnd = arrivalTimeAtDropOff.minus(delay);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation, pickUpLocationId,
        dropOffLocation, dropOffLocationId, null, null, pickUpTimeStart2, null, null,
        pickUpServiceTime2, null, dropOffTimeWindowEnd, null, null, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    routeGenerator.add(new RouteStop(request1, DROP_OFF));
    routeGenerator.add(new RouteStop(request2, DROP_OFF));
    assertTrue(routeGenerator.failed());
    assertEquals(DROP_OFF_AFTER_TIME_WINDOW_END, routeGenerator.failureReason());
  }

  @Test
  public void twoPickUpsTwoDropOffsAtSameLocationBeforeWindowStart() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation = new LatLng(40.699161529541016, -73.985969543457031);
    Integer pickUpLocationId = 1;
    LatLng dropOffLocation = new LatLng(40.701595306396484, -74.012008666992188);
    Integer dropOffLocationId = 2;
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation), eq(dropOffLocation)))
        .thenReturn(drivingTime);
    Instant pickUpTimeStart1 = time;
    Duration pickUpServiceTime1 = Duration.ofMinutes(5);
    Instant pickUpTimeStart2 = time;
    Duration pickUpServiceTime2 = Duration.ofMinutes(7);
    Instant arrivalTimeAtDropOff = time.plus(pickUpServiceTime2).plus(drivingTime);
    Instant dropOffTimeWindowStart1 = arrivalTimeAtDropOff;
    Duration dropOffServiceTime1 = Duration.ofMinutes(3);
    Instant dropOffTimeTarget1 = dropOffTimeWindowStart1.plus(dropOffServiceTime1);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation, pickUpLocationId,
        dropOffLocation, dropOffLocationId, null, null, pickUpTimeStart1, null, null,
        pickUpServiceTime1, dropOffTimeWindowStart1, null, dropOffServiceTime1, dropOffTimeTarget1,
        null);
    Duration extraWait = Duration.ofMinutes(2);
    Instant dropOffTimeWindowStart2 =
        arrivalTimeAtDropOff.plus(dropOffServiceTime1).plus(extraWait);
    Duration dropOffServiceTime2 = Duration.ofMinutes(4);
    Instant dropOffTimeTarget = dropOffTimeWindowStart2.plus(dropOffServiceTime2);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation, pickUpLocationId,
        dropOffLocation, dropOffLocationId, null, null, pickUpTimeStart2, null, null,
        pickUpServiceTime2, dropOffTimeWindowStart2, null, dropOffServiceTime2, dropOffTimeTarget, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    routeGenerator.add(new RouteStop(request1, DROP_OFF));
    routeGenerator.add(new RouteStop(request2, DROP_OFF));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO, Duration.ZERO, extraWait),
        routeGenerator.extraWait());
    assertEquals(dropOffTimeWindowStart2, routeGenerator.time());
    assertEquals(dropOffServiceTime2, routeGenerator.serviceTime());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO), routeGenerator.dropOffDelays());
  }

  @Test
  public void twoPickUpsTwoDropOffsAtSameLocationAfterWindowStart() {
    Instant time = Instant.now();
    DrivingTimeMatrix drivingTimeMatrix = mock(DrivingTimeMatrix.class);
    RouteGenerator routeGenerator = new RouteGenerator(time, drivingTimeMatrix);
    LatLng pickUpLocation = new LatLng(40.699161529541016, -73.985969543457031);
    Integer pickUpLocationId = 1;
    LatLng dropOffLocation = new LatLng(40.701595306396484, -74.012008666992188);
    Integer dropOffLocationId = 2;
    Duration drivingTime = Duration.ofMinutes(10);
    when(drivingTimeMatrix.drivingTime(eq(pickUpLocation), eq(dropOffLocation)))
        .thenReturn(drivingTime);
    Instant pickUpTimeStart1 = time;
    Duration pickUpServiceTime1 = Duration.ofMinutes(5);
    Instant pickUpTimeStart2 = time;
    Duration pickUpServiceTime2 = Duration.ofMinutes(7);
    Instant arrivalTimeAtDropOff = time.plus(pickUpServiceTime2).plus(drivingTime);
    Instant dropOffTimeWindowStart1 = arrivalTimeAtDropOff;
    Duration dropOffServiceTime1 = Duration.ofMinutes(3);
    Instant dropOffTimeTarget1 = dropOffTimeWindowStart1.plus(dropOffServiceTime1);
    Request request1 = new Request(UUID.randomUUID().toString(), pickUpLocation, pickUpLocationId,
        dropOffLocation, dropOffLocationId, null, null, pickUpTimeStart1, null, null,
        pickUpServiceTime1, dropOffTimeWindowStart1, null, dropOffServiceTime1, dropOffTimeTarget1,
        null);
    Duration delay = Duration.ofMinutes(2);
    Instant dropOffTimeWindowStart2 =
        arrivalTimeAtDropOff.minus(delay);
    Duration dropOffServiceTime2 = Duration.ofMinutes(4);
    Instant dropOffTimeTarget = dropOffTimeWindowStart2.plus(dropOffServiceTime2);
    Request request2 = new Request(UUID.randomUUID().toString(), pickUpLocation, pickUpLocationId,
        dropOffLocation, dropOffLocationId, null, null, pickUpTimeStart2, null, null,
        pickUpServiceTime2, dropOffTimeWindowStart2, null, dropOffServiceTime2, dropOffTimeTarget, null);
    routeGenerator.add(new RouteStop(request1, PICK_UP));
    routeGenerator.add(new RouteStop(request2, PICK_UP));
    routeGenerator.add(new RouteStop(request1, DROP_OFF));
    routeGenerator.add(new RouteStop(request2, DROP_OFF));
    assertFalse(routeGenerator.failed());
    assertEquals(List.of(Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO),
        routeGenerator.extraWait());
    assertEquals(dropOffTimeWindowStart1, routeGenerator.time());
    assertEquals(dropOffServiceTime2, routeGenerator.serviceTime());
    assertEquals(List.of(Duration.ZERO, delay), routeGenerator.dropOffDelays());
  }
}
