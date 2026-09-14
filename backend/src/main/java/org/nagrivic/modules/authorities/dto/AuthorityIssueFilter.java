package org.nagrivic.modules.authorities.dto;

import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.priority.model.PriorityLevel;

import java.time.Instant;
import java.util.UUID;

public record AuthorityIssueFilter(
        IssueStatus status,
        PriorityLevel priority,
        UUID categoryId,
        UUID wardId,
        UUID departmentId,
        String search,
        Instant fromDate,
        Instant toDate,
        String sort
) {}
