CREATE TABLE `entries` (
  `_id` INTEGER PRIMARY KEY,
  `cat` INTEGER,
  `title` TEXT COLLATE NOCASE,
  `maker` INTEGER,
  `price` TEXT COLLATE NOCASE,
  `location` TEXT COLLATE NOCASE,
  `date` INTEGER,
  `rating` REAL,
  `notes` TEXT COLLATE NOCASE,
  `updated` INTEGER,
  `deleted` INTEGER DEFAULT 0,
  `remote_id` INTEGER DEFAULT 0
);
--
CREATE TABLE `entries_extras` (
  `_id` INTEGER PRIMARY KEY,
  `entry` INTEGER,
  `extra` INTEGER,
  `value` TEXT COLLATE NOCASE,
  UNIQUE(`entry`, `extra`) ON CONFLICT REPLACE
);
--
CREATE TABLE `entries_flavors` (
  `_id` INTEGER PRIMARY KEY,
  `entry` INTEGER,
  `flavor` TEXT COLLATE NOCASE,
  `value` INTEGER
);
--
CREATE TABLE `extras` (
  `_id` INTEGER PRIMARY KEY,
  `cat` INTEGER,
  `name` TEXT,
  `preset` INTEGER DEFAULT 0,
  `deleted` INTEGER DEFAULT 0,
  `remote_id` INTEGER DEFAULT 0
);
--
CREATE TABLE `flavors` (
  `_id` INTEGER PRIMARY KEY,
  `cat` INTEGER,
  `name` TEXT
);
--
CREATE TABLE `locations` (
  `_id` INTEGER PRIMARY KEY,
  `lat` REAL,
  `lon` REAL,
  `name` TEXT,
  UNIQUE(`lat`, `lon`) ON CONFLICT REPLACE
);
--
CREATE TABLE `makers` (
  `_id` INTEGER PRIMARY KEY,
  `name` TEXT COLLATE NOCASE,
  `location`  TEXT COLLATE NOCASE,
  UNIQUE(`name`, `location`) ON CONFLICT IGNORE
);
--
CREATE TABLE `photos` (
  `_id` INTEGER PRIMARY KEY,
  `entry` INTEGER,
  `path` TEXT,
  `drive_id` TEXT
);
--
CREATE TABLE `cats` (
  `_id` INTEGER PRIMARY KEY,
  `name`  TEXT COLLATE NOCASE,
  `preset` INTEGER DEFAULT 0,
  `updated` INTEGER,
  `deleted` INTEGER DEFAULT 0,
  `remote_id` INTEGER DEFAULT 0
);
