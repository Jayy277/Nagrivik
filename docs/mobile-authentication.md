# Nagrivic Mobile Authentication Foundation

## 1. Overview & Architectural Philosophy

Nagrivic employs a secure, citizen-centric authentication architecture built on **Google Sign-In** as the primary authentication mechanism and **Nagrivic JWT access/refresh token sessions**.

> [!IMPORTANT]
> **Authoritative Backend Rule**:
> Backend authentication remains authoritative; mobile validation and token handling is only for user experience. The backend independently verifies Google ID tokens with Google's API client.
>
> **Credential Protection Policy**:
> Access and refresh tokens are device credentials and must never be logged or exposed to UI components. They are stored exclusively in hardware-backed `expo-secure-store`.

---

## 2. Authentication Flow (Google Sign-In)

```text
Citizen Launches App
        ↓
[Check Secure Storage]
        ├── Tokens found → Validate session (GET /api/auth/me) → [Home Feed]
        └── None found  → [Welcome Screen]
                                ↓
                    [Continue with Google]
                    (Google brand guidelines)
                                ↓
                    Interactive Google Auth (WebBrowser)
                    (Redirect: nagrivic://auth/callback)
                                ↓
                    Receive Google ID Token
                                ↓
                    POST /api/auth/google
                    {"idToken": "<Google ID Token>"}
                                ↓
                    AuthResponse
                    (accessToken, refreshToken, user)
                                ↓
                    [Secure Token Storage (SecureStore)]
                                ↓
                    [Target Destination / returnTo]
```

---

## 3. API Endpoints

All authentication endpoints are located under `/api/auth`:

| Method | Endpoint | Description | Request Body | Response Payload |
|---|---|---|---|---|
| `POST` | `/api/auth/google` | Primary Google ID token verification & auto-provisioning | `{ "idToken": string }` | `{ "accessToken": string, "refreshToken": string, "tokenType": "Bearer", "expiresIn": number, "user": UserAuthSummary }` |
| `POST` | `/api/auth/refresh` | Rotate expired access token | `{ "refreshToken": string }` | `{ "accessToken": string, "refreshToken": string, "tokenType": "Bearer", "expiresIn": number, "user": UserAuthSummary }` |
| `POST` | `/api/auth/logout` | Revoke active session on server | `{ "refreshToken": string }` | `{ "message": "Logged out successfully" }` |
| `GET` | `/api/auth/me` | Fetch authenticated citizen | None (`Authorization: Bearer <token>`) | `{ "id": UUID, "email": string, "role": string, "fullName": string, "profilePictureUrl": string }` |
| `POST` | `/api/auth/send-otp` | (Secondary/Test) Generate & dispatch SMS OTP | `{ "phoneNumber": "+91XXXXXXXXXX" }` | `{ "message": string, "expiresInSeconds": number }` |
| `POST` | `/api/auth/verify-otp` | (Secondary/Test) Verify 6-digit OTP code | `{ "phoneNumber": "+91XXXXXXXXXX", "otp": "XXXXXX" }` | `{ "accessToken": string, "refreshToken": string, "tokenType": "Bearer", "expiresIn": number, "user": UserAuthSummary }` |

---

## 4. API Base URL & Physical Device Configuration

The API client reads configuration dynamically:
1. `EXPO_PUBLIC_API_BASE_URL` (preferred) or `EXPO_PUBLIC_API_URL`
2. **Android Emulator**: Defaults to `http://10.0.2.2:8080` (maps to development host `localhost:8080`).
3. **iOS Simulator & Web**: Defaults to `http://localhost:8080`.

### Physical Device Setup
On a physical smartphone connected to the same Wi-Fi network, `localhost` refers to the phone itself, not the backend computer. To connect a physical phone:
```bash
# Set your development computer's local LAN IP in .env or shell:
EXPO_PUBLIC_API_BASE_URL=http://192.168.1.100:8080
```

---

## 5. Token Storage Abstraction (`src/services/storage/tokenStorage.ts`)

- **Native Platforms (iOS / Android)**: Backed by `expo-secure-store`, leveraging iOS Keychain and Android Keystore hardware-backed encryption.
- **Web Browser**: Backed by secure session/memory storage fallback.
- **Interface**:
  - `saveTokens(accessToken: string, refreshToken: string): Promise<void>`
  - `getAccessToken(): Promise<string | null>`
  - `getRefreshToken(): Promise<string | null>`
  - `clearTokens(): Promise<void>`

---

## 6. Authentication State Machine (`src/context/AuthContext.tsx`)

- `UNKNOWN`: Initializing app and reading secure credentials. A neutral splash indicator is rendered.
- `UNAUTHENTICATED`: No active session exists. Redirected to `/(auth)/welcome`.
- `AUTHENTICATED`: Active session verified against server. Citizen profile is available.

---

## 7. Protected Routes & Redirection

Using Expo Router file-based layout guards:
- `src/app/index.tsx`: Initial router redirecting based on `AuthStatus`.
- `src/app/(auth)/_layout.tsx`: Guards auth screens; if a citizen is already authenticated, they are automatically forwarded to `/(tabs)`.
- `src/app/(tabs)/_layout.tsx`: Guards application tabs; if a citizen is unauthenticated, they are redirected to `/(auth)/welcome`.

---

## 8. Automated Token Refresh & Concurrent Request Queue

1. When any authenticated API call encounters a `401 Unauthorized`:
   - The request is paused.
   - The client invokes `executeTokenRefresh()`.
2. **Concurrency Lock**: If multiple parallel requests trigger 401 at the same time, they share a single `refreshPromise`. Only ONE network call to `/api/auth/refresh` is dispatched.
3. **Rotation**: The backend generates a new refresh token and invalidates the previous token. The client immediately updates storage via `tokenStorage.saveTokens()`.
4. **Single-Retry**: The original request is retried once with the new Bearer token.
5. **Terminal Failure**: If refresh fails or token is revoked, stored credentials are wiped and the user state transitions to `UNAUTHENTICATED`.

---

## 9. Logout Behavior

- Invokes `POST /api/auth/logout` with the active refresh token to revoke the session in the backend database.
- **Offline Resilient**: Local storage (`clearTokens()`) and auth state reset are guaranteed to execute even if the backend is unreachable or offline.

---

## 10. Security & Privacy Rules

- **Zero Credential Logging**: Access tokens, refresh tokens, OTP codes, and `Authorization` headers are strictly excluded from logs.
- **Masked Citizen Contact**: Phone numbers displayed in public or semi-public UI use safe masking (`+91 98765 •••••`).
- **No Client Secrets**: No API secrets or passwords are embedded in mobile bundle code.
