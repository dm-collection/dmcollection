package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Rarity.RARITY;
import static org.jooq.impl.DSL.coalesce;

import net.dmcollection.server.card.internal.SearchFilter.Range;
import org.jooq.Condition;
import org.jooq.Field;

public class RarityConditionBuilder {

  public static Condition build(short sortOrder, Range range) {
    Field<Short> effectiveSortOrder = coalesce(RARITY.SORT_ORDER, (short) 0);

    return switch (range) {
      case EQ -> effectiveSortOrder.eq(sortOrder);
      case LE -> effectiveSortOrder.le(sortOrder);
      case GE -> effectiveSortOrder.ge(sortOrder);
    };
  }
}
