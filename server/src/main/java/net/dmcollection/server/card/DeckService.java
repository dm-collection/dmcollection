package net.dmcollection.server.card;

import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.tables.Deck.DECK;
import static net.dmcollection.server.jooq.generated.tables.DeckVersion.DECK_VERSION;
import static net.dmcollection.server.jooq.generated.tables.DeckVersionEntry.DECK_VERSION_ENTRY;
import static net.dmcollection.server.jooq.generated.tables.Printing.PRINTING;
import static net.dmcollection.server.jooq.generated.tables.PrintingSide.PRINTING_SIDE;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.currentOffsetDateTime;
import static org.jooq.impl.DSL.sum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.dmcollection.server.card.CardService.CardStub;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeckService {

  private static final Logger log = LoggerFactory.getLogger(DeckService.class);
  private static final int EXPORT_FORMAT_VERSION = 2;
  private static final Field<Long> UNIQUE_COUNT =
      count(DECK_VERSION_ENTRY.ID).cast(Long.class).as("unique_count");
  private static final Field<Long> TOTAL_COUNT =
      coalesce(sum(DECK_VERSION_ENTRY.QUANTITY), 0).cast(Long.class).as("total_count");

  private final DSLContext dsl;
  private final CollectionService collectionService;

  public DeckService(DSLContext dsl, CollectionService collectionService) {
    this.dsl = dsl;
    this.collectionService = collectionService;
  }

  public record DeckInfo(
      UUID id,
      String name,
      long uniqueCardCount,
      long totalCardCount,
      LocalDateTime lastModified,
      UUID ownerId) {}

  public record DeckDto(DeckInfo info, PagedModel<CardStub> cardPage) {}

  public record DeckCardExport(String name, String shortName, int amount) {}

  public record DeckExport(
      int version,
      LocalDateTime exportDateTime,
      String title,
      int cardCount,
      int countWithoutDuplicates,
      List<DeckCardExport> cards) {}

  public List<DeckInfo> getDecks(UUID userId) {
    return dsl.select(DECK.ID, DECK.NAME, DECK.UPDATED_AT, DECK.USER_ID, UNIQUE_COUNT, TOTAL_COUNT)
        .from(DECK)
        .leftJoin(DECK_VERSION)
        .on(DECK_VERSION.DECK_ID.eq(DECK.ID).and(DECK_VERSION.IS_DRAFT.isTrue()))
        .leftJoin(DECK_VERSION_ENTRY)
        .on(DECK_VERSION_ENTRY.DECK_VERSION_ID.eq(DECK_VERSION.ID))
        .where(DECK.USER_ID.eq(userId))
        .groupBy(DECK.ID)
        .orderBy(DECK.UPDATED_AT.desc())
        .fetch(
            r ->
                new DeckInfo(
                    r.get(DECK.ID),
                    r.get(DECK.NAME),
                    r.get(UNIQUE_COUNT),
                    r.get(TOTAL_COUNT),
                    r.get(DECK.UPDATED_AT).toLocalDateTime(),
                    r.get(DECK.USER_ID)));
  }

  @Transactional
  public DeckInfo createDeck(UUID userId, String name) {
    var deckRecord =
        dsl.insertInto(DECK)
            .set(DECK.USER_ID, userId)
            .set(DECK.NAME, name)
            .returningResult(DECK.ID, DECK.NAME, DECK.UPDATED_AT, DECK.USER_ID)
            .fetchOne();

    UUID deckId = deckRecord.get(DECK.ID);
    dsl.insertInto(DECK_VERSION)
        .set(DECK_VERSION.DECK_ID, deckId)
        .set(DECK_VERSION.IS_DRAFT, true)
        .execute();

    return new DeckInfo(
        deckId,
        deckRecord.get(DECK.NAME),
        0,
        0,
        deckRecord.get(DECK.UPDATED_AT).toLocalDateTime(),
        deckRecord.get(DECK.USER_ID));
  }

  public boolean deleteDeck(UUID userId, UUID deckId) {
    return dsl.deleteFrom(DECK).where(DECK.ID.eq(deckId).and(DECK.USER_ID.eq(userId))).execute()
        > 0;
  }

  @Transactional
  public Optional<DeckInfo> renameDeck(UUID userId, UUID deckId, String name) {
    int updated =
        dsl.update(DECK)
            .set(DECK.NAME, name)
            .set(DECK.UPDATED_AT, currentOffsetDateTime())
            .where(DECK.ID.eq(deckId).and(DECK.USER_ID.eq(userId)))
            .execute();
    if (updated == 0) {
      return Optional.empty();
    }
    return Optional.of(getDeckInfo(deckId));
  }

  public Optional<DeckDto> getDeck(UUID userId, UUID deckId) {
    boolean exists =
        dsl.fetchExists(
            dsl.selectOne().from(DECK).where(DECK.ID.eq(deckId).and(DECK.USER_ID.eq(userId))));
    if (!exists) {
      return Optional.empty();
    }
    return Optional.of(toDeckDto(deckId, userId));
  }

  @Transactional
  public Optional<DeckInfo> setCardAmount(UUID userId, UUID deckId, Long printingId, int amount) {
    // Get draft version ID and verify ownership
    UUID draftVersionId =
        dsl.select(DECK_VERSION.ID)
            .from(DECK_VERSION)
            .join(DECK)
            .on(DECK.ID.eq(DECK_VERSION.DECK_ID))
            .where(
                DECK_VERSION
                    .DECK_ID
                    .eq(deckId)
                    .and(DECK_VERSION.IS_DRAFT.isTrue())
                    .and(DECK.USER_ID.eq(userId)))
            .fetchOne(DECK_VERSION.ID);

    if (draftVersionId == null) {
      return Optional.empty();
    }

    // Look up card_id from printing
    Integer cardId =
        dsl.select(PRINTING.CARD_ID)
            .from(PRINTING)
            .where(PRINTING.ID.eq(printingId.intValue()))
            .fetchOne(PRINTING.CARD_ID);

    if (cardId == null) {
      return Optional.empty();
    }

    if (amount <= 0) {
      dsl.deleteFrom(DECK_VERSION_ENTRY)
          .where(
              DECK_VERSION_ENTRY
                  .DECK_VERSION_ID
                  .eq(draftVersionId)
                  .and(DECK_VERSION_ENTRY.PRINTING_ID.eq(printingId.intValue())))
          .execute();
    } else {
      dsl.insertInto(DECK_VERSION_ENTRY)
          .set(DECK_VERSION_ENTRY.DECK_VERSION_ID, draftVersionId)
          .set(DECK_VERSION_ENTRY.CARD_ID, cardId)
          .set(DECK_VERSION_ENTRY.PRINTING_ID, printingId.intValue())
          .set(DECK_VERSION_ENTRY.QUANTITY, amount)
          .onConflict(
              DECK_VERSION_ENTRY.DECK_VERSION_ID,
              DECK_VERSION_ENTRY.CARD_ID,
              DECK_VERSION_ENTRY.PRINTING_ID)
          .doUpdate()
          .set(DECK_VERSION_ENTRY.QUANTITY, amount)
          .execute();
    }

    dsl.update(DECK)
        .set(DECK.UPDATED_AT, currentOffsetDateTime())
        .where(DECK.ID.eq(deckId))
        .execute();

    return Optional.of(getDeckInfo(deckId));
  }

  public Optional<DeckExport> exportDeck(UUID userId, UUID deckId) {
    boolean exists =
        dsl.fetchExists(
            dsl.selectOne().from(DECK).where(DECK.ID.eq(deckId).and(DECK.USER_ID.eq(userId))));
    if (!exists) {
      return Optional.empty();
    }

    String deckName =
        dsl.select(DECK.NAME).from(DECK).where(DECK.ID.eq(deckId)).fetchOne(DECK.NAME);

    return Optional.of(forExport(deckId, deckName));
  }

  public List<DeckExport> exportDecks(UUID userId) {
    record DeckIdName(UUID id, String name) {}

    var decks =
        dsl.select(DECK.ID, DECK.NAME)
            .from(DECK)
            .where(DECK.USER_ID.eq(userId))
            .orderBy(DECK.UPDATED_AT.desc())
            .fetch(r -> new DeckIdName(r.get(DECK.ID), r.get(DECK.NAME)));

    return decks.stream().map(d -> forExport(d.id(), d.name())).toList();
  }

  @Transactional
  public void importDeck(UUID userId, DeckExport toImport) {
    // Create new deck with draft version
    UUID deckId =
        dsl.insertInto(DECK)
            .set(DECK.USER_ID, userId)
            .set(DECK.NAME, toImport.title())
            .returningResult(DECK.ID)
            .fetchOne()
            .value1();

    UUID draftVersionId =
        dsl.insertInto(DECK_VERSION)
            .set(DECK_VERSION.DECK_ID, deckId)
            .set(DECK_VERSION.IS_DRAFT, true)
            .returningResult(DECK_VERSION.ID)
            .fetchOne()
            .value1();

    // Collect shortNames for lookup
    List<String> shortNames =
        toImport.cards().stream().map(DeckCardExport::shortName).filter(Objects::nonNull).toList();

    if (shortNames.isEmpty()) {
      return;
    }

    // Look up printing IDs and card IDs by official_site_id
    record PrintingLookup(int printingId, int cardId) {}

    Map<String, PrintingLookup> lookupByShortName =
        dsl
            .select(PRINTING.ID, PRINTING.CARD_ID, PRINTING.OFFICIAL_SITE_ID)
            .from(PRINTING)
            .where(PRINTING.OFFICIAL_SITE_ID.in(shortNames))
            .fetch()
            .stream()
            .collect(
                Collectors.toMap(
                    r -> r.get(PRINTING.OFFICIAL_SITE_ID),
                    r -> new PrintingLookup(r.get(PRINTING.ID), r.get(PRINTING.CARD_ID))));

    // Batch insert entries
    var insert =
        dsl.insertInto(
            DECK_VERSION_ENTRY,
            DECK_VERSION_ENTRY.DECK_VERSION_ID,
            DECK_VERSION_ENTRY.CARD_ID,
            DECK_VERSION_ENTRY.PRINTING_ID,
            DECK_VERSION_ENTRY.QUANTITY);

    int matched = 0;
    for (DeckCardExport card : toImport.cards()) {
      PrintingLookup lookup = lookupByShortName.get(card.shortName());
      if (lookup != null && card.amount() > 0) {
        insert = insert.values(draftVersionId, lookup.cardId(), lookup.printingId(), card.amount());
        matched++;
      } else if (lookup == null) {
        log.warn("Deck import: no printing found for shortName '{}'", card.shortName());
      }
    }

    if (matched > 0) {
      insert.execute();
    }
  }

  private DeckExport forExport(UUID deckId, String title) {
    record ExportRow(String officialSiteId, String sideName, short sideOrder, int quantity) {}

    var rows =
        dsl.select(
                PRINTING.OFFICIAL_SITE_ID,
                CARD_SIDE.NAME,
                CARD_SIDE.SIDE_ORDER,
                DECK_VERSION_ENTRY.QUANTITY)
            .from(DECK_VERSION_ENTRY)
            .join(DECK_VERSION)
            .on(
                DECK_VERSION
                    .ID
                    .eq(DECK_VERSION_ENTRY.DECK_VERSION_ID)
                    .and(DECK_VERSION.IS_DRAFT.isTrue()))
            .join(PRINTING)
            .on(PRINTING.ID.eq(DECK_VERSION_ENTRY.PRINTING_ID))
            .join(CARD_SIDE)
            .on(CARD_SIDE.CARD_ID.eq(PRINTING.CARD_ID))
            .where(DECK_VERSION.DECK_ID.eq(deckId))
            .orderBy(PRINTING.OFFICIAL_SITE_ID, CARD_SIDE.SIDE_ORDER)
            .fetch(
                r ->
                    new ExportRow(
                        r.get(PRINTING.OFFICIAL_SITE_ID),
                        r.get(CARD_SIDE.NAME),
                        r.get(CARD_SIDE.SIDE_ORDER),
                        r.get(DECK_VERSION_ENTRY.QUANTITY)));

    Map<String, List<ExportRow>> byPrinting = new LinkedHashMap<>();
    for (ExportRow row : rows) {
      byPrinting.computeIfAbsent(row.officialSiteId(), k -> new ArrayList<>()).add(row);
    }

    List<DeckCardExport> cardExports =
        byPrinting.entrySet().stream()
            .map(
                entry -> {
                  String officialSiteId = entry.getKey();
                  List<ExportRow> sideRows = entry.getValue();
                  String cardName =
                      sideRows.stream()
                          .map(ExportRow::sideName)
                          .filter(Objects::nonNull)
                          .collect(Collectors.joining("\uff0f"));
                  int quantity = sideRows.getFirst().quantity();
                  return new DeckCardExport(cardName, officialSiteId, quantity);
                })
            .toList();

    int total = cardExports.stream().mapToInt(DeckCardExport::amount).sum();
    return new DeckExport(
        EXPORT_FORMAT_VERSION, LocalDateTime.now(), title, total, cardExports.size(), cardExports);
  }

  private DeckDto toDeckDto(UUID deckId, UUID userId) {
    record EntryRow(
        int printingId,
        String officialSiteId,
        String collectorNumber,
        int quantity,
        short sideOrder,
        Short[] civilizationIds,
        String imageFilename) {}

    var rows =
        dsl.select(
                DECK_VERSION_ENTRY.PRINTING_ID,
                PRINTING.OFFICIAL_SITE_ID,
                PRINTING.COLLECTOR_NUMBER,
                DECK_VERSION_ENTRY.QUANTITY,
                CARD_SIDE.SIDE_ORDER,
                CARD_SIDE.CIVILIZATION_IDS,
                PRINTING_SIDE.IMAGE_FILENAME)
            .from(DECK_VERSION_ENTRY)
            .join(DECK_VERSION)
            .on(
                DECK_VERSION
                    .ID
                    .eq(DECK_VERSION_ENTRY.DECK_VERSION_ID)
                    .and(DECK_VERSION.IS_DRAFT.isTrue())
                    .and(DECK_VERSION.DECK_ID.eq(deckId)))
            .join(PRINTING)
            .on(PRINTING.ID.eq(DECK_VERSION_ENTRY.PRINTING_ID))
            .join(PRINTING_SIDE)
            .on(PRINTING_SIDE.PRINTING_ID.eq(PRINTING.ID))
            .join(CARD_SIDE)
            .on(CARD_SIDE.ID.eq(PRINTING_SIDE.CARD_SIDE_ID))
            .orderBy(DECK_VERSION_ENTRY.PRINTING_ID, CARD_SIDE.SIDE_ORDER)
            .fetch(
                r ->
                    new EntryRow(
                        r.get(DECK_VERSION_ENTRY.PRINTING_ID),
                        r.get(PRINTING.OFFICIAL_SITE_ID),
                        r.get(PRINTING.COLLECTOR_NUMBER),
                        r.get(DECK_VERSION_ENTRY.QUANTITY),
                        r.get(CARD_SIDE.SIDE_ORDER),
                        r.get(CARD_SIDE.CIVILIZATION_IDS),
                        r.get(PRINTING_SIDE.IMAGE_FILENAME)));

    // Group by printing
    Map<Integer, List<EntryRow>> byPrinting = new LinkedHashMap<>();
    for (EntryRow row : rows) {
      byPrinting.computeIfAbsent(row.printingId(), k -> new ArrayList<>()).add(row);
    }

    Map<Long, Integer> collectionAmounts = collectionService.getPrimaryStub(userId);

    List<CardStub> stubs =
        byPrinting.entrySet().stream()
            .map(
                entry -> {
                  int printingId = entry.getKey();
                  List<EntryRow> sideRows = entry.getValue();
                  EntryRow first = sideRows.getFirst();

                  Set<Civilization> civs = EnumSet.noneOf(Civilization.class);
                  List<String> images = new ArrayList<>();
                  for (EntryRow row : sideRows) {
                    if (row.civilizationIds() != null) {
                      for (Short civId : row.civilizationIds()) {
                        civs.add(Civilization.values()[civId]);
                      }
                    }
                    if (row.imageFilename() != null) {
                      images.add("/image/" + row.imageFilename());
                    }
                  }
                  // Add ZERO if no non-zero civs
                  if (civs.isEmpty()) {
                    civs.add(Civilization.ZERO);
                  }

                  int collectionAmount = collectionAmounts.getOrDefault((long) printingId, 0);

                  return new CardStub(
                      (long) printingId,
                      first.officialSiteId(),
                      first.collectorNumber(),
                      civs,
                      images,
                      first.quantity(),
                      collectionAmount);
                })
            .sorted(
                (c1, c2) -> {
                  int civComparison = compareCivs(c1.civilizations(), c2.civilizations());
                  if (civComparison != 0) {
                    return civComparison;
                  }
                  return c1.dmId().compareTo(c2.dmId());
                })
            .toList();

    DeckInfo info = getDeckInfo(deckId);
    return new DeckDto(
        info, new PagedModel<>(new PageImpl<>(stubs, Pageable.unpaged(), stubs.size())));
  }

  private DeckInfo getDeckInfo(UUID deckId) {
    var result =
        dsl.select(DECK.ID, DECK.NAME, DECK.UPDATED_AT, DECK.USER_ID, UNIQUE_COUNT, TOTAL_COUNT)
            .from(DECK)
            .leftJoin(DECK_VERSION)
            .on(DECK_VERSION.DECK_ID.eq(DECK.ID).and(DECK_VERSION.IS_DRAFT.isTrue()))
            .leftJoin(DECK_VERSION_ENTRY)
            .on(DECK_VERSION_ENTRY.DECK_VERSION_ID.eq(DECK_VERSION.ID))
            .where(DECK.ID.eq(deckId))
            .groupBy(DECK.ID)
            .fetchOne();

    return new DeckInfo(
        result.get(DECK.ID),
        result.get(DECK.NAME),
        result.get(UNIQUE_COUNT),
        result.get(TOTAL_COUNT),
        result.get(DECK.UPDATED_AT).toLocalDateTime(),
        result.get(DECK.USER_ID));
  }

  private int compareCivs(Set<Civilization> c1, Set<Civilization> c2) {
    if ((c1.size() == 1 || c2.size() == 1) && c1.size() != c2.size()) {
      return Integer.compare(c1.size(), c2.size());
    }

    String civString1 =
        Civilization.toInts(c1).stream().map(Objects::toString).collect(Collectors.joining());
    String civString2 =
        Civilization.toInts(c2).stream().map(Objects::toString).collect(Collectors.joining());
    return civString1.compareTo(civString2);
  }
}
