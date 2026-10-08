package com.technova.helpdesk.service;

import com.technova.helpdesk.dto.SummaryResponse;
import com.technova.helpdesk.entity.TicketStatus;
import com.technova.helpdesk.entity.User;
import com.technova.helpdesk.repository.ReportRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

@Service
public class ReportService {

    private final ReportRepository reports;
    private final TicketAccessService access;

    public ReportService(ReportRepository reports, TicketAccessService access) {
        this.reports = reports;
        this.access = access;
    }

    /** ADMIN: global. TECHNICIAN: sus asignados. USER: sus solicitudes. */
    @Transactional(readOnly = true)
    public SummaryResponse summary(Authentication auth) {
        User user = access.currentUser(auth);
        long total;
        Function<TicketStatus, Long> byStatus;
        switch (user.getRole()) {
            case ADMIN -> {
                total = reports.countAll();
                byStatus = reports::countByStatus;
            }
            case TECHNICIAN -> {
                total = reports.countAllByTechnician(user);
                byStatus = s -> reports.countByTechnicianAndStatus(user, s);
            }
            default -> {
                total = reports.countAllByRequester(user);
                byStatus = s -> reports.countByRequesterAndStatus(user, s);
            }
        }
        return new SummaryResponse(total,
                byStatus.apply(TicketStatus.ABIERTA),
                byStatus.apply(TicketStatus.ASIGNADA),
                byStatus.apply(TicketStatus.EN_PROCESO),
                byStatus.apply(TicketStatus.RESUELTA),
                byStatus.apply(TicketStatus.CERRADA));
    }
}
