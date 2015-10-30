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
import java.util.HashMap;
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
     * @param since    The timestamp of the previous sync
     * @return The updated data from the server
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "fetchUpdates", httpMethod = ApiMethod.HttpMethod.GET)
    public UpdateRecord fetch(User user, @Named("clientId") long clientId,
                              @Named("since") long since)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final UpdateRecord updateRecord = new UpdateRecord();
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());
            helper.setClientId(clientId);

            updateRecord.setCats(helper.getUpdatedCats(since));
            updateRecord.setEntries(helper.getUpdatedEntries(since));
        } catch(SQLException e) {
            e.printStackTrace();
            throw new InternalServerErrorException(e);
        } finally {
            helper.close();
        }

        return updateRecord;
    }

    /**
     * Send updated journal data to the server.
     *
     * @param user     The User
     * @param record   The updated journal data
     * @param clientId The database ID of the client
     * @return The response containing the results of the update operations
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "pushUpdates")
    public UpdateResponse push(User user, UpdateRecord record, @Named("clientId") long clientId)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final UpdateResponse response = new UpdateResponse();
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());
            helper.setClientId(clientId);

            boolean dataChanged = false;
            boolean status;
            if(record.getCats() != null) {
                final HashMap<String, Boolean> catStatuses = new HashMap<>();
                for(CatRecord catRecord : record.getCats()) {
                    try {
                        status = helper.update(catRecord);
                        if(status) {
                            dataChanged = true;
                        }
                        catStatuses.put(catRecord.getUuid(), status);
                    } catch(SQLException e) {
                        e.printStackTrace();
                        catStatuses.put(catRecord.getUuid(), false);
                    }
                }
                response.setCatStatuses(catStatuses);
            }

            if(record.getEntries() != null) {
                final HashMap<String, Boolean> entryStatuses = new HashMap<>();
                for(EntryRecord entryRecord : record.getEntries()) {
                    try {
                        status = helper.update(entryRecord);
                        if(status) {
                            dataChanged = true;
                        }
                        entryStatuses.put(entryRecord.getUuid(), status);
                    } catch(SQLException e) {
                        e.printStackTrace();
                        entryStatuses.put(entryRecord.getUuid(), false);
                    }
                }
                response.setEntryStatuses(entryStatuses);
            }

            if(dataChanged) {
                notifyClients(helper);
            }
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
     * @param helper The DatabaseHelper
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    private void notifyClients(DatabaseHelper helper)
            throws InternalServerErrorException, UnauthorizedException {
        try {
            final Sender sender = new Sender(GCM_API_KEY);
            final Message message = new Message.Builder().collapseKey("requestKey").build();
            Result result;
            String canonicalRegId;
            String error;
            for(Map.Entry<Long, String> entry : helper.listGcmIds().entrySet()) {
                if(entry.getKey() == helper.getClientId()) {
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
        }
    }
}
