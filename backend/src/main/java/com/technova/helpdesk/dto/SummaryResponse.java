package com.technova.helpdesk.dto;

/** "assigned" se incluye porque el flujo tiene el estado ASIGNADA. */
public record SummaryResponse(long total, long open, long assigned,
                              long inProgress, long resolved, long closed) {}
