package org.nagrivic.modules.media.storage.exception;

/**
 * Thrown when object storage configuration is invalid or missing required parameters.
 */
public class StorageConfigurationException extends StorageException {

    public StorageConfigurationException(String message) {
        super(message);
    }

    public StorageConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
