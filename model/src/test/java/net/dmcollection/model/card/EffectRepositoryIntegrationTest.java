package net.dmcollection.model.card;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;
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
    var effect = new Effect(BLOCKER);
    var saved = repository.save(effect);
    assertThat(saved.id()).isNotNull();
    assertThat(saved.text()).isEqualTo(BLOCKER);
  }

  @Test
  void effectWithChildCanBeSaved() {
    var child1 = new Effect(0, "カードを１枚引く。");
    var child2 = new Effect(1, "GR召喚する。");
    var child3 = new Effect(2, "相手のクリーチャーを１体選び、持ち主の手札に戻す。");

    var parent = new Effect("次の中から２回選ぶ。（同じものを選んでもよい）", List.of(child1, child2, child3));
    var saved = repository.findById(repository.save(parent).id()).get();
    assertThat(saved.id()).isNotNull();
    assertThat(saved.children()).hasSize(3);
    assertThat(saved.children().stream().map(Effect::text))
        .containsExactly(child1.text(), child2.text(), child3.text());
  }

  @Test
  void findByText() {
    var effect = new Effect(BLOCKER);
    repository.save(effect);
    var found = repository.findByText(BLOCKER);
    assertThat(found).isNotEmpty();
  }
}
