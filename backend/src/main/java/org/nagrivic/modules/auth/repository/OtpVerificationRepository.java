package org.nagrivic.modules.auth.repository;

import org.nagrivic.modules.auth.entity.OtpVerificationEntity;
import org.nagrivic.modules.auth.model.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerificationEntity, UUID> {

    Optional<OtpVerificationEntity> findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(
            String phoneNumber,
            OtpPurpose purpose
    );

    Optional<OtpVerificationEntity> findTopByPhoneNumberAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String phoneNumber,
            OtpPurpose purpose
    );
}
