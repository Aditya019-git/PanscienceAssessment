package com.panscience.assessment.service;

import com.panscience.assessment.config.StorageProperties;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.exception.StorageOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path rootDirectory;

    public FileStorageService(StorageProperties storageProperties) {
        this.rootDirectory = Path.of(storageProperties.uploadDir()).toAbsolutePath().normalize();
    }

    public StoredFileLocation store(MultipartFile file, FileCategory fileCategory) {
        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
            ? StringUtils.cleanPath(file.getOriginalFilename())
            : "uploaded-file";
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String suffix = StringUtils.hasText(extension) ? "." + extension.toLowerCase(Locale.ROOT) : "";
        String folderName = fileCategory.name().toLowerCase(Locale.ROOT);
        String storedName = UUID.randomUUID() + suffix;
        String relativePath = folderName + "/" + storedName;

        try {
            Files.createDirectories(rootDirectory.resolve(folderName));
            Path targetPath = rootDirectory.resolve(relativePath).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StoredFileLocation(storedName, relativePath);
        } catch (IOException exception) {
            throw new StorageOperationException("Failed to store uploaded file " + originalFilename, exception);
        }
    }

    public Path resolve(String relativePath) {
        return rootDirectory.resolve(relativePath).normalize();
    }
}

