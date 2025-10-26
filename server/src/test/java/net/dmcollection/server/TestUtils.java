package net.dmcollection.server;

import static net.dmcollection.model.card.Civilization.FIRE;
import static net.dmcollection.model.card.Civilization.NATURE;
import static net.dmcollection.model.card.Civilization.WATER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.model.card.CardEntity;
import net.dmcollection.model.card.CardFacet;
import net.dmcollection.model.card.Civilization;
import net.dmcollection.model.card.OfficialSet;
import net.dmcollection.model.card.Rarity;
import net.dmcollection.model.card.RarityCode;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.internal.SearchFilter;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import net.dmcollection.server.card.internal.SearchFilter.FilterState;
import net.dmcollection.server.card.internal.SearchFilter.Range;
import net.dmcollection.server.card.internal.SearchFilter.RarityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;

public class TestUtils {

  public static final String CREATURE = "クリーチャー";
  public static final String SPELL = "呪文";
  public static final String D2_FIELD = "D2フィールド";
  public static final String PSYCHIC_CREATURE = "サイキック・クリーチャー";
  private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

  private final List<LogEntry> sqlLog = new ArrayList<>();
  private final Map<Long, CardStub> testCards = new HashMap<>();
  private final Map<Long, OfficialSet> testSets = new HashMap<>();
  private final Map<String, Long> testSpecies = new HashMap<>();
  private final Map<RarityCode, Rarity> testRarities = new HashMap<>();
  private final Map<String, Long> testEffects = new HashMap<>();
  private final JdbcTemplate jdbcTemplate;
  private long nextId = 1;

  public static final Map<RarityCode, Integer> rarityOrder;

  static {
    rarityOrder =
        Map.of(
            RarityCode.NONE,
            0,
            RarityCode.C,
            1,
            RarityCode.U,
            2,
            RarityCode.R,
            3,
            RarityCode.VR,
            4,
            RarityCode.SR,
            5);
  }

  public TestUtils(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public static SearchBuilder search() {
    return new SearchBuilder();
  }

  private record LogEntry(String sql, @NonNull List<Object> args) {
    LogEntry {
      args = new ArrayList<>(args);
    }
  }

  public void printLog() {
    log.info(
        sqlLog.stream()
            .map(entry -> entry.sql.replaceAll("\\?", "{}") + ";")
            .collect(Collectors.joining("\n", "\n", "")),
        sqlLog.stream().flatMap(entry -> entry.args.stream()).toArray());
  }

  public Map<Long, CardStub> getTestCards() {
    return this.testCards;
  }

  /**
   * Create DMBD13-001, the dreaded four-sided card and add it to the database.
   *
   * @return An instance of DMBD13-001
   */
  public CardStub createFoursides() {
    return card(
        "dmbd13-001",
        "DMBD13 1/26",
        false,
        null,
        201L,
        List.of("dmbd13-001a.jpg", "dmbd13-001b.jpg", "dmbd13-001c.jpg", "dmbd13-001d.jpg"),
        List.of(Set.of(WATER), Set.of(FIRE), Set.of(NATURE), Set.of(WATER, FIRE, NATURE)),
        List.of(7, 7, 7, 21),
        List.of(5000, 5000, 7000, 11000),
        List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE, PSYCHIC_CREATURE, PSYCHIC_CREATURE));
  }

  public CardStub multiCard(String officialId, Civilization... civs) {
    return card(officialId, List.of(officialId + ".jpg"), List.of(Set.of(civs)));
  }

  public CardStub monoCard(String officialId, Civilization civ) {
    return card(officialId, List.of(officialId + ".jpg"), List.of(Set.of(civ)));
  }

  public CardStub monoCard(String officialId, String effectText) {
    return monoCard(officialId, 7, 6500, Civilization.ZERO, effectText);
  }

  public CardStub multiCard(String officialId, Integer cost, Civilization... civs) {
    return card(
        officialId,
        false,
        RarityCode.R,
        1L,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civs)),
        cost != null ? List.of(cost) : null,
        Collections.singletonList(1000),
        List.of(CREATURE));
  }

  public CardStub multiCard(String officialId, Integer cost, Integer power, Civilization... civs) {
    return card(
        officialId,
        false,
        RarityCode.R,
        1L,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civs)),
        cost != null ? List.of(cost) : null,
        Collections.singletonList(power),
        power != null ? List.of(CREATURE) : List.of(SPELL));
  }

  public CardStub monoCard(String officialId, Integer cost, Civilization civ) {
    return card(
        officialId,
        false,
        RarityCode.C,
        1L,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civ)),
        cost != null ? List.of(cost) : null,
        List.of(5000),
        List.of(CREATURE));
  }

  public CardStub monoCard(String officialId, Integer cost, Integer power, Civilization civ) {
    return card(
        officialId,
        false,
        RarityCode.C,
        1L,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civ)),
        cost != null ? List.of(cost) : null,
        Collections.singletonList(power),
        power != null ? List.of(CREATURE) : List.of(SPELL));
  }

  /**
   * Create a mono-civilization card with a single effect (no children).
   *
   * @param officialId The official card ID
   * @param cost The cost
   * @param power The power (null for spells)
   * @param civ The civilization
   * @param effect Single effect text
   * @return The created CardStub
   */
  public CardStub monoCard(
      String officialId, Integer cost, Integer power, Civilization civ, String effect) {
    return monoCard(officialId, cost, power, civ, List.of(List.of(effect)));
  }

  /**
   * Create a mono-civilization card with effects.
   *
   * @param officialId The official card ID
   * @param cost The cost
   * @param power The power (null for spells)
   * @param civ The civilization
   * @param effects Effects for the card. Each inner list represents: [parent, child1, child2, ...]
   * @return The created CardStub
   */
  public CardStub monoCard(
      String officialId,
      Integer cost,
      Integer power,
      Civilization civ,
      List<List<String>> effects) {
    return card(
        officialId,
        officialId,
        false,
        RarityCode.C,
        1L,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civ)),
        cost != null ? List.of(cost) : null,
        Collections.singletonList(power),
        power != null ? List.of(CREATURE) : List.of(SPELL),
        List.of(effects));
  }

  public CardStub twoSided(String officialId, Set<Civilization> civs1, Set<Civilization> civs2) {
    return card(
        officialId,
        false,
        RarityCode.VR,
        1L,
        List.of(officialId + ".jpg", officialId + "b.jpg"),
        List.of(civs1, civs2),
        List.of(6, 13),
        List.of(99999, 1000),
        List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE));
  }

  public CardStub twoSided(
      String officialId, Set<Civilization> civs1, Set<Civilization> civs2, int cost1, int cost2) {
    return card(
        officialId,
        false,
        RarityCode.VR,
        1L,
        List.of(officialId + ".jpg", officialId + "b.jpg"),
        List.of(civs1, civs2),
        List.of(cost1, cost2),
        List.of(4000, 11000),
        List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE));
  }

  public CardStub twoSided(
      String officialId,
      Set<Civilization> civs1,
      Set<Civilization> civs2,
      int cost1,
      int cost2,
      int power1,
      int power2) {
    return card(
        officialId,
        false,
        RarityCode.VR,
        1L,
        List.of(officialId + ".jpg", officialId + "b.jpg"),
        List.of(civs1, civs2),
        List.of(cost1, cost2),
        List.of(power1, power2),
        List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE));
  }

  public CardStub twinpact(String officialId, Set<Civilization> civs1, Set<Civilization> civs2) {
    return card(
        officialId,
        true,
        RarityCode.SR,
        1L,
        List.of(officialId + ".jpg"),
        List.of(civs1, civs2),
        List.of(5, 3),
        Arrays.asList(3000, null),
        List.of(CREATURE, SPELL));
  }

  public CardStub twinpact(
      String officialId, Set<Civilization> civs1, Set<Civilization> civs2, int cost1, int cost2) {
    return card(
        officialId,
        true,
        RarityCode.SR,
        1L,
        List.of(officialId + ".jpg"),
        List.of(civs1, civs2),
        List.of(cost1, cost2),
        Arrays.asList(5000, null),
        List.of(CREATURE, SPELL));
  }

  public CardStub twinpact(
      String officialId,
      Set<Civilization> civs1,
      Set<Civilization> civs2,
      int cost1,
      int cost2,
      int power1) {
    return card(
        officialId,
        true,
        RarityCode.SR,
        1L,
        List.of(officialId + ".jpg"),
        List.of(civs1, civs2),
        List.of(cost1, cost2),
        Arrays.asList(power1, null),
        List.of(CREATURE, SPELL));
  }

  public CardStub card(
      String officialId, List<String> imageFiles, List<Set<Civilization>> facetCivs) {
    return card(
        officialId,
        false,
        RarityCode.C,
        1L,
        imageFiles,
        facetCivs,
        List.of(5),
        List.of(1000),
        List.of(CREATURE));
  }

  public CardStub card(
      String officialId,
      boolean twinpact,
      RarityCode rarity,
      Long setId,
      List<String> imageFiles,
      List<Set<Civilization>> facetCivs,
      List<Integer> costs,
      List<Integer> powers,
      List<String> facetTypes) {
    return card(
        officialId,
        officialId,
        twinpact,
        rarity,
        setId,
        imageFiles,
        facetCivs,
        costs,
        powers,
        facetTypes);
  }

  /**
   * Create a card with effects. Effects are specified as a list of lists where each inner list
   * represents one parent effect followed by its child effects.
   *
   * @param officialId The official card ID
   * @param idText The ID text
   * @param twinpact Whether this is a twinpact card
   * @param rarity The rarity code
   * @param setId The set ID
   * @param imageFiles Image filenames for each facet
   * @param facetCivs Civilizations for each facet
   * @param costs Costs for each facet
   * @param powers Powers for each facet
   * @param facetTypes Types for each facet
   * @param facetEffects Effects for each facet. Each inner list represents: [parent, child1,
   *     child2, ...]. Use null for facets without effects.
   * @return The created CardStub
   */
  public CardStub card(
      String officialId,
      String idText,
      boolean twinpact,
      RarityCode rarity,
      Long setId,
      List<String> imageFiles,
      List<Set<Civilization>> facetCivs,
      List<Integer> costs,
      List<Integer> powers,
      List<String> facetTypes,
      List<List<List<String>>> facetEffects) {
    // Create the card without effects first
    CardStub card =
        card(
            officialId,
            idText,
            twinpact,
            rarity,
            setId,
            imageFiles,
            facetCivs,
            costs,
            powers,
            facetTypes);

    // Add effects to each facet
    if (facetEffects != null) {
      for (int facetIndex = 0; facetIndex < facetEffects.size(); facetIndex++) {
        List<List<String>> effectGroups = facetEffects.get(facetIndex);
        if (effectGroups != null) {
          long facetId = card.id() + 1 + facetIndex;
          for (int effectPos = 0; effectPos < effectGroups.size(); effectPos++) {
            List<String> effectGroup = effectGroups.get(effectPos);
            if (effectGroup != null && !effectGroup.isEmpty()) {
              // First element is parent effect
              long parentId = addEffectToFacet(facetId, effectPos, effectGroup.getFirst());
              // Remaining elements are child effects
              for (int childIndex = 1; childIndex < effectGroup.size(); childIndex++) {
                addEffectToFacet(facetId, childIndex - 1, effectGroup.get(childIndex), parentId);
              }
            }
          }
        }
      }
    }

    return card;
  }

  public CardStub card(
      String officialId,
      String idText,
      boolean twinpact,
      RarityCode rarity,
      Long setId,
      List<String> imageFiles,
      List<Set<Civilization>> facetCivs,
      List<Integer> costs,
      List<Integer> powers,
      List<String> facetTypes) {
    Long cardId = nextId++;

    List<Object> args = new ArrayList<>();

    if (!testSets.containsKey(setId)) {
      String insert = "INSERT INTO CARD_SET (ID, DM_ID, NAME) VALUES (?, ?, ?)";
      args.addAll(List.of(setId, "DM-" + setId, "Set " + setId));
      sqlLog.add(new LogEntry(insert, args));
      jdbcTemplate.update(insert, args.toArray());
      args.clear();
      testSets.put(
          setId, new OfficialSet(setId, "DM-" + setId, "Test Set " + setId, LocalDate.now()));
    }

    if (rarity != null && !testRarities.containsKey(rarity)) {
      String insert = "INSERT INTO RARITY (CODE, NAME, \"ORDER\") VALUES (?, ?, ?)";
      args.addAll(
          List.of(rarity.toString(), rarity.toString(), rarityOrder.getOrDefault(rarity, 0)));
      sqlLog.add(new LogEntry(insert, args));
      jdbcTemplate.update(insert, args.toArray());
      args.clear();
      testRarities.put(
          rarity, new Rarity(rarity, rarityOrder.getOrDefault(rarity, 0), rarity.toString()));
    }

    String insert =
        "INSERT INTO CARDS (\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\") VALUES (?, ?, ?, ?, ?, ?)"
            .formatted(
                CardEntity.Columns.ID,
                CardEntity.Columns.ID_TEXT,
                CardEntity.Columns.OFFICIAL_ID,
                CardEntity.Columns.SET,
                CardEntity.Columns.TWINPACT,
                CardEntity.Columns.RARITY);
    args.add(cardId);
    args.add(idText);
    args.add(officialId);
    args.add(setId);
    args.add(twinpact);
    args.add(rarity != null && rarity != RarityCode.NONE ? rarity.toString() : null);
    sqlLog.add(new LogEntry(insert, args));
    jdbcTemplate.update(insert, args.toArray());
    args.clear();
    List<Civilization> allCivs = new ArrayList<>();
    for (int i = 0; i < facetCivs.size(); i++) {
      Long facetId = nextId++;
      String imageFile = imageFiles != null && imageFiles.size() > i ? imageFiles.get(i) : null;
      Integer cost = costs != null && costs.size() > i ? costs.get(i) : null;
      Integer power = powers != null && i < powers.size() ? powers.get(i) : null;
      insert =
          "INSERT INTO CARD_FACETS (ID, \"%s\", CARDS, POSITION, IMAGE_FILENAME, COST, CIVS, POWER, POWER_SORT, TYPE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
              .formatted(CardFacet.Columns.NAME);
      args.add(facetId);
      args.add(officialId);
      args.add(cardId);
      args.add(i);
      args.add(imageFile);
      args.add(cost);
      args.add(Civilization.toInts(facetCivs.get(i)).toArray());
      args.add(power);
      args.add(power);
      args.add(facetTypes != null && i < facetTypes.size() ? facetTypes.get(i) : null);
      sqlLog.add(new LogEntry(insert, args));
      jdbcTemplate.update(insert, args.toArray());
      args.clear();
      allCivs.addAll(facetCivs.get(i));
    }

    CardStub stub =
        new CardStub(
            cardId,
            officialId,
            idText,
            new HashSet<>(allCivs),
            imageFiles != null
                ? imageFiles.stream()
                    .filter(Objects::nonNull)
                    .map(image -> "/image/" + image)
                    .toList()
                : Collections.emptyList(),
            0,
            0);
    testCards.put(cardId, stub);
    return stub;
  }

  public void addSpeciesToFacet(long facetId, int position, String species) {
    long speciesId = testSpecies.getOrDefault(species, nextId++);
    if (!testSpecies.containsKey(species)) {
      Object[] values = {speciesId, species};
      jdbcTemplate.update("INSERT INTO SPECIES (ID, SPECIES) VALUES (?, ?)", values);
      testSpecies.put(species, speciesId);
    }
    Object[] values = {facetId, position, speciesId};
    jdbcTemplate.update(
        "INSERT INTO FACET_SPECIES (CARD_FACETS, POSITION, SPECIES) VALUES (?, ?, ?)", values);
  }

  /**
   * Adds an effect to a facet. Supports parent-child hierarchy. Reuses existing effects if the
   * effect text already exists in the database.
   *
   * @param facetId The facet ID to add the effect to
   * @param position The position of the effect on the facet
   * @param effectText The effect text
   * @param parentEffectId Optional parent effect ID for child effects (null for parent effects)
   * @return The ID of the effect (existing or newly created)
   */
  public long addEffectToFacet(long facetId, int position, String effectText, Long parentEffectId) {
    String effectKey = effectText + "|" + (parentEffectId != null ? parentEffectId : "root");

    // Check if effect already exists
    Long effectId = testEffects.get(effectKey);
    if (effectId == null) {
      effectId = nextId++;
      // Insert into EFFECT table
      Object[] effectValues = {effectId, effectText, parentEffectId, 0};
      jdbcTemplate.update(
          "INSERT INTO EFFECT (ID, TEXT, PARENT, \"POSITION\") VALUES (?, ?, ?, ?)", effectValues);
      testEffects.put(effectKey, effectId);
    }

    // Only parent effects are linked to facets via FACET_EFFECT
    if (parentEffectId == null) {
      Object[] facetEffectValues = {facetId, position, effectId};
      jdbcTemplate.update(
          "INSERT INTO FACET_EFFECT (CARD_FACETS, \"POSITION\", EFFECT) VALUES (?, ?, ?)",
          facetEffectValues);
    }

    return effectId;
  }

  /**
   * Convenience method to add a parent effect without children.
   *
   * @param facetId The facet ID to add the effect to
   * @param position The position of the effect on the facet
   * @param effectText The effect text
   * @return The ID of the created effect
   */
  public long addEffectToFacet(long facetId, int position, String effectText) {
    return addEffectToFacet(facetId, position, effectText, null);
  }

  public static class SearchBuilder {
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
    private FilterState twinpact = FilterState.IN;
    private CardType cardType = null;
    private String speciesSearch = null;
    private String effectSearch = null;
    private RarityFilter rarity = null;
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
      this.rarity = new RarityFilter(rarity, Range.EQ);
      return this;
    }

    public SearchBuilder setRarity(RarityCode rarity, Range range) {
      this.rarity = new RarityFilter(rarity, range);
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

    public SearchBuilder setTwinpact(FilterState twinpact) {
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

    public SearchBuilder setCardType(CardType cardType) {
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
}
