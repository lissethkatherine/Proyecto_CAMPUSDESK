package com.technova.helpdesk.controller;

import com.technova.helpdesk.dto.SummaryResponse;
import com.technova.helpdesk.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reportes")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Indicadores del dashboard (según el rol)")
    public SummaryResponse summary(Authentication auth) {
        return reportService.summary(auth);
    }
}
