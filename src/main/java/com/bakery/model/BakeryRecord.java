package com.bakery.model;

import java.util.List;

public abstract class BakeryRecord {
    private String id;

    protected BakeryRecord() {

    }

    protected BakeryRecord(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public abstract String getDisplayName();

    public abstract List<String> toFileFields();

    public abstract void applyFileFields(List<String> fields);
}
