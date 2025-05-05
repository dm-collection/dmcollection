package net.dmcollection.server.card;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.model.card.CardEntity;
import net.dmcollection.model.card.CardFacet;
import net.dmcollection.model.card.CardRepository;
import net.dmcollection.model.card.Civilization;
import net.dmcollection.model.card.OfficialSet;
import net.dmcollection.model.card.SpeciesRepository;
import org.springframework.stereotype.Component;

@Component
public class CardService {

  private final SetService setService;
  private final CardRepository cardRepository;
  private final SpeciesRepository speciesRepository;
  private final ImageService imageService;

  public CardService(
      SetService setService,
      CardRepository cardRepository,
      SpeciesRepository speciesRepository,
      ImageService imageService) {
    this.setService = setService;
    this.cardRepository = cardRepository;
    this.speciesRepository = speciesRepository;
    this.imageService = imageService;
  }

  public record CardStub(
      Long id,
      String dmId,
      String idText,
      Set<Civilization> civilizations,
      List<String> imagePaths,
      int amount) {}

  public record CardDto(
      Long id,
      String dmId,
      String idText,
      String rarity,
      SetDto set,
      Set<String> civilizations,
      List<CardFacetDto> facets) {}

  public record SetDto(Long id, String idText, String name) {}

  public record CardFacetDto(
      Integer position,
      String name,
      String cost,
      List<String> civilizations,
      String power,
      String type,
      List<String> species,
      String imagePath) {}

  public List<CardStub> getByIds(List<Long> cardIds) {
    List<CardEntity> cards = cardRepository.findAllById(cardIds);
    return cards.stream().map(this::fromCardEntity).toList();
  }

  public List<CardStub> fromCollectionCards(Set<CollectionCards> cards) {
    Map<Long, Integer> amountById =
        cards.stream()
            .filter(card -> card.id().getId() != null)
            .collect(Collectors.toMap(card -> card.id().getId(), CollectionCards::amount));

    return cardRepository.findAllById(amountById.keySet()).stream()
        .map(card -> fromCardEntity(card, amountById.get(card.id())))
        .toList();
  }

  public List<CardDto> getCards(Set<CollectionCards> collectionCards) {
    return cardRepository
        .findAllById(
            collectionCards.stream()
                .filter(c -> c.id().getId() != null)
                .map(c -> c.id().getId())
                .toList())
        .stream()
        .filter(c -> c.set().getId() != null)
        .map(c -> fromCardEntity(c, setService.getSet(c.set().getId()).orElse(null)))
        .toList();
  }

  private CardStub fromCardEntity(CardEntity card) {
    return this.fromCardEntity(card, 0);
  }

  private CardStub fromCardEntity(CardEntity card, int amount) {
    List<String> images = null;
    Set<Civilization> civilizations = null;
    if (card.facets() != null) {
      images =
          card.facets().stream()
              .map(CardFacet::imageFilename)
              .map(imageService::makeImageUrl)
              .filter(Objects::nonNull)
              .toList();
      civilizations =
          card.facets().stream()
              .flatMap(f -> f.getCivs().stream())
              .collect(Collectors.toUnmodifiableSet());
    }
    return new CardStub(card.id(), card.officialId(), card.idText(), civilizations, images, amount);
  }

  public boolean cardExists(Long id) {
    return cardRepository.existsById(id);
  }

  public Optional<CardDto> getCardDto(String dmId) {
    var cardEntity = cardRepository.findByOfficialId(dmId);
    if (cardEntity.isPresent()) {
      var card = cardEntity.get();
      OfficialSet set = null;
      if (card.set() != null && card.set().getId() != null) {
        set = setService.getSet(card.set().getId()).orElse(null);
      }
      return Optional.of(fromCardEntity(card, set));
    }
    return Optional.empty();
  }

  private CardDto fromCardEntity(CardEntity cardEntity, OfficialSet set) {
    SetDto setDto;
    if (set != null) {
      setDto = new SetDto(set.id(), set.dmId(), set.name());
    } else {
      setDto = new SetDto(null, null, null);
    }

    var facets = cardEntity.facets();

    if (facets.isEmpty()) {
      return new CardDto(
          cardEntity.id(), cardEntity.officialId(), null, null, setDto, Set.of(), null);
    }

    var facetDtos =
        facets.stream()
            .map(
                facet -> {
                  List<String> species = new ArrayList<>();
                  if (facet.species() != null && !facet.species().isEmpty()) {
                    speciesRepository
                        .findAllById(facet.species().stream().map(fs -> fs.id().getId()).toList())
                        .forEach(s -> species.add(htmlEscape(s.species(), "UTF-8")));
                  }
                  return new CardFacetDto(
                      facet.position(),
                      facet.name(),
                      facet.cost() == null ? null : facet.cost().toString(),
                      facet.getCivs().stream()
                          .sorted(Comparator.comparingInt(Civilization::ordinal))
                          .map(Civilization::toString)
                          .toList(),
                      facet.powerText() == null ? null : facet.powerText().value(),
                      facet.type() == null ? null : htmlEscape(facet.type(), "UTF-8"),
                      species,
                      imageService.makeImageUrl(facet.imageFilename()));
                })
            .toList();
    return new CardDto(
        cardEntity.id(),
        htmlEscape(cardEntity.officialId(), "UTF-8"),
        htmlEscape(cardEntity.idText(), "UTF-8"),
        cardEntity.rarityCode() == null ? null : cardEntity.rarityCode().name(),
        setDto,
        cardEntity.facets().stream()
            .flatMap(f -> f.getCivs().stream())
            .map(Civilization::toString)
            .collect(Collectors.toUnmodifiableSet()),
        facetDtos);
  }
}
