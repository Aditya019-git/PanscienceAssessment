package com.panscience.assessment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panscience.assessment.dto.FileMetadataResponse;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.exception.GlobalExceptionHandler;
import com.panscience.assessment.exception.StoredFileNotFoundException;
import com.panscience.assessment.service.FileUploadService;
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
            Instant.parse("2026-05-11T12:00:00Z")
        );
    }
}
