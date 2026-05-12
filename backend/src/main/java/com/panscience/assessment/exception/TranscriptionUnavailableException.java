package com.panscience.assessment.exception;

public class TranscriptionUnavailableException extends RuntimeException {

    public TranscriptionUnavailableException(String message) {
        super(message);
    }

    public TranscriptionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
