package net.dmcollection.server.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import net.dmcollection.server.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthenticationController {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);
  private final AuthenticationManager authenticationManager;
  private final UserService userService;
  private final AbstractRememberMeServices rememberMeServices;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();
  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();
  private final AppProperties appProperties;

  /**
   * Length limit of BCrypt used by default Spring Security {@link
   * org.springframework.security.crypto.password.DelegatingPasswordEncoder}. Could be adjusted if
   * the default (see {@link PasswordEncoderFactories#createDelegatingPasswordEncoder()}) encoding
   * algorithm changes.
   */
  static final int PASSWORD_MAX_LENGTH = 72;

  static final int PASSWORD_MIN_LENGTH = 8;

  public AuthenticationController(
      AuthenticationManager authenticationManager,
      UserService userService,
      AppProperties appProperties,
      AbstractRememberMeServices rememberMeServices) {
    this.authenticationManager = authenticationManager;
    this.userService = userService;
    this.appProperties = appProperties;
    this.rememberMeServices = rememberMeServices;
  }

  @GetMapping("/api/auth/me")
  public UserInfo getAuthStatus(@AuthenticationPrincipal UserDetails userDetails) {
    return new UserInfo(userDetails.getUsername());
  }

  @PostMapping(path = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public UserInfo login(
      @Valid @RequestBody LoginRequest loginRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    try {
      Authentication authenticationToken =
          new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());
      // Delegate authentication to AuthenticationManager
      Authentication authentication = authenticationManager.authenticate(authenticationToken);

      HttpSession existingSession = request.getSession(false);
      if (existingSession != null) {
        existingSession.invalidate();
      }

      SecurityContext context = securityContextHolderStrategy.createEmptyContext();
      context.setAuthentication(authentication);
      this.securityContextHolderStrategy.setContext(context);
      securityContextRepository.saveContext(context, request, response);

      if (Boolean.TRUE.equals(loginRequest.rememberMe)) {
        rememberMeServices.loginSuccess(request, response, authentication);
      }

      return new UserInfo(authentication.getName());

    } catch (AuthenticationException e) {
      log.warn("Login failed for user: {} - {}", loginRequest.username(), e.getMessage());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
  }

  @GetMapping(path = "/api/auth/available")
  @ResponseStatus(HttpStatus.OK)
  public boolean checkUsername(@NotBlank @RequestParam String username) {
    return !userService.existsByUsername(username);
  }

  @GetMapping(path = "/api/auth/register")
  @ResponseStatus(HttpStatus.OK)
  public boolean needRegistrationCode() {
    return !appProperties.registrationCode().isBlank();
  }

  @PostMapping(path = "/api/auth/register", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UserInfo register(
      @Valid @RequestBody RegistrationRequest registrationRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      HttpSession session) {
    if (!appProperties.registrationCode().isBlank()
        && !appProperties.registrationCode().equalsIgnoreCase(registrationRequest.code)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invitation code.");
    }
    if (userService.existsByUsername(registrationRequest.username())) {
      log.warn(
          "Registration failed: Username '{}' already exists.", registrationRequest.username());
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    }

    if (registrationRequest.password.getBytes(StandardCharsets.UTF_8).length
        > PASSWORD_MAX_LENGTH) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Password cannot be longer than " + PASSWORD_MAX_LENGTH + " characters.");
    }

    User newUser;
    try {
      newUser =
          userService.createUser(registrationRequest.username(), registrationRequest.password());
      log.info("User '{}' registered successfully.", newUser.getUsername());
    } catch (Exception e) {
      log.error("Error saving registered user '{}'", registrationRequest.username(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error saving user", e);
    }

    session.invalidate();
    // Use the authentication flow
    Authentication authenticationToken =
        UsernamePasswordAuthenticationToken.unauthenticated(
            newUser.getUsername(), registrationRequest.password());
    Authentication authentication = authenticationManager.authenticate(authenticationToken);
    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);
    this.securityContextHolderStrategy.setContext(context);
    securityContextRepository.saveContext(context, request, response);
    return new UserInfo(registrationRequest.username());
  }

  public record LoginRequest(
      @NotBlank(message = "Username cannot be blank") String username,
      @NotBlank(message = "Password cannot be blank") String password,
      Boolean rememberMe) {}

  public record RegistrationRequest(
      @NotBlank(message = "Username cannot be blank")
          @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
          String username,
      @NotBlank(message = "Password cannot be blank")
          @Size(min = PASSWORD_MIN_LENGTH, message = "Password must be at least 8 characters long")
          String password,
      String code) {}

  public record UserInfo(String username) {}
}
