package org.nagrivic.modules.departments.dto;

import java.util.UUID;

public record DepartmentSummaryDto(
        UUID id,
        String name,
        String code
) {
}
