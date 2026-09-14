package org.nagrivic.modules.media.storage;

import java.io.InputStream;

/**
 * Storage abstraction for media binary objects.
 * Decouples the domain layer from specific storage backends (Local Disk, S3, MinIO, Cloudflare R2, etc.).
 */
public interface MediaStorageService extends ObjectStorageService {

    /**
     * Stores media binary content at the specified storage key.
     *
     * @param storageKey    server-generated unique storage path
     * @param inputStream   stream of the file content
     * @param contentLength size of the content in bytes
     * @param contentType   MIME type of the content
     */
    void store(String storageKey, InputStream inputStream, long contentLength, String contentType);

    /**
     * Deletes media binary content at the specified storage key if it exists.
     *
     * @param storageKey server-generated unique storage path
     */
    void delete(String storageKey);

    /**
     * Checks if media binary content exists at the specified storage key.
     *
     * @param storageKey server-generated unique storage path
     * @return true if content exists, false otherwise
     */
    boolean exists(String storageKey);

    /**
     * Opens an input stream to read media binary content at the specified storage key.
     *
     * @param storageKey server-generated unique storage path
     * @return InputStream to the media content
     */
    InputStream load(String storageKey);
}
