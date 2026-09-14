package org.nagrivic.modules.media.ai.provider;

import java.util.UUID;

public record ImageUnderstandingRequest(
    UUID mediaId,
    byte[] imageBytes,
    String contentType,
    long fileSizeBytes,
    String issueTitle,
    String issueDescription,
    String issueCategorySlug,
    String issueCategoryName
) {}
