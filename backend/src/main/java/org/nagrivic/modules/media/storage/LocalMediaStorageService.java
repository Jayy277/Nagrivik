package org.nagrivic.modules.media.storage;

import org.nagrivic.modules.media.config.MediaStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalMediaStorageService implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalMediaStorageService.class);

    private final Path rootLocation;

    public LocalMediaStorageService(MediaStorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocalStorageDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            log.info("Initialized LocalMediaStorageService at: {}", this.rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize local storage directory: " + this.rootLocation, e);
        }
    }

    @Override
    public void store(String storageKey, InputStream inputStream, long contentLength, String contentType) {
        Path targetPath = resolveAndVerifyPath(storageKey);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored media file at key: {} ({}, {} bytes)", storageKey, contentType, contentLength);
        } catch (IOException e) {
            log.error("Failed to store media file at key: {}", storageKey, e);
            throw new RuntimeException("Failed to store media file: " + storageKey, e);
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
            throw new org.nagrivic.common.error.ResourceNotFoundException("Media file not found: " + storageKey);
        }
        try {
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read media file: " + storageKey, e);
        }
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

        Path resolvedPath = this.rootLocation.resolve(normalizedKey).normalize();
        if (!resolvedPath.startsWith(this.rootLocation)) {
            log.warn("Path traversal attempt detected with key: {}", storageKey);
            throw new SecurityException("Storage key path traversal attempt detected: " + storageKey);
        }
        return resolvedPath;
    }
}
