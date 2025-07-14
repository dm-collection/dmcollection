package net.dmcollection.server.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.dmcollection.model.card.CardEntity;
import net.dmcollection.model.card.CardEntity.Columns;
import net.dmcollection.model.card.CardFacet;
import net.dmcollection.model.card.Civilization;
import net.dmcollection.model.card.OfficialSet;
import net.dmcollection.model.card.Rarity;
import net.dmcollection.model.card.RarityCode;
import net.dmcollection.server.card.internal.CardQueryService;
import net.dmcollection.server.card.internal.SearchFilter;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import net.dmcollection.server.card.internal.SearchFilter.FilterState;
import net.dmcollection.server.card.internal.SearchFilter.Range;
import net.dmcollection.server.card.internal.SearchFilter.RarityFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

public record SearchFilterApi(
    Long setId,
    Set<Civilization> includeCivs,
    Set<Civilization> excludeCivs,
    Boolean matchNumberOfCivs,
    Boolean includeMono,
    Boolean includeRainbow,
    Integer minCost,
    Integer maxCost,
    Integer minPower,
    Integer maxPower,
    FilterState twinpact,
    CardType cardType,
    Range rRange,
    RarityCode rarity,
    String species,
    String name,
    Integer pageSize,
    String sort) {

  public SearchFilter toSearchFilter() {
    return toSearchFilter(Pageable.unpaged(parseSort()));
  }

  public SearchFilter toSearchFilter(int pageNumber, int pageSize) {
    return toSearchFilter(PageRequest.of(pageNumber, pageSize, parseSort()));
  }

  private SearchFilter toSearchFilter(Pageable pageable) {
    var rarityFilter =
        rarity != null ? new RarityFilter(rarity, rRange != null ? rRange : Range.EQ) : null;

    return new SearchFilter(
        setId,
        includeCivs,
        excludeCivs,
        includeMono,
        includeRainbow,
        Boolean.TRUE.equals(matchNumberOfCivs),
        minCost,
        maxCost,
        minPower,
        maxPower,
        twinpact,
        cardType,
        rarityFilter,
        species,
        name,
        null,
        pageable);
  }

  private Sort parseSort() {
    if (sort == null || sort.trim().isBlank()) {
      return Sort.by(OfficialSet.Columns.RELEASE)
          .descending()
          .and(Sort.by(CardEntity.Columns.OFFICIAL_ID).ascending());
    }
    String[] sortParts = sort.split(",");
    List<Order> orders = new ArrayList<>(sortParts.length);
    for (String sortPart : sortParts) {
      String[] fieldAndDirection = sortPart.trim().split(":");
      if (fieldAndDirection.length != 2) {
        continue;
      }

      String field = fieldAndDirection[0].trim();
      String direction = fieldAndDirection[1].trim().toLowerCase();

      String actualField = mapColumn(field);
      if (actualField != null) {
        if ("desc".equals(direction)) {
          orders.add(Order.desc(actualField));
        } else if ("asc".equals(direction)) {
          orders.add(Order.asc(actualField));
        }
      }
    }

    if (orders.stream()
        .noneMatch(order -> OfficialSet.Columns.RELEASE.equals(order.getProperty()))) {
      orders.add(Order.desc(OfficialSet.Columns.RELEASE));
    }

    if (orders.stream().noneMatch(order -> Columns.OFFICIAL_ID.equals(order.getProperty()))) {
      orders.add(Order.asc(Columns.OFFICIAL_ID));
    }

    return Sort.by(orders);
  }

  private String mapColumn(String parameter) {
    return switch (parameter.toLowerCase()) {
      case "rel" -> OfficialSet.Columns.RELEASE;
      case "id" -> Columns.OFFICIAL_ID;
      case "amt" -> CardQueryService.AMOUNT;
      case "cost" -> CardFacet.Columns.COST;
      case "pwr" -> CardFacet.Columns.POWER_SORT;
      case "rar" -> Rarity.Columns.ORDER;
      default -> null;
    };
  }
}
