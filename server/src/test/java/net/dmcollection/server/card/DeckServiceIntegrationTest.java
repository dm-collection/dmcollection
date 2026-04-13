package net.dmcollection.server.card;

import static net.dmcollection.server.TestFixtureBuilder.D2_FIELD;
import static net.dmcollection.server.card.Civilization.DARK;
import static net.dmcollection.server.card.Civilization.FIRE;
import static net.dmcollection.server.card.Civilization.LIGHT;
import static net.dmcollection.server.card.Civilization.NATURE;
import static net.dmcollection.server.card.Civilization.WATER;
import static net.dmcollection.server.card.Civilization.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.TestFixtureBuilder;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.DeckService.DeckCardExport;
import net.dmcollection.server.card.DeckService.DeckExport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DeckServiceIntegrationTest extends IntegrationTestBase {

  @Autowired DeckService deckService;
  @Autowired CollectionService collectionService;

  private UUID userId;

  private CardStub lightCard;
  private CardStub rainbowCard;
  private CardStub fireCard;
  private CardStub zeroCard;

  @BeforeEach
  void setup() {
    TestFixtureBuilder fixtures = new TestFixtureBuilder(dsl, cardTypeResolver);

    userId = createUser("testuser").getId();

    lightCard = fixtures.monoCard("dm01-001", 6, LIGHT);
    rainbowCard =
        fixtures.card(
            "dm24ex2-040",
            "DM24EX2 40/100",
            false,
            RarityCode.VR,
            350,
            List.of("dm24ex2-040.jpg"),
            List.of(Set.of(LIGHT, WATER, DARK, FIRE, NATURE)),
            null,
            null,
            List.of(D2_FIELD));
    fireCard = fixtures.monoCard("dmc36-003", 7, 7000, FIRE);
    zeroCard = fixtures.monoCard("dmr08-021", 5, 2000, ZERO);
  }

  @Test
  void createsNewDecks() {
    var stub = deckService.createDeck(userId, "New Deck");
    assertThat(stub.name()).isEqualTo("New Deck");
    assertThat(stub.uniqueCardCount()).isZero();
  }

  @Test
  void cardsCanBeAddedToDeck() {
    var info = deckService.createDeck(userId, "New Deck");
    deckService.setCardAmount(userId, info.id(), lightCard.id(), 5);
    var result = deckService.getDeck(userId, info.id());
    assertThat(result).isNotEmpty();
    assertThat(result.get().info().uniqueCardCount()).isEqualTo(1);

    deckService.setCardAmount(userId, info.id(), rainbowCard.id(), 1);
    result = deckService.getDeck(userId, info.id());
    assertThat(result).isNotEmpty();
    assertThat(result.get().info().uniqueCardCount()).isEqualTo(2);
  }

  @Test
  void cardsCanBeRemoved() {
    var info = deckService.createDeck(userId, "New Deck");
    deckService.setCardAmount(userId, info.id(), rainbowCard.id(), 1);
    deckService.setCardAmount(userId, info.id(), lightCard.id(), 5);
    deckService.setCardAmount(userId, info.id(), lightCard.id(), 0);
    var result = deckService.getDeck(userId, info.id());
    assertThat(result).isPresent();
    assertThat(result.get().info().uniqueCardCount()).isEqualTo(1);
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
    assertThat(result.get().info().totalCardCount()).isEqualTo(5007);
    assertThat(result.get().info().uniqueCardCount()).isEqualTo(3);
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
    assertThat(importedDeck.get().uniqueCardCount()).isEqualTo(2);
    assertThat(importedDeck.get().totalCardCount()).isEqualTo(10);
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
    assertThat(decks.getFirst().uniqueCardCount()).isEqualTo(2);
    assertThat(decks.getFirst().totalCardCount()).isEqualTo(8);
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
