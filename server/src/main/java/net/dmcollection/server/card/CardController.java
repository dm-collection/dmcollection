package net.dmcollection.server.card;

import java.util.List;
import java.util.UUID;
import net.dmcollection.server.AppProperties;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.internal.CardQueryService;
import net.dmcollection.server.user.CurrentUserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CardController {

  private static final Logger log = LoggerFactory.getLogger(CardController.class);
  private final CardService cardService;
  private final CardQueryService cardQueryService;
  private final AppProperties appProperties;
  private final CollectionService collectionService;

  public CardController(
      CardService cardService,
      CardQueryService cardQueryService,
      CollectionService collectionService,
      AppProperties appProperties) {
    this.cardService = cardService;
    this.cardQueryService = cardQueryService;
    this.collectionService = collectionService;
    this.appProperties = appProperties;
  }

  @GetMapping("/api/cards/{pageNumber}")
  ResponseEntity<PagedModel<CardStub>> getCards(
      @CurrentUserId UUID currentUserId,
      @PathVariable int pageNumber,
      @ModelAttribute SearchFilterApi searchParams) {
    Integer pageSize = searchParams.pageSize();
    if (pageSize == null) {
      pageSize =
          Math.min(appProperties.cardPage().defaultSize(), appProperties.cardPage().maxSize());
    }
    var searchFilter = searchParams.toSearchFilter(pageNumber, pageSize);
    var collectionIds = collectionService.getPrimaryCollectionIds(currentUserId);
    if (collectionIds.isPresent()) {
      searchFilter = searchFilter.withCollectionFilter(collectionIds.get().internalId(), false);
    }
    try {
      return ResponseEntity.ok(
          new PagedModel<>(cardQueryService.search(searchFilter).pageOfCards()));
    } catch (RuntimeException e) {
      log.error("Error searching for {}", searchFilter, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/api/card/{id}")
  ResponseEntity<?> getCard(@PathVariable String id) {
    var cardDto = cardService.getCardDto(id);
    if (cardDto.isPresent()) {
      return ResponseEntity.ok(cardDto.get());
    }
    return ResponseEntity.notFound().build();
  }

  @GetMapping("/api/cards")
  ResponseEntity<?> getCardsById(@RequestParam List<Long> cardIds) {
    var cards = cardService.getByIds(cardIds);
    if (cards.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(cards);
  }
}
