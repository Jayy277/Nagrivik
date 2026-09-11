package org.nagrivic.modules.media.repository;

import org.nagrivic.modules.media.entity.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {

    List<MediaEntity> findByIssue_IdOrderByDisplayOrderAsc(UUID issueId);

    long countByIssue_Id(UUID issueId);

    boolean existsByStorageKey(String storageKey);
}
