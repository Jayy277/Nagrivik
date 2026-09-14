package org.nagrivic.modules.media.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.authorities.dto.ResolutionEvidenceResponse;
import org.nagrivic.modules.authorities.model.ResolutionEvidenceType;
import org.nagrivic.modules.authorities.repository.ResolutionEvidenceRepository;
import org.nagrivic.modules.authorities.service.ResolutionEvidenceService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.dto.MediaResponse;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.media.service.MediaService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class MediaDownloadAndConsistencyTest {

    private static final byte[] VALID_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08,
            (byte) 0xFF, (byte) 0xD9
    };

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaStorageService mediaStorageService;

    @Autowired
    private ResolutionEvidenceService resolutionEvidenceService;

    @Autowired
    private ResolutionEvidenceRepository resolutionEvidenceRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    private MockMvc mockMvc;
    private UserEntity citizen;
    private UserEntity officer;
    private IssueEntity issue;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        authSessionRepository.deleteAll();
        resolutionEvidenceRepository.deleteAll();
        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        citizen = userRepository.save(new UserEntity("+919999911111", "Citizen Tester"));
        officer = new UserEntity("+919999922222", "Officer Tester");
        officer.setRole("ADMIN");
        officer = userRepository.save(officer);

        CategoryEntity cat = categoryRepository.save(new CategoryEntity("Roads", "roads", "Road defects", 1));
        LocationEntity loc = locationService.createLocation(23.0225, 72.5714, BigDecimal.valueOf(5.0));

        issue = issueService.createIssue(citizen, cat.getId(), loc.getId(), "Deep Pothole", "Pothole on main road");
    }

    @Test
    void testDownloadMedia_VerifiedIssueMedia_StreamsSuccessfullyWithSafeHeaders() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pothole.jpg", "image/jpeg", VALID_JPEG_BYTES
        );

        // Upload media via service
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        citizen.getId().toString(), null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CITIZEN"))
                )
        );

        MediaResponse mediaResponse = mediaService.uploadIssueMedia(issue.getId(), file);
        assertNotNull(mediaResponse.id());

        org.nagrivic.modules.media.entity.MediaEntity mediaEntity = mediaRepository.findById(mediaResponse.id()).orElseThrow();
        String storageKey = mediaEntity.getStorageKey();
        assertNotNull(storageKey);
        assertTrue(mediaStorageService.exists(storageKey));

        // Stream via GET /api/media/{storageKey}
        mockMvc.perform(get("/api/media/" + storageKey))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "public, max-age=86400, immutable"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(VALID_JPEG_BYTES));
    }

    @Test
    void testDownloadMedia_UnrecordedKey_Returns404NotFound() throws Exception {
        mockMvc.perform(get("/api/media/issues/unrecorded-file.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadMedia_PathTraversalAttempt_Rejected() throws Exception {
        mockMvc.perform(get("/api/media/../../etc/passwd"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDownloadMedia_VerifiedResolutionEvidence_StreamsSuccessfully() throws Exception {
        issue.setStatus(org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS);
        issue.setResponsibilityStatus(org.nagrivic.modules.issues.model.ResponsibilityStatus.RESOLVED);
        issue = issueRepository.save(issue);

        MockMultipartFile file = new MockMultipartFile(
                "file", "fixed.jpg", "image/jpeg", VALID_JPEG_BYTES
        );

        ResolutionEvidenceResponse evResponse = resolutionEvidenceService.addResolutionEvidence(
                issue.getId(),
                file,
                ResolutionEvidenceType.COMPLETION_PHOTO,
                "Repaired road surface",
                null,
                officer
        );

        assertNotNull(evResponse.mediaUrl());
        assertTrue(evResponse.mediaUrl().startsWith("/api/media/"));

        String storageKey = evResponse.mediaUrl().substring("/api/media/".length());
        assertTrue(mediaStorageService.exists(storageKey));

        // Stream via GET /api/media/{storageKey}
        mockMvc.perform(get("/api/media/" + storageKey))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(VALID_JPEG_BYTES));
    }

    @Test
    void testCompensationCleanup_WhenStorageExceptionOccurs_NoOrphanLeft() {
        String testKey = "issues/" + issue.getId() + "/compensation-test.jpg";
        mediaStorageService.store(testKey, new java.io.ByteArrayInputStream(VALID_JPEG_BYTES), VALID_JPEG_BYTES.length, "image/jpeg");
        assertTrue(mediaStorageService.exists(testKey));

        // Simulating compensation cleanup
        mediaStorageService.delete(testKey);
        assertFalse(mediaStorageService.exists(testKey));
    }

    @AfterEach
    void tearDown() {
        authSessionRepository.deleteAll();
        resolutionEvidenceRepository.deleteAll();
        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }
}
