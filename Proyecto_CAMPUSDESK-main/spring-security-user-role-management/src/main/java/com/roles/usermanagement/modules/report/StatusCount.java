package com.roles.usermanagement.modules.report;

import com.roles.usermanagement.modules.ticket.TicketStatus;

public record StatusCount(TicketStatus status, Long total) {}
