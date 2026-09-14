package org.nagrivic.modules.media.storage;

import org.nagrivic.modules.media.config.StorageProperties;
import org.nagrivic.modules.media.storage.exception.StorageConfigurationException;
import org.nagrivic.modules.media.storage.exception.StorageException;
import org.nagrivic.modules.media.storage.exception.StorageFileNotFoundException;
import org.nagrivic.modules.media.storage.exception.StorageUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Production-ready S3-compatible object storage implementation.
 * Supports AWS S3, MinIO, Cloudflare R2, and local S3-compatible emulation.
 */
public class S3ObjectStorageService implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorageService.class);
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB strict limit

    private final String bucket;
    private final S3Client s3Client;
    private final boolean closeClientOnDestroy;

    public S3ObjectStorageService(StorageProperties properties) {
        StorageProperties.S3 s3Props = properties.getS3();
        if (s3Props == null || s3Props.getBucket() == null || s3Props.getBucket().isBlank()) {
            throw new StorageConfigurationException("S3 bucket name must be configured when storage.provider=s3");
        }
        this.bucket = s3Props.getBucket().trim();

        S3ClientBuilder builder = S3Client.builder();

        // Configure region
        String regionStr = s3Props.getRegion() != null && !s3Props.getRegion().isBlank()
                ? s3Props.getRegion().trim()
                : "us-east-1";
        builder.region(Region.of(regionStr));

        // Configure HTTP connection client
        builder.httpClient(UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofMillis(s3Props.getConnectionTimeoutMs()))
                .build());

        // Custom endpoint override (MinIO, Cloudflare R2, LocalStack)
        if (s3Props.getEndpoint() != null && !s3Props.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3Props.getEndpoint().trim()));
        }

        // Path-style access for MinIO / local emulation
        if (s3Props.isPathStyleAccess()) {
            builder.forcePathStyle(true);
        }

        // Credentials: static if provided, else DefaultCredentialsProvider
        if (s3Props.getAccessKey() != null && !s3Props.getAccessKey().isBlank()
                && s3Props.getSecretKey() != null && !s3Props.getSecretKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3Props.getAccessKey().trim(), s3Props.getSecretKey().trim())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        this.s3Client = builder.build();
        this.closeClientOnDestroy = true;
        log.info("Initialized S3ObjectStorageService for bucket: '{}' in region: '{}' (path-style={})",
                this.bucket, regionStr, s3Props.isPathStyleAccess());
    }

    /**
     * Testing constructor allowing dependency injection of a custom or mock S3Client.
     */
    public S3ObjectStorageService(StorageProperties properties, S3Client s3Client) {
        StorageProperties.S3 s3Props = properties.getS3();
        this.bucket = s3Props != null && s3Props.getBucket() != null ? s3Props.getBucket().trim() : "test-bucket";
        this.s3Client = s3Client;
        this.closeClientOnDestroy = false;
    }

    @Override
    public void store(String storageKey, InputStream inputStream, long contentLength, String contentType) {
        String normalizedKey = normalizeAndVerifyKey(storageKey);

        if (contentLength > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Payload size " + contentLength + " exceeds maximum allowed limit of " + MAX_FILE_SIZE_BYTES + " bytes");
        }

        String safeContentType = contentType != null && !contentType.isBlank()
                ? contentType.trim()
                : "application/octet-stream";

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(normalizedKey)
                    .contentType(safeContentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            log.debug("Stored S3 object at key: '{}' in bucket: '{}' ({} bytes, {})",
                    normalizedKey, this.bucket, contentLength, safeContentType);
        } catch (S3Exception e) {
            log.error("S3 error storing object at key '{}': {} (status: {})", normalizedKey, e.getMessage(), e.statusCode());
            throw new StorageUnavailableException("Failed to store object in S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error storing object at key '{}': {}", normalizedKey, e.getMessage());
            throw new StorageException("Failed to store object in S3: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream load(String storageKey) {
        String normalizedKey = normalizeAndVerifyKey(storageKey);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(normalizedKey)
                    .build();

            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            log.debug("Object not found in S3 at key: '{}'", normalizedKey);
            throw new StorageFileNotFoundException("Media object not found in S3: " + normalizedKey, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new StorageFileNotFoundException("Media object not found in S3: " + normalizedKey, e);
            }
            log.error("S3 error loading object at key '{}': {} (status: {})", normalizedKey, e.getMessage(), e.statusCode());
            throw new StorageUnavailableException("Failed to load object from S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error loading object at key '{}': {}", normalizedKey, e.getMessage());
            throw new StorageException("Failed to load object from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        String normalizedKey = normalizeAndVerifyKey(storageKey);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(normalizedKey)
                    .build();

            s3Client.deleteObject(request);
            log.debug("Deleted S3 object at key: '{}'", normalizedKey);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object at key '{}': {}", normalizedKey, e.getMessage());
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        String normalizedKey = normalizeAndVerifyKey(storageKey);
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(normalizedKey)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.warn("S3 error checking existence of key '{}': {}", normalizedKey, e.getMessage());
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public StorageMetadata getMetadata(String storageKey) {
        String normalizedKey = normalizeAndVerifyKey(storageKey);
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(normalizedKey)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            return new StorageMetadata(
                    normalizedKey,
                    response.contentLength(),
                    response.contentType(),
                    response.lastModified()
            );
        } catch (NoSuchKeyException e) {
            throw new StorageFileNotFoundException("Media object not found in S3: " + normalizedKey, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new StorageFileNotFoundException("Media object not found in S3: " + normalizedKey, e);
            }
            throw new StorageUnavailableException("Failed to read object metadata from S3: " + e.getMessage(), e);
        }
    }

    private String normalizeAndVerifyKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key cannot be blank");
        }
        String normalized = storageKey.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..") || normalized.contains("./") || normalized.contains("//")) {
            log.warn("Path traversal or invalid sequence detected in S3 storage key: '{}'", storageKey);
            throw new SecurityException("Storage key contains illegal path traversal sequences: " + storageKey);
        }
        return normalized;
    }

    public String getBucket() {
        return bucket;
    }

    public void close() {
        if (closeClientOnDestroy && s3Client != null) {
            try {
                s3Client.close();
            } catch (Exception ignored) {}
        }
    }
}
