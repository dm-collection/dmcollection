package net.dmcollection.server;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnBooleanProperty("dmcollection.image-service.enabled")
public class ImageServiceController {

  private final WebClient webClient;

  private static final Set<String> FORWARDED_REQUEST_HEADERS =
      Set.of(
          HttpHeaders.ACCEPT,
          HttpHeaders.IF_NONE_MATCH,
          HttpHeaders.IF_MODIFIED_SINCE,
          HttpHeaders.RANGE,
          HttpHeaders.IF_RANGE);

  private static final Set<String> FORWARDED_RESPONSE_HEADERS =
      Set.of(
          HttpHeaders.CONTENT_TYPE,
          HttpHeaders.CACHE_CONTROL,
          HttpHeaders.PRAGMA,
          HttpHeaders.EXPIRES,
          HttpHeaders.ETAG,
          HttpHeaders.LAST_MODIFIED,
          HttpHeaders.CONTENT_RANGE,
          HttpHeaders.ACCEPT_RANGES);

  public ImageServiceController(@Qualifier("imageServiceClient") WebClient webClient) {
    this.webClient = webClient;
  }

  @RequestMapping(
      method = {RequestMethod.GET, RequestMethod.HEAD},
      path = "/image/{*imagePath}")
  public ResponseEntity<byte[]> image(@PathVariable String imagePath, HttpServletRequest request) {
    if (imagePath.contains("..")) {
      return ResponseEntity.badRequest().build();
    }
    HttpMethod method = HttpMethod.valueOf(request.getMethod());
    WebClient.RequestHeadersSpec<?> requestSpec = webClient.method(method).uri(imagePath);
    copyRequestHeaders(request, requestSpec);

    UpstreamResponse upstream;
    try {
      upstream =
          requestSpec
              .exchangeToMono(
                  response -> {
                    HttpHeaders headers = copyResponseHeaders(response.headers().asHttpHeaders());
                    if (method == HttpMethod.HEAD) {
                      return Mono.just(new UpstreamResponse(response.statusCode(), headers, null));
                    }
                    return response
                        .bodyToMono(byte[].class)
                        .defaultIfEmpty(new byte[0])
                        .map(body -> new UpstreamResponse(response.statusCode(), headers, body));
                  })
              .block();
    } catch (WebClientRequestException _) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
    if (upstream == null) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
    if (method == HttpMethod.HEAD) {
      return new ResponseEntity<>(upstream.headers(), upstream.status());
    }
    return new ResponseEntity<>(upstream.body(), upstream.headers(), upstream.status());
  }

  private static void copyRequestHeaders(
      HttpServletRequest request, WebClient.RequestHeadersSpec<?> requestSpec) {
    FORWARDED_REQUEST_HEADERS.forEach(
        headerName -> {
          var values = request.getHeaders(headerName);
          while (values.hasMoreElements()) {
            requestSpec.header(headerName, values.nextElement());
          }
        });
  }

  private static HttpHeaders copyResponseHeaders(HttpHeaders source) {
    HttpHeaders headers = new HttpHeaders();
    FORWARDED_RESPONSE_HEADERS.forEach(
        headerName -> {
          var values = source.get(headerName);
          if (values != null && !values.isEmpty()) {
            headers.put(headerName, values);
          }
        });
    return headers;
  }

  private record UpstreamResponse(
      @NonNull HttpStatusCode status, @NonNull HttpHeaders headers, @Nullable byte[] body) {
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UpstreamResponse that = (UpstreamResponse) o;
      return this.status.equals(that.status)
          && this.headers.equals(that.headers)
          && Arrays.equals(this.body, that.body);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(status, headers);
      result = 31 * result + Arrays.hashCode(body);
      return result;
    }

    @Override
    @NonNull
    public String toString() {
      return "UpstreamResponse{" + "status=" + status + "headers=" + headers + "}";
    }
  }
}
