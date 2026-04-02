package net.dmcollection.server.carddata;

import static net.dmcollection.server.jooq.generated.Tables.ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.CARD;
import static net.dmcollection.server.jooq.generated.Tables.CARD_CIV_GROUP;
import static net.dmcollection.server.jooq.generated.Tables.CARD_PRIVATE_TAG;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SET;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_CARD_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_RACE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.DECK_VERSION_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.ILLUSTRATOR;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE_ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.PRODUCT_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.RACE;
import static net.dmcollection.server.jooq.generated.Tables.RARITY;
import static net.dmcollection.server.jooq.generated.Tables.SET_GROUP;
import static net.dmcollection.server.jooq.generated.Tables.WISHLIST_ENTRY;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.server.carddata.CardDataJson.CardAliasJson;
import net.dmcollection.server.carddata.CardDataJson.CardJson;
import net.dmcollection.server.carddata.CardDataJson.CardSetJson;
import net.dmcollection.server.carddata.CardDataJson.CivGroupJson;
import net.dmcollection.server.carddata.CardDataJson.PrintingJson;
import net.dmcollection.server.carddata.CardDataJson.RarityJson;
import net.dmcollection.server.carddata.CardDataJson.SetGroupJson;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardDataImportService {

  private static final Logger log = LoggerFactory.getLogger(CardDataImportService.class);

  private final DSLContext dsl;

  public CardDataImportService(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Transactional
  public void importCardData(CardDataJson data) {
    // Step 1: Process aliases
    processAliases(data.cardAliases());

    // Step 2: Lookup tables
    Map<String, Short> cardTypeIds =
        upsertSmallLookup(CARD_TYPE, CARD_TYPE.NAME, extractCardTypes(data));
    Map<String, Short> raceIds = upsertSmallLookup(RACE, RACE.NAME, extractRaces(data));
    Map<String, Short> rarityIds = upsertRarities(data.rarities());
    Map<String, Integer> illustratorIds =
        upsertIntLookup(ILLUSTRATOR, ILLUSTRATOR.NAME, extractIllustrators(data));
    Map<String, Short> productTypeIds =
        upsertSmallLookup(PRODUCT_TYPE, PRODUCT_TYPE.NAME, extractProductTypes(data));

    // Step 3: Set groups and card sets
    Map<String, Integer> setGroupIds = upsertSetGroups(data.setGroups());
    Map<String, Integer> setIds =
        upsertCardSets(data.cardSets(), setGroupIds, productTypeIds);

    // Step 4: Cards
    Map<String, Integer> cardIds = upsertCards(data.cards());

    // Step 5: Card sides
    Map<Long, Integer> cardSideIds = upsertCardSides(data.cards(), cardIds);

    // Step 6: Side junction tables
    replaceSideJunctions(data.cards(), cardIds, cardSideIds, cardTypeIds, raceIds);

    // Step 7: card_civ_group
    insertCivGroups(data.cardCivGroups(), cardIds);

    // Steps 8, 12: no-op (no keyword_abilities or public_tags in JSON)

    // Step 9: abilities (bulk upsert)
    Map<String, Integer> abilityIds = upsertAbilities(data.printings());

    // Step 10: printings
    Map<String, Integer> printingIds =
        upsertPrintings(data.printings(), cardIds, setIds, rarityIds, illustratorIds);

    // Step 11: printing_side + printing_side_ability
    replacePrintingSides(data.printings(), printingIds, cardIds, cardSideIds, abilityIds);

    // Cleanup orphaned abilities
    cleanupOrphanedAbilities();

    log.info(
        "Import complete: {} cards, {} printings", data.cards().size(), data.printings().size());
  }

  // -- Step 1: Aliases --

  private void processAliases(List<CardAliasJson> aliases) {
    if (aliases == null || aliases.isEmpty()) return;
    for (var alias : aliases) {
      Integer oldCardId =
          dsl.select(CARD.ID).from(CARD).where(CARD.NAME.eq(alias.oldName())).fetchOne(CARD.ID);
      if (oldCardId == null) continue;

      Integer survivorCardId =
          dsl.select(CARD.ID).from(CARD).where(CARD.NAME.eq(alias.newName())).fetchOne(CARD.ID);

      if (survivorCardId == null) {
        // Simple rename
        dsl.update(CARD).set(CARD.NAME, alias.newName()).where(CARD.ID.eq(oldCardId)).execute();
        log.info("Renamed card '{}' to '{}'", alias.oldName(), alias.newName());
      } else {
        // Merge: re-point references from old card to survivor, then delete old card
        mergeCards(oldCardId, survivorCardId);
        log.info(
            "Merged card '{}' (id={}) into '{}' (id={})",
            alias.oldName(),
            oldCardId,
            alias.newName(),
            survivorCardId);
      }
    }
  }

  private void mergeCards(int oldCardId, int survivorCardId) {
    // Re-point printings (no unique constraint on card_id)
    dsl.update(PRINTING)
        .set(PRINTING.CARD_ID, survivorCardId)
        .where(PRINTING.CARD_ID.eq(oldCardId))
        .execute();

    // Re-point card_private_tag (PK: card_id, private_tag_id)
    // Delete old card's tags that would conflict with survivor's existing tags
    dsl.deleteFrom(CARD_PRIVATE_TAG)
        .where(CARD_PRIVATE_TAG.CARD_ID.eq(oldCardId))
        .and(
            CARD_PRIVATE_TAG.PRIVATE_TAG_ID.in(
                dsl.select(CARD_PRIVATE_TAG.PRIVATE_TAG_ID)
                    .from(CARD_PRIVATE_TAG)
                    .where(CARD_PRIVATE_TAG.CARD_ID.eq(survivorCardId))))
        .execute();
    dsl.update(CARD_PRIVATE_TAG)
        .set(CARD_PRIVATE_TAG.CARD_ID, survivorCardId)
        .where(CARD_PRIVATE_TAG.CARD_ID.eq(oldCardId))
        .execute();

    // Re-point deck_version_entry (UNIQUE: deck_version_id, card_id, printing_id)
    mergeQuantityEntries(
        oldCardId,
        survivorCardId,
        DECK_VERSION_ENTRY,
        DECK_VERSION_ENTRY.ID,
        DECK_VERSION_ENTRY.CARD_ID,
        DECK_VERSION_ENTRY.PRINTING_ID,
        DECK_VERSION_ENTRY.QUANTITY,
        DECK_VERSION_ENTRY.DECK_VERSION_ID);

    // Re-point wishlist_entry (UNIQUE NULLS NOT DISTINCT: wishlist_id, card_id, printing_id)
    mergeQuantityEntries(
        oldCardId,
        survivorCardId,
        WISHLIST_ENTRY,
        WISHLIST_ENTRY.ID,
        WISHLIST_ENTRY.CARD_ID,
        WISHLIST_ENTRY.PRINTING_ID,
        WISHLIST_ENTRY.QUANTITY,
        WISHLIST_ENTRY.WISHLIST_ID);

    // Delete old card (CASCADE handles card_side, card_civ_group, card_keyword_ability,
    // card_public_tag, and any remaining card_private_tag)
    dsl.deleteFrom(CARD).where(CARD.ID.eq(oldCardId)).execute();
  }

  /**
   * Merges quantity-bearing entries from old card into survivor, handling unique constraint
   * conflicts by summing quantities.
   */
  private <R extends org.jooq.Record, P> void mergeQuantityEntries(
      int oldCardId,
      int survivorCardId,
      Table<R> table,
      TableField<R, Integer> idField,
      TableField<R, Integer> cardIdField,
      TableField<R, Integer> printingIdField,
      TableField<R, Integer> quantityField,
      TableField<R, P> parentIdField) {
    // Find conflicting old-card entries (same parent + printing already exists for survivor)
    var oldEntries =
        dsl.select(idField, parentIdField, printingIdField, quantityField)
            .from(table)
            .where(cardIdField.eq(oldCardId))
            .fetch();

    for (var row : oldEntries) {
      P parentId = row.get(parentIdField);
      int existingCount =
          dsl.selectCount()
              .from(table)
              .where(cardIdField.eq(survivorCardId))
              .and(parentIdField.eq(parentId))
              .and(printingIdField.isNotDistinctFrom(row.get(printingIdField)))
              .fetchOne(0, int.class);

      if (existingCount > 0) {
        // Conflict: add quantity to survivor's entry and delete old entry
        dsl.update(table)
            .set(quantityField, quantityField.plus(row.get(quantityField)))
            .where(cardIdField.eq(survivorCardId))
            .and(parentIdField.eq(parentId))
            .and(printingIdField.isNotDistinctFrom(row.get(printingIdField)))
            .execute();
        dsl.deleteFrom(table).where(idField.eq(row.get(idField))).execute();
      }
    }

    // Re-point remaining non-conflicting entries
    dsl.update(table).set(cardIdField, survivorCardId).where(cardIdField.eq(oldCardId)).execute();
  }

  // -- Step 2: Lookup tables --

  private Set<String> extractCardTypes(CardDataJson data) {
    return data.cards().stream()
        .flatMap(c -> c.sides().stream())
        .flatMap(s -> s.cardTypes().stream())
        .collect(Collectors.toSet());
  }

  private Set<String> extractRaces(CardDataJson data) {
    return data.cards().stream()
        .flatMap(c -> c.sides().stream())
        .flatMap(s -> s.races().stream())
        .collect(Collectors.toSet());
  }

  private Set<String> extractProductTypes(CardDataJson data) {
    return data.cardSets().stream()
        .map(CardSetJson::productType)
        .filter(pt -> pt != null && !pt.isEmpty())
        .collect(Collectors.toSet());
  }

  private Set<String> extractIllustrators(CardDataJson data) {
    return data.printings().stream()
        .map(PrintingJson::illustrator)
        .filter(i -> i != null && !i.isEmpty())
        .collect(Collectors.toSet());
  }

  private <R extends org.jooq.Record> Map<String, Short> upsertSmallLookup(
      Table<R> table, TableField<R, String> nameField, Set<String> names) {
    if (names.isEmpty()) return Map.of();
    Field<Short> idField = table.field("id", Short.class);
    for (String name : names) {
      dsl.insertInto(table)
          .columns(nameField)
          .values(name)
          .onConflict(nameField)
          .doNothing()
          .execute();
    }
    return dsl.select(idField, nameField)
        .from(table)
        .where(nameField.in(names))
        .fetchMap(nameField, idField);
  }

  private <R extends org.jooq.Record> Map<String, Integer> upsertIntLookup(
      Table<R> table, TableField<R, String> nameField, Set<String> names) {
    if (names.isEmpty()) return Map.of();
    Field<Integer> idField = table.field("id", Integer.class);
    for (String name : names) {
      dsl.insertInto(table)
          .columns(nameField)
          .values(name)
          .onConflict(nameField)
          .doNothing()
          .execute();
    }
    return dsl.select(idField, nameField)
        .from(table)
        .where(nameField.in(names))
        .fetchMap(nameField, idField);
  }

  private Map<String, Short> upsertRarities(List<RarityJson> rarities) {
    if (rarities == null || rarities.isEmpty()) return Map.of();
    for (var rarity : rarities) {
      dsl.insertInto(RARITY)
          .columns(RARITY.NAME, RARITY.SORT_ORDER)
          .values(rarity.name(), rarity.sortOrder())
          .onConflict(RARITY.NAME)
          .doUpdate()
          .set(RARITY.SORT_ORDER, DSL.field("excluded.sort_order", Short.class))
          .execute();
    }
    Set<String> names = rarities.stream().map(RarityJson::name).collect(Collectors.toSet());
    return dsl.select(RARITY.ID, RARITY.NAME)
        .from(RARITY)
        .where(RARITY.NAME.in(names))
        .fetchMap(RARITY.NAME, RARITY.ID);
  }

  // -- Step 3: Set groups and card sets --

  private Map<String, Integer> upsertSetGroups(List<SetGroupJson> setGroups) {
    for (var sg : setGroups) {
      dsl.insertInto(SET_GROUP)
          .columns(SET_GROUP.NAME, SET_GROUP.SORT_ORDER)
          .values(sg.name(), sg.sortOrder())
          .onConflict(SET_GROUP.NAME)
          .doUpdate()
          .set(SET_GROUP.SORT_ORDER, DSL.field("excluded.sort_order", Integer.class))
          .execute();
    }
    return dsl.select(SET_GROUP.ID, SET_GROUP.NAME)
        .from(SET_GROUP)
        .fetchMap(SET_GROUP.NAME, SET_GROUP.ID);
  }

  private Map<String, Integer> upsertCardSets(
      List<CardSetJson> cardSets,
      Map<String, Integer> setGroupIds,
      Map<String, Short> productTypeIds) {
    for (var cs : cardSets) {
      Integer setGroupId = setGroupIds.get(cs.setGroup());
      Short productTypeId = productTypeIds.get(cs.productType());
      dsl.insertInto(CARD_SET)
          .columns(
              CARD_SET.CODE,
              CARD_SET.NAME,
              CARD_SET.RELEASE_DATE,
              CARD_SET.PRODUCT_TYPE_ID,
              CARD_SET.SET_GROUP_ID)
          .values(
              cs.code(), cs.name(), LocalDate.parse(cs.releaseDate()), productTypeId, setGroupId)
          .onConflict(CARD_SET.CODE)
          .doUpdate()
          .set(CARD_SET.NAME, DSL.field("excluded.name", String.class))
          .set(CARD_SET.RELEASE_DATE, DSL.field("excluded.release_date", LocalDate.class))
          .set(CARD_SET.PRODUCT_TYPE_ID, DSL.field("excluded.product_type_id", Short.class))
          .set(CARD_SET.SET_GROUP_ID, DSL.field("excluded.set_group_id", Integer.class))
          .execute();
    }
    return dsl.select(CARD_SET.ID, CARD_SET.CODE)
        .from(CARD_SET)
        .fetchMap(CARD_SET.CODE, CARD_SET.ID);
  }

  // -- Step 4: Cards --

  private Map<String, Integer> upsertCards(List<CardJson> cards) {
    for (var card : cards) {
      Short[] sortCiv = toShortArray(card.sortCivilization());
      dsl.insertInto(CARD)
          .columns(
              CARD.NAME,
              CARD.IS_TWINPACT,
              CARD.DECK_ZONE,
              CARD.SORT_COST,
              CARD.SORT_POWER,
              CARD.SORT_POWER_MODIFIER,
              CARD.SORT_CIVILIZATION)
          .values(
              card.name(),
              card.isTwinpact(),
              card.deckZone(),
              card.sortCost(),
              card.sortPower(),
              card.sortPowerModifier(),
              sortCiv)
          .onConflict(CARD.NAME)
          .doUpdate()
          .set(CARD.IS_TWINPACT, DSL.field("excluded.is_twinpact", Boolean.class))
          .set(CARD.DECK_ZONE, DSL.field("excluded.deck_zone", String.class))
          .set(CARD.SORT_COST, DSL.field("excluded.sort_cost", Integer.class))
          .set(CARD.SORT_POWER, DSL.field("excluded.sort_power", Integer.class))
          .set(CARD.SORT_POWER_MODIFIER, DSL.field("excluded.sort_power_modifier", Short.class))
          .set(CARD.SORT_CIVILIZATION, DSL.field("excluded.sort_civilization", Short[].class))
          .execute();
    }
    return dsl.select(CARD.ID, CARD.NAME).from(CARD).fetchMap(CARD.NAME, CARD.ID);
  }

  // -- Step 5: Card sides --

  private static long cardSideKey(int cardId, int sideOrder) {
    return ((long) cardId << 32) | (sideOrder & 0xFFFFFFFFL);
  }

  private Map<Long, Integer> upsertCardSides(List<CardJson> cards, Map<String, Integer> cardIds) {
    for (var card : cards) {
      int cardId = cardIds.get(card.name());
      for (var side : card.sides()) {
        Short[] civIds = toShortArray(side.civilizationIds());
        dsl.insertInto(CARD_SIDE)
            .columns(
                CARD_SIDE.CARD_ID,
                CARD_SIDE.SIDE_ORDER,
                CARD_SIDE.NAME,
                CARD_SIDE.COST,
                CARD_SIDE.COST_IS_INFINITY,
                CARD_SIDE.POWER,
                CARD_SIDE.POWER_IS_INFINITY,
                CARD_SIDE.POWER_MODIFIER,
                CARD_SIDE.CIVILIZATION_IDS)
            .values(
                cardId,
                (short) side.sideOrder(),
                side.name(),
                side.cost(),
                side.costIsInfinity(),
                side.power(),
                side.powerIsInfinity(),
                side.powerModifier(),
                civIds)
            .onConflict(CARD_SIDE.CARD_ID, CARD_SIDE.SIDE_ORDER)
            .doUpdate()
            .set(CARD_SIDE.NAME, DSL.field("excluded.name", String.class))
            .set(CARD_SIDE.COST, DSL.field("excluded.cost", Integer.class))
            .set(CARD_SIDE.COST_IS_INFINITY, DSL.field("excluded.cost_is_infinity", Boolean.class))
            .set(CARD_SIDE.POWER, DSL.field("excluded.power", Integer.class))
            .set(
                CARD_SIDE.POWER_IS_INFINITY, DSL.field("excluded.power_is_infinity", Boolean.class))
            .set(CARD_SIDE.POWER_MODIFIER, DSL.field("excluded.power_modifier", String.class))
            .set(CARD_SIDE.CIVILIZATION_IDS, DSL.field("excluded.civilization_ids", Short[].class))
            .execute();
      }
    }
    // Fetch all card side IDs
    Map<Long, Integer> result = new HashMap<>();
    dsl.select(CARD_SIDE.ID, CARD_SIDE.CARD_ID, CARD_SIDE.SIDE_ORDER)
        .from(CARD_SIDE)
        .fetch()
        .forEach(
            r ->
                result.put(
                    cardSideKey(r.get(CARD_SIDE.CARD_ID), r.get(CARD_SIDE.SIDE_ORDER)),
                    r.get(CARD_SIDE.ID)));
    return result;
  }

  // -- Step 6: Side junction tables --

  private void replaceSideJunctions(
      List<CardJson> cards,
      Map<String, Integer> cardIds,
      Map<Long, Integer> cardSideIds,
      Map<String, Short> cardTypeIds,
      Map<String, Short> raceIds) {
    for (var card : cards) {
      int cardId = cardIds.get(card.name());
      for (var side : card.sides()) {
        int cardSideId = cardSideIds.get(cardSideKey(cardId, side.sideOrder()));

        // card_side_card_type
        dsl.deleteFrom(CARD_SIDE_CARD_TYPE)
            .where(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.eq(cardSideId))
            .execute();
        for (int i = 0; i < side.cardTypes().size(); i++) {
          dsl.insertInto(CARD_SIDE_CARD_TYPE)
              .columns(
                  CARD_SIDE_CARD_TYPE.CARD_SIDE_ID,
                  CARD_SIDE_CARD_TYPE.CARD_TYPE_ID,
                  CARD_SIDE_CARD_TYPE.POSITION)
              .values(cardSideId, cardTypeIds.get(side.cardTypes().get(i)), (short) i)
              .execute();
        }

        // card_side_race
        dsl.deleteFrom(CARD_SIDE_RACE).where(CARD_SIDE_RACE.CARD_SIDE_ID.eq(cardSideId)).execute();
        for (int i = 0; i < side.races().size(); i++) {
          dsl.insertInto(CARD_SIDE_RACE)
              .columns(CARD_SIDE_RACE.CARD_SIDE_ID, CARD_SIDE_RACE.RACE_ID, CARD_SIDE_RACE.POSITION)
              .values(cardSideId, raceIds.get(side.races().get(i)), (short) i)
              .execute();
        }
      }
    }
  }

  // -- Step 7: card_civ_group --

  private void insertCivGroups(List<CivGroupJson> civGroups, Map<String, Integer> cardIds) {
    // Delete all existing civ groups for cards being imported
    for (int cardId : cardIds.values()) {
      dsl.deleteFrom(CARD_CIV_GROUP).where(CARD_CIV_GROUP.CARD_ID.eq(cardId)).execute();
    }
    for (var group : civGroups) {
      Integer cardId = cardIds.get(group.cardName());
      if (cardId == null) continue;
      Short[] civIds = toShortArray(group.civilizationIds());
      dsl.insertInto(CARD_CIV_GROUP)
          .columns(
              CARD_CIV_GROUP.CARD_ID,
              CARD_CIV_GROUP.CIVILIZATION_IDS,
              CARD_CIV_GROUP.INCLUDES_COLORLESS_SIDE)
          .values(cardId, civIds, group.includesColorlessSide())
          .execute();
    }
  }

  // -- Step 9: Abilities --

  private Map<String, Integer> upsertAbilities(List<PrintingJson> printings) {
    // Collect all unique ability (text, search_text) pairs
    Map<String, String> abilityTexts = new LinkedHashMap<>();
    for (var printing : printings) {
      for (var side : printing.sides()) {
        if (side.abilities() != null) {
          for (var ability : side.abilities()) {
            abilityTexts.putIfAbsent(ability.text(), ability.searchText());
          }
        }
      }
    }

    if (abilityTexts.isEmpty()) return Map.of();

    // Batch upsert abilities
    for (var entry : abilityTexts.entrySet()) {
      dsl.insertInto(ABILITY)
          .columns(ABILITY.TEXT, ABILITY.SEARCH_TEXT)
          .values(entry.getKey(), entry.getValue())
          .onConflict(ABILITY.TEXT)
          .doUpdate()
          .set(ABILITY.SEARCH_TEXT, DSL.field("excluded.search_text", String.class))
          .execute();
    }

    // Fetch all ability IDs
    return dsl.select(ABILITY.ID, ABILITY.TEXT)
        .from(ABILITY)
        .where(ABILITY.TEXT.in(abilityTexts.keySet()))
        .fetchMap(ABILITY.TEXT, ABILITY.ID);
  }

  // -- Step 10: Printings --

  private Map<String, Integer> upsertPrintings(
      List<PrintingJson> printings,
      Map<String, Integer> cardIds,
      Map<String, Integer> setIds,
      Map<String, Short> rarityIds,
      Map<String, Integer> illustratorIds) {
    for (var p : printings) {
      Integer cardId = cardIds.get(p.cardName());
      Integer setId = setIds.get(p.setCode());
      Short rarityId =
          (p.rarity() != null && !p.rarity().isEmpty()) ? rarityIds.get(p.rarity()) : null;
      Integer illustratorId =
          (p.illustrator() != null && !p.illustrator().isEmpty())
              ? illustratorIds.get(p.illustrator())
              : null;

      dsl.insertInto(PRINTING)
          .columns(
              PRINTING.OFFICIAL_SITE_ID,
              PRINTING.CARD_ID,
              PRINTING.SET_ID,
              PRINTING.COLLECTOR_NUMBER,
              PRINTING.RARITY_ID,
              PRINTING.ILLUSTRATOR_ID)
          .values(p.officialSiteId(), cardId, setId, p.collectorNumber(), rarityId, illustratorId)
          .onConflict(PRINTING.OFFICIAL_SITE_ID)
          .doUpdate()
          .set(PRINTING.CARD_ID, DSL.field("excluded.card_id", Integer.class))
          .set(PRINTING.SET_ID, DSL.field("excluded.set_id", Integer.class))
          .set(PRINTING.COLLECTOR_NUMBER, DSL.field("excluded.collector_number", String.class))
          .set(PRINTING.RARITY_ID, DSL.field("excluded.rarity_id", Short.class))
          .set(PRINTING.ILLUSTRATOR_ID, DSL.field("excluded.illustrator_id", Integer.class))
          .execute();
    }
    return dsl.select(PRINTING.ID, PRINTING.OFFICIAL_SITE_ID)
        .from(PRINTING)
        .fetchMap(PRINTING.OFFICIAL_SITE_ID, PRINTING.ID);
  }

  // -- Step 11: Printing sides + abilities --

  private void replacePrintingSides(
      List<PrintingJson> printings,
      Map<String, Integer> printingIds,
      Map<String, Integer> cardIds,
      Map<Long, Integer> cardSideIds,
      Map<String, Integer> abilityIds) {
    for (var p : printings) {
      int printingId = printingIds.get(p.officialSiteId());
      int cardId = cardIds.get(p.cardName());

      // Delete existing printing_side (cascades to printing_side_ability)
      dsl.deleteFrom(PRINTING_SIDE).where(PRINTING_SIDE.PRINTING_ID.eq(printingId)).execute();

      for (var side : p.sides()) {
        int cardSideId = cardSideIds.get(cardSideKey(cardId, side.sideOrder()));

        // Insert printing_side
        Integer printingSideId =
            dsl.insertInto(PRINTING_SIDE)
                .columns(
                    PRINTING_SIDE.PRINTING_ID,
                    PRINTING_SIDE.CARD_SIDE_ID,
                    PRINTING_SIDE.FLAVOR_TEXT,
                    PRINTING_SIDE.IMAGE_FILENAME)
                .values(printingId, cardSideId, side.flavorText(), side.imageFilename())
                .returning(PRINTING_SIDE.ID)
                .fetchOne(PRINTING_SIDE.ID);

        // Insert printing_side_ability
        if (side.abilities() != null) {
          for (var ability : side.abilities()) {
            Integer abilityId = abilityIds.get(ability.text());
            dsl.insertInto(PRINTING_SIDE_ABILITY)
                .columns(
                    PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID,
                    PRINTING_SIDE_ABILITY.ABILITY_ID,
                    PRINTING_SIDE_ABILITY.POSITION,
                    PRINTING_SIDE_ABILITY.INDENT_LEVEL)
                .values(
                    printingSideId,
                    abilityId,
                    (short) ability.position(),
                    (short) ability.indentLevel())
                .execute();
          }
        }
      }
    }
  }

  // -- Cleanup --

  private void cleanupOrphanedAbilities() {
    int deleted =
        dsl.deleteFrom(ABILITY)
            .where(
                ABILITY.ID.notIn(
                    dsl.selectDistinct(PRINTING_SIDE_ABILITY.ABILITY_ID)
                        .from(PRINTING_SIDE_ABILITY)))
            .execute();
    if (deleted > 0) {
      log.info("Cleaned up {} orphaned abilities", deleted);
    }
  }

  // -- Utilities --

  private static Short[] toShortArray(List<Integer> ints) {
    if (ints == null || ints.isEmpty()) return new Short[0];
    return ints.stream().map(Integer::shortValue).toArray(Short[]::new);
  }
}
