package com.roles.usermanagement.modules.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(max = 1000, message = "Máximo 1000 caracteres")
        String content
) {}
