package net.dmcollection.server;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnBooleanProperty("dmcollection.image-service.enabled")
public class ImageServiceController {

  private final WebClient webClient;

  public ImageServiceController(WebClient webClient) {
    this.webClient = webClient;
  }

  @GetMapping("/image/{**]")
  public Mono<ResponseEntity<Flux<DataBuffer>>> image(@PathVariable String imagePath) {
    if (imagePath.contains("..")) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    String relativeImagePath = "/" + imagePath;
    try {
      URI uri = new URI(relativeImagePath);
      if (uri.isAbsolute()) {
        return Mono.just(ResponseEntity.badRequest().build());
      }
    } catch (URISyntaxException _) {
    }
    return webClient
        .get()
        .uri(relativeImagePath)
        .exchangeToMono(
            response -> {
              HttpHeaders headers = response.headers().asHttpHeaders();
              MediaType contentType = headers.getContentType();
              String cacheControl = headers.getFirst(HttpHeaders.CACHE_CONTROL);
              Flux<DataBuffer> body = response.bodyToFlux(DataBuffer.class);
              var builder = ResponseEntity.status(response.statusCode());
              builder.contentType(
                  contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
              if (cacheControl != null) {
                builder.header(HttpHeaders.CACHE_CONTROL, cacheControl);
              }
              return Mono.just(builder.body(body));
            });
  }
}
