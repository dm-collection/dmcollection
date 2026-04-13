package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.CollectionEntry.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.tables.Printing.PRINTING;
import static org.jooq.impl.DSL.noCondition;

import java.util.UUID;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class CollectionConditionBuilder {

  private CollectionConditionBuilder() {}

  public static Condition build(UUID userId, boolean inCollection) {
    if (userId == null) {
      return noCondition();
    }
    var ownedPrintings =
        DSL.select(COLLECTION_ENTRY.PRINTING_ID)
            .from(COLLECTION_ENTRY)
            .where(COLLECTION_ENTRY.USER_ID.eq(userId));
    return inCollection ? PRINTING.ID.in(ownedPrintings) : PRINTING.ID.notIn(ownedPrintings);
  }
}
