package net.dmcollection.model.card;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class CardCost implements Comparable<CardCost> {

  public static final CardCost NO_COST = new CardCost(null); // Represents a card with no cost
  public static final CardCost INFINITY = new CardCost(Integer.MAX_VALUE); // Represents infinity
  public static final String INFINITY_SYMBOL = "∞";
  private static final Logger log = LoggerFactory.getLogger(CardCost.class);

  private final Integer cost; // Can be null for NO_COST

  private CardCost(Integer cost) {
    this.cost = cost;
  }

  public static CardCost parseCost(Integer cost) {
    if (cost == null) return NO_COST;
    if (cost == Integer.MAX_VALUE) return INFINITY;
    return new CardCost(cost);
  }

  public static CardCost parseCost(String cost) {
    if (cost == null || cost.isBlank()) {
      return NO_COST;
    }
    cost = cost.trim();
    if (INFINITY_SYMBOL.equals(cost)) {
      return INFINITY;
    }
    try {
      int costValue = Integer.parseInt(cost);
      return parseCost(costValue);
    } catch (NumberFormatException e) {
      log.warn("Unexpected cost value: {}", cost);
      return NO_COST;
    }
  }

  public Integer value() {
    return cost;
  }

  public boolean isNoCost() {
    return cost == null;
  }

  public boolean isInfinity() {
    return cost == Integer.MAX_VALUE;
  }

  @Override
  public int compareTo(@NonNull CardCost other) {
    if (this.isNoCost() && !other.isNoCost()) {
      return -1; // NO_COST is considered the lowest
    } else if (!this.isNoCost() && other.isNoCost()) {
      return 1; // Any cost is considered greater than NO_COST
    }

    if (this.isInfinity() && !other.isInfinity()) {
      return 1; // INFINITY is considered the highest
    } else if (!this.isInfinity() && other.isInfinity()) {
      return -1; // Any cost is considered less than INFINITY
    }

    if (this.isInfinity() && other.isInfinity()) {
      return 0; // Both are INFINITY, considered equal
    }

    return Integer.compare(this.cost, other.cost); // Compare based on the cost value
  }

  @Override
  public String toString() {
    if (isNoCost()) {
      return null;
    } else if (isInfinity()) {
      return INFINITY_SYMBOL;
    } else {
      return String.valueOf(cost);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CardCost cardCost = (CardCost) o;
    return Objects.equals(cost, cardCost.cost);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cost);
  }
}
