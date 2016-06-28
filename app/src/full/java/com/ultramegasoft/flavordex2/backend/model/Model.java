package com.ultramegasoft.flavordex2.backend.model;

import com.google.gson.Gson;

/**
 * Base for all backend models.
 *
 * @author Steve Guidetti
 */
public class Model {
    /**
     * Get the model data as a JSON string.
     *
     * @return The model data as a JSON string
     */
    public String toJson() {
        return new Gson().toJson(this);
    }
}
