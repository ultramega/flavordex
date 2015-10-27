package com.ultramegasoft.flavordex2.backend;

import com.google.api.server.spi.response.UnauthorizedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Database access helper.
 *
 * @author Steve Guidetti
 */
public class DatabaseHelper {
    /**
     * The database connection URL
     */
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/flavordex2?user=root";

    /**
     * The database connection
     */
    private Connection mConnection;

    /**
     * The user ID to use for performing authorized requests
     */
    private long mUserId;

    /**
     * Open a connection to the database.
     *
     * @throws SQLException
     */
    public void open() throws SQLException {
        close();
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        mConnection = DriverManager.getConnection(DB_URL);
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
                e.printStackTrace();
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
        String sql = "SELECT id, gcm_id FROM clients WHERE user = ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, mUserId);

        ResultSet result = stmt.executeQuery();
        while(result.next()) {
            if(gcmId.equals(result.getString(2))) {
                record.setClientId(result.getLong(1));
                return record;
            }
        }

        sql = "INSERT INTO clients (user, gcm_id) VALUES (?, ?)";
        stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, mUserId);
        stmt.setString(2, gcmId);

        stmt.executeUpdate();
        result = stmt.getGeneratedKeys();
        if(result.next()) {
            record.setClientId(result.getLong(1));
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
        stmt.setLong(1, clientId);
        stmt.setLong(2, mUserId);
        stmt.executeUpdate();
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
        stmt.setLong(1, clientId);

        final ResultSet result = stmt.executeQuery();
        if(result.next()) {
            return result.getString(1);
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
        stmt.setString(1, gcmId);
        stmt.setLong(2, clientId);
        stmt.executeUpdate();
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
        stmt.setLong(1, mUserId);

        final ResultSet result = stmt.executeQuery();
        final HashMap<Long, String> records = new HashMap<>();
        while(result.next()) {
            records.put(result.getLong(1), result.getString(2));
        }

        return records;
    }

    /**
     * Get all entries updated since the specified time.
     *
     * @param since Unix timestamp with milliseconds
     * @return The list of entries updated since the specified time
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public ArrayList<EntryRecord> getUpdatedEntries(long since)
            throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }
        final ArrayList<EntryRecord> records = new ArrayList<>();
        EntryRecord record;

        String sql = "SELECT uuid FROM deleted WHERE user = ? AND time > ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, mUserId);
        stmt.setLong(2, since);

        ResultSet result = stmt.executeQuery();
        while(result.next()) {
            record = new EntryRecord();
            record.setUuid(result.getString("uuid"));
            record.setDeleted(true);
            records.add(record);
        }

        sql = "SELECT a.*, b.uuid AS cat_uuid FROM entries a LEFT JOIN categories b ON a.cat = b.id WHERE a.user = ? AND a.updated > ?";
        stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, mUserId);
        stmt.setLong(2, since);

        result = stmt.executeQuery();
        while(result.next()) {
            record = new EntryRecord();
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
        stmt.setLong(1, entryId);

        final ResultSet result = stmt.executeQuery();
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
        stmt.setLong(1, entryId);

        final ResultSet result = stmt.executeQuery();
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
    }

    /**
     * Get the photos for an entry.
     *
     * @param entryId The database ID of the entry
     * @return The list of photos
     * @throws SQLException
     */
    public ArrayList<PhotoRecord> getEntryPhotos(long entryId) throws SQLException {
        final String sql = "SELECT id, path, drive_id, pos FROM photos WHERE entry = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, entryId);

        final ResultSet result = stmt.executeQuery();
        final ArrayList<PhotoRecord> records = new ArrayList<>();
        PhotoRecord record;
        while(result.next()) {
            record = new PhotoRecord();
            record.setPath(result.getString("path"));
            record.setDriveId(result.getString("drive_id"));
            record.setPos(result.getInt("pos"));
            records.add(record);
        }

        return records;
    }

    /**
     * Update the entry, inserting, updating, or deleting as indicated.
     *
     * @param entry The EntryRecord to update
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public void update(EntryRecord entry) throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        try {
            mConnection.setAutoCommit(false);

            String sql = "SELECT id, cat FROM entries WHERE uuid = ? AND user = ?";
            PreparedStatement stmt = mConnection.prepareStatement(sql);
            stmt.setString(1, entry.getUuid());
            stmt.setLong(2, mUserId);

            ResultSet result = stmt.executeQuery();
            if(result.next()) {
                entry.setId(result.getLong(1));
                entry.setCat(result.getLong(2));
            }

            if(entry.getId() == 0) {
                sql = "SELECT id FROM categories WHERE uuid = ? AND user = ?";
                stmt = mConnection.prepareStatement(sql);
                stmt.setString(1, entry.getCatUuid());
                stmt.setLong(2, mUserId);

                result = stmt.executeQuery();
                if(result.next()) {
                    entry.setCat(result.getLong(1));
                }
                insertEntry(entry);
            } else if(entry.isDeleted()) {
                deleteEntry(entry);
            } else {
                updateEntry(entry);
            }

            mConnection.commit();
        } finally {
            mConnection.setAutoCommit(true);
        }
    }

    /**
     * Insert a new entry.
     *
     * @param entry The EntryRecord to insert
     * @throws SQLException
     */
    private void insertEntry(EntryRecord entry) throws SQLException {
        final String sql = "INSERT INTO entries (uuid, user, cat, title, maker, origin, price, location, date, rating, notes, updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final PreparedStatement stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
        stmt.setLong(12, System.currentTimeMillis());
        stmt.executeUpdate();

        final ResultSet result = stmt.getGeneratedKeys();
        if(result.next()) {
            entry.setId(result.getLong(1));
            insertEntryExtras(entry);
            insertEntryFlavors(entry);
            insertEntryPhotos(entry);
        }
    }

    /**
     * Update an entry.
     *
     * @param entry The EntryRecord to update
     * @throws SQLException
     */
    private void updateEntry(EntryRecord entry) throws SQLException {
        final String sql = "UPDATE entries SET title = ?, maker = ?, origin = ?, price = ?, location = ?, date = ?, rating = ?, notes = ?, updated = ? WHERE user = ? AND id = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setString(1, entry.getTitle());
        stmt.setString(2, entry.getMaker());
        stmt.setString(3, entry.getOrigin());
        stmt.setString(4, entry.getPrice());
        stmt.setString(5, entry.getLocation());
        stmt.setLong(6, entry.getDate());
        stmt.setFloat(7, entry.getRating());
        stmt.setString(8, entry.getNotes());
        stmt.setLong(9, System.currentTimeMillis());
        stmt.setLong(10, mUserId);
        stmt.setLong(11, entry.getId());
        if(stmt.executeUpdate() > 0) {
            updateEntryExtras(entry);
            updateEntryFlavors(entry);
            updateEntryPhotos(entry);
        }
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
        stmt.setLong(1, entry.getId());
        stmt.setLong(2, entry.getCat());
        for(ExtraRecord extra : entry.getExtras()) {
            stmt.setString(3, extra.getUuid());
            stmt.setString(4, extra.getValue());
            stmt.executeUpdate();
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
        stmt.setLong(1, entry.getId());
        stmt.executeUpdate();

        insertEntryExtras(entry);
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
        stmt.setLong(1, entry.getId());
        for(FlavorRecord flavor : entry.getFlavors()) {
            stmt.setString(2, flavor.getName());
            stmt.setInt(3, flavor.getValue());
            stmt.setInt(4, flavor.getPos());
            stmt.executeUpdate();
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
        stmt.setLong(1, entry.getId());
        stmt.executeUpdate();

        insertEntryFlavors(entry);
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
        final String sql = "INSERT INTO photos (entry, path, drive_id, pos) VALUES (?, ?, ?, ?)";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, entry.getId());
        for(PhotoRecord photo : entry.getPhotos()) {
            stmt.setString(2, photo.getPath());
            stmt.setString(3, photo.getDriveId());
            stmt.setInt(4, photo.getPos());
            stmt.executeUpdate();
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
        stmt.setLong(1, entry.getId());
        stmt.executeUpdate();

        insertEntryPhotos(entry);
    }

    /**
     * Delete an entry. This will delete the entry and add its UUID to the deleted log.
     *
     * @param entry The entry to delete
     * @throws SQLException
     */
    private void deleteEntry(EntryRecord entry) throws SQLException {
        String sql = "DELETE FROM entries WHERE user = ? AND id = ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, mUserId);
        stmt.setLong(2, entry.getId());
        if(stmt.executeUpdate() > 0) {
            sql = "INSERT INTO deleted (user, type, cat, uuid, time) VALUES (?, 'entry', ?, ?, ?)";
            stmt = mConnection.prepareStatement(sql);
            stmt.setLong(1, mUserId);
            stmt.setLong(2, entry.getCat());
            stmt.setString(3, entry.getUuid());
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    /**
     * Get all categories updated since the specified time.
     *
     * @param since Unix timestamp with milliseconds
     * @return The list of categories updated since the specified time
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public ArrayList<CatRecord> getUpdatedCats(long since)
            throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }
        final ArrayList<CatRecord> records = new ArrayList<>();
        CatRecord record;

        String sql = "SELECT uuid FROM deleted WHERE user = ? AND time > ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, mUserId);
        stmt.setLong(2, since);

        ResultSet result = stmt.executeQuery();
        while(result.next()) {
            record = new CatRecord();
            record.setUuid(result.getString(1));
            record.setDeleted(true);
            records.add(record);
        }

        sql = "SELECT * FROM categories WHERE user = ? AND updated > ?";
        stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, mUserId);
        stmt.setLong(2, since);

        result = stmt.executeQuery();
        while(result.next()) {
            record = new CatRecord();
            record.setUuid(result.getString("uuid"));
            record.setName(result.getString("name"));
            record.setUpdated(result.getLong("updated"));

            record.setExtras(getCatExtras(record.getId()));
            record.setFlavors(getCatFlavors(record.getId()));

            records.add(record);
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
        final String sql = "SELECT uuid, name, pos FROM extras WHERE cat = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, catId);

        final ResultSet result = stmt.executeQuery();
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
    }

    /**
     * Get the flavors for a category.
     *
     * @param catId The database ID of the category
     * @return The list of flavors
     * @throws SQLException
     */
    private ArrayList<FlavorRecord> getCatFlavors(long catId) throws SQLException {
        final String sql = "SELECT id, name, pos FROM flavors WHERE cat = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, catId);

        final ResultSet result = stmt.executeQuery();
        final ArrayList<FlavorRecord> records = new ArrayList<>();
        FlavorRecord record;
        while(result.next()) {
            record = new FlavorRecord();
            record.setId(result.getLong("id"));
            record.setName(result.getString("name"));
            record.setPos(result.getInt("pos"));
            records.add(record);
        }

        return records;
    }

    /**
     * Update the category, inserting, updating, or deleting as indicated.
     *
     * @param cat The CatRecord to update
     * @throws SQLException
     * @throws UnauthorizedException
     */
    public void update(CatRecord cat) throws SQLException, UnauthorizedException {
        if(mUserId == 0) {
            throw new UnauthorizedException("Unknown user");
        }

        try {
            mConnection.setAutoCommit(false);

            final String sql = "SELECT id FROM categories WHERE uuid = ? AND user = ?";
            final PreparedStatement stmt = mConnection.prepareStatement(sql);
            stmt.setString(1, cat.getUuid());
            stmt.setLong(2, mUserId);

            final ResultSet result = stmt.executeQuery();
            if(result.next()) {
                cat.setId(result.getLong(1));
            }

            if(cat.getId() == 0) {
                insertCat(cat);
            } else if(cat.isDeleted()) {
                deleteCat(cat);
            } else {
                updateCat(cat);
            }

            mConnection.commit();
        } finally {
            mConnection.setAutoCommit(true);
        }
    }

    /**
     * Insert a new category.
     *
     * @param cat The CatRecord to insert
     * @throws SQLException
     */
    private void insertCat(CatRecord cat) throws SQLException {
        final String sql = "INSERT INTO categories (uuid, user, name, updated) VALUES (?, ?, ?, ?)";
        final PreparedStatement stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, cat.getUuid());
        stmt.setLong(2, mUserId);
        stmt.setString(3, cat.getName());
        stmt.setLong(4, System.currentTimeMillis());
        stmt.executeUpdate();

        final ResultSet result = stmt.getGeneratedKeys();
        if(result.next()) {
            cat.setId(result.getLong(1));
            insertCatExtras(cat);
            insertCatFlavors(cat);
        }
    }

    /**
     * Update a category.
     *
     * @param cat The CatRecord to update
     * @throws SQLException
     */
    private void updateCat(CatRecord cat) throws SQLException {
        final String sql = "UPDATE categories SET name = ?, updated = ? WHERE user = ? AND id = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setString(1, cat.getName());
        stmt.setLong(2, System.currentTimeMillis());
        stmt.setLong(3, mUserId);
        stmt.setLong(4, cat.getId());
        if(stmt.executeUpdate() > 0) {
            updateCatExtras(cat);
            updateCatFlavors(cat);
        }
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
        final String sql = "REPLACE INTO extras (uuid, cat, name, pos, deleted) VALUES (?, ?, ?, ?, ?)";
        final PreparedStatement stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(2, cat.getId());
        for(ExtraRecord extra : cat.getExtras()) {
            stmt.setString(1, extra.getUuid());
            stmt.setString(3, extra.getName());
            stmt.setInt(4, extra.getPos());
            stmt.setBoolean(5, extra.isDeleted());
            stmt.executeUpdate();

            if(extra.getId() == 0) {
                ResultSet result = stmt.getGeneratedKeys();
                if(result.next()) {
                    extra.setId(result.getLong(1));
                }
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
            if(extra.getId() != 0) {
                extraUuids.add(extra.getUuid());
            }
        }

        String sql = "SELECT id, uuid FROM extras WHERE cat = ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, cat.getId());

        final ResultSet result = stmt.executeQuery();
        sql = "DELETE FROM extras WHERE id = ?";
        stmt = mConnection.prepareStatement(sql);
        while(result.next()) {
            if(!extraUuids.contains(result.getString(2))) {
                stmt.setLong(1, result.getLong(1));
                stmt.executeUpdate();
            }
        }

        insertCatExtras(cat);
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
        stmt.setLong(1, cat.getId());
        for(FlavorRecord flavor : cat.getFlavors()) {
            stmt.setString(2, flavor.getName());
            stmt.setInt(3, flavor.getPos());
            stmt.executeUpdate();
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
        stmt.setLong(1, cat.getId());
        stmt.executeUpdate();

        insertCatFlavors(cat);
    }

    /**
     * Delete a category. This will delete the category and add its UUID to the deleted log.
     *
     * @param cat The category to delete
     * @throws SQLException
     */
    private void deleteCat(CatRecord cat) throws SQLException {
        String sql = "DELETE FROM categories WHERE user = ? AND id = ?";
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, mUserId);
        stmt.setLong(2, cat.getId());
        if(stmt.executeUpdate() > 0) {
            sql = "INSERT INTO deleted (user, type, uuid, time) VALUES (?, 'cat', ?, ?)";
            stmt = mConnection.prepareStatement(sql);
            stmt.setLong(1, mUserId);
            stmt.setString(2, cat.getUuid());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    /**
     * Get the time of the specified client's last data sync.
     *
     * @param clientId The database ID of the client
     * @return The Unix timestamp with milliseconds
     * @throws SQLException
     */
    public long getLastSync(long clientId) throws SQLException {
        final String sql = "SELECT last_sync FROM clients WHERE id = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, clientId);

        final ResultSet result = stmt.executeQuery();
        if(result.next()) {
            return result.getLong(1);
        }

        return 0;
    }

    /**
     * Set the time of the specified client's last data sync.
     *
     * @param clientId  The database ID of the client
     * @param timestamp The Unix timestamp with milliseconds
     * @throws SQLException
     */
    public void setLastSync(long clientId, long timestamp) throws SQLException {
        final String sql = "UPDATE clients SET last_sync = ? WHERE id = ?";
        final PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setLong(1, timestamp);
        stmt.setLong(2, clientId);
        stmt.executeUpdate();
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
        PreparedStatement stmt = mConnection.prepareStatement(sql);
        stmt.setString(1, userEmail);

        ResultSet result = stmt.executeQuery();
        if(result.next()) {
            return result.getLong(1);
        } else {
            sql = "INSERT INTO users (email) VALUES (?)";
            stmt = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, userEmail);
            stmt.executeUpdate();

            result = stmt.getGeneratedKeys();
            if(result.next()) {
                return result.getLong(1);
            }
        }

        return 0;
    }
}
