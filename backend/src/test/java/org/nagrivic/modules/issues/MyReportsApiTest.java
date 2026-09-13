package org.nagrivic.modules.issues;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.moderation.model.ModerationStatus;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MyReportsApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

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

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizenA;
    private String tokenA;

    private UserEntity citizenB;
    private String tokenB;

    private CategoryEntity roadCategory;
    private CategoryEntity waterCategory;
    private LocationEntity testLocation;

    private UserEntity createUniqueUser(String name) {
        long uniqueNum = Math.abs(ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L));
        UserEntity u = new UserEntity("+91" + uniqueNum, name);
        u.setRole("CITIZEN");
        return userRepository.save(u);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        citizenA = createUniqueUser("Citizen Aarav");
        tokenA = jwtService.generateAccessToken(citizenA.getId(), citizenA.getRole());

        citizenB = createUniqueUser("Citizen Priya");
        tokenB = jwtService.generateAccessToken(citizenB.getId(), citizenB.getRole());

        roadCategory = categoryRepository.save(new CategoryEntity(
                "Roads / Potholes",
                "roads-potholes-" + System.nanoTime(),
                "Pothole issues",
                1
        ));

        waterCategory = categoryRepository.save(new CategoryEntity(
                "Water Supply",
                "water-supply-" + System.nanoTime(),
                "Water supply issues",
                2
        ));

        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("1. Unauthenticated request to GET /api/issues/my returns 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/issues/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. User A sees only User A's reported issues and User B sees only User B's reports")
    void userSeesOnlyTheirOwnReports() throws Exception {
        // Create 2 issues for Citizen A
        issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "A Pothole 1", "Description 1");
        issueService.createIssue(citizenA, waterCategory.getId(), testLocation.getId(), "A Water Leak 2", "Description 2");

        // Create 1 issue for Citizen B
        issueService.createIssue(citizenB, roadCategory.getId(), testLocation.getId(), "B Garbage Pile", "Description B");

        // Citizen A queries /api/issues/my
        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].title", containsInAnyOrder("A Pothole 1", "A Water Leak 2")))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("B Garbage Pile"))));

        // Citizen B queries /api/issues/my
        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("B Garbage Pile"));
    }

    @Test
    @DisplayName("3. Citizen A cannot access Citizen B reports by passing reportedBy query parameter")
    void reportedByParamCannotImpersonateAnotherCitizen() throws Exception {
        // Citizen B has an issue
        issueService.createIssue(citizenB, roadCategory.getId(), testLocation.getId(), "Citizen B Secret Issue", "Description");

        // Citizen A tries to send reportedBy=citizenB.getId()
        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("reportedBy", citizenB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", empty()));
    }

    @Test
    @DisplayName("4. Pagination works correctly on /api/issues/my")
    void paginationWorks() throws Exception {
        for (int i = 1; i <= 5; i++) {
            issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "Issue #" + i, "Desc");
        }

        // Request page 0, size 2
        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Request page 2, size 2 (1 remaining item)
        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("5. Status filter works on /api/issues/my")
    void statusFilterWorks() throws Exception {
        IssueEntity issue1 = issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "Issue 1", "Desc");
        IssueEntity issue2 = issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "Issue 2", "Desc");

        // Update issue1 to VERIFIED
        issue1.setStatus(IssueStatus.VERIFIED);
        issueRepository.save(issue1);

        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(issue1.getId().toString()))
                .andExpect(jsonPath("$.content[0].status").value("VERIFIED"));
    }

    @Test
    @DisplayName("6. Category filter works on /api/issues/my")
    void categoryFilterWorks() throws Exception {
        issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "Road Issue", "Desc");
        issueService.createIssue(citizenA, waterCategory.getId(), testLocation.getId(), "Water Issue", "Desc");

        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("categoryId", waterCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Water Issue"))
                .andExpect(jsonPath("$.content[0].category.id").value(waterCategory.getId().toString()));
    }

    @Test
    @DisplayName("7. Duplicate-linked issues are included for the reporting citizen")
    void duplicateLinkedIssuesAreIncluded() throws Exception {
        IssueEntity primary = issueService.createIssue(citizenB, roadCategory.getId(), testLocation.getId(), "Primary Report", "Desc");
        IssueEntity duplicate = issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "My Duplicate Report", "Desc");

        // Link citizenA's report as duplicate of primary
        duplicate.setDuplicateOf(primary);
        duplicate.setStatus(IssueStatus.RESOLVED);
        issueRepository.save(duplicate);

        // Citizen A MUST see their own duplicate-linked report
        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(duplicate.getId().toString()))
                .andExpect(jsonPath("$.content[0].isDuplicate").value(true))
                .andExpect(jsonPath("$.content[0].primaryIssueId").value(primary.getId().toString()));
    }

    @Test
    @DisplayName("8. Hidden/moderated issues are excluded according to moderation policy")
    void hiddenModeratedIssuesExcluded() throws Exception {
        IssueEntity normalIssue = issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "Clean Issue", "Desc");
        IssueEntity abusiveIssue = issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "Abusive Issue", "Desc");

        abusiveIssue.setModerationStatus(ModerationStatus.HIDDEN);
        issueRepository.save(abusiveIssue);

        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Clean Issue"))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Abusive Issue"))));
    }

    @Test
    @DisplayName("9. Issue response DTO does not leak private user data")
    void responseDtoDoesNotLeakPii() throws Exception {
        issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "Pothole", "Desc");

        mockMvc.perform(get("/api/issues/my")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Pothole"))
                .andExpect(jsonPath("$.content[*].password").doesNotExist())
                .andExpect(jsonPath("$.content[*].phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.content[*].email").doesNotExist());
    }

    @Test
    @DisplayName("10. Public GET /api/issues remains open and functional")
    void publicIssueFeedRemainsIntact() throws Exception {
        issueService.createIssue(citizenA, roadCategory.getId(), testLocation.getId(), "Public Issue", "Desc");

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())));
    }
}
