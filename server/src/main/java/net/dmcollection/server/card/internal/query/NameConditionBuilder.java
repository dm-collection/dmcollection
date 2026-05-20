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
    if (nameSearch.startsWith("\"") && nameSearch.endsWith("\"")) {
      nameSearch = nameSearch.substring(1, nameSearch.length() - 1);
      return CARD.NAME.equalIgnoreCase(nameSearch);
    }
    return CARD.NAME.containsIgnoreCase(nameSearch);
  }
}
