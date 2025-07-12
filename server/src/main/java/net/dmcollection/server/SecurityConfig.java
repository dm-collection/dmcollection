package net.dmcollection.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import net.dmcollection.server.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer.SessionFixationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  private final AppProperties appProperties;
  private final UserDetailsService userDetailsService;

  public SecurityConfig(AppProperties appProperties, UserService userDetailsService) {
    this.appProperties = appProperties;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public TokenBasedRememberMeServices rememberMeServices() {
    var rememberMeServices =
        new TokenBasedRememberMeServices(
            appProperties.rememberMeKey(), userDetailsService);
    rememberMeServices.setTokenValiditySeconds(30 * 24 * 60 * 60);
    rememberMeServices.setAlwaysRemember(true); // would otherwise not work with our login request format
    return rememberMeServices;
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.authorizeHttpRequests(
            auth ->
                // Permit access to static resources needed for frontend
                auth.requestMatchers("/", "/index.html", "/_app/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/checkUsername")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auth/status")
                    .permitAll()
                    .requestMatchers("/image/**")
                    .authenticated()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .sessionManagement(
            session ->
                session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(SessionFixationConfigurer::changeSessionId))
        .rememberMe(remember -> remember.rememberMeServices(rememberMeServices()))
        // Configure Exception Handling (custom entry point for SPA routing)
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        new SpaAuthenticationEntryPoint()) // Handle 401 for SPA
                    .accessDeniedHandler(new SpaAccessDeniedHandler()) // Handle 403 for SPA
            )
        // Disable default form login page
        .formLogin(AbstractHttpConfigurer::disable)
        // Disable default basic authentication prompt
        .httpBasic(AbstractHttpConfigurer::disable)
        // Configure Logout
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/logout") // The endpoint the frontend will call
                    .logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler(
                            HttpStatus.NO_CONTENT)) // Return 204 on success
                    .invalidateHttpSession(true) // Invalidate session
                    .deleteCookies("JSESSIONID", "remember-me")
                    .permitAll() // Allow anyone to call logout
            );

    return http.build();
  }

  /**
   * AuthenticationEntryPoint for SPAs: - For API requests (starting with /api/ or /image/), return
   * 401 Unauthorized. - For other GET requests (assumed to be frontend routes), forward to
   * /index.html. - For other methods (POST, PUT etc.) on non-API paths, return 401 (shouldn't
   * happen often).
   */
  private static class SpaAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException)
        throws IOException, ServletException {
      String path = request.getRequestURI();
      boolean isApiRequest = path.startsWith("/api/") || path.startsWith("/image/");
      boolean isGetRequest = Objects.equals(request.getMethod(), HttpMethod.GET.name());

      if (!isApiRequest && isGetRequest) {
        // Forward to index.html for frontend routing (for GET requests)
        request.getRequestDispatcher("/index.html").forward(request, response);
      } else {
        // For API requests or non-GET requests, return 401
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
      }
    }
  }

  /**
   * AccessDeniedHandler for SPAs: - For API requests (starting with /api/ or /image/), return 403
   * Forbidden. - For other GET requests (assumed to be frontend routes), forward to /index.html
   * (even if forbidden, let the frontend decide what to show based on auth status). - For other
   * methods (POST, PUT etc.) on non-API paths, return 403.
   */
  private static class SpaAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        org.springframework.security.access.AccessDeniedException accessDeniedException)
        throws IOException, ServletException {

      String path = request.getRequestURI();
      boolean isApiRequest = path.startsWith("/api/") || path.startsWith("/image/");
      boolean isGetRequest = Objects.equals(request.getMethod(), HttpMethod.GET.name());

      if (!isApiRequest && isGetRequest) {
        // Forward to index.html for frontend routing (for GET requests)
        // Let the frontend handle the fact that the user might be logged in
        // but still can't access the intended *client-side* route if there's
        // specific logic tied to it. Or, the access denied might have been
        // for the index.html itself if permissions were stricter.
        request.getRequestDispatcher("/index.html").forward(request, response);
      } else {
        // For API requests or non-GET requests, return 403 Forbidden
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
      }
    }
  }

  // https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa
  static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
        HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
      /*
       * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
       * the CsrfToken when it is rendered in the response body.
       */
      this.xor.handle(request, response, csrfToken);
      /*
       * Render the token value to a cookie by causing the deferred token to be loaded.
       */
      csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
      String headerValue = request.getHeader(csrfToken.getHeaderName());
      /*
       * If the request contains a request header, use CsrfTokenRequestAttributeHandler
       * to resolve the CsrfToken. This applies when a single-page application includes
       * the header value automatically, which was obtained via a cookie containing the
       * raw CsrfToken.
       *
       * In all other cases (e.g. if the request contains a request parameter), use
       * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
       * when a server-side rendered form includes the _csrf request parameter as a
       * hidden input.
       */
      return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
          .resolveCsrfTokenValue(request, csrfToken);
    }
  }
}
