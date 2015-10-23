CREATE TRIGGER IF NOT EXISTS `delete_entry` AFTER DELETE ON `entries`
BEGIN
    DELETE FROM `entries_flavors` WHERE `entry` = OLD.`_id`;
    DELETE FROM `entries_extras` WHERE `entry` = OLD.`_id`;
    DELETE FROM `photos` WHERE `entry` = OLD.`_id`;
    DELETE FROM `makers` WHERE `_id` = OLD.`maker`
     AND NOT EXISTS (SELECT 1 FROM `entries` WHERE `maker` = OLD.`maker`);
END;
--
CREATE TRIGGER IF NOT EXISTS `update_entry` AFTER UPDATE OF `maker` ON `entries`
BEGIN
    DELETE FROM `makers` WHERE `_id` = OLD.`maker`
     AND NOT EXISTS (SELECT 1 FROM `entries` WHERE `maker` = OLD.`maker`);
END;
--
CREATE TRIGGER IF NOT EXISTS `delete_cat` AFTER DELETE ON `cats`
BEGIN
    DELETE FROM `entries` WHERE `cat` = OLD.`_id`;
    DELETE FROM `extras` WHERE `cat` = OLD.`_id`;
    DELETE FROM `flavors` WHERE `cat` = OLD.`_id`;
    DELETE FROM `deleted` WHERE `cat` = OLD.`_id`;
END;
--
CREATE TRIGGER IF NOT EXISTS `delete_entry_extra` AFTER DELETE ON `entries_extras`
BEGIN
    DELETE FROM `extras` WHERE `deleted` = 1
     AND NOT EXISTS (SELECT 1 FROM `entries_extras` WHERE `extra` = `extras`.`_id`);
END;
--
CREATE TRIGGER IF NOT EXISTS `update_entry_extra` AFTER UPDATE OF `extra` ON `entries_extras`
BEGIN
    DELETE FROM `extras` WHERE `deleted` = 1
     AND NOT EXISTS (SELECT 1 FROM `entries_extras` WHERE `extra` = `extras`.`_id`);
END;
--
CREATE TRIGGER IF NOT EXISTS `delete_extra` BEFORE DELETE ON `extras`
BEGIN
    UPDATE `extras` SET `deleted` = 1 WHERE `_id` = OLD.`_id`;
    SELECT RAISE(IGNORE) WHERE EXISTS (SELECT 1 FROM `entries_extras` WHERE `extra` = OLD.`_id`);
END;
