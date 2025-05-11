package net.dmcollection.server.card;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

@Service
public class ImageService {

  public @Nullable String makeImageUrl(@Nullable String imageFileName) {
    if (imageFileName == null) return null;
    return "/image/" + imageFileName;
  }
}
