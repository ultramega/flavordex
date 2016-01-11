package com.ultramegasoft.flavordex2.backend;

import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.utils.SystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database access helper.
 *
 * @author Steve Guidetti
 */
public class DatabaseHelper {
    private static final Logger LOGGER = Logger.getLogger(DatabaseHelper.class.getName());

    /**
     * The database connection URL
     */
    private static final String DB_URL_DEBUG = "jdbc:mysql://127.0.0.1:3306/flavordex2?user=root";
    private static final String DB_URL = "jdbc:mysql://api.flavordex.com:3306/flavordex?user=flavordex&password=7hLfRTyGT53bQzKc";

    /**
     * The database connection
     */
    private Connection mConnection;

    /**
     * The user ID to use for performing authorized requests
     */
    private long mUserId;

    /**
     * The database ID of the client making requests
     */
    private long mClientId;

    /**
     * Open a connection to the database.
     *
     * @throws SQLException
     */
    public void open() throws SQLException {
        close();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
                mConnection = DriverManager.getConnection(DB_URL);
            } else {
                mConnection = DriverManager.getConnection(DB_URL_DEBUG);
            }
        } catch(ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to the database", e);
        }
    }

    /**
     * Close the connection to the database.
     */
    public void close() {
        if(mConnection != null) {
            try {
                mConnection.close();
                mConnection = null;
            } catch(SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to close the database connection", e);
            }
        }
    }

    /**
     * Set the user to perform authorized requests.
     *
     * @param userEmail The user's email address
     * @throws SQLException
     */
    public void setUser(String userEmail) throws SQLException {
        mUserId = getUserId(userEmail);
    }

    /**
     * Get the current client ID
     *
     * @return The database ID of the client
     */
    public long getClientId() {
        return mClientId;
    }

    /**
     * Set the client to make requests.
     *
     * @param clientId The database ID of the client
     */
    public void setClientId(long clientId) {
        mClientId = clientId;
    }

    /**
     * Register a client device with the database.
     *
     * @param gcmId The Google Cloud Messaging ID
     * @return The RegistrationRecord
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public RegistrationRecord registerClient(String gcmId)
            throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        final RegistrationRecord record = new RegistrationRecord();

        String sql = "DELETE FROM clients WHERE user = ? AND gcm_id = ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setString(2, gcmId);
            stmt.executeUpdate();
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        sql = "INSERT INTO clients (user, gcm_id) VALUES (?, ?)";
        stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            stmt.setLong(1, mUserId);
            stmt.setString(2, gcmId);

            stmt.executeUpdate();
            final ResultSet result = stmt.getGeneratedKeys();
            try {
                if(result.next()) {
                    record.setClientId(result.getLong(1));
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return record;
    }

    /**
     * Unregister a client device from the database.
     *
     * @param clientId The database ID of the client
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public void unregisterClient(long clientId) throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        final String sql = "DELETE FROM clients WHERE id = ? AND user = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, clientId);
            stmt.setLong(2, mUserId);
            stmt.executeUpdate();
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Update the last sync time for the client.
     *
     * @param time The Unix timestamp
     * @throws SQLException
     */
    public void setSyncTime(long time) throws SQLException {
        final String sql = "UPDATE clients SET last_sync = ? WHERE user = ? AND id = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, time);
            stmt.setLong(2, mUserId);
            stmt.setLong(3, mClientId);
            stmt.executeUpdate();
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Get the Google Cloud Messaging ID for the specified client.
     *
     * @param clientId The database ID of the client
     * @return The Google Cloud Messaging ID
     * @throws SQLException
     */
    public String getGcmId(long clientId) throws SQLException {
        final String sql = "SELECT gcm_id FROM clients WHERE id = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, clientId);

            final ResultSet result = stmt.executeQuery();
            if(result.next()) {
                return result.getString(1);
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return null;
    }

    /**
     * Set the Google Cloud Messaging ID for the specified client.
     *
     * @param clientId The database ID of the client
     * @param gcmId    The Google Cloud ID
     * @throws SQLException
     */
    public void setGcmId(long clientId, String gcmId) throws SQLException {
        final String sql = "UPDATE clients SET gcm_id = ? WHERE id = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setString(1, gcmId);
            stmt.setLong(2, clientId);
            stmt.executeUpdate();
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Get a list of Google Cloud Messaging IDs for the current user.
     *
     * @return A map of client database IDs to Google Cloud Messaging IDs
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public HashMap<Long, String> listGcmIds() throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        final String sql = "SELECT id, gcm_id FROM clients WHERE user = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);

            final ResultSet result = stmt.executeQuery();
            try {
                final HashMap<Long, String> records = new HashMap<>();
                while(result.next()) {
                    records.put(result.getLong(1), result.getString(2));
                }

                return records;
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Get all entries updated by other clients since the client's last sync.
     *
     * @return The list of entries updated since the last sync
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public ArrayList<EntryRecord> getUpdatedEntries() throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        final ArrayList<EntryRecord> records = new ArrayList<>();
        EntryRecord record;

        String sql = "SELECT uuid FROM deleted WHERE user = ? AND client != ? AND sync_time > (SELECT last_sync FROM clients WHERE id = ?)";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setLong(2, mClientId);
            stmt.setLong(3, mClientId);

            final ResultSet result = stmt.executeQuery();
            try {
                while(result.next()) {
                    record = new EntryRecord();
                    record.setUuid(result.getString("uuid"));
                    record.setDeleted(true);
                    records.add(record);
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        sql = "SELECT a.*, b.uuid AS cat_uuid FROM entries a LEFT JOIN categories b ON a.cat = b.id WHERE a.user = ? AND a.client != ? AND a.sync_time > (SELECT last_sync FROM clients WHERE id = ?)";
        stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setLong(2, mClientId);
            stmt.setLong(3, mClientId);

            final ResultSet result = stmt.executeQuery();
            try {
                while(result.next()) {
                    record = new EntryRecord();
                    record.setId(result.getLong("id"));
                    record.setUuid(result.getString("uuid"));
                    record.setCat(result.getLong("cat"));
                    record.setCatUuid(result.getString("cat_uuid"));
                    record.setTitle(result.getString("title"));
                    record.setMaker(result.getString("maker"));
                    record.setOrigin(result.getString("origin"));
                    record.setPrice(result.getString("price"));
                    record.setLocation(result.getString("location"));
                    record.setDate(result.getLong("date"));
                    record.setRating(result.getFloat("rating"));
                    record.setNotes(result.getString("notes"));
                    record.setUpdated(result.getLong("updated"));

                    record.setExtras(getEntryExtras(record.getId()));
                    record.setFlavors(getEntryFlavors(record.getId()));
                    record.setPhotos(getEntryPhotos(record.getId()));

                    records.add(record);
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return records;
    }

    /**
     * Get the extras for an entry.
     *
     * @param entryId The database ID of the entry
     * @return The list of extras
     * @throws SQLException
     */
    private ArrayList<ExtraRecord> getEntryExtras(long entryId) throws SQLException {
        final String sql = "SELECT a.uuid, a.name, b.value FROM extras a LEFT JOIN entries_extras b ON a.id = b.extra WHERE b.entry = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entryId);

            final ResultSet result = stmt.executeQuery();
            try {
                final ArrayList<ExtraRecord> records = new ArrayList<>();
                ExtraRecord record;
                while(result.next()) {
                    record = new ExtraRecord();
                    record.setUuid(result.getString("uuid"));
                    record.setName(result.getString("name"));
                    record.setValue(result.getString("value"));
                    records.add(record);
                }

                return records;
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Get the flavors for an entry.
     *
     * @param entryId The database ID of the entry
     * @return The list of flavors
     * @throws SQLException
     */
    private ArrayList<FlavorRecord> getEntryFlavors(long entryId) throws SQLException {
        final String sql = "SELECT flavor, value, pos FROM entries_flavors WHERE entry = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entryId);

            final ResultSet result = stmt.executeQuery();
            try {
                final ArrayList<FlavorRecord> records = new ArrayList<>();
                FlavorRecord record;
                while(result.next()) {
                    record = new FlavorRecord();
                    record.setName(result.getString("flavor"));
                    record.setValue(result.getInt("value"));
                    record.setPos(result.getInt("pos"));
                    records.add(record);
                }

                return records;
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Get the photos for an entry.
     *
     * @param entryId The database ID of the entry
     * @return The list of photos
     * @throws SQLException
     */
    private ArrayList<PhotoRecord> getEntryPhotos(long entryId) throws SQLException {
        final String sql = "SELECT hash, drive_id, pos FROM photos WHERE entry = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entryId);

            final ResultSet result = stmt.executeQuery();
            try {
                final ArrayList<PhotoRecord> records = new ArrayList<>();
                PhotoRecord record;
                while(result.next()) {
                    record = new PhotoRecord();
                    record.setHash(result.getString("hash"));
                    record.setDriveId(result.getString("drive_id"));
                    record.setPos(result.getInt("pos"));
                    records.add(record);
                }

                return records;
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Update the entry, inserting, updating, or deleting as indicated.
     *
     * @param entry The EntryRecord to update
     * @return Whether the operation was successful
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public boolean update(EntryRecord entry) throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        boolean success = false;
        try {
            mConnection.setAutoCommit(false);

            String sql = "SELECT id, cat FROM entries WHERE uuid = ? AND user = ?";
            PreparedStatement stmt = mConnection.prepareStatement(sql);
            try {
                stmt.setString(1, entry.getUuid());
                stmt.setLong(2, mUserId);

                final ResultSet result = stmt.executeQuery();
                try {
                    if(result.next()) {
                        entry.setId(result.getLong(1));
                        entry.setCat(result.getLong(2));
                    }
                } finally {
                    if(result != null) {
                        result.close();
                    }
                }
            } finally {
                if(stmt != null) {
                    stmt.close();
                }
            }

            if(entry.isDeleted()) {
                success = deleteEntry(entry);
            } else if(entry.getId() == 0) {
                sql = "SELECT id FROM categories WHERE uuid = ? AND user = ?";
                stmt = mConnection.prepareStatement(sql);
                try {
                    stmt.setString(1, entry.getCatUuid());
                    stmt.setLong(2, mUserId);

                    final ResultSet result = stmt.executeQuery();
                    try {
                        if(result.next()) {
                            entry.setCat(result.getLong(1));
                            success = insertEntry(entry);
                        }
                    } finally {
                        if(result != null) {
                            result.close();
                        }
                    }
                } finally {
                    if(stmt != null) {
                        stmt.close();
                    }
                }
            } else {
                success = updateEntry(entry);
            }

            mConnection.commit();
        } finally {
            mConnection.setAutoCommit(true);
        }

        return success;
    }

    /**
     * Insert a new entry.
     *
     * @param entry The EntryRecord to insert
     * @return Whether the operation was successful
     * @throws SQLException
     */
    private boolean insertEntry(EntryRecord entry) throws SQLException {
        int changed;
        String sql = "INSERT INTO entries (uuid, user, cat, title, maker, origin, price, location, date, rating, notes, updated, sync_time, client) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            stmt.setString(1, entry.getUuid());
            stmt.setLong(2, mUserId);
            stmt.setLong(3, entry.getCat());
            stmt.setString(4, entry.getTitle());
            stmt.setString(5, entry.getMaker());
            stmt.setString(6, entry.getOrigin());
            stmt.setString(7, entry.getPrice());
            stmt.setString(8, entry.getLocation());
            stmt.setLong(9, entry.getDate());
            stmt.setFloat(10, entry.getRating());
            stmt.setString(11, entry.getNotes());
            stmt.setLong(12, entry.getUpdated());
            stmt.setLong(13, System.currentTimeMillis());
            stmt.setLong(14, mClientId);
            changed = stmt.executeUpdate();

            final ResultSet result = stmt.getGeneratedKeys();
            try {
                if(result.next()) {
                    entry.setId(result.getLong(1));
                    insertEntryExtras(entry);
                    insertEntryFlavors(entry);
                    insertEntryPhotos(entry);
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        sql = "DELETE FROM deleted WHERE user = ? AND uuid = ?";
        stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setString(2, entry.getUuid());
            stmt.executeUpdate();
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return changed > 0;
    }

    /**
     * Update an entry.
     *
     * @param entry The EntryRecord to update
     * @return Whether the operation was successful
     * @throws SQLException
     */
    private boolean updateEntry(EntryRecord entry) throws SQLException {
        final String sql = "UPDATE entries SET title = ?, maker = ?, origin = ?, price = ?, location = ?, date = ?, rating = ?, notes = ?, updated = ?, sync_time = ?, client = ? WHERE user = ? AND id = ? AND updated < ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getMaker());
            stmt.setString(3, entry.getOrigin());
            stmt.setString(4, entry.getPrice());
            stmt.setString(5, entry.getLocation());
            stmt.setLong(6, entry.getDate());
            stmt.setFloat(7, entry.getRating());
            stmt.setString(8, entry.getNotes());
            stmt.setLong(9, entry.getUpdated());
            stmt.setLong(10, System.currentTimeMillis());
            stmt.setLong(11, mClientId);
            stmt.setLong(12, mUserId);
            stmt.setLong(13, entry.getId());
            stmt.setLong(14, entry.getUpdated());
            if(stmt.executeUpdate() > 0) {
                updateEntryExtras(entry);
                updateEntryFlavors(entry);
                updateEntryPhotos(entry);

                return true;
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return false;
    }

    /**
     * Insert the extras for an entry.
     *
     * @param entry The EntryRecord with extras to insert
     * @throws SQLException
     */
    private void insertEntryExtras(EntryRecord entry) throws SQLException {
        if(entry.getExtras() == null) {
            return;
        }

        final String sql = "INSERT INTO entries_extras (entry, extra, value) VALUES (?, (SELECT id FROM extras WHERE cat = ? AND uuid = ?), ?)";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entry.getId());
            stmt.setLong(2, entry.getCat());
            for(ExtraRecord extra : entry.getExtras()) {
                stmt.setString(3, extra.getUuid());
                stmt.setString(4, extra.getValue());
                stmt.executeUpdate();
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Update the extras for an entry.
     *
     * @param entry The EntryRecord with extras to update
     * @throws SQLException
     */
    private void updateEntryExtras(EntryRecord entry) throws SQLException {
        if(entry.getExtras() == null) {
            return;
        }

        final String sql = "DELETE FROM entries_extras WHERE entry = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entry.getId());
            stmt.executeUpdate();

            insertEntryExtras(entry);
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Insert the flavors for an entry.
     *
     * @param entry The EntryRecord with flavors to insert
     * @throws SQLException
     */
    private void insertEntryFlavors(EntryRecord entry) throws SQLException {
        if(entry.getFlavors() == null) {
            return;
        }

        final String sql = "INSERT INTO entries_flavors (entry, flavor, value, pos) VALUES (?, ?, ?, ?)";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entry.getId());
            for(FlavorRecord flavor : entry.getFlavors()) {
                stmt.setString(2, flavor.getName());
                stmt.setInt(3, flavor.getValue());
                stmt.setInt(4, flavor.getPos());
                stmt.executeUpdate();
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Update the flavors for an entry.
     *
     * @param entry The EntryRecord with flavors to update
     * @throws SQLException
     */
    private void updateEntryFlavors(EntryRecord entry) throws SQLException {
        if(entry.getFlavors() == null) {
            return;
        }

        final String sql = "DELETE FROM entries_flavors WHERE entry = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entry.getId());
            stmt.executeUpdate();

            insertEntryFlavors(entry);
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Insert the photos for an entry.
     *
     * @param entry The EntryRecord with photos to insert
     * @throws SQLException
     */
    private void insertEntryPhotos(EntryRecord entry) throws SQLException {
        if(entry.getPhotos() == null) {
            return;
        }

        final String sql = "INSERT INTO photos (entry, hash, drive_id, pos) VALUES (?, ?, ?, ?)";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entry.getId());
            for(PhotoRecord photo : entry.getPhotos()) {
                stmt.setString(2, photo.getHash());
                stmt.setString(3, photo.getDriveId());
                stmt.setInt(4, photo.getPos());
                stmt.executeUpdate();
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Update the photos for an entry.
     *
     * @param entry The EntryRecord with photos to update
     * @throws SQLException
     */
    private void updateEntryPhotos(EntryRecord entry) throws SQLException {
        if(entry.getPhotos() == null) {
            return;
        }

        final String sql = "DELETE FROM photos WHERE entry = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, entry.getId());
            stmt.executeUpdate();

            insertEntryPhotos(entry);
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Delete an entry. This will delete the entry and add its UUID to the deleted log.
     *
     * @param entry The entry to delete
     * @return Whether the operation was successful
     * @throws SQLException
     */
    private boolean deleteEntry(EntryRecord entry) throws SQLException {
        int updated;
        String sql = "DELETE FROM entries WHERE user = ? AND id = ? AND updated < ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setLong(2, entry.getId());
            stmt.setLong(3, entry.getUpdated());
            updated = stmt.executeUpdate();
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        if(updated > 0) {
            sql = "INSERT INTO deleted (user, type, cat, uuid, sync_time, client) VALUES (?, 'entry', ?, ?, ?, ?)";
            stmt = mConnection.prepareStatement(sql);
            try {
                stmt.setLong(1, mUserId);
                stmt.setLong(2, entry.getCat());
                stmt.setString(3, entry.getUuid());
                stmt.setLong(4, System.currentTimeMillis());
                stmt.setLong(5, mClientId);
                stmt.executeUpdate();

                return true;
            } finally {
                if(stmt != null) {
                    stmt.close();
                }
            }
        }

        return false;
    }

    /**
     * Get all categories updated by other clients since the client's last sync.
     *
     * @return The list of categories updated since the last sync
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public ArrayList<CatRecord> getUpdatedCats() throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        final ArrayList<CatRecord> records = new ArrayList<>();
        CatRecord record;

        String sql = "SELECT uuid FROM deleted WHERE user = ? AND client != ? AND sync_time > (SELECT last_sync FROM clients WHERE id = ?)";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setLong(2, mClientId);
            stmt.setLong(3, mClientId);

            final ResultSet result = stmt.executeQuery();
            try {
                while(result.next()) {
                    record = new CatRecord();
                    record.setUuid(result.getString("uuid"));
                    record.setDeleted(true);
                    records.add(record);
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        sql = "SELECT * FROM categories WHERE user = ? AND client != ? AND sync_time > (SELECT last_sync FROM clients WHERE id = ?)";
        stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setLong(2, mClientId);
            stmt.setLong(3, mClientId);

            final ResultSet result = stmt.executeQuery();
            try {
                while(result.next()) {
                    record = new CatRecord();
                    record.setId(result.getLong("id"));
                    record.setUuid(result.getString("uuid"));
                    record.setName(result.getString("name"));
                    record.setUpdated(result.getLong("updated"));

                    record.setExtras(getCatExtras(record.getId()));
                    record.setFlavors(getCatFlavors(record.getId()));

                    records.add(record);
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return records;
    }

    /**
     * Get the extras for a category.
     *
     * @param catId The database ID of the category
     * @return The list of extras
     * @throws SQLException
     */
    private ArrayList<ExtraRecord> getCatExtras(long catId) throws SQLException {
        final String sql = "SELECT uuid, name, pos, deleted FROM extras WHERE cat = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, catId);

            final ResultSet result = stmt.executeQuery();
            try {
                final ArrayList<ExtraRecord> records = new ArrayList<>();
                ExtraRecord record;
                while(result.next()) {
                    record = new ExtraRecord();
                    record.setUuid(result.getString("uuid"));
                    record.setName(result.getString("name"));
                    record.setPos(result.getInt("pos"));
                    record.setDeleted(result.getBoolean("deleted"));
                    records.add(record);
                }

                return records;
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Get the flavors for a category.
     *
     * @param catId The database ID of the category
     * @return The list of flavors
     * @throws SQLException
     */
    private ArrayList<FlavorRecord> getCatFlavors(long catId) throws SQLException {
        final String sql = "SELECT name, pos FROM flavors WHERE cat = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, catId);

            final ResultSet result = stmt.executeQuery();
            try {
                final ArrayList<FlavorRecord> records = new ArrayList<>();
                FlavorRecord record;
                while(result.next()) {
                    record = new FlavorRecord();
                    record.setName(result.getString("name"));
                    record.setPos(result.getInt("pos"));
                    records.add(record);
                }

                return records;
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Update the category, inserting, updating, or deleting as indicated.
     *
     * @param cat The CatRecord to update
     * @return Whether the operation was successful
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public boolean update(CatRecord cat) throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        boolean success = false;
        try {
            mConnection.setAutoCommit(false);

            final String sql = "SELECT id FROM categories WHERE uuid = ? AND user = ?";
            final PreparedStatement stmt = mConnection.prepareStatement(sql);
            try {
                stmt.setString(1, cat.getUuid());
                stmt.setLong(2, mUserId);

                final ResultSet result = stmt.executeQuery();
                try {
                    if(result.next()) {
                        cat.setId(result.getLong(1));
                    }

                    if(cat.isDeleted()) {
                        success = deleteCat(cat);
                    } else if(cat.getId() == 0) {
                        success = insertCat(cat);
                    } else {
                        success = updateCat(cat);
                    }

                    mConnection.commit();
                } finally {
                    if(result != null) {
                        result.close();
                    }
                }
            } finally {
                if(stmt != null) {
                    stmt.close();
                }
            }
        } finally {
            mConnection.setAutoCommit(true);
        }

        return success;
    }

    /**
     * Insert a new category.
     *
     * @param cat The CatRecord to insert
     * @return Whether the operation was successful
     * @throws SQLException
     */
    private boolean insertCat(CatRecord cat) throws SQLException {
        int changed;
        String sql = "INSERT INTO categories (uuid, user, name, updated, sync_time, client) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            stmt.setString(1, cat.getUuid());
            stmt.setLong(2, mUserId);
            stmt.setString(3, cat.getName());
            stmt.setLong(4, cat.getUpdated());
            stmt.setLong(5, System.currentTimeMillis());
            stmt.setLong(6, mClientId);
            changed = stmt.executeUpdate();

            final ResultSet result = stmt.getGeneratedKeys();
            try {
                if(result.next()) {
                    cat.setId(result.getLong(1));
                    insertCatExtras(cat);
                    insertCatFlavors(cat);
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        sql = "DELETE FROM deleted WHERE user = ? AND uuid = ?";
        stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setString(2, cat.getUuid());
            stmt.executeUpdate();
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return changed > 0;
    }

    /**
     * Update a category.
     *
     * @param cat The CatRecord to update
     * @return Whether the operation was successful
     * @throws SQLException
     */
    private boolean updateCat(CatRecord cat) throws SQLException {
        final String sql = "UPDATE categories SET name = ?, updated = ?, sync_time = ?, client = ? WHERE user = ? AND id = ? AND updated < ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setString(1, cat.getName());
            stmt.setLong(2, cat.getUpdated());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setLong(4, mClientId);
            stmt.setLong(5, mUserId);
            stmt.setLong(6, cat.getId());
            stmt.setLong(7, cat.getUpdated());
            if(stmt.executeUpdate() > 0) {
                updateCatExtras(cat);
                updateCatFlavors(cat);

                return true;
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return false;
    }

    /**
     * Insert the extras for a category.
     *
     * @param cat The CatRecord with extras to insert
     * @throws SQLException
     */
    private void insertCatExtras(CatRecord cat) throws SQLException {
        if(cat.getExtras() == null) {
            return;
        }

        final String sql = "INSERT INTO extras (uuid, cat, name, pos, deleted) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = ?, pos = ?, deleted = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            stmt.setLong(2, cat.getId());
            for(ExtraRecord extra : cat.getExtras()) {
                stmt.setString(1, extra.getUuid());
                stmt.setString(3, extra.getName());
                stmt.setInt(4, extra.getPos());
                stmt.setBoolean(5, extra.isDeleted());
                stmt.setString(6, extra.getName());
                stmt.setInt(7, extra.getPos());
                stmt.setBoolean(8, extra.isDeleted());
                stmt.executeUpdate();

                if(extra.getId() == 0) {
                    final ResultSet result = stmt.getGeneratedKeys();
                    try {
                        if(result.next()) {
                            extra.setId(result.getLong(1));
                        }
                    } finally {
                        if(result != null) {
                            result.close();
                        }
                    }
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Update the extras for a category.
     *
     * @param cat The CatRecord with extras to update
     * @throws SQLException
     */
    private void updateCatExtras(CatRecord cat) throws SQLException {
        if(cat.getExtras() == null) {
            return;
        }

        final ArrayList<String> extraUuids = new ArrayList<>();
        for(ExtraRecord extra : cat.getExtras()) {
            extraUuids.add(extra.getUuid());
        }

        String sql = "SELECT id, uuid FROM extras WHERE cat = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, cat.getId());

            final ResultSet result = stmt.executeQuery();
            try {
                sql = "DELETE FROM extras WHERE id = ?";
                final PreparedStatement stmt2 = mConnection.prepareStatement(sql);
                try {
                    while(result.next()) {
                        if(!extraUuids.contains(result.getString(2))) {
                            stmt2.setLong(1, result.getLong(1));
                            stmt2.executeUpdate();
                        }
                    }

                    insertCatExtras(cat);
                } finally {
                    if(stmt2 != null) {
                        stmt2.close();
                    }
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Insert the flavors for a category.
     *
     * @param cat The CatRecord with flavors to insert
     * @throws SQLException
     */
    private void insertCatFlavors(CatRecord cat) throws SQLException {
        if(cat.getFlavors() == null) {
            return;
        }

        final String sql = "INSERT INTO flavors (cat, name, pos) VALUES (?, ?, ?)";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, cat.getId());
            for(FlavorRecord flavor : cat.getFlavors()) {
                stmt.setString(2, flavor.getName());
                stmt.setInt(3, flavor.getPos());
                stmt.executeUpdate();
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Update the flavors for a category.
     *
     * @param cat The CatRecord with flavors to update
     * @throws SQLException
     */
    private void updateCatFlavors(CatRecord cat) throws SQLException {
        if(cat.getFlavors() == null) {
            return;
        }

        final String sql = "DELETE FROM flavors WHERE cat = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, cat.getId());
            stmt.executeUpdate();

            insertCatFlavors(cat);
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Delete a category. This will delete the category and add its UUID to the deleted log.
     *
     * @param cat The category to delete
     * @return Whether the operation was successful
     * @throws SQLException
     */
    private boolean deleteCat(CatRecord cat) throws SQLException {
        int changed;
        String sql = "DELETE FROM categories WHERE user = ? AND id = ? AND updated < ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setLong(1, mUserId);
            stmt.setLong(2, cat.getId());
            stmt.setLong(3, cat.getUpdated());
            changed = stmt.executeUpdate();
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        if(changed > 0) {
            sql = "INSERT INTO deleted (user, type, uuid, sync_time, client) VALUES (?, 'cat', ?, ?, ?)";
            stmt = mConnection.prepareStatement(sql);
            try {
                stmt.setLong(1, mUserId);
                stmt.setString(2, cat.getUuid());
                stmt.setLong(3, System.currentTimeMillis());
                stmt.setLong(4, mClientId);
                stmt.executeUpdate();

                return true;
            } finally {
                if(stmt != null) {
                    stmt.close();
                }
            }
        }

        return false;
    }

    /**
     * Get the database ID of a user, creating one if it doesn't exist.
     *
     * @param userEmail The user's email address
     * @return The user's database ID
     * @throws SQLException
     */
    private long getUserId(String userEmail) throws SQLException {
        String sql = "SELECT id FROM users WHERE email = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        try {
            stmt.setString(1, userEmail);

            final ResultSet result = stmt.executeQuery();
            try {
                if(result.next()) {
                    return result.getLong(1);
                } else {
                    sql = "INSERT INTO users (email) VALUES (?)";
                    final PreparedStatement stmt2 = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    try {
                        stmt2.setString(1, userEmail);
                        stmt2.executeUpdate();

                        final ResultSet result2 = stmt2.getGeneratedKeys();
                        try {
                            if(result2.next()) {
                                return result2.getLong(1);
                            }
                        } finally {
                            if(result2 != null) {
                                result2.close();
                            }
                        }
                    } finally {
                        if(stmt2 != null) {
                            stmt2.close();
                        }
                    }
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }

        return 0;
    }
}
