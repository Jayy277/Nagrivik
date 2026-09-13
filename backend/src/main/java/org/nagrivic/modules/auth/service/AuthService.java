package org.nagrivic.modules.auth.service;

import org.nagrivic.modules.auth.dto.*;
import org.nagrivic.modules.auth.entity.AuthSessionEntity;
import org.nagrivic.modules.auth.entity.UserAuthIdentityEntity;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.model.OtpPurpose;
import org.nagrivic.modules.auth.provider.OtpProvider;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.UserAuthIdentityRepository;
import org.nagrivic.modules.moderation.ratelimit.RateLimiter;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String PROVIDER_GOOGLE = "GOOGLE";

    private final PhoneNumberService phoneNumberService;
    private final OtpService otpService;
    private final OtpProvider otpProvider;
    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;
    private final AuthSessionRepository authSessionRepository;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final RateLimiter rateLimiter;
    private final int refreshTokenExpirationDays;
    private final int googleAuthPerMinute;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            PhoneNumberService phoneNumberService,
            OtpService otpService,
            OtpProvider otpProvider,
            UserRepository userRepository,
            UserAuthIdentityRepository userAuthIdentityRepository,
            AuthSessionRepository authSessionRepository,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier,
            RateLimiter rateLimiter,
            @Value("${nagrivic.auth.jwt.refresh-token-expiration-days:30}") int refreshTokenExpirationDays,
            @Value("${nagrivic.moderation.rate-limit.google-auth-per-minute:10}") int googleAuthPerMinute
    ) {
        this.phoneNumberService = phoneNumberService;
        this.otpService = otpService;
        this.otpProvider = otpProvider;
        this.userRepository = userRepository;
        this.userAuthIdentityRepository = userAuthIdentityRepository;
        this.authSessionRepository = authSessionRepository;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.rateLimiter = rateLimiter;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.googleAuthPerMinute = googleAuthPerMinute;
    }

    @Transactional
    public SendOtpResponse sendOtp(SendOtpRequest request) {
        String normalizedPhone = phoneNumberService.normalize(request.phoneNumber());
        OtpService.OtpGenerationResult result = otpService.generateAndSaveOtp(normalizedPhone, OtpPurpose.LOGIN);
        otpProvider.sendOtp(normalizedPhone, result.plainOtp(), OtpPurpose.LOGIN.name());

        long expiresInSeconds = Math.max(0, Duration.between(Instant.now(), result.expiresAt()).getSeconds());
        return new SendOtpResponse("OTP sent successfully to " + normalizedPhone, expiresInSeconds);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String normalizedPhone = phoneNumberService.normalize(request.phoneNumber());
        otpService.verifyOtp(normalizedPhone, OtpPurpose.LOGIN, request.otp());

        // Find or auto-provision citizen user
        UserEntity user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseGet(() -> {
                    log.info("Auto-provisioning new citizen user for phone: {}", normalizedPhone);
                    UserEntity newUser = new UserEntity(normalizedPhone, null);
                    return userRepository.save(newUser);
                });

        return createAuthSession(user);
    }

    @Transactional
    public AuthResponse googleLogin(GoogleAuthRequest request, String clientIp) {
        // 1. Rate limiting on client IP / key
        String rateKey = "auth:google:" + (clientIp != null && !clientIp.isBlank() ? clientIp : "unknown");
        rateLimiter.checkLimit(rateKey, googleAuthPerMinute, Duration.ofMinutes(1));

        // 2. Independently verify Google credential
        GoogleUserClaims claims = googleTokenVerifier.verify(request.idToken());

        String providerSubject = claims.subject();

        // 3. Find existing auth identity or safely create citizen
        UserAuthIdentityEntity authIdentity = userAuthIdentityRepository
                .findByProviderAndProviderSubject(PROVIDER_GOOGLE, providerSubject)
                .orElse(null);

        UserEntity user;
        if (authIdentity != null) {
            user = authIdentity.getUser();
            log.info("Existing Google citizen authenticated: userId={}, subject={}", user.getId(), providerSubject);
        } else {
            // First-time Google user auto-provisioning with race-condition safety
            try {
                log.info("Auto-provisioning new Citizen account for Google subject: {}", providerSubject);
                UserEntity newUser = new UserEntity(claims.name(), claims.email(), claims.pictureUrl());
                newUser.setRole("CITIZEN");
                newUser.setActive(true);
                user = userRepository.save(newUser);

                UserAuthIdentityEntity newIdentity = new UserAuthIdentityEntity(
                        user,
                        PROVIDER_GOOGLE,
                        providerSubject,
                        claims.email()
                );
                userAuthIdentityRepository.save(newIdentity);
            } catch (DataIntegrityViolationException e) {
                // Concurrent first-time login for the same Google account: recover and use the created record
                log.info("Concurrent Google login detected for subject: {}. Recovering existing record.", providerSubject);
                UserAuthIdentityEntity existing = userAuthIdentityRepository
                        .findByProviderAndProviderSubject(PROVIDER_GOOGLE, providerSubject)
                        .orElseThrow(() -> e);
                user = existing.getUser();
            }
        }

        // 4. Reject inactive/disabled user
        if (!user.isActive()) {
            log.warn("Login attempt for deactivated citizen: userId={}", user.getId());
            throw AuthException.unauthorized("Account has been deactivated or suspended");
        }

        return createAuthSession(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawRefreshToken = request.refreshToken();
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw AuthException.badRequest("Refresh token must not be blank");
        }

        String hashed = hashRefreshToken(rawRefreshToken);
        AuthSessionEntity session = authSessionRepository.findByRefreshTokenHash(hashed)
                .orElseThrow(() -> AuthException.unauthorized("Invalid or expired refresh token"));

        if (!session.isActive()) {
            throw AuthException.unauthorized("Refresh token is revoked or expired");
        }

        UserEntity user = session.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole());

        // Session rotation
        String newRawRefreshToken = generateOpaqueToken();
        String newHashedRefreshToken = hashRefreshToken(newRawRefreshToken);
        Instant newExpiresAt = Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        AuthSessionEntity newSession = authSessionRepository.save(
                new AuthSessionEntity(user, newHashedRefreshToken, newExpiresAt)
        );

        session.rotate(newSession.getId());
        authSessionRepository.save(session);

        long expiresIn = jwtService.getAccessTokenExpirationMinutes() * 60;
        UserAuthSummary userSummary = toUserSummary(user);

        return new AuthResponse(newAccessToken, newRawRefreshToken, "Bearer", expiresIn, userSummary);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String hashed = hashRefreshToken(request.refreshToken());
            authSessionRepository.findByRefreshTokenHash(hashed).ifPresent(session -> {
                session.revoke();
                authSessionRepository.save(session);
                log.info("Revoked auth session for user: {}", session.getUser().getId());
            });
        }
    }

    @Transactional(readOnly = true)
    public UserAuthSummary getCurrentUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> AuthException.unauthorized("User not found"));

        return toUserSummary(user);
    }

    private AuthResponse createAuthSession(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());

        // Create refresh token session
        String rawRefreshToken = generateOpaqueToken();
        String hashedRefreshToken = hashRefreshToken(rawRefreshToken);
        Instant refreshExpiresAt = Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        AuthSessionEntity session = new AuthSessionEntity(user, hashedRefreshToken, refreshExpiresAt);
        authSessionRepository.save(session);

        long expiresIn = jwtService.getAccessTokenExpirationMinutes() * 60;
        UserAuthSummary userSummary = toUserSummary(user);

        return new AuthResponse(accessToken, rawRefreshToken, "Bearer", expiresIn, userSummary);
    }

    private UserAuthSummary toUserSummary(UserEntity user) {
        return new UserAuthSummary(
                user.getId(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole(),
                user.getFullName(),
                user.getProfilePictureUrl()
        );
    }

    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
