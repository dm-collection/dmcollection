package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.tables.CardSideRace.CARD_SIDE_RACE;
import static net.dmcollection.server.jooq.generated.tables.Race.RACE;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.selectDistinct;

import org.jooq.Condition;

public class RaceConditionBuilder {

  private RaceConditionBuilder() {}

  public static Condition build(String raceSearch) {
    if (raceSearch == null || raceSearch.isEmpty()) {
      return noCondition();
    }

    return CARD.ID.in(
        selectDistinct(CARD_SIDE.CARD_ID)
            .from(CARD_SIDE)
            .join(CARD_SIDE_RACE)
            .on(CARD_SIDE_RACE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
            .join(RACE)
            .on(RACE.ID.eq(CARD_SIDE_RACE.RACE_ID))
            .where(RACE.NAME.contains(raceSearch)));
  }
}
