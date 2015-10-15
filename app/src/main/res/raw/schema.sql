CREATE TABLE `entries` (
  `_id` INTEGER PRIMARY KEY,
  `cat` INTEGER,
  `title` TEXT COLLATE NOCASE,
  `maker` INTEGER,
  `price` TEXT COLLATE NOCASE,
  `location` TEXT COLLATE NOCASE,
  `date` INTEGER,
  `rating` REAL,
  `notes` TEXT COLLATE NOCASE
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
  `deleted` INTEGER DEFAULT 0
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
  `preset` INTEGER DEFAULT 0
);
--
CREATE TRIGGER `delete_entry` AFTER DELETE ON `entries`
BEGIN
    DELETE FROM `entries_flavors` WHERE `entry` = OLD.`_id`;
    DELETE FROM `entries_extras` WHERE `entry` = OLD.`_id`;
    DELETE FROM `photos` WHERE `entry` = OLD.`_id`;
    DELETE FROM `makers` WHERE `_id` = OLD.`maker`
     AND NOT EXISTS (SELECT 1 FROM `entries` WHERE `maker` = OLD.`maker`);
END;
--
CREATE TRIGGER `update_entry` AFTER UPDATE OF `maker` ON `entries`
BEGIN
    DELETE FROM `makers` WHERE `_id` = OLD.`maker`
     AND NOT EXISTS (SELECT 1 FROM `entries` WHERE `maker` = OLD.`maker`);
END;
--
CREATE TRIGGER `delete_cat` AFTER DELETE ON `cats`
BEGIN
    DELETE FROM `entries` WHERE `cat` = OLD.`_id`;
    DELETE FROM `extras` WHERE `cat` = OLD.`_id`;
    DELETE FROM `flavors` WHERE `cat` = OLD.`_id`;
END;
--
CREATE TRIGGER `delete_entry_extra` AFTER DELETE ON `entries_extras`
BEGIN
    DELETE FROM `extras` WHERE `deleted` = 1
     AND NOT EXISTS (SELECT 1 FROM `entries_extras` WHERE `extra` = `extras`.`_id`);
END;
--
CREATE TRIGGER `update_entry_extra` AFTER UPDATE OF `extra` ON `entries_extras`
BEGIN
    DELETE FROM `extras` WHERE `deleted` = 1
     AND NOT EXISTS (SELECT 1 FROM `entries_extras` WHERE `extra` = `extras`.`_id`);
END;
--
CREATE TRIGGER `delete_extra` BEFORE DELETE ON `extras`
BEGIN
    UPDATE `extras` SET `deleted` = 1 WHERE `_id` = OLD.`_id`;
    SELECT RAISE(IGNORE) WHERE EXISTS (SELECT 1 FROM `entries_extras` WHERE `extra` = OLD.`_id`);
END;
--
CREATE VIEW `view_entry` AS SELECT
a.`_id` AS `_id`,
a.`cat` AS `cat_id`,
b.`name` AS `cat`,
a.`title` AS `title`,
a.`maker` AS `maker_id`,
c.`name` AS `maker`,
c.`location` AS `origin`,
a.`location` AS `location`,
a.`date` AS `date`,
a.`price` AS `price`,
a.`rating` AS `rating`,
a.`notes` AS `notes`
FROM `entries` a LEFT JOIN `cats` b LEFT JOIN `makers` c
WHERE a.`cat` = b.`_id` AND a.`maker` = c.`_id`;
--
CREATE VIEW `view_entry_extra` AS SELECT
a.`_id` AS `_id`,
a.`entry` AS `entry`,
a.`extra` AS `extra`,
b.`name` AS `name`,
a.`value` AS `value`,
b.`preset` AS `preset`
FROM `entries_extras` a LEFT JOIN `extras` b
WHERE a.`extra` = b.`_id`;
--
CREATE VIEW `view_cat` AS SELECT
*,
(SELECT COUNT() FROM `entries` WHERE `cat` = `cats`.`_id`) AS `num_entries`
FROM `cats`;
