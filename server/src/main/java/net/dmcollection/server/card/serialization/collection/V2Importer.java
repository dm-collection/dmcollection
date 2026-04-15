package net.dmcollection.server.card.serialization.collection;

import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_HISTORY_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.dmcollection.server.card.serialization.collection.format.v2.V2CollectionExport;
import net.dmcollection.server.card.serialization.collection.format.v2.V2Printing;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class V2Importer {
  private final DSLContext dsl;

  V2Importer(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Transactional
  public void importCollection(V2CollectionExport exported, UUID userId) {
    Map<Integer, Integer> before = deleteAndReturnAmounts(userId);
    Map<Integer, Integer> after = printingsToImport(exported);

    List<ChangeEntry> changes = getChangeEntries(after, before);

    // batch insert new collection entries
    if (!after.isEmpty()) {
      var insert =
          dsl.insertInto(
              COLLECTION_ENTRY,
              COLLECTION_ENTRY.USER_ID,
              COLLECTION_ENTRY.PRINTING_ID,
              COLLECTION_ENTRY.QUANTITY);
      int matched = 0;
      for (var entry : after.entrySet()) {
        if (entry.getValue() > 0) {
          insert = insert.values(userId, entry.getKey(), entry.getValue());
          matched++;
        }
      }
      if (matched > 0) {
        insert.execute();
      }
    }

    // batch write history entries, all with same timestamp
    if (!changes.isEmpty()) {
      OffsetDateTime now = OffsetDateTime.now();
      var historyInsert =
          dsl.insertInto(
              COLLECTION_HISTORY_ENTRY,
              COLLECTION_HISTORY_ENTRY.USER_ID,
              COLLECTION_HISTORY_ENTRY.PRINTING_ID,
              COLLECTION_HISTORY_ENTRY.PREVIOUS_QTY,
              COLLECTION_HISTORY_ENTRY.NEW_QTY,
              COLLECTION_HISTORY_ENTRY.CHANGED_AT);
      for (ChangeEntry change : changes) {
        historyInsert =
            historyInsert.values(
                userId, change.printingId(), change.previousQty(), change.newQty(), now);
      }
      historyInsert.execute();
    }
  }

  private static List<ChangeEntry> getChangeEntries(
      Map<Integer, Integer> after, Map<Integer, Integer> before) {
    List<ChangeEntry> changes = new ArrayList<>();
    // add everything that is changed after the import
    changes.addAll(
        after.entrySet().stream()
            .map(e -> new ChangeEntry(e.getKey(), before.getOrDefault(e.getKey(), 0), e.getValue()))
            .filter(c -> c.previousQty() != c.newQty())
            .toList());
    // add everything that is no longer there after the import
    changes.addAll(
        before.entrySet().stream()
            .filter(e -> !after.containsKey(e.getKey()))
            .map(e -> new ChangeEntry(e.getKey(), e.getValue(), 0))
            .filter(c -> c.previousQty() != c.newQty())
            .toList());
    return changes;
  }

  private record ChangeEntry(int printingId, int previousQty, int newQty) {}

  private Map<Integer, Integer> deleteAndReturnAmounts(UUID userId) {
    return dsl.deleteFrom(COLLECTION_ENTRY)
        .where(COLLECTION_ENTRY.USER_ID.eq(userId))
        .returning(COLLECTION_ENTRY.PRINTING_ID, COLLECTION_ENTRY.QUANTITY)
        .fetchMap(COLLECTION_ENTRY.PRINTING_ID, COLLECTION_ENTRY.QUANTITY);
  }

  private Map<Integer, Integer> printingsToImport(V2CollectionExport exported) {
    Map<String, Integer> toImport =
        exported.cards().stream()
            .flatMap(c -> c.prints().stream())
            .filter(p -> p.amount() > 0)
            .collect(Collectors.toMap(V2Printing::id, V2Printing::amount, Integer::sum));
    var idMap =
        dsl.select(PRINTING.OFFICIAL_SITE_ID, PRINTING.ID)
            .from(PRINTING)
            .where(PRINTING.OFFICIAL_SITE_ID.in(toImport.keySet()))
            .fetchMap(PRINTING.OFFICIAL_SITE_ID, PRINTING.ID);

    return toImport.entrySet().stream()
        .filter(e -> idMap.containsKey(e.getKey()))
        .collect(Collectors.toMap(e -> idMap.get(e.getKey()), Map.Entry::getValue));
  }
}
