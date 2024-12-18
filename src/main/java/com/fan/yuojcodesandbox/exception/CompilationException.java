package com.fan.yuojcodesandbox.exception;

public class CompilationException extends RuntimeException {
    private String errorCode;

    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
