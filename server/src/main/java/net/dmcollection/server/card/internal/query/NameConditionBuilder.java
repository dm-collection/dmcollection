package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.selectDistinct;

import org.jooq.Condition;

public class NameConditionBuilder {

  private static final String TWINPACT_NAME_SEPARATOR = "／";
  private static final String COMBINED_NAME_SEPARATOR = "/";

  private NameConditionBuilder() {}

  public static Condition build(String nameSearch) {
    if (nameSearch == null || nameSearch.isEmpty()) {
      return noCondition();
    }

    Condition sideMatch =
        CARD.ID.in(
            selectDistinct(CARD_SIDE.CARD_ID)
                .from(CARD_SIDE)
                .where(CARD_SIDE.NAME.containsIgnoreCase(nameSearch)));

    if (nameSearch.contains(COMBINED_NAME_SEPARATOR)
        || nameSearch.contains(TWINPACT_NAME_SEPARATOR)) {
      return sideMatch.or(CARD.NAME.containsIgnoreCase(nameSearch));
    }

    return sideMatch;
  }
}
