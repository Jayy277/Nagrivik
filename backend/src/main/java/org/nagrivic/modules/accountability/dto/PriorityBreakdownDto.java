package org.nagrivic.modules.accountability.dto;

public record PriorityBreakdownDto(
        long low,
        long medium,
        long high,
        long critical
) {}
