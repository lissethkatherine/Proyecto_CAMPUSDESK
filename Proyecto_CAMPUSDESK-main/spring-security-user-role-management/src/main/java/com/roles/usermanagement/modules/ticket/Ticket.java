package com.roles.usermanagement.modules.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tickets")
@Getter
@Setter
public class Ticket {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(nullable = false, length = 200)
 private String title;

 @Enumerated(EnumType.STRING)
 @Column(nullable = false, length = 20)
 private TicketStatus status = TicketStatus.ABIERTA;

 /** Username de quien registró el ticket. */
 @Column(nullable = false, length = 50)
 private String createdBy;

 /** Username del técnico asignado; null mientras no tenga. */
 @Column(length = 50)
 private String assignedTo;

 @Column(nullable = false)
 private LocalDateTime createdAt;
}
