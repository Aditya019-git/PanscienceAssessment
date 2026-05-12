package com.panscience.assessment.service;

import com.panscience.assessment.dto.ContentChunkResponse;
import com.panscience.assessment.dto.FileMetadataResponse;
import com.panscience.assessment.dto.FileProcessingResponse;
import com.panscience.assessment.dto.TranscriptSegmentResponse;
import com.panscience.assessment.entity.ContentChunk;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.entity.StoredFile;
import com.panscience.assessment.entity.TranscriptSegment;
import com.panscience.assessment.exception.FileProcessingException;
import com.panscience.assessment.exception.StoredFileNotFoundException;
import com.panscience.assessment.repository.ContentChunkRepository;
import com.panscience.assessment.repository.StoredFileRepository;
import com.panscience.assessment.repository.TranscriptSegmentRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FileProcessingService {

    private final StoredFileRepository storedFileRepository;
    private final ContentChunkRepository contentChunkRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final FileStorageService fileStorageService;
    private final PdfExtractionService pdfExtractionService;
    private final TranscriptionService transcriptionService;

    public FileProcessingService(
        StoredFileRepository storedFileRepository,
        ContentChunkRepository contentChunkRepository,
        TranscriptSegmentRepository transcriptSegmentRepository,
        FileStorageService fileStorageService,
        PdfExtractionService pdfExtractionService,
        TranscriptionService transcriptionService
    ) {
        this.storedFileRepository = storedFileRepository;
        this.contentChunkRepository = contentChunkRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.fileStorageService = fileStorageService;
        this.pdfExtractionService = pdfExtractionService;
        this.transcriptionService = transcriptionService;
    }

    @Transactional
    public FileProcessingResponse processFile(Long fileId) {
        StoredFile storedFile = storedFileRepository.findById(fileId)
            .orElseThrow(() -> new StoredFileNotFoundException(fileId));

        storedFile.setProcessingStatus(ProcessingStatus.PROCESSING);
        storedFile.setProcessingError(null);
        storedFileRepository.save(storedFile);

        try {
            return switch (storedFile.getFileCategory()) {
                case PDF -> processPdf(storedFile);
                case AUDIO, VIDEO -> processMedia(storedFile);
            };
        } catch (RuntimeException exception) {
            storedFile.setProcessingStatus(ProcessingStatus.FAILED);
            storedFile.setProcessingError(exception.getMessage());
            storedFileRepository.save(storedFile);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ContentChunkResponse> listChunks(Long fileId) {
        ensureFileExists(fileId);
        return contentChunkRepository.findAllByFileIdOrderByPageNumberAscIdAsc(fileId)
            .stream()
            .map(ContentChunkResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TranscriptSegmentResponse> listTranscriptSegments(Long fileId) {
        ensureFileExists(fileId);
        return transcriptSegmentRepository.findAllByFileIdOrderBySequenceNumberAsc(fileId)
            .stream()
            .map(TranscriptSegmentResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public FileMetadataResponse getFileMetadata(Long fileId) {
        return storedFileRepository.findById(fileId)
            .map(FileMetadataResponse::from)
            .orElseThrow(() -> new StoredFileNotFoundException(fileId));
    }

    private FileProcessingResponse processPdf(StoredFile storedFile) {
        Path filePath = fileStorageService.resolve(storedFile.getStoragePath());
        List<ExtractedPdfChunk> extractedChunks = pdfExtractionService.extract(filePath);

        contentChunkRepository.deleteAllByFileId(storedFile.getId());
        transcriptSegmentRepository.deleteAllByFileId(storedFile.getId());

        List<ContentChunk> persistentChunks = extractedChunks.stream()
            .map(chunk -> {
                ContentChunk entity = new ContentChunk();
                entity.setFileId(storedFile.getId());
                entity.setChunkText(chunk.text());
                entity.setPageNumber(chunk.pageNumber());
                return entity;
            })
            .toList();

        contentChunkRepository.saveAll(persistentChunks);
        storedFile.setProcessingStatus(ProcessingStatus.READY);
        storedFile.setProcessingError(null);
        storedFileRepository.save(storedFile);

        return new FileProcessingResponse(
            storedFile.getId(),
            storedFile.getProcessingStatus(),
            persistentChunks.size(),
            0,
            "Extracted " + persistentChunks.size() + " content chunks from the PDF"
        );
    }

    private FileProcessingResponse processMedia(StoredFile storedFile) {
        Path filePath = fileStorageService.resolve(storedFile.getStoragePath());
        TranscriptionResult transcription = transcriptionService.transcribe(filePath);

        contentChunkRepository.deleteAllByFileId(storedFile.getId());
        transcriptSegmentRepository.deleteAllByFileId(storedFile.getId());

        List<TranscriptSegment> segments = new ArrayList<>(transcription.segments().stream()
            .filter(segment -> StringUtils.hasText(segment.text()))
            .map(segment -> {
                TranscriptSegment entity = new TranscriptSegment();
                entity.setFileId(storedFile.getId());
                entity.setSegmentText(segment.text());
                entity.setStartTime(segment.startTime());
                entity.setEndTime(segment.endTime());
                entity.setSequenceNumber(segment.sequenceNumber());
                return entity;
            })
            .toList());

        if (segments.isEmpty() && StringUtils.hasText(transcription.transcriptText())) {
            TranscriptSegment fallbackSegment = new TranscriptSegment();
            fallbackSegment.setFileId(storedFile.getId());
            fallbackSegment.setSegmentText(transcription.transcriptText());
            fallbackSegment.setStartTime(0.0);
            fallbackSegment.setEndTime(0.0);
            fallbackSegment.setSequenceNumber(1);
            segments.add(fallbackSegment);
        }

        List<ContentChunk> chunks = segments.stream()
            .map(segment -> {
                ContentChunk entity = new ContentChunk();
                entity.setFileId(storedFile.getId());
                entity.setChunkText(segment.getSegmentText());
                entity.setStartTime(segment.getStartTime());
                entity.setEndTime(segment.getEndTime());
                return entity;
            })
            .toList();

        transcriptSegmentRepository.saveAll(segments);
        contentChunkRepository.saveAll(chunks);
        storedFile.setProcessingStatus(ProcessingStatus.READY);
        storedFile.setProcessingError(null);
        storedFileRepository.save(storedFile);

        return new FileProcessingResponse(
            storedFile.getId(),
            storedFile.getProcessingStatus(),
            chunks.size(),
            segments.size(),
            "Generated " + segments.size() + " transcript segments from the media file"
        );
    }

    private void ensureFileExists(Long fileId) {
        if (!storedFileRepository.existsById(fileId)) {
            throw new StoredFileNotFoundException(fileId);
        }
    }
}
