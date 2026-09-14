package org.nagrivic.modules.accountability.dto;

import java.util.UUID;

public record DepartmentBreakdownDto(
        UUID departmentId,
        String name,
        String code,
        long totalAssigned,
        long openActionable,
        long resolved,
        long citizenVerified
) {}
