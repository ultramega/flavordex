package com.ultramegasoft.flavordex2.backend;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ultramegasoft.flavordex2.backend.model.CatRecord;
import com.ultramegasoft.flavordex2.backend.model.EntryRecord;
import com.ultramegasoft.flavordex2.backend.model.SyncRecord;
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
     * Start a synchronization session.
     *
     * @throws ApiException
     */
    public void startSync() throws ApiException {
        post("startSync", null, BackendUtils.getClientId(getContext()));
    }

    /**
     * End the synchronization session.
     *
     * @throws ApiException
     */
    public void endSync() throws ApiException {
        post("endSync", null, BackendUtils.getClientId(getContext()));
    }

    /**
     * Get a list of deleted and updated categories and entries.
     *
     * @return The SyncRecord
     * @throws ApiException
     */
    public SyncRecord getUpdates() throws ApiException {
        final String response = get("getUpdates", BackendUtils.getClientId(getContext()));
        try {
            return new Gson().fromJson(response, SyncRecord.class);
        } catch(JsonSyntaxException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Get a single category.
     *
     * @param catUuid The UUID of the category
     * @return The CatRecord
     * @throws ApiException
     */
    public CatRecord getCat(String catUuid) throws ApiException {
        final String response = get("getCat", BackendUtils.getClientId(getContext()), catUuid);
        try {
            return new Gson().fromJson(response, CatRecord.class);
        } catch(JsonSyntaxException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Send a single category.
     *
     * @param catRecord The CatRecord
     * @return The UpdateResponse
     * @throws ApiException
     */
    public UpdateResponse putCat(CatRecord catRecord) throws ApiException {
        final String response = post("putCat", catRecord, BackendUtils.getClientId(getContext()));
        try {
            return new Gson().fromJson(response, UpdateResponse.class);
        } catch(JsonSyntaxException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Get a single entry.
     *
     * @param entryUuid The UUID of the entry
     * @return The EntryRecord
     * @throws ApiException
     */
    public EntryRecord getEntry(String entryUuid) throws ApiException {
        final String response = get("getEntry", BackendUtils.getClientId(getContext()), entryUuid);
        try {
            return new Gson().fromJson(response, EntryRecord.class);
        } catch(JsonSyntaxException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Send a single entry.
     *
     * @param entryRecord The EntryRecord
     * @return The UpdateResponse
     * @throws ApiException
     */
    public UpdateResponse putEntry(EntryRecord entryRecord) throws ApiException {
        final String response =
                post("putEntry", entryRecord, BackendUtils.getClientId(getContext()));
        try {
            return new Gson().fromJson(response, UpdateResponse.class);
        } catch(JsonSyntaxException e) {
            throw new ParseException(e);
        }
    }
}
