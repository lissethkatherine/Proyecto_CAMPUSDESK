package com.technova.helpdesk.dto;

import java.time.LocalDateTime;

public record StatusHistoryResponse(Long id, Long ticketId, String previousStatus,
                                    String newStatus, String changedByName,
                                    String changedByRole, LocalDateTime changedAt) {}
