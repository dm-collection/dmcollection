ALTER TABLE card_side ADD COLUMN name_ruby jsonb;

ALTER TABLE card ADD COLUMN search_name text NOT NULL DEFAULT '';
ALTER TABLE card ADD COLUMN search_reading text NOT NULL DEFAULT '';
