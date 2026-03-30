package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardSet.CARD_SET;
import static net.dmcollection.server.jooq.generated.tables.CollectionEntry.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.tables.Printing.PRINTING;
import static net.dmcollection.server.jooq.generated.tables.Rarity.RARITY;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.val;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Sort;

public class SortBuilder {

  public static List<OrderField<?>> build(Sort sort, UUID userId) {
    List<OrderField<?>> fields = new ArrayList<>();

    if (sort.isUnsorted()) {
      fields.add(CARD_SET.RELEASE_DATE.desc());
      fields.add(PRINTING.OFFICIAL_SITE_ID.asc().nullsLast());
      return fields;
    }

    for (Sort.Order order : sort) {
      Field<? extends Comparable<?>> column =
          switch (order.getProperty()) {
            case "COST", "sort_cost" -> CARD.SORT_COST;
            case "POWER_SORT", "sort_power" -> CARD.SORT_POWER;
            case "ORDER" -> coalesce(RARITY.SORT_ORDER, (short) 0);
            case "RELEASE", "release_date" -> CARD_SET.RELEASE_DATE;
            case "AMOUNT" ->
                userId != null
                    ? coalesce(
                        DSL.select(COLLECTION_ENTRY.QUANTITY)
                            .from(COLLECTION_ENTRY)
                            .where(COLLECTION_ENTRY.USER_ID.eq(userId))
                            .and(COLLECTION_ENTRY.PRINTING_ID.eq(PRINTING.ID))
                            .asField(),
                        0)
                    : val(0);
            case "OFFICIAL_ID", "official_site_id" -> PRINTING.OFFICIAL_SITE_ID;
            default -> null;
          };

      if (column == null) {
        continue;
      }

      SortField<? extends Comparable<?>> sortField =
          order.isAscending()
              ? column.asc().nullsLast()
              : column.desc().nullsLast();
      fields.add(sortField);
    }

    fields.add(PRINTING.OFFICIAL_SITE_ID.asc());
    return fields;
  }
}
