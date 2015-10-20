package com.ultramegasoft.flavordex2.backend;

import com.google.api.server.spi.response.UnauthorizedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
