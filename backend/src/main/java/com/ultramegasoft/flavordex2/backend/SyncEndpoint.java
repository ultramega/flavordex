package com.ultramegasoft.flavordex2.backend;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.Constant;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.inject.Named;

/**
 * The sync endpoint for synchronizing journal data between the client and the server.
 *
 * @author Steve Guidetti
 */
@Api(
        name = "sync",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "backend.flavordex2.ultramegasoft.com",
                ownerName = "backend.flavordex2.ultramegasoft.com",
                packagePath = ""
        ),
        scopes = {
                BackendConstants.EMAIL_SCOPE
        },
        clientIds = {
                BackendConstants.WEB_CLIENT_ID,
                BackendConstants.ANDROID_CLIENT_ID,
                BackendConstants.ANDROID_CLIENT_ID_DEBUG,
                Constant.API_EXPLORER_CLIENT_ID
        },
        audiences = {
                BackendConstants.ANDROID_AUDIENCE
        }
)
public class SyncEndpoint {
    /**
     * The API key for Google Cloud Messaging
     */
    private static final String GCM_API_KEY = System.getProperty("gcm.api.key");

    /**
     * Get updated journal data from the server.
     *
     * @param user     The User
     * @param clientId The database ID of the client
     * @return The updated data from the server
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "fetchUpdates", httpMethod = ApiMethod.HttpMethod.GET)
    public UpdateRecord fetch(User user, @Named("clientId") long clientId)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final UpdateRecord updateRecord = new UpdateRecord();
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());

            final long since = helper.getLastSync(clientId);
            updateRecord.setCats(helper.getUpdatedCats(since));
            updateRecord.setEntries(helper.getUpdatedEntries(since));
            updateRecord.setTimestamp(System.currentTimeMillis());
        } catch(SQLException e) {
            e.printStackTrace();
            throw new InternalServerErrorException(e);
        } finally {
            helper.close();
        }

        return updateRecord;
    }

    /**
     * Confirm that the fetch was successful.
     *
     * @param user      The user
     * @param clientId  The database ID of the client
     * @param timestamp The Unix timestamp of the fetch response with milliseconds
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "confirmFetch")
    public void confirm(User user, @Named("clientId") long clientId,
                        @Named("timestamp") long timestamp)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());
            helper.setLastSync(clientId, timestamp);
        } catch(SQLException e) {
            e.printStackTrace();
            throw new InternalServerErrorException(e);
        } finally {
            helper.close();
        }
    }

    /**
     * Send an updated journal entry to the server.
     *
     * @param user     The User
     * @param record   The updated journal entry from the client
     * @param clientId The database ID of the client
     * @return The update response containing the result of the request
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "pushCategory")
    public UpdateResponse pushCat(User user, CatRecord record, @Named("clientId") long clientId)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final UpdateResponse response = new UpdateResponse();
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());
            helper.update(record);
            helper.setLastSync(clientId, System.currentTimeMillis());

            response.setSuccess(true);
        } catch(SQLException e) {
            e.printStackTrace();
            throw new InternalServerErrorException(e);
        } finally {
            helper.close();
        }

        return response;
    }

    /**
     * Send an updated journal entry to the server.
     *
     * @param user     The User
     * @param record   The updated journal entry from the client
     * @param clientId The database ID of the client
     * @return The update response containing the result of the request
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "pushEntry")
    public UpdateResponse pushEntry(User user, EntryRecord record, @Named("clientId") long clientId)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final UpdateResponse response = new UpdateResponse();
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());
            helper.update(record);
            helper.setLastSync(clientId, System.currentTimeMillis());

            response.setSuccess(true);
        } catch(SQLException e) {
            e.printStackTrace();
            throw new InternalServerErrorException(e);
        } finally {
            helper.close();
        }

        return response;
    }

    /**
     * Notify all clients belonging to the user the a sync is requested.
     *
     * @param user     The User
     * @param clientId The database ID of the client sending the request
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "notifyClients")
    public void notifyClients(User user, @Named("clientId") long clientId)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());

            final Sender sender = new Sender(GCM_API_KEY);
            final Message message = new Message.Builder().collapseKey("requestKey").build();
            Result result;
            String canonicalRegId;
            String error;
            for(Map.Entry<Long, String> entry : helper.listGcmIds().entrySet()) {
                if(entry.getKey() == clientId) {
                    continue;
                }
                result = sender.send(message, entry.getValue(), 5);
                if(result.getMessageId() != null) {
                    canonicalRegId = result.getCanonicalRegistrationId();
                    if(canonicalRegId != null) {
                        helper.setGcmId(entry.getKey(), canonicalRegId);
                    }
                } else {
                    error = result.getErrorCodeName();
                    if(error.equals(Constants.ERROR_NOT_REGISTERED)) {
                        helper.unregisterClient(entry.getKey());
                    }
                }
            }
        } catch(IOException | SQLException e) {
            e.printStackTrace();
            throw new InternalServerErrorException(e);
        } finally {
            helper.close();
        }
    }
}
