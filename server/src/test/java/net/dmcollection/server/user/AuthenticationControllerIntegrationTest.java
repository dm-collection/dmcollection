package net.dmcollection.server.user;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;
import net.dmcollection.server.user.AuthenticationController.LoginRequest;
import net.dmcollection.server.user.AuthenticationController.RegistrationRequest;
import net.dmcollection.server.user.AuthenticationController.UsernameRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration
@TestPropertySource(properties = {"dmcollection.registrationCode=test123"})
public class AuthenticationControllerIntegrationTest {

  public static final String USERNAME = "existingUser";
  public static final String PASSWORD = "thatUsersSecretPassword";
  private static final Logger log =
      LoggerFactory.getLogger(AuthenticationControllerIntegrationTest.class);
  @Autowired UserRepository userRepository;
  @Autowired PasswordEncoder passwordEncoder;

  WebTestClient webTestClient;

  private final String testUsername = "testuser-webclient-" + System.currentTimeMillis();
  private final String testPassword = "password123";
  private final String testCode = "test123";

  private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
  private static final String SESSION_COOKIE_NAME = "JSESSIONID";
  private static final String CSRF_HEADER = "X-XSRF-TOKEN";
  private static final String REMEMBER_ME_COOKIE_NAME = "remember-me";

  private String csrfToken;
  private String sessionId;
  private String rememberMeToken;

  @LocalServerPort private int port;

  @BeforeEach
  void setup() {
    String baseUrl = "http://localhost:" + port;
    this.webTestClient = WebTestClient.bindToServer().baseUrl(baseUrl).build();
    csrfToken = null;
    sessionId = null;
    userRepository.deleteAll();
    User user = new User(null, USERNAME);
    user.setPassword(passwordEncoder.encode(PASSWORD));
    user.setEnabled(true);
    userRepository.save(user);
  }

  @Test
  void testLoginLogoutFlow() {
    // user requests landing page
    sendInitialGetRequest();
    // frontend gets auth status
    expectStatusUnauthenticated();
    expectAuthStatus(false, null);

    // frontend shows login form, sends request
    LoginRequest request = new LoginRequest(USERNAME, PASSWORD, false);

    EntityExchangeResult<byte[]> loginResponse =
        postJson("/api/auth/login", request)
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(APPLICATION_JSON)
            .expectCookie()
            .exists(SESSION_COOKIE_NAME)
            .expectCookie()
            .sameSite(SESSION_COOKIE_NAME, "Lax")
            .expectBody()
            .jsonPath("$.authenticated")
            .isEqualTo(true)
            .jsonPath("$.username")
            .isEqualTo(USERNAME)
            .returnResult();
    updateCookiesFromResponse(loginResponse);
    expectStatusAuthenticated(USERNAME);

    // access restricted endpoints
    getAuthenticated("/api/cards/0").expectStatus().isOk();
    getAuthenticated("/api/decks").expectStatus().isOk();

    // log out
    postLogout().expectStatus().isNoContent().expectCookie().valueEquals(SESSION_COOKIE_NAME, "");

    // cannot access restricted endpoints anymore with previously valid cookies
    getAuthenticated("/api/decks").expectStatus().isUnauthorized();
    expectStatusUnauthenticated();
  }

  @Test
  void testLoginRememberMeFlow() {
    // user requests landing page
    sendInitialGetRequest();
    // frontend gets auth status
    expectStatusUnauthenticated();
    expectAuthStatus(false, null);

    // frontend shows login form, sends request
    LoginRequest request = new LoginRequest(USERNAME, PASSWORD, true);

    EntityExchangeResult<byte[]> loginResponse =
        postJson("/api/auth/login", request)
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(APPLICATION_JSON)
            .expectCookie()
            .exists(SESSION_COOKIE_NAME)
            .expectCookie()
            .exists(REMEMBER_ME_COOKIE_NAME)
            .expectCookie()
            .sameSite(SESSION_COOKIE_NAME, "Lax")
            .expectBody()
            .jsonPath("$.authenticated")
            .isEqualTo(true)
            .jsonPath("$.username")
            .isEqualTo(USERNAME)
            .returnResult();
    updateCookiesFromResponse(loginResponse);

    // remember me cookie should be sufficient
    sessionId = null;
    csrfToken = null;
    expectStatusAuthenticated(USERNAME);

    // access restricted endpoints
    getAuthenticated("/api/cards/0").expectStatus().isOk();
    getAuthenticated("/api/decks").expectStatus().isOk();

    // log out
    EntityExchangeResult<byte[]> logoutResponse =
        postLogout()
            .expectStatus()
            .isNoContent()
            .expectCookie()
            .valueEquals(REMEMBER_ME_COOKIE_NAME, "")
            .expectBody()
            .returnResult();
    updateCookiesFromResponse(logoutResponse);
  }

  @Test
  void testRegistrationFlow() {
    // user requests some UI path
    EntityExchangeResult<byte[]> initialResponse =
        webTestClient
            .get()
            .uri("/cards")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult();
    updateCookiesFromResponse(initialResponse);
    // frontend gets auth status
    expectStatusUnauthenticated();

    // frontend shows registration form, sends request
    var request = new RegistrationRequest(testUsername, testPassword, testCode);
    EntityExchangeResult<byte[]> registrationResponse =
        postJson("/api/auth/register", request)
            .expectStatus()
            .isCreated()
            .expectHeader()
            .contentTypeCompatibleWith(APPLICATION_JSON)
            .expectCookie()
            .exists(SESSION_COOKIE_NAME)
            .expectCookie()
            .sameSite(SESSION_COOKIE_NAME, "Lax")
            .expectBody()
            .jsonPath("$.authenticated")
            .isEqualTo(true)
            .jsonPath("$.username")
            .isEqualTo(testUsername)
            .returnResult();
    updateCookiesFromResponse(registrationResponse);

    // new user should be logged in, access restricted pages
    getAuthenticated("/api/cards/0").expectStatus().isOk();
    getAuthenticated("/api/decks").expectStatus().isOk();
  }

  @Test
  void testUsernameExists() {
    sendInitialGetRequest();
    postJson("/api/auth/checkUsername", new UsernameRequest(USERNAME))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$")
        .isEqualTo(false);

    postJson("/api/auth/checkUsername", new UsernameRequest(USERNAME.toUpperCase()))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$")
        .isEqualTo(false);

    postJson("/api/auth/checkUsername", new UsernameRequest("newUserName"))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$")
        .isEqualTo(true);
  }

  @Test
  void testRegistrationWithExistingUsername() {
    sendInitialGetRequest();
    expectStatusUnauthenticated();

    // Attempt to register with the username that already exists
    var request = new RegistrationRequest(USERNAME, testPassword, testCode);
    postJson("/api/auth/register", request).expectStatus().isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void testRegistrationWithPasswordTooShort() {
    sendInitialGetRequest();
    expectStatusUnauthenticated();

    String shortPassword = "short";
    var request = new RegistrationRequest(testUsername, shortPassword, testCode);
    postJson("/api/auth/register", request).expectStatus().isBadRequest();
  }

  @Test
  void testRegistrationWithPasswordTooLong() {
    sendInitialGetRequest();
    expectStatusUnauthenticated();

    String longPassword = "a".repeat(AuthenticationController.PASSWORD_MAX_LENGTH + 1);

    var request = new RegistrationRequest(testUsername, longPassword, testCode);
    postJson("/api/auth/register", request).expectStatus().isBadRequest();
  }

  @Test
  void testRegistrationWithEmptyPassword() {
    sendInitialGetRequest();
    expectStatusUnauthenticated();

    String emptyPassword = "";
    var request = new RegistrationRequest(testUsername, emptyPassword, testCode);
    postJson("/api/auth/register", request).expectStatus().isBadRequest();
  }

  @Test
  void testRegistrationWithInvalidCode() {
    sendInitialGetRequest();
    expectStatusUnauthenticated();

    var request = new RegistrationRequest(testUsername, "newPassword", "wrongCode");
    postJson("/api/auth/register", request).expectStatus().isBadRequest();
  }

  @Test
  void testLoginWithWrongCredentials() {
    sendInitialGetRequest();

    LoginRequest wrongPasswordRequest = new LoginRequest(USERNAME, "wrongPassword", false);

    postJson("/api/auth/login", wrongPasswordRequest).expectStatus().isUnauthorized();

    LoginRequest nonExistentUserRequest = new LoginRequest("nonExistentUser", PASSWORD, false);

    postJson("/api/auth/login", nonExistentUserRequest).expectStatus().isUnauthorized();
  }

  @Test
  void testLoginMissingCsrfToken() {
    sendInitialGetRequest();
    csrfToken = null;

    LoginRequest request = new LoginRequest(USERNAME, PASSWORD, false);

    webTestClient
        .post()
        .uri("/api/auth/login")
        .cookies(this::setCookies)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void testRegistrationMissingCsrfToken() {
    sendInitialGetRequest();
    csrfToken = null; // no token

    RegistrationRequest request = new RegistrationRequest(testUsername, testPassword, null);

    webTestClient
        .post()
        .uri("/api/auth/register")
        .cookies(this::setCookies)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void testLogoutMissingCsrfToken() {
    sendInitialGetRequest();

    LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD, false);
    EntityExchangeResult<byte[]> loginResponse =
        postJson("/api/auth/login", loginRequest).expectStatus().isOk().expectBody().returnResult();
    updateCookiesFromResponse(loginResponse);

    this.csrfToken = null;

    webTestClient
        .post()
        .uri("/api/auth/logout")
        .cookies(this::setCookies)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  private void sendInitialGetRequest() {
    EntityExchangeResult<byte[]> initialResponse =
        webTestClient.get().uri("/").exchange().expectStatus().isOk().expectBody().returnResult();
    updateCookiesFromResponse(initialResponse);
  }

  private WebTestClient.ResponseSpec getAuthenticated(String uri) {
    return webTestClient.get().uri(uri).cookies(this::setCookies).exchange();
  }

  private <T> WebTestClient.ResponseSpec postJson(String uri, T body) {
    return webTestClient
        .post()
        .uri(uri)
        .cookies(this::setCookies)
        .contentType(APPLICATION_JSON)
        .header(CSRF_HEADER, csrfToken)
        .bodyValue(body)
        .exchange();
  }

  private WebTestClient.ResponseSpec postLogout() {
    return webTestClient
        .post()
        .uri("/api/auth/logout")
        .cookies(this::setCookies)
        .header(CSRF_HEADER, csrfToken)
        .exchange();
  }

  private void expectStatusAuthenticated(String expectedUsername) {
    expectAuthStatus(true, expectedUsername);
  }

  private void expectStatusUnauthenticated() {
    expectAuthStatus(false, null);
  }

  private void expectAuthStatus(boolean authenticated, String expectedUsernameOrEmpty) {
    WebTestClient.BodyContentSpec spec =
        webTestClient
            .get()
            .uri("/api/auth/status")
            .cookies(this::setCookies)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.authenticated")
            .isEqualTo(authenticated);

    if (authenticated) {
      spec.jsonPath("$.username").isEqualTo(expectedUsernameOrEmpty);
    } else {
      spec.jsonPath("$.username").isEmpty();
    }
    updateCookiesFromResponse(spec.returnResult());
  }

  private void setCookies(MultiValueMap<String, String> cookies) {
    if (csrfToken != null) {
      cookies.putIfAbsent(CSRF_COOKIE_NAME, new ArrayList<>(List.of(csrfToken)));
    }
    if (sessionId != null) {
      cookies.putIfAbsent(SESSION_COOKIE_NAME, new ArrayList<>(List.of(sessionId)));
    }
    if (rememberMeToken != null) {
      cookies.putIfAbsent(REMEMBER_ME_COOKIE_NAME, new ArrayList<>(List.of(rememberMeToken)));
    }
  }

  private <T> void updateCookiesFromResponse(EntityExchangeResult<T> response) {
    if (response.getResponseCookies().containsKey(CSRF_COOKIE_NAME)) {
      csrfToken = response.getResponseCookies().getFirst(CSRF_COOKIE_NAME).getValue();
      log.info("Got csrf token value {} from cookie", csrfToken);
    }
    if (response.getResponseCookies().containsKey(SESSION_COOKIE_NAME)) {
      sessionId = response.getResponseCookies().getFirst(SESSION_COOKIE_NAME).getValue();
      log.info("Got session id {} from cookie", sessionId);
    }
    if (response.getResponseCookies().containsKey(REMEMBER_ME_COOKIE_NAME)) {
      rememberMeToken = response.getResponseCookies().getFirst(REMEMBER_ME_COOKIE_NAME).getValue();
      log.info("Got remember me token {} from cookie", rememberMeToken);
    }
  }
}
