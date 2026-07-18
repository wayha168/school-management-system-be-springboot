package com.project.school_management.exception;

public class ErrorRuntime extends RuntimeException {

    public ErrorRuntime(String message) {
        super(message);
    }

    public ErrorRuntime(String message, Throwable cause) {
        super(message, cause);
    }
}
