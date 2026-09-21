package net.dmcollection.server.card;

import static net.dmcollection.server.card.Civilization.DARK;
import static net.dmcollection.server.card.Civilization.FIRE;
import static net.dmcollection.server.card.Civilization.LIGHT;
import static net.dmcollection.server.card.Civilization.NATURE;
import static net.dmcollection.server.card.Civilization.WATER;
import static net.dmcollection.server.card.Civilization.ZERO;
import static net.dmcollection.server.card.RarityCode.C;
import static net.dmcollection.server.card.RarityCode.NONE;
import static net.dmcollection.server.card.RarityCode.R;
import static net.dmcollection.server.card.RarityCode.SR;
import static net.dmcollection.server.card.RarityCode.VIC;
import static net.dmcollection.server.card.RarityCode.VR;
import static net.dmcollection.server.card.SearchFilterApi.SORT_AMOUNT;
import static net.dmcollection.server.card.SearchFilterApi.SORT_COST;
import static net.dmcollection.server.card.SearchFilterApi.SORT_POWER;
import static net.dmcollection.server.testutils.TestFixtureBuilder.Modifier.LEADING_PLUS;
import static net.dmcollection.server.testutils.TestFixtureBuilder.Modifier.TRAILING_MINUS;
import static net.dmcollection.server.testutils.TestFixtureBuilder.Modifier.TRAILING_PLUS;
import static net.dmcollection.server.testutils.TestFixtureBuilder.PSYCHIC_CREATURE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.card.internal.CardQueryService;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import net.dmcollection.server.card.internal.SearchFilter.FilterState;
import net.dmcollection.server.card.internal.SearchFilter.Range;
import net.dmcollection.server.testutils.SearchBuilder;
import net.dmcollection.server.testutils.TestFixtureBuilder;
import net.dmcollection.server.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CardQueryServiceIntegrationTest extends IntegrationTestBase {

  @Autowired CardQueryService cardQueryService;

  TestFixtureBuilder utils;
  User testUser;

  @BeforeEach
  void setup() {
    testUser = createUser("queryTest-");
    utils = new TestFixtureBuilder(dsl, cardTypeResolver, testUser);
  }

  protected SearchBuilder search() {
    return SearchBuilder.search(testUser);
  }

  @Test
  void findsTwoPrintingsOfSameCard() {
    var printings =
        utils
            .testCard("dm03-004")
            .withSet("dm03", "2002-10-17")
            .withPrinting("dm01-001", "dm01", "2002-05-30")
            .withName("Test Card F")
            .creature()
            .light()
            .power(5000)
            .secondSide()
            .withName("Test Card B")
            .psychicCreature()
            .dark()
            .power(7000)
            .buildAll();

    assertQueryFinds(search(), printings);
  }

  @Test
  void filtersByOwned() {
    var printings =
        utils
            .testCard("dm01-05")
            .withCollectionAmount(5)
            .withPrinting("dm26-01", "dm26", "2026-01-01")
            .withPrinting("dm26-02", "dm26", "2026-01-01", 5)
            .buildAll();
    var separate = utils.testCard("dm2-02").withCollectionAmount(3).build();
    var filter = search().setOwnedOnly();

    assertQueryFinds(filter, printings.getFirst(), printings.getLast(), separate);
  }

  @Test
  void sortsByOwned() {
    var printings =
        utils
            .testCard("dm01-05")
            .withCollectionAmount(5)
            .withPrinting("dm26-06", "dm26", "2026-01-01", 6)
            .buildAll();
    var separate = utils.testCard("dm2-10").withCollectionAmount(10).build();
    var unowned = utils.testCard("dm01-00").build();
    var filter = search().setPageable(PageRequest.of(0, 10, Sort.by(SORT_AMOUNT).descending()));

    // since there are together 11 copies of the first card,
    // it comes first with its printings sorted by amount
    assertQueryFindsInOrder(filter, printings.getLast(), printings.getFirst(), separate, unowned);
  }

  @Test
  void findsColorlessCard() {
    var zero = utils.testCard("Zero").build();

    var filter = search().addIncludedCivs(ZERO).setIncludeMono(true).setIncludeRainbow(false);

    assertQueryFinds(filter, zero);
  }

  @Test
  void excludesMultiCivCards() {
    var mono = utils.testCard("MONO-1").light().build();

    utils.testCard("MULTI-1").light().dark().build();

    var filter = search().addIncludedCivs(LIGHT).setIncludeMono(true).setIncludeRainbow(false);

    assertQueryFinds(filter, mono);
  }

  @Test
  void excludesMonoCivCards() {
    utils.testCard("MONO-1").light().build();
    utils.testCard("MONO-2").dark().build();
    utils.testCard("MONO-3").water().build();

    var multi = utils.testCard("MULTI-1").light().dark().build();
    var multi2 = utils.testCard("MULTI-2").light().dark().water().build();
    var multi3 = utils.testCard("MULTI-3").water().fire().build();
    utils.testCard("MULTI-4").fire().nature().build();

    var filter =
        search().addIncludedCivs(LIGHT, WATER).setIncludeMono(false).setIncludeRainbow(true);
    assertQueryFinds(filter, multi, multi2, multi3);
  }

  @Test
  void findsOnlyTwinpacts() {
    utils.testCard("NORMAL-1").light().build();

    var twinpact = utils.testCard("TWIN-1").twinpact().light().secondSide().dark().build();

    var filter = search().setTwinpact(FilterState.ONLY);
    assertQueryFinds(filter, twinpact);
  }

  @Test
  void findsOnlyTwinpactsWithoutZero() {
    utils.testCard("mono").light().build();
    utils.testCard("zero").build();
    var twinpact1 = utils.testCard("TWIN-1").twinpact().light().secondSide().light().build();
    var twinpact2 =
        utils.testCard("TWIN-2").twinpact().water().dark().secondSide().nature().build();
    utils.testCard("multi").nature().fire().build();
    var all = utils.testCard("all").twinpact().secondSide().allCivs().build();

    var filter =
        search().setTwinpact(FilterState.ONLY).addIncludedCivs(LIGHT, WATER, DARK, FIRE, NATURE);

    assertQueryFinds(filter, twinpact1, twinpact2, all);
  }

  @Test
  void findsNonTwinpacts() {
    var mono = utils.testCard("NORMAL-1").light().build();

    utils.testCard("TWIN-1").twinpact().light().secondSide().dark().build();

    var filter = search().setTwinpact(FilterState.EX);
    assertQueryFinds(filter, mono);
  }

  @Test
  void findsMultiCivCardsWithTwoExactCivs() {
    var lightAndDark = utils.testCard("EXACT-1").light().dark().build();

    utils.testCard("EXTRA-1").light().dark().nature().build();
    utils.testCard("EXTRA-2").light().nature().build();

    var filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK)
            .setMatchExactRainbowCivs(true);
    assertQueryFinds(filter, lightAndDark);
  }

  @Test
  void findsMultiCivCardsWithThreeExactCivs() {
    var lightAndDark = utils.testCard("EXACT-1").light().dark().fire().build();

    utils.testCard("EXTRA-1").light().dark().nature().build();
    utils.testCard("EXTRA-2").light().dark().build();
    utils.testCard("EXTRA-3").light().dark().fire().water().build();
    utils.testCard("EXTRA-4").light().fire().build();

    var filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK, FIRE)
            .setMatchExactRainbowCivs(true);

    assertQueryFinds(filter, lightAndDark);
  }

  @Test
  void findsTwinpactsWithExactCivs() {
    var rainbow = utils.testCard("RAINBOW-1").light().dark().build();
    var twinpact = utils.testCard("TWIN-1").twinpact().light().secondSide().dark().build();
    var twinpact2 = utils.testCard("TWIN-2").twinpact().light().dark().secondSide().dark().build();
    utils.testCard("Exclude").twinpact().light().secondSide().dark().water().build();

    var filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK)
            .setMatchExactRainbowCivs(true);

    assertQueryFinds(filter, rainbow, twinpact, twinpact2);
  }

  @Test
  void findsTwinpactsWithThreeExactCivs() {
    var rainbow = utils.testCard("RAINBOW-1").light().dark().fire().build();
    utils.testCard("RAINBOW-2").light().dark().fire().nature().build();
    utils.testCard("TWIN-1").twinpact().light().secondSide().dark().build();
    var twinpact2 = utils.testCard("TWIN-2").twinpact().light().dark().secondSide().fire().build();
    var twinpact3 = utils.testCard("TWIN-3").twinpact().light().fire().secondSide().dark().build();
    utils.testCard("Exclude").twinpact().light().secondSide().dark().water().build();

    var filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK, FIRE)
            .setMatchExactRainbowCivs(true);

    assertQueryFinds(filter, rainbow, twinpact2, twinpact3);
  }

  @Test
  void findsTwinpactCardsWithoutExactMatch() {
    var fire = utils.testCard("FIRE").fire().build();
    var dark = utils.testCard("DARK").dark().build();
    var rainbow = utils.testCard("RAINBOW-1").light().dark().fire().build();
    var rainbow2 = utils.testCard("RAINBOW-2").light().dark().fire().nature().build();
    var twinpact = utils.testCard("TWIN-1").twinpact().light().secondSide().dark().build();
    var twinpact2 = utils.testCard("TWIN-2").twinpact().light().dark().secondSide().fire().build();
    var twinpact3 = utils.testCard("TWIN-3").twinpact().light().fire().secondSide().dark().build();
    var twinpact4 = utils.testCard("TWIN-4").twinpact().dark().secondSide().fire().build();
    var monoTwinpact = utils.testCard("MONO-TWIN").twinpact().fire().secondSide().fire().build();
    utils.testCard("Exclude").twinpact().water().secondSide().nature().water().build();

    var filter =
        search()
            .setIncludeMono(true)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK, FIRE)
            .setMatchExactRainbowCivs(false);

    assertQueryFinds(
        filter,
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
    utils.testCard("FIRE").fire().build();
    utils.testCard("DARK").dark().build();
    var rainbow = utils.testCard("RAINBOW-1").light().dark().fire().build();
    var rainbow2 = utils.testCard("RAINBOW-2").light().dark().fire().nature().build();
    var twinpact = utils.testCard("TWIN-1").twinpact().light().secondSide().dark().build();
    var twinpact2 = utils.testCard("TWIN-2").twinpact().light().dark().secondSide().fire().build();
    var twinpact3 = utils.testCard("TWIN-3").twinpact().light().fire().secondSide().dark().build();
    var twinpact4 = utils.testCard("TWIN-4").twinpact().dark().secondSide().fire().build();
    utils.testCard("MONO-TWIN").twinpact().fire().secondSide().fire().build();
    utils.testCard("Exclude").twinpact().water().secondSide().nature().water().build();

    var filter =
        search()
            .setIncludeMono(false)
            .setIncludeRainbow(true)
            .addIncludedCivs(LIGHT, DARK, FIRE)
            .setMatchExactRainbowCivs(false);

    assertQueryFinds(filter, rainbow, rainbow2, twinpact, twinpact2, twinpact3, twinpact4);
  }

  @Test
  void defaultFilterFindsSingle() {
    var dm01 = utils.testCard("dm01-001").creature().light().cost(6).power(9000).rarity(VR).build();
    var filter = search();
    assertQueryFinds(filter, dm01);
  }

  @Test
  void defaultFilterFindsAll() {
    var card1 = utils.testCard("CARD-1").light().dark().build();

    var card2 = utils.testCard("CARD-2").twinpact().light().dark().secondSide().water().build();

    var card3 =
        utils
            .testCard("card-3")
            .withSetCode("dm02")
            .fire()
            .psychicCreature()
            .cost(1)
            .power(2000)
            .rarity(R)
            .secondSide()
            .water()
            .cost(2)
            .power(1000)
            .psychicCreature()
            .build();

    var card4 =
        utils
            .testCard("card-4")
            .withSetCode("dm02")
            .creature()
            .cost(4)
            .power(4500)
            .rarity(VIC)
            .build();

    var filter = search();

    assertQueryFinds(filter, card1, card2, card3, card4);
  }

  @Test
  void defaultFilterFindsAllPaged() {
    var card1 = utils.testCard("CARD-1").light().dark().build();

    var card2 = utils.testCard("CARD-2").light().dark().secondSide().water().build();

    var card3 =
        utils
            .testCard("card-3")
            .withSetCode("dm02")
            .fire()
            .psychicCreature()
            .cost(1)
            .power(1500)
            .rarity(R)
            .secondSide()
            .water()
            .cost(2)
            .power(3000)
            .psychicCreature()
            .build();

    var card4 =
        utils
            .testCard("card-4")
            .withSetCode("dm02")
            .creature()
            .cost(4)
            .power(5000)
            .rarity(VIC)
            .build();

    var filter = search().setPageable(PageRequest.of(0, 2, Sort.unsorted()));

    assertQueryFinds(filter, card4, card3);
    filter = search().setPageable(PageRequest.of(1, 2, Sort.unsorted()));
    assertQueryFinds(filter, card2, card1);
  }

  @Test
  void findsCardsInSet() {
    utils.testCard("CARD-1").light().dark().build();

    var expected =
        utils
            .testCard("card-2")
            .withSetCode("dm02")
            .rarity(R)
            .water()
            .psychicCreature()
            .cost(4)
            .power(4000)
            .secondSide()
            .water()
            .psychicCreature()
            .cost(13)
            .power(13000)
            .build();

    var filter = search().setSetId(utils.getSetId("dm02"));

    assertQueryFinds(filter, expected);
  }

  @Test
  void mismatchedMonoFilterFindsNothing() {
    utils.testCard("CARD-1").light().build();
    utils.testCard("CARD-2").fire().build();
    utils.testCard("CARD-3").nature().dark().build();

    var filter = search().addIncludedCivs(WATER).setIncludeMono(true);

    assertQueryFindsNothing(filter);
  }

  @Test
  void findsMultipleCivilizations() {
    var mono = utils.testCard("MONO-1").fire().build();
    var mono2 = utils.testCard("MONO-2").light().build();
    var card1 = utils.testCard("CARD-1").light().dark().build();
    var card2 = utils.testCard("CARD-2").light().dark().water().build();
    var card3 = utils.testCard("CARD-3").fire().water().build();

    utils.testCard("MONO-4").water().build();
    utils.testCard("MONO-3").nature().build();
    utils.testCard("CARD-4").build();
    utils.testCard("foo").dark().nature().build();

    var filter = search().addIncludedCivs(FIRE, LIGHT);
    assertQueryFinds(filter, mono, mono2, card1, card2, card3);
  }

  @Test
  void findsMultipleCivilizationsExcludingMono() {
    utils.testCard("MONO-1").fire().build();
    utils.testCard("MONO-2").light().build();
    var card1 = utils.testCard("CARD-1").light().dark().build();
    var card2 = utils.testCard("CARD-2").light().dark().water().build();
    var card3 = utils.testCard("CARD-3").fire().water().build();

    utils.testCard("MONO-4").water().build();
    utils.testCard("MONO-3").nature().build();
    utils.testCard("CARD-4").build();
    utils.testCard("foo").dark().nature().build();

    var filter = search().addIncludedCivs(FIRE, LIGHT).setIncludeMono(false);
    assertQueryFinds(filter, card1, card2, card3);
  }

  @Test
  void excludesCivilization() {
    utils.testCard("light").light().build();
    var fire = utils.testCard("fire").fire().build();
    utils.testCard("water").water().build();
    utils.testCard("zero").build();
    var dark = utils.testCard("dark").dark().build();

    var darkFire = utils.testCard("darkFire").dark().fire().build();
    utils.testCard("darkFireLight").dark().fire().light().build();
    utils.testCard("waterLight").light().water().build();
    var darkNature = utils.testCard("darkNature").dark().nature().build();

    var filter = search().addIncludedCivs(DARK, FIRE).addExcludedCivs(LIGHT, WATER);
    assertQueryFinds(filter, fire, dark, darkFire, darkNature);
  }

  @Test
  void excludesCivilizationsAndMono() {
    utils.testCard("light").light().build();
    utils.testCard("fire").fire().build();
    utils.testCard("water").water().build();
    utils.testCard("zero").build();
    utils.testCard("dark").dark().build();

    var darkFire = utils.testCard("darkFire").dark().fire().build();
    utils.testCard("darkFireLight").dark().fire().light().build();
    utils.testCard("waterLight").light().water().build();
    var darkNature = utils.testCard("darkNature").dark().nature().build();

    var filter =
        search().addIncludedCivs(DARK, FIRE).addExcludedCivs(LIGHT, WATER).setIncludeMono(false);
    assertQueryFinds(filter, darkFire, darkNature);
  }

  @Test
  void findsZeroOnly() {
    var zero = utils.testCard("zero").build();
    utils.testCard("light").light().build();
    utils.testCard("fire").fire().build();
    utils.testCard("water").water().build();
    utils.testCard("nature").nature().build();
    var zero2 = utils.testCard("02").build();
    utils.testCard("multi1").light().water().build();
    utils.testCard("multi2").dark().fire().light().water().nature().build();

    var filter = search().addIncludedCivs(ZERO).addExcludedCivs(LIGHT, WATER, FIRE, DARK, NATURE);
    assertQueryFinds(filter, zero, zero2);
  }

  @Test
  void excludesRainbow() {
    // excluded rainbow cards
    utils.testCard("TWIN-1").twinpact().light().secondSide().dark().build();
    utils.testCard("TWIN-2").twinpact().light().dark().secondSide().fire().build();
    utils.testCard("TWIN-3").twinpact().fire().secondSide().dark().build();
    utils.testCard("TWIN-4").twinpact().dark().secondSide().light().build();
    utils.testCard("multi1").light().dark().build();

    var mono1 = utils.testCard("mono1").light().build();
    var mono2 = utils.testCard("mono2").dark().build();
    var monoTwinpact = utils.testCard("monoTwin").twinpact().light().secondSide().light().build();
    utils.testCard("unrelatedMonoTwinpact").twinpact().water().secondSide().water().build();

    // included via both sides
    var twoSides = utils.testCard("twoSided1").light().secondSide().dark().build();
    // included because of monochrome light side
    var twoSides2 = utils.testCard("twoSided2").light().secondSide().fire().build();
    // included because of monochrome dark side
    var twoSides3 = utils.testCard("twoSided3").light().fire().secondSide().dark().build();
    // excluded because no monochrome light or dark side
    utils.testCard("exclude").water().secondSide().nature().light().build();

    var filter = search().setIncludeRainbow(false).addIncludedCivs(LIGHT, DARK);
    assertQueryFinds(filter, mono1, mono2, monoTwinpact, twoSides, twoSides2, twoSides3);
  }

  @Test
  void findsCardsWithMinimumCost() {
    utils.testCard("CHEAP-1").light().cost(2).build();
    utils.testCard("CHEAP-2").fire().cost(3).build();
    var expensive1 = utils.testCard("EXPENSIVE-1").water().cost(5).build();
    var expensive2 = utils.testCard("EXPENSIVE-2").dark().fire().cost(7).build();
    var expensive3 = utils.testCard("EXPENSIVE-3").nature().cost(6).build();

    var twinpact =
        utils
            .testCard("twinpact")
            .twinpact()
            .light()
            .cost(5)
            .secondSide(s -> s.dark().cost(3))
            .build();

    var filter = search().setMinCost(5);
    assertQueryFinds(filter, expensive1, expensive2, expensive3, twinpact);
  }

  @Test
  void findsCardsWithMaximumCost() {
    var cheap1 = utils.testCard("CHEAP-1").light().cost(2).build();
    var cheap2 = utils.testCard("CHEAP-2").fire().cost(3).build();
    var cheap3 = utils.testCard("CHEAP-3").water().dark().cost(4).build();
    utils.testCard("EXPENSIVE-1").water().cost(5).build();
    utils.testCard("EXPENSIVE-2").dark().fire().cost(7).build();
    utils.testCard("EXPENSIVE-3").nature().cost(6).build();

    var twinpact =
        utils.testCard("twinpact").light().cost(5).secondSide(s -> s.dark().cost(3)).build();

    var filter = search().setMaxCost(4);
    assertQueryFinds(filter, cheap1, cheap2, cheap3, twinpact);
  }

  @Test
  void findsCardsWithinCostRange() {
    utils.testCard("CHEAP-1").light().cost(2).build();
    utils.testCard("CHEAP-2").fire().cost(3).build();
    var medium1 = utils.testCard("MEDIUM-1").water().dark().cost(4).build();
    var medium2 = utils.testCard("MEDIUM-2").water().cost(5).build();
    var medium3 = utils.testCard("MEDIUM-3").nature().cost(6).build();
    utils.testCard("EXPENSIVE-1").dark().fire().cost(7).build();
    utils.testCard("EXPENSIVE-2").nature().cost(8).build();

    var twinpact =
        utils.testCard("twinpact").light().cost(5).secondSide(s -> s.dark().cost(3)).build();
    utils.testCard("outside").water().cost(3).secondSide(s -> s.nature().fire().cost(8)).build();

    var filter = search().setMinCost(4).setMaxCost(6);
    assertQueryFinds(filter, medium1, medium2, medium3, twinpact);
  }

  @Test
  void findsCardsWithMinimumPower() {
    utils.testCard("WEAK-1").light().cost(2).power(2000).creature().build();
    utils.testCard("WEAK-2").fire().cost(3).power(3000).creature().build();
    var strong1 = utils.testCard("STRONG-1").water().cost(5).power(5000).creature().build();
    var strong2 = utils.testCard("STRONG-2").dark().fire().cost(7).power(7000).build();
    var strong3 = utils.testCard("STRONG-3").nature().cost(6).power(6000).creature().build();

    var twinpact =
        utils.testCard("twinpact").twinpact().light().power(5000).secondSide().dark().build();
    var twoSided =
        utils
            .testCard("two-sides")
            .water()
            .cost(4)
            .power(4000)
            .secondSide()
            .fire()
            .cost(7)
            .power(7000)
            .build();

    var filter = search().setMinPower(5000);
    assertQueryFinds(filter, strong1, strong2, strong3, twinpact, twoSided);
  }

  @Test
  void findsCardsWithMaximumPower() {
    var weak1 = utils.testCard("WEAK-1").light().cost(2).power(2000).creature().build();
    var weak2 = utils.testCard("WEAK-2").fire().cost(3).power(3000).creature().build();
    var weak3 = utils.testCard("WEAK-3").water().dark().cost(4).power(4000).build();
    utils.testCard("STRONG-1").water().cost(5).power(5000).creature().build();
    utils.testCard("STRONG-2").dark().fire().cost(7).power(7000).build();
    utils.testCard("STRONG-3").nature().cost(6).power(6000).creature().build();

    utils
        .testCard("twinpact")
        .twinpact()
        .light()
        .cost(5)
        .power(5000)
        .secondSide()
        .dark()
        .cost(3)
        .build();
    var twoSided =
        utils
            .testCard("two-sides")
            .water()
            .cost(4)
            .power(4000)
            .secondSide()
            .fire()
            .cost(7)
            .power(7000)
            .build();

    var filter = search().setMaxPower(4000);
    assertQueryFinds(filter, weak1, weak2, weak3, twoSided);
  }

  @Test
  void findsCardsWithinPowerRange() {
    utils.testCard("WEAK-1").light().cost(2).power(2000).creature().build();
    utils.testCard("WEAK-2").fire().cost(3).power(3000).creature().build();
    var medium1 = utils.testCard("MEDIUM-1").water().dark().cost(4).power(4000).build();
    var medium2 = utils.testCard("MEDIUM-2").water().cost(5).power(5000).creature().build();
    var medium3 = utils.testCard("MEDIUM-3").nature().cost(6).power(6000).creature().build();
    utils.testCard("STRONG-1").dark().fire().cost(7).power(7000).build();
    utils.testCard("STRONG-2").nature().cost(8).power(8000).creature().build();

    var twinpact =
        utils.testCard("twinpact").light().cost(5).power(5000).secondSide().dark().cost(3).build();
    utils
        .testCard("outside")
        .twinpact()
        .water()
        .cost(3)
        .power(3000)
        .secondSide()
        .nature()
        .fire()
        .cost(8)
        .build();
    utils
        .testCard("two-sides")
        .water()
        .cost(4)
        .power(2000)
        .secondSide()
        .fire()
        .cost(7)
        .power(7000)
        .build();

    var filter = search().setMinPower(4000).setMaxPower(6000);
    assertQueryFinds(filter, medium1, medium2, medium3, twinpact);
  }

  @Test
  void onlyMinPowerOrCostFindsInfinitePowerCard() {
    var thatInfiniteCard =
        utils
            .testCard("dm24ex1-SP2")
            .water()
            .dark()
            .cost(Integer.MAX_VALUE)
            .power(Integer.MAX_VALUE)
            .build();
    var filter = search().setMinCost(1000000);
    assertQueryFinds(filter, thatInfiniteCard);
    filter = search().setMinPower(1000000);
    assertQueryFinds(filter, thatInfiniteCard);
    filter = search().setMinCost(1000000).setMaxCost(Integer.MAX_VALUE);
    assertQueryFindsNothing(filter);
    filter = search().setMinPower(1000000).setMaxPower(Integer.MAX_VALUE);
    assertQueryFindsNothing(filter);
    filter = search().setMinPower(Integer.MAX_VALUE).setMaxPower(Integer.MAX_VALUE);
    assertQueryFindsNothing(filter);
    filter = search().setMinPower(Integer.MAX_VALUE);
    assertQueryFinds(filter, thatInfiniteCard);
  }

  @Test
  void handlesCostRangeWithCivilizationFilter() {
    utils.testCard("CHEAP-1").light().cost(2).build();
    var medium1 = utils.testCard("MEDIUM-1").light().cost(4).build();
    var medium2 = utils.testCard("MEDIUM-2").light().dark().cost(5).build();
    utils.testCard("EXPENSIVE-1").light().cost(7).build();
    utils.testCard("OTHER-1").fire().cost(4).build();
    utils.testCard("OTHER-2").water().dark().cost(5).build();

    var filter = search().setMinCost(4).setMaxCost(6).addIncludedCivs(LIGHT);
    assertQueryFinds(filter, medium1, medium2);
  }

  @Test
  void includesNullCostWhenNoCostFilter() {
    var cards =
        List.of(
            utils.testCard("COST-1").light().cost(2).build(),
            utils.testCard("COST-2").light().cost(null).build(),
            utils.testCard("COST-3").light().cost(4).build(),
            utils.testCard("COST-4").light().cost(null).build(),
            utils.testCard("COST-5").light().cost(6).build());

    assertQueryFinds(search(), cards);
  }

  @Test
  void excludesNullCostWhenCostFilter() {
    var card1 = utils.testCard("COST-1").light().cost(2).build();
    utils.testCard("COST-null").light().cost(null);
    var card2 = utils.testCard("COST-3").light().cost(4).build();
    utils.testCard("COST-null2").light().cost(null);
    var card3 = utils.testCard("COST-5").light().cost(6).build();

    var filter = search().setMinCost(3);
    assertQueryFinds(filter, card2, card3);
    filter = search().setMaxCost(4);
    assertQueryFinds(filter, card1, card2);
  }

  @Test
  void findsThatOneFourSidedCard() {
    var thatOneCard = utils.createFourSides();
    var filter =
        search()
            .setIncludeMono(false)
            .addIncludedCivs(WATER, FIRE, NATURE)
            .setMatchExactRainbowCivs(true);
    assertQueryFinds(filter, thatOneCard);
    filter = search().addIncludedCivs(FIRE);
    assertQueryFinds(filter, thatOneCard);
  }

  @Test
  void twinpactExcludesFoursides() {
    utils.createFourSides();
    var filter = search().setTwinpact(FilterState.ONLY).setIncludeRainbow(false);
    assertQueryFindsNothing(filter);
  }

  @Test
  void findsAllMultiColoredCards() {
    utils.testCard("uncolored");
    utils.testCard("light").light().build();
    utils.testCard("water").water().build();
    utils.testCard("darkness").dark().build();
    utils.testCard("fire").fire().build();
    utils.testCard("nature").nature().build();
    utils.testCard("monoTwin").twinpact().water().secondSide().water().build();

    var multi1 = utils.testCard("multi1").light().water().build();
    var multi2 = utils.testCard("multi2").fire().nature().build();
    var multi3 = utils.testCard("multi3").water().dark().nature().build();
    var multiTwinpact = utils.testCard("twinpact").twinpact().light().secondSide().fire().build();

    var filter =
        search().setIncludeMono(false).setIncludeRainbow(true).setMatchExactRainbowCivs(false);
    assertQueryFinds(filter, multi1, multi2, multi3, multiTwinpact);
  }

  @Test
  void findsSpellsOrCreatures() {
    var spell1 = utils.testCard("spell1").spell().light().cost(6).build();
    var spell2 = utils.testCard("spell2").spell().nature().cost(2).build();

    var creature1 = utils.testCard("creature1").light().cost(1).power(4000).creature().build();
    var creature2 = utils.testCard("creature2").fire().cost(1).power(1000).creature().build();

    var filter = search().setCardType(CardType.CREATURE);
    assertQueryFinds(filter, creature1, creature2);
    filter = search().setCardType(CardType.SPELL);
    assertQueryFinds(filter, spell1, spell2);
  }

  @Test
  void findsOnlyEvolutionCreature() {
    var creature = utils.testCard("creature1").creature().light().cost(1).power(4000).build();
    var evolutionCreature = utils.testCard("evoCreature").evolutionCreature().dark().build();

    var filter = search().setCardType(CardType.CREATURE);
    assertQueryFinds(filter, creature);

    filter = search().setCardType(CardType.EVOLUTION);
    assertQueryFinds(filter, evolutionCreature);
  }

  @Test
  void findsOthers() {
    var rulePlus =
        utils
            .testCard("rulePlus")
            .type("ルール・プラス")
            .water()
            .secondSide()
            .withType(PSYCHIC_CREATURE)
            .water()
            .nature()
            .cost(7)
            .power(4000)
            .build();
    var sealed =
        utils
            .testCard("sealed")
            .fire()
            .type("禁断の鼓動")
            .secondSide()
            .fire()
            .withType("禁断クリーチャー")
            .cost(99)
            .power(99999)
            .build();
    utils.testCard("creature1").light().cost(1).power(4000).creature().build();

    var filter = search().setCardType(CardType.OTHER);

    assertQueryFinds(filter, rulePlus, sealed);
  }

  @Test
  void findsExactSpecies() {
    var card1 =
        utils.testCard("test").light().cost(6).power(6000).creature().race("アーマード・ドラゴン").build();
    var card2 =
        utils.testCard("test-2").light().cost(6).power(6000).creature().race("アーマード・ドラゴン").build();
    utils.testCard("test-3").light().cost(6).power(6000).creature().race("ガーディアン").build();

    var filter = search().setSpeciesSearch("アーマード・ドラゴン");
    assertQueryFinds(filter, card1, card2);
  }

  @Test
  void findsMatchingSpecies() {
    var card1 =
        utils.testCard("test").light().cost(6).power(6000).creature().race("アーマード・ドラゴン").build();
    var card2 =
        utils.testCard("test-2").light().cost(6).power(6000).creature().race("アーマード・ドラゴン").build();
    var card3 =
        utils.testCard("test-3").light().cost(6).power(6000).creature().race("ガーディアン").build();

    var filter = search().setSpeciesSearch("マード");
    assertQueryFinds(filter, card1, card2);
    filter = search().setSpeciesSearch("ドラゴン");
    assertQueryFinds(filter, card1, card2);
    filter = search().setSpeciesSearch("ー");
    assertQueryFinds(filter, card1, card2, card3);
  }

  @Test
  void filtersByRarityEquals() {
    var commonCard = utils.testCard("common").light().cost(6).rarity(C).build();
    var rareCard = utils.testCard("rare").water().nature().cost(4).rarity(R).build();

    var filter = search().setRarity(C);
    assertQueryFinds(filter, commonCard);
    filter = search().setRarity(R);
    assertQueryFinds(filter, rareCard);
  }

  @Test
  void filtersByRarityRange() {
    var commonCard = utils.testCard("common").light().power(6000).creature().rarity(C).build();
    var rareCard = utils.testCard("rare").water().nature().cost(4).power(5500).rarity(R).build();
    var superRareCard = utils.testCard("superRare").fire().cost(5).power(10000).rarity(SR).build();

    var filter = search().setRarity(R, Range.LE);
    assertQueryFinds(filter, commonCard, rareCard);
    filter = search().setRarity(R, Range.GE);
    assertQueryFinds(filter, rareCard, superRareCard);
  }

  @Test
  void findsCardsWithoutRarity() {
    var noRarity =
        utils
            .testCard("dm23rp2x-TF02")
            .withSetCode("dm23rp2x")
            .spell()
            .water()
            .fire()
            .nature()
            .cost(3)
            .rarity(NONE)
            .build();
    var filter = search().setRarity(NONE, Range.EQ);
    assertQueryFinds(filter, noRarity);
  }

  @Test
  void findsCardsByName() {
    var card = utils.testCard("超神星DEATH・ドラゲリオン").dark().cost(8).power(11000).creature().build();
    var card2 = utils.testCard("超神星ライラ・ボルストーム").fire().cost(5).power(18000).creature().build();
    var card3 = utils.testCard("メガ・ドラゲナイ・ドラゴン").fire().nature().cost(9).power(15000).build();

    var filter = search().setNameSearch("ラ");
    assertQueryFinds(filter, card, card2, card3);

    filter = search().setNameSearch("death");
    assertQueryFinds(filter, card);

    filter = search().setNameSearch("ドラゲ");
    assertQueryFinds(filter, card, card3);

    filter = search().setNameSearch("超神星");
    assertQueryFinds(filter, card, card2);
  }

  @Test
  void ignoresEmptyNameSearch() {
    var card = utils.testCard("超神星DEATH・ドラゲリオン").dark().cost(8).power(11000).creature().build();
    var filter = search().setNameSearch("");
    assertQueryFinds(filter, card);
  }

  @Test
  void quotedNameMatchesExactly() {
    var bolshack =
        utils
            .testCard("dm01-008")
            .withName("ボルシャック・ドラゴン")
            .fire()
            .creature()
            .cost(6)
            .power(6000, TRAILING_PLUS)
            .build();
    var neoBolshak =
        utils
            .testCard("dmx19-s32")
            .withName("ネオ・ボルシャック・ドラゴン")
            .fire()
            .creature()
            .cost(8)
            .power(11000, TRAILING_PLUS)
            .build();
    var bolshackCharger =
        utils
            .testCard("dmex04-027")
            .twinpact()
            .withName("ボルシャック・ドラゴン")
            .fire()
            .cost(6)
            .power(6000, TRAILING_PLUS)
            .secondSide()
            .withName("決闘者・チャージャー")
            .fire()
            .cost(3)
            .build();

    var filter = search().setNameSearch("ボルシャック・ドラゴン");
    assertQueryFinds(filter, bolshack, neoBolshak, bolshackCharger);

    filter = search().setNameSearch("\"ボルシャック・ドラゴン\"");
    assertQueryFinds(filter, bolshack);

    filter = search().setNameSearch("\"ボルシャック・ドラゴン／決闘者・チャージャー\"");
    assertQueryFinds(filter, bolshackCharger);
  }

  @Test
  void findsCardsByEffectText() {
    var blocker = utils.testCard("blocker").withAbility("ブロッカー").build();
    var wBreaker = utils.testCard("double-breaker").withAbility("W・ブレイカー").build();
    utils.testCard("unrelated").withAbility("このクリーチャーが攻撃する時、カードを1枚引く。").build();

    var filter = search().setEffectSearch("ブロッカー");
    assertQueryFinds(filter, blocker);

    filter = search().setEffectSearch("ブレイカー");
    assertQueryFinds(filter, wBreaker);

    filter = search().setEffectSearch("W");
    assertQueryFinds(filter, wBreaker);
  }

  @Test
  void findsCardsByEffectTextWithSpecialCharacters() {
    var percentCard = utils.testCard("percent-card").withAbility("パワー+50%").build();
    var underscoreCard = utils.testCard("underscore-card").withAbility("test_effect").build();
    utils.testCard("normal-card").withAbility("通常の効果").build();

    var filter = search().setEffectSearch("%");
    assertQueryFinds(filter, percentCard);

    filter = search().setEffectSearch("_");
    assertQueryFinds(filter, underscoreCard);

    filter = search().setEffectSearch("+");
    assertQueryFinds(filter, percentCard);
  }

  @Test
  void findsCardsByChildEffectText() {
    var cardWithChildren =
        utils
            .testCard("modal-effect-card")
            .fire()
            .creature()
            .cost(5)
            .power(7000)
            .withAbility("このクリーチャーが出た時、次の中から２回選ぶ。(同じものを選んでもよい)")
            .withChildAbilities(
                "相手のクリーチャーを１体選ぶ。このターン、そのクリーチャーのパワーを－4000する。",
                "自分の山札の上から４枚を墓地に置く。",
                "コスト４以下のクリーチャーを１体、自分の墓地から出す。")
            .build();
    utils
        .testCard("different-card")
        .water()
        .cost(4)
        .power(5000)
        .withAbility("このクリーチャーが出た時、カードを２枚引く。")
        .build();

    var filter = search().setEffectSearch("墓地に置く");
    assertQueryFinds(filter, cardWithChildren);

    filter = search().setEffectSearch("パワーを－4000する");
    assertQueryFinds(filter, cardWithChildren);

    filter = search().setEffectSearch("次の中から");
    assertQueryFinds(filter, cardWithChildren);
  }

  @Test
  void combinesEffectSearchWithNameFilter() {
    var dragonBlocker =
        utils
            .testCard("dm01-008")
            .withName("ボルシャック・ドラゴン")
            .fire()
            .cost(6)
            .power(9000)
            .withAbility("ブロッカー")
            .build();
    var dragonBreaker =
        utils
            .testCard("dmxy-zz")
            .withName("ボルシャック・大剣")
            .fire()
            .cost(7)
            .power(11000)
            .withAbility("W・ブレイカー")
            .build();
    utils
        .testCard("dm01-013")
        .withName("光の守護者")
        .light()
        .cost(5)
        .power(7000)
        .withAbility("ブロッカー")
        .build();

    var filter = search().setEffectSearch("ブロッカー").setNameSearch("ボルシャック");
    assertQueryFinds(filter, dragonBlocker);

    filter = search().setEffectSearch("ブレイカー").setNameSearch("ボルシャック");
    assertQueryFinds(filter, dragonBreaker);

    filter = search().setEffectSearch("ブレイカー").setNameSearch("ドラゴン");
    assertQueryFindsNothing(filter);
  }

  @Test
  void combinesEffectSearchWithSpeciesFilter() {
    var dragonWithBlocker =
        utils
            .testCard("test-dragon-1")
            .fire()
            .cost(6)
            .power(8000)
            .withAbility("ブロッカー")
            .race("アーマード・ドラゴン")
            .build();

    var dragonWithBreaker =
        utils
            .testCard("test-dragon-2")
            .fire()
            .cost(7)
            .power(10000)
            .withAbility("W・ブレイカー")
            .race("アーマード・ドラゴン")
            .build();

    utils.testCard("test-guardian").light().cost(5).power(6000).withAbility("ブロッカー").race("ガーディアン");

    var filter = search().setEffectSearch("ブロッカー").setSpeciesSearch("ドラゴン");
    assertQueryFinds(filter, dragonWithBlocker);

    filter = search().setEffectSearch("ブレイカー").setSpeciesSearch("アーマード・ドラゴン");
    assertQueryFinds(filter, dragonWithBreaker);
  }

  @Test
  void combinesEffectSearchWithNameAndCivilizationFilters() {
    var lightCard =
        utils.testCard("聖なる守護者").light().cost(5).power(7000).withAbility("ブロッカー").build();
    var fireCard = utils.testCard("聖なる炎").fire().cost(6).power(8000).withAbility("ブロッカー").build();
    utils.testCard("水の守護者").water().cost(4).power(6000).withAbility("ブロッカー").build();

    var filter = search().setEffectSearch("ブロッカー").setNameSearch("守護者").addIncludedCivs(LIGHT);
    assertQueryFinds(filter, lightCard);

    filter = search().setEffectSearch("ブロッカー").setNameSearch("聖なる").addIncludedCivs(FIRE);
    assertQueryFinds(filter, fireCard);
  }

  @Test
  void findsEffectsCaseInsensitive() {
    var doubleBreaker = utils.testCard("w-breaker").withAbility("W・ブレイカー").build();

    var filter = search().setEffectSearch("w");
    assertQueryFinds(filter, doubleBreaker);

    var exLife = utils.testCard("exLife").withAbility("EXライフ").build();

    filter = search().setEffectSearch("ex");
    assertQueryFinds(filter, exLife);

    var over = utils.testCard("over").withAbility("OVERハイパー化：自分の他のクリーチャーを２体タップする。").build();
    assertQueryFinds(search().setEffectSearch("over"), over);

    var dlsys = utils.testCard("dlsys").withAbility("DL-Sys：これを付けたクリーチャーの攻撃の終わりに、相手の...").build();
    assertQueryFinds(search().setEffectSearch("dl-sys"), dlsys);

    var code =
        utils
            .testCard("code")
            .withAbility("S-MAX進化：自分がゲームに負ける時、かわりにこのクリーチャーを破壊するか、自分の手札から《Code:-MAX》を１枚捨てる...）")
            .build();
    assertQueryFinds(search().setEffectSearch("code"), code);
    assertQueryFinds(search().setEffectSearch("max"), code);
    assertQueryFinds(search().setEffectSearch("s-max"), code);

    var artifact =
        utils
            .testCard("artifact")
            .withAbility("このArtifactが出た時、封印を３つ付ける。（カードを封印するには、自分の山札の上から１枚目を裏向きのままそのカードの上に置く）")
            .build();
    assertQueryFinds(search().setEffectSearch("artifact"), artifact);

    var revo =
        utils
            .testCard("revo")
            .withAbility("キリフダReVo：このクリーチャーが「キリフダッシュ」能力によってバトルゾーンに出たターンの間、...")
            .build();
    assertQueryFinds(search().setEffectSearch("revo"), revo);

    var mt =
        utils
            .testCard("mt")
            .withAbility("バトルゾーンに自分の他の《Mt.富士山ックス》があれば、このクリーチャーのパワーを+11000し、「T・ブレイカー」を与える。")
            .build();
    assertQueryFinds(search().setEffectSearch("mt"), mt);

    var shigenobu =
        utils
            .testCard("shigenobu-m")
            .withAbility(
                "自分の、イラストレーター名がShigenobu Matsumotoのクリーチャーの召喚コストを１少なくしてもよい。ただし、コストは０以下にならない。")
            .build();
    assertQueryFinds(search().setEffectSearch("shigenobu"), shigenobu);
    assertQueryFinds(search().setEffectSearch("matsumoto"), shigenobu);

    var second =
        utils
            .testCard("second")
            .withAbility(
                "G・ゼロ―このターン、カードを６枚以上引いていて、自分の 《天災超邪 クロスファイア ２nd》がバトルゾーンになければ、このクリーチャーをコストを支払わずに召喚してもよい。")
            .build();
    assertQueryFinds(search().setEffectSearch("nd"), second);
  }

  @Test
  void sortsByCost() {
    var oneCost = utils.testCard("one").fire().cost(1).build();
    var zeroCost = utils.testCard("zero").dark().cost(0).build();
    var fiveCost = utils.testCard("five").water().cost(5).build();

    var filter = search().setPageable(PageRequest.of(0, 100, Sort.by(SORT_COST).ascending()));
    assertQueryFindsInOrder(filter, zeroCost, oneCost, fiveCost);
    filter = search().setPageable(PageRequest.of(0, 100, Sort.by(SORT_COST).descending()));
    assertQueryFindsInOrder(filter, fiveCost, oneCost, zeroCost);
  }

  @Test
  void sortsByFirstSideCost() {
    var oneCost = utils.testCard("one").fire().cost(1).build();
    var zeroCost = utils.testCard("zero").dark().cost(0).build();
    var fiveCost = utils.testCard("five").water().cost(5).build();
    var threeCost = utils.testCard("three").fire().cost(3).secondSide().water().cost(1).build();
    var fourCost =
        utils.testCard("four").twinpact().light().cost(4).secondSide().light().cost(3).build();

    var filter = search().setPageable(PageRequest.of(0, 100, Sort.by(SORT_COST).ascending()));
    assertQueryFindsInOrder(filter, zeroCost, oneCost, threeCost, fourCost, fiveCost);
    filter = search().setPageable(PageRequest.of(0, 100, Sort.by(SORT_COST).descending()));
    assertQueryFindsInOrder(filter, fiveCost, fourCost, threeCost, oneCost, zeroCost);
  }

  @Test
  void sortsNullCostLast() {
    var oneCost = utils.testCard("one").fire().cost(1).build();
    var zeroCost = utils.testCard("zero").dark().cost(0).build();
    var nullCost = utils.testCard("null").water().cost(null).build();
    var fiveCost = utils.testCard("five").water().cost(5).build();
    var threeCost = utils.testCard("three").fire().cost(3).secondSide().water().cost(1).build();

    var filter = search().setPageable(PageRequest.of(0, 100, Sort.by(SORT_COST).ascending()));
    assertQueryFindsInOrder(filter, zeroCost, oneCost, threeCost, fiveCost, nullCost);
    filter = search().setPageable(PageRequest.of(0, 100, Sort.by(SORT_COST).descending()));
    assertQueryFindsInOrder(filter, fiveCost, threeCost, oneCost, zeroCost, nullCost);
  }

  @Test
  void sortsByFirstSidePower() {
    var negative = utils.testCard("negative").cost(0).power(-5000).secondSide().power(5000).build();
    var zero = utils.testCard("zero").cost(0).power(0).build();
    var noPower = utils.testCard("none").spell().build();
    var plusZero = utils.testCard("plusZero").power(0, LEADING_PLUS).build();
    var zeroPlus = utils.testCard("zeroPlus").power(0, TRAILING_PLUS).build();
    var tenk = utils.testCard("10k").power(10000).secondSide().power(2000).build();
    var tenkPlus = utils.testCard("10k+").power(10000, TRAILING_PLUS).build();
    var tenkMinus = utils.testCard("10k-").power(10000, TRAILING_MINUS).build();

    var filter = search().setPageable(PageRequest.of(0, 100, Sort.by(SORT_POWER).ascending()));
    assertQueryFindsInOrder(
        filter, negative, zero, plusZero, zeroPlus, tenkMinus, tenk, tenkPlus, noPower);
    filter = search().setPageable(PageRequest.of(0, 100, Sort.by(SORT_POWER).descending()));
    assertQueryFindsInOrder(
        filter, tenkPlus, tenk, tenkMinus, zeroPlus, plusZero, zero, negative, noPower);
  }

  protected void assertQueryFindsInOrder(SearchBuilder builder, PrintingStub... expectedCards) {
    assertQueryFindsInOrder(builder, Arrays.asList(expectedCards));
  }

  protected void assertQueryFinds(SearchBuilder builder, PrintingStub... expectedCards) {

    assertQueryFinds(builder, Arrays.asList(expectedCards));
  }

  protected void assertQueryFindsNothing(SearchBuilder builder) {
    assertQueryFinds(builder, Collections.emptyList());
  }

  protected void assertQueryFindsInOrder(SearchBuilder builder, List<PrintingStub> expectedCards) {
    var result = mapResult(cardQueryService.search(builder.build()));
    assertThat(result)
        .usingRecursiveComparison()
        .ignoringFields("civilizations")
        .isEqualTo(expectedCards);
  }

  protected void assertQueryFinds(SearchBuilder builder, List<PrintingStub> expectedCards) {
    var result = mapResult(cardQueryService.search(builder.build()));
    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .ignoringFields("civilizations")
        .isEqualTo(expectedCards);
  }

  private List<PrintingStub> mapResult(Page<CardStub> page) {
    List<PrintingStub> oldFormat = new ArrayList<>();
    if (page == null) {
      return oldFormat;
    }
    page.forEach(card -> oldFormat.addAll(card.printings()));

    return oldFormat;
  }
}
