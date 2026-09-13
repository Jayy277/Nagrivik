package org.nagrivic.modules.auth.service;

import org.nagrivic.modules.auth.entity.OtpVerificationEntity;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.model.OtpPurpose;
import org.nagrivic.modules.auth.repository.OtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final int expirationMinutes;
    private final int cooldownSeconds;
    private final int maxAttempts;
    private final SecureRandom secureRandom = new SecureRandom();

    public record OtpGenerationResult(String plainOtp, Instant expiresAt) {}

    public OtpService(
            OtpVerificationRepository otpVerificationRepository,
            PasswordEncoder passwordEncoder,
            @Value("${nagrivic.auth.otp.expiration-minutes:5}") int expirationMinutes,
            @Value("${nagrivic.auth.otp.cooldown-seconds:60}") int cooldownSeconds,
            @Value("${nagrivic.auth.otp.max-attempts:5}") int maxAttempts
    ) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
        this.cooldownSeconds = cooldownSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public OtpGenerationResult generateAndSaveOtp(String phoneNumber, OtpPurpose purpose) {
        // Enforce cooldown
        Optional<OtpVerificationEntity> latestOpt = otpVerificationRepository
                .findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(phoneNumber, purpose);

        if (latestOpt.isPresent()) {
            OtpVerificationEntity latest = latestOpt.get();
            if (latest.getCreatedAt() != null) {
                long elapsedSeconds = Duration.between(latest.getCreatedAt(), Instant.now()).getSeconds();
                if (elapsedSeconds < cooldownSeconds) {
                    long waitSeconds = cooldownSeconds - elapsedSeconds;
                    throw AuthException.rateLimited("Please wait " + waitSeconds + " seconds before requesting a new OTP.");
                }
            }
        }

        // Generate 6-digit OTP
        int code = secureRandom.nextInt(900_000) + 100_000;
        String plainOtp = String.valueOf(code);
        String hashedOtp = passwordEncoder.encode(plainOtp);
        Instant expiresAt = Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);

        OtpVerificationEntity entity = new OtpVerificationEntity(
                phoneNumber,
                hashedOtp,
                purpose,
                expiresAt,
                maxAttempts
        );

        otpVerificationRepository.save(entity);
        log.info("Generated new OTP verification for phone: {} [expiresAt={}]", phoneNumber, expiresAt);

        return new OtpGenerationResult(plainOtp, expiresAt);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public OtpVerificationEntity verifyOtp(String phoneNumber, OtpPurpose purpose, String code) {
        OtpVerificationEntity verification = otpVerificationRepository
                .findTopByPhoneNumberAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber, purpose)
                .orElseThrow(() -> AuthException.badRequest("No active OTP found. Please request a new OTP."));

        if (verification.isExpired()) {
            throw AuthException.badRequest("OTP has expired. Please request a new OTP.");
        }

        if (verification.isMaxAttemptsExceeded()) {
            throw AuthException.badRequest("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(code, verification.getOtpHash())) {
            verification.incrementAttempts();
            otpVerificationRepository.save(verification);
            int remaining = verification.getMaxAttempts() - verification.getAttempts();
            if (remaining <= 0) {
                throw AuthException.badRequest("Invalid OTP. Maximum attempts exceeded. Please request a new OTP.");
            }
            throw AuthException.badRequest("Invalid OTP. Attempts remaining: " + remaining);
        }

        verification.consume();
        return otpVerificationRepository.save(verification);
    }
}
