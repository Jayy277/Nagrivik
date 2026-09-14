package org.nagrivic.modules.media.storage;

import java.time.Instant;

/**
 * Metadata descriptor for an object stored in object storage.
 */
public record StorageMetadata(
        String storageKey,
        long contentLength,
        String contentType,
        Instant lastModified
) {}
