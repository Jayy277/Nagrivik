# Object Storage Foundation (S3-Compatible)

## 1. Domain Purpose & Philosophy

Nagrivic is a civic issue reporting platform handling thousands of citizen-submitted photos, resolution evidence images, and visual inspection data. 

Storing binary objects directly in PostgreSQL bloats database backups, increases connection memory, and degrades database query performance. Conversely, relying solely on local server filesystems introduces single-point-of-failure risks and prevents horizontal scaling.

Task 49 establishes a **production-ready, provider-independent object storage abstraction** compatible with standard S3-style object storage (AWS S3, MinIO, Cloudflare R2, LocalStack), while preserving a seamless local filesystem implementation for development and offline testing.

```
                      Client Upload / Retrieval
                                  │
                                  ▼
                     Application Controllers
       (IssueMediaController, MediaDownloadController, etc.)
                                  │
                                  ▼
               MediaService / ResolutionEvidenceService
                                  │
                                  ▼
                    ObjectStorageService (Interface)
                                  │
                 ┌────────────────┴────────────────┐
                 ▼                                 ▼
   LocalMediaStorageService            S3ObjectStorageService
   (STORAGE_PROVIDER=local)            (STORAGE_PROVIDER=s3)
   - Sandboxed root location           - AWS SDK for Java v2
   - Zero cloud credentials            - AWS S3, MinIO, Cloudflare R2
   - Path traversal defense            - Path-style access support
   - Dev & test environments           - Private bucket enforcement
```

---

## 2. Core Non-Negotiable Invariants

> [!IMPORTANT]
> **Object Storage Architectural Rules**:
> 1. **Provider Independence**: Application and domain layers depend strictly on `ObjectStorageService` / `MediaStorageService`. Domain code never imports or references `software.amazon.awssdk`, MinIO, or S3-specific classes.
> 2. **Default Local Storage**: In development and automated tests, `STORAGE_PROVIDER=local` is the default. Zero AWS credentials or external MinIO instances are required for `./gradlew.bat test` or local backend startup.
> 3. **Private Buckets by Default**: Storage buckets remain private. Nagrivic never exposes direct permanent S3 URLs to citizens, requires public bucket ACLs, or leaks cloud credentials.
> 4. **Controlled Backend Streaming**: Public media display occurs via authenticated or database-verified endpoints (`/api/issues/{id}/media/{mediaId}` and `/api/media/**`) with strict database verification, path-traversal prevention, and defensive HTTP headers.
> 5. **Two-System Consistency**: File storage precedes database metadata persistence. If database persistence fails, compensation cleanup (`delete(storageKey)`) is triggered to prevent orphaned storage objects.
> 6. **Authoritative Server Keys**: Storage keys are 100% server-generated (e.g. `issues/{id}/{uuid}.{ext}` or `resolution-evidence/{id}/{uuid}.{ext}`). User-supplied filenames are never used as storage keys.
> 7. **Media Constraints**: Enforces 10 MB strict file size limit and allowed MIME types (`image/jpeg`, `image/png`, `image/webp`).

---

## 3. Storage Abstraction Architecture

### 3.1 Interface: `ObjectStorageService`

Located in `org.nagrivic.modules.media.storage`:

```java
public interface ObjectStorageService {
    void store(String storageKey, InputStream inputStream, long contentLength, String contentType);
    InputStream load(String storageKey);
    void delete(String storageKey);
    boolean exists(String storageKey);
    StorageMetadata getMetadata(String storageKey);
    default String generatePresignedReadUrl(String storageKey, Duration expiration) { return null; }
}
```

The existing `MediaStorageService` extends `ObjectStorageService`, providing 100% backwards compatibility with all existing domain services.

### 3.2 Implementations

1. **`LocalMediaStorageService`**:
   - Stores files under a configured sandboxed directory (default: `build/uploads`).
   - Normalizes keys and verifies `resolvedPath.startsWith(rootLocation)` to reject directory traversal (`..`, leading slashes).
   - Generates `StorageMetadata` using `java.nio.file.Files`.
   - Throws `StorageFileNotFoundException` if missing and `SecurityException` on traversal.

2. **`S3ObjectStorageService`**:
   - Uses AWS SDK for Java v2 (`software.amazon.awssdk:s3:2.29.50`) with `UrlConnectionHttpClient` for minimal memory overhead.
   - Supports custom endpoints (e.g., `http://localhost:9000` for MinIO or Cloudflare R2 endpoints).
   - Supports path-style access (`STORAGE_S3_PATH_STYLE_ACCESS=true`) required for local MinIO deployments.
   - Uploads via streaming `RequestBody.fromInputStream(inputStream, contentLength)`.
   - Streams downloads via `ResponseInputStream<GetObjectResponse>`.
   - Maps S3 exceptions safely (`NoSuchKeyException` -> `StorageFileNotFoundException`, 5xx/network -> `StorageUnavailableException`).

### 3.3 Factory Configuration: `StorageConfiguration`

The Spring configuration class `StorageConfiguration` registers the active `@Primary` `MediaStorageService` bean based on `nagrivic.storage.provider`. At startup, it logs the active provider and target bucket/directory without leaking secrets or credentials.

---

## 4. Two-System Consistency & Failure Cleanup

Uploading media involves two systems:
1. Object Storage (binary payload)
2. PostgreSQL (metadata record)

Nagrivic implements compensation cleanup on failure:

```
[Client Upload]
       │
       ▼
1. Validate MIME & Magic Bytes (JPEG/PNG/WebP, ≤10MB)
       │
       ▼
2. Generate Server Storage Key (e.g. issues/{id}/{uuid}.jpg)
       │
       ▼
3. Store Object in ObjectStorageService
       │
       ▼
4. Persist MediaEntity / ResolutionEvidenceEntity in PostgreSQL
       │
  ┌────┴────┐
  ▼         ▼
[Success] [Failure (DB Exception)]
            │
            ▼
          Compensate: objectStorageService.delete(storageKey)
            │
            ▼
          Rethrow Domain Exception
```

This prevents dangling, untracked binary files from accumulating in cloud buckets when database transactions fail or rollback.

---

## 5. Controlled Media Download & Streaming Security

Public access to issue photos and resolution evidence is strictly governed:

### 5.1 Endpoint: `GET /api/media/{*storageKey}`
- **Database Record Verification**: Before reading from storage, the server verifies that `storageKey` is recorded in either `media.storage_key` or `resolution_evidence.storage_key`.
- **Arbitrary Download Defense**: Requests for unregistered keys, test files, or unpersisted paths are immediately rejected with `404 Not Found`.
- **Defensive HTTP Headers**:
  - `Content-Type`: Validated MIME (`image/jpeg`, `image/png`, `image/webp`)
  - `Content-Disposition`: `inline; filename="[id].[ext]"` (prevents executable script download)
  - `Cache-Control`: `public, max-age=86400, immutable`
  - `X-Content-Type-Options`: `nosniff`

---

## 6. Configuration & Environment Variables

| Variable | Property Path | Default | Description |
| :--- | :--- | :--- | :--- |
| `STORAGE_PROVIDER` | `nagrivic.storage.provider` | `local` | Storage provider (`local` or `s3`) |
| `STORAGE_LOCAL_DIR` | `nagrivic.storage.local.directory` | `build/uploads` | Root directory for local storage |
| `STORAGE_S3_ENDPOINT` | `nagrivic.storage.s3.endpoint` | `""` | Optional endpoint override (MinIO, R2) |
| `STORAGE_S3_REGION` | `nagrivic.storage.s3.region` | `us-east-1` | S3 region |
| `STORAGE_S3_BUCKET` | `nagrivic.storage.s3.bucket` | `nagrivic-media` | Target bucket name |
| `STORAGE_S3_ACCESS_KEY` | `nagrivic.storage.s3.access-key` | `""` | S3 Access Key ID |
| `STORAGE_S3_SECRET_KEY` | `nagrivic.storage.s3.secret-key` | `""` | S3 Secret Access Key |
| `STORAGE_S3_PATH_STYLE_ACCESS` | `nagrivic.storage.s3.path-style-access`| `false` | Enable path-style for MinIO |
| `STORAGE_S3_CONNECTION_TIMEOUT_MS` | `nagrivic.storage.s3.connection-timeout-ms` | `10000` | HTTP connection timeout |

---

## 7. Migration Considerations

Existing media stored locally during development can remain at their relative keys (`issues/...`). In a future migration to cloud buckets:
- The database schema requires zero changes because `storage_key` strings remain invariant.
- Existing objects can be synchronized to S3 using standard tools (e.g. AWS CLI `aws s3 sync`) without modifying application code.
- No breaking changes or downtime are required when switching `STORAGE_PROVIDER=s3`.
