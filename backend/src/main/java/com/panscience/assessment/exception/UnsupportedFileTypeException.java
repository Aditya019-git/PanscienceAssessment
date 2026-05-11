package com.panscience.assessment.exception;

public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String contentType, String filename) {
        super("Unsupported file type for '" + filename + "' with content type '" + contentType + "'");
    }
}

