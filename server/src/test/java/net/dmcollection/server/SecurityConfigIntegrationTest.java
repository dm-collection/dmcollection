package net.dmcollection.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityConfigIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/",
        "/api/auth/status",
        "/index.html",
        "/favicon.ico",
        "/favicon.png",
        "/_app/env.js"
      })
  public void publicPathsGet(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isOk());
  }

  @Test
  public void privatePathsGet() throws Exception {
    mockMvc
        .perform(get("/card/dm01-001"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "auth/logout",
        "sets",
        "species",
        "rarities",
        "decks",
        "collection",
        "collection/export",
        "decks/export",
        "cards"
      })
  public void privateApiPathsGet(String path) throws Exception {
    mockMvc.perform(get("/api/" + path)).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {"auth/logout", "decks"})
  public void privateApiPathsPost(String path) throws Exception {
    mockMvc.perform(post("/api/" + path)).andExpect(status().isForbidden());
  }
}
