package net.dmcollection.model.card;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ActiveProfiles("test")
class CardRepositoryIntegrationTest {
  @Autowired SpeciesRepository speciesRepository;

  @Autowired EffectRepository effectRepository;

  @Autowired CardRepository cardRepository;

  @Autowired SetRepository setRepository;

  @Test
  void saveAndRetrieve() {
    OfficialSet set =
        setRepository.save(
            new OfficialSet(
                null, "dmbd13", "DMBD-13 クロニクル最終決戦デッキ 覚醒流星譚", LocalDate.of(2020, 8, 22)));
    Species hunter = makeSpecies("ハンター");
    Effect doubleBreaker = makeEffect("W・ブレイカー");
    CardEntity card =
        new CardEntity(
            null,
            "dmbd13-001",
            "DMBD13 1/26",
            List.of(
                makeBlueSide(hunter),
                makeRedSide(hunter),
                makeGreenSide(hunter, doubleBreaker),
                makeRainbowSide(hunter, doubleBreaker)),
            AggregateReference.to(set.id()),
            false,
            null);

    cardRepository.save(card);
    assertThat(cardRepository.findByOfficialId("dmbd13-001")).isPresent();
  }

  private CardFacet makeBlueSide(Species hunter) {
    Species blue = makeSpecies("ブルー・コマンド・ドラゴン");
    Effect cip =
        makeEffect(
            "このクリーチャーがバトルゾーンに出た時、カードを１枚引く。 "
                + "その後、相手のクリーチャーを１体選ぶ。次の自分のターンのはじめまで、そのクリーチャーは攻撃もブロックもできない。");
    Effect awakening =
        makeEffect(
            "覚醒 ：自分のターンのはじめに、バトルゾーンに自分の火と自然のサイキック・クリーチャーがそれぞれ１体以上あれば、" + "このクリーチャーをコストの大きいほうに裏返す。");
    return new CardFacet(
        null,
        0,
        "激浪のリュウセイ・スプラッシュ",
        CardCost.parseCost(7),
        List.of(Civilization.WATER.ordinal()),
        5000,
        5000,
        new Power("5000"),
        "サイキック・クリーチャー",
        List.of(
            new FacetSpecies(0, AggregateReference.to(blue.id())),
            new FacetSpecies(1, AggregateReference.to(hunter.id()))),
        List.of(
            new FacetEffect(0, AggregateReference.to(cip.id())),
            new FacetEffect(1, AggregateReference.to(awakening.id()))),
        "dmbd13-001a.jpg");
  }

  CardFacet makeRedSide(Species hunter) {
    Species red = makeSpecies("レッド・コマンド・ドラゴン");
    Effect cip =
        makeEffect(
            "このクリーチャーがバトルゾーンに出た時、相手は自身の山札の上から３枚を表向きにする。そのうちの１枚のコスト以下のコストを持つ相手のクリーチャーを１体破壊する。その後、相手は表向きにした３枚を好きな順序で山札の一番下に置く。");
    Effect awakening =
        makeEffect(
            "覚醒 ：自分のターンのはじめに、バトルゾーンに自分の水と自然のサイキック・クリーチャーがそれぞれ１体以上あれば、このクリーチャーをコストの大きいほうに裏返す。");
    return new CardFacet(
        null,
        1,
        "灼熱のリュウセイ・ボルケーノ",
        CardCost.parseCost(7),
        List.of(Civilization.FIRE.ordinal()),
        5000,
        5000,
        new Power("5000"),
        "サイキック・クリーチャー",
        List.of(
            new FacetSpecies(0, AggregateReference.to(red.id())),
            new FacetSpecies(1, AggregateReference.to(hunter.id()))),
        List.of(
            new FacetEffect(0, AggregateReference.to(cip.id())),
            new FacetEffect(1, AggregateReference.to(awakening.id()))),
        "dmbd13-001b.jpg");
  }

  private CardFacet makeGreenSide(Species hunter, Effect doubleBreaker) {
    Species green = makeSpecies("グリーン・コマンド・ドラゴン");
    Effect cip =
        makeEffect("このクリーチャーがバトルゾーンに出た時、自分の山札の上から１枚目をマナゾーンに置く。その後、カードを１枚、自分のマナゾーンから手札に戻してもよい。");
    Effect awakening =
        makeEffect(
            "覚醒 ：自分のターンのはじめに、バトルゾーンに自分の水と火のサイキック・クリーチャーがそれぞれ１体以上あれば、このクリーチャーをコストの大きいほうに裏返す。");
    return new CardFacet(
        null,
        2,
        "大地のリュウセイ・ガイア",
        CardCost.parseCost(7),
        List.of(Civilization.NATURE.ordinal()),
        7000,
        7000,
        new Power("7000"),
        "サイキック・クリーチャー",
        List.of(
            new FacetSpecies(0, AggregateReference.to(green.id())),
            new FacetSpecies(1, AggregateReference.to(hunter.id()))),
        List.of(
            new FacetEffect(0, AggregateReference.to(doubleBreaker.id())),
            new FacetEffect(1, AggregateReference.to(cip.id())),
            new FacetEffect(2, AggregateReference.to(awakening.id()))),
        "dmbd13-001c.jpg");
  }

  private CardFacet makeRainbowSide(Species hunter, Effect doubleBreaker) {
    Species rainbow = makeSpecies("レインボー・コマンド・ドラゴン");
    Effect attack =
        makeEffect(
            "このクリーチャーが攻撃する時、カードを１枚引き、自分の山札の上から１枚目をマナゾーンに置く。その後、相手のクリーチャーを１体選んでもよい。その選んだクリーチャーとこのクリーチャーをバトルさせる。");
    Effect parallelRelease =
        makeEffect("パラレル解除（このクリーチャーがバトルゾーンを離れる時、かわりにこのクリーチャーを裏返し、その３体のうちの１体にする）");
    return new CardFacet(
        null,
        3,
        "真羅万龍 リュウセイ・ザ・ファイナル",
        CardCost.parseCost(21),
        List.of(
            Civilization.WATER.ordinal(),
            Civilization.FIRE.ordinal(),
            Civilization.NATURE.ordinal()),
        11000,
        11000,
        new Power("11000"),
        "サイキック・クリーチャー",
        List.of(
            new FacetSpecies(0, AggregateReference.to(rainbow.id())),
            new FacetSpecies(1, AggregateReference.to(hunter.id()))),
        List.of(
            new FacetEffect(0, AggregateReference.to(doubleBreaker.id())),
            new FacetEffect(1, AggregateReference.to(attack.id())),
            new FacetEffect(2, AggregateReference.to(parallelRelease.id()))),
        "dmbd13-001d.jpg");
  }

  private Species makeSpecies(String species) {
    return speciesRepository.save(new Species(null, species));
  }

  private Effect makeEffect(String text) {
    return effectRepository.save(new Effect(text));
  }
}
