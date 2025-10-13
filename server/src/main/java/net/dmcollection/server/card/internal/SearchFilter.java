package net.dmcollection.server.card.internal;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.model.card.Civilization;
import net.dmcollection.model.card.RarityCode;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

/**
 * Search filter for card search.
 *
 * @param setId The id of a set to filter by. If null, all sets are included.
 * @param includedCivs Civilizations that a card should have one of to be included. If empty or
 *     null, all civilizations are included.
 * @param excludedCivs Civilizations that a card must not have to be included. If empty or null, no
 *     civilizations are excluded. If {@code includedCivs} is empty, this field is ignored.
 *     Civilizations that are both included and excluded are treated as included. If {@code
 *     matchExactRainbowCivs} is {@code true}, this field is ignored.
 * @param includeMono Whether monochrome cards should be included. A card is monochrome if one of
 *     its facets has only one civilization. In the case of twinpacts, both sideds need to have the
 *     same civilization. {@code null} is interpreted as {@code false} unless {@code includeRainbow}
 *     is also {@code null}.
 * @param includeRainbow Whether multicolored cards should be included. A card is multicolored if
 *     one of its facets has multiple civilizations. An exception are twinpacts, where both facets
 *     having different civilizations makes it a multicolored card. {@code null} is interpreted as
 *     {@code false} unless {@code includeMono} is also {@code null}.
 * @param matchExactRainbowCivs Only valid if {@code includeRainbow} is {@code true}. If {@code
 *     true}, a multicolored card is only included if it has exactly the included civilizations and
 *     not more or fewer. If monochrome cards are not included, the included civilizations must be
 *     more than one, not counting the zero civilization. Twinpact cards are included if one of the
 *     facets has the required configuration of civilizations or if both facets together have the
 *     required configuration.
 * @param minCost The minimum cost of a card to be included. If null, there is no minimum.
 * @param maxCost The maximum cost of a card to be included. If null, there is no maximum.
 * @param twinpact Whether twinpact cards should be included, excluded or only twinpact cards should
 *     be included. Default is {@code IN}.
 * @param cardType Include only cards of this type.
 * @param rarityFilter Include only cards matching the rarity filter.
 * @param speciesSearch A string contained in one or more species. Only cards of those will be
 *     included.
 * @param nameSearch A string contained in the card name. Only cards with matching names will be
 *     included.
 * @param effectSearch A string contained in the effect text. Only cards with matching effects will
 *     be included. Searches both parent and child effects.
 * @param pageable Spring pageable object for pagination and sorting. If null, no paging is used.
 */
public record SearchFilter(
    Long setId,
    Set<Civilization> includedCivs,
    Set<Civilization> excludedCivs,
    Boolean includeMono,
    Boolean includeRainbow,
    boolean matchExactRainbowCivs,
    Integer minCost,
    Integer maxCost,
    Integer minPower,
    Integer maxPower,
    FilterState twinpact,
    CardType cardType,
    RarityFilter rarityFilter,
    String speciesSearch,
    String nameSearch,
    String effectSearch,
    CollectionFilter collectionFilter,
    Pageable pageable) {

  public SearchFilter {
    if (minCost != null && maxCost != null) {
      if (minCost > maxCost) {
        int tmp = maxCost;
        maxCost = minCost;
        minCost = tmp;
      }
    }
    if (maxCost != null && maxCost == Integer.MAX_VALUE) {
      maxCost -= 1; // wouldn't want to find anything with "infinite" cost here
    }
    if (minCost != null && minCost == Integer.MAX_VALUE) {
      minCost -= 1; // wouldn't want to exclude anything with "infinite" cost here
    }

    if (minPower != null && maxPower != null) {
      if (minPower > maxPower) {
        int tmp = maxPower;
        maxPower = minPower;
        minPower = tmp;
      }
    }
    if (maxPower != null && maxPower == Integer.MAX_VALUE) {
      maxPower -= 1; // wouldn't want to find anything with "infinite" power here
    }
    if (minPower != null && minPower == Integer.MAX_VALUE) {
      minPower -= 1; // wouldn't want to exclude anything with "infinite" power here
    }
    if (pageable == null) {
      pageable = org.springframework.data.domain.Pageable.unpaged();
    }
    includedCivs =
        includedCivs != null && !includedCivs.isEmpty()
            ? EnumSet.copyOf(includedCivs)
            : EnumSet.allOf(Civilization.class);
    if (matchExactRainbowCivs) {
      excludedCivs = EnumSet.complementOf(EnumSet.copyOf(includedCivs));
    } else {
      if (excludedCivs == null || excludedCivs.isEmpty()) {
        excludedCivs = EnumSet.noneOf(Civilization.class);
      } else {
        final EnumSet<Civilization> excludedCivsCopy = EnumSet.copyOf(excludedCivs);
        excludedCivsCopy.removeAll(includedCivs);
        excludedCivs = excludedCivsCopy;
      }
    }

    if (includeMono == null && includeRainbow == null) {
      includeMono = true;
      includeRainbow = true;
    } else {
      includeMono = !Boolean.FALSE.equals(includeMono);
      includeRainbow = !Boolean.FALSE.equals(includeRainbow);
      if (includeMono.equals(includeRainbow)) {
        includeMono = true;
        includeRainbow = true;
      }
    }
    if (!includeRainbow) {
      matchExactRainbowCivs = false;
    }
    if (twinpact == null) {
      twinpact = FilterState.IN;
    }
  }

  public Set<Civilization> includedCivsWithoutZero() {
    return includedCivs().stream().filter(c -> c != Civilization.ZERO).collect(Collectors.toSet());
  }

  public boolean isInvalid() {
    long rainbowCivsCount = includedCivs().stream().filter(c -> c != Civilization.ZERO).count();
    if (!includeMono) {
      if (rainbowCivsCount == 0) {
        // invalid search for rainbows matching only zero
        return true;
      }
      // invalid search for rainbows with only one civ
      return rainbowCivsCount == 1 && matchExactRainbowCivs;
    }
    return false;
  }

  public @NonNull Set<Civilization> includedCivs() {
    return includedCivs;
  }

  public @NonNull Set<Civilization> excludedCivs() {
    return excludedCivs;
  }

  public @NonNull Boolean includeMono() {
    return includeMono;
  }

  public @NonNull Boolean includeRainbow() {
    return includeRainbow;
  }

  public boolean needsCardColumnsFilter() {
    return setId != null || (rarityFilter != null) || twinpact != FilterState.IN;
  }

  public boolean needsCivFilter() {
    boolean isDefault =
        includedCivs().size() == Civilization.values().length
            && includeMono()
            && includeRainbow()
            && !matchExactRainbowCivs;
    return !isDefault;
  }

  public boolean needsFacetColumnFilter() {
    return minCost != null
        || maxCost != null
        || minPower != null
        || maxPower != null
        || cardType != null
        || (nameSearch != null && !nameSearch.isBlank());
  }

  public record CollectionFilter(long internalId, boolean searchCollection) {}

  public SearchFilter withCollectionFilter(long internalId, boolean searchCollection) {
    return new SearchFilter(
        this.setId,
        this.includedCivs,
        this.excludedCivs,
        this.includeMono,
        this.includeRainbow,
        this.matchExactRainbowCivs,
        this.minCost,
        this.maxCost,
        this.minPower,
        this.maxPower,
        this.twinpact,
        this.cardType,
        this.rarityFilter,
        this.speciesSearch,
        this.nameSearch,
        this.effectSearch,
        new CollectionFilter(internalId, searchCollection),
        this.pageable);
  }

  public enum FilterState {
    IN, // include
    EX, // exclude
    ONLY // exclude others
  }

  public enum Range {
    LE,
    EQ,
    GE
  }

  public record RarityFilter(RarityCode rarityCode, Range range) {}

  public enum CardType {
    CREATURE("クリーチャー"),
    SPELL("呪文"),
    EVOLUTION("進化"),
    PSYCHIC("サイキック"),
    DRAGHEART("ドラグハート"),
    FIELD("フィールド"),
    CASTLE("城"),
    CROSSGEAR("クロスギア"),
    EXILE("エグザイル"),
    GACHALLENGE("GR"),
    AURA("オーラ"),
    TAMASEED("タマシード"),
    OTHER("その他");

    private final String keyWord;

    public String getKeyWord() {
      return this.keyWord;
    }

    CardType(String keyWord) {
      this.keyWord = keyWord;
    }
  }
}
