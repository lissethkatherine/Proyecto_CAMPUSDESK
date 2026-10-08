package com.technova.helpdesk.repository;

import com.technova.helpdesk.entity.Ticket;
import com.technova.helpdesk.entity.TicketStatus;
import com.technova.helpdesk.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Consultas de conteo */
public interface ReportRepository extends Repository<Ticket, Long> {

    @Query("select count(t) from Ticket t")
    long countAll();

    @Query("select count(t) from Ticket t where t.status = :status")
    long countByStatus(@Param("status") TicketStatus status);

    @Query("select count(t) from Ticket t where t.requester = :user")
    long countAllByRequester(@Param("user") User user);

    @Query("select count(t) from Ticket t where t.requester = :user and t.status = :status")
    long countByRequesterAndStatus(@Param("user") User user, @Param("status") TicketStatus status);

    @Query("select count(t) from Ticket t where t.technician = :user")
    long countAllByTechnician(@Param("user") User user);

    @Query("select count(t) from Ticket t where t.technician = :user and t.status = :status")
    long countByTechnicianAndStatus(@Param("user") User user, @Param("status") TicketStatus status);
}
