package com.panscience.assessment.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.panscience.assessment.dto.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handlesStoredFileNotFoundException() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleNotFound(new StoredFileNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("FILE_NOT_FOUND");
        assertThat(response.getBody().message()).contains("42");
    }

    @Test
    void handlesFileNotReadyException() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleFileNotReady(new FileNotReadyException(1L, com.panscience.assessment.entity.ProcessingStatus.PROCESSING));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("FILE_NOT_READY");
    }

    @Test
    void handlesUnsupportedFileTypeException() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleUnsupportedType(new UnsupportedFileTypeException("text/plain", "file.txt"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("UNSUPPORTED_FILE_TYPE");
    }

    @Test
    void handlesIllegalArgumentException() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("bad input");
    }

    @Test
    void handlesMaxUploadSizeExceededException() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleMaxUpload(new MaxUploadSizeExceededException(1024L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void handlesStorageOperationException() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleStorage(new StorageOperationException("disk error", new RuntimeException()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("STORAGE_ERROR");
    }

    @Test
    void handlesFileProcessingException() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleProcessing(new FileProcessingException("failed to extract", new RuntimeException()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("PROCESSING_ERROR");
    }

    @Test
    void handlesTranscriptionUnavailableException() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleTranscriptionUnavailable(new TranscriptionUnavailableException("no key"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code()).isEqualTo("TRANSCRIPTION_UNAVAILABLE");
    }

    @Test
    void responseBodyContainsTimestamp() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleNotFound(new StoredFileNotFoundException(1L));

        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
