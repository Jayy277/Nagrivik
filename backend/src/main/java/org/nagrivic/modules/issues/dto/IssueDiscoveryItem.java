package org.nagrivic.modules.issues.dto;

import java.util.UUID;

/**
 * Projected lightweight item from dynamic discovery query with optional calculated distance.
 */
public record IssueDiscoveryItem(
        UUID issueId,
        Double distanceMeters
) {}
