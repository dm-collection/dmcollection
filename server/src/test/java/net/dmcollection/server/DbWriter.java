package net.dmcollection.server;

import static net.dmcollection.server.jooq.generated.Tables.ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.CARD;
import static net.dmcollection.server.jooq.generated.Tables.CARD_CIV_GROUP;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SET;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_CARD_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_RACE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE_ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.PRODUCT_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.RACE;
import static net.dmcollection.server.jooq.generated.Tables.RARITY;
import static net.dmcollection.server.jooq.generated.Tables.SET_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.dmcollection.server.card.Civilization;
import net.dmcollection.server.card.RarityCode;
import net.dmcollection.server.card.internal.query.CardTypeResolver;
import net.dmcollection.server.jooq.generated.tables.records.AbilityRecord;
import net.dmcollection.server.jooq.generated.tables.records.CardCivGroupRecord;
import net.dmcollection.server.jooq.generated.tables.records.CardRecord;
import net.dmcollection.server.jooq.generated.tables.records.CardSetRecord;
import net.dmcollection.server.jooq.generated.tables.records.CardSideCardTypeRecord;
import net.dmcollection.server.jooq.generated.tables.records.CardSideRaceRecord;
import net.dmcollection.server.jooq.generated.tables.records.CardSideRecord;
import net.dmcollection.server.jooq.generated.tables.records.PrintingRecord;
import net.dmcollection.server.jooq.generated.tables.records.PrintingSideAbilityRecord;
import net.dmcollection.server.jooq.generated.tables.records.PrintingSideRecord;
import net.dmcollection.server.jooq.generated.tables.records.ProductTypeRecord;
import net.dmcollection.server.jooq.generated.tables.records.RarityRecord;
import net.dmcollection.server.jooq.generated.tables.records.SetGroupRecord;
import org.jooq.DSLContext;

public class DbWriter {

  private final DSLContext db;

  public DbWriter(DSLContext db) {
    this.db = db;
  }

  public int upsertSetGroup(String name, int sortOrder) {
    var table = SET_GROUP;
    db.deleteFrom(table).where(table.NAME.eq(name).and(table.SORT_ORDER.eq(sortOrder))).execute();
    var group = new SetGroupRecord().setName(name).setSortOrder(sortOrder);
    Integer id = db.insertInto(table).set(group).returningResult(table.ID).fetchOne(table.ID);
    assertThat(id).isNotNull();
    return id;
  }

  public int upsertSet(
      String setCode, String name, LocalDate release, String productType, Integer groupId) {
    var table = CARD_SET;
    short typeId = upsertProductType(productType);
    var cardSet =
        new CardSetRecord()
            .setCode(setCode)
            .setName(name)
            .setReleaseDate(release)
            .setProductTypeId(typeId)
            .setSetGroupId(groupId);
    Integer id =
        db.insertInto(table)
            .set(cardSet)
            .onConflict(table.CODE)
            .doUpdate()
            .set(cardSet)
            .returningResult(table.ID)
            .fetchOne(table.ID);
    assertThat(id).isNotNull();
    return id;
  }

  public short upsertRace(String race) {
    var r = RACE;
    Short id =
        db.insertInto(r)
            .set(r.NAME, race)
            .onConflict(r.NAME)
            .doUpdate()
            .set(r.NAME, race)
            .returningResult(r.ID)
            .fetchOne(r.ID);
    assertThat(id).isNotNull();
    return id;
  }

  private short upsertCardType(String cardType) {
    var ct = CARD_TYPE;
    Short id =
        db.insertInto(ct)
            .set(ct.NAME, cardType)
            .onConflict(ct.NAME)
            .doUpdate()
            .set(ct.NAME, cardType)
            .returningResult(ct.ID)
            .fetchOne(ct.ID);
    assertThat(id).isNotNull();
    return id;
  }

  private void addTypeToSide(String cardType, int cardSideId, int position) {
    short typeId = upsertCardType(cardType);
    var rel = new CardSideCardTypeRecord();
    rel.setCardSideId(cardSideId).setCardTypeId(typeId).setPosition((short) position);
    var table = CARD_SIDE_CARD_TYPE;
    db.insertInto(table)
        .set(rel)
        .onConflict(table.CARD_SIDE_ID, table.POSITION)
        .doUpdate()
        .set(rel)
        .execute();
  }

  private void addRaceToSide(String race, int cardSideId, int position) {
    short raceId = upsertRace(race);
    var rel = new CardSideRaceRecord();
    rel.setCardSideId(cardSideId).setRaceId(raceId).setPosition((short) position);
    db.insertInto(CARD_SIDE_RACE).set(rel).execute();
  }

  public void addAbility(int printingSideId, String text, int position, int indent) {
    var a = ABILITY;
    var ability = new AbilityRecord();
    ability.setText(text);
    ability.setSearchText(text);
    Integer id =
        db.insertInto(a)
            .set(ability)
            .onConflict(a.TEXT)
            .doUpdate()
            .set(ability)
            .returningResult(a.ID)
            .fetchOne(a.ID);
    assertThat(id).isNotNull();

    var table = PRINTING_SIDE_ABILITY;
    var sideAbility =
        new PrintingSideAbilityRecord()
            .setPrintingSideId(printingSideId)
            .setAbilityId(id)
            .setPosition((short) position)
            .setIndentLevel((short) indent);
    db.insertInto(table)
        .set(sideAbility)
        .onConflict(table.PRINTING_SIDE_ID, table.POSITION)
        .doUpdate()
        .set(sideAbility)
        .execute();
  }

  public short upsertProductType(String typeName) {
    ProductTypeRecord productType = new ProductTypeRecord();
    productType.setName(typeName);
    var pt = PRODUCT_TYPE;
    Short id =
        db.insertInto(pt)
            .set(productType)
            .onConflict(pt.NAME)
            .doUpdate()
            .set(productType)
            .returningResult(pt.ID)
            .fetchOne(pt.ID);
    assertThat(id).isNotNull();
    return id;
  }

  private short upsertRarity(RarityCode rarityCode) {
    if (rarityCode == RarityCode.NONE) {
      throw new IllegalArgumentException("NONE rarity should be null");
    }
    var r = RARITY;
    Short existing = db.select(r.ID).from(r).where(r.NAME.eq(rarityCode.name())).fetchOne(r.ID);
    if (existing != null) return existing;
    RarityRecord rarity = new RarityRecord();
    rarity
        .setName(rarityCode.toString())
        .setSortOrder((short) rarityCode.ordinal())
        .setDescription(rarity.toString());

    Short id = db.insertInto(r).set(rarity).returningResult(r.ID).fetchOne(r.ID);
    assertThat(id).isNotNull();
    return id;
  }

  public int upsertPrinting(
      int cardId, int setId, String officialId, String idText, RarityCode rarity) {
    var table = PRINTING;
    Short rarityId = rarity == null || rarity == RarityCode.NONE ? null : upsertRarity(rarity);
    var printing =
        new PrintingRecord()
            .setCardId(cardId)
            .setSetId(setId)
            .setOfficialSiteId(officialId)
            .setCollectorNumber(idText)
            .setRarityId(rarityId);
    Integer id =
        db.insertInto(table)
            .set(printing)
            .onConflict(table.OFFICIAL_SITE_ID)
            .doUpdate()
            .set(printing)
            .returningResult(table.ID)
            .fetchOne(table.ID);
    assertThat(id).isNotNull();
    return id;
  }

  public int upsertPrintingSide(
      int printingId, int cardSideId, String flavorText, String imageFileName) {
    var table = PRINTING_SIDE;
    var side =
        new PrintingSideRecord()
            .setPrintingId(printingId)
            .setCardSideId(cardSideId)
            .setFlavorText(flavorText)
            .setImageFilename(imageFileName);
    Integer id =
        db.insertInto(table)
            .set(side)
            .onConflict(table.PRINTING_ID, table.CARD_SIDE_ID)
            .doUpdate()
            .set(side)
            .returningResult(table.ID)
            .fetchOne(table.ID);
    assertThat(id).isNotNull();
    return id;
  }

  public static class IntOrInfinity {
    private final Integer value;
    private final boolean isInfinite;

    private IntOrInfinity() {
      isInfinite = true;
      value = null;
    }

    public IntOrInfinity(int value) {
      if (value == Integer.MAX_VALUE) {
        this.isInfinite = true;
        this.value = null;
      } else {
        this.value = value;
        this.isInfinite = false;
      }
    }

    public Integer value() {
      return value;
    }

    public boolean isInfinite() {
      return isInfinite;
    }
  }

  public int upsertCardSide(
      int cardId,
      int position,
      String name,
      IntOrInfinity cost,
      IntOrInfinity power,
      Collection<Civilization> civs,
      List<String> cardTypes,
      CardTypeResolver cardTypeResolver,
      List<String> races) {
    Short[] civIds = civsWithoutZero(civs);
    var side = new CardSideRecord();
    side.setCardId(cardId)
        .setSideOrder((short) position)
        .setName(name)
        .setCost(cost != null ? cost.value : null)
        .setCostIsInfinity(cost != null && cost.isInfinite())
        .setPower(power != null ? power.value() : null)
        .setPowerIsInfinity(power != null && power.isInfinite())
        .setCivilizationIds(civIds);

    var table = CARD_SIDE;
    Integer id =
        db.insertInto(table)
            .set(side)
            .onConflict(table.CARD_ID, table.SIDE_ORDER)
            .doUpdate()
            .set(side)
            .returningResult(table.ID)
            .fetchOne(table.ID);
    assertThat(id).isNotNull();

    if (cardTypes != null) {
      for (int i = 0; i < cardTypes.size(); i++) {
        addTypeToSide(cardTypes.get(i), id, i);
      }
      cardTypeResolver.loadNameToId();
    }

    if (races != null) {
      for (int i = 0; i < races.size(); i++) {
        addRaceToSide(races.get(i), id, i);
      }
    }

    return id;
  }

  private void upsertCivGroups(
      int cardId, boolean twinpact, Collection<? extends Collection<Civilization>> civs) {
    final var table = CARD_CIV_GROUP;
    // delete existing
    db.deleteFrom(table).where(table.CARD_ID.eq(cardId)).execute();
    if (twinpact) {
      // Twinpact: single row with union of all sides' civilizations
      boolean includesColorlessSide = containsZeroSide(civs);
      var civIds =
          civsWithoutZero(civs.stream().flatMap(Collection::stream).collect(Collectors.toSet()));
      var group =
          new CardCivGroupRecord()
              .setCardId(cardId)
              .setCivilizationIds(civIds)
              .setIncludesColorlessSide(includesColorlessSide);
      db.insertInto(table).set(group).execute();
    } else {
      // one row per side
      for (var sideCivs : civs) {
        var civIds = civsWithoutZero(sideCivs);
        var group =
            new CardCivGroupRecord()
                .setCardId(cardId)
                .setCivilizationIds(civIds)
                .setIncludesColorlessSide(civIds.length == 0);
        db.insertInto(table).set(group).execute();
      }
    }
  }

  public int upsertCard(
      String name,
      boolean twinpact,
      Integer sortCost,
      Integer sortPower,
      Collection<? extends Collection<Civilization>> sideCivs,
      String zone) {
    var sortCivilization =
        civsWithoutZero(sideCivs.stream().flatMap(Collection::stream).collect(Collectors.toSet()));
    CardRecord cardRecord = new CardRecord();
    cardRecord
        .setName(name)
        .setIsTwinpact(twinpact)
        .setSortCost(sortCost)
        .setSortPower(sortPower)
        .setSortCivilization(sortCivilization)
        .setDeckZone(zone);
    var c = CARD;
    var id =
        db.insertInto(c)
            .set(cardRecord)
            .onConflict(c.NAME)
            .doUpdate()
            .set(cardRecord)
            .returningResult(c.ID)
            .fetchOne(c.ID);
    assertThat(id).isNotNull();
    upsertCivGroups(id, twinpact, sideCivs);
    return id;
  }

  private boolean containsZeroSide(Collection<? extends Collection<Civilization>> civilizations) {
    boolean sideOnlyZero = false;
    for (var collection : civilizations) {
      sideOnlyZero =
          sideOnlyZero
              || collection.stream()
                  .filter(c -> c != Civilization.ZERO)
                  .collect(Collectors.toSet())
                  .isEmpty();
    }
    return sideOnlyZero;
  }

  private Short[] civsWithoutZero(Collection<Civilization> civs) {
    return civs.stream()
        .filter(c -> c != Civilization.ZERO)
        .map(c -> (short) c.ordinal())
        .distinct()
        .sorted()
        .toArray(Short[]::new);
  }
}
