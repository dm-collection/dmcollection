package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.tables.CardSideCardType.CARD_SIDE_CARD_TYPE;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.selectDistinct;

import java.util.Set;
import org.jooq.Condition;

public class CardTypeConditionBuilder {

  public static Condition build(Set<Integer> includedIds, Set<Integer> excludedIds) {
    if (includedIds.isEmpty() && excludedIds.isEmpty()) {
      return noCondition();
    }

    Condition condition = noCondition();

    if (!includedIds.isEmpty()) {
      condition =
          condition.and(
              CARD.ID.in(
                  selectDistinct(CARD_SIDE.CARD_ID)
                      .from(CARD_SIDE)
                      .join(CARD_SIDE_CARD_TYPE)
                      .on(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
                      .where(CARD_SIDE_CARD_TYPE.CARD_TYPE_ID.in(includedIds))));
    }

    if (!excludedIds.isEmpty()) {
      condition =
          condition.and(
              CARD.ID.notIn(
                  selectDistinct(CARD_SIDE.CARD_ID)
                      .from(CARD_SIDE)
                      .join(CARD_SIDE_CARD_TYPE)
                      .on(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
                      .where(CARD_SIDE_CARD_TYPE.CARD_TYPE_ID.in(excludedIds))));
    }

    return condition;
  }
}
