package org.nagrivic.modules.auth.service;

import org.nagrivic.modules.auth.dto.GoogleUserClaims;

public interface GoogleTokenVerifier {

    /**
     * Independently validates a Google-issued ID token (cryptographic signature, issuer, audience, expiry, subject).
     *
     * @param idTokenString The raw Google ID token provided by the client
     * @return Verified Google user claims
     * @throws org.nagrivic.modules.auth.exception.AuthException if validation fails
     */
    GoogleUserClaims verify(String idTokenString);
}
