package com.panscience.assessment.controller;

import com.panscience.assessment.dto.ContentChunkResponse;
import com.panscience.assessment.dto.FileMetadataResponse;
import com.panscience.assessment.dto.FileProcessingResponse;
import com.panscience.assessment.dto.FileSummaryResponse;
import com.panscience.assessment.dto.QuestionAnswerRequest;
import com.panscience.assessment.dto.QuestionAnswerResponse;
import com.panscience.assessment.dto.TranscriptSegmentResponse;
import com.panscience.assessment.service.FileProcessingService;
import com.panscience.assessment.service.FileUploadService;
import com.panscience.assessment.service.QuestionAnswerService;
import com.panscience.assessment.service.StoredFileResource;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileProcessingService fileProcessingService;
    private final QuestionAnswerService questionAnswerService;

    public FileController(
        FileUploadService fileUploadService,
        FileProcessingService fileProcessingService,
        QuestionAnswerService questionAnswerService
    ) {
        this.fileUploadService = fileUploadService;
        this.fileProcessingService = fileProcessingService;
        this.questionAnswerService = questionAnswerService;
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

    @GetMapping(value = "/{id}/content")
    public ResponseEntity<Resource> getFileContent(@PathVariable Long id) {
        StoredFileResource fileResource = fileUploadService.loadFileContent(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fileResource.contentType()))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + fileResource.originalName().replace("\"", "") + "\""
            )
            .body(new PathResource(fileResource.path()));
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

    @GetMapping(value = "/{id}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public FileSummaryResponse getSummary(@PathVariable Long id) {
        return fileProcessingService.getSummary(id);
    }

    @PostMapping(
        value = "/{id}/questions",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public QuestionAnswerResponse answerQuestion(
        @PathVariable Long id,
        @Valid @RequestBody QuestionAnswerRequest request
    ) {
        return questionAnswerService.answerQuestion(id, request.question());
    }

    @GetMapping(value = "/{id}/questions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> answerQuestionStream(
        @PathVariable Long id,
        @RequestParam String question
    ) {
        return questionAnswerService.streamAnswer(id, question);
    }
}
