package net.dmcollection.server.card.internal.query;

import net.dmcollection.server.card.internal.SearchFilter.CardType;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.dmcollection.server.jooq.generated.tables.CardType.CARD_TYPE;

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
              idsContaining(nameToId, CardType.CREATURE),
              idsContainingAny(
                  nameToId,
                  CardType.EVOLUTION,
                  CardType.CROSSGEAR,
                  CardType.PSYCHIC,
                  CardType.DRAGHEART,
                  CardType.GACHALLENGE));
      case EVOLUTION ->
          new IncludedExcluded(
              idsContaining(nameToId, CardType.EVOLUTION),
              idsContainingAny(nameToId, CardType.CROSSGEAR, CardType.PSYCHIC, CardType.DRAGHEART));
      case CASTLE -> new IncludedExcluded(idsExact(nameToId, CardType.CASTLE), Set.of());
      case OTHER -> new IncludedExcluded(Set.of(), idsForAllStandardTypes(nameToId));
      case SPELL, PSYCHIC, DRAGHEART, FIELD, CROSSGEAR, EXILE, GACHALLENGE, AURA, TAMASEED ->
          new IncludedExcluded(idsContaining(nameToId, cardType), Set.of());
    };
  }

  private static Set<Integer> idsContaining(Map<String, Integer> nameToId, CardType type) {
    String keyword = type.getKeyWord();
    return nameToId.entrySet().stream()
        .filter(e -> e.getKey().contains(keyword))
        .map(Map.Entry::getValue)
        .collect(Collectors.toSet());
  }

  private static Set<Integer> idsContainingAny(Map<String, Integer> nameToId, CardType... types) {
    return Arrays.stream(types)
        .flatMap(t -> idsContaining(nameToId, t).stream())
        .collect(Collectors.toSet());
  }

  private static Set<Integer> idsExact(Map<String, Integer> nameToId, CardType type) {
    Integer id = nameToId.get(type.getKeyWord());
    return id != null ? Set.of(id) : Set.of();
  }

  private static Set<Integer> idsForAllStandardTypes(Map<String, Integer> nameToId) {
    return Arrays.stream(CardType.values())
        .filter(t -> t != CardType.OTHER)
        .flatMap(t -> idsContaining(nameToId, t).stream())
        .collect(Collectors.toSet());
  }
}
