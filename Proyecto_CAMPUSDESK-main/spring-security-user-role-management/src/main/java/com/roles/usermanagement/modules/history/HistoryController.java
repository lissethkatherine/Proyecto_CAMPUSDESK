package com.roles.usermanagement.modules.history;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{ticketId}/history")
@Tag(name = "Historial")
@SecurityRequirement(name = "bearerAuth")
public class HistoryController {

 private final StatusHistoryService service;

 public HistoryController(StatusHistoryService service) {
  this.service = service;
 }

 @Operation(
         summary = "Consultar el historial de estados de un ticket",
         description = "Solo lectura, en orden cronológico. Mismas reglas de acceso que los comentarios."
 )
 @GetMapping
 @PreAuthorize("hasAuthority('HISTORY_READ')")
 public List<StatusHistoryResponse> all(@PathVariable Long ticketId, Authentication auth) {
  return service.list(ticketId, auth);
 }
}
