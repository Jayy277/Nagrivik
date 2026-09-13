package org.nagrivic.modules.civicgeography.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CivicAreaResponse(
        String city,
        WardAreaSummary ward,
        CivicBodyAreaSummary civicBody,
        DepartmentAreaSummary department
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WardAreaSummary(
            String name,
            String number
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CivicBodyAreaSummary(
            String name
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DepartmentAreaSummary(
            String name
    ) {}
}
