package net.dmcollection.server.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.TestFixtureBuilder;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
@Transactional
class DeckControllerIntegrationTest extends IntegrationTestBase {
  @Autowired private MockMvcTester mockMvc;

  TestFixtureBuilder fixtures;
  User testUser;
  JsonMapper mapper = new JsonMapper();

  @BeforeEach
  void setup() {
    fixtures = new TestFixtureBuilder(dsl, cardTypeResolver);
    testUser = createUser("deckTest-");
  }

  @Test
  void exportAndImportWorks() throws UnsupportedEncodingException {
    CardStub granGure = fixtures.monoCard("dm01-001", Civilization.LIGHT);
    CardStub bolshack = fixtures.monoCard("dm01-008", Civilization.FIRE);
    CardStub silphy = fixtures.monoCard("dm001-005", Civilization.DARK);
    var deck1 = createDeck("test-1");
    addCardToDeck(deck1, granGure.id(), 4);
    addCardToDeck(deck1, bolshack.id(), 3);

    var deck2 = createDeck("test-2");
    addCardToDeck(deck2, silphy.id(), 2);
    addCardToDeck(deck2, bolshack.id(), 1);

    assertThat(getDecks(testUser)).bodyJson().extractingPath("$").asArray().hasSize(2);

    var exportResult =
        mockMvc.get().with(user(testUser)).with(csrf()).uri("/api/decks/export").exchange();
    assertThat(exportResult).hasStatusOk();
    var exported = exportResult.getResponse().getContentAsByteArray();

    var newUser = createUser("deckTest-");
    assertThat(getDecks(newUser)).hasStatusOk().bodyJson().extractingPath("$").asArray().isEmpty();

    assertThat(
            mockMvc
                .post()
                .with(user(newUser))
                .with(csrf())
                .uri("/api/decks/import")
                .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .content(exported))
        .hasStatusOk();
    assertThat(getDecks(newUser)).bodyJson().extractingPath("$").asArray().hasSize(2);
  }

  private MvcTestResult getDecks(User user) {
    return mockMvc.get().with(user(user)).uri("/api/decks").exchange();
  }

  private void addCardToDeck(UUID deckId, long cardId, int amount) {
    String body = "{\"amount\": " + amount + "}";
    assertThat(
            mockMvc
                .put()
                .with(user(testUser))
                .with(csrf())
                .uri("/api/deck/{id}/cards/{cardId}", deckId, cardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .hasStatusOk();
  }

  private UUID createDeck(String name) throws UnsupportedEncodingException {
    String newDeckName = "{\"name\": \"" + name + "\"}";
    var newDeckResult =
        mockMvc
            .post()
            .with(user(testUser))
            .with(csrf())
            .uri("/api/decks")
            .content(newDeckName)
            .contentType(MediaType.APPLICATION_JSON)
            .exchange();
    assertThat(newDeckResult).hasStatus(HttpStatus.CREATED);
    DeckService.DeckInfo info =
        mapper
            .readerFor(DeckService.DeckInfo.class)
            .readValue(newDeckResult.getResponse().getContentAsString());
    return info.id();
  }
}
