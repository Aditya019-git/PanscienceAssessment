package com.panscience.assessment.service;

import com.panscience.assessment.dto.FileMetadataResponse;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.entity.StoredFile;
import com.panscience.assessment.exception.StoredFileNotFoundException;
import com.panscience.assessment.repository.StoredFileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    private final StoredFileRepository storedFileRepository;
    private final FileTypeClassifier fileTypeClassifier;
    private final FileStorageService fileStorageService;

    public FileUploadService(
        StoredFileRepository storedFileRepository,
        FileTypeClassifier fileTypeClassifier,
        FileStorageService fileStorageService
    ) {
        this.storedFileRepository = storedFileRepository;
        this.fileTypeClassifier = fileTypeClassifier;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public FileMetadataResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty file is required");
        }

        FileCategory fileCategory = fileTypeClassifier.classify(file);
        StoredFileLocation location = fileStorageService.store(file, fileCategory);
        StoredFile storedFile = new StoredFile();

        storedFile.setOriginalName(safeOriginalFilename(file));
        storedFile.setStoredName(location.storedName());
        storedFile.setContentType(defaultContentType(file.getContentType()));
        storedFile.setFileCategory(fileCategory);
        storedFile.setProcessingStatus(ProcessingStatus.UPLOADED);
        storedFile.setStoragePath(location.relativePath());

        return FileMetadataResponse.from(storedFileRepository.save(storedFile));
    }

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> listFiles() {
        return storedFileRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(FileMetadataResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public FileMetadataResponse getFile(Long id) {
        return storedFileRepository.findById(id)
            .map(FileMetadataResponse::from)
            .orElseThrow(() -> new StoredFileNotFoundException(id));
    }

    private String safeOriginalFilename(MultipartFile file) {
        return StringUtils.hasText(file.getOriginalFilename())
            ? StringUtils.cleanPath(file.getOriginalFilename())
            : "uploaded-file";
    }

    private String defaultContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }
}

