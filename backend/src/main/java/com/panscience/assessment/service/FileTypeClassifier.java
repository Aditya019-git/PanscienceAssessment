package com.panscience.assessment.service;

import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.exception.UnsupportedFileTypeException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileTypeClassifier {

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "mpga");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mpeg", "webm");

    public FileCategory classify(MultipartFile file) {
        String contentType = safeContentType(file.getContentType());
        String filename = safeFilename(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(filename);
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);

        if ("application/pdf".equals(contentType) || "pdf".equals(normalizedExtension)) {
            return FileCategory.PDF;
        }

        if (contentType.startsWith("audio/") || AUDIO_EXTENSIONS.contains(normalizedExtension)) {
            return FileCategory.AUDIO;
        }

        if (contentType.startsWith("video/") || VIDEO_EXTENSIONS.contains(normalizedExtension)) {
            return FileCategory.VIDEO;
        }

        throw new UnsupportedFileTypeException(contentType, filename);
    }

    private String safeContentType(String contentType) {
        return contentType == null ? "application/octet-stream" : contentType.toLowerCase(Locale.ROOT);
    }

    private String safeFilename(String filename) {
        return StringUtils.hasText(filename) ? filename : "uploaded-file";
    }
}

