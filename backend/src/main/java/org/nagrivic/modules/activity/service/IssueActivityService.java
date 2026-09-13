package org.nagrivic.modules.activity.service;

import org.nagrivic.modules.activity.dto.ActivityResponse;
import org.nagrivic.modules.activity.entity.IssueActivityEntity;
import org.nagrivic.modules.activity.model.IssueActivityType;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface IssueActivityService {

    /**
     * Records a domain event in the append-only activity timeline using an issue ID.
     */
    IssueActivityEntity recordActivity(
            UUID issueId,
            IssueActivityType eventType,
            UserEntity actor,
            Map<String, Object> eventData
    );

    /**
     * Records a domain event in the append-only activity timeline using an existing IssueEntity.
     */
    IssueActivityEntity recordActivity(
            IssueEntity issue,
            IssueActivityType eventType,
            UserEntity actor,
            Map<String, Object> eventData
    );

    /**
     * Retrieves paginated, privacy-safe activity entries for an issue.
     * Enforces deterministic chronological sorting.
     */
    Page<ActivityResponse> getIssueActivities(UUID issueId, Pageable pageable);
}
