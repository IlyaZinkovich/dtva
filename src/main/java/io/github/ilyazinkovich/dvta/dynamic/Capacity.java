package io.github.ilyazinkovich.dvta.dynamic;

import java.util.Objects;

class Capacity {

  public final double quantity;
  public final Unit unit;

  Capacity(double quantity, Unit unit) {
    this.quantity = quantity;
    this.unit = unit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Capacity capacity = (Capacity) o;
    return Double.compare(capacity.quantity, quantity) == 0 && unit == capacity.unit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(quantity, unit);
  }

  @Override
  public String toString() {
    return "Capacity{" + quantity + " " + unit.name().toLowerCase() + "}";
  }

  public enum Unit {
    SEAT, KG
  }
}
