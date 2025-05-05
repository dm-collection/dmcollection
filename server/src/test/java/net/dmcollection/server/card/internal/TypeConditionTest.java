package net.dmcollection.server.card.internal;

import static org.assertj.core.api.Assertions.assertThat;

import net.dmcollection.server.card.internal.SearchFilter.CardType;
import org.junit.jupiter.api.Test;

class TypeConditionTest {

  private final TypeCondition condition = new TypeCondition("TYPE");

  @Test
  void forCreature() {
    assertThat(condition.forType(CardType.CREATURE))
        .isEqualTo(
            "(TYPE LIKE '%クリーチャー%' AND NOT (TYPE LIKE '%進化%' OR TYPE LIKE '%クロスギア%' OR TYPE LIKE '%サイキック%' OR TYPE LIKE '%ドラグハート%' OR TYPE LIKE '%GR%'))");
  }

  @Test
  void forCastle() {
    assertThat(condition.forType(CardType.CASTLE)).isEqualTo("TYPE = '城'");
  }
}
