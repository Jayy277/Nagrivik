package org.nagrivic.modules.media.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nagrivic.modules.media.storage.exception.StorageFileNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageTest {

    @TempDir
    Path tempDir;

    private LocalMediaStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalMediaStorageService(tempDir);
    }

    @Test
    void testStoreAndLoad_ValidObject_ContentMatches() throws Exception {
        String key = "issues/123/photo.jpg";
        byte[] data = "test image binary content".getBytes(StandardCharsets.UTF_8);

        storageService.store(key, new ByteArrayInputStream(data), data.length, "image/jpeg");

        assertTrue(storageService.exists(key));

        try (InputStream is = storageService.load(key)) {
            byte[] loaded = is.readAllBytes();
            assertArrayEquals(data, loaded);
        }

        StorageMetadata metadata = storageService.getMetadata(key);
        assertEquals(key, metadata.storageKey());
        assertEquals(data.length, metadata.contentLength());
        assertNotNull(metadata.lastModified());
    }

    @Test
    void testDelete_ExistingObject_RemovedSuccessfully() {
        String key = "issues/123/temp.jpg";
        byte[] data = "temp content".getBytes(StandardCharsets.UTF_8);

        storageService.store(key, new ByteArrayInputStream(data), data.length, "image/jpeg");
        assertTrue(storageService.exists(key));

        storageService.delete(key);
        assertFalse(storageService.exists(key));
    }

    @Test
    void testDelete_NonExistingObject_NoOpWithoutError() {
        assertDoesNotThrow(() -> storageService.delete("issues/non-existing.jpg"));
    }

    @Test
    void testLoad_MissingObject_ThrowsStorageFileNotFoundException() {
        assertThrows(StorageFileNotFoundException.class, () -> storageService.load("issues/missing-file.jpg"));
    }

    @Test
    void testGetMetadata_MissingObject_ThrowsStorageFileNotFoundException() {
        assertThrows(StorageFileNotFoundException.class, () -> storageService.getMetadata("issues/missing-file.jpg"));
    }

    @Test
    void testStore_PathTraversalAttempt_ThrowsSecurityException() {
        byte[] data = "malicious payload".getBytes(StandardCharsets.UTF_8);

        assertThrows(SecurityException.class, () ->
                storageService.store("../../etc/passwd", new ByteArrayInputStream(data), data.length, "text/plain")
        );

        assertThrows(SecurityException.class, () ->
                storageService.store("issues/../../sensitive.key", new ByteArrayInputStream(data), data.length, "text/plain")
        );
    }

    @Test
    void testLoad_PathTraversalAttempt_ThrowsSecurityException() {
        assertThrows(SecurityException.class, () -> storageService.load("../../etc/shadow"));
        assertThrows(SecurityException.class, () -> storageService.load("issues/../../../secret"));
    }

    @Test
    void testStore_PayloadExceeds10MB_ThrowsIllegalArgumentException() {
        long oversized = 11L * 1024 * 1024; // 11 MB
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        assertThrows(IllegalArgumentException.class, () ->
                storageService.store("issues/huge.jpg", emptyStream, oversized, "image/jpeg")
        );
    }

    @Test
    void testExists_BlankKey_ReturnsFalse() {
        assertFalse(storageService.exists(null));
        assertFalse(storageService.exists(""));
        assertFalse(storageService.exists("   "));
    }
}
