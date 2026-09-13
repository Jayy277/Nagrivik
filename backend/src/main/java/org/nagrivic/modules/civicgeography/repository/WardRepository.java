package org.nagrivic.modules.civicgeography.repository;

import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WardRepository extends JpaRepository<WardEntity, UUID> {
    List<WardEntity> findByCityId(UUID cityId);
    Optional<WardEntity> findByCityIdAndWardNumber(UUID cityId, String wardNumber);
}
