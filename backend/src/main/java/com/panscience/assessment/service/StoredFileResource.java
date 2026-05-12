package com.panscience.assessment.service;

import java.nio.file.Path;

public record StoredFileResource(
    String originalName,
    String contentType,
    Path path
) {
}
