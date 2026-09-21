package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.selectOne;

import org.jooq.Condition;
import org.jooq.Field;

public final class RangeConditionBuilder {

  private RangeConditionBuilder() {}

  public static Condition build(Field<Integer> field, Integer min, Integer max) {
    if (min == null && max == null) {
      return noCondition();
    }

    Condition sideCondition = noCondition();
    if (min != null) {
      sideCondition = sideCondition.and(field.greaterOrEqual(min));
    }
    if (max != null) {
      sideCondition = sideCondition.and(field.lessOrEqual(max));
    }
    return exists(
        selectOne().from(CARD_SIDE).where(CARD_SIDE.CARD_ID.eq(CARD.ID).and(sideCondition)));
  }
}
