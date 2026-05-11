package com.panscience.assessment.repository;

import com.panscience.assessment.entity.StoredFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    List<StoredFile> findAllByOrderByCreatedAtDesc();
}

