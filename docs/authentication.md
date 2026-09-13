# Nagrivic Authentication Architecture (Task 33)

This document provides a comprehensive technical reference for the primary **Google Sign-In / Sign-Up Authentication** system across the **React Native + Expo mobile app**, **Next.js public web app**, and **Java + Spring Boot backend**.

---

## 1. Architectural Overview

Nagrivic uses **Google Sign-In** as its primary citizen authentication mechanism.

### Key Principles:
1. **Zero Client Trust**: Google profile details (`email`, `name`, `picture`, `role`) sent directly in client payloads are strictly untrusted. The backend derives authoritative identity exclusively by cryptographically verifying Google ID tokens using the official Google API Client SDK.
2. **Immutable Subject Mapping**: Google accounts are identified by Google's permanent, immutable subject identifier (`sub` claim), stored in `user_auth_identities (provider, provider_subject)`. We do **not** use email alone as the primary identity key, protecting accounts against email mutation or hijacking.
3. **Nagrivic Own Session System**: Google tokens are **never** treated as Nagrivic API credentials. Upon Google verification, Nagrivic issues its own stateless JWT access tokens (15-minute expiry) and cryptographically hashed, rotatable refresh tokens (30-day expiry).
4. **Zero Passwords / Zero Form Registration**: First-time Google users are automatically provisioned as verified `CITIZEN` accounts. There is no separate signup form, no password creation, and no mandatory SMS OTP for this flow.
5. **Preserved Secondary Phone OTP**: The pre-existing phone OTP backend services (`OtpService`, `DevOtpProvider`) remain available in the backend for automated regression tests and development fallback, but are deferred from primary citizen UI.

---

## 2. Authentication Data Model

Flyway migration: `V25__create_user_auth_identities_and_make_phone_nullable.sql`

### `user_auth_identities` Table

```sql
CREATE TABLE IF NOT EXISTS user_auth_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_auth_identities_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_auth_provider_subject UNIQUE (provider, provider_subject)
);

CREATE INDEX idx_user_auth_identities_user_id ON user_auth_identities(user_id);
CREATE INDEX idx_user_auth_identities_provider_subject ON user_auth_identities(provider, provider_subject);
```

### `users` Table Modifications:
- `phone_number`: Made nullable (`ALTER COLUMN phone_number DROP NOT NULL`)
- `email`: `VARCHAR(255)` with index `idx_users_email`
- `profile_picture_url`: `VARCHAR(1024)`

---

## 3. Authentication Flows

### A. Mobile Flow (React Native + Expo)

```
[Citizen]
   │
   ▼
[Welcome / Action Guard Screen]
   │ Taps "Continue with Google"
   ▼
[WebBrowser.openAuthSessionAsync] ──► Google OAuth 2.0 Consent Screen
   │                                  (openid profile email)
   ▼
[Google returns ID Token]
   │
   ▼
[POST /api/auth/google] ────────────► [Spring Boot Backend]
   │  Payload: { idToken }                │
   │                                      ├─ 1. Rate limiter check
   │                                      ├─ 2. GoogleIdTokenVerifier:
   │                                      │     - signature & expiry
   │                                      │     - issuer in accounts.google.com
   │                                      │     - aud in allowed client IDs
   │                                      │     - immutable sub claim
   │                                      ├─ 3. Find or auto-provision CITIZEN
   │                                      ├─ 4. Issue Nagrivic JWT + Refresh Token
   │                                      └─ 5. Return AuthResponse
   ◄──────────────────────────────────────┘
   │
   ▼
[SecureStore] Stores accessToken & refreshToken
[AuthContext] Updates state: status = 'AUTHENTICATED', user = { id, fullName, email, role: 'CITIZEN' }
[Navigation] Navigates to app or returns to intended issue action
```

### B. Web Flow (Next.js Public Web Application)

```
[Browser]                                    [Next.js Server]                     [Spring Boot Backend]
   │                                                │                                       │
   ├─ Taps "Continue with Google" ─────────────────►│                                       │
   │                                                ├─ GET /api/auth/google/login           │
   │                                                │  - Generate CSRF state & nonce        │
   │                                                │  - Set HttpOnly nagrivic_oauth_state  │
   │                                                │  - Redirect 302 to Google OAuth       │
   │◄───────────────────────────────────────────────┤                                       │
   │                                                │                                       │
   ├─ Google consent & user selection ─────────────►│                                       │
   │                                                │                                       │
   ├─ Google redirects to /api/auth/google/callback►│                                       │
   │                                                ├─ Validate state vs HttpOnly cookie    │
   │                                                ├─ Exchange code for id_token           │
   │                                                ├─ POST /api/auth/google ──────────────►│
   │                                                │   { idToken }                         ├─ Verify ID token
   │                                                │                                       ├─ Issue Nagrivic JWT
   │                                                │◄── AuthResponse ──────────────────────┤
   │                                                ├─ Set HttpOnly cookies:                │
   │                                                │   - nagrivic_access_token             │
   │                                                │   - nagrivic_refresh_token            │
   │                                                │   - nagrivic_user                     │
   │                                                ├─ Redirect to returnUrl                │
   │◄───────────────────────────────────────────────┤                                       │
   │                                                │                                       │
   ├─ Subsequent Actions (Support, Comment) ───────►├─ Proxy /api/web/issues/... ──────────►│
   │  (Browser sends HttpOnly cookies automatically)│   Authorization: Bearer <JWT>         ├─ Authorize & execute
```

---

## 4. Backend Cryptographic Verification

Implemented in:
- `GoogleTokenVerifierService.java`
- `AuthService.java`

Verification criteria:
1. **Signature**: Cryptographically verified against Google's public keys fetched over HTTPS using `GoogleNetHttpTransport.newTrustedTransport()`.
2. **Issuer**: Must be exactly `"accounts.google.com"` or `"https://accounts.google.com"`.
3. **Audience**: Must match one of the configured Client IDs in `nagrivic.auth.google.client-ids`.
4. **Expiry**: Token expiration timestamp must be in the future.
5. **Subject**: Must contain a non-blank, immutable subject string.
6. **Role Guard**: Role is **never** accepted from client requests or claims; always defaulted to `"CITIZEN"`.
7. **Active Check**: If `user.isActive() == false`, session issuance is denied with 401 Unauthorized.

---

## 5. Account Creation & Race-Condition Safety

When a verified Google user logs in for the first time:
```java
try {
    UserEntity newUser = new UserEntity(claims.name(), claims.email(), claims.pictureUrl());
    newUser.setRole("CITIZEN");
    newUser.setActive(true);
    user = userRepository.save(newUser);

    UserAuthIdentityEntity newIdentity = new UserAuthIdentityEntity(
            user, "GOOGLE", providerSubject, claims.email()
    );
    userAuthIdentityRepository.save(newIdentity);
} catch (DataIntegrityViolationException e) {
    // Concurrent first-time login for the same Google account: recover existing record
    UserAuthIdentityEntity existing = userAuthIdentityRepository
            .findByProviderAndProviderSubject("GOOGLE", providerSubject)
            .orElseThrow(() -> e);
    user = existing.getUser();
}
```
This guarantees that concurrent simultaneous first-time login requests for the same Google account safely resolve to **one** user and **one** identity.

---

## 6. Email Matching Safety (No Unsafe Merging)

Nagrivic **never** automatically merges accounts based on email alone.
If an existing account exists with phone verification and email `priya@example.com`, a new Google login with `priya@example.com` will **not** hijack the phone account. It safely provisions a distinct Google identity and account unless an explicit, authenticated account-linking workflow is performed.

---

## 7. Protected Routes Matrix

| Platform | Screen / Action | Public / Protected | Behavior When Unauthenticated |
|---|---|---|---|
| **Mobile** | Home Feed (`(tabs)/index.tsx`) | Public | Browsable freely |
| **Mobile** | Nearby Map (`(tabs)/nearby.tsx`) | Public | Browsable freely |
| **Mobile** | Issue Detail (`/issue/[id]`) | Public | Browsable freely |
| **Mobile** | Support Action | Protected | Prompts "Continue with Google" -> completes action |
| **Mobile** | Comment Action | Protected | Prompts "Continue with Google" -> completes action |
| **Mobile** | Report Issue (`(tabs)/report.tsx`) | Protected | Prompts "Continue with Google" |
| **Mobile** | Updates (`(tabs)/notifications.tsx`)| Protected | Prompts "Continue with Google" |
| **Mobile** | Profile (`(tabs)/profile.tsx`) | Protected | Prompts "Continue with Google" |
| **Web** | Home (`/`) | Public | Browsable freely |
| **Web** | Explore Issues (`/issues`) | Public | Browsable freely |
| **Web** | Civic Map (`/map`) | Public | Browsable freely |
| **Web** | Issue Detail (`/issues/[id]`) | Public | Browsable freely |
| **Web** | Support Action | Protected | Redirects to `/login?returnUrl=...` -> completes |
| **Web** | Comment Action | Protected | Shows "Continue with Google" banner |
| **Web** | Profile (`/profile`) | Protected | Shows "Sign In Required" with Google button |

---

## 8. Google Cloud Console Setup Guide

To configure Google OAuth outside the codebase:

### 1. Google Cloud Project Setup
1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select project: `nagrivic-civic-platform`.

### 2. OAuth Consent Screen
1. Navigate to **APIs & Services** > **OAuth consent screen**.
2. Select User Type: **External**.
3. App Name: `Nagrivic`
4. User support email: civic-support@nagrivic.org
5. Authorized domains: `nagrivic.org`
6. Scopes requested:
   - `.../auth/userinfo.email`
   - `.../auth/userinfo.profile`
   - `openid`
   *(Do NOT request Gmail, Drive, Calendar, or Contacts scopes)*

### 3. Create Credentials
Navigate to **APIs & Services** > **Credentials** > **Create Credentials** > **OAuth client ID**:

1. **Web Client ID (for Next.js Web App)**:
   - Application type: Web application
   - Name: `Nagrivic Web Client`
   - Authorized JavaScript origins:
     - Development: `http://localhost:3000`
     - Production: `https://nagrivic.org`
   - Authorized redirect URIs:
     - Development: `http://localhost:3000/api/auth/google/callback`
     - Production: `https://nagrivic.org/api/auth/google/callback`
   - Save `Client ID` and `Client Secret` (keep secret strictly server-side).

2. **Android Client ID (for Expo Mobile App)**:
   - Application type: Android
   - Package name: `org.nagrivic.app`
   - SHA-1 certificate fingerprint of your release/debug keystore.

3. **iOS Client ID (for Expo Mobile App)**:
   - Application type: iOS
   - Bundle ID: `org.nagrivic.app`

---

## 9. Environment Variables

### Backend (`backend/src/main/resources/application.yml`)
```bash
# Allowlist of accepted client IDs across Web, Android, and iOS
GOOGLE_CLIENT_IDS=nagrivic-dev-web-client-id,nagrivic-dev-android-client-id,nagrivic-dev-ios-client-id
```

### Web (`web/.env.local`)
```bash
# Server-side (never exposed to browser)
GOOGLE_WEB_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
GOOGLE_WEB_CLIENT_SECRET=your-google-web-client-secret
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
```

### Mobile (`NagrivicApp/.env`)
```bash
EXPO_PUBLIC_GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
```
