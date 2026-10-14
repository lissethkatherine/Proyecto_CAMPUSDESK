package com.roles.usermanagement.modules.report;

public record SummaryResponse(
        long total,
        long open,
        long assigned,
        long inProgress,
        long resolved,
        long closed
) {}
