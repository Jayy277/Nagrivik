package org.nagrivic.modules.media.storage.exception;

/**
 * Thrown when a requested object is not found in object storage.
 */
public class StorageFileNotFoundException extends StorageException {

    public StorageFileNotFoundException(String message) {
        super(message);
    }

    public StorageFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
