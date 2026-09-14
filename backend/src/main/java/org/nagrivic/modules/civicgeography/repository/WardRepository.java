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
    Optional<WardEntity> findByCityIdAndWardCode(UUID cityId, String wardCode);
    boolean existsByCityIdAndWardNumber(UUID cityId, String wardNumber);
    boolean existsByCityIdAndWardCode(UUID cityId, String wardCode);

    long countByCivicBodyId(UUID civicBodyId);
    long countByCityId(UUID cityId);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM WardEntity w " +
           "WHERE (:cityId IS NULL OR w.city.id = :cityId) " +
           "AND (:isActive IS NULL OR w.isActive = :isActive) " +
           "AND (:search IS NULL OR LOWER(w.wardName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(w.wardNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(w.wardCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY w.wardNumber ASC")
    org.springframework.data.domain.Page<WardEntity> searchWards(
            @org.springframework.data.repository.query.Param("cityId") UUID cityId,
            @org.springframework.data.repository.query.Param("isActive") Boolean isActive,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    List<WardEntity> findAllByOrderByWardNumberAsc();
}
