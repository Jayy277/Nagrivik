package org.nagrivic.modules.civicgeography.dto.admin;

import java.util.List;
import java.util.UUID;

public record WardBoundaryValidationResponse(
        int totalWardsChecked,
        int validBoundaries,
        int invalidBoundaries,
        int missingBoundaries,
        int duplicateGeometries,
        List<WardBoundaryIssue> issues
) {
    public record WardBoundaryIssue(
            UUID wardId,
            String wardNumber,
            String wardName,
            String issueType,
            String message
    ) {}
}
