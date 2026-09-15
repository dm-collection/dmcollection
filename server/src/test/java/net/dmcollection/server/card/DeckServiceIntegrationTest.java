package net.dmcollection.server.card;

import static net.dmcollection.server.card.RarityCode.VR;
import static net.dmcollection.server.testutils.TestFixtureBuilder.D2_FIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.card.CardService.PrintingStub;
import net.dmcollection.server.card.serialization.deck.format.v1.DeckCardExport;
import net.dmcollection.server.card.serialization.deck.format.v1.DeckExport;
import net.dmcollection.server.testutils.TestFixtureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DeckServiceIntegrationTest extends IntegrationTestBase {

  @Autowired DeckService deckService;
  @Autowired CollectionService collectionService;

  private UUID userId;

  private PrintingStub lightCard;
  private PrintingStub rainbowCard;
  private PrintingStub fireCard;
  private PrintingStub zeroCard;

  @BeforeEach
  void setup() {
    var fixtures = new TestFixtureBuilder(dsl, cardTypeResolver);

    userId = createUser("testuser").getId();

    lightCard = fixtures.testCard("dm01-001").light().cost(6).build();
    rainbowCard =
        fixtures
            .testCard("dm24ex2-040")
            .type(D2_FIELD)
            .allCivs()
            .rarity(VR)
            .withSetCode("dm24ex2")
            .build();
    fireCard = fixtures.testCard("dmc36-003").fire().creature().cost(7).power(7000).build();
    zeroCard = fixtures.testCard("dmr08-021").creature().cost(5).power(2000).build();
  }

  @Test
  void createsNewDecks() {
    var stub = deckService.createDeck(userId, "New Deck");
    assertThat(stub.name()).isEqualTo("New Deck");
    assertThat(stub.numberOfCards()).isZero();
  }

  @Test
  void cardsCanBeAddedToDeck() {
    var info = deckService.createDeck(userId, "New Deck");
    deckService.setCardAmount(userId, info.id(), lightCard.id(), 5);
    var result = deckService.getDeck(userId, info.id());
    assertThat(result).isNotEmpty();
    assertThat(result.get().info().numberOfCards()).isEqualTo(1);

    deckService.setCardAmount(userId, info.id(), rainbowCard.id(), 1);
    result = deckService.getDeck(userId, info.id());
    assertThat(result).isNotEmpty();
    assertThat(result.get().info().numberOfCards()).isEqualTo(2);
  }

  @Test
  void cardsCanBeRemoved() {
    var info = deckService.createDeck(userId, "New Deck");
    deckService.setCardAmount(userId, info.id(), rainbowCard.id(), 1);
    deckService.setCardAmount(userId, info.id(), lightCard.id(), 5);
    deckService.setCardAmount(userId, info.id(), lightCard.id(), 0);
    var result = deckService.getDeck(userId, info.id());
    assertThat(result).isPresent();
    assertThat(result.get().info().numberOfCards()).isEqualTo(1);
  }

  @Test
  void cardsAreCounted() {
    var info = deckService.createDeck(userId, "New Deck");
    deckService.setCardAmount(userId, info.id(), rainbowCard.id(), 2);
    deckService.setCardAmount(userId, info.id(), lightCard.id(), 5);
    deckService.setCardAmount(userId, info.id(), fireCard.id(), 0);
    deckService.setCardAmount(userId, info.id(), zeroCard.id(), 5000);
    var result = deckService.getDeck(userId, info.id());
    assertThat(result).isPresent();
    assertThat(result.get().info().numberOfCopies()).isEqualTo(5007);
    assertThat(result.get().info().numberOfCards()).isEqualTo(3);
  }

  @Test
  void deckCanBeRetrieved() {
    var deckInfo = deckService.createDeck(userId, "New Deck");
    deckService.setCardAmount(userId, deckInfo.id(), rainbowCard.id(), 2);
    deckService.setCardAmount(userId, deckInfo.id(), lightCard.id(), 5);
    deckService.setCardAmount(userId, deckInfo.id(), fireCard.id(), 28);
    deckService.setCardAmount(userId, deckInfo.id(), zeroCard.id(), 5000);

    var result = deckService.getDeck(userId, deckInfo.id());
    assertThat(result).isNotEmpty();
    var deck = result.get();
    assertThat(deck.cardPage().getContent()).hasSize(4);
    assertThat(
            deck.cardPage().getContent().stream()
                .map(card -> new CardIdAndAmount(card.dmId(), card.amount())))
        .containsExactlyInAnyOrder(
            new CardIdAndAmount("dm24ex2-040", 2),
            new CardIdAndAmount("dm01-001", 5),
            new CardIdAndAmount("dmc36-003", 28),
            new CardIdAndAmount("dmr08-021", 5000));
  }

  @Test
  void deckResponseIncludesCollectionAmounts() {
    collectionService.setCardAmount(userId, lightCard.id(), 10);
    collectionService.setCardAmount(userId, rainbowCard.id(), 3);
    collectionService.setCardAmount(userId, fireCard.id(), 7);

    var deckInfo = deckService.createDeck(userId, "Test Deck");
    deckService.setCardAmount(userId, deckInfo.id(), lightCard.id(), 4);
    deckService.setCardAmount(userId, deckInfo.id(), rainbowCard.id(), 2);
    deckService.setCardAmount(userId, deckInfo.id(), zeroCard.id(), 1);

    var result = deckService.getDeck(userId, deckInfo.id());
    assertThat(result).isNotEmpty();
    var deck = result.get();

    assertThat(deck.cardPage().getContent()).hasSize(3);
    deck.cardPage()
        .getContent()
        .forEach(
            cardStub -> {
              switch (cardStub.dmId()) {
                case "dm01-001":
                  assertThat(cardStub.amount()).isEqualTo(4);
                  assertThat(cardStub.collectionAmount()).isEqualTo(10);
                  break;
                case "dm24ex2-040":
                  assertThat(cardStub.amount()).isEqualTo(2);
                  assertThat(cardStub.collectionAmount()).isEqualTo(3);
                  break;
                case "dmr08-021":
                  assertThat(cardStub.amount()).isEqualTo(1);
                  assertThat(cardStub.collectionAmount()).isZero();
                  break;
                default:
                  throw new AssertionError("Unexpected card: " + cardStub.dmId());
              }
            });
  }

  @Test
  void exportAndImportRoundTrip() {
    var deckInfo = deckService.createDeck(userId, "Export Deck");
    deckService.setCardAmount(userId, deckInfo.id(), lightCard.id(), 3);
    deckService.setCardAmount(userId, deckInfo.id(), fireCard.id(), 7);

    var export = deckService.exportDeck(userId, deckInfo.id());
    assertThat(export).isPresent();
    assertThat(export.get().version()).isEqualTo(2);
    assertThat(export.get().title()).isEqualTo("Export Deck");
    assertThat(export.get().countWithoutDuplicates()).isEqualTo(2);
    assertThat(export.get().cardCount()).isEqualTo(10);
    assertThat(export.get().cards()).hasSize(2);

    // Import into same user as a new deck
    deckService.importDeck(userId, export.get());

    var decks = deckService.getDecks(userId);
    assertThat(decks).hasSize(2);
    var importedDeck = decks.stream().filter(d -> !d.id().equals(deckInfo.id())).findFirst();
    assertThat(importedDeck).isPresent();
    assertThat(importedDeck.get().name()).isEqualTo("Export Deck");
    assertThat(importedDeck.get().numberOfCards()).isEqualTo(2);
    assertThat(importedDeck.get().numberOfCopies()).isEqualTo(10);
  }

  @Test
  void importV1FormatByShortName() {
    // Simulate v1 export: has extra fields that Jackson should ignore
    DeckExport v1Export =
        new DeckExport(
            1,
            LocalDateTime.now(),
            "V1 Deck",
            8,
            2,
            List.of(
                new DeckCardExport("Light Card", "dm01-001", 3),
                new DeckCardExport("Fire Card", "dmc36-003", 5)));

    deckService.importDeck(userId, v1Export);

    var decks = deckService.getDecks(userId);
    assertThat(decks).hasSize(1);
    assertThat(decks.getFirst().name()).isEqualTo("V1 Deck");
    assertThat(decks.getFirst().numberOfCards()).isEqualTo(2);
    assertThat(decks.getFirst().numberOfCopies()).isEqualTo(8);
  }

  @Test
  void deckCanBeDeleted() {
    var info = deckService.createDeck(userId, "To Delete");
    deckService.setCardAmount(userId, info.id(), lightCard.id(), 1);
    assertThat(deckService.deleteDeck(userId, info.id())).isTrue();
    assertThat(deckService.getDecks(userId)).isEmpty();
  }

  @Test
  void deckCanBeRenamed() {
    var info = deckService.createDeck(userId, "Old Name");
    var result = deckService.renameDeck(userId, info.id(), "New Name");
    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("New Name");
  }

  private record CardIdAndAmount(String cardId, int amount) {}
}
