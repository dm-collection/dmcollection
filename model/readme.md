# Database structure for card data

Card data is maintained across several tables. These tables are filled or updated at application startup.
Afterward, they are only accessed for reading. Currently, an in-memory H2 database is used.

## Card Sets

Card sets are products that contain cards such as expansion packs, pre-built decks or even promotional cards attached to
issues of the コロコロ magazine.

```sql
CREATE TABLE CARD_SET (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	DM_ID CHARACTER VARYING(255) NOT NULL,
	NAME CHARACTER VARYING(255) NOT NULL,
	"RELEASE" DATE,
	CONSTRAINT PK_CARD_SET PRIMARY KEY (ID)
);
CREATE INDEX IDX_CARD_SET_DM_ID ON CARD_SET (DM_ID);
CREATE INDEX IDX_CARD_SET_RELEASE ON CARD_SET ("RELEASE");
CREATE UNIQUE INDEX PRIMARY_KEY_2 ON CARD_SET (ID);
```

### Example rows

|  ID | DM_ID          | NAME                                                      | RELEASE    |
|----:|----------------|-----------------------------------------------------------|------------|
| 371 | dm25bd2        | DM25-BD2 ドリーム英雄譚デッキ アルカディアスの書                             | 2025-08-09 |
| 372 | promoy24-spd12 | コロコロ・スペシャル デッキ12                                          | 2025-09-12 |
| 373 | dm25sp2        | DM25-SP2 キャラプレミアムデッキ ドラゴン娘になりたくないっ！ はじけろスポーツ！青春☆ワールドカップ!! | 2025-09-13 |
| 374 | dm25rp3        | DM25-RP3 王道W 第3弾 邪神vs時皇 ～ビヨンド・ザ・タイム～                      | 2025-09-20 |

## Cards

This entity models the physical cards. A card can have up to four sides (call them facets).
Some cards use the front- and backside, while others unfold to create additional facets.
ツインパクト ("Twinpact") cards contain two facets (usually, but not always, a creature and a spell) on one side.

```sql
CREATE TABLE CARDS (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	OFFICIAL_ID CHARACTER VARYING(255) NOT NULL,
	ID_TEXT CHARACTER VARYING(255),
	TWINPACT BOOLEAN NOT NULL,
	RARITY CHARACTER VARYING(3),
	"SET" BIGINT,
	CONSTRAINT PK_CARDS PRIMARY KEY (ID),
	CONSTRAINT FK_CARD_SET FOREIGN KEY ("SET") REFERENCES CARD_SET(ID) ON DELETE RESTRICT ON UPDATE RESTRICT
);
CREATE INDEX FK_CARD_SET_INDEX_3 ON CARDS ("SET");
CREATE INDEX IDX_CARDS_SET_ID ON CARDS ("SET");
CREATE INDEX IDX_CARD_RARITY ON CARDS (RARITY);
CREATE INDEX IDX_OFFICIAL_ID_CARDS ON CARDS (OFFICIAL_ID);
CREATE UNIQUE INDEX PRIMARY_KEY_3 ON CARDS (ID);
```

### Example rows

|   ID | OFFICIAL_ID  | ID_TEXT            | TWINPACT | RARITY | SET |
|-----:|--------------|--------------------|----------|--------|----:|
|    1 | dm01-001     | DM1 1/110          | false    | VR     |   1 |
| 9561 | promoy17-035 | DMPROMOY17 P35/Y17 | true     |        | 197 |

## Card Facets

The facets of physical cards, each corresponding to an entity or concept in the actual game.

`POSITION` denotes the order in which these facets belong to the physical card, starting with `0`.
E.g. for twinpact cards, the card with position `0` is shown on the top half, the card with position `1`
is shown on the lower half of the card. For cards with a ハイパーモード, the first facet is the creature when summoned
and the
second facet is the same creature when hypermode is activated. Then there are サイキック creatures that can have
multiple sides
and effects that cause them to switch between them and various other card types that have multiple facets.

`IMAGE_FILENAME` is the filename of the image that shows the facet. Where two facets appear on the same physical side of
a card,
the first facet has the image of that side while the second facet has no `IMAGE_FILENAME`.

The `TYPE` of a facet primarily denotes the in-game concept. There are more than 70 unique types that can be roughly
sorted into the categories クリーチャー, 呪文, 進化クリーチャー, サイキック, ドラグハート, フィールド, 城, クロスギア,
エグザイル, GR, オーラ, タマシード and "その他".

A facet's power is not ony a number but can also be denoted by the infinity symbol or a number with a plus or minus
behind
(to signify that its power increase or decreases due to its effects). Thus, the `POWER_TXT` column where the power is
saved
as a string, the `POWER` column where only the numerical value is saved (with infinity being mapped to Java
`Integer.MAX_VALUE`)
and the `POWER_SORT` column where the numerical values are increased by 1 if there is a "+" after the number or
decreased by 1 if there is a "-".
The latter is then used for sorting (so far there are no collisions; e.g. no "999" and "1000-" creatures).

`CIVS` are the (文明) "civilizations" of a card.
"Colorless" is treated as a civilization here so the `CIVS` array can contain the values
0 (ゼロ=zero), 1 (光=light), 2 (水=water), 3 (闇=darkness), 4 (火=fire), 5 (自然=nature).
A facet can have up to 5 civilizations - ゼロ is not combined with other civilizations and only appears on its own.

```sql
CREATE TABLE CARD_FACETS (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	CARDS BIGINT NOT NULL,
	"POSITION" INTEGER NOT NULL,
	IMAGE_FILENAME CHARACTER VARYING(255),
	NAME CHARACTER VARYING(255),
	COST INTEGER,
	"TYPE" CHARACTER VARYING(255),
	POWER_TXT CHARACTER VARYING(10),
	POWER INTEGER,
	POWER_SORT INTEGER,
	CIVS TINYINT ARRAY,
	CONSTRAINT PK_CARD_FACETS PRIMARY KEY (ID),
	CONSTRAINT FK_FACETS_CARDS FOREIGN KEY (CARDS) REFERENCES CARDS(ID) ON DELETE RESTRICT ON UPDATE RESTRICT
);
CREATE INDEX FK_FACETS_CARDS_INDEX_7 ON CARD_FACETS (CARDS);
CREATE INDEX IDX_FACETS_CARD_ID ON CARD_FACETS (CARDS);
CREATE INDEX IDX_FACET_CIVS ON CARD_FACETS (CIVS);
CREATE INDEX IDX_FACET_COST ON CARD_FACETS (COST);
CREATE INDEX IDX_FACET_POWER ON CARD_FACETS (POWER);
CREATE INDEX IDX_FACET_POWER_SORT ON CARD_FACETS (POWER_SORT);
CREATE INDEX IDX_FACET_TYPE ON CARD_FACETS ("TYPE");
CREATE UNIQUE INDEX PRIMARY_KEY_7 ON CARD_FACETS (ID);
```

### Example rows

|    ID | CARDS | POSITION | IMAGE_FILENAME    | NAME               |       COST | TYPE   | POWER_TXT |      POWER | POWER_SORT | CIVS    |
|------:|------:|---------:|-------------------|--------------------|-----------:|--------|-----------|-----------:|-----------:|---------|
|     1 |     1 |        0 | dm01-001.jpg      | 天空の守護者グラン・ギューレ     |          6 | クリーチャー | 9000      |       9000 |       9000 | {1}     |
|  9794 |  9561 |        0 | promoy17-035a.jpg | ガンバＧ               |          6 | クリーチャー | 3000      |       3000 |       3000 | {0}     |
|  9795 |  9561 |        1 |                   | ガガン・ガン・ガガン         |          2 | 呪文     |           |            |            | {0}     |
| 19752 | 18435 |        0 | dmart17-001.jpg   | 切札勝太&カツキング ー熱血の物語ー |          5 | クリーチャー | 5000+     |       5000 |       5001 | {2,4,5} |
| 22043 | 20454 |        0 | dm25bd2-007.jpg   | 極限の精霊インフィニティ・シリウス  | 2147483647 | クリーチャー | ∞         | 2147483647 | 2147483647 | {1,2}   |

## Effects

The separate items in a facet's card text denote the 特殊能力 of a facet.
Each item is modelled as a separate effect.
Some effects (e.g. ブロッカー、シールドトリガー) appear on multiple facets.
There are effects that have child effects (e.g. 次の中から２回選ぶ。).
This is modeled via the `PARENT` and `POSITION` column, denoting an effect
as child of `PARENT` in position `POSITION` if `PARENT` is not `NULL`.
This only extends to one level; there are no grandparent effects or grandchildren effects.

Effects may contain placeholders for icons in the form `[[icon:<keyword>]]` or `[[mana:<cost>]]`.
An example for a keyword is `blocker` and `df05` would denote a cost of 5 dark/fire mana.

Effects and facets are associated via the `FACET_EFFECT` table where the `POSITION` of
an effect of the facet is also recorded.

```sql
CREATE TABLE EFFECT (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	TEXT CHARACTER VARYING,
	PARENT BIGINT,
	"POSITION" INTEGER DEFAULT 0 NOT NULL,
	CONSTRAINT PK_EFFECT PRIMARY KEY (ID)
);
CREATE UNIQUE INDEX PRIMARY_KEY_79 ON EFFECT (ID);

CREATE TABLE FACET_EFFECT (
	CARD_FACETS BIGINT NOT NULL,
	"POSITION" INTEGER NOT NULL,
	EFFECT BIGINT NOT NULL,
	CONSTRAINT FK_EFFECT_FACETS FOREIGN KEY (CARD_FACETS) REFERENCES CARD_FACETS(ID) ON DELETE RESTRICT ON UPDATE RESTRICT
);
CREATE INDEX FK_EFFECT_FACETS_INDEX_F ON FACET_EFFECT (CARD_FACETS);
```

### Example rows

#### `EFFECT`

|  ID | TEXT                                                                                                                 | PARENT | POSITION |
|----:|----------------------------------------------------------------------------------------------------------------------|--------|----------|
|   1 | [[icon:blocker]]ブロッカー                                                                                                |        | 0        |
|   2 | W・ブレイカー                                                                                                              |        | 0        |
|  97 | このクリーチャーが出た時、次の中から２回選ぶ。(同じものを選んでもよい)                                                                                 |        | 0        |
|  98 | 相手のクリーチャーを１体選ぶ。このターン、そのクリーチャーのパワーを－4000する。                                                                           | 97     | 0        |
|  99 | 自分の山札の上から４枚を墓地に置く。                                                                                                   | 97     | 1        |
| 100 | コスト４以下のクリーチャーを１体、自分の墓地から出す。                                                                                          | 97     | 2        |
| 425 | バズレンダ[[mana:cc02]]（この呪文のコストを支払う時、追加で[[mana:cc02]]を好きな回数支払ってもよい。その[[icon:buzzrenda]]能力を１回と、追加で[[mana:cc02]]支払った回数、使う） |        | 0        |

#### `FACET_EFFECT`

| CARD_FACETS | POSITION | EFFECT |
|-------------|----------|--------|
| 1           | 0        | 14938  |
| 1           | 1        | 177    |
| 2           | 0        | 16025  |
| 3           | 0        | 16026  |
| 4           | 0        | 14938  |

## 種族 (Species)

The species of a facet (usually of a creature, but not always). Most facets have multiple (e.g. ハンター, エイリアン,
テクノ・サムライ). Some effects refer to "species categories" to affect all facets that have a species with that
category. An example is an effect referring to "サムライ", which means both facets with the "アーマード・サムライ" and
facets with the "テクノ・サムライ" species.

```sql
CREATE TABLE SPECIES (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	SPECIES CHARACTER VARYING(255),
	CONSTRAINT PK_SPECIES PRIMARY KEY (ID)
);
CREATE INDEX IDX_SPECIES_VALUE ON SPECIES (SPECIES);
CREATE UNIQUE INDEX PRIMARY_KEY_B ON SPECIES (ID);

CREATE TABLE FACET_SPECIES (
	CARD_FACETS BIGINT NOT NULL,
	"POSITION" INTEGER NOT NULL,
	SPECIES BIGINT NOT NULL,
	CONSTRAINT FK_FACETS_SPECIES FOREIGN KEY (SPECIES) REFERENCES SPECIES(ID) ON DELETE RESTRICT ON UPDATE RESTRICT,
	CONSTRAINT FK_SPECIES_FACETS FOREIGN KEY (CARD_FACETS) REFERENCES CARD_FACETS(ID) ON DELETE RESTRICT ON UPDATE RESTRICT
);
CREATE INDEX FK_FACETS_SPECIES_INDEX_E ON FACET_SPECIES (SPECIES);
CREATE INDEX FK_SPECIES_FACETS_INDEX_E ON FACET_SPECIES (CARD_FACETS);
```

### Example rows

#### `SPECIES`

| ID | SPECIES          |
|----|------------------|
| 1  | アーマード・ドラゴン       |
| 2  | ガーディアン           |
| 3  | ハンター             |
| 4  | エイリアン            |
| 5  | マジック・スプラッシュ・クイーン |

#### `FACET_SPECIES`

| CARD_FACETS | POSITION | SPECIES |
|-------------|----------|---------|
| 2973        | 0        | 35      |
| 2973        | 1        | 16      |
| 2973        | 2        | 110     |
| 2974        | 0        | 47      |
| 2975        | 0        | 82      |

## Rarities

This table contains the rarities cards can have, with their full name and an ordering (roughly by how rare the card is -
some rarities did not occur together in expansions and some cards that had a low probability to appear in an expansion
pack were later included in pre-built decks or reprinted with the same "rarity" but a different probability to be
drawn).

The `CODE` values are also used in the `CARDS` table though the `NONE` rarity is denoted there as `NULL`.

```sql
CREATE TABLE RARITY (
	CODE CHARACTER VARYING(10) NOT NULL,
	"ORDER" INTEGER NOT NULL,
	NAME CHARACTER VARYING(255) NOT NULL,
	CONSTRAINT PK_RARITY PRIMARY KEY (CODE)
);
CREATE UNIQUE INDEX PRIMARY_KEY_8 ON RARITY (CODE);
```

### Example rows

| CODE | ORDER | NAME    |
|------|-------|---------|
| NONE | 0     | レアリティなし |
| C    | 1     | コモン     |
| U    | 2     | アンコモン   |
| R    | 3     | レア      |
| VR   | 4     | ベリーレア   |
| SR   | 5     | スーパーレア  |