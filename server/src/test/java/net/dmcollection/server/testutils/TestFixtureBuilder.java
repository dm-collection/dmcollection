package net.dmcollection.server.testutils;

import static net.dmcollection.server.card.Civilization.DARK;
import static net.dmcollection.server.card.Civilization.FIRE;
import static net.dmcollection.server.card.Civilization.LIGHT;
import static net.dmcollection.server.card.Civilization.NATURE;
import static net.dmcollection.server.card.Civilization.WATER;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SET;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dmcollection.server.card.CardService.PrintingStub;
import net.dmcollection.server.card.Civilization;
import net.dmcollection.server.card.RarityCode;
import net.dmcollection.server.card.internal.query.CardTypeResolver;
import org.jooq.DSLContext;

public class TestFixtureBuilder {

  public static final String DEFAULT_PRODUCT_TYPE = "ブースターパック";
  public static final String DEFAULT_SET_CODE = "dm01";
  public static final String DEFAULT_SET_GROUP = "all";
  public static final String PSYCHIC_CREATURE = "サイキック・クリーチャー";
  public static final String CREATURE = "クリーチャー";
  public static final String EVOLUTION_CREATURE = "進化クリーチャー";
  public static final String SPELL = "呪文";
  public static final String D2_FIELD = "D2フィールド";
  public static final String TWINPACT_SEPARATOR = "／";

  private final DbWriter dbWriter;
  private final DSLContext db;
  private final CardTypeResolver cardTypeResolver;

  public TestFixtureBuilder(DSLContext db, CardTypeResolver cardTypeResolver) {
    this.db = db;
    this.dbWriter = new DbWriter(db);
    this.cardTypeResolver = cardTypeResolver;
  }

  public TestCardBuilder testCard(String printingId) {
    return new TestCardBuilder(printingId);
  }

  public int getSetId(String setCode) {
    Integer id =
        db.select(CARD_SET.ID)
            .from(CARD_SET)
            .where(CARD_SET.CODE.eq(setCode))
            .fetchOne(CARD_SET.ID);
    if (id == null) {
      throw new IllegalArgumentException("No set with code " + setCode);
    }
    return id;
  }

  public PrintingStub createFourSides() {
    return this.testCard("dmbd13-001")
        .withSetCode("dmbd13")
        .firstSide(
            side ->
                side.withName("激浪のリュウセイ・スプラッシュ")
                    .water()
                    .cost(7)
                    .power(5000)
                    .races("ブルー・コマンド・ドラゴン", "ハンター")
                    .withAbility(
                        "このクリーチャーがバトルゾーンに出た時、カードを１枚引く。その後、相手のクリーチャーを１体選ぶ。次の自分のターンのはじめまで、そのクリーチャーは攻撃もブロックもできない。")
                    .withAbility(
                        "覚醒 ：自分のターンのはじめに、バトルゾーンに自分の火と自然のサイキック・クリーチャーがそれぞれ１体以上あれば、このクリーチャーをコストの大きいほうに裏返す。"))
        .secondSide(
            side ->
                side.withName("灼熱のリュウセイ・ボルケーノ")
                    .fire()
                    .cost(7)
                    .power(5000)
                    .races("レッド・コマンド・ドラゴン", "ハンター")
                    .withAbility(
                        "このクリーチャーがバトルゾーンに出た時、相手は自身の山札の上から３枚を表向きにする。そのうちの１枚のコスト以下のコストを持つ相手のクリーチャーを１体破壊する。その後、相手は表向きにした３枚を好きな順序で山札の一番下に置く。")
                    .withAbility(
                        "覚醒 ：自分のターンのはじめに、バトルゾーンに自分の水と自然のサイキック・クリーチャーがそれぞれ１体以上あれば、このクリーチャーをコストの大きいほうに裏返す。"))
        .thirdSide(
            side ->
                side.withName("大地のリュウセイ・ガイア")
                    .nature()
                    .cost(7)
                    .power(7000)
                    .races("グリーン・コマンド・ドラゴン", "ハンター")
                    .withAbility("W・ブレイカー")
                    .withAbility(
                        "このクリーチャーがバトルゾーンに出た時、自分の山札の上から１枚目をマナゾーンに置く。その後、カードを１枚、自分のマナゾーンから手札に戻してもよい。")
                    .withAbility(
                        "覚醒 ：自分のターンのはじめに、バトルゾーンに自分の水と火のサイキック・クリーチャーがそれぞれ１体以上あれば、このクリーチャーをコストの大きいほうに裏返す。"))
        .fourthSide(
            side ->
                side.withName("真羅万龍 リュウセイ・ザ・ファイナル")
                    .water()
                    .fire()
                    .nature()
                    .cost(21)
                    .power(11000)
                    .races("レインボー・コマンド・ドラゴン", "ハンター")
                    .withAbility("W・ブレイカー")
                    .withAbility(
                        "このクリーチャーが攻撃する時、カードを１枚引き、自分の山札の上から１枚目をマナゾーンに置く。その後、相手のクリーチャーを１体選んでもよい。その選んだクリーチャーとこのクリーチャーをバトルさせる。")
                    .withAbility("パラレル解除（このクリーチャーがバトルゾーンを離れる時、かわりにこのクリーチャーを裏返し、その３体のうちの１体にする）"))
        .withAllTypes(PSYCHIC_CREATURE)
        .build();
  }

  public enum Modifier {
    TRAILING_MINUS("trailing_minus"),
    NONE("none"),
    LEADING_PLUS("leading_plus"),
    TRAILING_PLUS("trailing_plus");

    private final String value;

    Modifier(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  public static class Cost {
    public final Integer value;
    public final boolean isInfinite;

    public Cost() {
      this.value = null;
      this.isInfinite = false;
    }

    public Cost(Integer value) {
      if (value != null && value == Integer.MAX_VALUE) {
        this.isInfinite = true;
        this.value = null;
      } else {
        this.value = value;
        this.isInfinite = false;
      }
    }
  }

  public static class Power {
    public final Integer value;
    public final boolean isInfinite;
    public final Modifier modifier;

    public Power() {
      this.value = null;
      this.isInfinite = false;
      this.modifier = Modifier.NONE;
    }

    public Power(Integer value) {
      if (value != null && value == Integer.MAX_VALUE) {
        this.value = null;
        this.isInfinite = true;
      } else {
        this.value = value;
        this.isInfinite = false;
      }
      this.modifier = Modifier.NONE;
    }

    public Power(int value, Modifier modifier) {
      this.isInfinite = false;
      this.value = value;
      this.modifier = modifier;
    }
  }

  public class TestCardBuilder {
    private String cardName;
    private boolean twinpact;
    private String deckZone;
    private final List<SideBuilder> cardSides = new ArrayList<>();
    private final List<TestPrintingBuilder> printings = new ArrayList<>();

    private TestCardBuilder(String printingId) {
      cardSides.add(new SideBuilder(this, 0));
      printings.add(new TestPrintingBuilder());
      this.printings.getFirst().officialId = printingId;
    }

    public TestCardBuilder twinpact() {
      this.twinpact = true;
      this.cardSides.getFirst().cardTypes.add(CREATURE);
      this.secondSide(side -> side.cardTypes.add(SPELL));
      return this;
    }

    private TestCardBuilder withCivs(Civilization... civilizations) {
      this.cardSides.getFirst().withCivs(civilizations);
      return this;
    }

    public TestCardBuilder light() {
      return this.withCivs(LIGHT);
    }

    public TestCardBuilder water() {
      return this.withCivs(WATER);
    }

    public TestCardBuilder dark() {
      return this.withCivs(DARK);
    }

    public TestCardBuilder fire() {
      return this.withCivs(FIRE);
    }

    public TestCardBuilder nature() {
      return this.withCivs(NATURE);
    }

    public TestCardBuilder allCivs() {
      return this.withCivs(LIGHT, WATER, DARK, FIRE, NATURE);
    }

    public TestCardBuilder cost(Integer cost) {
      this.cardSides.getFirst().cost(cost);
      return this;
    }

    public TestCardBuilder power(Integer power) {
      this.cardSides.getFirst().power(power);
      return this;
    }

    public TestCardBuilder power(Integer power, Modifier modifier) {
      this.cardSides.getFirst().power(power, modifier);
      return this;
    }

    public TestCardBuilder type(String cardType) {
      this.cardSides.getFirst().withType(cardType);
      return this;
    }

    public TestCardBuilder creature() {
      return this.type(CREATURE);
    }

    public TestCardBuilder evolutionCreature() {
      return this.type(EVOLUTION_CREATURE);
    }

    public TestCardBuilder psychicCreature() {
      return this.type(PSYCHIC_CREATURE);
    }

    public TestCardBuilder spell() {
      return this.type(SPELL);
    }

    public TestCardBuilder withAllTypes(String cardType) {
      this.cardSides.forEach(side -> side.withType(cardType));
      return this;
    }

    public TestCardBuilder race(String race) {
      this.cardSides.getFirst().races(race);
      return this;
    }

    public TestCardBuilder withAbility(String ability) {
      this.printings.getFirst().addAbility(ability, 0);
      return this;
    }

    public TestCardBuilder withAbilities(String... abilities) {
      for (var ability : abilities) {
        this.printings.getFirst().addAbility(ability, 0);
      }
      return this;
    }

    public TestCardBuilder withChildAbility(String ability) {
      this.printings.getFirst().addChildAbility(ability, 0);
      return this;
    }

    public TestCardBuilder withChildAbilities(String... abilities) {
      for (var ability : abilities) {
        this.printings.getFirst().addChildAbility(ability, 0);
      }
      return this;
    }

    public TestCardBuilder withName(String name) {
      this.cardSides.getFirst().withName(name);
      return this;
    }

    public TestCardBuilder rarity(RarityCode rarity) {
      this.printings.forEach(printing -> printing.rarity(rarity));
      return this;
    }

    public TestCardBuilder withSetCode(String setCode) {
      this.printings.getFirst().setCode = setCode;
      return this;
    }

    public TestCardBuilder firstSide(Consumer<SideBuilder> sideProps) {
      return buildSide(sideProps, 0);
    }

    public TestCardBuilder secondSide(Consumer<SideBuilder> sideProps) {
      return buildSide(sideProps, 1);
    }

    public SideBuilder secondSide() {
      if (this.cardSides.size() < 2) {
        this.cardSides.add(new SideBuilder(this, 1));
      }
      return this.cardSides.get(1);
    }

    public TestCardBuilder thirdSide(Consumer<SideBuilder> sideProps) {
      return buildSide(sideProps, 2);
    }

    public TestCardBuilder fourthSide(Consumer<SideBuilder> sideProps) {
      return buildSide(sideProps, 3);
    }

    private TestCardBuilder buildSide(Consumer<SideBuilder> sideProps, int index) {
      while (this.cardSides.size() <= index) {
        this.cardSides.add(new SideBuilder(this, this.cardSides.size()));
      }
      if (sideProps != null) {
        sideProps.accept(this.cardSides.get(index));
      }
      return this;
    }

    private Integer sortCost() {
      return this.cardSides.getFirst().cost.isInfinite
          ? Integer.valueOf(Integer.MAX_VALUE)
          : this.cardSides.getFirst().cost.value;
    }

    private Integer sortPower() {
      return this.cardSides.getFirst().power.isInfinite
          ? Integer.valueOf(Integer.MAX_VALUE)
          : this.cardSides.getFirst().power.value;
    }

    private short sortPowerModifier() {
      return (short) this.cardSides.getFirst().power.modifier.ordinal();
    }

    private List<Set<Civilization>> sideCivs() {
      var sideCivs = new ArrayList<Set<Civilization>>();
      this.cardSides.forEach(side -> sideCivs.add(side.civilizations));
      return sideCivs;
    }

    private List<String> sideTypes() {
      return cardSides.stream().map(s -> s.cardTypes).flatMap(List::stream).toList();
    }

    public PrintingStub build() {
      return buildAll().getFirst();
    }

    public List<PrintingStub> buildAll() {
      int defaultGroupId = dbWriter.upsertSetGroup(DEFAULT_SET_GROUP, 1);

      if (deckZone == null) {
        deckZone = "main";
        if (sideTypes().contains(PSYCHIC_CREATURE)) {
          deckZone = "hyperspatial";
        }
      }
      if (this.cardName == null) {
        List<String> sideNames =
            this.cardSides.stream().map(side -> side.name).filter(Objects::nonNull).toList();
        if (sideNames.isEmpty()) {
          this.cardName = this.printings.getFirst().officialId;
        } else {
          String delimiter = this.twinpact ? TWINPACT_SEPARATOR : "/";
          this.cardName = String.join(delimiter, sideNames);
        }
      }

      int id =
          dbWriter.upsertCard(
              this.cardName,
              twinpact,
              sortCost(),
              sortPower(),
              sortPowerModifier(),
              sideCivs(),
              deckZone);
      for (var side : this.cardSides) {
        if (side.name == null) {
          if (this.cardName != null) {

            if (this.cardName.contains("/")) {
              var names = this.cardName.split("/");
              if (names.length <= side.position) {
                throw new IllegalStateException("Not enough side names in card name");
              }
              side.name = names[side.position];
            } else if (this.cardName.contains(TWINPACT_SEPARATOR)) {
              var names = this.cardName.split(TWINPACT_SEPARATOR);
              if (names.length <= side.position) {
                throw new IllegalStateException("Not enough side names in twinpact name");
              }
              side.name = names[side.position];
            } else {
              side.name = cardName;
              if (side.position > 0) {
                side.name += " side " + side.position;
              }
            }
          } else {
            side.name = printings.getFirst().officialId;
            if (side.name == null) {
              throw new IllegalStateException("Set either card name, printing id or side name");
            }
            if (side.position > 0) {
              side.name += "-side-" + side.position;
            }
          }
        }
        side.id =
            dbWriter.upsertCardSide(
                id,
                side.position,
                side.name,
                side.cost,
                side.power,
                side.civilizations,
                side.cardTypes,
                cardTypeResolver,
                side.races);
      }

      for (var printing : this.printings) {
        String setCode = printing.setCode == null ? DEFAULT_SET_CODE : printing.setCode;
        int setId =
            dbWriter.upsertSet(
                setCode,
                "Set \"" + setCode + "\"",
                LocalDate.now(),
                DEFAULT_PRODUCT_TYPE,
                defaultGroupId);
        if (printing.officialId == null) {
          printing.officialId = setCode + "-" + this.cardName;
        }
        printing.idText = printing.officialId.replace("-", " ").toUpperCase(Locale.ROOT);
        printing.id =
            dbWriter.upsertPrinting(
                id, setId, printing.officialId, printing.idText, printing.rarity);
        for (int i = 0; i < this.cardSides.size(); i++) {
          String sideLetter = this.cardSides.size() > 1 ? String.valueOf((char) (i + 'a')) : "";
          String imageFileName = printing.officialId + sideLetter + ".jpg";
          if (this.twinpact && i == 1) {
            imageFileName = null;
          } else {
            printing.imageFileNames.add(imageFileName);
          }
          int sideId =
              dbWriter.upsertPrintingSide(printing.id, this.cardSides.get(i).id, "", imageFileName);
          if (i < printing.abilities.size()) {
            for (int j = 0; j < printing.abilities.get(i).size(); j++) {
              var ability = printing.abilities.get(i).get(j);
              dbWriter.addAbility(sideId, ability.text, j, ability.indent);
            }
          }
        }
      }

      Set<Civilization> allCivilizations =
          this.cardSides.stream()
              .flatMap(
                  side ->
                      side.civilizations.isEmpty()
                          ? Stream.of(Civilization.ZERO)
                          : side.civilizations.stream())
              .collect(Collectors.toSet());

      return this.printings.stream()
          .map(
              printing ->
                  new PrintingStub(
                      printing.id,
                      printing.officialId,
                      printing.idText,
                      allCivilizations,
                      printing.imageFileNames,
                      0,
                      0))
          .toList();
    }

    public class SideBuilder {
      Set<Civilization> civilizations = EnumSet.noneOf(Civilization.class);
      String name;
      Cost cost;
      Power power;
      List<String> cardTypes = new ArrayList<>();
      List<String> races = new ArrayList<>();
      private final TestCardBuilder parent;
      final int position;
      private int id;

      public SideBuilder(TestCardBuilder parent, int position) {
        this.cost = new Cost();
        this.power = new Power();
        this.position = position;
        this.parent = parent;
      }

      public PrintingStub build() {
        return this.parent.build();
      }

      public SideBuilder withName(String name) {
        this.name = name;
        return this;
      }

      public SideBuilder cost(Integer cost) {
        this.cost = new Cost(cost);
        return this;
      }

      public SideBuilder power(Integer power) {
        this.power = new Power(power);
        return this;
      }

      public SideBuilder power(Integer power, Modifier modifier) {
        this.power = new Power(power, modifier);
        return this;
      }

      public SideBuilder withType(String type) {
        this.cardTypes.add(type);
        return this;
      }

      public SideBuilder psychicCreature() {
        return this.withType(PSYCHIC_CREATURE);
      }

      public SideBuilder races(String... race) {
        Collections.addAll(this.races, race);
        return this;
      }

      private SideBuilder withCivs(Civilization... civilizations) {
        this.civilizations.addAll(List.of(civilizations));
        return this;
      }

      public SideBuilder light() {
        return this.withCivs(LIGHT);
      }

      public SideBuilder water() {
        return this.withCivs(WATER);
      }

      public SideBuilder dark() {
        return this.withCivs(DARK);
      }

      public SideBuilder fire() {
        return this.withCivs(FIRE);
      }

      public SideBuilder nature() {
        return this.withCivs(NATURE);
      }

      public SideBuilder allCivs() {
        return this.withCivs(LIGHT, WATER, DARK, FIRE, NATURE);
      }

      public SideBuilder withAbility(String ability) {
        this.parent.printings.getFirst().addAbility(ability, this.position);
        return this;
      }

      public SideBuilder withChildAbility(String ability) {
        this.parent.printings.getFirst().addChildAbility(ability, this.position);
        return this;
      }
    }

    private static class TestPrintingBuilder {

      private int id;

      private String setCode;

      private String officialId;

      private String idText;

      private record Ability(String text, int indent) {}

      private final List<List<Ability>> abilities = new ArrayList<>();
      private final List<String> imageFileNames = new ArrayList<>();

      private RarityCode rarity;

      private TestPrintingBuilder() {}

      private void addAbility(String text, int sideIndex) {
        if (text != null) {
          while (this.abilities.size() <= sideIndex) {
            this.abilities.add(new ArrayList<>());
          }
          this.abilities.get(sideIndex).add(new Ability(text, 0));
        }
      }

      private void addChildAbility(String text, int sideIndex) {
        if (text != null) {
          if (this.abilities.size() <= sideIndex || this.abilities.get(sideIndex).isEmpty()) {
            throw new IllegalStateException("Add parent ability to side first");
          }
          this.abilities.get(sideIndex).add(new Ability(text, 1));
        }
      }

      private TestPrintingBuilder rarity(RarityCode rarity) {
        this.rarity = rarity;
        this.abilities.add(new ArrayList<>());
        return this;
      }
    }
  }
}
