package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.tables.CollectionEntry.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.tables.Printing.PRINTING;
import static org.jooq.impl.DSL.noCondition;

import java.util.List;
import java.util.UUID;
import net.dmcollection.server.card.internal.RarityService;
import net.dmcollection.server.card.internal.SearchFilter;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import net.dmcollection.server.card.internal.SearchFilter.CollectionFilter;
import net.dmcollection.server.card.internal.SearchFilter.RarityFilter;
import org.jooq.Condition;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

@Component
public class SearchFilterTranslator {

  private final CardTypeResolver cardTypeResolver;
  private final RarityService rarityService;

  public SearchFilterTranslator(CardTypeResolver cardTypeResolver, RarityService rarityService) {
    this.cardTypeResolver = cardTypeResolver;
    this.rarityService = rarityService;
  }

  public record TranslatedFilter(
      Condition civilizationCondition, Condition mainCondition, List<OrderField<?>> orderBy) {}

  public TranslatedFilter translate(SearchFilter filter) {
    Condition civCondition = translateCivilization(filter);

    Condition main =
        noCondition()
            .and(TwinpactConditionBuilder.build(filter.twinpact()))
            .and(SetConditionBuilder.build(filter.setId()))
            .and(
                RangeConditionBuilder.build(
                    CARD_SIDE.COST_FILTER, filter.minCost(), filter.maxCost()))
            .and(
                RangeConditionBuilder.build(
                    CARD_SIDE.POWER_FILTER, filter.minPower(), filter.maxPower()))
            .and(translateRarity(filter.rarityFilter()))
            .and(RaceConditionBuilder.build(filter.raceSearch()))
            .and(translateCardType(filter.cardType()))
            .and(NameConditionBuilder.build(filter.nameSearch()))
            .and(AbilityTextConditionBuilder.build(filter.effectSearch()))
            .and(translateCollection(filter.collectionFilter()));

    UUID userId = filter.collectionFilter() != null ? filter.collectionFilter().userId() : null;
    List<OrderField<?>> orderBy = SortBuilder.build(filter.pageable().getSort(), userId);

    return new TranslatedFilter(civCondition, main, orderBy);
  }

  private Condition translateCivilization(SearchFilter filter) {
    if (!filter.needsCivFilter()) {
      return noCondition();
    }
    return CivilizationConditionBuilder.build(
        filter.includedCivs(),
        filter.excludedCivs(),
        filter.includeMono(),
        filter.includeRainbow(),
        filter.matchExactRainbowCivs());
  }

  private Condition translateRarity(RarityFilter rarityFilter) {
    if (rarityFilter == null) {
      return noCondition();
    }
    short sortOrder = (short) rarityService.getOrder(rarityFilter.rarityCode());
    return RarityConditionBuilder.build(sortOrder, rarityFilter.range());
  }

  private Condition translateCardType(CardType cardType) {
    if (cardType == null) {
      return noCondition();
    }
    CardTypeResolver.IncludedExcluded ie = cardTypeResolver.resolve(cardType);
    return CardTypeConditionBuilder.build(ie.included(), ie.excluded());
  }

  private Condition translateCollection(CollectionFilter collectionFilter) {
    if (!collectionFilter.ownedOnly()) {
      return noCondition();
    }
    UUID userId = collectionFilter.userId();
    if (userId == null) {
      return noCondition();
    }
    var ownedPrintings =
        DSL.select(COLLECTION_ENTRY.PRINTING_ID)
            .from(COLLECTION_ENTRY)
            .where(COLLECTION_ENTRY.USER_ID.eq(userId));
    return PRINTING.ID.in(ownedPrintings);
  }
}
