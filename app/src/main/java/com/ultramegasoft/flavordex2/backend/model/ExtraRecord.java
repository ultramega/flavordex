package com.ultramegasoft.flavordex2.backend.model;

/**
 * Model for an extra record.
 *
 * @author Steve Guidetti
 */
public class ExtraRecord extends Model {
    public long id;
    public String uuid;
    public long cat;
    public String name;
    public String value;
    public int pos;
    public boolean deleted;
}
