package com.ultramegasoft.flavordex2.backend.model;

import java.util.HashMap;

/**
 * Model for a response to a push request.
 *
 * @author Steve Guidetti
 */
public class UpdateResponse extends Model {
    public HashMap<String, Boolean> catStatuses;
    public HashMap<String, Boolean> entryStatuses;
    public HashMap<String, Long> entryIds;
}
