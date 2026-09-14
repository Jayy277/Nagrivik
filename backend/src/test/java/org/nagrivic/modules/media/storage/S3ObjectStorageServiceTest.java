package org.nagrivic.modules.media.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.media.config.StorageProperties;
import org.nagrivic.modules.media.storage.exception.StorageConfigurationException;
import org.nagrivic.modules.media.storage.exception.StorageFileNotFoundException;
import org.nagrivic.modules.media.storage.exception.StorageUnavailableException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class S3ObjectStorageServiceTest {

    private S3Client s3Client;
    private StorageProperties properties;
    private S3ObjectStorageService storageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        properties = new StorageProperties();
        properties.setProvider("s3");
        properties.getS3().setBucket("nagrivic-test-bucket");
        properties.getS3().setRegion("us-east-1");

        storageService = new S3ObjectStorageService(properties, s3Client);
    }

    @Test
    void testConfigurationValidation_MissingBucket_ThrowsStorageConfigurationException() {
        StorageProperties emptyBucketProps = new StorageProperties();
        emptyBucketProps.setProvider("s3");
        emptyBucketProps.getS3().setBucket("");

        assertThrows(StorageConfigurationException.class, () -> new S3ObjectStorageService(emptyBucketProps));
    }

    @Test
    void testStore_CallsPutObject_WithCorrectBucketAndKey() {
        String key = "issues/test-issue/photo.jpg";
        byte[] data = "s3 test binary data".getBytes(StandardCharsets.UTF_8);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        storageService.store(key, new ByteArrayInputStream(data), data.length, "image/jpeg");

        verify(s3Client).putObject(
                argThat((PutObjectRequest req) ->
                        req.bucket().equals("nagrivic-test-bucket") &&
                        req.key().equals("issues/test-issue/photo.jpg") &&
                        req.contentType().equals("image/jpeg") &&
                        req.contentLength().equals((long) data.length)
                ),
                any(RequestBody.class)
        );
    }

    @Test
    void testLoad_CallsGetObject_ReturnsStream() throws Exception {
        String key = "issues/test-issue/photo.jpg";
        byte[] data = "downloaded content".getBytes(StandardCharsets.UTF_8);

        GetObjectResponse response = GetObjectResponse.builder()
                .contentLength((long) data.length)
                .contentType("image/jpeg")
                .build();
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(
                response,
                AbortableInputStream.create(new ByteArrayInputStream(data))
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

        try (InputStream is = storageService.load(key)) {
            byte[] loaded = is.readAllBytes();
            assertArrayEquals(data, loaded);
        }

        verify(s3Client).getObject(argThat((GetObjectRequest req) ->
                req.bucket().equals("nagrivic-test-bucket") &&
                req.key().equals("issues/test-issue/photo.jpg")
        ));
    }

    @Test
    void testLoad_NoSuchKey_ThrowsStorageFileNotFoundException() {
        String key = "issues/test-issue/missing.jpg";
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        assertThrows(StorageFileNotFoundException.class, () -> storageService.load(key));
    }

    @Test
    void testLoad_S3ServerError_ThrowsStorageUnavailableException() {
        String key = "issues/test-issue/error.jpg";
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(500)
                        .awsErrorDetails(software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                                .errorMessage("Internal S3 error")
                                .build())
                        .build());

        assertThrows(StorageUnavailableException.class, () -> storageService.load(key));
    }

    @Test
    void testDelete_CallsDeleteObject() {
        String key = "issues/test-issue/delete-me.jpg";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        storageService.delete(key);

        verify(s3Client).deleteObject(argThat((DeleteObjectRequest req) ->
                req.bucket().equals("nagrivic-test-bucket") &&
                req.key().equals("issues/test-issue/delete-me.jpg")
        ));
    }

    @Test
    void testExists_HeadObjectSuccess_ReturnsTrue() {
        String key = "issues/test-issue/exists.jpg";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertTrue(storageService.exists(key));
    }

    @Test
    void testExists_NoSuchKey_ReturnsFalse() {
        String key = "issues/test-issue/missing.jpg";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

        assertFalse(storageService.exists(key));
    }

    @Test
    void testGetMetadata_CallsHeadObject_ReturnsStorageMetadata() {
        String key = "issues/test-issue/meta.jpg";
        Instant now = Instant.now();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(1024L)
                        .contentType("image/jpeg")
                        .lastModified(now)
                        .build());

        StorageMetadata metadata = storageService.getMetadata(key);
        assertEquals(key, metadata.storageKey());
        assertEquals(1024L, metadata.contentLength());
        assertEquals("image/jpeg", metadata.contentType());
        assertEquals(now, metadata.lastModified());
    }

    @Test
    void testStore_PathTraversalAttempt_ThrowsSecurityException() {
        byte[] data = "traversal".getBytes(StandardCharsets.UTF_8);

        assertThrows(SecurityException.class, () ->
                storageService.store("../../bucket/secret.jpg", new ByteArrayInputStream(data), data.length, "image/jpeg")
        );
    }

    @Test
    void testStore_PayloadExceeds10MB_ThrowsIllegalArgumentException() {
        long oversized = 11L * 1024 * 1024;
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        assertThrows(IllegalArgumentException.class, () ->
                storageService.store("issues/large.jpg", stream, oversized, "image/jpeg")
        );
    }
}
