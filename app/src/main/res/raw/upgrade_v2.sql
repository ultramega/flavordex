DROP TRIGGER IF EXISTS delete_cat;
--
DROP VIEW IF EXISTS view_entry;
--
DROP VIEW IF EXISTS view_entry_extra;
--
CREATE TABLE cats2 (
  _id INTEGER PRIMARY KEY,
  uuid TEXT,
  name  TEXT COLLATE NOCASE,
  preset INTEGER DEFAULT 0,
  updated INTEGER DEFAULT 0,
  published INTEGER DEFAULT 0,
  synced INTEGER DEFAULT 0,
  UNIQUE(uuid) ON CONFLICT FAIL
);
--
INSERT INTO cats2 (_id, name, preset) SELECT * FROM cats;
--
DROP TABLE cats;
--
ALTER TABLE cats2 RENAME TO cats;
--
CREATE TABLE entries2 (
  _id INTEGER PRIMARY KEY,
  uuid TEXT,
  cat INTEGER,
  title TEXT COLLATE NOCASE,
  maker INTEGER,
  price TEXT COLLATE NOCASE,
  location TEXT COLLATE NOCASE,
  date INTEGER,
  rating REAL,
  notes TEXT COLLATE NOCASE,
  updated INTEGER DEFAULT 0,
  published INTEGER DEFAULT 0,
  synced INTEGER DEFAULT 0,
  UNIQUE(uuid) ON CONFLICT FAIL
);
--
INSERT INTO entries2 (_id, cat, title, maker, price, location, date, rating, notes) SELECT * FROM entries;
--
DROP TABLE entries;
--
ALTER TABLE entries2 RENAME TO entries;
--
CREATE TABLE extras2 (
  _id INTEGER PRIMARY KEY,
  uuid TEXT,
  cat INTEGER,
  name TEXT,
  pos INTEGER DEFAULT 0,
  preset INTEGER DEFAULT 0,
  deleted INTEGER DEFAULT 0,
  UNIQUE(uuid) ON CONFLICT FAIL
);
--
INSERT INTO extras2 (_id, cat, name, preset, deleted) SELECT * FROM extras;
--
DROP TABLE extras;
--
ALTER TABLE extras2 RENAME TO extras;
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
ALTER TABLE entries_flavors ADD COLUMN pos INTEGER DEFAULT 0;
--
ALTER TABLE flavors ADD COLUMN pos INTEGER DEFAULT 0;
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
  uuid TEXT,
  time INTEGER DEFAULT (CAST((julianday('now') - 2440587.5)*86400000.0 AS INTEGER))
);
