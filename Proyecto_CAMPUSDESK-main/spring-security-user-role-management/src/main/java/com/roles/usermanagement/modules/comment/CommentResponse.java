package com.roles.usermanagement.modules.comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long ticketId,
        String author,
        String content,
        LocalDateTime createdAt
) {}
