package net.dmcollection.server.card;

import static net.dmcollection.server.jooq.generated.Tables.APP_USER;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import net.dmcollection.server.PostgresTestBase;
import net.dmcollection.server.TestFixtureBuilder;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.internal.query.CardTypeResolver;
import net.dmcollection.server.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class CardControllerIntegrationTest extends PostgresTestBase {

  @Autowired MockMvc mockMvc;
  @Autowired CardTypeResolver cardTypeResolver;

  TestFixtureBuilder fixtures;
  User testUser;

  @BeforeEach
  void setup() {
    fixtures = new TestFixtureBuilder(dsl, cardTypeResolver);

    var user =
        dsl.insertInto(APP_USER)
            .set(APP_USER.USERNAME, "cardtest-" + UUID.randomUUID())
            .set(APP_USER.PASSWORD_HASH, "$2a$10$test")
            .set(APP_USER.DISPLAY_NAME, "Test User")
            .returning()
            .fetchOne();
    testUser =
        new User(
            user.get(APP_USER.ID),
            user.get(APP_USER.USERNAME),
            user.get(APP_USER.PASSWORD_HASH),
            user.get(APP_USER.DISPLAY_NAME),
            user.get(APP_USER.AVATAR_PATH),
            user.get(APP_USER.IS_ADMIN));
  }

  @Test
  void getCardsReturnsPageOfCards() throws Exception {
    fixtures.monoCard("ctrl-card-1", Civilization.LIGHT);
    fixtures.monoCard("ctrl-card-2", Civilization.WATER);

    mockMvc
        .perform(get("/api/cards/0").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  void getCardsReturnsEmptyPageWhenNoCards() throws Exception {
    mockMvc
        .perform(get("/api/cards/0").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void getCardsRespectsPageSize() throws Exception {
    fixtures.monoCard("ctrl-page-1", Civilization.LIGHT);
    fixtures.monoCard("ctrl-page-2", Civilization.WATER);
    fixtures.monoCard("ctrl-page-3", Civilization.DARK);

    mockMvc
        .perform(get("/api/cards/0").param("pageSize", "2").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.page.totalElements").value(3))
        .andExpect(jsonPath("$.page.totalPages").value(2));
  }

  @Test
  void getCardsFiltersByName() throws Exception {
    fixtures.monoCard("ctrl-alpha", Civilization.LIGHT);
    fixtures.monoCard("ctrl-beta", Civilization.WATER);

    mockMvc
        .perform(get("/api/cards/0").param("name", "alpha").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].dmId").value("ctrl-alpha"));
  }

  @Test
  void getCardsRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/cards/0")).andExpect(status().isUnauthorized());
  }

  @Test
  void getCardReturnsCardWhenFound() throws Exception {
    fixtures.monoCard("ctrl-detail-1", Civilization.LIGHT);

    mockMvc
        .perform(get("/api/card/ctrl-detail-1").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dmId").value("ctrl-detail-1"))
        .andExpect(jsonPath("$.facets").isArray())
        .andExpect(jsonPath("$.facets.length()").value(1))
        .andExpect(jsonPath("$.facets[0].civilizations[0]").value("光"));
  }

  @Test
  void getCardReturns404WhenNotFound() throws Exception {
    mockMvc
        .perform(get("/api/card/nonexistent").with(user(testUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getCardRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/card/anything")).andExpect(status().isUnauthorized());
  }

  @Test
  void getCardsByIdReturnsMatchingCards() throws Exception {
    CardStub card1 = fixtures.monoCard("ctrl-byid-1", Civilization.LIGHT);
    CardStub card2 = fixtures.monoCard("ctrl-byid-2", Civilization.WATER);

    mockMvc
        .perform(
            get("/api/cards")
                .param("cardIds", String.valueOf(card1.id()), String.valueOf(card2.id()))
                .with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void getCardsByIdReturns404WhenNoneFound() throws Exception {
    mockMvc
        .perform(get("/api/cards").param("cardIds", "99999").with(user(testUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getCardsByIdRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/cards").param("cardIds", "1")).andExpect(status().isUnauthorized());
  }
}
