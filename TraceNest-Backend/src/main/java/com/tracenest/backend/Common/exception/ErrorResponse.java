package com.tracenest.backend.Common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private Map<String, String> fieldErrors;
    private String path;
    private String traceId;

    public ErrorResponse() {
    }

    public ErrorResponse(OffsetDateTime timestamp, int status, String error, String code, String message, Map<String, String> fieldErrors, String path, String traceId) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.path = path;
        this.traceId = traceId;
    }
}
