package com.roles.usermanagement.modules.comment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@Tag(name = "Comentarios")
@SecurityRequirement(name = "bearerAuth")
public class CommentController {

 private final CommentService service;

 public CommentController(CommentService service) {
  this.service = service;
 }

 @Operation(
         summary = "Listar comentarios de un ticket",
         description = "Orden cronológico. Solo quien registró el ticket, el técnico asignado o quien tenga TICKET_VIEW_ALL."
 )
 @GetMapping
 @PreAuthorize("hasAuthority('COMMENT_READ')")
 public List<CommentResponse> all(@PathVariable Long ticketId, Authentication auth) {
  return service.list(ticketId, auth);
 }

 @Operation(
         summary = "Registrar un comentario",
         description = "El autor es el usuario autenticado. Un ticket CERRADA responde 409."
 )
 @PostMapping
 @PreAuthorize("hasAuthority('COMMENT_CREATE')")
 public ResponseEntity<CommentResponse> create(
         @PathVariable Long ticketId,
         @Valid @RequestBody CommentRequest dto,
         Authentication auth
 ) {
  return ResponseEntity.status(HttpStatus.CREATED).body(service.add(ticketId, dto, auth));
 }
}
