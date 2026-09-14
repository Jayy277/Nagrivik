package org.nagrivic.modules.media.storage.exception;

/**
 * Thrown when the object storage backend is unreachable or returns a transient/connectivity failure.
 */
public class StorageUnavailableException extends StorageException {

    public StorageUnavailableException(String message) {
        super(message);
    }

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
