package net.dmcollection.server;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  public CardStub multiCard(String officialId, Civilization... civs) {
    return card(officialId, List.of(officialId + ".jpg"), List.of(Set.of(civs)));
  }

  public CardStub monoCard(String officialId, Civilization civ) {
    return card(officialId, List.of(officialId + ".jpg"), List.of(Set.of(civ)));
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
                CardEntity.ID,
                CardEntity.ID_TEXT,
                CardEntity.OFFICIAL_ID,
                CardEntity.SET,
                CardEntity.TWINPACT,
                CardEntity.RARITY);
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
      insert =
          "INSERT INTO CARD_FACETS (ID, \"%s\", CARDS, POSITION, IMAGE_FILENAME, COST, CIVS, POWER, TYPE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
              .formatted(CardFacet.NAME);
      args.add(facetId);
      args.add(officialId);
      args.add(cardId);
      args.add(i);
      args.add(imageFile);
      args.add(cost);
      args.add(Civilization.toInts(facetCivs.get(i)).toArray());
      args.add(powers != null && i < powers.size() ? powers.get(i) : null);
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
}
