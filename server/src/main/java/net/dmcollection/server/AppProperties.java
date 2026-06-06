package net.dmcollection.server;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "dmcollection")
@Validated
public record AppProperties(
    @Nonnull CardPage cardPage,
    @Nonnull String registrationCode,
    @Nonnull String rememberMeKey,
    @Nonnull ImageService imageService) {

  public record CardPage(@Min(1) int maxSize, @Min(1) int defaultSize) {}

  public record ImageService(
      boolean enabled, @Nullable String baseUrl, @Nullable String imageStoragePath) {
    public ImageService {
      if (enabled && (baseUrl == null || baseUrl.isBlank())) {
        throw new IllegalArgumentException("Enabled image service requires a base URL");
      }
      if (!enabled && (imageStoragePath == null || imageStoragePath.isBlank())) {
        throw new IllegalArgumentException(
            "Disabled image storage service requires an image storage path");
      }
    }
  }
}
