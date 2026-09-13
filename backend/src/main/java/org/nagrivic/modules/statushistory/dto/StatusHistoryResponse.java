package org.nagrivic.modules.statushistory.dto;

import java.util.List;
import java.util.UUID;

public record StatusHistoryResponse(
    UUID issueId,
    List<StatusHistoryItemDto> history
) {}
