package net.dmcollection.model.card;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ActiveProfiles("test")
class EffectRepositoryIntegrationTest {
  @Autowired EffectRepository repository;

  private static final String BLOCKER =
      "[[icon:blocker]]ブロッカー(相手クリーチャーが攻撃するとき、このクリーチャーをタップして、その攻撃を阻止してよい。そのあと、その相手クリーチャーとバトルする。)";

  @Test
  void childlessEffectCanBeSaved() {
    var effect = new Effect(null, BLOCKER, null);
    var saved = repository.save(effect);
    assertThat(saved.id()).isNotNull();
    assertThat(saved.text()).isEqualTo(BLOCKER);
  }

  @Test
  void effectWithChildCanBeSaved() {
    var child1 =
        new GroupedEffect(
            0, AggregateReference.to(repository.save(new Effect(null, "カードを１枚引く。", null)).id()));
    var child2 =
        new GroupedEffect(
            1, AggregateReference.to(repository.save(new Effect(null, "GR召喚する。", null)).id()));
    var child3 =
        new GroupedEffect(
            2,
            AggregateReference.to(
                repository.save(new Effect(null, "相手のクリーチャーを１体選び、持ち主の手札に戻す。", null)).id()));

    var parent = new Effect(null, "次の中から２回選ぶ。（同じものを選んでもよい）", List.of(child1, child2, child3));
    var saved = repository.save(parent);
    assertThat(saved.id()).isNotNull();
    assertThat(saved.children()).hasSize(3).containsExactly(child1, child2, child3);
  }

  @Test
  void findByText() {
    var effect = new Effect(null, BLOCKER, null);
    repository.save(effect);
    var found = repository.findByText(BLOCKER);
    assertThat(found).isNotEmpty();
  }
}
