package com.panscience.assessment.repository;

import com.panscience.assessment.entity.ChunkEmbedding;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbedding, Long> {

    List<ChunkEmbedding> findAllByChunkIdInAndEmbeddingModel(Collection<Long> chunkIds, String embeddingModel);
}
