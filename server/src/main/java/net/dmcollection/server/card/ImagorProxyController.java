package net.dmcollection.server.card;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Controller
public class ImagorProxyController {

  private static final Logger log = LoggerFactory.getLogger(ImagorProxyController.class);
  private static final Map<String, String> ALLOWED_SIZES = Map.of("250", "250x0", "650", "650x0");
  private static final Resource PLACEHOLDER_IMAGE =
      new ClassPathResource("images/placeholder-card.webp");

  private final WebClient imagorWebClient;

  public ImagorProxyController(@Qualifier("imagorWebClient") WebClient imagorWebClient) {
    this.imagorWebClient = imagorWebClient;
  }

  /**
   * Proxies image requests to Imagor with size validation.
   *
   * @param size Size descriptor from frontend ("250" or "650")
   * @param filename Card image filename
   * @return Processed image from Imagor or placeholder on error
   */
  @GetMapping("/image/{size}/{filename}")
  public ResponseEntity<byte[]> getImage(@PathVariable String size, @PathVariable String filename) {
    // Validate size against whitelist
    String imagorSize = ALLOWED_SIZES.get(size);
    if (imagorSize == null) {
      log.warn("Invalid size requested: {}", size);
      return ResponseEntity.badRequest().build();
    }

    return proxyToImagor(imagorSize, filename);
  }

  /**
   * Backward compatibility endpoint - defaults to 650x0 size.
   *
   * @param filename Card image filename
   * @return Processed image from Imagor or placeholder on error
   */
  @GetMapping("/image/{filename}")
  public ResponseEntity<byte[]> getImageDefault(@PathVariable String filename) {
    return proxyToImagor("650x0", filename);
  }

  private ResponseEntity<byte[]> proxyToImagor(String imagorSize, String filename) {
    // Validate filename to prevent path traversal
    if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
      log.warn("Invalid filename requested: {}", filename);
      return ResponseEntity.badRequest().build();
    }

    // Build Imagor URL path: /fit-in/{size}/{filename}
    String imagorPath = String.format("/fit-in/%s/%s", imagorSize, filename);
    log.debug("Proxying to Imagor: {}", imagorPath);

    try {
      return imagorWebClient
          .get()
          .uri(imagorPath)
          .exchangeToMono(
              response -> {
                HttpStatus status = HttpStatus.resolve(response.statusCode().value());
                if (status == null) {
                  log.error("Unknown status code from Imagor: {}", response.statusCode());
                  return response.bodyToMono(byte[].class).map(b -> servePlaceholder());
                }

                if (status.is2xxSuccessful()) {
                  // Success - return image bytes
                  return response
                      .bodyToMono(byte[].class)
                      .map(
                          bytes ->
                              ResponseEntity.ok()
                                  .contentType(
                                      MediaType.IMAGE_JPEG) // Default, Imagor may return WebP/AVIF
                                  .body(bytes));
                } else if (status.is5xxServerError()) {
                  // Server error - return 503 with Retry-After
                  log.error("Server error from Imagor: {} - {}", status, filename);
                  return response
                      .releaseBody()
                      .thenReturn(
                          ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                              .header("Retry-After", "60")
                              .build());
                } else {
                  // Client error (404, etc.) - return placeholder
                  log.debug("Client error from Imagor: {} - {}", status, filename);
                  return response.releaseBody().thenReturn(servePlaceholder());
                }
              })
          .onErrorResume(
              WebClientRequestException.class,
              e -> {
                log.error("Network error connecting to Imagor: {}", e.getMessage());
                return Mono.just(servePlaceholder());
              })
          .onErrorResume(
              Exception.class,
              e -> {
                log.error("Unexpected error proxying to Imagor for: {}", filename, e);
                return Mono.just(ResponseEntity.internalServerError().build());
              })
          .block(); // Block to return synchronously from controller
    } catch (Exception e) {
      log.error("Unexpected error in proxyToImagor for: {}", filename, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  private ResponseEntity<byte[]> servePlaceholder() {
    try (InputStream is = PLACEHOLDER_IMAGE.getInputStream()) {
      byte[] placeholderBytes = is.readAllBytes();
      // Use no-cache to allow revalidation when images become available
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("image/webp"))
          .header("Cache-Control", "no-cache")
          .body(placeholderBytes);
    } catch (IOException e) {
      log.error("Failed to load placeholder image", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
