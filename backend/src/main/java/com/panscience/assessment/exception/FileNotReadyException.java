package com.panscience.assessment.exception;

import com.panscience.assessment.entity.ProcessingStatus;

public class FileNotReadyException extends RuntimeException {

    public FileNotReadyException(Long fileId, ProcessingStatus status) {
        super("File " + fileId + " is not ready for Q&A. Current status: " + status);
    }
}
