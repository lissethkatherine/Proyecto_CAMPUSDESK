package com.roles.usermanagement.modules.history;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

 List<StatusHistory> findByTicketIdOrderByChangedAtAscIdAsc(Long ticketId);
}
