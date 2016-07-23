DROP VIEW IF EXISTS view_entry;
--
ALTER TABLE entries ADD COLUMN shared INTEGER DEFAULT 0;
--
ALTER TABLE entries ADD COLUMN link TEXT;
--
UPDATE entries SET updated = 0 WHERE synced = 1;
