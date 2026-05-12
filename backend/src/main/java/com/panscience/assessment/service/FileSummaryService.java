package com.panscience.assessment.service;

import com.panscience.assessment.entity.StoredFile;
import java.util.List;

public interface FileSummaryService {

    String generateSummary(StoredFile storedFile, List<String> contentParts);
}
