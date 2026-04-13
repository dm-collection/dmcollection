package net.dmcollection.server.card.serialization;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static net.dmcollection.server.jooq.generated.Tables.CARD;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

@Component
public class CollectionReader {

  private final DSLContext dsl;

  public record Printing(String officialSiteId, int quantity) {}

  public record CollectionEntry(String cardName, List<Printing> printings) {}

  CollectionReader(DSLContext dsl) {
    this.dsl = dsl;
  }

  List<CollectionEntry> readCollection(UUID userId) {
    return dsl.select(
            CARD.NAME,
            multiset(
                    select(PRINTING.OFFICIAL_SITE_ID, COLLECTION_ENTRY.QUANTITY)
                        .from(COLLECTION_ENTRY)
                        .join(PRINTING)
                        .on(PRINTING.ID.eq(COLLECTION_ENTRY.PRINTING_ID))
                        .where(COLLECTION_ENTRY.USER_ID.eq(userId))
                        .and(PRINTING.CARD_ID.eq(CARD.ID))
                        .orderBy(PRINTING.ID))
                .as("printings")
                .convertFrom(r -> r.map(mapping(Printing::new))))
        .from(CARD)
        .where(
            CARD.ID.in(
                select(PRINTING.CARD_ID)
                    .from(COLLECTION_ENTRY)
                    .join(PRINTING)
                    .on(PRINTING.ID.eq(COLLECTION_ENTRY.PRINTING_ID))
                    .where(COLLECTION_ENTRY.USER_ID.eq(userId))))
        .orderBy(CARD.ID)
        .fetch(mapping(CollectionEntry::new));
  }
}
