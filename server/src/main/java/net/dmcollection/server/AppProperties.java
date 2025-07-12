package net.dmcollection.server;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "dmcollection")
@Validated
public record AppProperties(
    @NotBlank @Nonnull String imageStoragePath,
    @Nonnull CardPage cardPage,
    @Nonnull String registrationCode,
    @Nonnull String rememberMeKey) {

  public record CardPage(@Min(1) int maxSize, @Min(1) int defaultSize) {}
}
