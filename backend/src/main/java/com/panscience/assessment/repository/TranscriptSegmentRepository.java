package com.panscience.assessment.repository;

import com.panscience.assessment.entity.TranscriptSegment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    void deleteAllByFileId(Long fileId);

    List<TranscriptSegment> findAllByFileIdOrderBySequenceNumberAsc(Long fileId);
}

