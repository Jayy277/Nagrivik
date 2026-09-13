package org.nagrivic.modules.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nagrivic.modules.auth.dto.GoogleUserClaims;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.GoogleTokenVerifierService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierServiceTest {

    @Mock
    private GoogleIdTokenVerifier mockVerifier;

    private GoogleTokenVerifierService service;

    private static final String ALLOWED_CLIENT_ID = "allowed-client-id-123";

    @BeforeEach
    void setUp() {
        service = new GoogleTokenVerifierService(mockVerifier, List.of(ALLOWED_CLIENT_ID));
    }

    @Test
    void testBlankTokenThrowsBadRequest() {
        AuthException ex1 = assertThrows(AuthException.class, () -> service.verify(null));
        assertEquals(400, ex1.getStatus().value());

        AuthException ex2 = assertThrows(AuthException.class, () -> service.verify("   "));
        assertEquals(400, ex2.getStatus().value());
    }

    @Test
    void testExpiredOrInvalidSignatureTokenReturnsUnauthorized() throws GeneralSecurityException, IOException {
        when(mockVerifier.verify("expired-or-invalid-sig")).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class, () -> service.verify("expired-or-invalid-sig"));
        assertEquals(401, ex.getStatus().value());
        assertTrue(ex.getMessage().contains("Invalid, expired, or untrusted"));
    }

    @Test
    void testUntrustedIssuerRejected() throws GeneralSecurityException, IOException {
        GoogleIdToken mockToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setIssuer("https://malicious-issuer.com");
        payload.setAudience(ALLOWED_CLIENT_ID);
        payload.setSubject("sub-123");

        when(mockToken.getPayload()).thenReturn(payload);
        when(mockVerifier.verify("untrusted-issuer-token")).thenReturn(mockToken);

        AuthException ex = assertThrows(AuthException.class, () -> service.verify("untrusted-issuer-token"));
        assertEquals(401, ex.getStatus().value());
        assertTrue(ex.getMessage().contains("Untrusted Google token issuer"));
    }

    @Test
    void testWrongAudienceRejected() throws GeneralSecurityException, IOException {
        GoogleIdToken mockToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setIssuer("https://accounts.google.com");
        payload.setAudience("unauthorized-client-id");
        payload.setSubject("sub-123");

        when(mockToken.getPayload()).thenReturn(payload);
        when(mockVerifier.verify("wrong-audience-token")).thenReturn(mockToken);

        AuthException ex = assertThrows(AuthException.class, () -> service.verify("wrong-audience-token"));
        assertEquals(401, ex.getStatus().value());
        assertTrue(ex.getMessage().contains("audience is not authorized"));
    }

    @Test
    void testMissingSubjectRejected() throws GeneralSecurityException, IOException {
        GoogleIdToken mockToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setIssuer("https://accounts.google.com");
        payload.setAudience(ALLOWED_CLIENT_ID);
        payload.setSubject(null); // missing sub

        when(mockToken.getPayload()).thenReturn(payload);
        when(mockVerifier.verify("missing-sub-token")).thenReturn(mockToken);

        AuthException ex = assertThrows(AuthException.class, () -> service.verify("missing-sub-token"));
        assertEquals(401, ex.getStatus().value());
        assertTrue(ex.getMessage().contains("missing immutable subject claim"));
    }

    @Test
    void testValidTokenExtractsClaimsCorrectly() throws GeneralSecurityException, IOException {
        GoogleIdToken mockToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setIssuer("accounts.google.com");
        payload.setAudience(ALLOWED_CLIENT_ID);
        payload.setSubject("verified-sub-456");
        payload.setEmail("Citizen@Nagrivic.org");
        payload.setEmailVerified(true);
        payload.set("name", "Diya Sharma");
        payload.set("picture", "https://profile.google.com/diya.jpg");

        when(mockToken.getPayload()).thenReturn(payload);
        when(mockVerifier.verify("valid-token")).thenReturn(mockToken);

        GoogleUserClaims claims = service.verify("valid-token");

        assertNotNull(claims);
        assertEquals("verified-sub-456", claims.subject());
        assertEquals("citizen@nagrivic.org", claims.email()); // lowercased
        assertTrue(claims.emailVerified());
        assertEquals("Diya Sharma", claims.name());
        assertEquals("https://profile.google.com/diya.jpg", claims.pictureUrl());
    }
}
