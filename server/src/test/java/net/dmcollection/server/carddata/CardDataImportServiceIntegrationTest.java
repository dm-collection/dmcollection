package net.dmcollection.server.carddata;

import static net.dmcollection.server.jooq.generated.Tables.APP_USER;
import static net.dmcollection.server.jooq.generated.Tables.CARD;
import static net.dmcollection.server.jooq.generated.Tables.CARD_CIV_GROUP;
import static net.dmcollection.server.jooq.generated.Tables.CARD_PRIVATE_TAG;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SET;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.CARD_SIDE_CARD_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.DECK;
import static net.dmcollection.server.jooq.generated.Tables.DECK_VERSION;
import static net.dmcollection.server.jooq.generated.Tables.DECK_VERSION_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING_SIDE_ABILITY;
import static net.dmcollection.server.jooq.generated.Tables.PRIVATE_TAG;
import static net.dmcollection.server.jooq.generated.Tables.PRODUCT_TYPE;
import static net.dmcollection.server.jooq.generated.Tables.RARITY;
import static net.dmcollection.server.jooq.generated.Tables.SET_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    classes = {
      DataSourceAutoConfiguration.class,
      DataSourceTransactionManagerAutoConfiguration.class,
      TransactionAutoConfiguration.class,
      FlywayAutoConfiguration.class,
      JooqAutoConfiguration.class,
      JacksonAutoConfiguration.class,
      CardDataImportService.class
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CardDataImportServiceIntegrationTest {

  static final PostgreSQLContainer<?> PG =
      new PostgreSQLContainer<>("postgres:18-alpine")
          .withDatabaseName("dmcollection_test")
          .withUsername("test")
          .withPassword("test");

  static {
    PG.start();
  }

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", PG::getJdbcUrl);
    registry.add("spring.datasource.username", PG::getUsername);
    registry.add("spring.datasource.password", PG::getPassword);
    registry.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.flyway.locations", () -> "classpath:db/migration");
  }

  @Autowired DSLContext dsl;

  @Autowired CardDataImportService importService;

  @Autowired ObjectMapper objectMapper;

  private CardDataJson data;

  private static final Path CARD_DATA_PATH = Path.of("data/card-data.json");

  @BeforeAll
  void importData() throws Exception {
    Assumptions.assumeTrue(
        Files.exists(CARD_DATA_PATH), "card-data.json not present — skipping import tests");
    data = objectMapper.readValue(CARD_DATA_PATH.toFile(), CardDataJson.class);
    importService.importCardData(data);
  }

  @Test
  void countsMatchJsonInput() {
    assertThat(dsl.fetchCount(CARD)).isEqualTo(data.cards().size());
    assertThat(dsl.fetchCount(PRINTING)).isEqualTo(data.printings().size());
    assertThat(dsl.fetchCount(SET_GROUP)).isEqualTo(data.setGroups().size());
    assertThat(dsl.fetchCount(CARD_SET)).isEqualTo(data.cardSets().size());
  }

  @Test
  void singleSideCardImportedCorrectly() {
    var card = dsl.selectFrom(CARD).where(CARD.NAME.eq("天空の守護者グラン・ギューレ")).fetchOne();
    assertThat(card).isNotNull();
    assertThat(card.getIsTwinpact()).isFalse();
    assertThat(card.getSortCost()).isEqualTo(6);
    assertThat(card.getSortPower()).isEqualTo(9000);
    assertThat(card.getDeckZone()).isEqualTo("main");
    assertThat(card.getSortCivilization()).containsExactly((short) 1);

    var sides =
        dsl.selectFrom(CARD_SIDE)
            .where(CARD_SIDE.CARD_ID.eq(card.getId()))
            .orderBy(CARD_SIDE.SIDE_ORDER)
            .fetch();
    assertThat(sides).hasSize(1);
    assertThat(sides.getFirst().getName()).isEqualTo("天空の守護者グラン・ギューレ");
    assertThat(sides.getFirst().getCivilizationIds()).containsExactly((short) 1);

    // Verify card types
    var types =
        dsl.selectFrom(CARD_SIDE_CARD_TYPE)
            .where(CARD_SIDE_CARD_TYPE.CARD_SIDE_ID.eq(sides.getFirst().getId()))
            .fetch();
    assertThat(types).hasSize(1);

    // Verify civ group
    var civGroups =
        dsl.selectFrom(CARD_CIV_GROUP).where(CARD_CIV_GROUP.CARD_ID.eq(card.getId())).fetch();
    assertThat(civGroups).hasSize(1);
    assertThat(civGroups.getFirst().getCivilizationIds()).containsExactly((short) 1);
    assertThat(civGroups.getFirst().getIncludesColorlessSide()).isFalse();
  }

  @Test
  void twinpactCivGroupIsUnion() {
    var card = dsl.selectFrom(CARD).where(CARD.NAME.eq("ハザード・オウ禍武斗／禍武斗の轟印")).fetchOne();
    assertThat(card).isNotNull();
    assertThat(card.getIsTwinpact()).isTrue();

    var civGroups =
        dsl.selectFrom(CARD_CIV_GROUP).where(CARD_CIV_GROUP.CARD_ID.eq(card.getId())).fetch();
    assertThat(civGroups).hasSize(1);
    assertThat(civGroups.getFirst().getCivilizationIds()).containsExactly((short) 5);
  }

  @Test
  void nonTwinpactMultiSideHasOneGroupPerSide() {
    var card = dsl.selectFrom(CARD).where(CARD.NAME.eq("時空の勇躍ディアナ/閃光の覚醒者エル・ディアナ")).fetchOne();
    assertThat(card).isNotNull();
    assertThat(card.getIsTwinpact()).isFalse();

    var sides =
        dsl.selectFrom(CARD_SIDE)
            .where(CARD_SIDE.CARD_ID.eq(card.getId()))
            .orderBy(CARD_SIDE.SIDE_ORDER)
            .fetch();
    assertThat(sides).hasSize(2);

    var civGroups =
        dsl.selectFrom(CARD_CIV_GROUP).where(CARD_CIV_GROUP.CARD_ID.eq(card.getId())).fetch();
    assertThat(civGroups).hasSize(2);
  }

  @Test
  void printingAbilitiesImported() {
    var printing =
        dsl.selectFrom(PRINTING).where(PRINTING.OFFICIAL_SITE_ID.eq("dm01-001")).fetchOne();
    assertThat(printing).isNotNull();
    assertThat(printing.getCollectorNumber()).isEqualTo("DM1 1/110");

    var printingSides =
        dsl.selectFrom(PRINTING_SIDE).where(PRINTING_SIDE.PRINTING_ID.eq(printing.getId())).fetch();
    assertThat(printingSides).hasSize(1);

    var abilities =
        dsl.selectFrom(PRINTING_SIDE_ABILITY)
            .where(PRINTING_SIDE_ABILITY.PRINTING_SIDE_ID.eq(printingSides.getFirst().getId()))
            .orderBy(PRINTING_SIDE_ABILITY.POSITION)
            .fetch();
    assertThat(abilities).hasSize(2);
    assertThat(abilities.getFirst().getPosition()).isEqualTo((short) 0);
    assertThat(abilities.get(1).getPosition()).isEqualTo((short) 1);
  }

  @Test
  void productTypesImportedFromJson() {
    var productTypes = dsl.select(PRODUCT_TYPE.NAME).from(PRODUCT_TYPE).fetchSet(PRODUCT_TYPE.NAME);
    assertThat(productTypes)
        .containsExactlyInAnyOrder("expansion", "special", "deck", "promo", "starter", "art");
  }

  @Test
  void idempotentImport() {
    int cardCountBefore = dsl.fetchCount(CARD);
    int printingCountBefore = dsl.fetchCount(PRINTING);

    importService.importCardData(data);

    assertThat(dsl.fetchCount(CARD)).isEqualTo(cardCountBefore);
    assertThat(dsl.fetchCount(PRINTING)).isEqualTo(printingCountBefore);
  }

  @Test
  void raritiesHaveSortOrder() {
    var rarities = dsl.selectFrom(RARITY).orderBy(RARITY.SORT_ORDER).fetch();
    assertThat(rarities).isNotEmpty();
    assertThat(rarities.getFirst().getName()).isEqualTo("NONE");
    assertThat(rarities.get(1).getName()).isEqualTo("C");
  }

  @Test
  void printingWithNullRarity() {
    long nullRarityCount =
        dsl.selectCount().from(PRINTING).where(PRINTING.RARITY_ID.isNull()).fetchOne(0, long.class);
    assertThat(nullRarityCount).isGreaterThan(0);
  }

  @Nested
  class AliasProcessing {

    private int insertTestCard(String name) {
      dsl.insertInto(CARD)
          .columns(CARD.NAME, CARD.IS_TWINPACT, CARD.DECK_ZONE)
          .values(name, false, "main")
          .onConflict(CARD.NAME)
          .doNothing()
          .execute();
      return dsl.select(CARD.ID).from(CARD).where(CARD.NAME.eq(name)).fetchOne(CARD.ID);
    }

    private int insertTestPrinting(String officialSiteId, int cardId) {
      int setId = dsl.select(CARD_SET.ID).from(CARD_SET).limit(1).fetchOne(CARD_SET.ID);
      dsl.insertInto(PRINTING)
          .columns(PRINTING.OFFICIAL_SITE_ID, PRINTING.CARD_ID, PRINTING.SET_ID)
          .values(officialSiteId, cardId, setId)
          .onConflict(PRINTING.OFFICIAL_SITE_ID)
          .doNothing()
          .execute();
      return dsl.select(PRINTING.ID)
          .from(PRINTING)
          .where(PRINTING.OFFICIAL_SITE_ID.eq(officialSiteId))
          .fetchOne(PRINTING.ID);
    }

    @Test
    void simpleRenamePreservesId() {
      int cardId = insertTestCard("__test_old_name__");

      importService.importCardData(
          new CardDataJson(
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              List.of(new CardDataJson.CardAliasJson("__test_old_name__", "__test_new_name__")),
              List.of(),
              List.of()));

      assertThat(dsl.fetchCount(CARD, CARD.NAME.eq("__test_old_name__"))).isZero();
      var renamed = dsl.selectFrom(CARD).where(CARD.NAME.eq("__test_new_name__")).fetchOne();
      assertThat(renamed).isNotNull();
      assertThat(renamed.getId()).isEqualTo(cardId);

      // Cleanup
      dsl.deleteFrom(CARD).where(CARD.ID.eq(cardId)).execute();
    }

    @Test
    void aliasForNonexistentCardIsNoOp() {
      int countBefore = dsl.fetchCount(CARD);

      importService.importCardData(
          new CardDataJson(
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              List.of(new CardDataJson.CardAliasJson("__nonexistent_old__", "__nonexistent_new__")),
              List.of(),
              List.of()));

      assertThat(dsl.fetchCount(CARD)).isEqualTo(countBefore);
    }

    @Test
    void mergePrintingsRePointedToSurvivor() {
      int oldCardId = insertTestCard("__test_merge_old__");
      int survivorCardId = insertTestCard("__test_merge_survivor__");
      int printingId = insertTestPrinting("__test-merge-001__", oldCardId);

      importService.importCardData(
          new CardDataJson(
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              List.of(
                  new CardDataJson.CardAliasJson("__test_merge_old__", "__test_merge_survivor__")),
              List.of(),
              List.of()));

      // Old card should be deleted
      assertThat(dsl.fetchCount(CARD, CARD.ID.eq(oldCardId))).isZero();

      // Printing should now point to survivor
      var printing = dsl.selectFrom(PRINTING).where(PRINTING.ID.eq(printingId)).fetchOne();
      assertThat(printing.getCardId()).isEqualTo(survivorCardId);

      // Cleanup
      dsl.deleteFrom(PRINTING).where(PRINTING.ID.eq(printingId)).execute();
      dsl.deleteFrom(CARD).where(CARD.ID.eq(survivorCardId)).execute();
    }

    @Test
    void mergeHandlesPrivateTagConflict() {
      int oldCardId = insertTestCard("__test_tag_old__");
      int survivorCardId = insertTestCard("__test_tag_survivor__");

      // Create a user and private tag
      UUID userId =
          dsl.insertInto(APP_USER)
              .columns(APP_USER.USERNAME, APP_USER.PASSWORD_HASH, APP_USER.DISPLAY_NAME)
              .values("__test_tag_user__", "hash", "Test")
              .returning(APP_USER.ID)
              .fetchOne(APP_USER.ID);
      int tagId =
          dsl.insertInto(PRIVATE_TAG)
              .columns(PRIVATE_TAG.USER_ID, PRIVATE_TAG.NAME)
              .values(userId, "__test_tag__")
              .returning(PRIVATE_TAG.ID)
              .fetchOne(PRIVATE_TAG.ID);

      // Both cards have the same tag
      dsl.insertInto(CARD_PRIVATE_TAG)
          .columns(CARD_PRIVATE_TAG.CARD_ID, CARD_PRIVATE_TAG.PRIVATE_TAG_ID)
          .values(oldCardId, tagId)
          .execute();
      dsl.insertInto(CARD_PRIVATE_TAG)
          .columns(CARD_PRIVATE_TAG.CARD_ID, CARD_PRIVATE_TAG.PRIVATE_TAG_ID)
          .values(survivorCardId, tagId)
          .execute();

      importService.importCardData(
          new CardDataJson(
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              List.of(new CardDataJson.CardAliasJson("__test_tag_old__", "__test_tag_survivor__")),
              List.of(),
              List.of()));

      // Old card deleted, survivor retains tag
      assertThat(dsl.fetchCount(CARD, CARD.ID.eq(oldCardId))).isZero();
      assertThat(
              dsl.fetchCount(
                  CARD_PRIVATE_TAG,
                  CARD_PRIVATE_TAG
                      .CARD_ID
                      .eq(survivorCardId)
                      .and(CARD_PRIVATE_TAG.PRIVATE_TAG_ID.eq(tagId))))
          .isEqualTo(1);

      // Cleanup
      dsl.deleteFrom(CARD_PRIVATE_TAG).where(CARD_PRIVATE_TAG.CARD_ID.eq(survivorCardId)).execute();
      dsl.deleteFrom(PRIVATE_TAG).where(PRIVATE_TAG.ID.eq(tagId)).execute();
      dsl.deleteFrom(CARD).where(CARD.ID.eq(survivorCardId)).execute();
      dsl.deleteFrom(APP_USER).where(APP_USER.ID.eq(userId)).execute();
    }

    @Test
    void mergeSumsDeckVersionEntryQuantities() {
      int oldCardId = insertTestCard("__test_deck_old__");
      int survivorCardId = insertTestCard("__test_deck_survivor__");

      // Create user → deck → deck_version
      UUID userId =
          dsl.insertInto(APP_USER)
              .columns(APP_USER.USERNAME, APP_USER.PASSWORD_HASH, APP_USER.DISPLAY_NAME)
              .values("__test_deck_user__", "hash", "Test")
              .returning(APP_USER.ID)
              .fetchOne(APP_USER.ID);
      UUID deckId =
          dsl.insertInto(DECK)
              .columns(DECK.USER_ID, DECK.NAME)
              .values(userId, "__test_deck__")
              .returning(DECK.ID)
              .fetchOne(DECK.ID);
      UUID deckVersionId =
          dsl.insertInto(DECK_VERSION)
              .columns(DECK_VERSION.DECK_ID, DECK_VERSION.IS_DRAFT)
              .values(deckId, true)
              .returning(DECK_VERSION.ID)
              .fetchOne(DECK_VERSION.ID);

      // Both cards in same deck version (no printing specified)
      dsl.insertInto(DECK_VERSION_ENTRY)
          .columns(
              DECK_VERSION_ENTRY.DECK_VERSION_ID,
              DECK_VERSION_ENTRY.CARD_ID,
              DECK_VERSION_ENTRY.QUANTITY)
          .values(deckVersionId, oldCardId, 2)
          .execute();
      dsl.insertInto(DECK_VERSION_ENTRY)
          .columns(
              DECK_VERSION_ENTRY.DECK_VERSION_ID,
              DECK_VERSION_ENTRY.CARD_ID,
              DECK_VERSION_ENTRY.QUANTITY)
          .values(deckVersionId, survivorCardId, 3)
          .execute();

      importService.importCardData(
          new CardDataJson(
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              List.of(
                  new CardDataJson.CardAliasJson("__test_deck_old__", "__test_deck_survivor__")),
              List.of(),
              List.of()));

      // Old card deleted
      assertThat(dsl.fetchCount(CARD, CARD.ID.eq(oldCardId))).isZero();

      // Survivor's entry should have summed quantity (2 + 3 = 5)
      var entry =
          dsl.selectFrom(DECK_VERSION_ENTRY)
              .where(DECK_VERSION_ENTRY.DECK_VERSION_ID.eq(deckVersionId))
              .and(DECK_VERSION_ENTRY.CARD_ID.eq(survivorCardId))
              .fetchOne();
      assertThat(entry.getQuantity()).isEqualTo(5);

      // Only one entry for this deck version
      assertThat(
              dsl.fetchCount(
                  DECK_VERSION_ENTRY, DECK_VERSION_ENTRY.DECK_VERSION_ID.eq(deckVersionId)))
          .isEqualTo(1);

      // Cleanup
      dsl.deleteFrom(DECK).where(DECK.ID.eq(deckId)).execute();
      dsl.deleteFrom(CARD).where(CARD.ID.eq(survivorCardId)).execute();
      dsl.deleteFrom(APP_USER).where(APP_USER.ID.eq(userId)).execute();
    }
  }
}
