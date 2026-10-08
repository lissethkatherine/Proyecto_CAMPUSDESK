package com.technova.helpdesk.controller;

import com.technova.helpdesk.dto.CommentRequest;
import com.technova.helpdesk.dto.CommentResponse;
import com.technova.helpdesk.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@Tag(name = "Comentarios")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "Obtener comentarios de un ticket (orden cronológico)")
    public List<CommentResponse> list(@PathVariable Long ticketId, Authentication auth) {
        return commentService.list(ticketId, auth);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar comentario")
    public CommentResponse add(@PathVariable Long ticketId,
                               @Valid @RequestBody CommentRequest request,
                               Authentication auth) {
        return commentService.add(ticketId, request, auth);
    }
}
