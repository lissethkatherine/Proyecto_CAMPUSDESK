package com.technova.helpdesk.service;

import com.technova.helpdesk.dto.StatusHistoryResponse;
import com.technova.helpdesk.entity.*;
import com.technova.helpdesk.repository.StatusHistoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatusHistoryService {

    private final StatusHistoryRepository historyRepository;
    private final TicketAccessService access;

    public StatusHistoryService(StatusHistoryRepository historyRepository, TicketAccessService access) {
        this.historyRepository = historyRepository;
        this.access = access;
    }

    /**
     * Se une a la transacción del llamador: si algo falla, se deshace TODO (RF-06).
     * previous puede ser null cuando se registra la creación del ticket.
     */
    @Transactional
    public void record(Ticket ticket, TicketStatus previous, TicketStatus next, User changedBy) {
        historyRepository.save(new StatusHistory(ticket, previous, next, changedBy));
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> list(Long ticketId, Authentication auth) {
        User user = access.currentUser(auth);
        Ticket ticket = access.findTicket(ticketId);
        access.assertCanView(user, ticket);
        return historyRepository.findByTicketIdOrderByChangedAtAsc(ticketId)
                .stream().map(this::toResponse).toList();
    }

    private StatusHistoryResponse toResponse(StatusHistory h) {
        return new StatusHistoryResponse(h.getId(), h.getTicket().getId(),
                h.getPreviousStatus() == null ? null : h.getPreviousStatus().name(),
                h.getNewStatus().name(), h.getChangedBy().getFullName(),
                h.getChangedBy().getRole().name(), h.getChangedAt());
    }
}
