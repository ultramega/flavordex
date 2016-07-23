CREATE TRIGGER IF NOT EXISTS delete_entry AFTER DELETE ON entries
BEGIN
    DELETE FROM entries_flavors WHERE entry = OLD._id;
    DELETE FROM entries_extras WHERE entry = OLD._id;
    DELETE FROM photos WHERE entry = OLD._id;
    DELETE FROM makers WHERE _id = OLD.maker
     AND NOT EXISTS (SELECT 1 FROM entries WHERE maker = OLD.maker);
END;
--
CREATE TRIGGER IF NOT EXISTS log_delete_entry AFTER DELETE ON entries
WHEN OLD.published = 1
BEGIN
    INSERT INTO deleted (type, cat, uuid) VALUES (1, OLD.cat, OLD.uuid);
END;
--
CREATE TRIGGER IF NOT EXISTS update_entry AFTER UPDATE OF maker ON entries
BEGIN
    DELETE FROM makers WHERE _id = OLD.maker
     AND NOT EXISTS (SELECT 1 FROM entries WHERE maker = OLD.maker);
END;
--
CREATE TRIGGER IF NOT EXISTS delete_cat AFTER DELETE ON cats
BEGIN
    DELETE FROM entries WHERE cat = OLD._id;
    DELETE FROM extras WHERE cat = OLD._id;
    DELETE FROM flavors WHERE cat = OLD._id;
    DELETE FROM deleted WHERE cat = OLD._id;
END;
--
CREATE TRIGGER IF NOT EXISTS log_delete_cat AFTER DELETE ON cats
WHEN OLD.published = 1
BEGIN
    INSERT INTO deleted (type, uuid) VALUES (0, OLD.uuid);
END;
--
CREATE TRIGGER IF NOT EXISTS delete_entry_extra AFTER DELETE ON entries_extras
BEGIN
    DELETE FROM extras WHERE deleted = 1
     AND NOT EXISTS (SELECT 1 FROM entries_extras WHERE extra = extras._id);
END;
--
CREATE TRIGGER IF NOT EXISTS update_entry_extra AFTER UPDATE OF extra ON entries_extras
BEGIN
    DELETE FROM extras WHERE deleted = 1
     AND NOT EXISTS (SELECT 1 FROM entries_extras WHERE extra = extras._id);
END;
--
CREATE TRIGGER IF NOT EXISTS delete_extra BEFORE DELETE ON extras
BEGIN
    UPDATE extras SET deleted = 1 WHERE _id = OLD._id;
    SELECT RAISE(IGNORE) WHERE EXISTS (SELECT 1 FROM entries_extras WHERE extra = OLD._id);
END;
--
CREATE TRIGGER IF NOT EXISTS delete_photo AFTER DELETE ON photos
WHEN OLD.drive_id NOT NULL AND OLD.hash NOT NULL
 AND NOT EXISTS (SELECT 1 FROM photos WHERE hash = OLD.hash)
BEGIN
    INSERT INTO deleted (type, uuid) VALUES (2, OLD.hash);
END;
--
CREATE TRIGGER IF NOT EXISTS insert_photo AFTER INSERT ON photos
WHEN NEW.hash NOT NULL
BEGIN
    DELETE FROM deleted WHERE type = 2 AND uuid = NEW.hash;
END;
--
CREATE TRIGGER IF NOT EXISTS update_photo AFTER UPDATE ON photos
WHEN NEW.hash NOT NULL
BEGIN
    DELETE FROM deleted WHERE type = 2 AND uuid = NEW.hash;
END;
