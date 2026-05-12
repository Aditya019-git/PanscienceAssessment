package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panscience.assessment.dto.ContentChunkResponse;
import com.panscience.assessment.dto.FileProcessingResponse;
import com.panscience.assessment.dto.FileSummaryResponse;
import com.panscience.assessment.dto.TranscriptSegmentResponse;
import com.panscience.assessment.entity.ContentChunk;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.entity.StoredFile;
import com.panscience.assessment.entity.TranscriptSegment;
import com.panscience.assessment.exception.StoredFileNotFoundException;
import com.panscience.assessment.repository.ContentChunkRepository;
import com.panscience.assessment.repository.StoredFileRepository;
import com.panscience.assessment.repository.TranscriptSegmentRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @TempDir
    Path tempDir;

    @Mock private StoredFileRepository storedFileRepository;
    @Mock private ContentChunkRepository contentChunkRepository;
    @Mock private TranscriptSegmentRepository transcriptSegmentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private PdfExtractionService pdfExtractionService;
    @Mock private TranscriptionService transcriptionService;
    @Mock private FileSummaryService fileSummaryService;

    private FileProcessingService service;

    @BeforeEach
    void setUp() {
        service = new FileProcessingService(
            storedFileRepository, contentChunkRepository, transcriptSegmentRepository,
            fileStorageService, pdfExtractionService, transcriptionService, fileSummaryService
        );
    }

    // ---------- processPdf ----------

    @Test
    void processesPdfSuccessfully() {
        StoredFile pdf = storedFile(1L, FileCategory.PDF);
        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(pdf));
        when(storedFileRepository.save(any())).thenReturn(pdf);
        when(fileStorageService.resolve(any())).thenReturn(tempDir.resolve("doc.pdf"));

        List<ExtractedPdfChunk> chunks = List.of(
            new ExtractedPdfChunk(1, "Page one text"),
            new ExtractedPdfChunk(2, "Page two text")
        );
        when(pdfExtractionService.extract(any(Path.class))).thenReturn(chunks);
        when(contentChunkRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(fileSummaryService.generateSummary(any(), anyList())).thenReturn("A summary of the PDF");

        FileProcessingResponse result = service.processFile(1L);

        assertThat(result.processingStatus()).isEqualTo(ProcessingStatus.READY);
        assertThat(result.chunkCount()).isEqualTo(2);
        assertThat(result.message()).contains("2 content chunks");
        verify(contentChunkRepository).deleteAllByFileId(1L);
    }

    @Test
    void setsStatusToFailedOnPdfError() {
        StoredFile pdf = storedFile(1L, FileCategory.PDF);
        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(pdf));
        when(storedFileRepository.save(any())).thenReturn(pdf);
        when(fileStorageService.resolve(any())).thenReturn(tempDir.resolve("bad.pdf"));
        when(pdfExtractionService.extract(any())).thenThrow(new RuntimeException("PDFBox error"));

        assertThatThrownBy(() -> service.processFile(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("PDFBox error");

        assertThat(pdf.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(pdf.getProcessingError()).contains("PDFBox error");
    }

    // ---------- processMedia ----------

    @Test
    void processesVideoSuccessfully() {
        StoredFile video = storedFile(2L, FileCategory.VIDEO);
        when(storedFileRepository.findById(2L)).thenReturn(Optional.of(video));
        when(storedFileRepository.save(any())).thenReturn(video);
        when(fileStorageService.resolve(any())).thenReturn(tempDir.resolve("clip.mp4"));

        TranscribedSegment seg = new TranscribedSegment("Hello there", 0.0, 1.5, 1);
        TranscriptionResult transcription = new TranscriptionResult("Hello there", List.of(seg));
        when(transcriptionService.transcribe(any(Path.class))).thenReturn(transcription);
        when(transcriptSegmentRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(contentChunkRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(fileSummaryService.generateSummary(any(), anyList())).thenReturn("Media summary");

        FileProcessingResponse result = service.processFile(2L);

        assertThat(result.processingStatus()).isEqualTo(ProcessingStatus.READY);
        assertThat(result.transcriptSegmentCount()).isEqualTo(1);
        assertThat(result.message()).contains("1 transcript segments");
        verify(transcriptSegmentRepository).deleteAllByFileId(2L);
    }

    @Test
    void processesAudioWithFallbackSegmentWhenNoSegments() {
        StoredFile audio = storedFile(3L, FileCategory.AUDIO);
        when(storedFileRepository.findById(3L)).thenReturn(Optional.of(audio));
        when(storedFileRepository.save(any())).thenReturn(audio);
        when(fileStorageService.resolve(any())).thenReturn(tempDir.resolve("sound.mp3"));

        // No segments but full text
        TranscriptionResult transcription = new TranscriptionResult("Full transcript text", List.of());
        when(transcriptionService.transcribe(any(Path.class))).thenReturn(transcription);
        when(transcriptSegmentRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(contentChunkRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(fileSummaryService.generateSummary(any(), anyList())).thenReturn("Summary");

        FileProcessingResponse result = service.processFile(3L);

        // Fallback segment created
        assertThat(result.transcriptSegmentCount()).isEqualTo(1);
    }

    // ---------- listChunks ----------

    @Test
    void listsChunksForExistingFile() {
        when(storedFileRepository.existsById(1L)).thenReturn(true);
        ContentChunk chunk = new ContentChunk();
        ReflectionTestUtils.setField(chunk, "id", 10L);
        chunk.setFileId(1L);
        chunk.setChunkText("Sample text");
        chunk.setPageNumber(1);
        when(contentChunkRepository.findAllByFileIdOrderByPageNumberAscIdAsc(1L))
            .thenReturn(List.of(chunk));

        List<ContentChunkResponse> result = service.listChunks(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).chunkText()).isEqualTo("Sample text");
    }

    @Test
    void throwsNotFoundWhenListingChunksForMissingFile() {
        when(storedFileRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listChunks(99L))
            .isInstanceOf(StoredFileNotFoundException.class);
    }

    // ---------- listTranscriptSegments ----------

    @Test
    void listsTranscriptSegmentsForExistingFile() {
        when(storedFileRepository.existsById(2L)).thenReturn(true);
        TranscriptSegment seg = new TranscriptSegment();
        ReflectionTestUtils.setField(seg, "id", 20L);
        seg.setFileId(2L);
        seg.setSegmentText("Hello");
        seg.setStartTime(0.0);
        seg.setEndTime(1.0);
        seg.setSequenceNumber(1);
        when(transcriptSegmentRepository.findAllByFileIdOrderBySequenceNumberAsc(2L))
            .thenReturn(List.of(seg));

        List<TranscriptSegmentResponse> result = service.listTranscriptSegments(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).segmentText()).isEqualTo("Hello");
    }

    // ---------- getSummary ----------

    @Test
    void returnsSummaryForReadyFile() {
        StoredFile f = storedFile(1L, FileCategory.PDF);
        f.setProcessingStatus(ProcessingStatus.READY);
        f.setSummary("This is a summary.");
        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(f));

        FileSummaryResponse response = service.getSummary(1L);

        assertThat(response.summary()).isEqualTo("This is a summary.");
        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.READY);
    }

    @Test
    void throwsNotFoundWhenGettingSummaryForMissingFile() {
        when(storedFileRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(88L))
            .isInstanceOf(StoredFileNotFoundException.class);
    }

    // ---------- throwsNotFound for processFile ----------

    @Test
    void throwsNotFoundWhenProcessingMissingFile() {
        when(storedFileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processFile(999L))
            .isInstanceOf(StoredFileNotFoundException.class);
    }

    // ---------- helper ----------

    private StoredFile storedFile(Long id, FileCategory category) {
        StoredFile f = new StoredFile();
        ReflectionTestUtils.setField(f, "id", id);
        f.setOriginalName("file-" + id);
        f.setStoredName("stored-file-" + id);
        f.setContentType("application/octet-stream");
        f.setFileCategory(category);
        f.setProcessingStatus(ProcessingStatus.UPLOADED);
        f.setStoragePath(category.name().toLowerCase() + "/stored-file-" + id);
        return f;
    }
}
