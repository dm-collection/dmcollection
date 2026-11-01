package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static org.jooq.impl.DSL.noCondition;

import org.jooq.Condition;

public class NameConditionBuilder {

  private NameConditionBuilder() {}

  public static Condition build(String nameSearch) {
    if (nameSearch == null || nameSearch.isEmpty()) {
      return noCondition();
    }
    boolean exact = nameSearch.startsWith("\"") && nameSearch.endsWith("\"");
    if (exact) {
      nameSearch = nameSearch.substring(1, nameSearch.length() - 1);
    }
    String normalized = NameNormalizer.normalize(nameSearch);
    // Inputs consisting only of ignored characters can only match the raw name
    if (normalized.isEmpty()) {
      return exact
          ? CARD.NAME.equalIgnoreCase(nameSearch)
          : CARD.NAME.containsIgnoreCase(nameSearch);
    }
    return exact ? matchesExactly(normalized) : matchesPartially(normalized);
  }

  /** Whether the normalized name of the card equals the normalized input. */
  public static Condition matchesExactly(String normalized) {
    return CARD.SEARCH_NAME.eq(normalized);
  }

  /** Whether the normalized name or reading of the card contains the normalized input. */
  public static Condition matchesPartially(String normalized) {
    return CARD.SEARCH_NAME.contains(normalized).or(CARD.SEARCH_READING.contains(normalized));
  }
}
