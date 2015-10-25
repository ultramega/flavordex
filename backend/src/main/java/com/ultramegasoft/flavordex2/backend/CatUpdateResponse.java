package com.ultramegasoft.flavordex2.backend;

import java.util.ArrayList;

/**
 * Model for a response to a category push request.
 *
 * @author Steve Guidetti
 */
public class CatUpdateResponse extends UpdateResponse {
    private ArrayList<Long> extraIds;

    public ArrayList<Long> getExtraIds() {
        return extraIds;
    }

    public void setExtraIds(ArrayList<Long> extraIds) {
        this.extraIds = extraIds;
    }
}
