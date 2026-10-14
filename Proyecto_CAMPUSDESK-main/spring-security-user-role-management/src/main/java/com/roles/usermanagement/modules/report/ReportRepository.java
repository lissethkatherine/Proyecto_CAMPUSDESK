package com.roles.usermanagement.modules.report;

import com.roles.usermanagement.modules.ticket.Ticket;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends Repository<Ticket, Long> {

 @Query("select new com.roles.usermanagement.modules.report.StatusCount(t.status, count(t)) "
         + "from Ticket t group by t.status")
 List<StatusCount> countAllByStatus();

 @Query("select new com.roles.usermanagement.modules.report.StatusCount(t.status, count(t)) "
         + "from Ticket t where t.createdBy = :username or t.assignedTo = :username group by t.status")
 List<StatusCount> countVisibleByStatus(@Param("username") String username);
}
