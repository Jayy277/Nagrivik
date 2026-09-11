# Nagrivic Security Architecture & Principles

> **Notice**: This document outlines security principles and architectural requirements for Nagrivic. Authentication, authorization, and rate limiting mechanisms described here are design specifications and must not be implemented until their designated security implementation phases.

---

## 1. Core Security Principles

1. **Zero Hardcoded Secrets**:
   - Secrets, database credentials, API keys, and signing keys must never be committed to Git.
   - All sensitive configurations must be injected via environment variables or cloud secret managers.
   - `.env` files are strictly excluded via `.gitignore`. A `.env.example` file must provide placeholder documentation only.

2. **Defense in Depth & Input Validation**:
   - Never trust input from the client (mobile app or web).
   - Validate all user input both on the client (for user experience) and strictly on the Spring Boot backend using Jakarta Bean Validation (`@Valid`).
   - Sanitize all text fields before persistence to prevent XSS and SQL injection.

3. **Least Privilege Principle**:
   - The backend database user must only possess permissions required for its schema (no superuser access).
   - Internal API endpoints enforce role-based access control (RBAC), granting users only the minimum permissions necessary for their role.

---

## 2. Authentication & Authorization Roadmap

### Authentication (Future Phase)
- **Citizens (Mobile & Web)**:
  - Mobile OTP (One-Time Password) via SMS to verify phone numbers.
  - Zero password storage for citizen accounts.
  - Stateless JSON Web Tokens (JWT) issued upon successful OTP verification with standard expiration and refresh mechanisms.
- **Municipal Authorities & Admins**:
  - Secure credential login with bcrypt-hashed passwords.
  - Multi-Factor Authentication (MFA) required for admin and municipal department officer logins.

### Role-Based Access Control (RBAC)
The platform defines three primary role tiers:

| Role | Permissions |
|---|---|
| `ROLE_CITIZEN` | Report issues, upload photos, upvote ("Support") issues, post comments, confirm resolution. |
| `ROLE_OFFICER` | View assigned department queue, update ticket progress, upload resolution proof photos, mark resolved. |
| `ROLE_ADMIN` | Moderate content, reassign wards, manage authority users, view city-wide audit logs. |

---

## 3. Privacy & Citizen Protection

Civic reporting must safeguard citizen safety and privacy:

- **PII Redaction on Public Feeds**:
  - Public issue detail pages and maps must **never** expose citizen phone numbers, email addresses, or full real names without explicit user opt-in.
  - Display citizen reports anonymously or with masked initials (e.g., `"Reported by Citizen A.K."`).
- **Location Precision vs. Privacy**:
  - Public issues display the location of the civic hazard.
  - User profile screens must never reveal private citizen home locations or movement history.
- **EXIF Metadata Scrubbing**:
  - Strip camera device serial numbers, personal metadata, and embedded EXIF data during the media upload pipeline before storing publicly accessible image URLs.

---

## 4. Rate Limiting & Abuse Prevention

To protect municipal operations from spam and denial of service:

1. **OTP Endpoints**:
   - Rate limit OTP generation requests by phone number and IP address (e.g., maximum 3 attempts per 10 minutes) to prevent SMS toll fraud.
2. **Issue Creation Throttle**:
   - Enforce cooldown periods between issue submissions per device/user to block scripted flood reporting.
3. **Upvoting / Support Throttling**:
   - Limit support toggles to one vote per user per issue; prevent rapid vote manipulation.

---

## 5. Media & File Upload Security

When media uploads are introduced in future tasks:
- **File Type Sniffing**: Inspect MIME magic bytes on the server rather than trusting user-provided file extensions. Only allow standard image formats (`image/jpeg`, `image/png`, `image/webp`).
- **File Size Limits**: Cap image uploads at 10 MB per photo and 50 MB per video.
- **Direct-to-Storage via Pre-signed URLs**: Clients upload media directly to S3-compatible cloud storage via short-lived pre-signed URLs, preventing denial-of-service on Spring Boot application servers.

---

## 6. Audit Logging & Administrative Accountability

To maintain public trust and civic integrity:
- **Audit Trails**: All status transitions (e.g., marking a pothole as `"RESOLVED"` or reassigning an issue to another department) must record:
  - Timestamp of action
  - Actor identity (User ID and role)
  - Previous status and new status
  - Optional official resolution remarks
- **Immutable History**: Issue activity logs must be append-only and cannot be altered or deleted.
