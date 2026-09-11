package org.nagrivic.modules.issues.service;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public IssueService(IssueRepository issueRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public IssueEntity createIssue(UUID reportedBy, String title, String description) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Issue title cannot be blank");
        }
        UserEntity reporter = userRepository.findById(reportedBy)
                .orElseThrow(() -> new IllegalArgumentException("Reporter user not found: " + reportedBy));

        IssueEntity issue = new IssueEntity(reporter, title.trim(), description);
        return issueRepository.save(issue);
    }

    public Optional<IssueEntity> findById(UUID id) {
        return issueRepository.findById(id);
    }

    public List<IssueEntity> findByReportedBy(UUID reportedBy) {
        return issueRepository.findByReporter_Id(reportedBy);
    }

    public List<IssueEntity> findByStatus(IssueStatus status) {
        return issueRepository.findByStatus(status);
    }
}
