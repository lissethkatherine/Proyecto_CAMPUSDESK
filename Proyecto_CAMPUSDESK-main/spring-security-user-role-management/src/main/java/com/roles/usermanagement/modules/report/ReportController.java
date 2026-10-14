package com.roles.usermanagement.modules.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reportes")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

 private final ReportService service;

 public ReportController(ReportService service) {
  this.service = service;
 }

 @Operation(
         summary = "Indicadores de tickets por estado",
         description = "Con TICKET_VIEW_ALL se cuentan todos los tickets; sin él, solo los propios o asignados."
 )
 @GetMapping("/summary")
 @PreAuthorize("hasAuthority('REPORT_READ')")
 public SummaryResponse summary(Authentication auth) {
  return service.summary(auth);
 }
}
