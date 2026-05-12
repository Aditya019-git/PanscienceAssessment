package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileTypeClassifierTest {

    private final FileTypeClassifier fileTypeClassifier = new FileTypeClassifier();

    @Test
    void classifiesPdfUpload() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "spec.pdf",
            "application/pdf",
            "pdf-content".getBytes()
        );

        assertThat(fileTypeClassifier.classify(file)).isEqualTo(FileCategory.PDF);
    }

    @Test
    void classifiesAudioUsingExtensionFallback() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "clip.mp3",
            "application/octet-stream",
            "audio-content".getBytes()
        );

        assertThat(fileTypeClassifier.classify(file)).isEqualTo(FileCategory.AUDIO);
    }

    @Test
    void classifiesVideoUsingContentType() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "movie.mov",
            "video/mp4",
            "video-content".getBytes()
        );
        assertThat(fileTypeClassifier.classify(file)).isEqualTo(FileCategory.VIDEO);
    }

    @Test
    void handlesNullContentTypeAndNoExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "unknown",
            null,
            "content".getBytes()
        );
        assertThatThrownBy(() -> fileTypeClassifier.classify(file))
            .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void handlesEmptyFilename() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "",
            "audio/wav",
            "content".getBytes()
        );
        assertThat(fileTypeClassifier.classify(file)).isEqualTo(FileCategory.AUDIO);
    }

    @Test
    void classifiesMixedCaseExtensions() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "CHART.PDF",
            "application/octet-stream",
            "pdf-content".getBytes()
        );
        assertThat(fileTypeClassifier.classify(file)).isEqualTo(FileCategory.PDF);
    }

    @Test
    void rejectsUnsupportedFileTypes() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "notes.txt",
            "text/plain",
            "plain text".getBytes()
        );

        assertThatThrownBy(() -> fileTypeClassifier.classify(file))
            .isInstanceOf(UnsupportedFileTypeException.class);
    }
}

