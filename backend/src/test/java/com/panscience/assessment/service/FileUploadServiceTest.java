package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.panscience.assessment.dto.FileMetadataResponse;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.entity.StoredFile;
import com.panscience.assessment.exception.StoredFileNotFoundException;
import com.panscience.assessment.repository.StoredFileRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private StoredFileRepository storedFileRepository;

    @Mock
    private FileTypeClassifier fileTypeClassifier;

    @Mock
    private FileStorageService fileStorageService;

    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService(
            storedFileRepository,
            fileTypeClassifier,
            fileStorageService
        );
    }

    @Test
    void uploadsFileSuccessfully() throws Exception {
        MockMultipartFile multipart = new MockMultipartFile(
            "file", "report.pdf", "application/pdf", "pdf-content".getBytes()
        );

        when(fileTypeClassifier.classify(any(MultipartFile.class))).thenReturn(FileCategory.PDF);
        when(fileStorageService.store(any(), any()))
            .thenReturn(new StoredFileLocation("stored-report.pdf", "pdf/stored-report.pdf"));

        StoredFile saved = savedFile(1L, "report.pdf", FileCategory.PDF);
        when(storedFileRepository.save(any(StoredFile.class))).thenReturn(saved);

        FileMetadataResponse response = fileUploadService.upload(multipart);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.originalName()).isEqualTo("report.pdf");
        assertThat(response.fileCategory()).isEqualTo(FileCategory.PDF);
        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
    }

    @Test
    void rejectsNullFile() {
        assertThatThrownBy(() -> fileUploadService.upload(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-empty file");
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertThatThrownBy(() -> fileUploadService.upload(emptyFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-empty file");
    }

    @Test
    void listsFilesOrderedByCreatedAt() {
        StoredFile f1 = savedFile(1L, "a.pdf", FileCategory.PDF);
        StoredFile f2 = savedFile(2L, "b.mp4", FileCategory.VIDEO);
        when(storedFileRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(f2, f1));

        List<FileMetadataResponse> result = fileUploadService.listFiles();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(2L);
        assertThat(result.get(1).id()).isEqualTo(1L);
    }

    @Test
    void returnsFileByIdSuccessfully() {
        StoredFile f = savedFile(5L, "doc.pdf", FileCategory.PDF);
        when(storedFileRepository.findById(5L)).thenReturn(Optional.of(f));

        FileMetadataResponse result = fileUploadService.getFile(5L);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.originalName()).isEqualTo("doc.pdf");
    }

    @Test
    void throwsNotFoundWhenFileIdMissing() {
        when(storedFileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileUploadService.getFile(99L))
            .isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    void loadsFileContentSuccessfully() throws Exception {
        StoredFile storedFile = savedFile(3L, "clip.mp4", FileCategory.VIDEO);
        storedFile.setStoragePath("video/clip.mp4");
        storedFile.setContentType("video/mp4");

        Path realFile = tempDir.resolve("clip.mp4");
        java.nio.file.Files.writeString(realFile, "video-data");

        when(storedFileRepository.findById(3L)).thenReturn(Optional.of(storedFile));
        when(fileStorageService.resolve("video/clip.mp4")).thenReturn(realFile);

        StoredFileResource resource = fileUploadService.loadFileContent(3L);

        assertThat(resource.originalName()).isEqualTo("clip.mp4");
        assertThat(resource.contentType()).isEqualTo("video/mp4");
    }

    @Test
    void throwsStorageExceptionWhenFileContentMissing() throws Exception {
        StoredFile storedFile = savedFile(4L, "missing.pdf", FileCategory.PDF);
        storedFile.setStoragePath("pdf/missing.pdf");

        Path missingPath = tempDir.resolve("missing.pdf"); // does NOT exist

        when(storedFileRepository.findById(4L)).thenReturn(Optional.of(storedFile));
        when(fileStorageService.resolve("pdf/missing.pdf")).thenReturn(missingPath);

        assertThatThrownBy(() -> fileUploadService.loadFileContent(4L))
            .isInstanceOf(com.panscience.assessment.exception.StorageOperationException.class);
    }

    private StoredFile savedFile(Long id, String name, FileCategory category) {
        StoredFile f = new StoredFile();
        ReflectionTestUtils.setField(f, "id", id);
        f.setOriginalName(name);
        f.setStoredName("stored-" + name);
        f.setContentType("application/octet-stream");
        f.setFileCategory(category);
        f.setProcessingStatus(ProcessingStatus.UPLOADED);
        f.setStoragePath(category.name().toLowerCase() + "/stored-" + name);
        return f;
    }
}
