package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.panscience.assessment.config.StorageProperties;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.exception.StorageOperationException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private StorageProperties storageProperties;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        when(storageProperties.uploadDir()).thenReturn(tempDir.toString());
        fileStorageService = new FileStorageService(storageProperties);
    }

    @Test
    void storesFileSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "content".getBytes()
        );

        StoredFileLocation location = fileStorageService.store(file, FileCategory.PDF);

        assertThat(location.storedName()).endsWith(".pdf");
        assertThat(location.relativePath()).startsWith("pdf/");
        
        Path storedPath = tempDir.resolve(location.relativePath());
        assertThat(Files.exists(storedPath)).isTrue();
        assertThat(Files.readAllBytes(storedPath)).isEqualTo("content".getBytes());
    }

    @Test
    void storesFileWithNoExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test", "application/octet-stream", "raw".getBytes()
        );

        StoredFileLocation location = fileStorageService.store(file, FileCategory.VIDEO);

        assertThat(location.storedName()).doesNotContain(".");
        assertThat(location.relativePath()).startsWith("video/");
    }

    @Test
    void resolvesPathCorrectly() {
        Path resolved = fileStorageService.resolve("some/file.txt");
        assertThat(resolved).isEqualTo(tempDir.resolve("some/file.txt").toAbsolutePath().normalize());
    }
}
