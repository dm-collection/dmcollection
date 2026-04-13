package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Printing.PRINTING;
import static org.jooq.impl.DSL.noCondition;

import org.jooq.Condition;

public class SetConditionBuilder {

  private SetConditionBuilder() {}

  public static Condition build(Long setId) {
    if (setId == null) {
      return noCondition();
    }
    return PRINTING.SET_ID.eq(setId.intValue());
  }
}
