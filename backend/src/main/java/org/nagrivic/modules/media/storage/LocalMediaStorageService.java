package org.nagrivic.modules.media.storage;

import org.nagrivic.modules.media.config.MediaStorageProperties;
import org.nagrivic.modules.media.config.StorageProperties;
import org.nagrivic.modules.media.storage.exception.StorageException;
import org.nagrivic.modules.media.storage.exception.StorageFileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * Local filesystem implementation of ObjectStorageService / MediaStorageService.
 * Used for development, local operation, and fast automated tests without cloud credentials.
 */
public class LocalMediaStorageService implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalMediaStorageService.class);
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final Path rootLocation;

    public LocalMediaStorageService(StorageProperties properties) {
        String dir = (properties != null && properties.getLocal() != null && properties.getLocal().getDirectory() != null)
                ? properties.getLocal().getDirectory()
                : "build/uploads";
        this.rootLocation = Paths.get(dir).toAbsolutePath().normalize();
        init();
    }

    public LocalMediaStorageService(MediaStorageProperties properties) {
        String dir = (properties != null && properties.getLocalStorageDir() != null)
                ? properties.getLocalStorageDir()
                : "build/uploads";
        this.rootLocation = Paths.get(dir).toAbsolutePath().normalize();
        init();
    }

    public LocalMediaStorageService(Path rootLocation) {
        this.rootLocation = rootLocation.toAbsolutePath().normalize();
        init();
    }

    private void init() {
        try {
            Files.createDirectories(this.rootLocation);
            log.info("Initialized LocalMediaStorageService at: {}", this.rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize local storage directory: " + this.rootLocation, e);
        }
    }

    @Override
    public void store(String storageKey, InputStream inputStream, long contentLength, String contentType) {
        if (contentLength > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Payload size " + contentLength + " exceeds maximum allowed limit of " + MAX_FILE_SIZE_BYTES + " bytes");
        }
        Path targetPath = resolveAndVerifyPath(storageKey);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored media file at key: {} ({}, {} bytes)", storageKey, contentType, contentLength);
        } catch (IOException e) {
            log.error("Failed to store media file at key: {}", storageKey, e);
            throw new StorageException("Failed to store media file: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Path targetPath = resolveAndVerifyPath(storageKey);
            boolean deleted = Files.deleteIfExists(targetPath);
            if (deleted) {
                log.debug("Deleted media file at key: {}", storageKey);
            }
        } catch (Exception e) {
            log.warn("Failed to delete media file at key: {}", storageKey, e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        try {
            Path targetPath = resolveAndVerifyPath(storageKey);
            return Files.exists(targetPath) && Files.isRegularFile(targetPath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public InputStream load(String storageKey) {
        Path targetPath = resolveAndVerifyPath(storageKey);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new StorageFileNotFoundException("Media file not found: " + storageKey);
        }
        try {
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            throw new StorageException("Failed to read media file: " + storageKey, e);
        }
    }

    @Override
    public StorageMetadata getMetadata(String storageKey) {
        Path targetPath = resolveAndVerifyPath(storageKey);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new StorageFileNotFoundException("Media file not found: " + storageKey);
        }
        try {
            long size = Files.size(targetPath);
            String probe = Files.probeContentType(targetPath);
            Instant lastModified = Files.getLastModifiedTime(targetPath).toInstant();
            return new StorageMetadata(storageKey, size, probe != null ? probe : "application/octet-stream", lastModified);
        } catch (IOException e) {
            throw new StorageException("Failed to read media metadata: " + storageKey, e);
        }
    }

    public Path getRootLocation() {
        return rootLocation;
    }

    private Path resolveAndVerifyPath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key cannot be blank");
        }
        // Normalize slashes
        String normalizedKey = storageKey.replace('\\', '/').trim();
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }

        if (normalizedKey.contains("..") || normalizedKey.contains("./") || normalizedKey.contains("//")) {
            log.warn("Path traversal attempt detected with key: {}", storageKey);
            throw new SecurityException("Storage key path traversal attempt detected: " + storageKey);
        }

        Path resolvedPath = this.rootLocation.resolve(normalizedKey).normalize();
        if (!resolvedPath.startsWith(this.rootLocation)) {
            log.warn("Path traversal attempt detected with key: {}", storageKey);
            throw new SecurityException("Storage key path traversal attempt detected: " + storageKey);
        }
        return resolvedPath;
    }
}
