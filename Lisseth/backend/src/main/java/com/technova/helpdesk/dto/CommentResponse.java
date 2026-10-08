package com.technova.helpdesk.dto;

import java.time.LocalDateTime;

public record CommentResponse(Long id, String content, String authorName,
                              String authorRole, Long ticketId, LocalDateTime createdAt) {}
