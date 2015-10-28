package com.ultramegasoft.flavordex2.backend;

/**
 * Model for a photo record.
 *
 * @author Steve Guidetti
 */
public class PhotoRecord {
    private long id;
    private long entry;
    private String driveId;
    private int pos;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEntry() {
        return entry;
    }

    public void setEntry(long entry) {
        this.entry = entry;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }
}
