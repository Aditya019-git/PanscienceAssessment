package com.panscience.assessment.exception;

import com.panscience.assessment.dto.ApiErrorResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(StoredFileNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(FileNotReadyException.class)
    public ResponseEntity<ApiErrorResponse> handleFileNotReady(FileNotReadyException exception) {
        return build(HttpStatus.CONFLICT, "FILE_NOT_READY", exception.getMessage());
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedType(UnsupportedFileTypeException exception) {
        return build(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage() == null ? "Validation failed" : error.getDefaultMessage())
            .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException exception) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUpload(MaxUploadSizeExceededException exception) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Uploaded file exceeds the configured size limit");
    }

    @ExceptionHandler(StorageOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleStorage(StorageOperationException exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", exception.getMessage());
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleProcessing(FileProcessingException exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "PROCESSING_ERROR", exception.getMessage());
    }

    @ExceptionHandler(TranscriptionUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleTranscriptionUnavailable(TranscriptionUnavailableException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "TRANSCRIPTION_UNAVAILABLE", exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
            .body(new ApiErrorResponse(code, message, Instant.now()));
    }
}
