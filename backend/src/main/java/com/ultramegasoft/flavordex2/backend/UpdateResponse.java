package com.ultramegasoft.flavordex2.backend;

/**
 * Model for a response to a push request.
 *
 * @author Steve Guidetti
 */
public class UpdateResponse {
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
