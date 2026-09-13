package org.nagrivic.modules.comments;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.comments.dto.CreateCommentRequest;
import org.nagrivic.modules.comments.entity.CommentEntity;
import org.nagrivic.modules.comments.repository.CommentRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CommentApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private SupportRepository supportRepository;

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

    @Autowired
    private AuthSessionRepository authSessionRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizen1;
    private UserEntity citizen2;
    private UserEntity inactiveCitizen;
    private IssueEntity testIssue;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        commentRepository.deleteAll();
        supportRepository.deleteAll();
        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        userRepository.deleteAll();

        citizen1 = userRepository.save(new UserEntity("+919876543210", "Aarav Patel"));
        citizen2 = userRepository.save(new UserEntity("+919876543211", "Rohan Sharma"));

        inactiveCitizen = new UserEntity("+919876543212", "Inactive Citizen");
        inactiveCitizen.setActive(false);
        inactiveCitizen = userRepository.save(inactiveCitizen);

        CategoryEntity category = categoryRepository.save(new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1));
        LocationEntity location = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));

        testIssue = issueService.createIssue(
                citizen1,
                category.getId(),
                location.getId(),
                "Large pothole near SG Highway",
                "Deep pothole causing vehicle damage"
        );
    }

    private String tokenFor(UserEntity user) {
        return jwtService.generateAccessToken(user.getId(), user.getRole());
    }

    // ==========================================
    // 1. CREATION & VALIDATION TESTS
    // ==========================================

    @Test
    void authenticatedUserCanCreateComment() throws Exception {
        String token = tokenFor(citizen1);
        CreateCommentRequest request = new CreateCommentRequest("This road has been damaged for several weeks.");

        mockMvc.perform(post("/api/issues/{issueId}/comments", testIssue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.issueId", equalTo(testIssue.getId().toString())))
                .andExpect(jsonPath("$.content", equalTo("This road has been damaged for several weeks.")))
                .andExpect(jsonPath("$.author.id", equalTo(citizen1.getId().toString())))
                .andExpect(jsonPath("$.author.displayName", equalTo("Aarav Patel")))
                .andExpect(jsonPath("$.deleted", equalTo(false)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        assertEquals(1, commentRepository.count());
        CommentEntity saved = commentRepository.findAll().get(0);
        assertEquals(testIssue.getId(), saved.getIssue().getId());
        assertEquals(citizen1.getId(), saved.getUser().getId());
        assertEquals("This road has been damaged for several weeks.", saved.getContent());
        assertNull(saved.getDeletedAt());
    }

    @Test
    void emptyContentShouldBeRejected() throws Exception {
        String token = tokenFor(citizen1);
        CreateCommentRequest request = new CreateCommentRequest("");

        mockMvc.perform(post("/api/issues/{issueId}/comments", testIssue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whitespaceOnlyContentShouldBeRejected() throws Exception {
        String token = tokenFor(citizen1);
        CreateCommentRequest request = new CreateCommentRequest("     \n   \t  ");

        mockMvc.perform(post("/api/issues/{issueId}/comments", testIssue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contentExceeding1000CharactersShouldBeRejected() throws Exception {
        String token = tokenFor(citizen1);
        String longContent = "A".repeat(1001);
        CreateCommentRequest request = new CreateCommentRequest(longContent);

        mockMvc.perform(post("/api/issues/{issueId}/comments", testIssue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contentExactly1000CharactersAccepted() throws Exception {
        String token = tokenFor(citizen1);
        String valid1000 = "B".repeat(1000);
        CreateCommentRequest request = new CreateCommentRequest(valid1000);

        mockMvc.perform(post("/api/issues/{issueId}/comments", testIssue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content", equalTo(valid1000)));
    }

    // ==========================================
    // 2. AUTHENTICATION & LOOKUP TESTS
    // ==========================================

    @Test
    void unauthenticatedCommentCreationReturns401() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("Public comment attempt");

        mockMvc.perform(post("/api/issues/{issueId}/comments", testIssue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtCommentCreationReturns401() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("Tampered comment attempt");

        mockMvc.perform(post("/api/issues/{issueId}/comments", testIssue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer invalid-tampered-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inactiveUserCannotCreateComment() throws Exception {
        String token = tokenFor(inactiveCitizen);
        CreateCommentRequest request = new CreateCommentRequest("Inactive citizen comment");

        mockMvc.perform(post("/api/issues/{issueId}/comments", testIssue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo("FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("User account is inactive")));
    }

    @Test
    void commentOnNonExistentIssueReturns404() throws Exception {
        String token = tokenFor(citizen1);
        UUID nonExistentId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest("Ghost issue comment");

        mockMvc.perform(post("/api/issues/{issueId}/comments", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")));
    }

    // ==========================================
    // 3. READING & PAGINATION TESTS
    // ==========================================

    @Test
    void publicUserCanRetrieveCommentsInChronologicalOrder() throws Exception {
        // Create 3 comments
        CommentEntity c1 = commentRepository.save(new CommentEntity(testIssue, citizen1, "First comment"));
        CommentEntity c2 = commentRepository.save(new CommentEntity(testIssue, citizen2, "Second comment"));
        CommentEntity c3 = commentRepository.save(new CommentEntity(testIssue, citizen1, "Third comment"));

        // Retrieve unauthenticated
        mockMvc.perform(get("/api/issues/{issueId}/comments", testIssue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(3)))
                .andExpect(jsonPath("$.size", equalTo(20)))
                .andExpect(jsonPath("$.content[0].id", equalTo(c1.getId().toString())))
                .andExpect(jsonPath("$.content[0].content", equalTo("First comment")))
                .andExpect(jsonPath("$.content[0].author.displayName", equalTo("Aarav Patel")))
                .andExpect(jsonPath("$.content[1].id", equalTo(c2.getId().toString())))
                .andExpect(jsonPath("$.content[1].content", equalTo("Second comment")))
                .andExpect(jsonPath("$.content[2].id", equalTo(c3.getId().toString())))
                .andExpect(jsonPath("$.content[2].content", equalTo("Third comment")));
    }

    @Test
    void commentsSupportPaginationParameters() throws Exception {
        Instant baseTime = Instant.now().minusSeconds(300);
        for (int i = 1; i <= 25; i++) {
            CommentEntity comment = new CommentEntity(testIssue, citizen1, "Comment " + i);
            comment.setCreatedAt(baseTime.plusSeconds(i));
            commentRepository.save(comment);
        }

        // Page 0, size 10
        mockMvc.perform(get("/api/issues/{issueId}/comments", testIssue.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(25)))
                .andExpect(jsonPath("$.size", equalTo(10)))
                .andExpect(jsonPath("$.page", equalTo(0)))
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.content[0].content", equalTo("Comment 1")));

        // Page 1, size 10
        mockMvc.perform(get("/api/issues/{issueId}/comments", testIssue.getId())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", equalTo(1)))
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.content[0].content", equalTo("Comment 11")));
    }

    @Test
    void pageSizeIsClampedToMaximum100() throws Exception {
        commentRepository.save(new CommentEntity(testIssue, citizen1, "Only comment"));

        mockMvc.perform(get("/api/issues/{issueId}/comments", testIssue.getId())
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(100)));
    }

    // ==========================================
    // 4. SOFT DELETION TESTS
    // ==========================================

    @Test
    void authorCanSoftDeleteOwnComment() throws Exception {
        String token = tokenFor(citizen1);
        CommentEntity comment = commentRepository.save(new CommentEntity(testIssue, citizen1, "To be deleted"));

        mockMvc.perform(delete("/api/issues/{issueId}/comments/{commentId}", testIssue.getId(), comment.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Comment still exists in database
        CommentEntity inDb = commentRepository.findById(comment.getId()).orElseThrow();
        assertNotNull(inDb.getDeletedAt(), "deleted_at must be populated");
        assertEquals("To be deleted", inDb.getContent());

        // When retrieved publicly, content is masked as "[Comment deleted]" and deleted=true
        mockMvc.perform(get("/api/issues/{issueId}/comments", testIssue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", equalTo(comment.getId().toString())))
                .andExpect(jsonPath("$.content[0].content", equalTo("[Comment deleted]")))
                .andExpect(jsonPath("$.content[0].deleted", equalTo(true)))
                .andExpect(jsonPath("$.content[0].author.displayName", equalTo("Citizen")));
    }

    @Test
    void otherUserCannotDeleteSomeoneElsesComment() throws Exception {
        String token2 = tokenFor(citizen2);
        CommentEntity comment1 = commentRepository.save(new CommentEntity(testIssue, citizen1, "Citizen 1's comment"));

        mockMvc.perform(delete("/api/issues/{issueId}/comments/{commentId}", testIssue.getId(), comment1.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo("FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("not authorized to delete this comment")));

        CommentEntity inDb = commentRepository.findById(comment1.getId()).orElseThrow();
        assertNull(inDb.getDeletedAt());
    }

    @Test
    void deleteNonExistentCommentReturns404() throws Exception {
        String token = tokenFor(citizen1);
        UUID nonExistentCommentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/issues/{issueId}/comments/{commentId}", testIssue.getId(), nonExistentCommentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")));
    }

    // ==========================================
    // 5. ISSUE INTEGRATION & COMMENT COUNT TESTS
    // ==========================================

    @Test
    void issueDetailExposesActiveCommentCount() throws Exception {
        String token = tokenFor(citizen1);

        // Initially 0 comments
        mockMvc.perform(get("/api/issues/{id}", testIssue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount", equalTo(0)));

        // Create 2 comments
        CommentEntity c1 = commentRepository.save(new CommentEntity(testIssue, citizen1, "Comment 1"));
        commentRepository.save(new CommentEntity(testIssue, citizen2, "Comment 2"));

        mockMvc.perform(get("/api/issues/{id}", testIssue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount", equalTo(2)));

        // Soft-delete 1 comment
        mockMvc.perform(delete("/api/issues/{issueId}/comments/{commentId}", testIssue.getId(), c1.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Comment count should now be 1
        mockMvc.perform(get("/api/issues/{id}", testIssue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount", equalTo(1)));
    }

    @Test
    void issueListExposesActiveCommentCountAcrossIssues() throws Exception {
        // testIssue has 2 comments (1 deleted, 1 active)
        CommentEntity c1 = commentRepository.save(new CommentEntity(testIssue, citizen1, "Active"));
        CommentEntity c2 = new CommentEntity(testIssue, citizen2, "Deleted");
        c2.softDelete();
        commentRepository.save(c2);

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", equalTo(testIssue.getId().toString())))
                .andExpect(jsonPath("$.content[0].commentCount", equalTo(1)));
    }
}
