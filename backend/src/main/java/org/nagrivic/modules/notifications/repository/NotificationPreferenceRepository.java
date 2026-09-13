package org.nagrivic.modules.notifications.repository;

import org.nagrivic.modules.notifications.entity.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, UUID> {

    Optional<NotificationPreferenceEntity> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);
}
