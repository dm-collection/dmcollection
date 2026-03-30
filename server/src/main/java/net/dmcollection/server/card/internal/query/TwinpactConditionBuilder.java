package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static org.jooq.impl.DSL.noCondition;

import net.dmcollection.server.card.internal.SearchFilter.FilterState;
import org.jooq.Condition;

public class TwinpactConditionBuilder {

  public static Condition build(FilterState twinpact) {
    return switch (twinpact) {
      case IN -> noCondition();
      case ONLY -> CARD.IS_TWINPACT.isTrue();
      case EX -> CARD.IS_TWINPACT.isFalse();
    };
  }
}
