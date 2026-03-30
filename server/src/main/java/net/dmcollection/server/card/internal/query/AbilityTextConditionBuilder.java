package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Ability.ABILITY;
import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.tables.PrintingSide.PRINTING_SIDE;
import static net.dmcollection.server.jooq.generated.tables.PrintingSideAbility.PRINTING_SIDE_ABILITY;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.selectDistinct;

import org.jooq.Condition;

public class AbilityTextConditionBuilder {

  public static Condition build(String abilityTextSearch) {
    if (abilityTextSearch == null || abilityTextSearch.isEmpty()) {
      return noCondition();
    }

    return CARD.ID.in(
        selectDistinct(CARD_SIDE.CARD_ID)
            .from(CARD_SIDE)
            .join(PRINTING_SIDE)
            .on(PRINTING_SIDE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
            .join(PRINTING_SIDE_ABILITY)
            .on(PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID.eq(PRINTING_SIDE.ID))
            .join(ABILITY)
            .on(ABILITY.ID.eq(PRINTING_SIDE_ABILITY.ABILITY_ID))
            .where(ABILITY.SEARCH_TEXT.containsIgnoreCase(abilityTextSearch)));
  }
}
