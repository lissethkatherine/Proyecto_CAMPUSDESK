package com.technova.helpdesk.controller;

import com.technova.helpdesk.dto.StatusHistoryResponse;
import com.technova.helpdesk.service.StatusHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/history")
@Tag(name = "Historial")
public class HistoryController {

    private final StatusHistoryService historyService;

    public HistoryController(StatusHistoryService historyService) {
        this.historyService = historyService;
    }

    /** Solo lectura: no hay POST/PUT/DELETE a propósito. */
    @GetMapping
    @Operation(summary = "Consultar historial de cambios de estado")
    public List<StatusHistoryResponse> list(@PathVariable Long ticketId, Authentication auth) {
        return historyService.list(ticketId, auth);
    }
}
