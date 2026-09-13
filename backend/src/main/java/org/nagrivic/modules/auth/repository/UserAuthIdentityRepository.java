package org.nagrivic.modules.auth.repository;

import org.nagrivic.modules.auth.entity.UserAuthIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAuthIdentityRepository extends JpaRepository<UserAuthIdentityEntity, UUID> {

    Optional<UserAuthIdentityEntity> findByProviderAndProviderSubject(String provider, String providerSubject);

    Optional<UserAuthIdentityEntity> findByUser_IdAndProvider(UUID userId, String provider);

    List<UserAuthIdentityEntity> findByUser_Id(UUID userId);

    boolean existsByProviderAndProviderSubject(String provider, String providerSubject);
}
