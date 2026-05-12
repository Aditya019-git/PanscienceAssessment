package com.panscience.assessment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panscience.assessment.dto.ContentChunkResponse;
import com.panscience.assessment.dto.FileMetadataResponse;
import com.panscience.assessment.dto.FileProcessingResponse;
import com.panscience.assessment.dto.FileSummaryResponse;
import com.panscience.assessment.dto.QuestionAnswerResponse;
import com.panscience.assessment.dto.AnswerSourceResponse;
import com.panscience.assessment.dto.TranscriptSegmentResponse;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.exception.GlobalExceptionHandler;
import com.panscience.assessment.exception.StoredFileNotFoundException;
import com.panscience.assessment.service.FileProcessingService;
import com.panscience.assessment.service.FileUploadService;
import com.panscience.assessment.service.QuestionAnswerService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
@Import(GlobalExceptionHandler.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploadService fileUploadService;

    @MockBean
    private FileProcessingService fileProcessingService;

    @MockBean
    private QuestionAnswerService questionAnswerService;

    @Test
    void uploadsFileAndReturnsCreatedResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "spec.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "pdf-content".getBytes()
        );

        when(fileUploadService.upload(any())).thenReturn(sampleResponse());

        mockMvc.perform(multipart("/api/files/upload").file(file))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/files/1"))
            .andExpect(jsonPath("$.originalName").value("spec.pdf"))
            .andExpect(jsonPath("$.fileCategory").value("PDF"))
            .andExpect(jsonPath("$.processingStatus").value("UPLOADED"));
    }

    @Test
    void listsFiles() throws Exception {
        when(fileUploadService.listFiles()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/files"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].storedName").value("stored-spec.pdf"));
    }

    @Test
    void returnsNotFoundForUnknownFileId() throws Exception {
        when(fileUploadService.getFile(99L)).thenThrow(new StoredFileNotFoundException(99L));

        mockMvc.perform(get("/api/files/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }

    @Test
    void processesFile() throws Exception {
        when(fileProcessingService.processFile(1L)).thenReturn(
            new FileProcessingResponse(1L, ProcessingStatus.READY, 2, 0, "Extracted 2 content chunks from the PDF")
        );

        mockMvc.perform(post("/api/files/1/process"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processingStatus").value("READY"))
            .andExpect(jsonPath("$.chunkCount").value(2));
    }

    @Test
    void listsFileChunks() throws Exception {
        when(fileProcessingService.listChunks(1L)).thenReturn(
            List.of(new ContentChunkResponse(10L, "Page one text", 1, null, null))
        );

        mockMvc.perform(get("/api/files/1/chunks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].pageNumber").value(1))
            .andExpect(jsonPath("$[0].chunkText").value("Page one text"));
    }

    @Test
    void listsTranscriptSegments() throws Exception {
        when(fileProcessingService.listTranscriptSegments(2L)).thenReturn(
            List.of(new TranscriptSegmentResponse(20L, "Hello there", 0.0, 1.4, 1))
        );

        mockMvc.perform(get("/api/files/2/segments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sequenceNumber").value(1))
            .andExpect(jsonPath("$[0].segmentText").value("Hello there"));
    }

    @Test
    void returnsStoredSummary() throws Exception {
        when(fileProcessingService.getSummary(1L)).thenReturn(
            new FileSummaryResponse(1L, ProcessingStatus.READY, "This PDF explains the upload and retrieval flow.")
        );

        mockMvc.perform(get("/api/files/1/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processingStatus").value("READY"))
            .andExpect(jsonPath("$.summary").value("This PDF explains the upload and retrieval flow."));
    }

    @Test
    void answersQuestionForProcessedFile() throws Exception {
        when(questionAnswerService.answerQuestion(1L, "What is this file about?")).thenReturn(
            new QuestionAnswerResponse(
                1L,
                "What is this file about?",
                "This file explains the upload flow [1].",
                null,
                List.of(new AnswerSourceResponse(1, 10L, "Upload flow overview", 1, null, null, 0.913))
            )
        );

        mockMvc.perform(
                post("/api/files/1/questions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"What is this file about?\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").value("This file explains the upload flow [1]."))
            .andExpect(jsonPath("$.sources[0].sourceNumber").value(1))
            .andExpect(jsonPath("$.sources[0].pageNumber").value(1));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mockMvc.perform(
                post("/api/files/1/questions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"   \"}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private FileMetadataResponse sampleResponse() {
        return new FileMetadataResponse(
            1L,
            "spec.pdf",
            "stored-spec.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            FileCategory.PDF,
            ProcessingStatus.UPLOADED,
            "pdf/stored-spec.pdf",
            null,
            null,
            Instant.parse("2026-05-11T12:00:00Z")
        );
    }
}
