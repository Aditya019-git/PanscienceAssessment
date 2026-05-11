package com.panscience.assessment.exception;

public class StoredFileNotFoundException extends RuntimeException {

    public StoredFileNotFoundException(Long fileId) {
        super("File metadata not found for id " + fileId);
    }
}

