package com.panscience.assessment.service;

import java.nio.file.Path;

public interface TranscriptionService {

    TranscriptionResult transcribe(Path mediaFilePath);
}

