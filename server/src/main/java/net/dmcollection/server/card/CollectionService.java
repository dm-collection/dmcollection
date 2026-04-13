package net.dmcollection.server.card;

import static net.dmcollection.server.jooq.generated.tables.CollectionEntry.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.tables.CollectionHistoryEntry.COLLECTION_HISTORY_ENTRY;
import static net.dmcollection.server.jooq.generated.tables.Printing.PRINTING;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.sum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.internal.CardQueryService;
import net.dmcollection.server.card.internal.CardQueryService.SearchResult;
import net.dmcollection.server.card.internal.SearchFilter;
import net.dmcollection.server.card.serialization.V2Exporter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionService {

  private static final Logger log = LoggerFactory.getLogger(CollectionService.class);
  private static final Field<Long> UNIQUE_COUNT = count().cast(Long.class).as("unique_count");
  private static final Field<Long> TOTAL_COUNT =
      coalesce(sum(COLLECTION_ENTRY.QUANTITY), 0).cast(Long.class).as("total_count");

  private final DSLContext dsl;
  private final CardQueryService cardQueryService;
  private final V2Exporter exporter;

  public CollectionService(DSLContext dsl, CardQueryService cardQueryService, V2Exporter exporter) {
    this.dsl = dsl;
    this.cardQueryService = cardQueryService;
    this.exporter = exporter;
  }

  public record CollectionInfo(long uniqueCardCount, long totalCardCount, UUID ownerId) {}

  public record CollectionDto(CollectionInfo info, PagedModel<CardStub> cardPage) {}

  public record CollectionCardStub(long cardId, int amount) {}

  public record CollectionCardExport(String name, String shortName, int amount) {}

  public record CollectionExport(
      int version,
      LocalDateTime exportDateTime,
      String title,
      int cardCount,
      int countWithoutDuplicates,
      List<CollectionCardExport> cards) {}

  public V2Exporter.V2CollectionExport exportPrimaryCollection(UUID userId) {
    return exporter.export(userId);
  }

  @Transactional
  public void importPrimaryCollection(UUID userId, CollectionExport toImport) {
    // Delete existing collection entries
    dsl.deleteFrom(COLLECTION_ENTRY).where(COLLECTION_ENTRY.USER_ID.eq(userId)).execute();

    // Collect shortNames from import
    List<String> shortNames =
        toImport.cards().stream()
            .map(CollectionCardExport::shortName)
            .filter(Objects::nonNull)
            .toList();

    if (shortNames.isEmpty()) {
      return;
    }

    // Look up printing IDs by official_site_id
    Map<String, Integer> printingIdByOfficialSiteId =
        dsl.select(PRINTING.ID, PRINTING.OFFICIAL_SITE_ID)
            .from(PRINTING)
            .where(PRINTING.OFFICIAL_SITE_ID.in(shortNames))
            .fetchMap(PRINTING.OFFICIAL_SITE_ID, PRINTING.ID);

    // Insert matching entries
    var insert =
        dsl.insertInto(
            COLLECTION_ENTRY,
            COLLECTION_ENTRY.USER_ID,
            COLLECTION_ENTRY.PRINTING_ID,
            COLLECTION_ENTRY.QUANTITY);

    int matched = 0;
    for (CollectionCardExport card : toImport.cards()) {
      Integer printingId = printingIdByOfficialSiteId.get(card.shortName());
      if (printingId != null && card.amount() > 0) {
        insert = insert.values(userId, printingId, card.amount());
        matched++;
      } else if (printingId == null) {
        log.warn("Import: no printing found for shortName '{}'", card.shortName());
      }
    }

    if (matched > 0) {
      insert.execute();
    }
  }

  public CollectionDto getPrimaryCollection(UUID userId, SearchFilter searchFilter) {
    searchFilter = searchFilter.withCollectionFilter(userId, true);
    SearchResult searchResult = cardQueryService.search(searchFilter);
    CollectionInfo ci =
        new CollectionInfo(
            searchResult.pageOfCards().getTotalElements(), searchResult.totalCollected(), userId);
    return new CollectionDto(ci, new PagedModel<>(searchResult.pageOfCards()));
  }

  public Map<Long, Integer> getPrimaryStub(UUID userId) {
    return dsl.select(COLLECTION_ENTRY.PRINTING_ID, COLLECTION_ENTRY.QUANTITY)
        .from(COLLECTION_ENTRY)
        .where(COLLECTION_ENTRY.USER_ID.eq(userId))
        .fetchMap(
            r -> r.get(COLLECTION_ENTRY.PRINTING_ID).longValue(),
            r -> r.get(COLLECTION_ENTRY.QUANTITY));
  }

  @Transactional
  public Optional<Map<Long, Integer>> setCardAmountOnStub(
      UUID userId, Long printingId, int amount) {
    if (!printingExists(printingId)) {
      return Optional.empty();
    }
    upsertCollectionEntry(userId, printingId.intValue(), amount);
    return Optional.of(getPrimaryStub(userId));
  }

  @Transactional
  public Optional<CollectionCardStub> setSingleCardAmount(
      UUID userId, Long printingId, int amount) {
    if (!printingExists(printingId)) {
      return Optional.empty();
    }
    upsertCollectionEntry(userId, printingId.intValue(), amount);
    int actualAmount = getQuantity(userId, printingId.intValue());
    return Optional.of(new CollectionCardStub(printingId, actualAmount));
  }

  public Optional<CollectionCardStub> getSingleCardAmount(UUID userId, Long printingId) {
    if (!printingExists(printingId)) {
      return Optional.empty();
    }
    int amount = getQuantity(userId, printingId.intValue());
    return Optional.of(new CollectionCardStub(printingId, amount));
  }

  @Transactional
  public Optional<CollectionInfo> setCardAmount(UUID userId, Long printingId, int amount) {
    if (!printingExists(printingId)) {
      return Optional.empty();
    }
    upsertCollectionEntry(userId, printingId.intValue(), amount);
    return Optional.of(getCollectionInfo(userId));
  }

  private boolean printingExists(Long printingId) {
    return dsl.fetchExists(
        dsl.selectOne().from(PRINTING).where(PRINTING.ID.eq(printingId.intValue())));
  }

  private void upsertCollectionEntry(UUID userId, int printingId, int amount) {
    int previousQty = getQuantity(userId, printingId);

    if (amount <= 0) {
      dsl.deleteFrom(COLLECTION_ENTRY)
          .where(COLLECTION_ENTRY.USER_ID.eq(userId))
          .and(COLLECTION_ENTRY.PRINTING_ID.eq(printingId))
          .execute();
    } else {
      dsl.insertInto(COLLECTION_ENTRY)
          .set(COLLECTION_ENTRY.USER_ID, userId)
          .set(COLLECTION_ENTRY.PRINTING_ID, printingId)
          .set(COLLECTION_ENTRY.QUANTITY, amount)
          .onConflict(COLLECTION_ENTRY.USER_ID, COLLECTION_ENTRY.PRINTING_ID)
          .doUpdate()
          .set(COLLECTION_ENTRY.QUANTITY, amount)
          .execute();
    }

    if (previousQty != amount) {
      dsl.insertInto(COLLECTION_HISTORY_ENTRY)
          .set(COLLECTION_HISTORY_ENTRY.USER_ID, userId)
          .set(COLLECTION_HISTORY_ENTRY.PRINTING_ID, printingId)
          .set(COLLECTION_HISTORY_ENTRY.PREVIOUS_QTY, previousQty)
          .set(COLLECTION_HISTORY_ENTRY.NEW_QTY, amount)
          .execute();
    }
  }

  private int getQuantity(UUID userId, int printingId) {
    Integer qty =
        dsl.select(COLLECTION_ENTRY.QUANTITY)
            .from(COLLECTION_ENTRY)
            .where(COLLECTION_ENTRY.USER_ID.eq(userId))
            .and(COLLECTION_ENTRY.PRINTING_ID.eq(printingId))
            .fetchOne(COLLECTION_ENTRY.QUANTITY);
    return qty != null ? qty : 0;
  }

  private CollectionInfo getCollectionInfo(UUID userId) {
    var result =
        dsl.select(UNIQUE_COUNT, TOTAL_COUNT)
            .from(COLLECTION_ENTRY)
            .where(COLLECTION_ENTRY.USER_ID.eq(userId))
            .fetchOne();
    return new CollectionInfo(result.get(UNIQUE_COUNT), result.get(TOTAL_COUNT), userId);
  }
}
