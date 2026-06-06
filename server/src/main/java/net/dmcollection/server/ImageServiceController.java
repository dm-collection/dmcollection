package net.dmcollection.server;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnBooleanProperty("dmcollection.image-service.enabled")
public class ImageServiceController {

  private final WebClient webClient;

  public ImageServiceController(@Qualifier("imageServiceClient") WebClient webClient) {
    this.webClient = webClient;
  }

  @GetMapping("/image/{*imagePath}")
  public Mono<ResponseEntity<byte[]>> image(@PathVariable String imagePath) {
    if (imagePath.contains("..")) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    try {
      URI uri = new URI(imagePath);
      if (uri.isAbsolute()) {
        return Mono.just(ResponseEntity.badRequest().build());
      }
    } catch (URISyntaxException _) {
      // relative URLs are OK
    }
    return webClient
        .get()
        .uri(imagePath)
        .exchangeToMono(
            response -> {
              HttpHeaders headers = response.headers().asHttpHeaders();
              MediaType contentType = headers.getContentType();
              String cacheControl = headers.getFirst(HttpHeaders.CACHE_CONTROL);
              String vary = headers.getFirst(HttpHeaders.VARY);
              var builder = ResponseEntity.status(response.statusCode());
              builder.contentType(
                  contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
              if (cacheControl != null) {
                builder.header(HttpHeaders.CACHE_CONTROL, cacheControl);
              }
              if (vary != null) {
                builder.header(HttpHeaders.VARY, vary);
              }
              return response.bodyToMono(byte[].class).map(builder::body);
            });
  }
}
