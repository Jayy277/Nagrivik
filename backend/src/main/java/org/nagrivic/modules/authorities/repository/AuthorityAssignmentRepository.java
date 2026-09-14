package org.nagrivic.modules.authorities.repository;

import org.nagrivic.modules.authorities.entity.AuthorityAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuthorityAssignmentRepository extends JpaRepository<AuthorityAssignmentEntity, UUID> {

    @Query("SELECT a FROM AuthorityAssignmentEntity a " +
           "LEFT JOIN FETCH a.civicBody " +
           "LEFT JOIN FETCH a.city " +
           "LEFT JOIN FETCH a.ward " +
           "LEFT JOIN FETCH a.department " +
           "WHERE a.user.id = :userId AND a.active = true")
    List<AuthorityAssignmentEntity> findByUser_IdAndActiveTrue(@Param("userId") UUID userId);

    List<AuthorityAssignmentEntity> findByUser_Id(UUID userId);
}
