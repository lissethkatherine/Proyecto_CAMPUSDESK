package com.roles.usermanagement.modules.comment;

import com.roles.usermanagement.modules.ticket.Ticket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ticket_comment")
@Getter
@Setter
public class TicketComment {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @ManyToOne(optional = false)
 @JoinColumn(name = "ticket_id", nullable = false)
 private Ticket ticket;

 @Column(nullable = false, length = 50)
 private String author;

 @Column(nullable = false, length = 1000)
 private String content;

 @Column(nullable = false)
 private LocalDateTime createdAt;
}
