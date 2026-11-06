package com.tracenest.backend.Report.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportType {
    PERSON("person"),
    PET("pet"),
    LOST_VEHICLE("lost-vehicle"),
    STOLEN_VEHICLE("stolen-vehicle");

    private final String value;

    ReportType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
