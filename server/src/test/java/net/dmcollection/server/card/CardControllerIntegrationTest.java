package net.dmcollection.server.card;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.testutils.TestFixtureBuilder;
import net.dmcollection.server.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class CardControllerIntegrationTest extends IntegrationTestBase {

  @Autowired MockMvc mockMvc;

  TestFixtureBuilder fixtures;
  User testUser;

  @BeforeEach
  void setup() {
    fixtures = new TestFixtureBuilder(dsl, cardTypeResolver, testUser);
    testUser = createUser("cardtest");
  }

  @Test
  void getCardsReturnsPageOfCards() throws Exception {
    fixtures.testCard("ctrl-card-1").light().build();
    fixtures.testCard("ctrl-card-2").water().build();

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
    fixtures.testCard("ctrl-page-1").light().build();
    fixtures.testCard("ctrl-page-2").water().build();
    fixtures.testCard("ctrl-page-3").dark().build();

    mockMvc
        .perform(get("/api/cards/0").param("pageSize", "2").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.page.totalElements").value(3))
        .andExpect(jsonPath("$.page.totalPages").value(2));
  }

  @Test
  void getCardsFiltersByName() throws Exception {
    fixtures.testCard("ctrl-alpha").light().build();
    fixtures.testCard("ctrl-beta").water().build();

    mockMvc
        .perform(get("/api/cards/0").param("name", "alpha").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].printings[0].officialId").value("ctrl-alpha"));
  }

  @Test
  void getCardsRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/cards/0")).andExpect(status().isUnauthorized());
  }

  @Test
  void getCardReturnsCardWhenFound() throws Exception {
    fixtures.testCard("ctrl-detail-1").light().build();

    mockMvc
        .perform(get("/api/card/ctrl-detail-1").with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dmId").value("ctrl-detail-1"))
        .andExpect(jsonPath("$.facets").isArray())
        .andExpect(jsonPath("$.facets.length()").value(1))
        .andExpect(jsonPath("$.facets[0].civilizations[0]").value("光"));
  }

  @Test
  void getCardIncludesDeckZone() throws Exception {
    var expected = fixtures.createFourSides();

    mockMvc
        .perform(get("/api/card/" + expected.officialId()).with(user(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.zone").value("hyperspatial"));
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
}
