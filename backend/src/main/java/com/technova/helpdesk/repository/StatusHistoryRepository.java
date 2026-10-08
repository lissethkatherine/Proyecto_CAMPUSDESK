package com.technova.helpdesk.repository;

import com.technova.helpdesk.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByTicketIdOrderByChangedAtAsc(Long ticketId);
}
