package net.dmcollection.server.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
    Integer setId,
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
    String effect,
    Integer pageSize,
    String sort) {

  public SearchFilter toSearchFilter(UUID userId, boolean ownedOnly, int pageNumber, int pageSize) {
    Pageable pageable = PageRequest.of(pageNumber, pageSize, parseSort());
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
        effect,
        new SearchFilter.CollectionFilter(userId, ownedOnly),
        pageable);
  }

  public static final String SORT_RELEASE = "RELEASE";
  public static final String SORT_OFFICIAL_ID = "OFFICIAL_ID";
  public static final String SORT_AMOUNT = "AMOUNT";
  public static final String SORT_COST = "COST";
  public static final String SORT_POWER = "POWER";
  public static final String SORT_RARITY = "RARITY";

  private Sort parseSort() {
    if (sort == null || sort.trim().isBlank()) {
      return Sort.by(SORT_RELEASE).descending().and(Sort.by(SORT_OFFICIAL_ID).ascending());
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

    if (orders.stream().noneMatch(order -> SORT_RELEASE.equals(order.getProperty()))) {
      orders.add(Order.desc(SORT_RELEASE));
    }

    if (orders.stream().noneMatch(order -> SORT_OFFICIAL_ID.equals(order.getProperty()))) {
      orders.add(Order.asc(SORT_OFFICIAL_ID));
    }

    return Sort.by(orders);
  }

  private String mapColumn(String parameter) {
    return switch (parameter.toLowerCase()) {
      case "rel" -> SORT_RELEASE;
      case "id" -> SORT_OFFICIAL_ID;
      case "amt" -> SORT_AMOUNT;
      case "cost" -> SORT_COST;
      case "pwr" -> SORT_POWER;
      case "rar" -> SORT_RARITY;
      default -> null;
    };
  }
}
