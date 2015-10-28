DROP TRIGGER IF EXISTS delete_cat;
--
DROP VIEW IF EXISTS view_entry;
--
DROP VIEW IF EXISTS view_entry_extra;
--
ALTER TABLE cats ADD COLUMN uuid TEXT;
--
ALTER TABLE cats ADD COLUMN updated INTEGER DEFAULT 0;
--
ALTER TABLE cats ADD COLUMN published INTEGER DEFAULT 0;
--
ALTER TABLE entries ADD COLUMN uuid TEXT;
--
ALTER TABLE entries ADD COLUMN updated INTEGER DEFAULT 0;
--
ALTER TABLE entries ADD COLUMN published INTEGER DEFAULT 0;
--
ALTER TABLE entries_flavors ADD COLUMN pos INTEGER DEFAULT 0;
--
ALTER TABLE extras ADD COLUMN uuid TEXT;
--
ALTER TABLE extras ADD COLUMN pos INTEGER DEFAULT 0;
--
ALTER TABLE flavors ADD COLUMN pos INTEGER DEFAULT 0;
--
ALTER TABLE photos ADD COLUMN pos INTEGER DEFAULT 0;
--
ALTER TABLE photos ADD COLUMN drive_id TEXT;
--
UPDATE entries_flavors SET pos = _id;
--
UPDATE extras SET pos = _id;
--
UPDATE flavors SET pos = _id;
--
UPDATE photos SET pos = _id;
--
CREATE TABLE deleted (
  _id INTEGER PRIMARY KEY,
  type INTEGER,
  cat INTEGER,
  uuid TEXT
);
