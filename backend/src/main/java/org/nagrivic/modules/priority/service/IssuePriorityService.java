package org.nagrivic.modules.priority.service;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.priority.entity.IssuePriorityEntity;

import java.util.UUID;

/**
 * Service responsible for calculating, updating, and auditing civic priority scores and levels.
 * The priority engine is 100% server-controlled, deterministic, and explainable.
 */
public interface IssuePriorityService {

    /**
     * Calculates and persists the civic priority for the given issue.
     * If the issue is a duplicate, operates on the canonical primary issue.
     *
     * @param issue the issue entity
     * @return the persisted or updated IssuePriorityEntity
     */
    IssuePriorityEntity calculatePriority(IssueEntity issue);

    /**
     * Recalculates and persists civic priority for the issue specified by ID.
     * Updates the existing priority record idempotently without creating duplicates.
     *
     * @param issueId the UUID of the issue
     * @return the updated IssuePriorityEntity
     */
    IssuePriorityEntity recalculatePriority(UUID issueId);
}
