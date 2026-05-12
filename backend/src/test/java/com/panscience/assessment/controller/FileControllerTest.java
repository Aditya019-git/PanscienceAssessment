package com.panscience.assessment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panscience.assessment.dto.FileMetadataResponse;
import com.panscience.assessment.dto.FileProcessingResponse;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.service.FileProcessingService;
import com.panscience.assessment.service.FileUploadService;
import com.panscience.assessment.service.QuestionAnswerService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity in unit test
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private FileUploadService fileUploadService;
    @MockitoBean private FileProcessingService fileProcessingService;
    @MockitoBean private QuestionAnswerService questionAnswerService;
    @MockitoBean private com.panscience.assessment.config.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private com.panscience.assessment.config.RateLimitingFilter rateLimitingFilter;

    @Test
    @WithMockUser
    void uploadsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        FileMetadataResponse metadata = new FileMetadataResponse(
            1L, "test.pdf", "stored.pdf", "application/pdf", 
            FileCategory.PDF, ProcessingStatus.UPLOADED, "pdf/stored.pdf", 
            null, null, java.time.Instant.now()
        );
        
        when(fileUploadService.upload(any())).thenReturn(metadata);

        mockMvc.perform(multipart("/api/files/upload").file(file))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.originalName").value("test.pdf"));
    }

    @Test
    void listsFiles() throws Exception {
        when(fileUploadService.listFiles()).thenReturn(List.of());

        mockMvc.perform(get("/api/files"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void processesFile() throws Exception {
        FileProcessingResponse response = new FileProcessingResponse(1L, ProcessingStatus.READY, 5, 0, "Success");
        when(fileProcessingService.processFile(1L)).thenReturn(response);

        mockMvc.perform(post("/api/files/1/process"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processingStatus").value("READY"));
    }
}
