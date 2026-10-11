package com.edgeaiplatform.backend.service;

public class ModelInspectionException extends Exception {
    private final String code;

    public ModelInspectionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
