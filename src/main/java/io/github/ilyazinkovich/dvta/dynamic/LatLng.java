package io.github.ilyazinkovich.dvta.dynamic;

import java.util.Objects;

class LatLng {

  public final double lat;
  public final double lng;

  LatLng(
      double lat,
      double lng
  ) {
    this.lat = lat;
    this.lng = lng;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LatLng latLng = (LatLng) o;
    return Double.compare(latLng.lat, lat) == 0
        && Double.compare(latLng.lng, lng) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lat, lng);
  }

  @Override
  public String toString() {
    return "LatLng{" +
        "lat=" + lat +
        ", lng=" + lng +
        '}';
  }
}
