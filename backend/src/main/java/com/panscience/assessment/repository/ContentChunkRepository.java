package com.panscience.assessment.repository;

import com.panscience.assessment.entity.ContentChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentChunkRepository extends JpaRepository<ContentChunk, Long> {

    void deleteAllByFileId(Long fileId);

    List<ContentChunk> findAllByFileIdOrderByPageNumberAscIdAsc(Long fileId);
}

