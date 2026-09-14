package org.nagrivic.modules.push.repository;

import org.nagrivic.modules.push.entity.PushDeviceEntity;
import org.nagrivic.modules.push.model.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PushDeviceRepository extends JpaRepository<PushDeviceEntity, UUID> {

    Optional<PushDeviceEntity> findByDeviceToken(String deviceToken);

    List<PushDeviceEntity> findByUser_IdAndIsActiveTrue(UUID userId);

    List<PushDeviceEntity> findByUser_IdAndPlatformAndIsActiveTrue(UUID userId, PlatformType platform);

    List<PushDeviceEntity> findByUser_Id(UUID userId);
}
