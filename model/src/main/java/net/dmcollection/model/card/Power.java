package net.dmcollection.model.card;

import jakarta.annotation.Nonnull;
import java.util.Objects;

public record Power(String value) implements Comparable<Power> {
  private static final String INFINITY_SYMBOL = "∞";
  private static final String PLUS = "+";
  private static final String MINUS = "－";

  public Power {
    Objects.requireNonNull(value);
    if (!INFINITY_SYMBOL.equals(value)) {
      var base = value;
      if (value.endsWith(PLUS) || value.endsWith(MINUS)) {
        base = base.substring(0, base.length() - 1);
      }
      try {
        Integer.parseInt(base);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  public int getNumeric() {
    if (this.isInfinite()) return Integer.MAX_VALUE;
    var base = value;
    if (value.endsWith(PLUS) || value.endsWith(MINUS)) {
      base = base.substring(0, base.length() - 1);
    }
    return Integer.parseInt(base);
  }

  public int getSort() {
    if (this.isInfinite()) return Integer.MAX_VALUE;
    var base = value;
    if (value.endsWith(PLUS) || value.endsWith(MINUS)) {
      base = base.substring(0, base.length() - 1);
    }
    int baseValue = Integer.parseInt(base);
    if (value.endsWith(PLUS)) {
      return baseValue + 1;
    }
    if (value.endsWith(MINUS)) {
      return baseValue - 1;
    }
    return baseValue;
  }

  public boolean isInfinite() {
    return INFINITY_SYMBOL.equals(value);
  }

  @Override
  public int compareTo(@Nonnull Power other) {
    if (other.equals(this)) return 0;
    if (this.isInfinite()) return 1;
    if (other.isInfinite()) return -1;

    int thisBase = Integer.parseInt(this.value.replace(PLUS, "").replace(MINUS, ""));
    int otherBase = Integer.parseInt(other.value.replace(PLUS, "").replace(MINUS, ""));

    if (thisBase == otherBase) {
      if (this.value.endsWith(PLUS)) return 1;
      if (other.value.endsWith(PLUS)) return -1;
      if (this.value.endsWith(MINUS)) return -1;
      if (other.value.endsWith(MINUS)) return 1;
    }
    return Integer.compare(thisBase, otherBase);
  }
}
