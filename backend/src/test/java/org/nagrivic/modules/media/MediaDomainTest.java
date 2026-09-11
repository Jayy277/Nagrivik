package org.nagrivic.modules.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.dto.MediaResponse;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.model.MediaType;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.media.service.MediaService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MediaDomainTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationService locationService;

    private UserEntity testUser;
    private CategoryEntity testCategory;
    private LocationEntity testLocation;
    private IssueEntity testIssue;

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();

        testUser = userRepository.save(new UserEntity("+919876543210", "Aarav Patel"));
        testCategory = categoryRepository.save(new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1));
        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));

        testIssue = issueService.createIssue(
                testUser.getId(),
                testCategory.getId(),
                testLocation.getId(),
                "Large pothole near SG Highway",
                "Deep pothole causing vehicle damage"
        );
    }

    @Test
    void shouldCreateAndPersistMediaMetadataBelongingToIssue() {
        MediaEntity media = mediaService.createMedia(
                testIssue.getId(),
                "issues/" + testIssue.getId() + "/photo-1.jpg",
                "pothole_evidence.jpg",
                "image/jpeg",
                2048576L, // ~2MB
                MediaType.IMAGE,
                1
        );

        assertNotNull(media.getId());
        assertEquals(testIssue.getId(), media.getIssue().getId());
        assertEquals("issues/" + testIssue.getId() + "/photo-1.jpg", media.getStorageKey());
        assertEquals("pothole_evidence.jpg", media.getOriginalFilename());
        assertEquals("image/jpeg", media.getContentType());
        assertEquals(2048576L, media.getFileSizeBytes());
        assertEquals(MediaType.IMAGE, media.getMediaType());
        assertEquals(1, media.getDisplayOrder());
        assertNotNull(media.getCreatedAt());
    }

    @Test
    void shouldSupportMultipleMediaRecordsForOneIssueOrderedByDisplayOrder() {
        MediaEntity media2 = mediaService.createMedia(
                testIssue.getId(),
                "issues/" + testIssue.getId() + "/photo-2.jpg",
                "photo_side.jpg",
                "image/jpeg",
                1024000L,
                MediaType.IMAGE,
                2
        );

        MediaEntity media1 = mediaService.createMedia(
                testIssue.getId(),
                "issues/" + testIssue.getId() + "/photo-1.jpg",
                "photo_front.jpg",
                "image/jpeg",
                1536000L,
                MediaType.IMAGE,
                1
        );

        List<MediaEntity> issueMedia = mediaService.findByIssueId(testIssue.getId());
        assertEquals(2, issueMedia.size());
        assertEquals(media1.getId(), issueMedia.get(0).getId(), "First item must be display_order 1");
        assertEquals(media2.getId(), issueMedia.get(1).getId(), "Second item must be display_order 2");
        assertEquals(2, mediaService.countByIssueId(testIssue.getId()));
    }

    @Test
    void shouldEnforceStorageKeyUniqueness() {
        String sharedKey = "issues/" + testIssue.getId() + "/unique-photo.jpg";

        mediaService.createMedia(
                testIssue.getId(),
                sharedKey,
                "first.jpg",
                "image/jpeg",
                500000L,
                MediaType.IMAGE,
                1
        );

        // Domain service check
        assertThrows(IllegalArgumentException.class, () -> {
            mediaService.createMedia(
                    testIssue.getId(),
                    sharedKey,
                    "duplicate.jpg",
                    "image/jpeg",
                    600000L,
                    MediaType.IMAGE,
                    2
            );
        });

        // Entity direct persistence check for DB unique constraint
        assertThrows(DataIntegrityViolationException.class, () -> {
            MediaEntity directDuplicate = new MediaEntity(
                    testIssue,
                    sharedKey,
                    "direct_dup.jpg",
                    "image/jpeg",
                    700000L,
                    MediaType.IMAGE,
                    3
            );
            mediaRepository.saveAndFlush(directDuplicate);
        });
    }

    @Test
    void shouldRejectNegativeFileSize() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            mediaService.createMedia(
                    testIssue.getId(),
                    "issues/" + testIssue.getId() + "/invalid.jpg",
                    "invalid.jpg",
                    "image/jpeg",
                    -10L,
                    MediaType.IMAGE,
                    1
            );
        });
        assertTrue(ex.getMessage().contains("File size must be non-negative"));
    }

    @Test
    void shouldRejectNegativeDisplayOrder() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            mediaService.createMedia(
                    testIssue.getId(),
                    "issues/" + testIssue.getId() + "/negative_order.jpg",
                    "neg.jpg",
                    "image/jpeg",
                    1000L,
                    MediaType.IMAGE,
                    -1
            );
        });
        assertTrue(ex.getMessage().contains("Display order cannot be negative"));
    }

    @Test
    void shouldRejectBlankStorageKey() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            mediaService.createMedia(
                    testIssue.getId(),
                    "   ",
                    "photo.jpg",
                    "image/jpeg",
                    1000L,
                    MediaType.IMAGE,
                    1
            );
        });
        assertTrue(ex.getMessage().contains("Storage key cannot be blank"));
    }

    @Test
    void shouldRejectBlankContentType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            mediaService.createMedia(
                    testIssue.getId(),
                    "issues/" + testIssue.getId() + "/valid.jpg",
                    "photo.jpg",
                    "   ",
                    1000L,
                    MediaType.IMAGE,
                    1
            );
        });
        assertTrue(ex.getMessage().contains("Content type cannot be blank"));
    }

    @Test
    void shouldRejectMediaForNonExistentIssue() {
        UUID nonExistentIssueId = UUID.randomUUID();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            mediaService.createMedia(
                    nonExistentIssueId,
                    "issues/" + nonExistentIssueId + "/orphaned.jpg",
                    "orphan.jpg",
                    "image/jpeg",
                    1000L,
                    MediaType.IMAGE,
                    1
            );
        });
        assertTrue(ex.getMessage().contains("Issue not found"));
    }

    @Test
    void shouldCascadeDeleteMediaWhenIssueIsDeleted() {
        MediaEntity media = mediaService.createMedia(
                testIssue.getId(),
                "issues/" + testIssue.getId() + "/to_be_deleted.jpg",
                "evidence.jpg",
                "image/jpeg",
                1000000L,
                MediaType.IMAGE,
                1
        );
        UUID mediaId = media.getId();
        assertTrue(mediaRepository.existsById(mediaId));

        // Delete parent issue
        issueRepository.delete(testIssue);
        issueRepository.flush();

        assertFalse(issueRepository.existsById(testIssue.getId()));
        assertFalse(mediaRepository.existsById(mediaId), "Associated media must be cascaded and deleted");
    }

    @Test
    void shouldMapMediaResponseWithoutLeakingStorageKey() {
        MediaEntity media = mediaService.createMedia(
                testIssue.getId(),
                "issues/" + testIssue.getId() + "/private-path.jpg",
                "report.jpg",
                "image/webp",
                850000L,
                MediaType.IMAGE,
                1
        );

        MediaResponse response = MediaResponse.fromEntity(media);
        assertNotNull(response);
        assertEquals(media.getId(), response.id());
        assertEquals(testIssue.getId(), response.issueId());
        assertEquals(MediaType.IMAGE, response.mediaType());
        assertEquals("image/webp", response.contentType());
        assertEquals(850000L, response.fileSizeBytes());
        assertEquals(1, response.displayOrder());
        assertNotNull(response.createdAt());
    }

    @Test
    void shouldMapIssueResponseWithMediaSummaries() {
        MediaEntity media1 = mediaService.createMedia(
                testIssue.getId(),
                "issues/" + testIssue.getId() + "/photo-1.jpg",
                "1.jpg",
                "image/jpeg",
                1000L,
                MediaType.IMAGE,
                1
        );
        MediaEntity media2 = mediaService.createMedia(
                testIssue.getId(),
                "issues/" + testIssue.getId() + "/photo-2.jpg",
                "2.jpg",
                "image/png",
                2000L,
                MediaType.IMAGE,
                2
        );

        List<MediaEntity> mediaList = mediaService.findByIssueId(testIssue.getId());
        IssueResponse issueResponse = IssueResponse.fromEntity(testIssue, mediaList);

        assertNotNull(issueResponse);
        assertNotNull(issueResponse.media());
        assertEquals(2, issueResponse.media().size());
        assertEquals(media1.getId(), issueResponse.media().get(0).id());
        assertEquals("image/jpeg", issueResponse.media().get(0).contentType());
        assertEquals(media2.getId(), issueResponse.media().get(1).id());
        assertEquals("image/png", issueResponse.media().get(1).contentType());
    }
}
