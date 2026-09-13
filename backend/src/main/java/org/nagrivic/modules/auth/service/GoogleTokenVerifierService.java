package org.nagrivic.modules.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.nagrivic.modules.auth.dto.GoogleUserClaims;
import org.nagrivic.modules.auth.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoogleTokenVerifierService implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);
    private static final List<String> TRUSTED_ISSUERS = List.of("accounts.google.com", "https://accounts.google.com");

    private final GoogleIdTokenVerifier verifier;
    private final List<String> allowedClientIds;

    @org.springframework.beans.factory.annotation.Autowired
    public GoogleTokenVerifierService(
            @Value("${nagrivic.auth.google.client-ids:}") String clientIdsConfig
    ) {
        this.allowedClientIds = parseClientIds(clientIdsConfig);
        try {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance()
            )
            .setAudience(this.allowedClientIds.isEmpty() ? Collections.singletonList("dummy-audience") : this.allowedClientIds)
            .build();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to initialize GoogleIdTokenVerifier HTTP transport", e);
            throw new RuntimeException("Could not initialize Google token verifier", e);
        }
    }

    /**
     * Constructor for testing or explicit verifier injection.
     */
    public GoogleTokenVerifierService(GoogleIdTokenVerifier verifier, List<String> allowedClientIds) {
        this.verifier = verifier;
        this.allowedClientIds = allowedClientIds != null ? allowedClientIds : Collections.emptyList();
    }

    @Override
    public GoogleUserClaims verify(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw AuthException.badRequest("Google ID token must not be blank");
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("Google token verification returned null (invalid signature, expired, or untrusted issuer/audience)");
                throw AuthException.unauthorized("Invalid, expired, or untrusted Google credential");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (payload == null) {
                throw AuthException.unauthorized("Malformed Google credential: empty payload");
            }

            // 1. Verify Issuer
            String issuer = payload.getIssuer();
            if (issuer == null || !TRUSTED_ISSUERS.contains(issuer)) {
                log.warn("Rejected Google credential with untrusted issuer: {}", issuer);
                throw AuthException.unauthorized("Untrusted Google token issuer");
            }

            // 2. Verify Audience against explicit allowlist
            Object audObj = payload.getAudience();
            boolean audienceAllowed = false;
            if (allowedClientIds.isEmpty()) {
                audienceAllowed = true;
            } else if (audObj instanceof String audStr) {
                audienceAllowed = allowedClientIds.contains(audStr);
            } else if (audObj instanceof List<?> audList) {
                audienceAllowed = audList.stream().anyMatch(a -> allowedClientIds.contains(String.valueOf(a)));
            }

            if (!audienceAllowed) {
                log.warn("Rejected Google credential with unallowed audience: {}", audObj);
                throw AuthException.unauthorized("Google token audience is not authorized for Nagrivic");
            }

            // 3. Verify Subject Claim
            String subject = payload.getSubject();
            if (subject == null || subject.isBlank()) {
                log.warn("Rejected Google credential without subject claim");
                throw AuthException.unauthorized("Google token missing immutable subject claim");
            }

            // 4. Extract Safe Claims
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            return new GoogleUserClaims(
                    subject.trim(),
                    email != null ? email.trim().toLowerCase() : null,
                    emailVerified,
                    name != null ? name.trim() : null,
                    picture != null ? picture.trim() : null
            );

        } catch (AuthException ae) {
            throw ae;
        } catch (GeneralSecurityException | IOException e) {
            log.error("Cryptographic error or I/O failure during Google token verification: {}", e.getMessage());
            throw AuthException.unauthorized("Unable to verify Google credential: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Malformed JWT credential syntax: {}", e.getMessage());
            throw AuthException.badRequest("Malformed Google credential format");
        }
    }

    private static List<String> parseClientIds(String config) {
        if (config == null || config.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
