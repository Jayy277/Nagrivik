package org.nagrivic.modules.civicgeography.repository;

import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CivicBodyRepository extends JpaRepository<CivicBodyEntity, UUID> {
    Optional<CivicBodyEntity> findByName(String name);
    List<CivicBodyEntity> findByStateAndCity(String state, String city);
}
