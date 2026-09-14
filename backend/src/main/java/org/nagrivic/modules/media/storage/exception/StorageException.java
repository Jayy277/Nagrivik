package org.nagrivic.modules.media.storage.exception;

/**
 * Base exception for object storage operations.
 * Protects clients from provider-specific exceptions and prevents credential/endpoint leakage.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
