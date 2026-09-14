package org.nagrivic.modules.media.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Provider-independent object storage abstraction.
 * Decouples the application from concrete storage backends (Local Disk, AWS S3, MinIO, Cloudflare R2).
 */
public interface ObjectStorageService {

    /**
     * Stores binary content at the specified server-generated storage key.
     *
     * @param storageKey    server-generated unique storage key
     * @param inputStream   stream of the object content
     * @param contentLength size of the content in bytes
     * @param contentType   MIME type of the content
     */
    void store(String storageKey, InputStream inputStream, long contentLength, String contentType);

    /**
     * Retrieves an input stream for reading the object at the specified storage key.
     *
     * @param storageKey server-generated unique storage key
     * @return InputStream to the object content
     */
    InputStream load(String storageKey);

    /**
     * Deletes the object at the specified storage key if it exists.
     *
     * @param storageKey server-generated unique storage key
     */
    void delete(String storageKey);

    /**
     * Checks if an object exists at the specified storage key.
     *
     * @param storageKey server-generated unique storage key
     * @return true if object exists, false otherwise
     */
    boolean exists(String storageKey);

    /**
     * Retrieves metadata for the object at the specified storage key.
     *
     * @param storageKey server-generated unique storage key
     * @return StorageMetadata containing length, content type, and last modified timestamp
     */
    StorageMetadata getMetadata(String storageKey);

    /**
     * Generates a short-lived presigned URL for reading private objects when supported.
     *
     * @param storageKey server-generated unique storage key
     * @param expiration duration for which the presigned URL remains valid
     * @return presigned read URL string, or null if direct streaming is preferred/used
     */
    default String generatePresignedReadUrl(String storageKey, Duration expiration) {
        return null;
    }
}
