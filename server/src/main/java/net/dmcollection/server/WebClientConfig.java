package net.dmcollection.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  private final AppProperties appProperties;

  public WebClientConfig(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Bean
  @ConditionalOnBooleanProperty("dmcollection.image-service.enabled")
  public WebClient webClient(WebClient.Builder builder) {
    return builder.baseUrl(appProperties.imageService().baseUrl()).build();
  }
}
