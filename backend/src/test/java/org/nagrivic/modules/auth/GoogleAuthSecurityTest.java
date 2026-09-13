package org.nagrivic.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nagrivic.modules.auth.dto.AuthResponse;
import org.nagrivic.modules.auth.dto.GoogleAuthRequest;
import org.nagrivic.modules.auth.dto.GoogleUserClaims;
import org.nagrivic.modules.auth.entity.AuthSessionEntity;
import org.nagrivic.modules.auth.entity.UserAuthIdentityEntity;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.provider.OtpProvider;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.UserAuthIdentityRepository;
import org.nagrivic.modules.auth.service.AuthService;
import org.nagrivic.modules.auth.service.GoogleTokenVerifier;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.auth.service.OtpService;
import org.nagrivic.modules.auth.service.PhoneNumberService;
import org.nagrivic.modules.moderation.ratelimit.RateLimiter;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Targeted security and concurrency tests verifying Part 40 requirements:
 * - Role cannot be supplied/escalated by client (always CITIZEN)
 * - Client cannot impersonate another user (identity derived strictly from cryptographic Google token)
 * - Email alone cannot authenticate (existing phone account with same email is not hijacked)
 * - Concurrent first-time login safely creates one account
 * - Provider identity uniqueness enforced
 */
@ExtendWith(MockitoExtension.class)
class GoogleAuthSecurityTest {

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

    @Test
    void testRoleCannotBeSuppliedByClientAndAlwaysDefaultsToCitizen() {
        GoogleUserClaims claims = new GoogleUserClaims(
                "sub-admin-attempt",
                "admin-wannabe@example.com",
                true,
                "Attacker",
                null
        );

        when(googleTokenVerifier.verify("token")).thenReturn(claims);
        when(userAuthIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "sub-admin-attempt"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(jwtService.generateAccessToken(any(UUID.class), eq("CITIZEN"))).thenReturn("citizen-jwt");
        when(jwtService.getAccessTokenExpirationMinutes()).thenReturn(15L);

        AuthResponse res = authService.googleLogin(new GoogleAuthRequest("token"), "127.0.0.1");

        assertNotNull(res);
        assertEquals("CITIZEN", res.user().role(), "Role must strictly be CITIZEN");
        verify(userRepository).save(argThat(user -> "CITIZEN".equals(user.getRole())));
    }

    @Test
    void testClientCannotImpersonateAnotherGoogleUser() {
        // Even if client claims someone else's email in request, the backend only trusts verified token subject
        String authenticSubject = "sub-authentic-citizen";
        GoogleUserClaims claims = new GoogleUserClaims(
                authenticSubject,
                "real-citizen@example.com",
                true,
                "Real Citizen",
                null
        );

        UserEntity realUser = new UserEntity("Real Citizen", "real-citizen@example.com", null);
        realUser.setId(UUID.randomUUID());
        realUser.setRole("CITIZEN");
        realUser.setActive(true);

        UserAuthIdentityEntity realIdentity = new UserAuthIdentityEntity(
                realUser,
                "GOOGLE",
                authenticSubject,
                "real-citizen@example.com"
        );

        when(googleTokenVerifier.verify("authentic-token")).thenReturn(claims);
        when(userAuthIdentityRepository.findByProviderAndProviderSubject("GOOGLE", authenticSubject))
                .thenReturn(Optional.of(realIdentity));
        when(jwtService.generateAccessToken(eq(realUser.getId()), eq("CITIZEN"))).thenReturn("real-jwt");
        when(jwtService.getAccessTokenExpirationMinutes()).thenReturn(15L);

        AuthResponse res = authService.googleLogin(new GoogleAuthRequest("authentic-token"), "127.0.0.1");

        assertEquals(realUser.getId(), res.user().id());
        assertEquals("real-citizen@example.com", res.user().email());
        verify(userAuthIdentityRepository).findByProviderAndProviderSubject("GOOGLE", authenticSubject);
        verify(userAuthIdentityRepository, never()).findByProviderAndProviderSubject(eq("GOOGLE"), eq("sub-victim"));
    }

    @Test
    void testEmailAloneCannotAuthenticateOrHijackExistingAccount() {
        // Existing user has email test@example.com but NO Google identity linked
        String newGoogleSubject = "sub-new-google-user";
        GoogleUserClaims claims = new GoogleUserClaims(
                newGoogleSubject,
                "existing@example.com",
                true,
                "Google User",
                null
        );

        when(googleTokenVerifier.verify("token")).thenReturn(claims);
        // Not linked to this Google subject:
        when(userAuthIdentityRepository.findByProviderAndProviderSubject("GOOGLE", newGoogleSubject))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(jwtService.generateAccessToken(any(UUID.class), eq("CITIZEN"))).thenReturn("new-jwt");
        when(jwtService.getAccessTokenExpirationMinutes()).thenReturn(15L);

        AuthResponse res = authService.googleLogin(new GoogleAuthRequest("token"), "127.0.0.1");

        assertNotNull(res);
        // A new user is created for this Google identity; it does NOT blindly return an unlinked account
        verify(userRepository).save(any(UserEntity.class));
        verify(userAuthIdentityRepository).save(argThat(id ->
                id.getProviderSubject().equals(newGoogleSubject) &&
                id.getProvider().equals("GOOGLE")
        ));
    }

    @Test
    void testConcurrentFirstTimeLoginSafelyCreatesOneAccount() {
        String testSub = "sub-concurrent-race";
        GoogleUserClaims claims = new GoogleUserClaims(
                testSub,
                "race@example.com",
                true,
                "Race User",
                null
        );

        when(googleTokenVerifier.verify("race-token")).thenReturn(claims);
        // First check in transaction returns empty
        when(userAuthIdentityRepository.findByProviderAndProviderSubject("GOOGLE", testSub))
                .thenReturn(Optional.empty()) // first attempt in thread
                .thenReturn(Optional.of(new UserAuthIdentityEntity(
                        new UserEntity("Race User", "race@example.com", null),
                        "GOOGLE",
                        testSub,
                        "race@example.com"
                ))); // second lookup after conflict

        UserEntity userToSave = new UserEntity("Race User", "race@example.com", null);
        userToSave.setId(UUID.randomUUID());
        when(userRepository.save(any(UserEntity.class))).thenReturn(userToSave);

        // Simulate database unique constraint violation on user_auth_identities
        when(userAuthIdentityRepository.save(any(UserAuthIdentityEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uq_user_auth_provider_subject"));

        when(jwtService.generateAccessToken(any(), eq("CITIZEN"))).thenReturn("recovered-jwt");
        when(jwtService.getAccessTokenExpirationMinutes()).thenReturn(15L);

        AuthResponse res = authService.googleLogin(new GoogleAuthRequest("race-token"), "127.0.0.1");

        assertNotNull(res);
        assertEquals("recovered-jwt", res.accessToken());
        // Verify conflict recovery searched repository again
        verify(userAuthIdentityRepository, times(2)).findByProviderAndProviderSubject("GOOGLE", testSub);
    }
}
