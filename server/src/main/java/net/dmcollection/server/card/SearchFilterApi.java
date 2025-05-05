package net.dmcollection.server.card;

import java.util.Set;
import net.dmcollection.model.card.Civilization;
import net.dmcollection.model.card.RarityCode;
import net.dmcollection.server.card.internal.SearchFilter;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import net.dmcollection.server.card.internal.SearchFilter.FilterState;
import net.dmcollection.server.card.internal.SearchFilter.Range;
import net.dmcollection.server.card.internal.SearchFilter.RarityFilter;
import org.springframework.data.domain.Pageable;

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
    Integer pageSize) {
  public SearchFilter toSearchFilter(Pageable pageable) {
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
}
