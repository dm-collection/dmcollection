package net.dmcollection.server;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_RANGES;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.CONTENT_RANGE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_NONE_MATCH;
import static org.springframework.http.HttpHeaders.RANGE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = {"dmcollection.image-service.enabled=true"})
class ImageServiceControllerIntegrationTest extends IntegrationTestBase {

  private static final WireMockServer wireMock =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    wireMock.start();
  }

  @DynamicPropertySource
  static void configureImageServiceUrl(DynamicPropertyRegistry registry) {
    registry.add("dmcollection.image-service.base-url", wireMock::baseUrl);
  }

  @Autowired MockMvc mockMvc;

  @BeforeEach
  void resetWireMock() {
    wireMock.resetAll();
  }

  @Test
  void getRequestReturnsBody() throws Exception {
    byte[] imageBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    wireMock.stubFor(
        WireMock.get(urlEqualTo("/cards/001.jpg"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, "image/jpeg")
                    .withBody(imageBytes)));

    mockMvc
        .perform(get("/image/cards/001.jpg").with(user("test")))
        .andExpect(status().isOk())
        .andExpect(header().string(CONTENT_TYPE, "image/jpeg"))
        .andExpect(content().bytes(imageBytes));
  }

  @Test
  void headRequestReturnsNoBody() throws Exception {
    wireMock.stubFor(
        WireMock.head(urlEqualTo("/cards/001.jpg"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, "image/jpeg")
                    .withHeader(ETAG, "\"abc123\"")));

    mockMvc
        .perform(head("/image/cards/001.jpg").with(user("test")))
        .andExpect(status().isOk())
        .andExpect(header().string(CONTENT_TYPE, "image/jpeg"))
        .andExpect(header().string(ETAG, "\"abc123\""))
        .andExpect(content().bytes(new byte[0]));
  }

  @Test
  void requestHeadersForwarded() throws Exception {
    wireMock.stubFor(
        WireMock.get(urlEqualTo("/cards/001.jpg")).willReturn(aResponse().withStatus(200)));

    mockMvc.perform(
        get("/image/cards/001.jpg")
            .with(user("test"))
            .header(ACCEPT, "image/webp")
            .header(IF_NONE_MATCH, "\"abc123\"")
            .header(RANGE, "bytes=0-1023"));

    wireMock.verify(
        getRequestedFor(urlEqualTo("/cards/001.jpg"))
            .withHeader(ACCEPT, equalTo("image/webp"))
            .withHeader(IF_NONE_MATCH, equalTo("\"abc123\""))
            .withHeader(RANGE, equalTo("bytes=0-1023")));
  }

  @Test
  void responseHeadersForwarded() throws Exception {
    wireMock.stubFor(
        WireMock.get(urlEqualTo("/cards/001.jpg"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, "image/png")
                    .withHeader(ETAG, "\"etag-value\"")
                    .withHeader(CACHE_CONTROL, "max-age=3600")
                    .withHeader(ACCEPT_RANGES, "bytes")
                    .withHeader(CONTENT_RANGE, "bytes 0-99/200")));

    mockMvc
        .perform(get("/image/cards/001.jpg").with(user("test")))
        .andExpect(status().isOk())
        .andExpect(header().string(CONTENT_TYPE, "image/png"))
        .andExpect(header().string(ETAG, "\"etag-value\""))
        .andExpect(header().string(CACHE_CONTROL, "max-age=3600"))
        .andExpect(header().string(ACCEPT_RANGES, "bytes"))
        .andExpect(header().string(CONTENT_RANGE, "bytes 0-99/200"));
  }

  @Test
  void nonForwardedResponseHeadersExcluded() throws Exception {
    wireMock.stubFor(
        WireMock.get(urlEqualTo("/cards/001.jpg"))
            .willReturn(
                aResponse().withStatus(200).withHeader("X-Custom-Header", "should-not-appear")));

    mockMvc
        .perform(get("/image/cards/001.jpg").with(user("test")))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("X-Custom-Header"));
  }

  @Test
  void conditionalGetPassesThrough304() throws Exception {
    wireMock.stubFor(
        WireMock.get(urlEqualTo("/cards/001.jpg"))
            .withHeader(IF_NONE_MATCH, equalTo("\"abc123\""))
            .willReturn(aResponse().withStatus(304)));

    mockMvc
        .perform(get("/image/cards/001.jpg").with(user("test")).header(IF_NONE_MATCH, "\"abc123\""))
        .andExpect(status().isNotModified());
  }

  @Test
  void dotDotPathRejectedWith400() throws Exception {
    mockMvc.perform(get("/image/../etc/passwd")).andExpect(status().isBadRequest());

    wireMock.verify(0, getRequestedFor(urlEqualTo("/../etc/passwd")));
  }

  @Test
  void upstreamUnavailableReturns502() throws Exception {
    wireMock.stop();
    try {
      mockMvc
          .perform(get("/image/cards/001.jpg").with(user("test")))
          .andExpect(status().isBadGateway());
    } finally {
      wireMock.start();
    }
  }
}
