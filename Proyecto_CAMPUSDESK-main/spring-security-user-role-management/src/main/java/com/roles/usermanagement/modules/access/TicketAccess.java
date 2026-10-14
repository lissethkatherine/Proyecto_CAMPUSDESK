package com.roles.usermanagement.modules.access;

import com.roles.usermanagement.modules.ticket.Ticket;
import com.roles.usermanagement.modules.ticket.TicketRepository;
import com.roles.usermanagement.modules.ticket.TicketStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas de acceso por ticket. Un usuario accede si tiene el permiso global
 * TICKET_VIEW_ALL, si registró el ticket o si es el técnico asignado.
 */
@Component
public class TicketAccess {

 public static final String VIEW_ALL = "TICKET_VIEW_ALL";

 private final TicketRepository tickets;

 public TicketAccess(TicketRepository tickets) {
  this.tickets = tickets;
 }

 public boolean viewsAll(Authentication auth) {
  return auth.getAuthorities().stream().anyMatch(a -> VIEW_ALL.equals(a.getAuthority()));
 }

 /** Ticket al que el usuario puede acceder; 404 si no existe y 403 si no tiene acceso. */
 public Ticket readable(Long ticketId, Authentication auth) {
  Ticket ticket = tickets.findById(ticketId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
  String user = auth.getName();
  boolean allowed = viewsAll(auth) || user.equals(ticket.getCreatedBy()) || user.equals(ticket.getAssignedTo());
  if (!allowed) {
   throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este ticket");
  }
  return ticket;
 }

 /** Igual que readable, pero un ticket CERRADA no admite comentarios (409). */
 public Ticket commentable(Long ticketId, Authentication auth) {
  Ticket ticket = readable(ticketId, auth);
  if (ticket.getStatus() == TicketStatus.CERRADA) {
   throw new ResponseStatusException(HttpStatus.CONFLICT, "Un ticket CERRADA no admite comentarios");
  }
  return ticket;
 }
}
