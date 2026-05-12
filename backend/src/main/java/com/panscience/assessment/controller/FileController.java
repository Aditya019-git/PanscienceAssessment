package com.panscience.assessment.controller;

import com.panscience.assessment.dto.ContentChunkResponse;
import com.panscience.assessment.dto.FileMetadataResponse;
import com.panscience.assessment.dto.FileProcessingResponse;
import com.panscience.assessment.dto.TranscriptSegmentResponse;
import com.panscience.assessment.service.FileProcessingService;
import com.panscience.assessment.service.FileUploadService;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileProcessingService fileProcessingService;

    public FileController(
        FileUploadService fileUploadService,
        FileProcessingService fileProcessingService
    ) {
        this.fileUploadService = fileUploadService;
        this.fileProcessingService = fileProcessingService;
    }

    @PostMapping(
        value = "/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FileMetadataResponse> upload(@RequestPart("file") MultipartFile file) {
        FileMetadataResponse response = fileUploadService.upload(file);
        return ResponseEntity.created(URI.create("/api/files/" + response.id())).body(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileMetadataResponse> listFiles() {
        return fileUploadService.listFiles();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FileMetadataResponse getFile(@PathVariable Long id) {
        return fileUploadService.getFile(id);
    }

    @PostMapping(value = "/{id}/process", produces = MediaType.APPLICATION_JSON_VALUE)
    public FileProcessingResponse processFile(@PathVariable Long id) {
        return fileProcessingService.processFile(id);
    }

    @GetMapping(value = "/{id}/chunks", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ContentChunkResponse> listChunks(@PathVariable Long id) {
        return fileProcessingService.listChunks(id);
    }

    @GetMapping(value = "/{id}/segments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TranscriptSegmentResponse> listTranscriptSegments(@PathVariable Long id) {
        return fileProcessingService.listTranscriptSegments(id);
    }
}

