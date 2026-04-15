-- =============================================================================
-- Duel Masters Collection Manager — Database Schema (PostgreSQL)
-- =============================================================================
-- Based on domain-summary.md and schema-decisions.md.
-- Designed for Flyway migrations; this file is the canonical reference DDL.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Civilization constants (resolved to display names by the application)
-- ---------------------------------------------------------------------------
-- The five civilizations are a fixed, closed set.  The integer value
-- doubles as both identifier and canonical sort order, so no lookup
-- table is needed.
--
--   1 = 光   (Light)
--   2 = 水   (Water)
--   3 = 闇   (Darkness)
--   4 = 火   (Fire)
--   5 = 自然 (Nature)
--
-- Colorless (無色 / ゼロ文明) is NOT a civilization — it is the absence
-- of civilization.  In the game rules "ゼロ文明" does not exist as a
-- declarable civilization; cards without a civilization are simply called
-- 無色.  Therefore colorless is represented as an empty civilization_ids
-- array ('{}''), not as a sixth ID.  The application maps display label
-- "ゼロ" / "無色" to the empty-array case.
--
-- Mono vs multi vs colorless (game-rule definitions):
--   colorless  = 0 civilizations (empty array)
--   mono-color = exactly 1 civilization
--   multicolor = 2+ civilizations (rainbow / レインボー)
-- These three categories are mutually exclusive; colorless is not the
-- opposite of multicolor.
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- Extensible managed lists (lookup / reference tables)
-- ---------------------------------------------------------------------------

CREATE TABLE card_type (
    id          smallint    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE        -- e.g. 'クリーチャー', '呪文', 'サイキック・クリーチャー'
);

CREATE TABLE race (
    id          smallint    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE         -- e.g. 'アーマード・ドラゴン'
);

CREATE TABLE soul (
    id          smallint    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE         -- e.g. 'マナ', 'ジャスティス'
);

CREATE TABLE keyword_ability (
    id          smallint    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE         -- e.g. 'ブロッカー', 'S・トリガー'
);

CREATE TABLE rarity (
    id          smallint    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE,        -- e.g. 'C', 'UC', 'R', 'VR', 'SR'
    description text,                               -- e.g. 'レアリティなし', 'スーパーレア'
    sort_order  smallint    NOT NULL UNIQUE
);

CREATE TABLE treatment (
    id          smallint    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE         -- e.g. 'ヒーローズ・カード', 'シークレットレア'
);

CREATE TABLE illustrator (
    id          integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE
);

CREATE TABLE product_type (
    id          smallint    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE         -- e.g. 'ブースターパック', 'スタートデッキ', 'プロモ'
);

CREATE TABLE public_tag (
    id          integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE         -- e.g. '墓地利用', '除去', '殿堂入り'
);

-- ---------------------------------------------------------------------------
-- Set / Product hierarchy
-- ---------------------------------------------------------------------------

CREATE TABLE set_group (
    id          integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL UNIQUE,
    sort_order  integer     NOT NULL UNIQUE
);

CREATE TABLE card_set (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            text        NOT NULL,
    code            text        NOT NULL UNIQUE,
    release_date    date        NOT NULL,           -- approximate for promos
    product_type_id smallint    NOT NULL REFERENCES product_type (id),
    set_group_id    integer     REFERENCES set_group (id)
);

CREATE INDEX idx_card_set_release_date ON card_set (release_date);

-- ---------------------------------------------------------------------------
-- Card core
-- ---------------------------------------------------------------------------

CREATE TABLE card (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            text        NOT NULL UNIQUE,    -- full name (composite for twinpact/multi-sided cards)
    language        text        NOT NULL DEFAULT 'ja',
    is_twinpact     boolean     NOT NULL DEFAULT false,
    -- Pre-computed sort values from the first side (side_order = 0).
    -- Avoids JOINs/subqueries to card_side for sorting.
    sort_cost            integer,    -- first side's cost_sort (null = no cost)
    sort_power           integer,    -- first side's power_sort
    sort_power_modifier  smallint,   -- first side's power_modifier_sort
    sort_civilization    smallint[] NOT NULL DEFAULT '{}', -- civ IDs 1–5 for sorting (from card_civ_group); empty = colorless
    deck_zone text NOT NULL DEFAULT 'main'
                       CHECK (deck_zone IN ('main', 'hyperspatial', 'gr')) -- game start zone classification
);

-- Card ↔ Keyword Ability (many-to-many, unordered)
CREATE TABLE card_keyword_ability (
    card_id             integer     NOT NULL REFERENCES card (id) ON DELETE CASCADE,
    keyword_ability_id  smallint    NOT NULL REFERENCES keyword_ability (id),
    PRIMARY KEY (card_id, keyword_ability_id)
);

-- ---------------------------------------------------------------------------
-- Card Side
-- ---------------------------------------------------------------------------

CREATE TABLE card_side (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id         integer     NOT NULL REFERENCES card (id) ON DELETE CASCADE,
    side_order      smallint    NOT NULL,           -- 0-based position within card
    name            text        NOT NULL,           -- kanji name
    name_reading    text,                           -- furigana reading (searchable)
    cost            integer,                        -- null = no cost
    cost_is_infinity boolean    NOT NULL DEFAULT false,
    power           integer,                        -- null = no power
    power_is_infinity boolean   NOT NULL DEFAULT false,
    power_modifier  text        NOT NULL DEFAULT 'none'
                    CHECK (power_modifier IN ('none', 'leading_plus',
                                               'trailing_plus', 'trailing_minus')),
    civilization_ids smallint[]  NOT NULL DEFAULT '{}',  -- sorted civ IDs (1–5); empty = colorless side
    side_type       text,                           -- nice-to-have label: 'base', 'hyper', etc.
    -- Derived filter columns (generated, no app maintenance needed).
    -- Merge infinity into a single comparable integer for range queries.
    -- Sorting uses card.sort_* (first side's values); these are for WHERE clauses only.
    cost_filter          integer     GENERATED ALWAYS AS (
                            CASE WHEN cost_is_infinity THEN 2147483647 ELSE cost END
                        ) STORED,
    power_filter         integer     GENERATED ALWAYS AS (
                            CASE WHEN power_is_infinity THEN 2147483647 ELSE power END
                        ) STORED,
    -- Consistency checks: infinity and numeric value are mutually exclusive;
    -- power_modifier only meaningful when power is present.
    CHECK (NOT (cost IS NOT NULL AND cost_is_infinity)),
    CHECK (NOT (power IS NOT NULL AND power_is_infinity)),
    CHECK (power IS NOT NULL OR power_modifier = 'none'),
    UNIQUE (card_id, side_order)
);

CREATE INDEX idx_card_side_card_id ON card_side (card_id);
CREATE INDEX idx_card_side_name ON card_side (name);
CREATE INDEX idx_card_side_name_reading ON card_side (name_reading);
CREATE INDEX idx_card_side_cost_filter ON card_side (cost_filter);
CREATE INDEX idx_card_side_power_filter ON card_side (power_filter);

-- Card Side ↔ Card Type (many-to-many, ordered per side)
CREATE TABLE card_side_card_type (
    card_side_id    integer     NOT NULL REFERENCES card_side (id) ON DELETE CASCADE,
    card_type_id    smallint    NOT NULL REFERENCES card_type (id),
    position        smallint    NOT NULL,           -- display order
    PRIMARY KEY (card_side_id, card_type_id),
    UNIQUE (card_side_id, position)
);

CREATE INDEX idx_card_side_card_type_type_id ON card_side_card_type (card_type_id);

-- ---------------------------------------------------------------------------
-- Civilization Filter Groups (denormalized for query simplicity)
-- ---------------------------------------------------------------------------
-- Pre-computed filterable units for civilization queries.
-- Eliminates twinpact vs non-twinpact branching in every civilization filter.
--
-- Row cardinality per card:
--   Single-side card              → 1 row (that side's civilizations)
--   Non-twinpact multi-side card  → 1 row per side (each independently filterable)
--   Twinpact card                 → 1 row (union of all sides' civilizations)
--
-- Source of truth remains card_side.civilization_ids.
--
-- Colorless-side handling:
--   Because colorless = empty array, the union of a colorless side with a
--   colored side loses the colorless information.  For example:
--     ブータンPUNK (colorless creature + 闇 spell) → union = {3}, civ_count = 1
--     カラーレス・レインボー (colorless creature + 5色 spell) → union = {1,2,3,4,5}, civ_count = 5
--   Both are correctly mono/multicolored at the card level (per game rules),
--   but a "colorless" filter (civ_count = 0) would miss them entirely.
--   The includes_colorless_side flag is the ONLY mechanism to surface these
--   cards in colorless-oriented queries.

CREATE TABLE card_civ_group (
    id                      integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id                 integer     NOT NULL REFERENCES card (id) ON DELETE CASCADE,
    civilization_ids        smallint[]  NOT NULL DEFAULT '{}',  -- sorted civ IDs 1–5; empty = purely colorless card
    civ_count               smallint    NOT NULL
                            GENERATED ALWAYS AS (cardinality(civilization_ids)) STORED,
    includes_colorless_side boolean     NOT NULL DEFAULT false
    -- True when any constituent side has an empty civilization_ids array.
    -- This is load-bearing for colorless queries: without it, cards like
    -- ブータンPUNK (colorless + 闇 → union {3}) and カラーレス・レインボー
    -- (colorless + 5色 → union {1,2,3,4,5}) would be invisible to any
    -- colorless filter, since their civ_count > 0 and their civilization_ids
    -- contain only real civilizations.
);

CREATE INDEX idx_card_civ_group_card_id ON card_civ_group (card_id);
CREATE INDEX idx_card_civ_group_civs ON card_civ_group USING GIN (civilization_ids);
CREATE INDEX idx_card_civ_group_civ_count ON card_civ_group (civ_count);

-- ---------------------------------------------------------------------------

-- Card Side ↔ Race (many-to-many, ordered per side)
CREATE TABLE card_side_race (
    card_side_id    integer     NOT NULL REFERENCES card_side (id) ON DELETE CASCADE,
    race_id         smallint    NOT NULL REFERENCES race (id),
    position        smallint    NOT NULL,
    PRIMARY KEY (card_side_id, race_id),
    UNIQUE (card_side_id, position)
);

CREATE INDEX idx_card_side_race_race_id ON card_side_race (race_id);

-- Card Side ↔ Soul (many-to-many, ordered per side)
CREATE TABLE card_side_soul (
    card_side_id    integer     NOT NULL REFERENCES card_side (id) ON DELETE CASCADE,
    soul_id         smallint    NOT NULL REFERENCES soul (id),
    position        smallint    NOT NULL,
    PRIMARY KEY (card_side_id, soul_id),
    UNIQUE (card_side_id, position)
);

-- ---------------------------------------------------------------------------
-- Ability (deduplicated by exact text)
-- ---------------------------------------------------------------------------

CREATE TABLE ability (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    text            text        NOT NULL UNIQUE,    -- raw text with placeholders/markers
    search_text     text        NOT NULL            -- stripped text for LIKE search
);

CREATE INDEX idx_ability_search_text ON ability (search_text);

-- ---------------------------------------------------------------------------
-- Printing
-- ---------------------------------------------------------------------------

CREATE TABLE printing (
    id                  integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id             integer     NOT NULL REFERENCES card (id),
    set_id              integer     NOT NULL REFERENCES card_set (id),
    official_site_id    text        UNIQUE,          -- Takara Tomy website ID
    collector_number    text,
    rarity_id           smallint    REFERENCES rarity (id),
    illustrator_id      integer     REFERENCES illustrator (id),
    treatment_id        smallint    REFERENCES treatment (id)
);

CREATE INDEX idx_printing_card_id ON printing (card_id);
CREATE INDEX idx_printing_set_id ON printing (set_id);
CREATE INDEX idx_printing_rarity_id ON printing (rarity_id);
CREATE INDEX idx_printing_illustrator_id ON printing (illustrator_id);

-- ---------------------------------------------------------------------------
-- Printing Side (per printing × per card side)
-- ---------------------------------------------------------------------------

CREATE TABLE printing_side (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    printing_id     integer     NOT NULL REFERENCES printing (id) ON DELETE CASCADE,
    card_side_id    integer     NOT NULL REFERENCES card_side (id),
    flavor_text     text,
    image_filename  text,
    UNIQUE (printing_id, card_side_id)
);

CREATE INDEX idx_printing_side_printing_id ON printing_side (printing_id);

-- Printing Side ↔ Ability (many-to-many, ordered with indent)
CREATE TABLE printing_side_ability (
    printing_side_id    integer     NOT NULL REFERENCES printing_side (id) ON DELETE CASCADE,
    ability_id          integer     NOT NULL REFERENCES ability (id),
    position            smallint    NOT NULL,        -- 0-based order within side
    indent_level        smallint    NOT NULL DEFAULT 0,
    PRIMARY KEY (printing_side_id, ability_id, position),
    UNIQUE (printing_side_id, position)
);

-- ---------------------------------------------------------------------------
-- Tags
-- ---------------------------------------------------------------------------

-- Card ↔ Public Tag (many-to-many, admin-curated)
CREATE TABLE card_public_tag (
    card_id         integer     NOT NULL REFERENCES card (id) ON DELETE CASCADE,
    public_tag_id   integer     NOT NULL REFERENCES public_tag (id),
    PRIMARY KEY (card_id, public_tag_id)
);

-- ---------------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------------

CREATE TABLE app_user (
    id              uuid        DEFAULT uuidv7() PRIMARY KEY,
    username        text        NOT NULL UNIQUE,
    password_hash   text        NOT NULL,
    display_name    text        NOT NULL,
    avatar_path     text,                           -- filename/path, resolved by app
    is_admin        boolean     NOT NULL DEFAULT false
);

-- Private Tag (user-created, applicable to cards and printings)
CREATE TABLE private_tag (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES app_user (id),
    name            text        NOT NULL,
    UNIQUE (user_id, name)
);

-- Card ↔ Private Tag (scoped per user)
CREATE TABLE card_private_tag (
    card_id         integer     NOT NULL REFERENCES card (id) ON DELETE CASCADE,
    private_tag_id  integer     NOT NULL REFERENCES private_tag (id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, private_tag_id)
);

-- Printing ↔ Private Tag (scoped per user)
CREATE TABLE printing_private_tag (
    printing_id     integer     NOT NULL REFERENCES printing (id) ON DELETE CASCADE,
    private_tag_id  integer     NOT NULL REFERENCES private_tag (id) ON DELETE CASCADE,
    PRIMARY KEY (printing_id, private_tag_id)
);

-- ---------------------------------------------------------------------------
-- Collection
-- ---------------------------------------------------------------------------

CREATE TABLE collection_entry (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES app_user (id),
    printing_id     integer     NOT NULL REFERENCES printing (id),
    quantity        integer     NOT NULL CHECK (quantity > 0),
    UNIQUE (user_id, printing_id)
);

CREATE TABLE collection_history_entry (
    id              bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES app_user (id),
    label           text,
    printing_id     integer     NOT NULL REFERENCES printing (id),
    previous_qty    integer     NOT NULL,
    new_qty         integer     NOT NULL,
    changed_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_collection_history_user_id ON collection_history_entry (user_id);
CREATE INDEX idx_collection_history_printing_id ON collection_history_entry (printing_id);
CREATE INDEX idx_collection_history_changed_at ON collection_history_entry (changed_at);

-- ---------------------------------------------------------------------------
-- Deck & Versioning
-- ---------------------------------------------------------------------------

CREATE TABLE deck (
    id              uuid        DEFAULT uuidv7() PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES app_user (id),
    name            text        NOT NULL,
    description     text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_deck_user_id ON deck (user_id);

CREATE TABLE deck_version (
    id              uuid        DEFAULT uuidv7() PRIMARY KEY,
    deck_id         uuid        NOT NULL REFERENCES deck (id) ON DELETE CASCADE,
    is_draft        boolean     NOT NULL DEFAULT false,
    created_at      timestamptz NOT NULL DEFAULT now()
);

-- At most one draft per deck
CREATE UNIQUE INDEX uq_deck_one_draft ON deck_version (deck_id) WHERE is_draft = true;

CREATE INDEX idx_deck_version_deck_id ON deck_version (deck_id);

CREATE TABLE deck_version_entry (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    deck_version_id uuid        NOT NULL REFERENCES deck_version (id) ON DELETE CASCADE,
    card_id         integer     NOT NULL REFERENCES card (id),
    printing_id     integer     REFERENCES printing (id),  -- null = unspecified printing
    quantity        integer     NOT NULL CHECK (quantity > 0),
    UNIQUE (deck_version_id, card_id, printing_id)
);

-- At most one unspecified-printing row per card per version
CREATE UNIQUE INDEX uq_deck_version_entry_unspecified
    ON deck_version_entry (deck_version_id, card_id) WHERE printing_id IS NULL;

CREATE INDEX idx_deck_version_entry_version_id ON deck_version_entry (deck_version_id);

-- ---------------------------------------------------------------------------
-- Wishlist
-- ---------------------------------------------------------------------------

CREATE TABLE wishlist (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES app_user (id),
    name            text        NOT NULL DEFAULT 'Default',
    UNIQUE (user_id, name)
);

CREATE TABLE wishlist_entry (
    id              integer     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wishlist_id     integer     NOT NULL REFERENCES wishlist (id) ON DELETE CASCADE,
    card_id         integer     NOT NULL REFERENCES card (id),
    printing_id     integer     REFERENCES printing (id),  -- null = any printing
    quantity        integer     NOT NULL CHECK (quantity > 0),
    UNIQUE NULLS NOT DISTINCT (wishlist_id, card_id, printing_id)
);

CREATE INDEX idx_wishlist_entry_wishlist_id ON wishlist_entry (wishlist_id);
