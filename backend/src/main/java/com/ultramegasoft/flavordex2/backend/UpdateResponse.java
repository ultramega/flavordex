package com.ultramegasoft.flavordex2.backend;

/**
 * Model for a response to a push request.
 *
 * @author Steve Guidetti
 */
public class UpdateResponse {
    private boolean success;
    private long id;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
