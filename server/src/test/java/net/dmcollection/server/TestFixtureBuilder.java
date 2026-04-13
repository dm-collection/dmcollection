package net.dmcollection.server;

import static net.dmcollection.server.card.Civilization.FIRE;
import static net.dmcollection.server.card.Civilization.NATURE;
import static net.dmcollection.server.card.Civilization.WATER;
import static net.dmcollection.server.jooq.generated.Tables.ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.CARD;
import static net.dmcollection.server.jooq.generated.Tables.CARD_CIV_GROUP;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SET;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_CARD_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_RACE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE_ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.PRODUCT_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.RACE;
import static net.dmcollection.server.jooq.generated.Tables.RARITY;
import static net.dmcollection.server.jooq.generated.Tables.SET_GROUP;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.Civilization;
import net.dmcollection.server.card.RarityCode;
import net.dmcollection.server.card.internal.SearchFilter;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import net.dmcollection.server.card.internal.SearchFilter.FilterState;
import net.dmcollection.server.card.internal.SearchFilter.Range;
import net.dmcollection.server.card.internal.SearchFilter.RarityFilter;
import net.dmcollection.server.card.internal.query.CardTypeResolver;
import org.jooq.DSLContext;
import org.springframework.data.domain.Pageable;

public class TestFixtureBuilder {

  public static final String CREATURE = "クリーチャー";
  public static final String SPELL = "呪文";
  public static final String D2_FIELD = "D2フィールド";
  public static final String PSYCHIC_CREATURE = "サイキック・クリーチャー";

  private final DSLContext dsl;
  private final Map<Long, CardStub> testCards = new HashMap<>();
  private final Map<Integer, Integer> testSets = new HashMap<>(); // setId param -> card_set.id
  private final Map<String, Short> cardTypes = new HashMap<>();
  private final Map<String, Short> races = new HashMap<>();
  private final Map<String, Integer> abilities = new HashMap<>();
  private final Map<RarityCode, Short> rarities = new EnumMap<>(RarityCode.class);
  private final CardTypeResolver cardTypeResolver;

  private Short defaultProductTypeId;
  private Integer defaultSetGroupId;

  public static final Map<RarityCode, Integer> rarityOrder;

  static {
    rarityOrder =
        Map.of(
            RarityCode.NONE, 0,
            RarityCode.C, 1,
            RarityCode.U, 2,
            RarityCode.R, 3,
            RarityCode.VR, 4,
            RarityCode.SR, 5);
  }

  public TestFixtureBuilder(DSLContext dsl, CardTypeResolver cardTypeResolver) {
    this.dsl = dsl;
    this.cardTypeResolver = cardTypeResolver;
  }

  public static SearchBuilder search() {
    return new SearchBuilder();
  }

  public Map<Long, CardStub> getTestCards() {
    return this.testCards;
  }

  /** Returns the actual DB card_set.id for a given set parameter used in card creation. */
  public int getCardSetId(int setParam) {
    return testSets.get(setParam);
  }

  // --- Convenience card creation methods ---

  public CardStub monoCard(String officialId, Civilization civ) {
    return card(officialId, List.of(officialId + ".jpg"), List.of(Set.of(civ)));
  }

  public CardStub monoCard(String officialId, Civilization civ, String cardType) {
    return card(
        officialId,
        false,
        RarityCode.VR,
        1,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civ)),
        List.of(6),
        List.of(8000),
        List.of(cardType));
  }

  public CardStub monoCard(String officialId, String effectText) {
    return monoCard(officialId, 7, 6500, Civilization.ZERO, effectText);
  }

  public CardStub monoCard(String officialId, Integer cost, Civilization civ) {
    return card(
        officialId,
        false,
        RarityCode.C,
        1,
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
        1,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civ)),
        cost != null ? List.of(cost) : null,
        Collections.singletonList(power),
        power != null ? List.of(CREATURE) : List.of(SPELL));
  }

  public CardStub monoCard(
      String officialId, Integer cost, Integer power, Civilization civ, String effect) {
    return monoCard(officialId, cost, power, civ, List.of(List.of(effect)));
  }

  public CardStub monoCard(
      String officialId,
      Integer cost,
      Integer power,
      Civilization civ,
      String effect,
      String species) {
    return card(
        officialId,
        officialId,
        false,
        RarityCode.C,
        1,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civ)),
        cost != null ? List.of(cost) : null,
        Collections.singletonList(power),
        power != null ? List.of(CREATURE) : List.of(SPELL),
        List.of(List.of(List.of(effect))),
        List.of(species));
  }

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
        1,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civ)),
        cost != null ? List.of(cost) : null,
        Collections.singletonList(power),
        power != null ? List.of(CREATURE) : List.of(SPELL),
        List.of(effects),
        null);
  }

  /** Add a species (race) to the first side of a card. */
  public void addSpecies(CardStub card, String speciesName) {
    // Look up the printing, then its first printing_side, then the card_side_id
    int printingId = card.id().intValue();
    Integer cardSideId =
        dsl.select(PRINTING_SIDE.CARD_SIDE_ID)
            .from(PRINTING_SIDE)
            .where(PRINTING_SIDE.PRINTING_ID.eq(printingId))
            .orderBy(PRINTING_SIDE.ID.asc())
            .limit(1)
            .fetchOne(PRINTING_SIDE.CARD_SIDE_ID);

    short raceId = ensureRace(speciesName);
    // Use position based on existing race count for this side
    short position =
        (short)
            dsl.fetchCount(
                dsl.selectFrom(CARD_SIDE_RACE).where(CARD_SIDE_RACE.CARD_SIDE_ID.eq(cardSideId)));
    dsl.insertInto(CARD_SIDE_RACE)
        .set(CARD_SIDE_RACE.CARD_SIDE_ID, cardSideId)
        .set(CARD_SIDE_RACE.RACE_ID, raceId)
        .set(CARD_SIDE_RACE.POSITION, position)
        .execute();
  }

  public CardStub multiCard(String officialId, Civilization... civs) {
    return card(officialId, List.of(officialId + ".jpg"), List.of(Set.of(civs)));
  }

  public CardStub multiCard(String officialId, Integer cost, Civilization... civs) {
    return card(
        officialId,
        false,
        RarityCode.R,
        1,
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
        1,
        List.of(officialId + ".jpg"),
        List.of(Set.of(civs)),
        cost != null ? List.of(cost) : null,
        Collections.singletonList(power),
        power != null ? List.of(CREATURE) : List.of(SPELL));
  }

  public CardStub twinpact(String officialId, Set<Civilization> civs1, Set<Civilization> civs2) {
    return card(
        officialId,
        true,
        RarityCode.SR,
        1,
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
        1,
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
        1,
        List.of(officialId + ".jpg"),
        List.of(civs1, civs2),
        List.of(cost1, cost2),
        Arrays.asList(power1, null),
        List.of(CREATURE, SPELL));
  }

  public CardStub twoSided(String officialId, Set<Civilization> civs1, Set<Civilization> civs2) {
    return card(
        officialId,
        false,
        RarityCode.VR,
        1,
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
        1,
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
        1,
        List.of(officialId + ".jpg", officialId + "b.jpg"),
        List.of(civs1, civs2),
        List.of(cost1, cost2),
        List.of(power1, power2),
        List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE));
  }

  public CardStub twoSided(
      String officialId,
      Set<Civilization> civs1,
      Set<Civilization> civs2,
      Integer cost1,
      int cost2,
      Integer power1,
      int power2,
      String cardType1,
      String cardType2) {
    var costs = new ArrayList<Integer>();
    costs.add(cost1);
    costs.add(cost2);
    var powers = new ArrayList<Integer>();
    powers.add(power1);
    powers.add(power2);

    return card(
        officialId,
        false,
        RarityCode.VR,
        1,
        List.of(officialId + ".jpg", officialId + "b.jpg"),
        List.of(civs1, civs2),
        costs,
        powers,
        List.of(cardType1, cardType2));
  }

  public CardStub createFoursides() {
    return card(
        "dmbd13-001",
        "DMBD13 1/26",
        false,
        null,
        201,
        List.of("dmbd13-001a.jpg", "dmbd13-001b.jpg", "dmbd13-001c.jpg", "dmbd13-001d.jpg"),
        List.of(Set.of(WATER), Set.of(FIRE), Set.of(NATURE), Set.of(WATER, FIRE, NATURE)),
        List.of(7, 7, 7, 21),
        List.of(5000, 5000, 7000, 11000),
        List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE, PSYCHIC_CREATURE, PSYCHIC_CREATURE));
  }

  public CardStub card(
      String officialId, List<String> imageFiles, List<Set<Civilization>> facetCivs) {
    return card(
        officialId,
        false,
        RarityCode.C,
        1,
        imageFiles,
        facetCivs,
        List.of(5),
        List.of(1000),
        List.of(CREATURE));
  }

  public CardStub card(
      String officialId,
      String idText,
      boolean twinpact,
      RarityCode rarity,
      long setId,
      List<String> imageFiles,
      List<Set<Civilization>> facetCivs,
      List<Integer> costs,
      List<Integer> powers,
      List<String> facetTypes) {
    return card(
        officialId,
        idText,
        twinpact,
        rarity,
        (int) setId,
        imageFiles,
        facetCivs,
        costs,
        powers,
        facetTypes,
        null,
        null);
  }

  public CardStub card(
      String officialId,
      boolean twinpact,
      RarityCode rarity,
      int setId,
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
        facetTypes,
        null,
        null);
  }

  /**
   * Creates a full card fixture: card, card_sides, card_civ_groups, printing, printing_sides, and
   * optionally effects and species.
   *
   * @param facetEffects Effects per facet. Each inner list is [parent, child1, child2, ...]. null
   *     means no effects.
   * @param facetSpecies Species name for first side. null means no species.
   */
  public CardStub card(
      String officialId,
      String idText,
      boolean twinpact,
      RarityCode rarity,
      int setId,
      List<String> imageFiles,
      List<Set<Civilization>> facetCivs,
      List<Integer> costs,
      List<Integer> powers,
      List<String> facetTypes,
      List<List<List<String>>> facetEffects,
      List<String> facetSpecies) {

    ensureDefaultLookups();
    int cardSetId = ensureCardSet(setId);
    Short rarityId = rarity != null ? ensureRarity(rarity) : null;

    // Compute sort values from first side
    Integer sortCost = costs != null && !costs.isEmpty() ? costs.getFirst() : null;
    Integer sortPower = powers != null && !powers.isEmpty() ? powers.getFirst() : null;

    // For multi-sided cards, sort_cost/sort_power is the max across sides
    if (costs != null) {
      for (Integer c : costs) {
        if (c != null && (sortCost == null || c > sortCost)) sortCost = c;
      }
    }
    if (powers != null) {
      for (Integer p : powers) {
        if (p != null && (sortPower == null || p > sortPower)) sortPower = p;
      }
    }

    // Compute sort_civilization (union of all side civs, as sorted smallint[])
    Set<Civilization> allCivs = new LinkedHashSet<>();
    for (Set<Civilization> sideCivs : facetCivs) {
      allCivs.addAll(sideCivs);
    }
    Short[] sortCivilization =
        allCivs.stream()
            .filter(c -> c != Civilization.ZERO)
            .map(c -> (short) c.ordinal())
            .sorted()
            .toArray(Short[]::new);

    // Insert card
    Integer cardId =
        dsl.insertInto(CARD)
            .set(CARD.NAME, officialId) // use officialId as card name for test fixtures
            .set(CARD.IS_TWINPACT, twinpact)
            .set(CARD.SORT_COST, sortCost)
            .set(CARD.SORT_POWER, sortPower)
            .set(CARD.SORT_CIVILIZATION, sortCivilization)
            .returningResult(CARD.ID)
            .fetchOne(CARD.ID);

    // Insert card sides and collect their IDs
    List<Integer> cardSideIds = new ArrayList<>();
    for (int i = 0; i < facetCivs.size(); i++) {
      Integer cost = costs != null && costs.size() > i ? costs.get(i) : null;
      Integer power = powers != null && i < powers.size() ? powers.get(i) : null;
      boolean costIsInfinity = cost != null && cost == Integer.MAX_VALUE;
      boolean powerIsInfinity = power != null && power == Integer.MAX_VALUE;

      Short[] civIds =
          facetCivs.get(i).stream()
              .filter(c -> c != Civilization.ZERO)
              .map(c -> (short) c.ordinal())
              .sorted()
              .toArray(Short[]::new);

      Integer cardSideId =
          dsl.insertInto(CARD_SIDE)
              .set(CARD_SIDE.CARD_ID, cardId)
              .set(CARD_SIDE.SIDE_ORDER, (short) i)
              .set(CARD_SIDE.NAME, officialId + (facetCivs.size() > 1 ? "-side" + i : ""))
              .set(CARD_SIDE.COST, costIsInfinity ? null : cost)
              .set(CARD_SIDE.COST_IS_INFINITY, costIsInfinity)
              .set(CARD_SIDE.POWER, powerIsInfinity ? null : power)
              .set(CARD_SIDE.POWER_IS_INFINITY, powerIsInfinity)
              .set(CARD_SIDE.CIVILIZATION_IDS, civIds)
              .returningResult(CARD_SIDE.ID)
              .fetchOne(CARD_SIDE.ID);
      cardSideIds.add(cardSideId);

      // Insert card_side_card_type
      String facetType = facetTypes != null && i < facetTypes.size() ? facetTypes.get(i) : null;
      if (facetType != null) {
        short cardTypeId = ensureCardType(facetType);
        dsl.insertInto(CARD_SIDE_CARD_TYPE)
            .set(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID, cardSideId)
            .set(CARD_SIDE_CARD_TYPE.CARD_TYPE_ID, cardTypeId)
            .set(CARD_SIDE_CARD_TYPE.POSITION, (short) 0)
            .execute();
      }

      // Insert species for this side
      if (facetSpecies != null && i < facetSpecies.size() && facetSpecies.get(i) != null) {
        short raceId = ensureRace(facetSpecies.get(i));
        dsl.insertInto(CARD_SIDE_RACE)
            .set(CARD_SIDE_RACE.CARD_SIDE_ID, cardSideId)
            .set(CARD_SIDE_RACE.RACE_ID, raceId)
            .set(CARD_SIDE_RACE.POSITION, (short) 0)
            .execute();
      }
    }

    // Insert card_civ_group rows
    insertCivGroups(cardId, twinpact, facetCivs);

    // Insert printing
    Integer printingId =
        dsl.insertInto(PRINTING)
            .set(PRINTING.CARD_ID, cardId)
            .set(PRINTING.SET_ID, cardSetId)
            .set(PRINTING.OFFICIAL_SITE_ID, officialId)
            .set(PRINTING.COLLECTOR_NUMBER, idText)
            .set(PRINTING.RARITY_ID, rarityId)
            .returningResult(PRINTING.ID)
            .fetchOne(PRINTING.ID);

    // Insert printing sides
    List<Integer> printingSideIds = new ArrayList<>();
    for (int i = 0; i < cardSideIds.size(); i++) {
      String imageFile = imageFiles != null && imageFiles.size() > i ? imageFiles.get(i) : null;
      Integer printingSideId =
          dsl.insertInto(PRINTING_SIDE)
              .set(PRINTING_SIDE.PRINTING_ID, printingId)
              .set(PRINTING_SIDE.CARD_SIDE_ID, cardSideIds.get(i))
              .set(PRINTING_SIDE.IMAGE_FILENAME, imageFile)
              .returningResult(PRINTING_SIDE.ID)
              .fetchOne(PRINTING_SIDE.ID);
      printingSideIds.add(printingSideId);
    }

    // Insert effects
    if (facetEffects != null) {
      for (int facetIndex = 0; facetIndex < facetEffects.size(); facetIndex++) {
        List<List<String>> effectGroups = facetEffects.get(facetIndex);
        if (effectGroups != null && facetIndex < printingSideIds.size()) {
          int printingSideId = printingSideIds.get(facetIndex);
          short position = 0;
          for (List<String> effectGroup : effectGroups) {
            if (effectGroup != null && !effectGroup.isEmpty()) {
              // First element is parent, rest are children — all are separate abilities
              // In the new schema, abilities are flat (no parent-child in ability table)
              // but indent_level distinguishes them
              int parentAbilityId = ensureAbility(effectGroup.getFirst());
              dsl.insertInto(PRINTING_SIDE_ABILITY)
                  .set(PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID, printingSideId)
                  .set(PRINTING_SIDE_ABILITY.ABILITY_ID, parentAbilityId)
                  .set(PRINTING_SIDE_ABILITY.POSITION, position)
                  .set(PRINTING_SIDE_ABILITY.INDENT_LEVEL, (short) 0)
                  .execute();
              position++;
              for (int childIndex = 1; childIndex < effectGroup.size(); childIndex++) {
                int childAbilityId = ensureAbility(effectGroup.get(childIndex));
                dsl.insertInto(PRINTING_SIDE_ABILITY)
                    .set(PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID, printingSideId)
                    .set(PRINTING_SIDE_ABILITY.ABILITY_ID, childAbilityId)
                    .set(PRINTING_SIDE_ABILITY.POSITION, position)
                    .set(PRINTING_SIDE_ABILITY.INDENT_LEVEL, (short) 1)
                    .execute();
                position++;
              }
            }
          }
        }
      }
    }

    // Build CardStub
    List<String> imageUrls =
        imageFiles != null
            ? imageFiles.stream().filter(Objects::nonNull).map(image -> "/image/" + image).toList()
            : Collections.emptyList();

    CardStub stub =
        new CardStub(
            (long) printingId, officialId, idText, new HashSet<>(allCivs), imageUrls, 0, 0);
    testCards.put((long) printingId, stub);
    return stub;
  }

  private void insertCivGroups(int cardId, boolean twinpact, List<Set<Civilization>> facetCivs) {
    if (twinpact) {
      // Twinpact: single row with union of all sides' civilizations
      Set<Civilization> union = new LinkedHashSet<>();
      boolean includesColorlessSide = false;
      for (Set<Civilization> sideCivs : facetCivs) {
        Set<Civilization> nonZero =
            sideCivs.stream().filter(c -> c != Civilization.ZERO).collect(Collectors.toSet());
        if (nonZero.isEmpty()) {
          includesColorlessSide = true;
        }
        union.addAll(nonZero);
      }
      Short[] civIds = union.stream().map(c -> (short) c.ordinal()).sorted().toArray(Short[]::new);
      dsl.insertInto(CARD_CIV_GROUP)
          .set(CARD_CIV_GROUP.CARD_ID, cardId)
          .set(CARD_CIV_GROUP.CIVILIZATION_IDS, civIds)
          .set(CARD_CIV_GROUP.INCLUDES_COLORLESS_SIDE, includesColorlessSide)
          .execute();
    } else {
      // Non-twinpact: one row per side
      for (Set<Civilization> sideCivs : facetCivs) {
        Set<Civilization> nonZero =
            sideCivs.stream().filter(c -> c != Civilization.ZERO).collect(Collectors.toSet());
        Short[] civIds =
            nonZero.stream().map(c -> (short) c.ordinal()).sorted().toArray(Short[]::new);
        boolean includesColorlessSide = nonZero.isEmpty();
        dsl.insertInto(CARD_CIV_GROUP)
            .set(CARD_CIV_GROUP.CARD_ID, cardId)
            .set(CARD_CIV_GROUP.CIVILIZATION_IDS, civIds)
            .set(CARD_CIV_GROUP.INCLUDES_COLORLESS_SIDE, includesColorlessSide)
            .execute();
      }
    }
  }

  private void ensureDefaultLookups() {
    if (defaultProductTypeId == null) {
      defaultProductTypeId =
          dsl.select(PRODUCT_TYPE.ID)
              .from(PRODUCT_TYPE)
              .where(PRODUCT_TYPE.NAME.eq("ブースターパック"))
              .fetchOne(PRODUCT_TYPE.ID);
      if (defaultProductTypeId == null) {
        defaultProductTypeId =
            dsl.insertInto(PRODUCT_TYPE)
                .set(PRODUCT_TYPE.NAME, "ブースターパック")
                .returningResult(PRODUCT_TYPE.ID)
                .fetchOne(PRODUCT_TYPE.ID);
      }
    }
    if (defaultSetGroupId == null) {
      defaultSetGroupId =
          dsl.select(SET_GROUP.ID)
              .from(SET_GROUP)
              .where(SET_GROUP.NAME.eq("Test Group"))
              .fetchOne(SET_GROUP.ID);
      if (defaultSetGroupId == null) {
        defaultSetGroupId =
            dsl.insertInto(SET_GROUP)
                .set(SET_GROUP.NAME, "Test Group")
                .set(SET_GROUP.SORT_ORDER, 1)
                .returningResult(SET_GROUP.ID)
                .fetchOne(SET_GROUP.ID);
      }
    }
  }

  private int ensureCardSet(int setId) {
    return testSets.computeIfAbsent(
        setId,
        id -> {
          String code = "DM-" + id;
          // Check if already exists
          Integer existing =
              dsl.select(CARD_SET.ID)
                  .from(CARD_SET)
                  .where(CARD_SET.CODE.eq(code))
                  .fetchOne(CARD_SET.ID);
          if (existing != null) return existing;

          return dsl.insertInto(CARD_SET)
              .set(CARD_SET.NAME, "Set " + id)
              .set(CARD_SET.CODE, code)
              .set(CARD_SET.RELEASE_DATE, LocalDate.now())
              .set(CARD_SET.PRODUCT_TYPE_ID, defaultProductTypeId)
              .set(CARD_SET.SET_GROUP_ID, defaultSetGroupId)
              .returningResult(CARD_SET.ID)
              .fetchOne(CARD_SET.ID);
        });
  }

  private Short ensureRarity(RarityCode rarity) {
    if (rarity == RarityCode.NONE) return null;
    return rarities.computeIfAbsent(
        rarity,
        r -> {
          String name = r.toString();
          Short existing =
              dsl.select(RARITY.ID).from(RARITY).where(RARITY.NAME.eq(name)).fetchOne(RARITY.ID);
          if (existing != null) return existing;

          int order = rarityOrder.getOrDefault(r, 0);
          return dsl.insertInto(RARITY)
              .set(RARITY.NAME, name)
              .set(RARITY.SORT_ORDER, (short) order)
              .returningResult(RARITY.ID)
              .fetchOne(RARITY.ID);
        });
  }

  private short ensureCardType(String typeName) {
    return cardTypes.computeIfAbsent(
        typeName,
        name -> {
          Short existing =
              dsl.select(CARD_TYPE.ID)
                  .from(CARD_TYPE)
                  .where(CARD_TYPE.NAME.eq(name))
                  .fetchOne(CARD_TYPE.ID);
          if (existing != null) return existing;

          var id =
              dsl.insertInto(CARD_TYPE)
                  .set(CARD_TYPE.NAME, name)
                  .returningResult(CARD_TYPE.ID)
                  .fetchOne(CARD_TYPE.ID);
          cardTypeResolver.loadNameToId();
          return id;
        });
  }

  private short ensureRace(String raceName) {
    return races.computeIfAbsent(
        raceName,
        name -> {
          Short existing =
              dsl.select(RACE.ID).from(RACE).where(RACE.NAME.eq(name)).fetchOne(RACE.ID);
          if (existing != null) return existing;

          return dsl.insertInto(RACE)
              .set(RACE.NAME, name)
              .returningResult(RACE.ID)
              .fetchOne(RACE.ID);
        });
  }

  private int ensureAbility(String text) {
    return abilities.computeIfAbsent(
        text,
        t -> {
          Integer existing =
              dsl.select(ABILITY.ID).from(ABILITY).where(ABILITY.TEXT.eq(t)).fetchOne(ABILITY.ID);
          if (existing != null) return existing;

          return dsl.insertInto(ABILITY)
              .set(ABILITY.TEXT, t)
              .set(ABILITY.SEARCH_TEXT, t) // for test fixtures, search_text = text
              .returningResult(ABILITY.ID)
              .fetchOne(ABILITY.ID);
        });
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
