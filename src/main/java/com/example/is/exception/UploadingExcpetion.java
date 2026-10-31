package com.example.is.exception;

public class UploadingExcpetion extends RuntimeException {
    public UploadingExcpetion(String message) {
        super(message);
    }

    public UploadingExcpetion(String message, Throwable cause) {
        super(message, cause);
    }
}
