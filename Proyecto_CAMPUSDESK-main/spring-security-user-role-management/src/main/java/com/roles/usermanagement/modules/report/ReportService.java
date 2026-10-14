package com.roles.usermanagement.modules.report;

import com.roles.usermanagement.modules.access.TicketAccess;
import com.roles.usermanagement.modules.ticket.TicketStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

 private final ReportRepository reports;
 private final TicketAccess access;

 public ReportService(ReportRepository reports, TicketAccess access) {
  this.reports = reports;
  this.access = access;
 }

 /** Con TICKET_VIEW_ALL cuenta todos los tickets; si no, solo los registrados por o asignados al usuario. */
 public SummaryResponse summary(Authentication auth) {
  List<StatusCount> rows = access.viewsAll(auth)
          ? reports.countAllByStatus()
          : reports.countVisibleByStatus(auth.getName());
  Map<TicketStatus, Long> counts = new EnumMap<>(TicketStatus.class);
  rows.forEach(r -> counts.put(r.status(), r.total()));
  long total = counts.values().stream().mapToLong(Long::longValue).sum();
  return new SummaryResponse(
          total,
          counts.getOrDefault(TicketStatus.ABIERTA, 0L),
          counts.getOrDefault(TicketStatus.ASIGNADA, 0L),
          counts.getOrDefault(TicketStatus.EN_PROCESO, 0L),
          counts.getOrDefault(TicketStatus.RESUELTA, 0L),
          counts.getOrDefault(TicketStatus.CERRADA, 0L));
 }
}
