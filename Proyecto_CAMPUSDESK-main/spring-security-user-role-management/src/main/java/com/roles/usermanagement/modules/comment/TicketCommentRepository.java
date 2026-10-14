package com.roles.usermanagement.modules.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

 List<TicketComment> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);
}
