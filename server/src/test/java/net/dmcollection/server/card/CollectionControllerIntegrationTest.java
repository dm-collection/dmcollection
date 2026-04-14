package net.dmcollection.server.card;

import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_HISTORY_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.TestFixtureBuilder;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.serialization.collection.format.v2.V2CollectionExport;
import net.dmcollection.server.jooq.generated.tables.records.CollectionHistoryEntryRecord;
import net.dmcollection.server.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class CollectionControllerIntegrationTest extends IntegrationTestBase {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  TestFixtureBuilder fixtures;
  User testUser;

  @BeforeEach
  void setup() {
    fixtures = new TestFixtureBuilder(dsl, cardTypeResolver);

    testUser = createUser("collTest-");
  }

  private List<CollectionHistoryEntryRecord> fetchHistory(UUID userId, long printingId) {
    return dsl.selectFrom(COLLECTION_HISTORY_ENTRY)
        .where(COLLECTION_HISTORY_ENTRY.USER_ID.eq(userId))
        .and(COLLECTION_HISTORY_ENTRY.PRINTING_ID.eq((int) printingId))
        .orderBy(COLLECTION_HISTORY_ENTRY.CHANGED_AT.asc())
        .fetch();
  }

  @Test
  void getCollectionRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/collection")).andExpect(status().isUnauthorized());
  }

  @Test
  void putCollectionCardRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            put("/api/collection/cards/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getCollectionReturnsEmptyForNewUser() throws Exception {
    mockMvc
        .perform(get("/api/collection").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.uniqueCardCount").value(0))
        .andExpect(jsonPath("$.info.totalCardCount").value(0))
        .andExpect(jsonPath("$.info.ownerId").value(testUser.getId().toString()));
  }

  @Test
  void getCollectionReturnsCardsAfterSettingAmount() throws Exception {
    CardStub card = fixtures.monoCard("coll-ctrl-1", Civilization.LIGHT);

    putRequest("/api/collection/cards/" + card.id(), "{\"amount\":3}");

    mockMvc
        .perform(get("/api/collection/0").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.uniqueCardCount").value(1))
        .andExpect(jsonPath("$.info.totalCardCount").value(3))
        .andExpect(jsonPath("$.cardPage.content.length()").value(1));
  }

  @Test
  void setCardAmountReturnsCollectionInfo() throws Exception {
    CardStub card = fixtures.monoCard("coll-ctrl-2", Civilization.WATER);

    putRequest("/api/collection/cards/" + card.id(), "{\"amount\":5}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uniqueCardCount").value(1))
        .andExpect(jsonPath("$.totalCardCount").value(5));
  }

  @Test
  void setCardAmountReturns404ForUnknownCard() throws Exception {
    putRequest("/api/collection/cards/999999", "{\"amount\":1}").andExpect(status().isNotFound());
  }

  @Test
  void setSingleCardAmountReturnsStub() throws Exception {
    CardStub card = fixtures.monoCard("coll-ctrl-3", Civilization.FIRE);

    putRequest("/api/collectionStub/cards/" + card.id(), "{\"amount\":4}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cardId").value(card.id()))
        .andExpect(jsonPath("$.amount").value(4));
  }

  @Test
  void setCardAmountOnStubReturnsMap() throws Exception {
    CardStub card = fixtures.monoCard("coll-ctrl-4", Civilization.DARK);

    putRequest("/api/collectionStub", "{\"cardId\":" + card.id() + ",\"amount\":2}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$." + card.id()).value(2));
  }

  @Test
  void exportReturnsJsonFile() throws Exception {
    CardStub card = fixtures.monoCard("coll-ctrl-5", Civilization.NATURE);

    putRequest("/api/collection/cards/" + card.id(), "{\"amount\":3}");

    byte[] responseBytes =
        mockMvc
            .perform(get("/api/collection/export").with(user(testUser)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.containsString("attachment"),
                            org.hamcrest.Matchers.containsString(".json"))))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    V2CollectionExport export = objectMapper.readValue(responseBytes, V2CollectionExport.class);
    assertThat(export.version().version()).isEqualTo(2);
    assertThat(export.cards()).hasSize(1);
    assertThat(export.cards().getFirst().prints().getFirst().amount()).isEqualTo(3);
  }

  @Test
  void importAndExportRoundTripThroughHttp() throws Exception {
    CardStub card1 = fixtures.monoCard("coll-ctrl-6a", Civilization.LIGHT);
    CardStub card2 = fixtures.monoCard("coll-ctrl-6b", Civilization.WATER);

    putRequest("/api/collection/cards/" + card1.id(), "{\"amount\":3}");
    putRequest("/api/collection/cards/" + card2.id(), "{\"amount\":7}");

    byte[] exportBytes =
        mockMvc
            .perform(get("/api/collection/export").with(user(testUser)))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    User otherUser = createUser("other");

    mockMvc
        .perform(
            post("/api/collection/import")
                .with(user(otherUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(exportBytes))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/collection/0").with(user(otherUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.uniqueCardCount").value(2))
        .andExpect(jsonPath("$.info.totalCardCount").value(10));
  }

  @Test
  void historyCreatedWhenSettingCardAmount() throws Exception {
    CardStub card = fixtures.monoCard("coll-hist-1", Civilization.LIGHT);

    putRequest("/api/collection/cards/" + card.id(), "{\"amount\":3}");

    putRequest("/api/collection/cards/" + card.id(), "{\"amount\":7}");

    var history = fetchHistory(testUser.getId(), card.id());
    assertThat(history).hasSize(2);
    assertThat(history.get(0).getPreviousQty()).isZero();
    assertThat(history.get(0).getNewQty()).isEqualTo(3);
    assertThat(history.get(1).getPreviousQty()).isEqualTo(3);
    assertThat(history.get(1).getNewQty()).isEqualTo(7);
  }

  @Test
  void historyCreatedViaStubEndpoints() throws Exception {
    CardStub card = fixtures.monoCard("coll-hist-2", Civilization.WATER);

    putRequest("/api/collectionStub/cards/" + card.id(), "{\"amount\":2}");

    putRequest("/api/collectionStub", "{\"cardId\":" + card.id() + ",\"amount\":5}");

    var history = fetchHistory(testUser.getId(), card.id());
    assertThat(history).hasSize(2);
    assertThat(history.get(0).getPreviousQty()).isZero();
    assertThat(history.get(0).getNewQty()).isEqualTo(2);
    assertThat(history.get(1).getPreviousQty()).isEqualTo(2);
    assertThat(history.get(1).getNewQty()).isEqualTo(5);
  }

  @Test
  void importRejectsUnknownFormat() throws Exception {
    mockMvc
        .perform(
            post("/api/collection/import")
                .with(user(testUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content("{\"version\":\"garbage\"}".getBytes()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void importDoesNotCreateHistoryEntries() throws Exception {
    CardStub card = fixtures.monoCard("coll-hist-3", Civilization.FIRE);

    putRequest("/api/collection/cards/" + card.id(), "{\"amount\":3}");

    byte[] exportBytes =
        mockMvc
            .perform(get("/api/collection/export").with(user(testUser)))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    mockMvc.perform(
        post("/api/collection/import")
            .with(user(testUser))
            .with(csrf())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .content(exportBytes));

    var history = fetchHistory(testUser.getId(), card.id());
    assertThat(history).hasSize(1);
  }

  private ResultActions putRequest(String url, String body) throws Exception {
    return mockMvc.perform(
        put(url)
            .with(user(testUser))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }
}
