package net.dmcollection.server.card.internal;

import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardCivGroup.CARD_CIV_GROUP;
import static net.dmcollection.server.jooq.generated.tables.CardSet.CARD_SET;
import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.tables.CollectionEntry.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.tables.Printing.PRINTING;
import static net.dmcollection.server.jooq.generated.tables.PrintingSide.PRINTING_SIDE;
import static net.dmcollection.server.jooq.generated.tables.Rarity.RARITY;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.sum;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.Civilization;
import net.dmcollection.server.card.internal.SearchFilter.CollectionFilter;
import net.dmcollection.server.card.internal.query.SearchFilterTranslator;
import net.dmcollection.server.card.internal.query.SearchFilterTranslator.TranslatedFilter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class CardQueryService {

  private static final Logger log = LoggerFactory.getLogger(CardQueryService.class);

  private static final Field<Long> TOTAL_COUNT = count().over().cast(Long.class).as("total_count");
  private static final Field<Integer> AMOUNT_FIELD =
      coalesce(COLLECTION_ENTRY.QUANTITY, 0).as("amount");
  private static final Field<Long> TOTAL_COLLECTED =
      sum(COLLECTION_ENTRY.QUANTITY).over().cast(Long.class).as("total_collected");

  private final DSLContext dsl;
  private final SearchFilterTranslator searchFilterTranslator;

  public CardQueryService(DSLContext dsl, SearchFilterTranslator searchFilterTranslator) {
    this.dsl = dsl;
    this.searchFilterTranslator = searchFilterTranslator;
  }

  public record SearchResult(Page<CardStub> pageOfCards, long totalCollected) {}

  public SearchResult search(@NonNull SearchFilter searchFilter) {
    if (searchFilter.isInvalid()) {
      log.warn("Invalid search filter: {}", searchFilter);
      return new SearchResult(new PageImpl<>(List.of()), 0);
    }
    log.debug("Searching with filter: {}", searchFilter);

    TranslatedFilter translated = searchFilterTranslator.translate(searchFilter);
    CollectionFilter collectionFilter = searchFilter.collectionFilter();
    boolean hasCollection = collectionFilter != null;
    Pageable pageable = searchFilter.pageable();

    // Phase 1: Filter and paginate
    var civSubquery =
        dsl.selectDistinct(CARD.ID)
            .from(CARD)
            .join(CARD_CIV_GROUP)
            .on(CARD_CIV_GROUP.CARD_ID.eq(CARD.ID))
            .where(translated.civilizationCondition());

    SelectJoinStep<? extends Record> fromClause;
    if (hasCollection) {
      fromClause =
          dsl.select(
                  PRINTING.ID,
                  PRINTING.OFFICIAL_SITE_ID,
                  PRINTING.COLLECTOR_NUMBER,
                  TOTAL_COUNT,
                  AMOUNT_FIELD,
                  TOTAL_COLLECTED)
              .from(PRINTING)
              .join(CARD)
              .on(CARD.ID.eq(PRINTING.CARD_ID))
              .join(CARD_SET)
              .on(CARD_SET.ID.eq(PRINTING.SET_ID))
              .leftJoin(RARITY)
              .on(RARITY.ID.eq(PRINTING.RARITY_ID))
              .leftJoin(COLLECTION_ENTRY)
              .on(
                  COLLECTION_ENTRY
                      .PRINTING_ID
                      .eq(PRINTING.ID)
                      .and(COLLECTION_ENTRY.USER_ID.eq(collectionFilter.userId())));
    } else {
      fromClause =
          dsl.select(PRINTING.ID, PRINTING.OFFICIAL_SITE_ID, PRINTING.COLLECTOR_NUMBER, TOTAL_COUNT)
              .from(PRINTING)
              .join(CARD)
              .on(CARD.ID.eq(PRINTING.CARD_ID))
              .join(CARD_SET)
              .on(CARD_SET.ID.eq(PRINTING.SET_ID))
              .leftJoin(RARITY)
              .on(RARITY.ID.eq(PRINTING.RARITY_ID));
    }

    SelectConditionStep<? extends Record> filtered =
        fromClause.where(PRINTING.CARD_ID.in(civSubquery)).and(translated.mainCondition());

    var ordered = filtered.orderBy(translated.orderBy());

    var query =
        pageable.isPaged()
            ? ordered.limit(pageable.getPageSize()).offset((int) pageable.getOffset())
            : ordered;

    // Execute and collect results
    record PrintingRow(
        int printingId,
        String officialSiteId,
        String collectorNumber,
        long totalCount,
        int amount,
        long totalCollected) {}

    Map<Integer, PrintingRow> matchedPrintings = new LinkedHashMap<>();
    query.forEach(
        r -> {
          int printingId = r.get(PRINTING.ID);
          matchedPrintings.putIfAbsent(
              printingId,
              new PrintingRow(
                  printingId,
                  r.get(PRINTING.OFFICIAL_SITE_ID),
                  r.get(PRINTING.COLLECTOR_NUMBER),
                  r.get(TOTAL_COUNT),
                  hasCollection ? r.get(AMOUNT_FIELD) : 0,
                  hasCollection ? valueOrZero(r.get(TOTAL_COLLECTED)) : 0));
        });

    if (matchedPrintings.isEmpty()) {
      return new SearchResult(new PageImpl<>(List.of(), pageable, 0), 0);
    }

    long totalCount = matchedPrintings.values().iterator().next().totalCount();
    long totalCollected =
        hasCollection ? matchedPrintings.values().iterator().next().totalCollected() : 0;

    // Phase 2: Enrich with side data
    record SideData(Short[] civilizationIds, String imageFilename) {}

    Map<Integer, List<SideData>> sidesByPrinting = new LinkedHashMap<>();
    dsl.select(
            PRINTING.ID,
            CARD_SIDE.SIDE_ORDER,
            CARD_SIDE.CIVILIZATION_IDS,
            PRINTING_SIDE.IMAGE_FILENAME)
        .from(PRINTING_SIDE)
        .join(PRINTING)
        .on(PRINTING.ID.eq(PRINTING_SIDE.PRINTING_ID))
        .join(CARD_SIDE)
        .on(CARD_SIDE.ID.eq(PRINTING_SIDE.CARD_SIDE_ID))
        .where(PRINTING.ID.in(matchedPrintings.keySet()))
        .orderBy(PRINTING.ID, CARD_SIDE.SIDE_ORDER)
        .forEach(
            r ->
                sidesByPrinting
                    .computeIfAbsent(r.get(PRINTING.ID), k -> new ArrayList<>())
                    .add(
                        new SideData(
                            r.get(CARD_SIDE.CIVILIZATION_IDS),
                            r.get(PRINTING_SIDE.IMAGE_FILENAME))));

    // Phase 3: Assemble CardStub records
    List<CardStub> pageContent = new ArrayList<>(matchedPrintings.size());
    for (PrintingRow row : matchedPrintings.values()) {
      List<SideData> sides = sidesByPrinting.getOrDefault(row.printingId(), List.of());

      Set<Civilization> civilizations = EnumSet.noneOf(Civilization.class);
      for (SideData side : sides) {
        if (side.civilizationIds() == null || side.civilizationIds().length == 0) {
          civilizations.add(Civilization.ZERO);
        } else {
          for (short civId : side.civilizationIds()) {
            civilizations.add(Civilization.values()[civId]);
          }
        }
      }

      List<String> imageUrls =
          sides.stream()
              .map(SideData::imageFilename)
              .filter(Objects::nonNull)
              .map(filename -> "/image/" + filename)
              .toList();

      pageContent.add(
          new CardStub(
              (long) row.printingId(),
              row.officialSiteId(),
              row.collectorNumber(),
              civilizations,
              imageUrls,
              row.amount(),
              row.amount()));
    }

    return new SearchResult(new PageImpl<>(pageContent, pageable, totalCount), totalCollected);
  }

  private static long valueOrZero(Long value) {
    return value != null ? value : 0;
  }
}
