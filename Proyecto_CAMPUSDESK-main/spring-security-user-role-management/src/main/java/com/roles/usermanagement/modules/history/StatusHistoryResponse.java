package com.roles.usermanagement.modules.history;

import java.time.LocalDateTime;

public record StatusHistoryResponse(
        Long id,
        Long ticketId,
        String previousStatus,
        String newStatus,
        String changedBy,
        LocalDateTime changedAt
) {}
