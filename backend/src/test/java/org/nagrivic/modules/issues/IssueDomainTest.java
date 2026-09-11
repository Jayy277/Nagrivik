package org.nagrivic.modules.issues;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IssueDomainTest {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssueService issueService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        issueRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new UserEntity("+919876543210", "Aarav Patel");
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldCreateAndPersistIssueWithDefaultStatusReported() {
        IssueEntity issue = issueService.createIssue(
                testUser.getId(),
                "Large pothole near SG Highway",
                "Deep pothole in the left lane causing traffic slowdown."
        );

        assertNotNull(issue.getId());
        assertEquals("Large pothole near SG Highway", issue.getTitle());
        assertEquals("Deep pothole in the left lane causing traffic slowdown.", issue.getDescription());
        assertEquals(IssueStatus.REPORTED, issue.getStatus());
        assertEquals(testUser.getId(), issue.getReporter().getId());
        assertNotNull(issue.getCreatedAt());
        assertNotNull(issue.getUpdatedAt());
    }

    @Test
    void shouldFindIssuesByReporterId() {
        issueService.createIssue(testUser.getId(), "Issue 1", "Desc 1");
        issueService.createIssue(testUser.getId(), "Issue 2", "Desc 2");

        List<IssueEntity> userIssues = issueService.findByReportedBy(testUser.getId());
        assertEquals(2, userIssues.size());
    }

    @Test
    void shouldFindIssuesByStatus() {
        issueService.createIssue(testUser.getId(), "Pothole report", "Details");

        List<IssueEntity> reportedIssues = issueService.findByStatus(IssueStatus.REPORTED);
        assertFalse(reportedIssues.isEmpty());
        assertEquals("Pothole report", reportedIssues.get(0).getTitle());
    }

    @Test
    void shouldRejectIssueWithBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> {
            issueService.createIssue(testUser.getId(), "   ", "Description");
        });
    }

    @Test
    void shouldRejectIssueWithNonExistentUser() {
        UUID nonExistentUserId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> {
            issueService.createIssue(nonExistentUserId, "Valid Title", "Description");
        });
    }

    @Test
    void shouldPreventUserDeletionWhenIssuesExistDueToForeignKeyIntegrity() {
        issueService.createIssue(testUser.getId(), "Pothole on Ring Road", "Needs urgent repair");

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.delete(testUser);
            userRepository.flush();
        });
    }
}
