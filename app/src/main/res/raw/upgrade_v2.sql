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
CREATE TABLE photos2 (
  _id INTEGER PRIMARY KEY,
  entry INTEGER,
  hash TEXT,
  path TEXT,
  drive_id TEXT,
  pos INTEGER DEFAULT 0,
  UNIQUE(entry, hash) ON CONFLICT IGNORE
);
--
INSERT INTO photos2 (_id, entry, path) SELECT * FROM photos;
--
DROP TABLE photos;
--
ALTER TABLE photos2 RENAME TO photos;
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
