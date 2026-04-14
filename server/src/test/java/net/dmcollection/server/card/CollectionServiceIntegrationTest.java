package net.dmcollection.server.card;

import static net.dmcollection.server.card.Civilization.FIRE;
import static net.dmcollection.server.card.Civilization.LIGHT;
import static net.dmcollection.server.card.Civilization.WATER;
import static net.dmcollection.server.card.Civilization.ZERO;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_HISTORY_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.TestFixtureBuilder;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.serialization.format.v1.V1CollectionCardExport;
import net.dmcollection.server.card.serialization.format.v1.V1CollectionExport;
import net.dmcollection.server.card.serialization.format.v2.V2CollectionExport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CollectionServiceIntegrationTest extends IntegrationTestBase {

  @Autowired CollectionService collectionService;
  @Autowired ObjectMapper objectMapper;

  private TestFixtureBuilder fixtures;
  private UUID userId;

  @BeforeEach
  void setup() {
    fixtures = new TestFixtureBuilder(dsl, cardTypeResolver);

    userId = createUser("testuser").getId();
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
    assertThat(result)
        .hasValueSatisfying(
            stub -> {
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
    assertThat(result).hasValueSatisfying(stub -> assertThat(stub.amount()).isZero());

    int rowCount =
        dsl.fetchCount(
            COLLECTION_ENTRY,
            COLLECTION_ENTRY
                .USER_ID
                .eq(userId)
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
        .allSatisfy(
            cardStub -> {
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

    V2CollectionExport export = collectionService.exportCollection(userId);

    assertThat(export.version().version()).isEqualTo(2);
    assertThat(export.meta().countWithoutDuplicates()).isEqualTo(2);
    assertThat(export.meta().cardCount()).isEqualTo(10);
    assertThat(export.cards()).hasSize(2);

    // Import into a different user
    UUID otherUserId = createUser("other").getId();

    collectionService.importCollection(otherUserId, export);

    Map<Long, Integer> otherStub = collectionService.getPrimaryStub(otherUserId);
    assertThat(otherStub).hasSize(2).containsEntry(card1.id(), 3).containsEntry(card2.id(), 7);
  }

  @Test
  void importV1isSupported() throws IOException {
    V1CollectionExport data;
    try (InputStream is = getClass().getResourceAsStream("/v1-collection-export-sample.json")) {
      data = objectMapper.readValue(is, V1CollectionExport.class);
    }
    collectionService.importCollection(userId, data);

    // TODO: assert collection is imported
  }

  @Test
  void importClearsExistingCollection() {
    CardStub card1 = fixtures.monoCard("dm01-001", LIGHT);
    CardStub card2 = fixtures.monoCard("dm02-002", WATER);

    collectionService.setCardAmount(userId, card1.id(), 10);

    V1CollectionExport importData =
        new V1CollectionExport(
            2,
            LocalDateTime.now(),
            "collection",
            5,
            1,
            List.of(new V1CollectionCardExport("Card 2", "dm02-002", 5)));

    collectionService.importCollection(userId, importData);

    Map<Long, Integer> stub = collectionService.getPrimaryStub(userId);
    assertThat(stub).hasSize(1).doesNotContainKey(card1.id()).containsEntry(card2.id(), 5);
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
    assertThat(history.get(0).getPreviousQty()).isZero();
    assertThat(history.get(0).getNewQty()).isEqualTo(3);
    assertThat(history.get(1).getPreviousQty()).isEqualTo(3);
    assertThat(history.get(1).getNewQty()).isEqualTo(5);
  }
}
