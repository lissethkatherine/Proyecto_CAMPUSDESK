package com.roles.usermanagement.modules.comment;

import com.roles.usermanagement.modules.access.TicketAccess;
import com.roles.usermanagement.modules.ticket.Ticket;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

 private final TicketCommentRepository comments;
 private final TicketAccess access;

 public CommentService(TicketCommentRepository comments, TicketAccess access) {
  this.comments = comments;
  this.access = access;
 }

 private CommentResponse dto(TicketComment c) {
  return new CommentResponse(c.getId(), c.getTicket().getId(), c.getAuthor(), c.getContent(), c.getCreatedAt());
 }

 @Transactional(readOnly = true)
 public List<CommentResponse> list(Long ticketId, Authentication auth) {
  access.readable(ticketId, auth);
  return comments.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId).stream().map(this::dto).toList();
 }

 public CommentResponse add(Long ticketId, CommentRequest request, Authentication auth) {
  Ticket ticket = access.commentable(ticketId, auth);
  TicketComment comment = new TicketComment();
  comment.setTicket(ticket);
  comment.setAuthor(auth.getName()); // el autor sale de la sesión, nunca del cuerpo
  comment.setContent(request.content().trim());
  comment.setCreatedAt(LocalDateTime.now());
  return dto(comments.save(comment));
 }
}
