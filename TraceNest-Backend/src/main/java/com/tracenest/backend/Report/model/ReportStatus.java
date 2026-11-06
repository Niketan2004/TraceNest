package com.tracenest.backend.Report.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportStatus {
    MISSING("missing"),
    FOUND("found"),
    RESOLVED("resolved");

    private final String value;

    ReportStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
