package com.technova.helpdesk.service;

import com.technova.helpdesk.dto.CommentRequest;
import com.technova.helpdesk.dto.CommentResponse;
import com.technova.helpdesk.entity.Comment;
import com.technova.helpdesk.entity.Ticket;
import com.technova.helpdesk.entity.User;
import com.technova.helpdesk.repository.CommentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketAccessService access;

    public CommentService(CommentRepository commentRepository, TicketAccessService access) {
        this.commentRepository = commentRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long ticketId, Authentication auth) {
        User user = access.currentUser(auth);
        Ticket ticket = access.findTicket(ticketId);
        access.assertCanView(user, ticket);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CommentResponse add(Long ticketId, CommentRequest request, Authentication auth) {
        User user = access.currentUser(auth);
        Ticket ticket = access.findTicket(ticketId);
        access.assertCanComment(user, ticket);

        Comment comment = new Comment();
        comment.setContent(request.content().trim());
        comment.setAuthor(user);      // el autor sale del token, NUNCA del body
        comment.setTicket(ticket);
        return toResponse(commentRepository.save(comment));
    }

    private CommentResponse toResponse(Comment c) {
        return new CommentResponse(c.getId(), c.getContent(), c.getAuthor().getFullName(),
                c.getAuthor().getRole().name(), c.getTicket().getId(), c.getCreatedAt());
    }
}
