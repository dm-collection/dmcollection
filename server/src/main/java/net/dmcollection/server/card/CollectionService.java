package net.dmcollection.server.card;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.dmcollection.server.card.CardCollection.CollectionIds;
import net.dmcollection.server.card.CardService.CardDto;
import net.dmcollection.server.card.CardService.CardFacetDto;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.internal.CardQueryService;
import net.dmcollection.server.card.internal.CardQueryService.SearchResult;
import net.dmcollection.server.card.internal.SearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class CollectionService {

  private static final Logger log = LoggerFactory.getLogger(CollectionService.class);
  private final CollectionRepository collectionRepository;
  private final CardService cardService;
  private final CardQueryService cardQueryService;
  private static final int EXPORT_FORMAT_VERSION = 1;

  public CollectionService(
      CollectionRepository collectionRepository,
      CardService cardService,
      CardQueryService cardQueryService) {
    this.collectionRepository = collectionRepository;
    this.cardService = cardService;
    this.cardQueryService = cardQueryService;
  }

  public record CollectionInfo(
      UUID id,
      String name,
      long uniqueCardCount,
      long totalCardCount,
      LocalDateTime lastModified,
      UUID ownerId) {
    static CollectionInfo ofCollection(CardCollection collection) {
      return new CollectionInfo(
          collection.getPublicId(),
          collection.getName(),
          collection.cards.size(),
          collection.cards.stream().mapToInt(CollectionCards::amount).sum(),
          collection.updatedAt,
          collection.owner.getId());
    }
  }

  public record CollectionDto(CollectionInfo info, PagedModel<CardStub> cardPage) {}

  public record CollectionCardStub(long cardId, int amount) {}

  public record CollectionCardExport(long Id, String name, String shortName, int amount) {}

  public record CollectionExport(
      int version,
      LocalDateTime exportDateTime,
      String title,
      int cardCount,
      int countWithoutDuplicates,
      List<CollectionCardExport> cards) {}

  public List<CollectionInfo> getCollections(UUID userId) {
    return collectionRepository.findByOwnerAndPrimaryIsFalseOrderByUpdatedAtDesc(userId).stream()
        .map(CollectionInfo::ofCollection)
        .toList();
  }

  public CollectionExport exportPrimaryCollection(UUID userId) {
    return forExport(getPrimary(userId), "collection");
  }

  public Optional<CollectionExport> exportDeck(UUID userId, UUID collectionId) {
    return collectionRepository
        .findByPublicIdAndOwnerAndPrimaryIsFalse(collectionId, userId)
        .map(c -> forExport(c, c.getName()));
  }

  public List<CollectionExport> exportDecks(UUID userId) {
    return collectionRepository.findByOwnerAndPrimaryIsFalseOrderByUpdatedAtDesc(userId).stream()
        .map(c -> forExport(c, c.getName()))
        .toList();
  }

  public void importPrimaryCollection(UUID userId, CollectionExport toImport) {
    CardCollection collection = getPrimary(userId);
    collection.removeAllCards();
    toImport.cards.forEach(c -> collection.setCardAmount(c.Id(), c.amount()));
    collectionRepository.save(collection);
  }

  public void importDeck(UUID userId, CollectionExport toImport) {
    CardCollection newCollection = new CardCollection(false);
    newCollection.setOwner(userId);
    newCollection.setName(toImport.title);
    toImport.cards().forEach(c -> newCollection.setCardAmount(c.Id(), c.amount()));
    collectionRepository.save(newCollection);
  }

  private CollectionExport forExport(CardCollection collection, String title) {
    Map<Long, Integer> amountsById =
        collection.getCards().stream()
            .collect(Collectors.toMap(c -> c.id().getId(), CollectionCards::amount));
    List<CardDto> cards = cardService.getCards(collection.getCards());
    List<CollectionCardExport> cardExport =
        cards.stream()
            .map(
                c -> {
                  String cardName =
                      String.join(
                          "／",
                          c.facets().stream()
                              .map(CardFacetDto::name)
                              .filter(Objects::nonNull)
                              .toList());

                  return new CollectionCardExport(
                      c.id(), cardName, c.dmId(), amountsById.get(c.id()));
                })
            .toList();
    int total = cardExport.stream().mapToInt(c -> c.amount).sum();
    return new CollectionExport(
        EXPORT_FORMAT_VERSION, LocalDateTime.now(), title, total, cardExport.size(), cardExport);
  }

  public Optional<CollectionDto> getCollection(UUID userId, UUID collectionId) {
    var collection =
        collectionRepository.findByPublicIdAndOwnerAndPrimaryIsFalse(collectionId, userId);
    return collection.map(this::forTransfer);
  }

  public CollectionDto getPrimaryCollection(UUID userId, SearchFilter searchFilter) {
    Optional<CollectionIds> collectionIds = getPrimaryCollectionIds(userId);
    if (collectionIds.isEmpty()) {
      CardCollection collection = makePrimaryCollection(userId);
      return forTransfer(collection);
    }
    searchFilter = searchFilter.withCollectionFilter(collectionIds.get().internalId(), true);
    SearchResult searchResult = cardQueryService.search(searchFilter);
    CollectionInfo ci =
        new CollectionInfo(
            collectionIds.get().publicId(),
            null,
            searchResult.pageOfCards().getTotalElements(),
            searchResult.totalCollected(),
            null,
            userId);
    return new CollectionDto(ci, new PagedModel<>(searchResult.pageOfCards()));
  }

  public Optional<CollectionIds> getPrimaryCollectionIds(UUID userId) {
    return collectionRepository.findIdsByOwnerAndPrimaryIsTrue(userId);
  }

  public Map<Long, Integer> getPrimaryStub(UUID userId) {
    var collection = collectionRepository.findByOwnerAndPrimaryIsTrue(userId);
    if (collection.isEmpty()) {
      var newCollection = new CardCollection(true);
      newCollection.setOwner(userId);
      newCollection.setName("Collection");
      collection = Optional.of(collectionRepository.save(newCollection));
    }
    return fromCollection(collection.get());
  }

  private static Map<Long, Integer> fromCollection(CardCollection collection) {
    return collection.cards.stream()
        .filter(CollectionService::hasCardId)
        .collect(Collectors.toMap(c -> c.id().getId(), CollectionCards::amount));
  }

  private static boolean hasCardId(CollectionCards card) {
    return card.id().getId() != null;
  }

  private CardCollection getPrimary(UUID userId) {
    var collection = collectionRepository.findByOwnerAndPrimaryIsTrue(userId);
    return collection.orElseGet(() -> makePrimaryCollection(userId));
  }

  public boolean deleteCollection(UUID userId, UUID collectionId) {
    return collectionRepository
        .findByPublicIdAndOwnerAndPrimaryIsFalse(collectionId, userId)
        .map(
            c -> {
              collectionRepository.deleteById(c.getInternalId());
              return true;
            })
        .orElse(false);
  }

  private CardCollection makePrimaryCollection(UUID userId) {
    var newCollection = new CardCollection(true);
    newCollection.setOwner(userId);
    newCollection.setName("Collection");
    collectionRepository.save(newCollection);
    return collectionRepository
        .findById(collectionRepository.save(newCollection).getInternalId())
        .orElseThrow(IllegalStateException::new);
  }

  public CollectionInfo createCollection(UUID userId, String name) {
    var collection = new CardCollection();
    collection.setName(name);
    collection.setOwner(userId);
    collection = collectionRepository.save(collection);
    return CollectionInfo.ofCollection(
        collectionRepository
            .findById(collection.getInternalId())
            .orElseThrow(IllegalStateException::new));
  }

  public Optional<CollectionInfo> renameCollection(UUID userId, UUID collectionId, String name) {
    var collection =
        collectionRepository.findByPublicIdAndOwnerAndPrimaryIsFalse(collectionId, userId);
    if (collection.isPresent()) {
      collection.get().setName(name);
      return Optional.of(CollectionInfo.ofCollection(collectionRepository.save(collection.get())));
    }
    return Optional.empty();
  }

  public Optional<Map<Long, Integer>> setCardAmountOnStub(UUID userId, Long cardId, int amount) {
    if (cardService.cardExists(cardId)) {
      CardCollection primary = getPrimary(userId);
      primary.setCardAmount(cardId, amount);
      primary = collectionRepository.save(primary);
      return Optional.of(fromCollection(primary));
    }
    return Optional.empty();
  }

  public Optional<CollectionCardStub> setSingleCardAmount(UUID userId, Long cardId, int amount) {
    if (cardService.cardExists(cardId)) {
      CardCollection primary = getPrimary(userId);
      primary.setCardAmount(cardId, amount);
      primary = collectionRepository.save(primary);
      return Optional.of(new CollectionCardStub(cardId, primary.getCardAmount(cardId)));
    }
    return Optional.empty();
  }

  public Optional<CollectionCardStub> getSingleCardAmount(UUID userId, Long cardId) {
    if (cardService.cardExists(cardId)) {
      return Optional.of(new CollectionCardStub(cardId, getPrimary(userId).getCardAmount(cardId)));
    }
    return Optional.empty();
  }

  public Optional<CollectionInfo> setCardAmount(UUID userId, Long cardId, int amount) {
    if (cardService.cardExists(cardId)) {
      var primary = getPrimary(userId);
      primary.setCardAmount(cardId, amount);
      return Optional.of(CollectionInfo.ofCollection(collectionRepository.save(primary)));
    }
    return Optional.empty();
  }

  public Optional<CollectionInfo> setCardAmount(
      UUID userId, UUID collectionId, Long cardId, int amount) {
    var collection =
        collectionRepository.findByPublicIdAndOwnerAndPrimaryIsFalse(collectionId, userId);
    if (collection.isPresent()) {
      var cc = collection.get();
      if (cardService.cardExists(cardId)) {
        cc.setCardAmount(cardId, amount);
        return Optional.of(CollectionInfo.ofCollection(collectionRepository.save(cc)));
      }
    }
    return Optional.empty();
  }

  private CollectionDto forTransfer(@NonNull CardCollection collection) {
    List<CardStub> cardStubs = cardService.fromCollectionCards(collection.cards);
    return new CollectionDto(
        CollectionInfo.ofCollection(collection),
        new PagedModel<>(new PageImpl<>(cardStubs, Pageable.unpaged(), cardStubs.size())));
  }
}
