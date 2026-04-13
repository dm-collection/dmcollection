package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.tables.CardSideCardType.CARD_SIDE_CARD_TYPE;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.selectDistinct;
import static org.jooq.impl.DSL.selectOne;

import java.util.Set;
import org.jooq.Condition;

public class CardTypeConditionBuilder {

  private CardTypeConditionBuilder() {}

  public static Condition build(Set<Integer> includedIds, Set<Integer> excludedIds) {
    if (includedIds.isEmpty() && excludedIds.isEmpty()) {
      return noCondition();
    }

    Condition sidePredicate = noCondition();

    if (!includedIds.isEmpty()) {
      sidePredicate =
          sidePredicate.and(
              exists(
                  selectOne()
                      .from(CARD_SIDE_CARD_TYPE)
                      .where(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
                      .and(CARD_SIDE_CARD_TYPE.CARD_TYPE_ID.in(includedIds))));
    }

    if (!excludedIds.isEmpty()) {
      sidePredicate =
          sidePredicate.and(
              notExists(
                  selectOne()
                      .from(CARD_SIDE_CARD_TYPE)
                      .where(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
                      .and(CARD_SIDE_CARD_TYPE.CARD_TYPE_ID.in(excludedIds))));
    }

    return CARD.ID.in(selectDistinct(CARD_SIDE.CARD_ID).from(CARD_SIDE).where(sidePredicate));
  }
}
