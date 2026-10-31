package com.example.is.exception;

public class FileTooBigException extends RuntimeException {
    public FileTooBigException() {
        super("TOO_BIG");
    }
}
