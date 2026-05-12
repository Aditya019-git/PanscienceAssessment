package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panscience.assessment.dto.FileProcessingResponse;
import com.panscience.assessment.entity.ContentChunk;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.entity.StoredFile;
import com.panscience.assessment.repository.ContentChunkRepository;
import com.panscience.assessment.repository.StoredFileRepository;
import com.panscience.assessment.repository.TranscriptSegmentRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @Mock
    private StoredFileRepository storedFileRepository;

    @Mock
    private ContentChunkRepository contentChunkRepository;

    @Mock
    private TranscriptSegmentRepository transcriptSegmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PdfExtractionService pdfExtractionService;

    @Mock
    private TranscriptionService transcriptionService;

    @Mock
    private FileSummaryService fileSummaryService;

    private FileProcessingService fileProcessingService;

    @BeforeEach
    void setUp() {
        fileProcessingService = new FileProcessingService(
            storedFileRepository,
            contentChunkRepository,
            transcriptSegmentRepository,
            fileStorageService,
            pdfExtractionService,
            transcriptionService,
            fileSummaryService
        );
    }

    @Test
    void storesSummaryWhenProcessingPdf() {
        StoredFile storedFile = pdfFile();

        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(storedFile));
        when(fileStorageService.resolve("uploads/spec.pdf")).thenReturn(Path.of("uploads/spec.pdf"));
        when(pdfExtractionService.extract(Path.of("uploads/spec.pdf"))).thenReturn(
            List.of(new ExtractedPdfChunk(1, "First page overview"), new ExtractedPdfChunk(2, "Second page details"))
        );
        when(fileSummaryService.generateSummary(any(), any())).thenReturn("Summary of the PDF.");

        FileProcessingResponse response = fileProcessingService.processFile(1L);

        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.READY);
        assertThat(storedFile.getSummary()).isEqualTo("Summary of the PDF.");
        assertThat(storedFile.getProcessingError()).isNull();
        verify(fileSummaryService).generateSummary(any(), any());
        verify(contentChunkRepository).saveAll(any());
    }

    @Test
    void returnsStoredSummaryMetadata() {
        StoredFile storedFile = pdfFile();
        storedFile.setProcessingStatus(ProcessingStatus.READY);
        storedFile.setSummary("Stored summary text");

        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(storedFile));

        assertThat(fileProcessingService.getSummary(1L).summary()).isEqualTo("Stored summary text");
    }

    private StoredFile pdfFile() {
        StoredFile storedFile = new StoredFile();
        ReflectionTestUtils.setField(storedFile, "id", 1L);
        storedFile.setOriginalName("spec.pdf");
        storedFile.setStoredName("stored-spec.pdf");
        storedFile.setContentType("application/pdf");
        storedFile.setFileCategory(FileCategory.PDF);
        storedFile.setProcessingStatus(ProcessingStatus.UPLOADED);
        storedFile.setStoragePath("uploads/spec.pdf");
        return storedFile;
    }
}
