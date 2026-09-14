package org.nagrivic.modules.civicgeography.dto.admin;

import java.util.UUID;

public record ReResolveRequest(
        UUID issueId,
        Boolean onlyUnresolved,
        Integer limit
) {}
