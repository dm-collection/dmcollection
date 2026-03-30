package net.dmcollection.server.card;

import static net.dmcollection.server.jooq.generated.tables.Ability.ABILITY;
import static net.dmcollection.server.jooq.generated.tables.Card.CARD;
import static net.dmcollection.server.jooq.generated.tables.CardSet.CARD_SET;
import static net.dmcollection.server.jooq.generated.tables.CardSide.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.tables.CardSideCardType.CARD_SIDE_CARD_TYPE;
import static net.dmcollection.server.jooq.generated.tables.CardSideRace.CARD_SIDE_RACE;
import static net.dmcollection.server.jooq.generated.tables.CardType.CARD_TYPE;
import static net.dmcollection.server.jooq.generated.tables.Printing.PRINTING;
import static net.dmcollection.server.jooq.generated.tables.PrintingSide.PRINTING_SIDE;
import static net.dmcollection.server.jooq.generated.tables.PrintingSideAbility.PRINTING_SIDE_ABILITY;
import static net.dmcollection.server.jooq.generated.tables.Race.RACE;
import static net.dmcollection.server.jooq.generated.tables.Rarity.RARITY;
import static org.springframework.web.util.HtmlUtils.htmlEscape;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.dmcollection.model.card.Civilization;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class CardService {

  private final DSLContext dsl;
  private final ImageService imageService;

  public CardService(DSLContext dsl, ImageService imageService) {
    this.dsl = dsl;
    this.imageService = imageService;
  }

  public record CardStub(
      Long id,
      String dmId,
      String idText,
      Set<Civilization> civilizations,
      List<String> imagePaths,
      int amount,
      int collectionAmount) {}

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
      List<EffectDto> effects,
      String imagePath) {}

  public record EffectDto(String text, int position, List<ChildEffectDto> children) {}

  public record ChildEffectDto(String text, int position) {}

  private record SideData(Short[] civilizationIds, String imageFilename) {}

  private record AbilityRow(String text, short position, short indentLevel) {}

  public List<CardStub> getByIds(List<Long> printingIds) {
    List<Integer> ids = printingIds.stream().map(Long::intValue).toList();

    record PrintingRow(int printingId, String officialSiteId, String collectorNumber) {}

    Map<Integer, PrintingRow> printings = new LinkedHashMap<>();
    dsl.select(PRINTING.ID, PRINTING.OFFICIAL_SITE_ID, PRINTING.COLLECTOR_NUMBER)
        .from(PRINTING)
        .where(PRINTING.ID.in(ids))
        .forEach(
            r ->
                printings.put(
                    r.get(PRINTING.ID),
                    new PrintingRow(
                        r.get(PRINTING.ID),
                        r.get(PRINTING.OFFICIAL_SITE_ID),
                        r.get(PRINTING.COLLECTOR_NUMBER))));

    if (printings.isEmpty()) {
      return List.of();
    }

    Map<Integer, List<SideData>> sidesByPrinting = fetchSideData(printings.keySet());

    List<CardStub> result = new ArrayList<>(printings.size());
    for (PrintingRow row : printings.values()) {
      List<SideData> sides = sidesByPrinting.getOrDefault(row.printingId(), List.of());
      result.add(
          new CardStub(
              (long) row.printingId(),
              row.officialSiteId(),
              row.collectorNumber(),
              collectCivilizations(sides),
              collectImageUrls(sides),
              0,
              0));
    }
    return result;
  }

  public boolean cardExists(Long id) {
    return dsl.fetchExists(dsl.selectOne().from(PRINTING).where(PRINTING.ID.eq(id.intValue())));
  }

  public Optional<CardDto> getCardDto(String dmId) {
    // Phase 1: Main printing data
    var printingRecord =
        dsl.select(
                PRINTING.ID,
                PRINTING.OFFICIAL_SITE_ID,
                PRINTING.COLLECTOR_NUMBER,
                CARD_SET.ID,
                CARD_SET.CODE,
                CARD_SET.NAME,
                RARITY.NAME)
            .from(PRINTING)
            .join(CARD)
            .on(CARD.ID.eq(PRINTING.CARD_ID))
            .join(CARD_SET)
            .on(CARD_SET.ID.eq(PRINTING.SET_ID))
            .leftJoin(RARITY)
            .on(RARITY.ID.eq(PRINTING.RARITY_ID))
            .where(PRINTING.OFFICIAL_SITE_ID.eq(dmId))
            .fetchOne();

    if (printingRecord == null) {
      return Optional.empty();
    }

    int printingId = printingRecord.get(PRINTING.ID);
    String officialSiteId = printingRecord.get(PRINTING.OFFICIAL_SITE_ID);
    String collectorNumber = printingRecord.get(PRINTING.COLLECTOR_NUMBER);
    String rarityName = printingRecord.get(RARITY.NAME);
    SetDto setDto =
        new SetDto(
            printingRecord.get(CARD_SET.ID).longValue(),
            printingRecord.get(CARD_SET.CODE),
            printingRecord.get(CARD_SET.NAME));

    // Phase 2: Side data (card_side + printing_side)
    record SideRow(
        int cardSideId,
        int printingSideId,
        short sideOrder,
        String name,
        Integer cost,
        boolean costIsInfinity,
        Integer power,
        boolean powerIsInfinity,
        String powerModifier,
        Short[] civilizationIds,
        String imageFilename) {}

    List<SideRow> sideRows =
        dsl.select(
                CARD_SIDE.ID,
                PRINTING_SIDE.ID,
                CARD_SIDE.SIDE_ORDER,
                CARD_SIDE.NAME,
                CARD_SIDE.COST,
                CARD_SIDE.COST_IS_INFINITY,
                CARD_SIDE.POWER,
                CARD_SIDE.POWER_IS_INFINITY,
                CARD_SIDE.POWER_MODIFIER,
                CARD_SIDE.CIVILIZATION_IDS,
                PRINTING_SIDE.IMAGE_FILENAME)
            .from(PRINTING_SIDE)
            .join(CARD_SIDE)
            .on(CARD_SIDE.ID.eq(PRINTING_SIDE.CARD_SIDE_ID))
            .where(PRINTING_SIDE.PRINTING_ID.eq(printingId))
            .orderBy(CARD_SIDE.SIDE_ORDER)
            .fetch(
                r ->
                    new SideRow(
                        r.get(CARD_SIDE.ID),
                        r.get(PRINTING_SIDE.ID),
                        r.get(CARD_SIDE.SIDE_ORDER),
                        r.get(CARD_SIDE.NAME),
                        r.get(CARD_SIDE.COST),
                        r.get(CARD_SIDE.COST_IS_INFINITY),
                        r.get(CARD_SIDE.POWER),
                        r.get(CARD_SIDE.POWER_IS_INFINITY),
                        r.get(CARD_SIDE.POWER_MODIFIER),
                        r.get(CARD_SIDE.CIVILIZATION_IDS),
                        r.get(PRINTING_SIDE.IMAGE_FILENAME)));

    if (sideRows.isEmpty()) {
      return Optional.of(
          new CardDto(
              (long) printingId,
              htmlEscape(officialSiteId, "UTF-8"),
              null,
              rarityName,
              setDto,
              Set.of(),
              null));
    }

    List<Integer> cardSideIds = sideRows.stream().map(SideRow::cardSideId).toList();
    List<Integer> printingSideIds = sideRows.stream().map(SideRow::printingSideId).toList();

    // Phase 3: Card types per side
    Map<Integer, List<String>> typesBySide = new LinkedHashMap<>();
    dsl.select(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID, CARD_TYPE.NAME)
        .from(CARD_SIDE_CARD_TYPE)
        .join(CARD_TYPE)
        .on(CARD_TYPE.ID.eq(CARD_SIDE_CARD_TYPE.CARD_TYPE_ID))
        .where(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.in(cardSideIds))
        .orderBy(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID, CARD_SIDE_CARD_TYPE.POSITION)
        .forEach(
            r ->
                typesBySide
                    .computeIfAbsent(
                        r.get(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID), k -> new ArrayList<>())
                    .add(r.get(CARD_TYPE.NAME)));

    // Phase 4: Races per side
    Map<Integer, List<String>> racesBySide = new LinkedHashMap<>();
    dsl.select(CARD_SIDE_RACE.CARD_SIDE_ID, RACE.NAME)
        .from(CARD_SIDE_RACE)
        .join(RACE)
        .on(RACE.ID.eq(CARD_SIDE_RACE.RACE_ID))
        .where(CARD_SIDE_RACE.CARD_SIDE_ID.in(cardSideIds))
        .orderBy(CARD_SIDE_RACE.CARD_SIDE_ID, CARD_SIDE_RACE.POSITION)
        .forEach(
            r ->
                racesBySide
                    .computeIfAbsent(r.get(CARD_SIDE_RACE.CARD_SIDE_ID), k -> new ArrayList<>())
                    .add(r.get(RACE.NAME)));

    // Phase 5: Abilities per printing side
    Map<Integer, List<AbilityRow>> abilitiesByPrintingSide = new LinkedHashMap<>();
    dsl.select(
            PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID,
            ABILITY.TEXT,
            PRINTING_SIDE_ABILITY.POSITION,
            PRINTING_SIDE_ABILITY.INDENT_LEVEL)
        .from(PRINTING_SIDE_ABILITY)
        .join(ABILITY)
        .on(ABILITY.ID.eq(PRINTING_SIDE_ABILITY.ABILITY_ID))
        .where(PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID.in(printingSideIds))
        .orderBy(PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID, PRINTING_SIDE_ABILITY.POSITION)
        .forEach(
            r ->
                abilitiesByPrintingSide
                    .computeIfAbsent(
                        r.get(PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID), k -> new ArrayList<>())
                    .add(
                        new AbilityRow(
                            r.get(ABILITY.TEXT),
                            r.get(PRINTING_SIDE_ABILITY.POSITION),
                            r.get(PRINTING_SIDE_ABILITY.INDENT_LEVEL))));

    // Assemble facets
    Set<String> allCivilizations = new java.util.LinkedHashSet<>();
    List<CardFacetDto> facets = new ArrayList<>(sideRows.size());

    for (SideRow side : sideRows) {
      List<String> civNames = civilizationNames(side.civilizationIds());
      allCivilizations.addAll(civNames);

      String typeStr = null;
      List<String> types = typesBySide.get(side.cardSideId());
      if (types != null && !types.isEmpty()) {
        typeStr = htmlEscape(String.join("／", types), StandardCharsets.UTF_8.name());
      }

      List<String> races =
          racesBySide.getOrDefault(side.cardSideId(), List.of()).stream()
              .map(name -> htmlEscape(name, StandardCharsets.UTF_8.name()))
              .toList();

      List<EffectDto> effects =
          buildEffects(abilitiesByPrintingSide.getOrDefault(side.printingSideId(), List.of()));

      facets.add(
          new CardFacetDto(
              (int) side.sideOrder(),
              side.name(),
              formatCost(side.cost(), side.costIsInfinity()),
              civNames,
              formatPower(side.power(), side.powerIsInfinity(), side.powerModifier()),
              typeStr,
              races,
              effects,
              imageService.makeImageUrl(side.imageFilename())));
    }

    return Optional.of(
        new CardDto(
            (long) printingId,
            htmlEscape(officialSiteId, "UTF-8"),
            collectorNumber != null ? htmlEscape(collectorNumber, "UTF-8") : null,
            rarityName,
            setDto,
            allCivilizations,
            facets));
  }

  // -- Helper methods --

  private Map<Integer, List<SideData>> fetchSideData(Collection<Integer> printingIds) {
    Map<Integer, List<SideData>> result = new LinkedHashMap<>();
    dsl.select(PRINTING.ID, CARD_SIDE.CIVILIZATION_IDS, PRINTING_SIDE.IMAGE_FILENAME)
        .from(PRINTING_SIDE)
        .join(PRINTING)
        .on(PRINTING.ID.eq(PRINTING_SIDE.PRINTING_ID))
        .join(CARD_SIDE)
        .on(CARD_SIDE.ID.eq(PRINTING_SIDE.CARD_SIDE_ID))
        .where(PRINTING.ID.in(printingIds))
        .orderBy(PRINTING.ID, CARD_SIDE.SIDE_ORDER)
        .forEach(
            r ->
                result
                    .computeIfAbsent(r.get(PRINTING.ID), k -> new ArrayList<>())
                    .add(
                        new SideData(
                            r.get(CARD_SIDE.CIVILIZATION_IDS),
                            r.get(PRINTING_SIDE.IMAGE_FILENAME))));
    return result;
  }

  private static Set<Civilization> collectCivilizations(List<SideData> sides) {
    Set<Civilization> civilizations = EnumSet.noneOf(Civilization.class);
    for (SideData side : sides) {
      if (side.civilizationIds() == null || side.civilizationIds().length == 0) {
        civilizations.add(Civilization.ZERO);
      } else {
        for (short civId : side.civilizationIds()) {
          civilizations.add(Civilization.values()[civId]);
        }
      }
    }
    return civilizations;
  }

  private static List<String> collectImageUrls(List<SideData> sides) {
    return sides.stream()
        .map(SideData::imageFilename)
        .filter(Objects::nonNull)
        .map(filename -> "/image/" + filename)
        .toList();
  }

  private static List<String> civilizationNames(Short[] civilizationIds) {
    if (civilizationIds == null || civilizationIds.length == 0) {
      return List.of(Civilization.ZERO.toString());
    }
    List<String> names = new ArrayList<>(civilizationIds.length);
    for (short civId : civilizationIds) {
      names.add(Civilization.values()[civId].toString());
    }
    return names;
  }

  private static String formatCost(Integer cost, boolean isInfinity) {
    if (isInfinity) return "∞";
    if (cost == null) return null;
    return String.valueOf(cost);
  }

  private static String formatPower(Integer power, boolean isInfinity, String modifier) {
    if (isInfinity) return "∞";
    if (power == null) return null;
    return switch (modifier) {
      case "leading_plus" -> "+" + power;
      case "trailing_plus" -> power + "+";
      case "trailing_minus" -> power + "－";
      default -> String.valueOf(power);
    };
  }

  private static List<EffectDto> buildEffects(List<AbilityRow> abilityRows) {
    if (abilityRows.isEmpty()) return List.of();

    List<EffectDto> effects = new ArrayList<>();
    String currentParentText = null;
    int currentParentPosition = 0;
    List<ChildEffectDto> currentChildren = null;

    for (AbilityRow row : abilityRows) {
      if (row.indentLevel() == 0) {
        // Flush previous parent
        if (currentParentText != null) {
          effects.add(new EffectDto(currentParentText, currentParentPosition, currentChildren));
        }
        currentParentText = row.text();
        currentParentPosition = row.position();
        currentChildren = new ArrayList<>();
      } else {
        // Child of current parent
        if (currentChildren != null) {
          currentChildren.add(new ChildEffectDto(row.text(), row.position()));
        }
      }
    }

    // Flush last parent
    if (currentParentText != null) {
      effects.add(new EffectDto(currentParentText, currentParentPosition, currentChildren));
    }

    return effects;
  }
}
