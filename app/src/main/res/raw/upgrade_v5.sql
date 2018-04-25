BEGIN TRANSACTION;
--
DROP VIEW IF EXISTS view_entry;
--
DROP VIEW IF EXISTS view_entry_extra;
--
DROP TABLE IF EXISTS deleted;
--
CREATE TABLE new_cats (
  _id INTEGER PRIMARY KEY,
  name  TEXT COLLATE NOCASE,
  preset INTEGER DEFAULT 0
);
--
INSERT INTO new_cats (_id, name, preset) SELECT _id, name, preset FROM cats;
--
DROP TABLE cats;
--
ALTER TABLE new_cats RENAME TO cats;
--
CREATE TABLE new_entries (
  _id INTEGER PRIMARY KEY,
  uuid TEXT NOT NULL ON CONFLICT FAIL,
  cat INTEGER,
  title TEXT COLLATE NOCASE,
  maker INTEGER,
  price TEXT COLLATE NOCASE,
  location TEXT COLLATE NOCASE,
  date INTEGER,
  rating REAL,
  notes TEXT COLLATE NOCASE,
  UNIQUE(uuid) ON CONFLICT FAIL
);
--
INSERT INTO new_entries (_id, uuid, cat, title, maker, price, location, date, rating, notes) SELECT _id, uuid, cat, title, maker, price, location, date, rating, notes FROM entries;
--
DROP TABLE entries;
--
ALTER TABLE new_entries RENAME TO entries;
--
CREATE TABLE new_extras (
  _id INTEGER PRIMARY KEY,
  cat INTEGER,
  name TEXT,
  pos INTEGER DEFAULT 0,
  preset INTEGER DEFAULT 0,
  deleted INTEGER DEFAULT 0
);
--
INSERT INTO new_extras (_id, cat, name, pos, preset, deleted) SELECT _id, cat, name, pos, preset, deleted FROM extras;
--
DROP TABLE extras;
--
ALTER TABLE new_extras RENAME TO extras;
--
CREATE TABLE new_photos (
  _id INTEGER PRIMARY KEY,
  entry INTEGER,
  hash TEXT,
  path TEXT,
  pos INTEGER DEFAULT 0,
  UNIQUE(entry, hash) ON CONFLICT REPLACE
);
--
INSERT INTO new_photos (_id, entry, hash, path, pos) SELECT _id, entry, hash, path, pos FROM photos;
--
DROP TABLE photos;
--
ALTER TABLE new_photos RENAME TO photos;
--
END TRANSACTION;
