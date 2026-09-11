package org.nagrivic.modules.issues;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.nagrivic.common.error.GlobalExceptionHandler;
import org.nagrivic.modules.issues.controller.IssueController;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class IssueApiTest {

    @Autowired
    private IssueController issueController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private MediaRepository mediaRepository;

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

    private UserEntity testUser;
    private CategoryEntity testCategory;
    private LocationEntity testLocation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(issueController)
                .setControllerAdvice(globalExceptionHandler)
                .build();

        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(new UserEntity("+919876543210", "Aarav Patel"));
        testCategory = categoryRepository.save(new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1));
        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    // ==========================================
    // 1. CREATE ISSUE TESTS (POST /api/issues)
    // ==========================================

    @Test
    void shouldCreateIssueAndReturn201WithLocationHeader() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Large pothole near SG Highway",
                "Deep pothole in the left lane causing traffic slowdown.",
                testCategory.getId(),
                testLocation.getId(),
                testUser.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/issues/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Large pothole near SG Highway"))
                .andExpect(jsonPath("$.description").value("Deep pothole in the left lane causing traffic slowdown."))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.reportedBy").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.category.id").value(testCategory.getId().toString()))
                .andExpect(jsonPath("$.category.name").value("Roads / Potholes"))
                .andExpect(jsonPath("$.category.slug").value("roads-potholes"))
                .andExpect(jsonPath("$.location.id").value(testLocation.getId().toString()))
                .andExpect(jsonPath("$.location.latitude").value(23.0225))
                .andExpect(jsonPath("$.location.longitude").value(72.5714))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void shouldRejectCreateIssueWithMissingTitle() throws Exception {
        Map<String, Object> body = Map.of(
                "description", "Some description",
                "categoryId", testCategory.getId().toString(),
                "locationId", testLocation.getId().toString(),
                "reportedBy", testUser.getId().toString()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("title")));
    }

    @Test
    void shouldRejectCreateIssueWithBlankTitle() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "    ",
                "Some description",
                testCategory.getId(),
                testLocation.getId(),
                testUser.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("title")));
    }

    @Test
    void shouldRejectCreateIssueWithMissingCategory() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Valid Title",
                "locationId", testLocation.getId().toString(),
                "reportedBy", testUser.getId().toString()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("categoryId")));
    }

    @Test
    void shouldRejectCreateIssueWithNonExistentCategory() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Description",
                UUID.randomUUID(),
                testLocation.getId(),
                testUser.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Category not found")));
    }

    @Test
    void shouldRejectCreateIssueWithInactiveCategory() throws Exception {
        CategoryEntity inactiveCategory = new CategoryEntity("Deprecated", "deprecated-cat", "Old category", 99);
        inactiveCategory.setActive(false);
        inactiveCategory = categoryRepository.save(inactiveCategory);

        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Description",
                inactiveCategory.getId(),
                testLocation.getId(),
                testUser.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Cannot report an issue under an inactive category")));
    }

    @Test
    void shouldRejectCreateIssueWithNonExistentLocation() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Description",
                testCategory.getId(),
                UUID.randomUUID(),
                testUser.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Location not found")));
    }

    @Test
    void shouldRejectCreateIssueWithNonExistentReporter() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Description",
                testCategory.getId(),
                testLocation.getId(),
                UUID.randomUUID()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Reporter user not found")));
    }

    @Test
    void shouldIgnoreOrDisallowClientSuppliedStatusAndAlwaysSetReported() throws Exception {
        // Even if client attempts to pass status="RESOLVED" in raw JSON, the server DTO does not bind it
        Map<String, Object> bodyWithStatus = Map.of(
                "title", "Pothole report",
                "description", "Pothole details",
                "categoryId", testCategory.getId().toString(),
                "locationId", testLocation.getId().toString(),
                "reportedBy", testUser.getId().toString(),
                "status", "RESOLVED"
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyWithStatus)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    // ==========================================
    // 2. GET ISSUE BY ID TESTS (GET /api/issues/{id})
    // ==========================================

    @Test
    void shouldGetIssueById() throws Exception {
        IssueEntity issue = issueService.createIssue(
                testUser.getId(),
                testCategory.getId(),
                testLocation.getId(),
                "Streetlight broken",
                "Streetlight #42 is flickering and dark at night"
        );

        mockMvc.perform(get("/api/issues/{id}", issue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(issue.getId().toString()))
                .andExpect(jsonPath("$.title").value("Streetlight broken"))
                .andExpect(jsonPath("$.description").value("Streetlight #42 is flickering and dark at night"))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.reportedBy").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.category.name").value("Roads / Potholes"))
                .andExpect(jsonPath("$.location.latitude").value(23.0225));
    }

    @Test
    void shouldReturn404ForNonExistentIssueId() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/issues/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Issue not found with identifier: " + nonExistentId)))
                .andExpect(jsonPath("$.path").value("/api/issues/" + nonExistentId));
    }

    @Test
    void shouldReturn400ForMalformedIssueId() throws Exception {
        mockMvc.perform(get("/api/issues/{id}", "not-a-valid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Invalid parameter: 'id'")));
    }

    // ==========================================
    // 3. LIST ISSUES TESTS (GET /api/issues)
    // ==========================================

    @Test
    void shouldListIssuesWithDefaultPaginationAndNewestFirstSorting() throws Exception {
        IssueEntity issue1 = issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Issue 1", "Desc 1");
        Thread.sleep(10);
        IssueEntity issue2 = issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Issue 2", "Desc 2");

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(issue2.getId().toString())) // Newest first
                .andExpect(jsonPath("$.content[1].id").value(issue1.getId().toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void shouldListIssuesWithCustomPagination() throws Exception {
        for (int i = 1; i <= 5; i++) {
            issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Issue " + i, "Desc " + i);
        }

        mockMvc.perform(get("/api/issues?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void shouldClampMaxPageSizeTo100() throws Exception {
        mockMvc.perform(get("/api/issues?size=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void shouldFilterIssuesByCategory() throws Exception {
        CategoryEntity waterCategory = categoryRepository.save(new CategoryEntity("Water", "water", "Water issues", 2));

        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Road Issue", "Desc");
        issueService.createIssue(testUser.getId(), waterCategory.getId(), testLocation.getId(), "Water Pipe Leak", "Desc");

        mockMvc.perform(get("/api/issues?categoryId=" + waterCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Water Pipe Leak"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterIssuesByReporter() throws Exception {
        UserEntity otherUser = userRepository.save(new UserEntity("+919999999999", "Other User"));

        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "My Issue", "Desc");
        issueService.createIssue(otherUser.getId(), testCategory.getId(), testLocation.getId(), "Other's Issue", "Desc");

        mockMvc.perform(get("/api/issues?reportedBy=" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("My Issue"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterIssuesByStatus() throws Exception {
        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Reported Issue", "Desc");

        mockMvc.perform(get("/api/issues?status=REPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Reported Issue"));

        mockMvc.perform(get("/api/issues?status=RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ==========================================
    // 4. SECURITY & HYGIENE CHECKS
    // ==========================================

    @Test
    void shouldNeverExposeSensitiveUserFieldsInIssueResponses() throws Exception {
        IssueEntity issue = issueService.createIssue(
                testUser.getId(),
                testCategory.getId(),
                testLocation.getId(),
                "Public report",
                "No private info"
        );

        mockMvc.perform(get("/api/issues/{id}", issue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportedBy").value(testUser.getId().toString()))
                // Assert no phone number, password, or profile information leaked
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.rawGeometry").doesNotExist());
    }
}
