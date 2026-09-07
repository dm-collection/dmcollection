package net.dmcollection.server;

import java.util.HashSet;
import java.util.Set;
import net.dmcollection.server.card.Civilization;
import net.dmcollection.server.card.RarityCode;
import net.dmcollection.server.card.internal.SearchFilter;
import org.springframework.data.domain.Pageable;

public class SearchBuilder {

  public static SearchBuilder search() {
    return new SearchBuilder();
  }

  private SearchBuilder() {}

  private Long setId;
  private Set<Civilization> includedCivs;
  private Set<Civilization> excludedCivs;
  private Boolean includeMono = null;
  private Boolean includeRainbow = null;
  private boolean matchNumberOfCivs = false;
  private Integer minCost = null;
  private Integer maxCost = null;
  private Integer minPower = null;
  private Integer maxPower = null;
  private SearchFilter.FilterState twinpact = SearchFilter.FilterState.IN;
  private SearchFilter.CardType cardType = null;
  private String speciesSearch = null;
  private String effectSearch = null;
  private SearchFilter.RarityFilter rarity = null;
  private String nameSearch = null;
  private Pageable pageable = null;

  private void makeCivSet() {
    if (includedCivs == null) {
      includedCivs = new HashSet<>();
    }
    if (excludedCivs == null) {
      excludedCivs = new HashSet<>();
    }
  }

  public SearchBuilder setNameSearch(String nameSearch) {
    this.nameSearch = nameSearch;
    return this;
  }

  public SearchBuilder setRarity(RarityCode rarity) {
    this.rarity = new SearchFilter.RarityFilter(rarity, SearchFilter.Range.EQ);
    return this;
  }

  public SearchBuilder setRarity(RarityCode rarity, SearchFilter.Range range) {
    this.rarity = new SearchFilter.RarityFilter(rarity, range);
    return this;
  }

  public SearchBuilder setSpeciesSearch(String speciesSearch) {
    this.speciesSearch = speciesSearch;
    return this;
  }

  public SearchBuilder setEffectSearch(String effectSearch) {
    this.effectSearch = effectSearch;
    return this;
  }

  public SearchBuilder setTwinpact(SearchFilter.FilterState twinpact) {
    this.twinpact = twinpact;
    return this;
  }

  public SearchBuilder setMatchExactRainbowCivs(boolean matchNumberOfCivs) {
    this.matchNumberOfCivs = matchNumberOfCivs;
    return this;
  }

  public SearchBuilder setPageable(Pageable pageable) {
    this.pageable = pageable;
    return this;
  }

  public SearchBuilder setIncludeMono(Boolean includeMono) {
    this.includeMono = includeMono;
    return this;
  }

  public SearchBuilder setIncludeRainbow(Boolean includeRainbow) {
    this.includeRainbow = includeRainbow;
    return this;
  }

  public SearchBuilder setSetId(Long setId) {
    this.setId = setId;
    return this;
  }

  public SearchBuilder addIncludedCivs(Civilization... civs) {
    makeCivSet();
    includedCivs.addAll(Set.of(civs));
    return this;
  }

  public SearchBuilder addExcludedCivs(Civilization... civs) {
    makeCivSet();
    excludedCivs.addAll(Set.of(civs));
    return this;
  }

  public SearchBuilder setMinCost(Integer minCost) {
    this.minCost = minCost;
    return this;
  }

  public SearchBuilder setMaxCost(Integer maxCost) {
    this.maxCost = maxCost;
    return this;
  }

  public SearchBuilder setMinPower(Integer minPower) {
    this.minPower = minPower;
    return this;
  }

  public SearchBuilder setMaxPower(Integer maxPower) {
    this.maxPower = maxPower;
    return this;
  }

  public SearchBuilder setCardType(SearchFilter.CardType cardType) {
    this.cardType = cardType;
    return this;
  }

  public SearchFilter build() {
    return new SearchFilter(
        setId,
        includedCivs,
        excludedCivs,
        includeMono,
        includeRainbow,
        matchNumberOfCivs,
        minCost,
        maxCost,
        minPower,
        maxPower,
        twinpact,
        cardType,
        rarity,
        speciesSearch,
        nameSearch,
        effectSearch,
        null,
        pageable);
  }
}
