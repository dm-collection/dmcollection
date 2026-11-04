package net.dmcollection.server.card.internal.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.dmcollection.server.card.internal.query.NameNormalizer.SideName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NameNormalizerTest {

  @ParameterizedTest
  @CsvSource({
    // hiragana is folded to katakana
    "てんくうのしゅごしゃグラン・ギューレ, テンクウノシュゴシャグランギューレ",
    "ゔぁ, ヴァ",
    // half-width katakana and full-width ASCII are folded
    "ﾎﾞﾙｼｬｯｸ, ボルシャック",
    "ＤＥＡＴＨ, death",
    // spaces, certain punctuation and symbols are removed, long vowel marks are kept
    "'超神星DEATH・ドラゲリオン', 超神星deathドラゲリオン",
    "'「D4に敗北は許されない！」', d4ニ敗北ハ許サレナイ",
    "'大焔団 フォルクス', 大焔団フォルクス",
    "'ボルシャック＝ＮＥＸ', ボルシャックnex",
    "'ドキンダム～エックス♪', ドキンダムエックス",
    "ギューレ, ギューレ",
    "“B-發”ボンバー,b-發ボンバー",
    "Eine k'Reine,einekreine",
    "Celeb k’Reine,celebkreine",
    "'  ', ''",
    // preserved symbols
    "∞, ∞",
    "。,。",
    "⤴,⤴",
    "…,..."
  })
  void normalizes(String input, String expected) {
    assertThat(NameNormalizer.normalize(input)).isEqualTo(expected);
  }

  @Test
  void normalizesNullToEmpty() {
    assertThat(NameNormalizer.normalize(null)).isEmpty();
  }

  @Test
  void readingConcatenatesSidesAndFallsBackToName() {
    var sides = List.of(new SideName("時空の勇躍ディアナ", "じくうのゆうやくディアナ"), new SideName("エル・ディアナ", null));
    assertThat(NameNormalizer.reading(sides)).isEqualTo("ジクウノユウヤクディアナエルディアナ");
  }
}
