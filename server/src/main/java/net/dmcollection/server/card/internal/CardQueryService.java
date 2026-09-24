package net.dmcollection.server.card.internal;

import static net.dmcollection.server.card.SearchFilterApi.SORT_AMOUNT;
import static net.dmcollection.server.card.SearchFilterApi.SORT_COST;
import static net.dmcollection.server.card.SearchFilterApi.SORT_POWER;
import static net.dmcollection.server.card.SearchFilterApi.SORT_RARITY;
import static net.dmcollection.server.card.SearchFilterApi.SORT_RELEASE;
import static net.dmcollection.server.jooq.generated.Tables.ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.CARD;
import static net.dmcollection.server.jooq.generated.Tables.CARD_CIV_GROUP;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SET;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_CARD_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_RACE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE_ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.RACE;
import static net.dmcollection.server.jooq.generated.tables.CollectionEntry.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.tables.Rarity.RARITY;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.lateral;
import static org.jooq.impl.DSL.min;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.nullif;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.sum;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.dmcollection.server.card.CardStub;
import net.dmcollection.server.card.PrintingStub;
import net.dmcollection.server.card.internal.query.CardTypeResolver;
import net.dmcollection.server.card.internal.query.CivilizationConditionBuilder;
import net.dmcollection.server.card.internal.query.NameConditionBuilder;
import net.dmcollection.server.card.internal.query.RangeConditionBuilder;
import net.dmcollection.server.card.internal.query.TwinpactConditionBuilder;
import net.dmcollection.server.jooq.generated.tables.CardSide;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

@Service
public class CardQueryService {
  private static final Logger log = LoggerFactory.getLogger(CardQueryService.class);
  private final DSLContext dsl;

  private final RarityService rarityService;
  private final CardTypeResolver cardTypeResolver;

  private static final Field<Integer> cardCount = count().over().as("card_count");

  private static final String CARD_RELEASE_AGG = "earliest_release";
  private static final String CARD_COPIES_AGG = "card_copies";
  private static final String CARD_RARITY_AGG = "card_rarity";
  private static final String CARD_ID_AGG = "print_id";

  public CardQueryService(
      DSLContext dsl, RarityService rarityService, CardTypeResolver cardTypeResolver) {
    this.dsl = dsl;
    this.rarityService = rarityService;
    this.cardTypeResolver = cardTypeResolver;
  }

  public record PrintingSide(String imageFileName) {}

  private static final Field<Integer> AMOUNT_FIELD =
      coalesce(COLLECTION_ENTRY.QUANTITY, 0).as("amount");

  public Page<CardStub> search(@NonNull SearchFilter filter) {
    if (filter.isInvalid()) {
      log.warn("Invalid search filter: {}", filter);
      return Page.empty();
    }
    log.debug("Searching with filter: {}", filter);
    Short raritySortOrder = null;
    if (filter.rarityFilter() != null) {
      raritySortOrder = (short) rarityService.getOrder(filter.rarityFilter().rarityCode());
    }
    CardTypeResolver.IncludedExcluded cardTypeIds = null;
    if (filter.cardType() != null) {
      cardTypeIds = cardTypeResolver.resolve(filter.cardType());
    }
    var printingSides =
        multiset(
                select(PRINTING_SIDE.IMAGE_FILENAME, CARD_SIDE.SIDE_ORDER)
                    .from(PRINTING_SIDE)
                    .join(CARD_SIDE)
                    .on(PRINTING_SIDE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
                    .where(PRINTING_SIDE.PRINTING_ID.eq(PRINTING.ID))
                    .orderBy(CARD_SIDE.SIDE_ORDER))
            .as("printingSides")
            .convertFrom(r -> r.map(s -> s.get(PRINTING_SIDE.IMAGE_FILENAME)));
    var printings =
        multiset(
                select(
                        PRINTING.ID,
                        PRINTING.OFFICIAL_SITE_ID,
                        PRINTING.COLLECTOR_NUMBER,
                        CARD_SET.CODE,
                        CARD_SET.RELEASE_DATE,
                        AMOUNT_FIELD,
                        printingSides)
                    .from(PRINTING)
                    .join(CARD_SET)
                    .on(PRINTING.SET_ID.eq(CARD_SET.ID))
                    .leftJoin(RARITY)
                    .on(RARITY.ID.eq(PRINTING.RARITY_ID))
                    .leftJoin(COLLECTION_ENTRY)
                    .on(
                        COLLECTION_ENTRY
                            .PRINTING_ID
                            .eq(PRINTING.ID)
                            .and(COLLECTION_ENTRY.USER_ID.eq(filter.collectionFilter().userId())))
                    .where(
                        PRINTING
                            .CARD_ID
                            .eq(CARD.ID)
                            .and(printingCondition(filter, raritySortOrder)))
                    .orderBy(printingOrderFields(filter)))
            .as("printings")
            .convertFrom(
                r ->
                    r.map(
                        p ->
                            new PrintingStub(
                                p.get(PRINTING.ID),
                                p.get(PRINTING.OFFICIAL_SITE_ID),
                                p.get(PRINTING.COLLECTOR_NUMBER),
                                p.get(CARD_SET.CODE),
                                p.get(CARD_SET.RELEASE_DATE),
                                p.get(AMOUNT_FIELD),
                                p.get(printingSides).stream().filter(Objects::nonNull).toList())));
    var cardAggregates =
        lateral(
                select(
                        min(PRINTING.OFFICIAL_SITE_ID).collate("C").as(CARD_ID_AGG),
                        min(CARD_SET.RELEASE_DATE).as(CARD_RELEASE_AGG),
                        coalesce(sum(COLLECTION_ENTRY.QUANTITY), 0).as(CARD_COPIES_AGG),
                        coalesce(min(nullif(RARITY.SORT_ORDER, 0)), 0).as(CARD_RARITY_AGG))
                    .from(PRINTING)
                    .join(CARD_SET)
                    .on(PRINTING.SET_ID.eq(CARD_SET.ID))
                    .leftJoin(RARITY)
                    .on(RARITY.ID.eq(PRINTING.RARITY_ID))
                    .leftJoin(COLLECTION_ENTRY)
                    .on(
                        COLLECTION_ENTRY
                            .PRINTING_ID
                            .eq(PRINTING.ID)
                            .and(COLLECTION_ENTRY.USER_ID.eq(filter.collectionFilter().userId())))
                    .where(
                        PRINTING
                            .CARD_ID
                            .eq(CARD.ID)
                            .and(printingCondition(filter, raritySortOrder))))
            .as("card_aggregates");
    var filteredCards =
        name("filtered_cards")
            .as(
                select(
                        CARD.ID,
                        CARD.NAME,
                        CARD.SORT_CIVILIZATION,
                        CARD.SORT_COST,
                        CARD.SORT_POWER,
                        CARD.SORT_POWER_MODIFIER,
                        cardAggregates.field(CARD_ID_AGG, String.class),
                        cardAggregates.field(CARD_RELEASE_AGG, LocalDate.class),
                        cardAggregates.field(CARD_COPIES_AGG, Long.class),
                        cardAggregates.field(CARD_RARITY_AGG, Short.class),
                        printings)
                    .from(CARD)
                    .crossJoin(cardAggregates)
                    .where(
                        cardConditions(filter, cardTypeIds)
                            .and(printingExistsCondition(filter, raritySortOrder)))
                    .orderBy(cardOrderFields(filter, CARD, cardAggregates)));
    var rows =
        dsl.with(filteredCards)
            .select(filteredCards.asterisk(), cardCount)
            .from(filteredCards)
            .orderBy(cardOrderFields(filter, filteredCards, filteredCards))
            .limit(filter.pageable().getPageSize())
            .offset(filter.pageable().getOffset())
            .fetch();

    int totalCount = rows.isEmpty() ? 0 : rows.getFirst().get(cardCount);
    List<CardStub> cards =
        rows.map(r -> new CardStub(r.get(CARD.ID), r.get(CARD.NAME), r.get(printings)));

    return new PageImpl<>(cards, filter.pageable(), totalCount);
  }

  private static List<OrderField<?>> printingOrderFields(SearchFilter filter) {
    var sort = filter.pageable().getSort();
    List<OrderField<?>> fields = new ArrayList<>();
    sort.forEach(
        order -> {
          SortOrder sortOrder = order.isAscending() ? SortOrder.ASC : SortOrder.DESC;
          switch (order.getProperty()) {
            case SORT_AMOUNT:
              {
                var field = COLLECTION_ENTRY.QUANTITY.sort(sortOrder);
                field = sortOrder == SortOrder.ASC ? field.nullsFirst() : field.nullsLast();
                fields.add(field);
                break;
              }
            case SORT_RELEASE:
              {
                fields.add(CARD_SET.RELEASE_DATE.sort(sortOrder));
                break;
              }
            case SORT_RARITY:
              {
                fields.add(RARITY.SORT_ORDER.sort(sortOrder).nullsLast());
                break;
              }
            default:
          }
        });
    if (sort.stream().noneMatch(order -> SORT_RELEASE.equals(order.getProperty()))) {
      fields.add(CARD_SET.RELEASE_DATE.desc());
    }
    fields.add(PRINTING.OFFICIAL_SITE_ID.asc());
    return fields;
  }

  private static List<OrderField<?>> cardOrderFields(
      SearchFilter filter, Table<?> cards, Table<?> aggregates) {
    var sort = filter.pageable().getSort();
    List<OrderField<?>> fields = new ArrayList<>();
    sort.forEach(
        order -> {
          Field<?> field =
              switch (order.getProperty()) {
                case SORT_COST -> cards.field(CARD.SORT_COST);
                case SORT_POWER -> cards.field(CARD.SORT_POWER);
                case SORT_RARITY -> aggregates.field(CARD_RARITY_AGG);
                case SORT_RELEASE -> aggregates.field(CARD_RELEASE_AGG, LocalDate.class);
                case SORT_AMOUNT -> aggregates.field(CARD_COPIES_AGG);
                default -> null;
              };
          if (field != null) {
            SortOrder sortOrder = order.isAscending() ? SortOrder.ASC : SortOrder.DESC;
            var sortField = field.sort(sortOrder).nullsLast();
            fields.add(sortField);
            if (order.getProperty().equals(SORT_POWER)) {
              fields.add(cards.field(CARD.SORT_POWER_MODIFIER).sort(sortOrder));
            }
          }
        });
    if (sort.stream().noneMatch(order -> SORT_RELEASE.equals(order.getProperty()))) {
      fields.add(aggregates.field(CARD_RELEASE_AGG, LocalDate.class).desc());
    }
    fields.add(aggregates.field(CARD_ID_AGG, String.class).asc());
    fields.add(cards.field(CARD.ID).desc());
    return fields;
  }

  private static Condition printingExistsCondition(SearchFilter filter, Short raritySortOrder) {
    var printingCondition = printingCondition(filter, raritySortOrder);
    if (printingCondition.equals(noCondition())) {
      return printingCondition;
    }
    return exists(
        selectOne()
            .from(PRINTING)
            .leftJoin(RARITY)
            .on(RARITY.ID.eq(PRINTING.RARITY_ID))
            .leftJoin(COLLECTION_ENTRY)
            .on(
                COLLECTION_ENTRY
                    .PRINTING_ID
                    .eq(PRINTING.ID)
                    .and(COLLECTION_ENTRY.USER_ID.eq(filter.collectionFilter().userId())))
            .where(PRINTING.CARD_ID.eq(CARD.ID).and(printingCondition)));
  }

  private static Condition printingCondition(SearchFilter filter, Short raritySortOrder) {
    Condition result = noCondition();
    if (filter.setId() != null) {
      result = result.and(PRINTING.SET_ID.eq(filter.setId()));
    }
    if (filter.collectionFilter().ownedOnly()) {
      result = result.and(COLLECTION_ENTRY.PRINTING_ID.isNotNull());
    }
    if (raritySortOrder != null) {
      result = result.and(rarityCondition(raritySortOrder, filter.rarityFilter().range()));
    }
    return result;
  }

  private static Condition cardConditions(
      SearchFilter filter, CardTypeResolver.IncludedExcluded cardTypeIds) {
    return civGroupCondition(filter)
        .and(NameConditionBuilder.build(filter.nameSearch()))
        .and(TwinpactConditionBuilder.build(filter.twinpact()))
        .and(
            RangeConditionBuilder.build(
                CardSide.CARD_SIDE.COST_FILTER, filter.minCost(), filter.maxCost()))
        .and(
            RangeConditionBuilder.build(
                CardSide.CARD_SIDE.POWER_FILTER, filter.minPower(), filter.maxPower()))
        .and(abilityCondition(filter))
        .and(raceCondition(filter))
        .and(typeCondition(cardTypeIds));
  }

  private static Condition civGroupCondition(SearchFilter filter) {
    var civGroupCondition =
        CivilizationConditionBuilder.build(
            filter.includedCivs(),
            filter.excludedCivs(),
            filter.includeMono(),
            filter.includeRainbow(),
            filter.matchExactRainbowCivs());
    if (civGroupCondition.equals(noCondition())) {
      return civGroupCondition;
    }
    return exists(
        selectOne()
            .from(CARD_CIV_GROUP)
            .where(CARD_CIV_GROUP.CARD_ID.eq(CARD.ID).and(civGroupCondition)));
  }

  private static Condition abilityCondition(SearchFilter filter) {
    if (filter.effectSearch() == null || filter.effectSearch().isBlank()) {
      return noCondition();
    }

    return exists(
        selectOne()
            .from(PRINTING)
            .join(PRINTING_SIDE)
            .on(PRINTING_SIDE.PRINTING_ID.eq(PRINTING.ID))
            .join(PRINTING_SIDE_ABILITY)
            .on(PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID.eq(PRINTING_SIDE.ID))
            .join(ABILITY)
            .on(ABILITY.ID.eq(PRINTING_SIDE_ABILITY.ABILITY_ID))
            .where(
                PRINTING
                    .CARD_ID
                    .eq(CARD.ID)
                    .and(ABILITY.SEARCH_TEXT.containsIgnoreCase(filter.effectSearch()))));
  }

  private static Condition typeCondition(CardTypeResolver.IncludedExcluded typeIds) {
    Condition sideCondition = noCondition();
    if (typeIds == null) {
      return sideCondition;
    }

    if (!typeIds.included().isEmpty()) {
      sideCondition =
          sideCondition.and(
              exists(
                  selectOne()
                      .from(CARD_SIDE_CARD_TYPE)
                      .where(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
                      .and(CARD_SIDE_CARD_TYPE.CARD_TYPE_ID.in(typeIds.included()))));
    }

    if (!typeIds.excluded().isEmpty()) {
      sideCondition =
          sideCondition.and(
              notExists(
                  selectOne()
                      .from(CARD_SIDE_CARD_TYPE)
                      .where(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
                      .and(CARD_SIDE_CARD_TYPE.CARD_TYPE_ID.in(typeIds.excluded()))));
    }

    return exists(
        selectOne().from(CARD_SIDE).where(CARD_SIDE.CARD_ID.eq(CARD.ID)).and(sideCondition));
  }

  private static Condition raceCondition(SearchFilter filter) {
    if (filter.raceSearch() == null || filter.raceSearch().isBlank()) {
      return noCondition();
    }

    return exists(
        selectOne()
            .from(CARD_SIDE)
            .join(CARD_SIDE_RACE)
            .on(CARD_SIDE_RACE.CARD_SIDE_ID.eq(CARD_SIDE.ID))
            .join(RACE)
            .on(RACE.ID.eq(CARD_SIDE_RACE.RACE_ID))
            .where(
                CARD_SIDE
                    .CARD_ID
                    .eq(CARD.ID)
                    .and(RACE.NAME.containsIgnoreCase(filter.raceSearch()))));
  }

  private static Condition rarityCondition(short sortOrder, SearchFilter.Range range) {

    Field<Short> effectiveSortOrder = coalesce(RARITY.SORT_ORDER, (short) 0);

    return switch (range) {
      case EQ -> effectiveSortOrder.eq(sortOrder);
      case LE -> effectiveSortOrder.le(sortOrder);
      case GE -> effectiveSortOrder.ge(sortOrder);
    };
  }
}
