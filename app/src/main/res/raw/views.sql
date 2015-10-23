CREATE VIEW IF NOT EXISTS `view_entry` AS SELECT
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
a.`notes` AS `notes`,
a.`updated` AS `updated`,
a.`remote_id` AS `remote_id`
FROM `entries` a LEFT JOIN `cats` b LEFT JOIN `makers` c
WHERE a.`cat` = b.`_id` AND a.`maker` = c.`_id`;
--
CREATE VIEW IF NOT EXISTS `view_entry_extra` AS SELECT
a.`_id` AS `_id`,
a.`entry` AS `entry`,
a.`extra` AS `extra`,
b.`name` AS `name`,
a.`value` AS `value`,
b.`preset` AS `preset`,
b.`deleted` AS `deleted`,
b.`remote_id` AS `remote_id`
FROM `entries_extras` a LEFT JOIN `extras` b
WHERE a.`extra` = b.`_id`;
--
CREATE VIEW IF NOT EXISTS `view_cat` AS SELECT
*,
(SELECT COUNT() FROM `entries` WHERE `cat` = `cats`.`_id`) AS `num_entries`
FROM `cats`;
