package org.nagrivic.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nagrivic.modules.auth.dto.*;
import org.nagrivic.modules.auth.entity.AuthSessionEntity;
import org.nagrivic.modules.auth.entity.UserAuthIdentityEntity;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.provider.OtpProvider;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.UserAuthIdentityRepository;
import org.nagrivic.modules.auth.service.*;
import org.nagrivic.modules.moderation.ratelimit.RateLimitExceededException;
import org.nagrivic.modules.moderation.ratelimit.RateLimiter;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private PhoneNumberService phoneNumberService;

    @Mock
    private OtpService otpService;

    @Mock
    private OtpProvider otpProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthIdentityRepository userAuthIdentityRepository;

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private RateLimiter rateLimiter;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                phoneNumberService,
                otpService,
                otpProvider,
                userRepository,
                userAuthIdentityRepository,
                authSessionRepository,
                jwtService,
                googleTokenVerifier,
                rateLimiter,
                30,
                10
        );
    }

    // 1. Valid Google credential creates citizen user
    @Test
    void shouldCreateNewCitizenAccountOnFirstTimeGoogleLogin() {
        GoogleUserClaims claims = new GoogleUserClaims(
                "google-sub-12345",
                "citizen@nagrivic.org",
                true,
                "Priya Sharma",
                "https://lh3.googleusercontent.com/photo.jpg"
        );

        when(googleTokenVerifier.verify("valid-id-token")).thenReturn(claims);
        when(userAuthIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-12345"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(jwtService.generateAccessToken(any(UUID.class), eq("CITIZEN"))).thenReturn("nagrivic-jwt-access-token");
        when(jwtService.getAccessTokenExpirationMinutes()).thenReturn(15L);

        AuthResponse response = authService.googleLogin(new GoogleAuthRequest("valid-id-token"), "127.0.0.1");

        assertNotNull(response);
        assertEquals("nagrivic-jwt-access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertEquals("Priya Sharma", response.user().fullName());
        assertEquals("citizen@nagrivic.org", response.user().email());
        assertEquals("CITIZEN", response.user().role());

        verify(userRepository).save(argThat(user ->
                "CITIZEN".equals(user.getRole()) &&
                user.isActive() &&
                "Priya Sharma".equals(user.getFullName()) &&
                "citizen@nagrivic.org".equals(user.getEmail()) &&
                user.getPhoneNumber() == null
        ));

        verify(userAuthIdentityRepository).save(argThat(identity ->
                "GOOGLE".equals(identity.getProvider()) &&
                "google-sub-12345".equals(identity.getProviderSubject()) &&
                "citizen@nagrivic.org".equals(identity.getProviderEmail())
        ));

        verify(authSessionRepository).save(any(AuthSessionEntity.class));
    }

    // 2. Valid Google credential logs in existing user
    @Test
    void shouldLoginExistingCitizenWithoutCreatingDuplicateAccount() {
        GoogleUserClaims claims = new GoogleUserClaims(
                "google-sub-existing",
                "existing@nagrivic.org",
                true,
                "Existing Citizen",
                null
        );

        UserEntity existingUser = new UserEntity("Existing Citizen", "existing@nagrivic.org", null);
        existingUser.setId(UUID.randomUUID());
        existingUser.setRole("CITIZEN");
        existingUser.setActive(true);

        UserAuthIdentityEntity existingIdentity = new UserAuthIdentityEntity(
                existingUser,
                "GOOGLE",
                "google-sub-existing",
                "existing@nagrivic.org"
        );

        when(googleTokenVerifier.verify("existing-id-token")).thenReturn(claims);
        when(userAuthIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-existing"))
                .thenReturn(Optional.of(existingIdentity));

        when(jwtService.generateAccessToken(eq(existingUser.getId()), eq("CITIZEN"))).thenReturn("nagrivic-access-token-existing");
        when(jwtService.getAccessTokenExpirationMinutes()).thenReturn(15L);

        AuthResponse response = authService.googleLogin(new GoogleAuthRequest("existing-id-token"), "127.0.0.1");

        assertNotNull(response);
        assertEquals(existingUser.getId(), response.user().id());
        assertEquals("existing@nagrivic.org", response.user().email());
        assertEquals("Existing Citizen", response.user().fullName());

        // Verify NO new user was saved
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(userAuthIdentityRepository, never()).save(any(UserAuthIdentityEntity.class));
        verify(authSessionRepository).save(any(AuthSessionEntity.class));
    }

    // 3. Disabled user rejected
    @Test
    void shouldRejectDisabledUser() {
        GoogleUserClaims claims = new GoogleUserClaims(
                "google-sub-disabled",
                "banned@nagrivic.org",
                true,
                "Banned User",
                null
        );

        UserEntity disabledUser = new UserEntity("Banned User", "banned@nagrivic.org", null);
        disabledUser.setId(UUID.randomUUID());
        disabledUser.setActive(false);

        UserAuthIdentityEntity identity = new UserAuthIdentityEntity(
                disabledUser,
                "GOOGLE",
                "google-sub-disabled",
                "banned@nagrivic.org"
        );

        when(googleTokenVerifier.verify("banned-token")).thenReturn(claims);
        when(userAuthIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-disabled"))
                .thenReturn(Optional.of(identity));

        AuthException ex = assertThrows(AuthException.class, () ->
                authService.googleLogin(new GoogleAuthRequest("banned-token"), "127.0.0.1"));

        assertEquals(401, ex.getStatus().value());
        assertTrue(ex.getMessage().contains("deactivated") || ex.getMessage().contains("suspended"));
    }

    // 4. Rate limiting works
    @Test
    void shouldEnforceRateLimitingOnGoogleLogin() {
        doThrow(new RateLimitExceededException(45))
                .when(rateLimiter).checkLimit(eq("auth:google:192.168.1.100"), eq(10), any(Duration.class));

        assertThrows(RateLimitExceededException.class, () ->
                authService.googleLogin(new GoogleAuthRequest("token"), "192.168.1.100"));

        verifyNoInteractions(googleTokenVerifier);
        verifyNoInteractions(userRepository);
    }

    // 5. Invalid token rejected by verifier
    @Test
    void shouldRejectInvalidTokenWhenVerifierFails() {
        when(googleTokenVerifier.verify("invalid-token"))
                .thenThrow(AuthException.unauthorized("Invalid or expired Google ID token"));

        AuthException ex = assertThrows(AuthException.class, () ->
                authService.googleLogin(new GoogleAuthRequest("invalid-token"), "127.0.0.1"));

        assertEquals(401, ex.getStatus().value());
        verifyNoInteractions(userRepository);
    }
}
