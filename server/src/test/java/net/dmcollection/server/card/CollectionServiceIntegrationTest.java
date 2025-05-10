package net.dmcollection.server.card;

import static net.dmcollection.model.card.Civilization.DARK;
import static net.dmcollection.model.card.Civilization.FIRE;
import static net.dmcollection.model.card.Civilization.LIGHT;
import static net.dmcollection.model.card.Civilization.NATURE;
import static net.dmcollection.model.card.Civilization.WATER;
import static net.dmcollection.model.card.Civilization.ZERO;
import static net.dmcollection.server.TestUtils.D2_FIELD;
import static net.dmcollection.server.TestUtils.search;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.dmcollection.model.card.CardRepository;
import net.dmcollection.model.card.RarityCode;
import net.dmcollection.server.TestUtils;
import net.dmcollection.server.card.CollectionService.CollectionInfo;
import net.dmcollection.server.user.User;
import net.dmcollection.server.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CollectionServiceIntegrationTest {

  @Autowired CardRepository cardRepository;
  @Autowired CollectionService collectionService;
  @Autowired UserRepository userRepository;
  @Autowired JdbcTemplate jdbcTemplate;

  private User user;

  @BeforeEach
  void setup() {
    user = new User(null, "user");
    user.setPassword("password");
    user.setEnabled(true);
    user = userRepository.save(user);
    TestUtils utils = new TestUtils(jdbcTemplate);
    utils.monoCard("dm01-001", 6, LIGHT);
    utils.card(
        "dm24ex2-040",
        "DM24EX2 40/100",
        false,
        RarityCode.VR,
        350L,
        List.of("dm24ex2-040"),
        List.of(Set.of(LIGHT, WATER, DARK, FIRE, NATURE)),
        null,
        null,
        List.of(D2_FIELD));
    utils.monoCard("dmc36-003", 7, 7000, FIRE);
    utils.monoCard("dmr08-021", 5, 2000, ZERO);
  }

  @Test
  void createsNewCollections() {
    var stub = collectionService.createCollection(user.getId(), "New Collection");
    assertThat(stub.name()).isEqualTo("New Collection");
    assertThat(stub.uniqueCardCount()).isEqualTo(0);
  }

  @Test
  void createsNewCollectionAndFindsId() {
    var collection = collectionService.getPrimaryCollection(user.getId(), search().build());
    assertThat(collectionService.getPrimaryCollectionIds(user.getId()))
        .hasValueSatisfying(ids -> assertThat(ids.publicId()).isEqualTo(collection.info().id()));
  }

  @Test
  void idIsEmptyIfCollectionDoesNotExist() {
    assertThat(collectionService.getPrimaryCollectionIds(user.getId())).isEmpty();
  }

  @Test
  void cardsCanBeAddedToDeck() {
    var userId = user.getId();
    var info = collectionService.createCollection(userId, "New Collection");
    addToDeck(info, new CardIdAndAmount("dm01-001", 5));
    var result = collectionService.getCollection(userId, info.id());
    assertThat(result).isNotEmpty();
    assertThat(result.get().info().uniqueCardCount()).isEqualTo(1);
    addToDeck(info, new CardIdAndAmount("dm24ex2-040", 1));
    result = collectionService.getCollection(userId, info.id());
    assertThat(result).isNotEmpty();
    assertThat(result.get().info().uniqueCardCount()).isEqualTo(2);
  }

  @Test
  void cardsCanBeRemoved() {
    var userId = user.getId();
    var info = collectionService.createCollection(userId, "New Collection");
    List<CardIdAndAmount> testCards =
        Arrays.asList(
            new CardIdAndAmount("dm24ex2-040", 1),
            new CardIdAndAmount("dm01-001", 5),
            new CardIdAndAmount("dm01-001", 0));
    addToDeck(info, testCards);
    var result = collectionService.getCollection(userId, info.id());
    assertThat(result.get().info().uniqueCardCount()).isEqualTo(1);
  }

  @Test
  void cardsAreCounted() {
    var userId = user.getId();
    var info = collectionService.createCollection(userId, "New Collection");
    List<CardIdAndAmount> testCards =
        Arrays.asList(
            new CardIdAndAmount("dm24ex2-040", 2),
            new CardIdAndAmount("dm01-001", 5),
            new CardIdAndAmount("dmc36-003", 0),
            new CardIdAndAmount("dmr08-021", 5000));
    addToDeck(info, testCards);
    var result = collectionService.getCollection(userId, info.id());
    assertThat(result.get().info().totalCardCount()).isEqualTo(5007);
    assertThat(result.get().info().uniqueCardCount()).isEqualTo(3);
  }

  @Test
  void deckCanBeRetrieved() {
    var userId = user.getId();
    var collectionInfo = collectionService.createCollection(userId, "New Collection");
    List<CardIdAndAmount> testCards =
        Arrays.asList(
            new CardIdAndAmount("dm24ex2-040", 2),
            new CardIdAndAmount("dm01-001", 5),
            new CardIdAndAmount("dmc36-003", 28),
            new CardIdAndAmount("dmr08-021", 5000));
    addToDeck(collectionInfo, testCards);
    var result = collectionService.getCollection(userId, collectionInfo.id());
    assertThat(result).isNotEmpty();
    var collection = result.get();
    assertThat(
            collection.cardPage().getContent().stream()
                .map(ccard -> new CardIdAndAmount(ccard.dmId(), ccard.amount())))
        .containsExactlyInAnyOrderElementsOf(testCards);
  }

  @Test
  void collectionCanBeFiltered() {
    var userId = user.getId();
    List<CardIdAndAmount> testCards =
        Arrays.asList(
            new CardIdAndAmount("dm24ex2-040", 2),
            new CardIdAndAmount("dm01-001", 5),
            new CardIdAndAmount("dmc36-003", 28),
            new CardIdAndAmount("dmr08-021", 5000));
    addToCollection(userId, testCards);
    var result =
        collectionService.getPrimaryCollection(
            userId, TestUtils.search().addIncludedCivs(ZERO).build());
    assertThat(result.info().uniqueCardCount()).isEqualTo(1);
    assertThat(result.cardPage().getContent())
        .hasSize(1)
        .allSatisfy(
            cardStub -> {
              assertThat(cardStub.dmId()).isEqualTo("dmr08-021");
              assertThat(cardStub.amount()).isEqualTo(5000);
            });
  }

  private void addToDeck(CollectionInfo info, CardIdAndAmount card) {
    if (cardRepository.existsByOfficialId(card.cardId())) {
      long id = cardRepository.findByOfficialId(card.cardId()).get().id();
      collectionService.setCardAmount(info.ownerId(), info.id(), id, card.amount);
    } else {
      throw new IllegalStateException("Card with id " + card.cardId() + " missing in test db");
    }
  }

  private void addToDeck(CollectionInfo info, Collection<CardIdAndAmount> cards) {
    cards.forEach(card -> addToDeck(info, card));
  }

  private void addToCollection(UUID userId, Collection<CardIdAndAmount> cards) {
    cards.forEach(card -> addToCollection(userId, card));
  }

  private void addToCollection(UUID userId, CardIdAndAmount card) {
    if (cardRepository.existsByOfficialId(card.cardId())) {
      long id = cardRepository.findByOfficialId(card.cardId()).get().id();
      collectionService.setCardAmount(userId, id, card.amount);
    } else {
      throw new IllegalStateException("Card with id " + card.cardId() + " missing in test db");
    }
  }

  private record CardIdAndAmount(String cardId, int amount) {}
}
