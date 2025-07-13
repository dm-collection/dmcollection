package net.dmcollection.server.card;

import static net.dmcollection.model.card.Civilization.DARK;
import static net.dmcollection.model.card.Civilization.FIRE;
import static net.dmcollection.model.card.Civilization.LIGHT;
import static net.dmcollection.model.card.Civilization.NATURE;
import static net.dmcollection.model.card.Civilization.WATER;
import static net.dmcollection.model.card.Civilization.ZERO;
import static net.dmcollection.server.TestUtils.CREATURE;
import static net.dmcollection.server.TestUtils.PSYCHIC_CREATURE;
import static net.dmcollection.server.TestUtils.SPELL;
import static net.dmcollection.server.TestUtils.search;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.dmcollection.model.card.CardEntity;
import net.dmcollection.model.card.CardFacet;
import net.dmcollection.model.card.OfficialSet;
import net.dmcollection.model.card.OfficialSet.Columns;
import net.dmcollection.model.card.RarityCode;
import net.dmcollection.server.TestUtils;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.internal.CardQueryService;
import net.dmcollection.server.card.internal.RarityService;
import net.dmcollection.server.card.internal.SearchFilter;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import net.dmcollection.server.card.internal.SearchFilter.FilterState;
import net.dmcollection.server.card.internal.SearchFilter.Range;
import net.dmcollection.server.card.internal.SpeciesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CardQueryServiceIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(CardQueryServiceIntegrationTest.class);

  CardQueryService cardQueryService;

  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired SpeciesService speciesService;

  TestUtils utils;

  @BeforeEach
  void setup() {
    utils = new TestUtils(jdbcTemplate);
    jdbcTemplate.update("DELETE FROM CARD_FACETS");
    jdbcTemplate.update("DELETE FROM CARDS");
    jdbcTemplate.update("DELETE FROM CARD_SET");
    jdbcTemplate.update("DELETE FROM SPECIES");
    jdbcTemplate.update("DELETE FROM FACET_SPECIES");
    jdbcTemplate.update("DELETE FROM RARITY");
    ImageService imageService = mock(ImageService.class);
    when(imageService.makeImageUrl(anyString()))
        .thenAnswer(i -> i.getArgument(0) != null ? "/image/" + i.getArgument(0) : null);
    RarityService rarityService = mock(RarityService.class);
    when(rarityService.getOrder(any(RarityCode.class)))
        .thenAnswer(invocation -> TestUtils.rarityOrder.get(invocation.getArgument(0)));
    cardQueryService =
        new CardQueryService(jdbcTemplate, imageService, speciesService, rarityService);
  }

  @AfterEach
  void printLog() {
    utils.printLog();
  }

  @Test
  void excludesMultiCivCards() {
    CardStub mono = utils.monoCard("MONO-1", LIGHT);

    utils.multiCard("MULTI-1", LIGHT, DARK);

    SearchFilter filter =
        search().addIncludedCivs(LIGHT).setIncludeMono(true).setIncludeRainbow(false).build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, mono);
  }

  @Test
  void excludesMonoCivCards() {
    utils.monoCard("MONO-1", LIGHT);
    utils.monoCard("MONO-2", DARK);
    utils.monoCard("MONO-3", WATER);

    var multi = utils.multiCard("MULTI-1", LIGHT, DARK);
    var multi2 = utils.multiCard("MULTI-2", LIGHT, DARK, WATER);
    var multi3 = utils.multiCard("MULTI-3", WATER, FIRE);
    utils.multiCard("MULTI-4", FIRE, NATURE);

    SearchFilter filter =
        search()
            .addIncludedCivs(LIGHT, WATER)
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .build();
    assertQueryFinds(filter, multi, multi2, multi3);
  }

  @Test
  void findsOnlyTwinpacts() {
    utils.monoCard("NORMAL-1", LIGHT);

    CardStub twinpact = utils.twinpact("TWIN-1", Set.of(LIGHT), Set.of(DARK));

    SearchFilter filter = search().setTwinpact(FilterState.ONLY).build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, twinpact);
  }

  @Test
  void findsOnlyTwinpactsWithoutZero() {
    utils.monoCard("mono", LIGHT);
    utils.monoCard("zero", ZERO);
    var twinpact1 = utils.twinpact("TWIN-1", Set.of(LIGHT), Set.of(LIGHT));
    var twinpact2 = utils.twinpact("TWIN-2", Set.of(WATER, DARK), Set.of(NATURE));
    utils.multiCard("multi", NATURE, FIRE);
    var all = utils.twinpact("all", Set.of(ZERO), Set.of(LIGHT, WATER, DARK, FIRE, NATURE));

    SearchFilter filter =
        search()
            .setTwinpact(FilterState.ONLY)
            .addIncludedCivs(LIGHT, WATER, DARK, FIRE, NATURE)
            .build();

    assertQueryFinds(filter, twinpact1, twinpact2, all);
  }

  @Test
  void findsNonTwinpacts() {
    CardStub mono = utils.monoCard("NORMAL-1", LIGHT);

    utils.twinpact("TWIN-1", Set.of(LIGHT), Set.of(DARK));

    SearchFilter filter = search().setTwinpact(FilterState.EX).build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, mono);
  }

  @Test
  void findsMultiCivCardsWithTwoExactCivs() {
    CardStub lightAndDark = utils.multiCard("EXACT-1", LIGHT, DARK);

    utils.multiCard("EXTRA-1", LIGHT, DARK, NATURE);
    utils.multiCard("EXTRA-2", LIGHT, NATURE);

    SearchFilter filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK)
            .setMatchExactRainbowCivs(true)
            .build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, lightAndDark);
  }

  @Test
  void findsMultiCivCardsWithThreeExactCivs() {
    CardStub lightAndDark = utils.multiCard("EXACT-1", LIGHT, DARK, FIRE);

    utils.multiCard("EXTRA-1", LIGHT, DARK, NATURE);
    utils.multiCard("EXTRA-2", LIGHT, DARK);
    utils.multiCard("EXTRA-3", LIGHT, DARK, FIRE, WATER);
    utils.multiCard("EXTRA-4", LIGHT, FIRE);

    SearchFilter filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK, FIRE)
            .setMatchExactRainbowCivs(true)
            .build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, lightAndDark);
  }

  @Test
  void findsTwinpactsWithExactCivs() {
    CardStub rainbow = utils.multiCard("RAINBOW-1", LIGHT, DARK);
    CardStub twinpact = utils.twinpact("TWIN-1", Set.of(LIGHT), Set.of(DARK));
    CardStub twinpact2 = utils.twinpact("TWIN-2", Set.of(LIGHT, DARK), Set.of(DARK));
    utils.twinpact("Exclude", Set.of(LIGHT), Set.of(DARK, WATER));

    SearchFilter filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK)
            .setMatchExactRainbowCivs(true)
            .build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, rainbow, twinpact, twinpact2);
  }

  @Test
  void findsTwinpactsWithThreeExactCivs() {
    CardStub rainbow = utils.multiCard("RAINBOW-1", LIGHT, DARK, FIRE);
    utils.multiCard("RAINBOW-2", LIGHT, DARK, FIRE, NATURE);
    utils.twinpact("TWIN-1", Set.of(LIGHT), Set.of(DARK));
    CardStub twinpact2 = utils.twinpact("TWIN-2", Set.of(LIGHT, DARK), Set.of(FIRE));
    CardStub twinpact3 = utils.twinpact("TWIN-3", Set.of(LIGHT, FIRE), Set.of(DARK));
    utils.twinpact("Exclude", Set.of(LIGHT), Set.of(DARK, WATER));

    SearchFilter filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK, FIRE)
            .setMatchExactRainbowCivs(true)
            .build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, rainbow, twinpact2, twinpact3);
  }

  @Test
  void findsTwinpactCardsWithoutExactMatch() {
    CardStub fire = utils.monoCard("FIRE", FIRE);
    CardStub dark = utils.monoCard("DARK", DARK);
    CardStub rainbow = utils.multiCard("RAINBOW-1", LIGHT, DARK, FIRE);
    CardStub rainbow2 = utils.multiCard("RAINBOW-2", LIGHT, DARK, FIRE, NATURE);
    CardStub twinpact = utils.twinpact("TWIN-1", Set.of(LIGHT), Set.of(DARK));
    CardStub twinpact2 = utils.twinpact("TWIN-2", Set.of(LIGHT, DARK), Set.of(FIRE));
    CardStub twinpact3 = utils.twinpact("TWIN-3", Set.of(LIGHT, FIRE), Set.of(DARK));
    CardStub twinpact4 = utils.twinpact("TWIN-4", Set.of(DARK), Set.of(FIRE));
    CardStub monoTwinpact = utils.twinpact("MONO-TWIN", Set.of(FIRE), Set.of(FIRE));
    utils.twinpact("Exclude", Set.of(WATER), Set.of(NATURE, WATER));

    SearchFilter filter =
        search()
            .setIncludeMono(true)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK, FIRE)
            .setMatchExactRainbowCivs(false)
            .build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(
        result,
        fire,
        dark,
        rainbow,
        rainbow2,
        twinpact,
        twinpact2,
        twinpact3,
        twinpact4,
        monoTwinpact);
  }

  @Test
  void findsTwinpactCardsWithoutExactMatchNoMono() {
    utils.monoCard("FIRE", FIRE);
    utils.monoCard("DARK", DARK);
    CardStub rainbow = utils.multiCard("RAINBOW-1", LIGHT, DARK, FIRE);
    CardStub rainbow2 = utils.multiCard("RAINBOW-2", LIGHT, DARK, FIRE, NATURE);
    CardStub twinpact = utils.twinpact("TWIN-1", Set.of(LIGHT), Set.of(DARK));
    CardStub twinpact2 = utils.twinpact("TWIN-2", Set.of(LIGHT, DARK), Set.of(FIRE));
    CardStub twinpact3 = utils.twinpact("TWIN-3", Set.of(LIGHT, FIRE), Set.of(DARK));
    CardStub twinpact4 = utils.twinpact("TWIN-4", Set.of(DARK), Set.of(FIRE));
    utils.twinpact("MONO-TWIN", Set.of(FIRE), Set.of(FIRE));
    utils.twinpact("Exclude", Set.of(WATER), Set.of(NATURE, WATER));

    SearchFilter filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK, FIRE)
            .setMatchExactRainbowCivs(false)
            .build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, rainbow, rainbow2, twinpact, twinpact2, twinpact3, twinpact4);
  }

  @Test
  void defaultFilterFindsSingle() {
    CardStub dm01 =
        utils.card(
            "DM01-001",
            "DM1 1/110",
            false,
            RarityCode.VR,
            1L,
            List.of("dm01-001.jpg"),
            List.of(Set.of(LIGHT)),
            List.of(6),
            List.of(9000),
            List.of(CREATURE));
    SearchFilter filter = search().build();
    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, dm01);
  }

  @Test
  void defaultFilterFindsAll() {
    CardStub card1 = utils.multiCard("CARD-1", LIGHT, DARK);

    CardStub card2 = utils.twinpact("CARD-2", Set.of(LIGHT, DARK), Set.of(WATER));

    CardStub card3 =
        utils.card(
            "CARD-3",
            false,
            RarityCode.R,
            2L,
            List.of("card3.jpg", "card3b.jpg"),
            List.of(Set.of(FIRE), Set.of(WATER)),
            List.of(1, 2),
            List.of(2000, 1000),
            List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE));

    CardStub card4 =
        utils.card(
            "CARD-4",
            false,
            RarityCode.VIC,
            2L,
            List.of("card4.jpg"),
            List.of(Set.of(ZERO)),
            List.of(4),
            List.of(4500),
            List.of(CREATURE));

    SearchFilter filter = search().build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, card1, card2, card3, card4);
  }

  @Test
  void defaultFilterFindsAllPaged() {
    CardStub card1 = utils.card("CARD-1", List.of("card1.jpg"), List.of(Set.of(LIGHT, DARK)));

    CardStub card2 =
        utils.card("CARD-2", List.of("card2.jpg"), List.of(Set.of(LIGHT, DARK), Set.of(WATER)));

    CardStub card3 =
        utils.card(
            "CARD-3",
            false,
            RarityCode.R,
            2L,
            List.of("card3.jpg", "card3b.jpg"),
            List.of(Set.of(FIRE), Set.of(WATER)),
            List.of(1, 2),
            List.of(1500, 3000),
            List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE));

    CardStub card4 =
        utils.card(
            "CARD-4",
            false,
            RarityCode.VIC,
            2L,
            List.of("card4.jpg"),
            List.of(Set.of(ZERO)),
            List.of(4),
            List.of(5000),
            List.of(CREATURE));

    SearchFilter filter =
        search()
            .setPageable(
                PageRequest.of(
                    0,
                    2,
                    Sort.by(OfficialSet.Columns.RELEASE)
                        .descending()
                        .and(Sort.by(CardEntity.Columns.OFFICIAL_ID).ascending())))
            .build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, card1, card2);
    filter =
        search()
            .setPageable(
                PageRequest.of(
                    1,
                    2,
                    Sort.by(Columns.RELEASE)
                        .descending()
                        .and(Sort.by(CardEntity.Columns.OFFICIAL_ID).ascending())))
            .build();
    result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, card3, card4);
  }

  @Test
  void findsCardsInSet() {
    utils.card("CARD-1", List.of("card1.jpg"), List.of(Set.of(LIGHT, DARK)));

    CardStub expected =
        utils.card(
            "CARD-2",
            false,
            RarityCode.R,
            2L,
            List.of("card2.jpg", "card2b.jpg"),
            List.of(Set.of(WATER), Set.of(WATER)),
            List.of(4, 13),
            List.of(4000, 13000),
            List.of(PSYCHIC_CREATURE, PSYCHIC_CREATURE));

    SearchFilter filter = search().setSetId(2L).build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, expected);
  }

  @Test
  void mismatchedMonoFilterFindsNothing() {
    utils.monoCard("CARD-1", LIGHT);
    utils.monoCard("CARD-2", FIRE);
    utils.multiCard("CARD-3", NATURE, DARK);

    SearchFilter filter = search().addIncludedCivs(WATER).setIncludeMono(true).build();

    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertThat(result.getTotalElements()).isEqualTo(0);
  }

  @Test
  void findsMultipleCivilizations() {
    var mono = utils.monoCard("MONO-1", FIRE);
    var mono2 = utils.monoCard("MONO-2", LIGHT);
    var card1 = utils.multiCard("CARD-1", LIGHT, DARK);
    var card2 = utils.multiCard("CARD-2", LIGHT, DARK, WATER);
    var card3 = utils.multiCard("CARD-3", FIRE, WATER);

    utils.monoCard("MONO-2", WATER);
    utils.monoCard("MONO-3", NATURE);
    utils.monoCard("CARD-4", ZERO);
    utils.multiCard("foo", DARK, NATURE);

    SearchFilter filter = search().addIncludedCivs(FIRE, LIGHT).build();
    assertQueryFinds(filter, mono, mono2, card1, card2, card3);
  }

  @Test
  void findsMultipleCivilizationsExcludingMono() {
    utils.monoCard("MONO-1", FIRE);
    utils.monoCard("MONO-2", LIGHT);
    var card1 = utils.multiCard("CARD-1", LIGHT, DARK);
    var card2 = utils.multiCard("CARD-2", LIGHT, DARK, WATER);
    var card3 = utils.multiCard("CARD-3", FIRE, WATER);

    utils.monoCard("MONO-2", WATER);
    utils.monoCard("MONO-3", NATURE);
    utils.monoCard("CARD-4", ZERO);
    utils.multiCard("foo", DARK, NATURE);

    SearchFilter filter = search().addIncludedCivs(FIRE, LIGHT).setIncludeMono(false).build();
    assertQueryFinds(filter, card1, card2, card3);
  }

  @Test
  void excludesCivilization() {
    utils.monoCard("light", LIGHT);
    var fire = utils.monoCard("fire", FIRE);
    utils.monoCard("water", WATER);
    utils.monoCard("zero", ZERO);
    var dark = utils.monoCard("dark", DARK);

    var darkFire = utils.multiCard("darkFire", DARK, FIRE);
    utils.multiCard("darkFireLight", DARK, FIRE, LIGHT);
    utils.multiCard("waterLight", LIGHT, WATER);
    var darkNature = utils.multiCard("darkNature", DARK, NATURE);

    SearchFilter filter =
        search().addIncludedCivs(DARK, FIRE).addExcludedCivs(LIGHT, WATER).build();
    assertQueryFinds(filter, fire, dark, darkFire, darkNature);
  }

  @Test
  void excludesCivilizationsAndMono() {
    utils.monoCard("light", LIGHT);
    utils.monoCard("fire", FIRE);
    utils.monoCard("water", WATER);
    utils.monoCard("zero", ZERO);
    utils.monoCard("dark", DARK);

    var darkFire = utils.multiCard("darkFire", DARK, FIRE);
    utils.multiCard("darkFireLight", DARK, FIRE, LIGHT);
    utils.multiCard("waterLight", LIGHT, WATER);
    var darkNature = utils.multiCard("darkNature", DARK, NATURE);

    SearchFilter filter =
        search()
            .addIncludedCivs(DARK, FIRE)
            .addExcludedCivs(LIGHT, WATER)
            .setIncludeMono(false)
            .build();
    assertQueryFinds(filter, darkFire, darkNature);
  }

  @Test
  void findsZeroOnly() {
    var zero = utils.monoCard("zero", ZERO);
    utils.monoCard("light", LIGHT);
    utils.monoCard("fire", FIRE);
    utils.monoCard("water", WATER);
    utils.monoCard("nature", NATURE);
    var zero2 = utils.monoCard("02", ZERO);
    utils.multiCard("multi1", LIGHT, WATER);
    utils.multiCard("multi2", DARK, FIRE, LIGHT, WATER, NATURE);

    SearchFilter filter =
        search().addIncludedCivs(ZERO).addExcludedCivs(LIGHT, WATER, FIRE, DARK, NATURE).build();
    assertQueryFinds(filter, zero, zero2);
  }

  @Test
  void excludesRainbow() {
    // excluded rainbow cards
    utils.twinpact("TWIN-1", Set.of(LIGHT), Set.of(DARK));
    utils.twinpact("TWIN-2", Set.of(LIGHT, DARK), Set.of(FIRE));
    utils.twinpact("TWIN-3", Set.of(FIRE), Set.of(DARK));
    utils.twinpact("TWIN-4", Set.of(DARK), Set.of(LIGHT));
    utils.multiCard("multi1", LIGHT, DARK);

    var mono1 = utils.monoCard("mono1", LIGHT);
    var mono2 = utils.monoCard("mono2", DARK);
    var monoTwinpact = utils.twinpact("monoTwin", Set.of(LIGHT), Set.of(LIGHT));
    utils.twinpact("unrelatedMonoTwinpact", Set.of(WATER), Set.of(WATER));

    // included via both sides
    var twoSides = utils.twoSided("twoSided", Set.of(LIGHT), Set.of(DARK));
    // included because of monochrome light side
    var twoSides2 = utils.twoSided("twoSided", Set.of(LIGHT), Set.of(FIRE));
    // included because of monochrome dark side
    var twoSides3 = utils.twoSided("twoSided2", Set.of(LIGHT, FIRE), Set.of(DARK));
    // excluded because no monochrome light or dark side
    utils.twoSided("exclude", Set.of(WATER), Set.of(NATURE, LIGHT));

    SearchFilter filter = search().setIncludeRainbow(false).addIncludedCivs(LIGHT, DARK).build();
    assertQueryFinds(filter, mono1, mono2, monoTwinpact, twoSides, twoSides2, twoSides3);
  }

  @Test
  void findsCardsWithMinimumCost() {
    utils.monoCard("CHEAP-1", 2, LIGHT);
    utils.monoCard("CHEAP-2", 3, FIRE);
    var expensive1 = utils.monoCard("EXPENSIVE-1", 5, WATER);
    var expensive2 = utils.multiCard("EXPENSIVE-2", 7, DARK, FIRE);
    var expensive3 = utils.monoCard("EXPENSIVE-3", 6, NATURE);

    var twinpact = utils.twinpact("TWINPACT", Set.of(LIGHT), Set.of(DARK), 5, 3);

    SearchFilter filter = search().setMinCost(5).build();
    assertQueryFinds(filter, expensive1, expensive2, expensive3, twinpact);
  }

  @Test
  void findsCardsWithMaximumCost() {
    var cheap1 = utils.monoCard("CHEAP-1", 2, LIGHT);
    var cheap2 = utils.monoCard("CHEAP-2", 3, FIRE);
    var cheap3 = utils.multiCard("CHEAP-3", 4, WATER, DARK);
    utils.monoCard("EXPENSIVE-1", 5, WATER);
    utils.multiCard("EXPENSIVE-2", 7, DARK, FIRE);
    utils.monoCard("EXPENSIVE-3", 6, NATURE);

    var twinpact = utils.twinpact("TWINPACT", Set.of(LIGHT), Set.of(DARK), 5, 3);

    SearchFilter filter = search().setMaxCost(4).build();
    assertQueryFinds(filter, cheap1, cheap2, cheap3, twinpact);
  }

  @Test
  void findsCardsWithinCostRange() {
    utils.monoCard("CHEAP-1", 2, LIGHT);
    utils.monoCard("CHEAP-2", 3, FIRE);
    var medium1 = utils.multiCard("MEDIUM-1", 4, WATER, DARK);
    var medium2 = utils.monoCard("MEDIUM-2", 5, WATER);
    var medium3 = utils.monoCard("MEDIUM-3", 6, NATURE);
    utils.multiCard("EXPENSIVE-1", 7, DARK, FIRE);
    utils.monoCard("EXPENSIVE-2", 8, NATURE);

    var twinpact = utils.twinpact("TWINPACT", Set.of(LIGHT), Set.of(DARK), 5, 3);
    utils.twinpact("outside", Set.of(WATER), Set.of(NATURE, FIRE), 3, 8);

    SearchFilter filter = search().setMinCost(4).setMaxCost(6).build();
    assertQueryFinds(filter, medium1, medium2, medium3, twinpact);
  }

  @Test
  void findsCardsWithMinimumPower() {
    utils.monoCard("WEAK-1", 2, 2000, LIGHT);
    utils.monoCard("WEAK-2", 3, 3000, FIRE);
    var strong1 = utils.monoCard("STRONG-1", 5, 5000, WATER);
    var strong2 = utils.multiCard("STRONG-2", 7, 7000, DARK, FIRE);
    var strong3 = utils.monoCard("STRONG-3", 6, 6000, NATURE);

    var twinpact = utils.twinpact("TWINPACT", Set.of(LIGHT), Set.of(DARK), 5, 3, 5000);
    var twoSided = utils.twoSided("two-sides", Set.of(WATER), Set.of(FIRE), 4, 7, 4000, 7000);

    SearchFilter filter = search().setMinPower(5000).build();
    assertQueryFinds(filter, strong1, strong2, strong3, twinpact, twoSided);
  }

  @Test
  void findsCardsWithMaximumPower() {
    var weak1 = utils.monoCard("WEAK-1", 2, 2000, LIGHT);
    var weak2 = utils.monoCard("WEAK-2", 3, 3000, FIRE);
    var weak3 = utils.multiCard("WEAK-3", 4, 4000, WATER, DARK);
    utils.monoCard("STRONG-1", 5, 5000, WATER);
    utils.multiCard("STRONG-2", 7, 7000, DARK, FIRE);
    utils.monoCard("STRONG-3", 6, 6000, NATURE);

    utils.twinpact("TWINPACT", Set.of(LIGHT), Set.of(DARK), 5, 3, 5000);
    var twoSided = utils.twoSided("two-sides", Set.of(WATER), Set.of(FIRE), 4, 7, 4000, 7000);

    SearchFilter filter = search().setMaxPower(4000).build();
    assertQueryFinds(filter, weak1, weak2, weak3, twoSided);
  }

  @Test
  void findsCardsWithinPowerRange() {
    utils.monoCard("WEAK-1", 2, 2000, LIGHT);
    utils.monoCard("WEAK-2", 3, 3000, FIRE);
    var medium1 = utils.multiCard("MEDIUM-1", 4, 4000, WATER, DARK);
    var medium2 = utils.monoCard("MEDIUM-2", 5, 5000, WATER);
    var medium3 = utils.monoCard("MEDIUM-3", 6, 6000, NATURE);
    utils.multiCard("STRONG-1", 7, 7000, DARK, FIRE);
    utils.monoCard("STRONG-2", 8, 8000, NATURE);

    var twinpact = utils.twinpact("TWINPACT", Set.of(LIGHT), Set.of(DARK), 5, 3, 5000);
    utils.twinpact("outside", Set.of(WATER), Set.of(NATURE, FIRE), 3, 8, 3000);
    utils.twoSided("two-sides", Set.of(WATER), Set.of(FIRE), 4, 7, 2000, 7000);

    SearchFilter filter = search().setMinPower(4000).setMaxPower(6000).build();
    assertQueryFinds(filter, medium1, medium2, medium3, twinpact);
  }

  @Test
  void onlyMinPowerOrCostFindsInfinitePowerCard() {
    var thatInfiniteCard =
        utils.multiCard("dm24ex1-SP2", Integer.MAX_VALUE, Integer.MAX_VALUE, WATER, DARK);
    SearchFilter filter = search().setMinCost(1000000).build();
    assertQueryFinds(filter, thatInfiniteCard);
    filter = search().setMinPower(1000000).build();
    assertQueryFinds(filter, thatInfiniteCard);
    filter = search().setMinCost(1000000).setMaxCost(Integer.MAX_VALUE).build();
    assertQueryFindsNothing(filter);
    filter = search().setMinPower(1000000).setMaxPower(Integer.MAX_VALUE).build();
    assertQueryFindsNothing(filter);
    filter = search().setMinPower(Integer.MAX_VALUE).setMaxPower(Integer.MAX_VALUE).build();
    assertQueryFindsNothing(filter);
    filter = search().setMinPower(Integer.MAX_VALUE).build();
    assertQueryFinds(filter, thatInfiniteCard);
  }

  @Test
  void handlesCostRangeWithCivilizationFilter() {
    utils.monoCard("CHEAP-1", 2, LIGHT);
    var medium1 = utils.monoCard("MEDIUM-1", 4, LIGHT);
    var medium2 = utils.multiCard("MEDIUM-2", 5, LIGHT, DARK);
    utils.monoCard("EXPENSIVE-1", 7, LIGHT);
    utils.monoCard("OTHER-1", 4, FIRE);
    utils.multiCard("OTHER-2", 5, WATER, DARK);

    SearchFilter filter = search().setMinCost(4).setMaxCost(6).addIncludedCivs(LIGHT).build();
    assertQueryFinds(filter, medium1, medium2);
  }

  @Test
  void includesNullCostWhenNoCostFilter() {
    utils.monoCard("COST-1", 2, LIGHT);
    utils.monoCard("COST-2", null, LIGHT);
    utils.monoCard("COST-3", 4, LIGHT);
    utils.monoCard("COST-4", null, LIGHT);
    utils.monoCard("COST-5", 6, LIGHT);

    SearchFilter filter = search().build();
    assertQueryFindsAllCards(filter);
  }

  @Test
  void excludesNullCostWhenCostFilter() {
    var card1 = utils.monoCard("COST-1", 2, LIGHT);
    utils.monoCard("COST-null", null, LIGHT);
    var card2 = utils.monoCard("COST-3", 4, LIGHT);
    utils.monoCard("COST-null2", null, LIGHT);
    var card3 = utils.monoCard("COST-5", 6, LIGHT);

    SearchFilter filter = search().setMinCost(3).build();
    assertQueryFinds(filter, card2, card3);
    filter = search().setMaxCost(4).build();
    assertQueryFinds(filter, card1, card2);
  }

  @Test
  void findsThatOneFourSidedCard() {
    CardStub thatOneCard = utils.createFoursides();
    SearchFilter filter =
        search()
            .setIncludeMono(false)
            .addIncludedCivs(WATER, FIRE, NATURE)
            .setMatchExactRainbowCivs(true)
            .build();
    assertQueryFinds(filter, thatOneCard);
    filter = search().addIncludedCivs(FIRE).build();
    assertQueryFinds(filter, thatOneCard);
  }

  @Test
  void twinpactExcludesFoursides() {
    utils.createFoursides();
    SearchFilter filter = search().setTwinpact(FilterState.ONLY).setIncludeRainbow(false).build();
    assertQueryFindsNothing(filter);
  }

  @Test
  void findsAllMultiColoredCards() {
    utils.monoCard("uncolored", 1, ZERO);
    utils.monoCard("light", 2, LIGHT);
    utils.monoCard("water", 3, WATER);
    utils.monoCard("darkness", 4, DARK);
    utils.monoCard("fire", 5, FIRE);
    utils.monoCard("nature", 6, NATURE);
    utils.twinpact("monoTwin", Set.of(WATER), Set.of(WATER));

    var multi1 = utils.multiCard("multi1", LIGHT, WATER);
    var multi2 = utils.multiCard("multi2", FIRE, NATURE);
    var multi3 = utils.multiCard("multi3", WATER, DARK, NATURE);
    var multiTwinpact = utils.twinpact("twinpact", Set.of(LIGHT), Set.of(FIRE));

    SearchFilter filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .setMatchExactRainbowCivs(false)
            .build();
    assertQueryFinds(filter, multi1, multi2, multi3, multiTwinpact);
  }

  @Test
  void findsSpellsOrCreatures() {
    var spell1 = utils.monoCard("spell1", 6, null, LIGHT);
    var spell2 = utils.monoCard("spell2", 2, null, NATURE);

    var creature1 = utils.monoCard("creature1", 1, 4000, LIGHT);
    var creature2 = utils.monoCard("creature2", 1, 1000, FIRE);

    SearchFilter filter = search().setCardType(CardType.CREATURE).build();
    assertQueryFinds(filter, creature1, creature2);
    filter = search().setCardType(CardType.SPELL).build();
    assertQueryFinds(filter, spell1, spell2);
  }

  @Test
  void findsExactSpecies() {
    var card1 = utils.monoCard("test", 6, 6000, LIGHT);
    var card2 = utils.monoCard("test-2", 6, 6000, LIGHT);
    var card3 = utils.monoCard("test-3", 6, 6000, LIGHT);

    utils.addSpeciesToFacet(card1.id() + 1, 0, "アーマード・ドラゴン");
    utils.addSpeciesToFacet(card2.id() + 1, 0, "アーマード・ドラゴン");
    utils.addSpeciesToFacet(card3.id() + 1, 0, "ガーディアン");

    SearchFilter filter = search().setSpeciesSearch("アーマード・ドラゴン").build();
    assertQueryFinds(filter, card1, card2);
  }

  @Test
  void findsMatchingSpecies() {
    var card1 = utils.monoCard("test", 6, 6000, LIGHT);
    var card2 = utils.monoCard("test-2", 6, 6000, LIGHT);
    var card3 = utils.monoCard("test-3", 6, 6000, LIGHT);

    utils.addSpeciesToFacet(card1.id() + 1, 0, "アーマード・ドラゴン");
    utils.addSpeciesToFacet(card2.id() + 1, 0, "アーマード・ドラゴン");
    utils.addSpeciesToFacet(card3.id() + 1, 0, "ガーディアン");

    SearchFilter filter = search().setSpeciesSearch("マード").build();
    assertQueryFinds(filter, card1, card2);
    filter = search().setSpeciesSearch("ドラゴン").build();
    assertQueryFinds(filter, card1, card2);
    filter = search().setSpeciesSearch("ー").build();
    assertQueryFinds(filter, card1, card2, card3);
  }

  @Test
  void filtersByRarityEquals() {
    var commonCard = utils.monoCard("common", 6, 6000, LIGHT);
    var rareCard = utils.multiCard("rare", 4, 5500, WATER, NATURE);

    SearchFilter filter = search().setRarity(RarityCode.C).build();
    assertQueryFinds(filter, commonCard);
    filter = search().setRarity(RarityCode.R).build();
    assertQueryFinds(filter, rareCard);
  }

  @Test
  void filtersByRarityRange() {
    var commonCard = utils.monoCard("common", 6, 6000, LIGHT);
    var rareCard = utils.multiCard("rare", 4, 5500, WATER, NATURE);
    var superRareCard = utils.twinpact("twinpact", Set.of(LIGHT), Set.of(FIRE));

    SearchFilter filter = search().setRarity(RarityCode.R, Range.LE).build();
    assertQueryFinds(filter, commonCard, rareCard);
    filter = search().setRarity(RarityCode.R, Range.GE).build();
    assertQueryFinds(filter, rareCard, superRareCard);
  }

  @Test
  void findsCardsWithoutRarity() {
    CardStub noRarity =
        utils.card(
            "dm23rp2x-TF02",
            "DM23RP2X TF2/TF10",
            false,
            RarityCode.NONE,
            1L,
            List.of("dm23rp2x-TF02.jpg"),
            List.of(Set.of(WATER, FIRE, NATURE)),
            List.of(3),
            List.of(),
            List.of(SPELL));
    SearchFilter filter = search().setRarity(RarityCode.NONE, Range.EQ).build();
    assertQueryFinds(filter, noRarity);
  }

  @Test
  void findsCardsByName() {
    var card = utils.monoCard("超神星DEATH・ドラゲリオン", 8, 11000, DARK);
    var card2 = utils.monoCard("超神星ライラ・ボルストーム", 5, 18000, FIRE);
    var card3 = utils.multiCard("メガ・ドラゲナイ・ドラゴン", 9, 15000, FIRE, NATURE);

    SearchFilter filter = search().setNameSearch("ラ").build();
    assertQueryFinds(filter, card, card2, card3);

    filter = search().setNameSearch("death").build();
    assertQueryFinds(filter, card);

    filter = search().setNameSearch("ドラゲ").build();
    assertQueryFinds(filter, card, card3);

    filter = search().setNameSearch("超神星").build();
    assertQueryFinds(filter, card, card2);
  }

  @Test
  void ignoresEmptyNameSearch() {
    var card = utils.monoCard("超神星DEATH・ドラゲリオン", 8, 11000, DARK);
    SearchFilter filter = search().setNameSearch("").build();
    assertQueryFinds(filter, card);
  }

  @Test
  void sortsByCost() {
    var oneCost = utils.monoCard("one", 1, FIRE);
    var zeroCost = utils.monoCard("zero", 0, DARK);
    var fiveCost = utils.monoCard("five", 5, WATER);

    SearchFilter filter =
        search().setPageable(Pageable.unpaged(Sort.by(CardFacet.Columns.COST).ascending())).build();
    assertQueryFindsInOrder(filter, zeroCost, oneCost, fiveCost);
    filter =
        search()
            .setPageable(Pageable.unpaged(Sort.by(CardFacet.Columns.COST).descending()))
            .build();
    assertQueryFindsInOrder(filter, fiveCost, oneCost, zeroCost);
  }

  @Test
  void sortsByMaxCost() {
    var oneCost = utils.monoCard("one", 1, FIRE);
    var zeroCost = utils.monoCard("zero", 0, DARK);
    var fiveCost = utils.monoCard("five", 5, WATER);
    var threeCost = utils.twoSided("three", Set.of(FIRE), Set.of(WATER), 1, 3, 4000, 8000);
    var fourCost = utils.twinpact("four", Set.of(LIGHT), Set.of(LIGHT), 4, 3, 5500);

    SearchFilter filter =
        search().setPageable(Pageable.unpaged(Sort.by(CardFacet.Columns.COST).ascending())).build();
    assertQueryFindsInOrder(filter, zeroCost, oneCost, threeCost, fourCost, fiveCost);
    filter =
        search()
            .setPageable(Pageable.unpaged(Sort.by(CardFacet.Columns.COST).descending()))
            .build();
    assertQueryFindsInOrder(filter, fiveCost, fourCost, threeCost, oneCost, zeroCost);
  }

  @Test
  void sortsNullCostLast() {
    var oneCost = utils.monoCard("one", 1, FIRE);
    var zeroCost = utils.monoCard("zero", 0, DARK);
    var nullCost = utils.monoCard("null", null, WATER);
    var fiveCost = utils.monoCard("five", 5, WATER);
    var threeCost = utils.twoSided("three", Set.of(FIRE), Set.of(WATER), 1, 3, 4000, 8000);

    SearchFilter filter =
        search().setPageable(Pageable.unpaged(Sort.by(CardFacet.Columns.COST).ascending())).build();
    assertQueryFindsInOrder(filter, zeroCost, oneCost, threeCost, fiveCost, nullCost);
    filter =
        search()
            .setPageable(Pageable.unpaged(Sort.by(CardFacet.Columns.COST).descending()))
            .build();
    assertQueryFindsInOrder(filter, fiveCost, threeCost, oneCost, zeroCost, nullCost);
  }

  private void assertQueryFindsAllCards(SearchFilter filter) {
    assertQueryFinds(filter, utils.getTestCards().values().toArray(new CardStub[0]));
  }

  private void assertQueryFindsInOrder(SearchFilter filter, CardStub... expectedCards) {
    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertThat(result.getContent())
        .usingRecursiveComparison()
        .isEqualTo(Arrays.asList(expectedCards));
  }

  private void assertQueryFinds(SearchFilter filter, CardStub... expectedCards) {
    Page<CardStub> result = cardQueryService.search(filter).pageOfCards();
    assertPageEquals(result, expectedCards);
  }

  private void assertQueryFindsNothing(SearchFilter filter) {
    assertQueryFinds(filter);
  }

  private void assertPageEquals(Page<CardStub> result, CardStub... expectedCards) {
    assertThat(result.getContent())
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(Arrays.asList(expectedCards));
  }
}
