package com.ultramegasoft.flavordex2.backend;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ultramegasoft.flavordex2.backend.model.RemoteIdsRecord;
import com.ultramegasoft.flavordex2.backend.model.UpdateRecord;
import com.ultramegasoft.flavordex2.backend.model.UpdateResponse;

/**
 * Represents the Sync endpoint.
 *
 * @author Steve Guidetti
 */
public class Sync extends Endpoint {
    public Sync(Context context) {
        super(context);
    }

    @Override
    protected String getName() {
        return "sync";
    }

    /**
     * Get updated journal data from the backend.
     *
     * @return UpdateRecord containing remote change
     * @throws ApiException
     */
    public UpdateRecord fetchUpdates() throws ApiException {
        final long id = BackendUtils.getClientId(getContext());
        try {
            return new Gson().fromJson(get("fetchUpdates", id), UpdateRecord.class);
        } catch(JsonSyntaxException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Send updated journal data to the backend.
     *
     * @param record UpdateRecord containing local changes
     * @return The UpdateResponse
     * @throws ApiException
     */
    public UpdateResponse pushUpdates(UpdateRecord record) throws ApiException {
        final long id = BackendUtils.getClientId(getContext());
        try {
            return new Gson().fromJson(post("pushUpdates", record, id), UpdateResponse.class);
        } catch(JsonSyntaxException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Confirm a successful sync with the backend.
     *
     * @param time The timestamp reported in the UpdateResponse
     * @throws ApiException
     */
    public void confirmSync(long time) throws ApiException {
        final long id = BackendUtils.getClientId(getContext());
        post("confirmSync", time, id);
    }

    /**
     * Get a list mapping UUIDs to remote IDs of all entries.
     *
     * @return The RemoteIdsRecord
     * @throws ApiException
     */
    public RemoteIdsRecord getRemoteIds() throws ApiException {
        try {
            return new Gson().fromJson(get("getRemoteIds"), RemoteIdsRecord.class);
        } catch(JsonSyntaxException e) {
            throw new ParseException(e);
        }
    }
}
