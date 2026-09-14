package org.nagrivic.modules.media.ai.repository;

import org.nagrivic.modules.media.ai.entity.ImageAiAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageAiAnalysisRepository extends JpaRepository<ImageAiAnalysisEntity, UUID> {

    Optional<ImageAiAnalysisEntity> findByMedia_Id(UUID mediaId);

    default Optional<ImageAiAnalysisEntity> findByMediaId(UUID mediaId) {
        return findByMedia_Id(mediaId);
    }

    java.util.List<ImageAiAnalysisEntity> findByIssue_Id(UUID issueId);

    boolean existsByMedia_Id(UUID mediaId);

    void deleteByMedia_Id(UUID mediaId);
}
