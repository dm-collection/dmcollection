package net.dmcollection.server.card;

import jakarta.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.dmcollection.server.AppProperties;
import org.springframework.stereotype.Service;

@Service
public class ImageService {

  private final AppProperties appProperties;

  public ImageService(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  public @Nullable String makeImageUrl(@Nullable String imageFileName) {
    if (imageFileName == null) return null;
    if (imageExists(imageFileName)) {
      return "/image/" + imageFileName;
    }
    return null;
  }

  private boolean imageExists(String imageFileName) {
    return Files.exists(Paths.get(appProperties.imageStoragePath(), imageFileName));
  }
}
