CREATE TABLE `entries` (
  `_id` INTEGER PRIMARY KEY,
  `type` INTEGER,
  `title` TEXT COLLATE NOCASE,
  `maker` INTEGER,
  `location` TEXT COLLATE NOCASE,
  `date` INTEGER,
  `price` TEXT COLLATE NOCASE,
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
  `flavor` INTEGER,
  `value` INTEGER,
  UNIQUE(`entry`, `flavor`) ON CONFLICT REPLACE
);
--
CREATE TABLE `extras` (
  `_id` INTEGER PRIMARY KEY,
  `type` INTEGER,
  `name` TEXT,
  `preset` INTEGER,
  `deleted` INTEGER
);
--
INSERT INTO `extras` VALUES
(1, 1, '_style', 1, 0),
(2, 1, '_serving', 1, 0),
(3, 1, '_stats_ibu', 1, 0),
(4, 1, '_stats_abv', 1, 0),
(5, 1, '_stats_og', 1, 0),
(6, 1, '_stats_fg', 1, 0),
(7, 2, '_varietal', 1, 0),
(8, 2, '_stats_vintage', 1, 0),
(9, 2, '_stats_abv', 1, 0),
(10, 3, '_style', 1, 0),
(11, 3, '_stats_age', 1, 0),
(12, 3, '_stats_abv', 1, 0),
(13, 4, '_roaster', 1, 0),
(14, 4, '_roast_date', 1, 0),
(15, 4, '_grind', 1, 0),
(16, 4, '_brew_method', 1, 0),
(17, 4, '_stats_dose', 1, 0),
(18, 4, '_stats_mass', 1, 0),
(19, 4, '_stats_temp', 1, 0),
(20, 4, '_stats_extime', 1, 0),
(21, 4, '_stats_tds', 1, 0),
(22, 4, '_stats_yield', 1, 0);
--
CREATE TABLE `flavors` (
  `_id` INTEGER PRIMARY KEY,
  `type` INTEGER,
  `name` TEXT,
  `deleted` INTEGER
);
--
INSERT INTO `flavors` VALUES
(1, 1, 'Body', 0),
(2, 1, 'Syrup', 0),
(3, 1, 'Fruit', 0),
(4, 1, 'Citrus', 0),
(5, 1, 'Hops', 0),
(6, 1, 'Linger', 0),
(7, 1, 'Spice', 0),
(8, 1, 'Herb', 0),
(9, 1, 'Malt', 0),
(10, 1, 'Alcohol', 0),
(11, 1, 'Sweet', 0),
(12, 1, 'Sour', 0),
(13, 1, 'Bitter', 0),
(14, 1, 'Astringent', 0),
(15, 2, 'Body', 0),
(16, 2, 'Fruit', 0),
(17, 2, 'Citrus', 0),
(18, 2, 'Berry', 0),
(19, 2, 'Floral', 0),
(20, 2, 'Spice', 0),
(21, 2, 'Herb', 0),
(22, 2, 'Nut', 0),
(23, 2, 'Earth', 0),
(24, 2, 'Wood', 0),
(25, 2, 'Caramel', 0),
(26, 2, 'Sweet', 0),
(27, 2, 'Sour', 0),
(28, 2, 'Astringent', 0),
(29, 2, 'Linger', 0),
(30, 2, 'Heat', 0),
(31, 3, 'Body', 0),
(32, 3, 'Charcoal', 0),
(33, 3, 'Oak', 0),
(34, 3, 'Leather', 0),
(35, 3, 'Spice', 0),
(36, 3, 'Alcohol', 0),
(37, 3, 'Astringent', 0),
(38, 3, 'Linger', 0),
(39, 3, 'Sweet', 0),
(40, 3, 'Maple', 0),
(41, 3, 'Fruit', 0),
(42, 3, 'Vanilla', 0),
(43, 3, 'Smoke', 0),
(44, 3, 'Peat', 0),
(45, 3, 'Nut', 0),
(46, 4, 'Body', 0),
(47, 4, 'Citrus', 0),
(48, 4, 'Berry', 0),
(49, 4, 'Floral', 0),
(50, 4, 'Spice', 0),
(51, 4, 'Smoke', 0),
(52, 4, 'Nut', 0),
(53, 4, 'Chocolate', 0),
(54, 4, 'Caramel', 0),
(55, 4, 'Sweet', 0),
(56, 4, 'Sour', 0),
(57, 4, 'Bitter', 0),
(58, 4, 'Salt', 0),
(59, 4, 'Finish', 0);
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
  `type` INTEGER,
  `name` TEXT COLLATE NOCASE,
  `location`  TEXT COLLATE NOCASE,
  UNIQUE(`type`, `name`, `location`) ON CONFLICT IGNORE
);
--
CREATE TABLE `photos` (
  `_id` INTEGER PRIMARY KEY,
  `entry` INTEGER,
  `path` TEXT,
  `from_gallery` INTEGER
);
--
CREATE TABLE `types` (
  `_id` INTEGER PRIMARY KEY,
  `name`  TEXT COLLATE NOCASE,
  `preset` INTEGER
);
--
INSERT INTO `types` VALUES
(1, '_beer', 1),
(2, '_wine', 1),
(3, '_whiskey', 1),
(4, '_coffee', 1);
--
CREATE TRIGGER `deleteentry` AFTER DELETE ON `entries`
BEGIN
    DELETE FROM `entries_flavors` WHERE `entry` = OLD.`_id`;
    DELETE FROM `entries_extras` WHERE `entry` = OLD.`_id`;
    DELETE FROM `photos` WHERE `entry` = OLD.`_id`;
    DELETE FROM `makers` WHERE (SELECT COUNT() FROM `entries` WHERE `maker` = OLD.`maker`) < 1;
    DELETE FROM `extras` WHERE `deleted` = 1
     AND (SELECT COUNT() FROM `entries_extras` WHERE `extra` = `extras`.`_id`) < 1;
    DELETE FROM `flavors` WHERE `deleted` = 1
     AND (SELECT COUNT() FROM `entries_extras` WHERE `extra` = `flavors`.`_id`) < 1;
END;
--
CREATE TRIGGER `updateentry` AFTER UPDATE OF `maker` ON `entries`
BEGIN
    DELETE FROM `makers` WHERE `_id` = OLD.`maker`
     AND (SELECT COUNT() FROM `entries` WHERE `maker` = OLD.`maker`) < 1;
END;
--
CREATE TRIGGER `updateentryextras` AFTER UPDATE OF `extra` ON `entries_extras`
BEGIN
    DELETE FROM `extras` WHERE `deleted` = 1
     AND (SELECT COUNT() FROM `entries_extras` WHERE `extra` = `extras`.`_id`) < 1;
END;
--
CREATE TRIGGER `updateentryflavors` AFTER UPDATE OF `flavor` ON `entries_flavors`
BEGIN
    DELETE FROM `flavors` WHERE `deleted` = 1
     AND (SELECT COUNT() FROM `entries_extras` WHERE `extra` = `flavors`.`_id`) < 1;
END;
--
CREATE TRIGGER `deleteentryextras` AFTER DELETE ON `entries_extras`
BEGIN
    DELETE FROM `extras` WHERE `deleted` = 1
     AND (SELECT COUNT() FROM `entries_extras` WHERE `extra` = `extras`.`_id`) < 1;
END;
--
CREATE TRIGGER `deleteentryflavors` AFTER DELETE ON `entries_flavors`
BEGIN
    DELETE FROM `flavors` WHERE `deleted` = 1
     AND (SELECT COUNT() FROM `entries_extras` WHERE `extra` = `flavors`.`_id`) < 1;
END;
--
CREATE TRIGGER `deleteextra` BEFORE DELETE ON `extras`
BEGIN
    UPDATE `extras` SET `deleted` = 1 WHERE `_id` = OLD.`_id`;
    SELECT RAISE(IGNORE) WHERE EXISTS (SELECT 1 FROM `entries_extras` WHERE `extra` = OLD.`_id`);
END;
--
CREATE TRIGGER `deleteflavor` BEFORE DELETE ON `flavors`
BEGIN
    UPDATE `flavors` SET `deleted` = 1 WHERE `_id` = OLD.`_id`;
    SELECT RAISE(IGNORE) WHERE EXISTS (SELECT 1 FROM `entries_flavors` WHERE `flavor` = OLD.`_id`);
END;
--
CREATE VIEW `viewentry` AS SELECT
a.`_id` AS `_id`,
a.`type` AS `type_id`,
b.`name` AS `type`,
a.`title` AS `title`,
a.`maker` AS `maker_id`,
c.`name` AS `maker`,
c.`location` AS `origin`,
a.`location` AS `location`,
a.`date` AS `date`,
a.`price` AS `price`,
a.`rating` AS `rating`,
a.`notes` AS `notes`
FROM `entries` a LEFT JOIN `types` b LEFT JOIN `makers` c
WHERE a.`type` = b.`_id` AND a.`maker` = c.`_id`;
--
CREATE VIEW `viewentryextras` AS SELECT
a.`_id` AS `_id`,
a.`entry` AS `entry`,
a.`extra` AS `extra`,
b.`name` AS `name`,
a.`value` AS `value`,
b.`preset` AS `preset`
FROM `entries_extras` a LEFT JOIN `extras` b
WHERE a.`extra` = b.`_id`;
--
CREATE VIEW `viewentryflavors` AS SELECT
a.`_id` AS `_id`,
a.`entry` AS `entry`,
a.`flavor` AS `flavor`,
b.`name` AS `name`,
a.`value` AS `value`
FROM `entries_flavors` a LEFT JOIN `flavors` b
WHERE a.`flavor` = b.`_id`;
