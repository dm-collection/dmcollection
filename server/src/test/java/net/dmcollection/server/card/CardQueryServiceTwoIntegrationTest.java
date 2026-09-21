package net.dmcollection.server.card;

import static net.dmcollection.server.card.RarityCode.R;
import static net.dmcollection.server.card.RarityCode.VIC;
import static net.dmcollection.server.card.SearchFilterApi.SORT_AMOUNT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.dmcollection.server.card.internal.CardQueryServiceTwo;
import net.dmcollection.server.testutils.SearchBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CardQueryServiceTwoIntegrationTest extends CardQueryServiceIntegrationTest {

  @Autowired CardQueryServiceTwo cardQueryServiceTwo;

  @Test
  void findsTwoPrintingsOfSameCardTwo() {
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
    var filter = search();

    var result = cardQueryServiceTwo.search(filter.build());
    assertThat(result).isNotNull();
    assertThat(result.cardCount()).isEqualTo(1);
    assertThat(result.cards()).hasSize(1);
    assertThat(result.cards().getFirst().printings()).hasSize(2);
  }

  @Override
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

  @Override
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

  @Override
  protected void assertQueryFindsInOrder(
      SearchBuilder builder, List<CardService.PrintingStub> expectedCards) {
    var result = mapNewResult(cardQueryServiceTwo.search(builder.build()));
    assertThat(result)
        .usingRecursiveComparison()
        .ignoringFields("civilizations")
        .isEqualTo(expectedCards);
  }

  @Override
  protected void assertQueryFinds(
      SearchBuilder builder, List<CardService.PrintingStub> expectedCards) {
    var result = mapNewResult(cardQueryServiceTwo.search(builder.build()));
    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .ignoringFields("civilizations")
        .isEqualTo(expectedCards);
  }

  private List<CardService.PrintingStub> mapNewResult(CardQueryServiceTwo.CardPage page) {
    List<CardService.PrintingStub> oldFormat = new ArrayList<>();
    if (page.cards() == null) {
      return oldFormat;
    }
    page.cards()
        .forEach(
            card -> {
              card.printings()
                  .forEach(
                      p -> {
                        var imageFileNames =
                            p.sides().stream()
                                .map(CardQueryServiceTwo.PrintingSide::imageFileName)
                                .filter(Objects::nonNull)
                                .toList();
                        var stub =
                            new CardService.PrintingStub(
                                p.id(),
                                p.officialId(),
                                p.idText(),
                                null,
                                imageFileNames,
                                p.amount(),
                                p.amount());
                        oldFormat.add(stub);
                      });
            });
    return oldFormat;
  }
}
