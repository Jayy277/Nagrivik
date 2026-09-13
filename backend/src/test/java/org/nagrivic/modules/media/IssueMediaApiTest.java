package org.nagrivic.modules.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.model.MediaType;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.media.storage.MediaStorageService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class IssueMediaApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaStorageService mediaStorageService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    private MockMvc mockMvc;

    private UserEntity issueOwner;
    private UserEntity otherUser;
    private UserEntity inactiveUser;
    private IssueEntity testIssue;

    // Minimal valid headers
    private static final byte[] VALID_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };

    private static final byte[] VALID_WEBP_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        userRepository.deleteAll();

        issueOwner = userRepository.save(new UserEntity("+919876543210", "Aarav Patel"));
        otherUser = userRepository.save(new UserEntity("+919876543211", "Rohan Sharma"));
        inactiveUser = new UserEntity("+919876543212", "Inactive Citizen");
        inactiveUser.setActive(false);
        inactiveUser = userRepository.save(inactiveUser);

        CategoryEntity category = categoryRepository.save(new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1));
        LocationEntity location = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));

        testIssue = issueService.createIssue(
                issueOwner,
                category.getId(),
                location.getId(),
                "Pothole near SG Highway",
                "Deep pothole causing vehicle damage"
        );
    }

    private String tokenFor(UserEntity user) {
        return jwtService.generateAccessToken(user.getId(), user.getRole());
    }

    // ==========================================
    // 1. AUTHENTICATION TESTS
    // ==========================================

    @Test
    void uploadWithoutJwtShouldReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadWithInvalidJwtShouldReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer invalid-tampered-token"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. AUTHORIZATION & OWNERSHIP TESTS
    // ==========================================

    @Test
    void issueOwnerCanUploadValidMedia() throws Exception {
        String token = tokenFor(issueOwner);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.issueId", equalTo(testIssue.getId().toString())))
                .andExpect(jsonPath("$.mediaType", equalTo("IMAGE")))
                .andExpect(jsonPath("$.contentType", equalTo("image/jpeg")))
                .andExpect(jsonPath("$.fileSizeBytes", equalTo(VALID_JPEG_BYTES.length)))
                .andExpect(jsonPath("$.displayOrder", equalTo(0)))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        List<MediaEntity> saved = mediaRepository.findByIssue_IdOrderByDisplayOrderAsc(testIssue.getId());
        assertEquals(1, saved.size());
        assertEquals("evidence.jpg", saved.get(0).getOriginalFilename());
        assertTrue(mediaStorageService.exists(saved.get(0).getStorageKey()));
    }

    @Test
    void differentAuthenticatedUserCannotUploadToOthersIssue() throws Exception {
        String token = tokenFor(otherUser);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo("FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("not authorized to attach media")));

        assertEquals(0, mediaRepository.countByIssue_Id(testIssue.getId()));
    }

    @Test
    void inactiveUserCannotUploadMedia() throws Exception {
        String token = tokenFor(inactiveUser);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo("FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("User account is inactive")));
    }

    // ==========================================
    // 3. ISSUE LOOKUP TESTS
    // ==========================================

    @Test
    void uploadToNonExistentIssueShouldReturn404() throws Exception {
        String token = tokenFor(issueOwner);
        UUID nonExistentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", nonExistentId)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")));
    }

    // ==========================================
    // 4. FILE VALIDATION TESTS
    // ==========================================

    @Test
    void validPngAccepted() throws Exception {
        String token = tokenFor(issueOwner);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "screenshot.png",
                "image/png",
                VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaType", equalTo("IMAGE")))
                .andExpect(jsonPath("$.contentType", equalTo("image/png")));
    }

    @Test
    void validWebpAccepted() throws Exception {
        String token = tokenFor(issueOwner);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.webp",
                "image/webp",
                VALID_WEBP_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaType", equalTo("IMAGE")))
                .andExpect(jsonPath("$.contentType", equalTo("image/webp")));
    }

    @Test
    void unsupportedContentTypeRejected() throws Exception {
        String token = tokenFor(issueOwner);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46} // %PDF
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported media type")));
    }

    @Test
    void emptyFileRejected() throws Exception {
        String token = tokenFor(issueOwner);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be empty")));
    }

    @Test
    void mismatchedMagicBytesRejected() throws Exception {
        String token = tokenFor(issueOwner);
        // Spoofed file: text/script pretending to be JPEG
        byte[] fakeJpegBytes = "echo 'malicious script'".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.jpg",
                "image/jpeg",
                fakeJpegBytes
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match JPEG signature")));
    }

    @Test
    void mismatchedExtensionRejected() throws Exception {
        String token = tokenFor(issueOwner);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match declared Content-Type")));
    }

    @Test
    void oversizedFileRejected() throws Exception {
        String token = tokenFor(issueOwner);
        // 11 MB file exceeds 10 MB limit
        byte[] largeBytes = new byte[11 * 1024 * 1024];
        System.arraycopy(VALID_JPEG_BYTES, 0, largeBytes, 0, VALID_JPEG_BYTES.length);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "huge.jpg",
                "image/jpeg",
                largeBytes
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds the maximum allowed limit")));
    }

    // ==========================================
    // 5. SECURITY & METADATA TESTS
    // ==========================================

    @Test
    void serverControlsStorageKeyAndPathTraversalIsPrevented() throws Exception {
        String token = tokenFor(issueOwner);
        // Client attempts directory traversal in filename
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../../etc/passwd.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()));

        List<MediaEntity> saved = mediaRepository.findByIssue_IdOrderByDisplayOrderAsc(testIssue.getId());
        assertEquals(1, saved.size());
        MediaEntity media = saved.get(0);

        // Verify storage key format is server controlled: issues/{issueId}/{uuid}.jpg
        assertTrue(media.getStorageKey().startsWith("issues/" + testIssue.getId() + "/"));
        assertTrue(media.getStorageKey().endsWith(".jpg"));
        assertFalse(media.getStorageKey().contains("passwd"));
        assertFalse(media.getStorageKey().contains(".."));

        // Verify original filename was preserved only as metadata
        assertEquals("../../../../etc/passwd.jpg", media.getOriginalFilename());
    }

    @Test
    void displayOrderIsIncrementedSequentiallyStartingFromZero() throws Exception {
        String token = tokenFor(issueOwner);

        // Upload first image
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "photo1.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );
        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayOrder", equalTo(0)));

        // Upload second image
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "photo2.png",
                "image/png",
                VALID_PNG_BYTES
        );
        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file2)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayOrder", equalTo(1)));

        // Upload third image
        MockMultipartFile file3 = new MockMultipartFile(
                "file",
                "photo3.webp",
                "image/webp",
                VALID_WEBP_BYTES
        );
        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file3)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayOrder", equalTo(2)));

        List<MediaEntity> mediaList = mediaRepository.findByIssue_IdOrderByDisplayOrderAsc(testIssue.getId());
        assertEquals(3, mediaList.size());
        assertEquals(0, mediaList.get(0).getDisplayOrder());
        assertEquals(1, mediaList.get(1).getDisplayOrder());
        assertEquals(2, mediaList.get(2).getDisplayOrder());
    }

    @Test
    void responseDoesNotLeakInternalPathsOrSecrets() throws Exception {
        String token = tokenFor(issueOwner);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "private.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/issues/{issueId}/media", testIssue.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.filePath").doesNotExist())
                .andExpect(jsonPath("$.serverPath").doesNotExist())
                .andExpect(jsonPath("$.bytes").doesNotExist())
                .andExpect(jsonPath("$.raw").doesNotExist());
    }
}
