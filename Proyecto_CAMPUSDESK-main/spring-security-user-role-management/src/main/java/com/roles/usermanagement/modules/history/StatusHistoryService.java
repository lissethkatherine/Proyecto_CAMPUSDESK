package com.roles.usermanagement.modules.history;

import com.roles.usermanagement.modules.access.TicketAccess;
import com.roles.usermanagement.modules.ticket.Ticket;
import com.roles.usermanagement.modules.ticket.TicketStatus;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StatusHistoryService {

 private final StatusHistoryRepository history;
 private final TicketAccess access;

 public StatusHistoryService(StatusHistoryRepository history, TicketAccess access) {
  this.history = history;
  this.access = access;
 }

 /**
  * Registra un cambio de estado. Debe invocarse desde el mismo método transaccional
  * que crea, asigna o cambia el estado del ticket: si el registro falla, se revierte todo.
  * previous es null al registrar la creación.
  */
 public void record(Ticket ticket, TicketStatus previous, TicketStatus next, String actor) {
  history.save(new StatusHistory(ticket, previous, next, actor));
 }

 @Transactional(readOnly = true)
 public List<StatusHistoryResponse> list(Long ticketId, Authentication auth) {
  access.readable(ticketId, auth);
  return history.findByTicketIdOrderByChangedAtAscIdAsc(ticketId).stream()
          .map(h -> new StatusHistoryResponse(
                  h.getId(),
                  h.getTicket().getId(),
                  h.getPreviousStatus() == null ? null : h.getPreviousStatus().name(),
                  h.getNewStatus().name(),
                  h.getChangedBy(),
                  h.getChangedAt()))
          .toList();
 }
}
