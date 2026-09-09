package net.dmcollection.server;

import static net.dmcollection.server.card.Civilization.FIRE;
import static net.dmcollection.server.card.Civilization.NATURE;
import static net.dmcollection.server.card.Civilization.WATER;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_RACE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.Civilization;
import net.dmcollection.server.card.RarityCode;
import net.dmcollection.server.card.internal.query.CardTypeResolver;
import org.jooq.DSLContext;

public class TestFixtureBuilder {

  public static final String CREATURE = "クリーチャー";
  public static final String SPELL = "呪文";
  public static final String D2_FIELD = "D2フィールド";
  public static final String PSYCHIC_CREATURE = "サイキック・クリーチャー";

  private final DSLContext dsl;
  private final Map<Long, CardStub> testCards = new HashMap<>();
  private final CardTypeResolver cardTypeResolver;
  private final DbWriter dbWriter;

  private Short defaultProductTypeId;
  private Integer defaultSetGroupId;

  public TestFixtureBuilder(DSLContext dsl, CardTypeResolver cardTypeResolver) {
    this.dsl = dsl;
    this.dbWriter = new DbWriter(dsl);
    this.cardTypeResolver = cardTypeResolver;
  }

  public Map<Long, CardStub> getTestCards() {
    return this.testCards;
  }

  /** Returns the actual DB card_set.id for a given set parameter used in card creation. */
  public int getCardSetId(int setParam) {
    return ensureCardSet(setParam);
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

    short raceId = dbWriter.upsertRace(speciesName);
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

    String cardZone = "main";
    if (facetTypes != null) {
      if (facetTypes.contains(PSYCHIC_CREATURE)) {
        cardZone = "hyperspatial";
      }
    }

    // Insert card
    int cardId =
        dbWriter.upsertCard(officialId, twinpact, sortCost, sortPower, facetCivs, cardZone);

    // Insert card sides and collect their IDs
    List<Integer> cardSideIds = new ArrayList<>();
    for (int i = 0; i < facetCivs.size(); i++) {
      Integer costValue = costs != null && i < costs.size() ? costs.get(i) : null;
      Integer powerValue = powers != null && i < powers.size() ? powers.get(i) : null;
      DbWriter.IntOrInfinity cost =
          costValue == null ? null : new DbWriter.IntOrInfinity(costValue);
      DbWriter.IntOrInfinity power =
          powerValue == null ? null : new DbWriter.IntOrInfinity(powerValue);
      String sideType = facetTypes != null && i < facetTypes.size() ? facetTypes.get(i) : null;
      String sideRace =
          facetSpecies != null && i < facetSpecies.size() ? facetSpecies.get(i) : null;

      int cardSideId =
          dbWriter.upsertCardSide(
              cardId,
              i,
              officialId + (facetCivs.size() > 1 ? "-side" + i : ""),
              cost,
              power,
              facetCivs.get(i),
              sideType != null ? List.of(sideType) : null,
              cardTypeResolver,
              sideRace != null ? List.of(sideRace) : null);
      cardSideIds.add(cardSideId);
    }

    // Insert printing
    int printingId = dbWriter.upsertPrinting(cardId, cardSetId, officialId, idText, rarity);

    // Insert printing sides
    List<Integer> printingSideIds = new ArrayList<>();
    for (int i = 0; i < cardSideIds.size(); i++) {
      String imageFile = imageFiles != null && imageFiles.size() > i ? imageFiles.get(i) : null;
      printingSideIds.add(
          dbWriter.upsertPrintingSide(printingId, cardSideIds.get(i), "", imageFile));
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
              dbWriter.addAbility(printingSideId, effectGroup.getFirst(), position, 0);
              position++;
              for (int childIndex = 1; childIndex < effectGroup.size(); childIndex++) {
                dbWriter.addAbility(printingSideId, effectGroup.get(childIndex), position, 1);
                position++;
              }
            }
          }
        }
      }
    }

    Set<Civilization> allCivs = facetCivs.stream().flatMap(Set::stream).collect(Collectors.toSet());

    CardStub stub =
        new CardStub(
            (long) printingId,
            officialId,
            idText,
            allCivs,
            imageFiles != null
                ? imageFiles.stream().filter(Objects::nonNull).toList()
                : Collections.emptyList(),
            0,
            0);
    testCards.put((long) printingId, stub);
    return stub;
  }

  private void ensureDefaultLookups() {
    if (defaultProductTypeId == null) {
      defaultProductTypeId = dbWriter.upsertProductType("ブースターパック");
    }
    if (defaultSetGroupId == null) {
      defaultSetGroupId = dbWriter.upsertSetGroup("Test Group", 1);
    }
  }

  private int ensureCardSet(int setId) {
    return dbWriter.upsertSet(
        "DM-" + setId, "Set " + setId, LocalDate.now(), "ブースターパック", defaultSetGroupId);
  }
}
