package org.nagrivic.modules.activity.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.activity.dto.ActivityResponse;
import org.nagrivic.modules.activity.entity.IssueActivityEntity;
import org.nagrivic.modules.activity.model.IssueActivityType;
import org.nagrivic.modules.activity.repository.IssueActivityRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class IssueActivityServiceImpl implements IssueActivityService {

    private static final Logger log = LoggerFactory.getLogger(IssueActivityServiceImpl.class);

    private final IssueActivityRepository issueActivityRepository;
    private final IssueRepository issueRepository;

    public IssueActivityServiceImpl(
            IssueActivityRepository issueActivityRepository,
            IssueRepository issueRepository
    ) {
        this.issueActivityRepository = issueActivityRepository;
        this.issueRepository = issueRepository;
    }

    @Override
    public IssueActivityEntity recordActivity(
            UUID issueId,
            IssueActivityType eventType,
            UserEntity actor,
            Map<String, Object> eventData
    ) {
        if (issueId == null) {
            throw new IllegalArgumentException("Issue ID cannot be null when recording activity");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Activity event type cannot be null");
        }

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        return recordActivity(issue, eventType, actor, eventData);
    }

    @Override
    public IssueActivityEntity recordActivity(
            IssueEntity issue,
            IssueActivityType eventType,
            UserEntity actor,
            Map<String, Object> eventData
    ) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue entity cannot be null when recording activity");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Activity event type cannot be null");
        }

        IssueActivityEntity activity = new IssueActivityEntity(issue, eventType, actor, eventData);
        IssueActivityEntity saved = issueActivityRepository.save(activity);

        log.debug("Recorded issue activity: issueId={}, eventType={}, actorId={}, activityId={}",
                issue.getId(), eventType, actor != null ? actor.getId() : "SYSTEM", saved.getId());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getIssueActivities(UUID issueId, Pageable pageable) {
        if (issueId == null) {
            throw new IllegalArgumentException("Issue ID cannot be null");
        }

        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue", issueId);
        }

        return issueActivityRepository.findByIssue_Id(issueId, pageable)
                .map(ActivityResponse::fromEntity);
    }
}
