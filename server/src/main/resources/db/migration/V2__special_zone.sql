ALTER TABLE card
DROP CONSTRAINT card_deck_zone_check;

ALTER TABLE card
ADD CONSTRAINT card_deck_zone_check CHECK (deck_zone IN ('main', 'hyperspatial', 'gr', 'special'));