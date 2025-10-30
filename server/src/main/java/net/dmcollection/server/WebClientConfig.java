package net.dmcollection.server;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  /**
   * WebClient bean configured specifically for Imagor image processing requests.
   *
   * <p>Uses shorter timeouts than default to fail fast on Imagor unavailability:
   *
   * <ul>
   *   <li>2s connect timeout - fails quickly if Imagor is down
   *   <li>10s read timeout - allows time for image processing while preventing hangs
   * </ul>
   *
   * <p><strong>Note:</strong> This bean is specifically configured for Imagor and should not be
   * used by other components. Use {@code @Qualifier("imagorWebClient")} when injecting.
   */
  @Bean("imagorWebClient")
  public WebClient imagorWebClient(AppProperties appProperties, WebClient.Builder builder) {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS)));

    return builder
        .baseUrl(appProperties.imagor().baseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}
