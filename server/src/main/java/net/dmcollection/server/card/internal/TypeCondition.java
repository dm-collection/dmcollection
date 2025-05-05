package net.dmcollection.server.card.internal;

import static net.dmcollection.server.card.internal.SearchFilter.CardType.AURA;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.CASTLE;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.CREATURE;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.CROSSGEAR;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.DRAGHEART;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.EVOLUTION;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.EXILE;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.FIELD;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.GACHALLENGE;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.PSYCHIC;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.SPELL;
import static net.dmcollection.server.card.internal.SearchFilter.CardType.TAMASEED;

import java.util.Arrays;
import net.dmcollection.server.card.internal.SearchFilter.CardType;

public class TypeCondition {

  private final String columnReference;

  public TypeCondition(String columnReference) {
    this.columnReference = columnReference;
  }

  public String forType(CardType cardType) {
    if (cardType == null) {
      return "";
    }
    return switch (cardType) {
      case CREATURE ->
          and(
              contains(CREATURE),
              containsNoneOf(EVOLUTION, CROSSGEAR, PSYCHIC, DRAGHEART, GACHALLENGE));
      case SPELL -> contains(SPELL);
      case EVOLUTION -> and(contains(EVOLUTION), containsNoneOf(CROSSGEAR, PSYCHIC, DRAGHEART));
      case PSYCHIC -> contains(PSYCHIC);
      case DRAGHEART -> contains(DRAGHEART);
      case FIELD -> contains(FIELD);
      case CASTLE -> isEqualTo(CASTLE);
      case CROSSGEAR -> contains(CROSSGEAR);
      case EXILE -> contains(EXILE);
      case GACHALLENGE -> contains(GACHALLENGE);
      case AURA -> contains(AURA);
      case TAMASEED -> contains(TAMASEED);
      case OTHER ->
          containsNoneOf(
              CREATURE,
              SPELL,
              EVOLUTION,
              PSYCHIC,
              DRAGHEART,
              FIELD,
              CASTLE,
              CROSSGEAR,
              EXILE,
              GACHALLENGE,
              AURA,
              TAMASEED);
    };
  }

  private String isEqualTo(CardType type) {
    return columnReference + " = '" + type.getKeyWord() + "'";
  }

  private String containsNoneOf(CardType... types) {
    if (types.length == 1) {
      return "NOT " + contains(types[0]);
    }
    return "NOT (" + String.join(" OR ", Arrays.stream(types).map(this::contains).toList()) + ")";
  }

  private String and(String condition1, String condition2) {
    return "(" + condition1 + " AND " + condition2 + ")";
  }

  private String contains(CardType cardType) {
    return columnReference + " LIKE '%" + cardType.getKeyWord() + "%'";
  }
}
