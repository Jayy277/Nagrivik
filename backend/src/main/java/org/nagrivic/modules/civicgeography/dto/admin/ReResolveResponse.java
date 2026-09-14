package org.nagrivic.modules.civicgeography.dto.admin;

import java.util.List;

public record ReResolveResponse(
        int totalProcessed,
        int resolvedCount,
        int unresolvedCount,
        int failureCount,
        List<String> messages
) {}
