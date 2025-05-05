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
import java.util.Map;
import java.util.UUID;
import net.dmcollection.server.AppProperties;
import net.dmcollection.server.card.CollectionService.CollectionDto;
import net.dmcollection.server.card.CollectionService.CollectionExport;
import net.dmcollection.server.card.CollectionService.CollectionInfo;
import net.dmcollection.server.user.CurrentUserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class CollectionController {

  private static final Logger log = LoggerFactory.getLogger(CollectionController.class);
  private final CollectionService collectionService;
  private final AppProperties appProperties;
  private final ObjectMapper objectMapper;

  public CollectionController(
      CollectionService collectionService, AppProperties appProperties, ObjectMapper objectMapper) {
    this.collectionService = collectionService;
    this.appProperties = appProperties;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/api/decks")
  ResponseEntity<List<CollectionInfo>> getCollections(@CurrentUserId UUID currentUserId) {
    return ResponseEntity.ok(collectionService.getCollections(currentUserId));
  }

  @PostMapping("/api/decks")
  ResponseEntity<CollectionInfo> createCollection(
      @CurrentUserId UUID currentUserId, @Valid @RequestBody NameCollectionRequest request) {
    CollectionInfo createdCollection =
        collectionService.createCollection(currentUserId, request.name());
    return ResponseEntity.status(HttpStatus.CREATED).body(createdCollection);
  }

  record NameCollectionRequest(@NotBlank String name) {}

  @GetMapping("/api/deck/{id}")
  ResponseEntity<CollectionDto> getCollection(
      @CurrentUserId UUID currentUserId, @PathVariable UUID id) {
    return collectionService
        .getCollection(currentUserId, id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/api/deck/{id}")
  ResponseEntity<CollectionInfo> renameCollection(
      @CurrentUserId UUID currentUserId,
      @PathVariable UUID id,
      @Valid @RequestBody NameCollectionRequest nameRequest) {
    return collectionService
        .renameCollection(currentUserId, id, nameRequest.name)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping(value = {"/api/collection", "/api/collection/{pageNumber}"})
  ResponseEntity<CollectionDto> getPrimaryCollection(
      @CurrentUserId UUID currentUserId,
      @PathVariable(required = false) Integer pageNumber,
      @ModelAttribute SearchFilterApi searchParams) {
    Pageable pageRequest;
    if (pageNumber != null) {
      Integer pageSize = searchParams.pageSize();
      if (pageSize == null) {
        pageSize =
            Math.min(appProperties.cardPage().defaultSize(), appProperties.cardPage().maxSize());
      }
      pageRequest =
          PageRequest.of(
              pageNumber,
              pageSize,
              Sort.by("RELEASE").descending().and(Sort.by("OFFICIAL_ID").ascending()));
    } else {
      pageRequest = Pageable.unpaged();
    }
    var searchFilter = searchParams.toSearchFilter(pageRequest);
    return ResponseEntity.ok(collectionService.getPrimaryCollection(currentUserId, searchFilter));
  }

  @GetMapping("/api/collection/export")
  public ResponseEntity<byte[]> exportCollection(@CurrentUserId UUID currentUserId) {
    return this.collectionExport(List.of(collectionService.exportPrimaryCollection(currentUserId)));
  }

  @GetMapping("/api/deck/{id}/export")
  public ResponseEntity<byte[]> exportCollection(
      @CurrentUserId UUID currentUserId, @PathVariable UUID id) {
    CollectionExport deck = collectionService.exportDeck(currentUserId, id).orElse(null);
    return this.collectionExport(deck == null ? List.of() : List.of(deck));
  }

  @GetMapping("/api/decks/export")
  public ResponseEntity<byte[]> exportDecks(@CurrentUserId UUID currentUserId) {
    return this.collectionExport(collectionService.exportDecks(currentUserId));
  }

  private ResponseEntity<byte[]> collectionExport(List<CollectionExport> exports) {
    try {
      if (exports.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      byte[] jsonBytes;
      String filename;
      if (exports.size() == 1) {
        CollectionExport export = exports.getFirst();
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
      log.error("Error serializing collection data to JSON: ", e);
      return ResponseEntity.internalServerError().build();
    } catch (Exception e) {
      log.error("An unexpected error occurred during export: ", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping(
      value = "/api/collection/import",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<?> importCollection(
      @RequestBody byte[] fileBytes, @CurrentUserId UUID currentUserId) {
    try {
      CollectionExport toImport =
          objectMapper
              .reader(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .readValue(fileBytes, CollectionExport.class);
      collectionService.importPrimaryCollection(currentUserId, toImport);
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      log.error("Error reading uploaded file: ", e);
      return ResponseEntity.badRequest().build();
    }
  }

  @PostMapping(value = "/api/decks/import", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<?> importDecks(
      @RequestBody byte[] fileBytes, @CurrentUserId UUID currentUserId) {
    try {
      CollectionExport toImport =
          objectMapper
              .reader(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .readValue(fileBytes, CollectionExport.class);
      collectionService.importDeck(currentUserId, toImport);
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      try {
        List<CollectionExport> toImport =
            Arrays.asList(
                objectMapper
                    .reader(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(fileBytes, CollectionExport[].class));
        toImport.forEach(i -> collectionService.importDeck(currentUserId, i));
        return ResponseEntity.ok().build();
      } catch (IOException ex) {
        log.error("Error reading uploaded file: ", e);
        return ResponseEntity.badRequest().build();
      }
    }
  }

  @GetMapping("/api/collectionStub/cards/{cardId}")
  ResponseEntity<CollectionService.CollectionCardStub> getSingleCardAmount(
      @CurrentUserId UUID currentUserId, @PathVariable Long cardId) {
    return collectionService
        .getSingleCardAmount(currentUserId, cardId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/api/collectionStub/cards/{cardId}")
  ResponseEntity<CollectionService.CollectionCardStub> setSingleCardAmount(
      @CurrentUserId UUID currentUserId,
      @PathVariable Long cardId,
      @Valid @RequestBody AmountRequest request) {
    return collectionService
        .setSingleCardAmount(currentUserId, cardId, request.amount())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/api/collectionStub")
  ResponseEntity<Map<Long, Integer>> setCardAmountOnStub(
      @CurrentUserId UUID currentUserId, @Valid @RequestBody SetCardAmountRequest request) {
    return collectionService
        .setCardAmountOnStub(currentUserId, request.cardId(), request.amount())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/api/collection/cards/{cardId}")
  ResponseEntity<CollectionInfo> setCardAmount(
      @CurrentUserId UUID currentUserId,
      @PathVariable Long cardId,
      @Valid @RequestBody AmountRequest request) {
    return collectionService
        .setCardAmount(currentUserId, cardId, request.amount())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/api/deck/{id}")
  ResponseEntity<?> deleteCollection(@CurrentUserId UUID currentUserId, @PathVariable UUID id) {
    if (collectionService.deleteCollection(currentUserId, id)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/api/deck/{collectionId}/cards/{cardId}")
  ResponseEntity<CollectionInfo> setCardAmount(
      @CurrentUserId UUID currentUserId,
      @PathVariable UUID collectionId,
      @PathVariable Long cardId,
      @Valid @RequestBody AmountRequest request) {
    return collectionService
        .setCardAmount(currentUserId, collectionId, cardId, request.amount())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  record SetCardAmountRequest(long cardId, @Min(0) int amount) {}

  record AmountRequest(@Min(0) int amount) {}
}
