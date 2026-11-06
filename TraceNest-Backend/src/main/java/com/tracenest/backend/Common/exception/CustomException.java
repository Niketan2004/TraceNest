package com.tracenest.backend.Common.exception;

public class CustomException extends RuntimeException {
    private final int status;
    private final String error;
    private final String code;

    public CustomException(int status, String error, String code, String message) {
        super(message);
        this.status = status;
        this.error = error;
        this.code = code;
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getCode() { return code; }
}
