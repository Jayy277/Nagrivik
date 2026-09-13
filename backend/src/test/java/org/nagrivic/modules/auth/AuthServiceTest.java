package org.nagrivic.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.dto.*;
import org.nagrivic.modules.auth.entity.AuthSessionEntity;
import org.nagrivic.modules.auth.entity.OtpVerificationEntity;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.model.OtpPurpose;
import org.nagrivic.modules.auth.provider.OtpProvider;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.OtpVerificationRepository;
import org.nagrivic.modules.auth.service.AuthService;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.auth.service.OtpService;
import org.nagrivic.modules.auth.service.PhoneNumberService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private PhoneNumberService phoneNumberService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        authSessionRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        authSessionRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==========================================
    // 1. PHONE NUMBER NORMALIZATION TESTS
    // ==========================================

    @Test
    void testPhoneNumberNormalization() {
        assertEquals("+919876543210", phoneNumberService.normalize("9876543210"));
        assertEquals("+919876543210", phoneNumberService.normalize("+919876543210"));
        assertEquals("+919876543210", phoneNumberService.normalize("+91 98765 43210"));
        assertEquals("+919876543210", phoneNumberService.normalize("09876543210"));
        assertEquals("+919876543210", phoneNumberService.normalize("98765-43210"));

        assertThrows(IllegalArgumentException.class, () -> phoneNumberService.normalize("12345"));
        assertThrows(IllegalArgumentException.class, () -> phoneNumberService.normalize("abcdefghij"));
        assertThrows(IllegalArgumentException.class, () -> phoneNumberService.normalize(""));
        assertThrows(IllegalArgumentException.class, () -> phoneNumberService.normalize(null));
    }

    // ==========================================
    // 2. OTP GENERATION & COOLDOWN TESTS
    // ==========================================

    @Test
    void testOtpGenerationAndCooldown() {
        String phone = "+919876543210";
        var result = otpService.generateAndSaveOtp(phone, OtpPurpose.LOGIN);

        assertNotNull(result.plainOtp());
        assertEquals(6, result.plainOtp().length());
        assertTrue(result.expiresAt().isAfter(Instant.now()));

        // Verification entity in repository
        Optional<OtpVerificationEntity> entityOpt = otpVerificationRepository
                .findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(phone, OtpPurpose.LOGIN);
        assertTrue(entityOpt.isPresent());
        OtpVerificationEntity entity = entityOpt.get();
        assertTrue(passwordEncoder.matches(result.plainOtp(), entity.getOtpHash()));

        // Immediate next generation should trigger cooldown rate limiting
        assertThrows(AuthException.class, () -> otpService.generateAndSaveOtp(phone, OtpPurpose.LOGIN));
    }

    // ==========================================
    // 3. OTP VERIFICATION TESTS
    // ==========================================

    @Test
    void testOtpVerificationSuccessAndFailure() {
        String phone = "+919876543210";
        var result = otpService.generateAndSaveOtp(phone, OtpPurpose.LOGIN);

        // Invalid OTP attempt
        assertThrows(AuthException.class, () -> otpService.verifyOtp(phone, OtpPurpose.LOGIN, "000000"));

        OtpVerificationEntity entityAfterFail = otpVerificationRepository
                .findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(phone, OtpPurpose.LOGIN).orElseThrow();
        assertEquals(1, entityAfterFail.getAttempts());
        assertFalse(entityAfterFail.isConsumed());

        // Valid OTP attempt
        OtpVerificationEntity verified = otpService.verifyOtp(phone, OtpPurpose.LOGIN, result.plainOtp());
        assertTrue(verified.isConsumed());
    }

    @Test
    void testOtpVerificationExpired() {
        String phone = "+919876543210";
        String hashed = passwordEncoder.encode("123456");
        OtpVerificationEntity expired = new OtpVerificationEntity(
                phone,
                hashed,
                OtpPurpose.LOGIN,
                Instant.now().minus(5, ChronoUnit.MINUTES),
                5
        );
        otpVerificationRepository.save(expired);

        assertThrows(AuthException.class, () -> otpService.verifyOtp(phone, OtpPurpose.LOGIN, "123456"));
    }

    // ==========================================
    // 4. AUTH SERVICE & AUTO-PROVISIONING TESTS
    // ==========================================

    @Test
    void testAuthWorkflowWithCitizenAutoProvisioning() {
        String phone = "9876543210";
        SendOtpResponse sendResp = authService.sendOtp(new SendOtpRequest(phone));
        assertNotNull(sendResp);

        // Grab generated OTP from repo
        OtpVerificationEntity entity = otpVerificationRepository
                .findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc("+919876543210", OtpPurpose.LOGIN).orElseThrow();

        // Let's create an OTP we know for direct verification
        entity.setOtpHash(passwordEncoder.encode("654321"));
        otpVerificationRepository.save(entity);

        AuthResponse authResp = authService.verifyOtp(new VerifyOtpRequest(phone, "654321"));
        assertNotNull(authResp.accessToken());
        assertNotNull(authResp.refreshToken());
        assertEquals("Bearer", authResp.tokenType());
        assertEquals("+919876543210", authResp.user().phoneNumber());
        assertEquals("CITIZEN", authResp.user().role());
        assertNull(authResp.user().fullName());

        // Verify JWT claims
        assertTrue(jwtService.validateToken(authResp.accessToken()));
        assertEquals(authResp.user().id(), jwtService.extractUserId(authResp.accessToken()));
        assertEquals("CITIZEN", jwtService.extractRole(authResp.accessToken()));
    }

    // ==========================================
    // 5. REFRESH TOKEN ROTATION TESTS
    // ==========================================

    @Test
    void testRefreshTokenRotation() {
        UserEntity user = userRepository.save(new UserEntity("+919876543210", "Citizen Test"));
        OtpVerificationEntity entity = new OtpVerificationEntity(
                "+919876543210",
                passwordEncoder.encode("112233"),
                OtpPurpose.LOGIN,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                5
        );
        otpVerificationRepository.save(entity);

        AuthResponse authResp = authService.verifyOtp(new VerifyOtpRequest("+919876543210", "112233"));
        String oldRefreshToken = authResp.refreshToken();

        // Perform refresh
        AuthResponse refreshResp = authService.refreshToken(new RefreshTokenRequest(oldRefreshToken));
        assertNotNull(refreshResp.accessToken());
        assertNotNull(refreshResp.refreshToken());
        assertNotEquals(oldRefreshToken, refreshResp.refreshToken());

        // Trying to use old refresh token again must fail (it was rotated/revoked)
        assertThrows(AuthException.class, () -> authService.refreshToken(new RefreshTokenRequest(oldRefreshToken)));
    }

    // ==========================================
    // 6. LOGOUT TESTS
    // ==========================================

    @Test
    void testLogoutRevocation() {
        UserEntity user = userRepository.save(new UserEntity("+919876543210", "Citizen Test"));
        OtpVerificationEntity entity = new OtpVerificationEntity(
                "+919876543210",
                passwordEncoder.encode("112233"),
                OtpPurpose.LOGIN,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                5
        );
        otpVerificationRepository.save(entity);

        AuthResponse authResp = authService.verifyOtp(new VerifyOtpRequest("+919876543210", "112233"));
        authService.logout(new LogoutRequest(authResp.refreshToken()));

        // Subsequent refresh must fail
        assertThrows(AuthException.class, () -> authService.refreshToken(new RefreshTokenRequest(authResp.refreshToken())));
    }
}
