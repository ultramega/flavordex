package com.ultramegasoft.flavordex2.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database access helpers.
 *
 * @author Steve Guidetti
 */
public class DatabaseHelper {
    /**
     * Open a connection to the database.
     *
     * @return The Connection object
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/flavordex2?user=root");
    }

    /**
     * Register a client device with the database.
     *
     * @param userEmail The user's email address
     * @param gcmId     The Google Cloud Messaging ID
     * @return The RegistrationRecord
     */
    public static RegistrationRecord registerClient(String userEmail, String gcmId) {
        final RegistrationRecord record = new RegistrationRecord();
        try {
            Connection conn = null;
            try {
                conn = getConnection();

                final long id = getUserId(conn, userEmail);

                String sql = "SELECT id, gcm_id FROM clients WHERE user = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setLong(1, id);

                ResultSet result = stmt.executeQuery();
                while(result.next()) {
                    if(gcmId.equals(result.getString(2))) {
                        record.setClientId(result.getLong(1));
                        return record;
                    }
                }

                sql = "INSERT INTO clients (user, gcm_id) VALUES (?, ?)";
                stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setLong(1, id);
                stmt.setString(2, gcmId);

                stmt.executeUpdate();
                result = stmt.getGeneratedKeys();
                if(result.next()) {
                    record.setClientId(result.getLong(1));
                }
            } finally {
                if(conn != null) {
                    conn.close();
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return record;
    }

    /**
     * Unregister a client device from the database.
     *
     * @param clientId  The database ID of the client
     * @param userEmail The user's email address
     */
    public static void unregisterClient(long clientId, String userEmail) {
        try {
            final Connection conn = getConnection();
            try {
                final long userId = getUserId(conn, userEmail);
                if(userId > 0) {
                    final String sql = "DELETE FROM clients WHERE id = ? AND user = ?";
                    final PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setLong(1, clientId);
                    stmt.setLong(2, userId);
                    stmt.executeUpdate();
                }
            } finally {
                if(conn != null) {
                    conn.close();
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the database ID of a user, creating one if it doesn't exist.
     *
     * @param conn      The database connection
     * @param userEmail The user's email address
     * @return The user's database ID
     * @throws SQLException
     */
    private static long getUserId(Connection conn, String userEmail) throws SQLException {
        String sql = "SELECT id FROM users WHERE email = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, userEmail);

        ResultSet result = stmt.executeQuery();
        if(result.next()) {
            return result.getLong(1);
        } else {
            sql = "INSERT INTO users (email) VALUES (?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
