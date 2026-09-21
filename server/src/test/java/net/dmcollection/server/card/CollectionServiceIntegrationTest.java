package net.dmcollection.server.card;

import static net.dmcollection.server.card.Civilization.ZERO;
import static net.dmcollection.server.card.serialization.collection.V2Importer.HISTORY_LABEL_IMPORT;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_HISTORY_ENTRY;
import static net.dmcollection.server.testutils.SearchBuilder.search;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.card.serialization.collection.format.v1.V1CollectionCardExport;
import net.dmcollection.server.card.serialization.collection.format.v1.V1CollectionExport;
import net.dmcollection.server.card.serialization.collection.format.v2.V2CollectionExport;
import net.dmcollection.server.testutils.TestFixtureBuilder;
import net.dmcollection.server.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Transactional
class CollectionServiceIntegrationTest extends IntegrationTestBase {

  @Autowired CollectionService collectionService;
  @Autowired JsonMapper objectMapper;

  private TestFixtureBuilder fixtures;
  private User user;

  @BeforeEach
  void setup() {
    user = createUser("testuser");
    fixtures = new TestFixtureBuilder(dsl, cardTypeResolver, user);
  }

  @Test
  void stubReturnsEmptyMapForNewUser() {
    Map<Integer, Integer> stub = collectionService.getPrimaryStub(user.getId());
    assertThat(stub).isEmpty();
  }

  @Test
  void setAndGetSingleCardAmount() {
    PrintingStub card = fixtures.testCard("dm01-001").light().build();

    collectionService.setCardAmount(user.getId(), card.id(), 3);

    var result = collectionService.getSingleCardAmount(user.getId(), card.id());
    assertThat(result)
        .hasValueSatisfying(
            stub -> {
              assertThat(stub.cardId()).isEqualTo(card.id());
              assertThat(stub.amount()).isEqualTo(3);
            });
  }

  @Test
  void upsertUpdatesExistingEntry() {
    PrintingStub card = fixtures.testCard("dm01-001").light().build();

    collectionService.setCardAmount(user.getId(), card.id(), 2);
    collectionService.setCardAmount(user.getId(), card.id(), 5);

    var result = collectionService.getSingleCardAmount(user.getId(), card.id());
    assertThat(result).hasValueSatisfying(stub -> assertThat(stub.amount()).isEqualTo(5));
  }

  @Test
  void deleteEntryWhenAmountIsZero() {
    PrintingStub card = fixtures.testCard("dm01-001").light().build();

    collectionService.setCardAmount(user.getId(), card.id(), 3);
    collectionService.setCardAmount(user.getId(), card.id(), 0);

    var result = collectionService.getSingleCardAmount(user.getId(), card.id());
    assertThat(result).hasValueSatisfying(stub -> assertThat(stub.amount()).isZero());

    int rowCount =
        dsl.fetchCount(
            COLLECTION_ENTRY,
            COLLECTION_ENTRY
                .USER_ID
                .eq(user.getId())
                .and(COLLECTION_ENTRY.PRINTING_ID.eq(card.id())));
    assertThat(rowCount).isZero();
  }

  @Test
  void collectionCanBeFiltered() {
    PrintingStub lightCard = fixtures.testCard("dm01-001").light().build();
    PrintingStub fireCard =
        fixtures.testCard("dmc36-003").fire().cost(7).power(7000).creature().build();
    PrintingStub zeroCard = fixtures.testCard("dmr08-021").cost(5).power(2000).creature().build();

    collectionService.setCardAmount(user.getId(), lightCard.id(), 5);
    collectionService.setCardAmount(user.getId(), fireCard.id(), 28);
    collectionService.setCardAmount(user.getId(), zeroCard.id(), 5000);

    var result =
        collectionService.getPrimaryCollection(
            user.getId(), search(user).addIncludedCivs(ZERO).build());

    assertThat(result.cardPage().getContent())
        .hasSize(1)
        .allSatisfy(
            cardStub -> {
              assertThat(cardStub.printings().getFirst().officialId()).isEqualTo("dmr08-021");
              assertThat(cardStub.printings().getFirst().amount()).isEqualTo(5000);
            });
  }

  @Test
  void exportAndImportV2RoundTrip() {
    PrintingStub card1 = fixtures.testCard("dm01-001").light().build();
    PrintingStub card2 = fixtures.testCard("dm02-002").water().build();

    collectionService.setCardAmount(user.getId(), card1.id(), 3);
    collectionService.setCardAmount(user.getId(), card2.id(), 7);

    V2CollectionExport export = collectionService.exportCollection(user.getId());

    assertThat(export.version().version()).isEqualTo(2);
    assertThat(export.meta().countWithoutDuplicates()).isEqualTo(2);
    assertThat(export.meta().cardCount()).isEqualTo(10);
    assertThat(export.cards()).hasSize(2);

    // Import into a different user
    UUID otherUserId = createUser("other").getId();

    collectionService.importCollection(otherUserId, export);

    Map<Integer, Integer> otherStub = collectionService.getPrimaryStub(otherUserId);
    assertThat(otherStub).hasSize(2).containsEntry(card1.id(), 3).containsEntry(card2.id(), 7);
  }

  @Test
  void importV1isSupported() {
    PrintingStub card1 = fixtures.testCard("dm01-001").light().build();
    PrintingStub card2 = fixtures.testCard("dm02-002").water().build();
    PrintingStub card3 = fixtures.testCard("dm03-005").fire().build();

    collectionService.setCardAmount(user.getId(), card1.id(), 3);
    collectionService.setCardAmount(user.getId(), card2.id(), 7);
    List<V1CollectionCardExport> importCards =
        Arrays.asList(
            new V1CollectionCardExport("first card", card1.officialId(), 6),
            new V1CollectionCardExport("third card", card3.officialId(), 4));
    V1CollectionExport toImport =
        new V1CollectionExport(
            1, LocalDateTime.now().minusDays(1), "collection", 10, 2, importCards);
    collectionService.importCollection(user.getId(), toImport);

    Map<Integer, Integer> result = collectionService.getPrimaryStub(user.getId());
    assertThat(result)
        .containsEntry(card1.id(), 6)
        .containsEntry(card3.id(), 4)
        .doesNotContainKey(card2.id());
  }

  @Test
  void importWritesHistoryForChanges() {
    PrintingStub unchanged = fixtures.testCard("dm01-001").light().build();
    PrintingStub updated = fixtures.testCard("dm02-002").water().build();
    PrintingStub removed = fixtures.testCard("dm03-003").fire().build();
    PrintingStub added = fixtures.testCard("dm04-004").light().build();

    collectionService.setCardAmount(user.getId(), unchanged.id(), 2);
    collectionService.setCardAmount(user.getId(), updated.id(), 3);
    collectionService.setCardAmount(user.getId(), removed.id(), 4);

    dsl.deleteFrom(COLLECTION_HISTORY_ENTRY)
        .where(COLLECTION_HISTORY_ENTRY.USER_ID.eq(user.getId()))
        .execute();

    V1CollectionExport importData =
        new V1CollectionExport(
            2,
            LocalDateTime.now(),
            "collection",
            0,
            0,
            List.of(
                new V1CollectionCardExport("Unchanged", unchanged.officialId(), 2),
                new V1CollectionCardExport("Updated", updated.officialId(), 9),
                new V1CollectionCardExport("Added", added.officialId(), 5)));

    collectionService.importCollection(user.getId(), importData);

    var history =
        dsl.selectFrom(COLLECTION_HISTORY_ENTRY)
            .where(COLLECTION_HISTORY_ENTRY.USER_ID.eq(user.getId()))
            .fetch();

    assertThat(history)
        .hasSize(3)
        .anySatisfy(
            h -> {
              assertThat(h.getPrintingId()).isEqualTo(updated.id());
              assertThat(h.getPreviousQty()).isEqualTo(3);
              assertThat(h.getNewQty()).isEqualTo(9);
            })
        .anySatisfy(
            h -> {
              assertThat(h.getPrintingId()).isEqualTo(added.id());
              assertThat(h.getPreviousQty()).isZero();
              assertThat(h.getNewQty()).isEqualTo(5);
            })
        .anySatisfy(
            h -> {
              assertThat(h.getPrintingId()).isEqualTo(removed.id());
              assertThat(h.getPreviousQty()).isEqualTo(4);
              assertThat(h.getNewQty()).isZero();
            })
        .allSatisfy(h -> assertThat(h.getLabel()).isEqualTo(HISTORY_LABEL_IMPORT))
        .noneSatisfy(h -> assertThat(h.getPrintingId()).isEqualTo(unchanged.id()));
  }

  @Test
  void importClearsExistingCollection() {
    PrintingStub card1 = fixtures.testCard("dm01-001").light().build();
    PrintingStub card2 = fixtures.testCard("dm02-002").water().build();

    collectionService.setCardAmount(user.getId(), card1.id(), 10);

    V1CollectionExport importData =
        new V1CollectionExport(
            2,
            LocalDateTime.now(),
            "collection",
            5,
            1,
            List.of(new V1CollectionCardExport("Card 2", "dm02-002", 5)));

    collectionService.importCollection(user.getId(), importData);

    Map<Integer, Integer> stub = collectionService.getPrimaryStub(user.getId());
    assertThat(stub).hasSize(1).doesNotContainKey(card1.id()).containsEntry(card2.id(), 5);
  }

  @Test
  void historyEntryCreatedOnQuantityChange() {
    PrintingStub card = fixtures.testCard("dm01-001").light().build();

    collectionService.setCardAmount(user.getId(), card.id(), 3);
    collectionService.setCardAmount(user.getId(), card.id(), 5);

    var history =
        dsl.selectFrom(COLLECTION_HISTORY_ENTRY)
            .where(COLLECTION_HISTORY_ENTRY.USER_ID.eq(user.getId()))
            .and(COLLECTION_HISTORY_ENTRY.PRINTING_ID.eq(card.id()))
            .orderBy(COLLECTION_HISTORY_ENTRY.CHANGED_AT.asc())
            .fetch();

    assertThat(history).hasSize(2);
    assertThat(history.get(0).getPreviousQty()).isZero();
    assertThat(history.get(0).getNewQty()).isEqualTo(3);
    assertThat(history.get(1).getPreviousQty()).isEqualTo(3);
    assertThat(history.get(1).getNewQty()).isEqualTo(5);
  }
}
