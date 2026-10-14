package com.roles.usermanagement.modules.history;

import com.roles.usermanagement.modules.ticket.Ticket;
import com.roles.usermanagement.modules.ticket.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Registro inmutable: sin setters ni endpoints de edición o borrado. */
@Entity
@Table(name = "status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @ManyToOne(optional = false)
 @JoinColumn(name = "ticket_id", nullable = false)
 private Ticket ticket;

 /** Null en el registro de creación del ticket. */
 @Enumerated(EnumType.STRING)
 @Column(length = 20)
 private TicketStatus previousStatus;

 @Enumerated(EnumType.STRING)
 @Column(nullable = false, length = 20)
 private TicketStatus newStatus;

 @Column(nullable = false, length = 50)
 private String changedBy;

 @Column(nullable = false)
 private LocalDateTime changedAt;

 public StatusHistory(Ticket ticket, TicketStatus previousStatus, TicketStatus newStatus, String changedBy) {
  this.ticket = ticket;
  this.previousStatus = previousStatus;
  this.newStatus = newStatus;
  this.changedBy = changedBy;
  this.changedAt = LocalDateTime.now();
 }
}
