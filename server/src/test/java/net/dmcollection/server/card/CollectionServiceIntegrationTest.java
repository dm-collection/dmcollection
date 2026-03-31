package net.dmcollection.server.card;

import static net.dmcollection.server.card.Civilization.FIRE;
import static net.dmcollection.server.card.Civilization.LIGHT;
import static net.dmcollection.server.card.Civilization.WATER;
import static net.dmcollection.server.card.Civilization.ZERO;
import static net.dmcollection.server.jooq.generated.Tables.APP_USER;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_HISTORY_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.dmcollection.server.PostgresTestBase;
import net.dmcollection.server.TestFixtureBuilder;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.CollectionService.CollectionCardExport;
import net.dmcollection.server.card.CollectionService.CollectionExport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CollectionServiceIntegrationTest extends PostgresTestBase {

  @Autowired CollectionService collectionService;

  private TestFixtureBuilder fixtures;
  private UUID userId;

  @BeforeEach
  void setup() {
    fixtures = new TestFixtureBuilder(dsl);

    userId =
        dsl.insertInto(APP_USER)
            .set(APP_USER.USERNAME, "testuser-" + UUID.randomUUID())
            .set(APP_USER.PASSWORD_HASH, "$2a$10$test")
            .set(APP_USER.DISPLAY_NAME, "Test User")
            .returningResult(APP_USER.ID)
            .fetchOne()
            .value1();
  }

  @Test
  void stubReturnsEmptyMapForNewUser() {
    Map<Long, Integer> stub = collectionService.getPrimaryStub(userId);
    assertThat(stub).isEmpty();
  }

  @Test
  void setAndGetSingleCardAmount() {
    CardStub card = fixtures.monoCard("dm01-001", LIGHT);

    collectionService.setCardAmount(userId, card.id(), 3);

    var result = collectionService.getSingleCardAmount(userId, card.id());
    assertThat(result).hasValueSatisfying(stub -> {
      assertThat(stub.cardId()).isEqualTo(card.id());
      assertThat(stub.amount()).isEqualTo(3);
    });
  }

  @Test
  void upsertUpdatesExistingEntry() {
    CardStub card = fixtures.monoCard("dm01-001", LIGHT);

    collectionService.setCardAmount(userId, card.id(), 2);
    collectionService.setCardAmount(userId, card.id(), 5);

    var result = collectionService.getSingleCardAmount(userId, card.id());
    assertThat(result).hasValueSatisfying(stub -> assertThat(stub.amount()).isEqualTo(5));
  }

  @Test
  void deleteEntryWhenAmountIsZero() {
    CardStub card = fixtures.monoCard("dm01-001", LIGHT);

    collectionService.setCardAmount(userId, card.id(), 3);
    collectionService.setCardAmount(userId, card.id(), 0);

    var result = collectionService.getSingleCardAmount(userId, card.id());
    assertThat(result).hasValueSatisfying(stub -> assertThat(stub.amount()).isEqualTo(0));

    int rowCount =
        dsl.fetchCount(
            COLLECTION_ENTRY,
            COLLECTION_ENTRY.USER_ID.eq(userId)
                .and(COLLECTION_ENTRY.PRINTING_ID.eq(card.id().intValue())));
    assertThat(rowCount).isZero();
  }

  @Test
  void collectionCanBeFiltered() {
    CardStub lightCard = fixtures.monoCard("dm01-001", LIGHT);
    CardStub fireCard = fixtures.monoCard("dmc36-003", 7, 7000, FIRE);
    CardStub zeroCard = fixtures.monoCard("dmr08-021", 5, 2000, ZERO);

    collectionService.setCardAmount(userId, lightCard.id(), 5);
    collectionService.setCardAmount(userId, fireCard.id(), 28);
    collectionService.setCardAmount(userId, zeroCard.id(), 5000);

    var result =
        collectionService.getPrimaryCollection(
            userId, TestFixtureBuilder.search().addIncludedCivs(ZERO).build());

    assertThat(result.info().uniqueCardCount()).isEqualTo(1);
    assertThat(result.cardPage().getContent())
        .hasSize(1)
        .allSatisfy(cardStub -> {
          assertThat(cardStub.dmId()).isEqualTo("dmr08-021");
          assertThat(cardStub.amount()).isEqualTo(5000);
        });
  }

  @Test
  void exportAndImportV2RoundTrip() {
    CardStub card1 = fixtures.monoCard("dm01-001", LIGHT);
    CardStub card2 = fixtures.monoCard("dm02-002", WATER);

    collectionService.setCardAmount(userId, card1.id(), 3);
    collectionService.setCardAmount(userId, card2.id(), 7);

    CollectionExport export = collectionService.exportPrimaryCollection(userId);

    assertThat(export.version()).isEqualTo(2);
    assertThat(export.countWithoutDuplicates()).isEqualTo(2);
    assertThat(export.cardCount()).isEqualTo(10);
    assertThat(export.cards()).hasSize(2);

    // Import into a different user
    UUID otherUserId =
        dsl.insertInto(APP_USER)
            .set(APP_USER.USERNAME, "other-" + UUID.randomUUID())
            .set(APP_USER.PASSWORD_HASH, "$2a$10$test")
            .set(APP_USER.DISPLAY_NAME, "Other User")
            .returningResult(APP_USER.ID)
            .fetchOne()
            .value1();

    collectionService.importPrimaryCollection(otherUserId, export);

    Map<Long, Integer> otherStub = collectionService.getPrimaryStub(otherUserId);
    assertThat(otherStub).hasSize(2);
    assertThat(otherStub.get(card1.id())).isEqualTo(3);
    assertThat(otherStub.get(card2.id())).isEqualTo(7);
  }

  @Test
  void importV1MatchesByShortName() {
    CardStub card = fixtures.monoCard("dm01-001", LIGHT);

    // Simulate a v1 export with the old Id field (ignored) and shortName for matching
    // v1 CollectionCardExport had: long Id, String name, String shortName, int amount
    // Our new record doesn't have Id, but Jackson ignores unknown fields on import
    // We construct a CollectionExport with the v2 record shape using shortName for matching
    CollectionExport v1Export =
        new CollectionExport(
            1,
            LocalDateTime.now(),
            "collection",
            4,
            1,
            List.of(new CollectionCardExport("Test Card", "dm01-001", 4)));

    collectionService.importPrimaryCollection(userId, v1Export);

    Map<Long, Integer> stub = collectionService.getPrimaryStub(userId);
    assertThat(stub).containsEntry(card.id(), 4);
  }

  @Test
  void importClearsExistingCollection() {
    CardStub card1 = fixtures.monoCard("dm01-001", LIGHT);
    CardStub card2 = fixtures.monoCard("dm02-002", WATER);

    collectionService.setCardAmount(userId, card1.id(), 10);

    CollectionExport importData =
        new CollectionExport(
            2,
            LocalDateTime.now(),
            "collection",
            5,
            1,
            List.of(new CollectionCardExport("Card 2", "dm02-002", 5)));

    collectionService.importPrimaryCollection(userId, importData);

    Map<Long, Integer> stub = collectionService.getPrimaryStub(userId);
    assertThat(stub).hasSize(1);
    assertThat(stub).doesNotContainKey(card1.id());
    assertThat(stub).containsEntry(card2.id(), 5);
  }

  @Test
  void historyEntryCreatedOnQuantityChange() {
    CardStub card = fixtures.monoCard("dm01-001", LIGHT);

    collectionService.setCardAmount(userId, card.id(), 3);
    collectionService.setCardAmount(userId, card.id(), 5);

    var history =
        dsl.selectFrom(COLLECTION_HISTORY_ENTRY)
            .where(COLLECTION_HISTORY_ENTRY.USER_ID.eq(userId))
            .and(COLLECTION_HISTORY_ENTRY.PRINTING_ID.eq(card.id().intValue()))
            .orderBy(COLLECTION_HISTORY_ENTRY.CHANGED_AT.asc())
            .fetch();

    assertThat(history).hasSize(2);
    assertThat(history.get(0).getPreviousQty()).isEqualTo(0);
    assertThat(history.get(0).getNewQty()).isEqualTo(3);
    assertThat(history.get(1).getPreviousQty()).isEqualTo(3);
    assertThat(history.get(1).getNewQty()).isEqualTo(5);
  }
}
