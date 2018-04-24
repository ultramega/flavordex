BEGIN TRANSACTION;
--
DROP VIEW IF EXISTS view_entry;
--
DROP VIEW IF EXISTS view_entry_extra;
--
CREATE TABLE new_entries (
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
  UNIQUE(uuid) ON CONFLICT FAIL
);
--
INSERT INTO new_entries (_id, cat, title, maker, price, location, date, rating, notes) SELECT * FROM entries;
--
DROP TABLE entries;
--
ALTER TABLE new_entries RENAME TO entries;
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
INSERT INTO new_photos (_id, entry, path) SELECT * FROM photos;
--
DROP TABLE photos;
--
ALTER TABLE new_photos RENAME TO photos;
--
ALTER TABLE entries_flavors ADD COLUMN pos INTEGER DEFAULT 0;
--
ALTER TABLE extras ADD COLUMN pos INTEGER DEFAULT 0;
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
END TRANSACTION;
