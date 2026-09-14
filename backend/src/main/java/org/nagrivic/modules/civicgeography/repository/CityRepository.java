package org.nagrivic.modules.civicgeography.repository;

import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<CityEntity, UUID> {
    Optional<CityEntity> findByNameAndState(String name, String state);
    boolean existsByNameIgnoreCaseAndStateIgnoreCase(String name, String state);
    List<CityEntity> findByState(String state);
    List<CityEntity> findByCivicBodyId(UUID civicBodyId);
    long countByCivicBodyId(UUID civicBodyId);
    org.springframework.data.domain.Page<CityEntity> findAllByOrderByNameAsc(org.springframework.data.domain.Pageable pageable);
    List<CityEntity> findAllByOrderByNameAsc();
}
