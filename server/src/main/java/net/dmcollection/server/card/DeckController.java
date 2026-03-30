package net.dmcollection.server.card;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.dmcollection.server.card.DeckService.DeckDto;
import net.dmcollection.server.card.DeckService.DeckExport;
import net.dmcollection.server.card.DeckService.DeckInfo;
import net.dmcollection.server.user.CurrentUserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class DeckController {

  private static final Logger log = LoggerFactory.getLogger(DeckController.class);
  private final DeckService deckService;
  private final ObjectMapper objectMapper;

  public DeckController(DeckService deckService, ObjectMapper objectMapper) {
    this.deckService = deckService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/api/decks")
  ResponseEntity<List<DeckInfo>> getDecks(@CurrentUserId UUID currentUserId) {
    return ResponseEntity.ok(deckService.getDecks(currentUserId));
  }

  @PostMapping("/api/decks")
  ResponseEntity<DeckInfo> createDeck(
      @CurrentUserId UUID currentUserId, @Valid @RequestBody NameRequest request) {
    DeckInfo createdDeck = deckService.createDeck(currentUserId, request.name());
    return ResponseEntity.status(HttpStatus.CREATED).body(createdDeck);
  }

  record NameRequest(@NotBlank String name) {}

  @GetMapping("/api/deck/{id}")
  ResponseEntity<DeckDto> getDeck(@CurrentUserId UUID currentUserId, @PathVariable UUID id) {
    return deckService
        .getDeck(currentUserId, id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/api/deck/{id}")
  ResponseEntity<DeckInfo> renameDeck(
      @CurrentUserId UUID currentUserId,
      @PathVariable UUID id,
      @Valid @RequestBody NameRequest nameRequest) {
    return deckService
        .renameDeck(currentUserId, id, nameRequest.name)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/api/deck/{id}")
  ResponseEntity<?> deleteDeck(@CurrentUserId UUID currentUserId, @PathVariable UUID id) {
    if (deckService.deleteDeck(currentUserId, id)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/api/deck/{collectionId}/cards/{cardId}")
  ResponseEntity<DeckInfo> setCardAmount(
      @CurrentUserId UUID currentUserId,
      @PathVariable UUID collectionId,
      @PathVariable Long cardId,
      @Valid @RequestBody AmountRequest request) {
    return deckService
        .setCardAmount(currentUserId, collectionId, cardId, request.amount())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  record AmountRequest(@Min(0) int amount) {}

  @GetMapping("/api/deck/{id}/export")
  public ResponseEntity<byte[]> exportDeck(
      @CurrentUserId UUID currentUserId, @PathVariable UUID id) {
    DeckExport deck = deckService.exportDeck(currentUserId, id).orElse(null);
    return deckExport(deck == null ? List.of() : List.of(deck));
  }

  @GetMapping("/api/decks/export")
  public ResponseEntity<byte[]> exportDecks(@CurrentUserId UUID currentUserId) {
    return deckExport(deckService.exportDecks(currentUserId));
  }

  private ResponseEntity<byte[]> deckExport(List<DeckExport> exports) {
    try {
      if (exports.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      byte[] jsonBytes;
      String filename;
      if (exports.size() == 1) {
        DeckExport export = exports.getFirst();
        jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export);
        filename = export.title();
      } else {
        jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(exports);
        filename = "decks";
      }

      filename += "-export-" + timestamp + ".json";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      headers.setContentDisposition(
          ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

      headers.setContentLength(jsonBytes.length);
      return new ResponseEntity<>(jsonBytes, headers, HttpStatus.OK);

    } catch (JsonProcessingException e) {
      log.error("Error serializing deck data to JSON: ", e);
      return ResponseEntity.internalServerError().build();
    } catch (Exception e) {
      log.error("An unexpected error occurred during export: ", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping(value = "/api/decks/import", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<?> importDecks(
      @RequestBody byte[] fileBytes, @CurrentUserId UUID currentUserId) {
    try {
      DeckExport toImport =
          objectMapper
              .reader()
              .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .readValue(fileBytes, DeckExport.class);
      deckService.importDeck(currentUserId, toImport);
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      try {
        List<DeckExport> toImport =
            Arrays.asList(
                objectMapper
                    .reader()
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(fileBytes, DeckExport[].class));
        toImport.forEach(i -> deckService.importDeck(currentUserId, i));
        return ResponseEntity.ok().build();
      } catch (IOException ex) {
        log.error("Error reading uploaded file: ", e);
        return ResponseEntity.badRequest().build();
      }
    }
  }
}
