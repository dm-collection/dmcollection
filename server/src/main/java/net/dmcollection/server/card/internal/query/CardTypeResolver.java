package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.CardType.CARD_TYPE;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class CardTypeResolver {

  public record IncludedExcluded(Set<Integer> included, Set<Integer> excluded) {}

  private final DSLContext dsl;

  private Map<String, Integer> nameToId;

  public CardTypeResolver(DSLContext dsl) {
    this.dsl = dsl;
  }

  public void loadNameToId() {
    this.nameToId =
        dsl.select(CARD_TYPE.NAME, CARD_TYPE.ID)
            .from(CARD_TYPE)
            .fetchMap(CARD_TYPE.NAME, r -> r.get(CARD_TYPE.ID).intValue());
  }

  public IncludedExcluded resolve(CardType cardType) {
    if (nameToId == null) {
      loadNameToId();
    }
    return switch (cardType) {
      case CREATURE ->
          new IncludedExcluded(
              idsContaining(CardType.CREATURE),
              idsContainingAny(
                  CardType.EVOLUTION,
                  CardType.CROSSGEAR,
                  CardType.PSYCHIC,
                  CardType.DRAGHEART,
                  CardType.GACHALLENGE,
                  CardType.DUELMATE));
      case EVOLUTION ->
          new IncludedExcluded(
              idsContaining(CardType.EVOLUTION),
              idsContainingAny(CardType.CROSSGEAR, CardType.PSYCHIC, CardType.DRAGHEART));
      case CASTLE -> new IncludedExcluded(idsContaining(CardType.CASTLE), Set.of());
      case OTHER -> new IncludedExcluded(Set.of(), idsForAllStandardTypes());
      case SPELL -> new IncludedExcluded(idsContaining(cardType), idsContaining(CardType.DUELMATE));
      case PSYCHIC,
          DRAGHEART,
          FIELD,
          CROSSGEAR,
          EXILE,
          GACHALLENGE,
          AURA,
          TAMASEED,
          DUELIST,
          DUELMATE ->
          new IncludedExcluded(idsContaining(cardType), Set.of());
    };
  }

  private Set<Integer> idsContaining(CardType type) {
    String keyword = type.getKeyWord();
    return nameToId.entrySet().stream()
        .filter(e -> e.getKey().contains(keyword))
        .map(Map.Entry::getValue)
        .collect(Collectors.toSet());
  }

  private Set<Integer> idsContainingAny(CardType... types) {
    return Arrays.stream(types).flatMap(t -> idsContaining(t).stream()).collect(Collectors.toSet());
  }

  private Set<Integer> idsForAllStandardTypes() {
    return Arrays.stream(CardType.values())
        .filter(t -> t != CardType.OTHER)
        .flatMap(t -> idsContaining(t).stream())
        .collect(Collectors.toSet());
  }
}
