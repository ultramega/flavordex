package com.ultramegasoft.flavordex2.backend;

/**
 * Model for a flavor record.
 *
 * @author Steve Guidetti
 */
public class FlavorRecord {
    private long id;
    private long cat;
    private String name;
    private int value;
    private int pos;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCat() {
        return cat;
    }

    public void setCat(long cat) {
        this.cat = cat;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }
}
