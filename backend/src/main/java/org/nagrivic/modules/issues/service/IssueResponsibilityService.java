package org.nagrivic.modules.issues.service;

import org.nagrivic.modules.issues.entity.IssueEntity;

import java.util.UUID;

/**
 * Service responsible for resolving and enriching civic responsibility context for civic issues.
 * All responsibility assignments are server-controlled and deterministic.
 */
public interface IssueResponsibilityService {

    /**
     * Resolves civic responsibility (City, Civic Body, Ward, Department) for the given issue.
     * If authoritative geography or mapping data is missing, the issue is marked UNRESOLVED
     * without failing issue creation.
     *
     * @param issue the issue entity to enrich
     */
    void resolveResponsibility(IssueEntity issue);

    /**
     * Re-evaluates and persists responsibility resolution for an existing issue.
     * Used when new ward boundary geography or department mappings become available.
     *
     * @param issueId the ID of the issue to re-resolve
     */
    void resolveIssueResponsibility(UUID issueId);
}
