package net.dmcollection.server.carddata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import net.dmcollection.server.card.internal.query.CardTypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CardDataStartupListener {

  private static final Logger log = LoggerFactory.getLogger(CardDataStartupListener.class);

  private final CardDataImportService importService;
  private final ObjectMapper objectMapper;
  private final String cardDataPath;
  private final CardTypeResolver cardTypeResolver;

  public CardDataStartupListener(
      CardDataImportService importService,
      ObjectMapper objectMapper,
      @Value("${dmcollection.card-data-path:}") String cardDataPath,
      CardTypeResolver cardTypeResolver) {
    this.importService = importService;
    this.objectMapper = objectMapper;
    this.cardDataPath = cardDataPath;
    this.cardTypeResolver = cardTypeResolver;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (cardDataPath == null || cardDataPath.isBlank()) {
      log.info("No card-data-path configured, skipping card data import");
      return;
    }
    try {
      log.info("Loading card data from {}", cardDataPath);
      CardDataJson data =
          objectMapper.readValue(Path.of(cardDataPath).toFile(), CardDataJson.class);
      importService.importCardData(data);
      cardTypeResolver.loadNameToId();
    } catch (IOException e) {
      log.error("Failed to load card data from {}", cardDataPath, e);
    }
  }
}
