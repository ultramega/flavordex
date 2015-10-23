package com.ultramegasoft.flavordex2.backend;

/**
 * Model for a response to a category push request.
 *
 * @author Steve Guidetti
 */
public class CatUpdateResponse extends UpdateResponse {
    private long[] extraIds;

    public long[] getExtraIds() {
        return extraIds;
    }

    public void setExtraIds(long[] extraIds) {
        this.extraIds = extraIds;
    }
}
