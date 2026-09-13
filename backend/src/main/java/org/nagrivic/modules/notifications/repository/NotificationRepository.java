package org.nagrivic.modules.notifications.repository;

import org.nagrivic.modules.notifications.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<NotificationEntity> findByIdAndUser_Id(UUID id, UUID userId);

    long countByUser_IdAndReadAtIsNull(UUID userId);

    boolean existsByUser_IdAndEventKey(UUID userId, String eventKey);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.readAt = :now WHERE n.user.id = :userId AND n.readAt IS NULL")
    int markAllAsReadForUser(@Param("userId") UUID userId, @Param("now") java.time.Instant now);

    default int markAllAsReadForUser(UUID userId) {
        return markAllAsReadForUser(userId, java.time.Instant.now());
    }
}
