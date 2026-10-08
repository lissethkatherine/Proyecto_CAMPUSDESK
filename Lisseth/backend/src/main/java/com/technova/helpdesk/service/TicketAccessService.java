package com.technova.helpdesk.service;

import com.technova.helpdesk.entity.*;
import com.technova.helpdesk.repository.TicketRepository;
import com.technova.helpdesk.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Reglas de "quién puede ver / comentar" un ticket. Las usan comentarios e historial. */
@Service
public class TicketAccessService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public TicketAccessService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    
    public User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    public Ticket findTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    }

    /** Ver comentarios / historial. */
    public void assertCanView(User user, Ticket ticket) {
        if (!canAccess(user, ticket)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este ticket");
        }
    }

    /** Comentar: mismo acceso + el ticket no puede estar CERRADA */
    public void assertCanComment(User user, Ticket ticket) {
        assertCanView(user, ticket);
        if (ticket.getStatus() == TicketStatus.CERRADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un ticket CERRADA no admite comentarios");
        }
    }

    private boolean canAccess(User user, Ticket ticket) {
        return switch (user.getRole()) {
            case ADMIN -> true;
            case TECHNICIAN -> ticket.getTechnician() != null
                    && ticket.getTechnician().getId().equals(user.getId());
            case USER -> ticket.getRequester().getId().equals(user.getId());
        };
    }
}
