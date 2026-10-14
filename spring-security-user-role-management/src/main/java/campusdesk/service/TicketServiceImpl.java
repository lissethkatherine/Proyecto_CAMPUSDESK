package campusdesk.service;

import campusdesk.dto.CreateTicketDTO;
import campusdesk.dto.TicketResponseDTO;
import campusdesk.dto.UserResponseDTO;
import campusdesk.entity.Role;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketCategory;
import campusdesk.entity.TicketHistory;
import campusdesk.entity.TicketPriority;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.User;
import campusdesk.exception.InvalidStateTransitionException;
import campusdesk.exception.TicketClosedException;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.repository.TicketHistoryRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de la lógica de negocio y máquina de estados para tickets (RF-03, RF-04, RF-06).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TicketResponseDTO createTicket(CreateTicketDTO dto, String userEmail) {
        User solicitante = findUserByEmail(userEmail);

        // Todo nuevo ticket inicia estrictamente en ABIERTA
        Ticket ticket = Ticket.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .solicitante(solicitante)
                .tecnico(null)
                .status(TicketStatus.ABIERTA)
                .priority(dto.getPriority())
                .category(dto.getCategory())
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        // Registro de auditoría inicial
        TicketHistory history = TicketHistory.builder()
                .ticket(savedTicket)
                .estadoAnterior(null)
                .estadoNuevo(TicketStatus.ABIERTA)
                .autor(solicitante)
                .comentarioTransicion("Creación de incidencia por el solicitante")
                .build();
        ticketHistoryRepository.save(history);

        log.info("Ticket #{} creado exitosamente por [{}] con estado ABIERTA", savedTicket.getId(), userEmail);
        return mapToResponse(savedTicket);
    }

    @Override
    @Transactional
    public TicketResponseDTO assignTechnician(Long ticketId, Long tecnicoId, String adminEmail) {
        User admin = findUserByEmail(adminEmail);

        // Solo ADMIN puede asignar técnicos
        if (admin.getRole() != Role.ADMIN) {
            throw new UnauthorizedTicketActionException("Solo un administrador puede asignar técnicos a una incidencia");
        }

        Ticket ticket = findTicketById(ticketId);

        // Ticket cerrado es inmutable
        if (ticket.getStatus() == TicketStatus.CERRADA) {
            throw new TicketClosedException("No se puede modificar una incidencia en estado CERRADA");
        }

        User tecnico = userRepository.findById(tecnicoId)
                .orElseThrow(() -> new IllegalArgumentException("El técnico especificado no existe"));

        if (tecnico.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("El usuario seleccionado no posee el rol TECHNICIAN");
        }

        TicketStatus estadoAnterior = ticket.getStatus();
        ticket.setTecnico(tecnico);
        ticket.setStatus(TicketStatus.ASIGNADA);
        Ticket updatedTicket = ticketRepository.save(ticket);

        // Auditoría automática
        TicketHistory history = TicketHistory.builder()
                .ticket(updatedTicket)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(TicketStatus.ASIGNADA)
                .autor(admin)
                .comentarioTransicion("Técnico asignado: " + tecnico.getName() + " (" + tecnico.getEmail() + ")")
                .build();
        ticketHistoryRepository.save(history);

        log.info("Ticket #{} asignado al técnico [{}] por el administrador [{}]", ticketId, tecnico.getEmail(), adminEmail);
        return mapToResponse(updatedTicket);
    }

    @Override
    @Transactional
    public TicketResponseDTO updateStatus(Long ticketId, TicketStatus newStatus, String comentario, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = findTicketById(ticketId);

        TicketStatus estadoActual = ticket.getStatus();

        // 1. Inmutabilidad de tickets cerrados
        if (estadoActual == TicketStatus.CERRADA) {
            throw new TicketClosedException("No se puede modificar una incidencia en estado CERRADA");
        }

        // Si el estado es el mismo, no hay transición
        if (estadoActual == newStatus) {
            return mapToResponse(ticket);
        }

        // 2. Validación de la Máquina de Estados (RF-04)
        switch (newStatus) {
            case EN_PROCESO -> {
                // Solo desde ASIGNADA
                if (estadoActual != TicketStatus.ASIGNADA) {
                    throw new InvalidStateTransitionException("Solo se puede pasar a EN_PROCESO desde el estado ASIGNADA");
                }
                // Solo el técnico asignado (o admin)
                if (!isAssignedTechnician(ticket, user) && user.getRole() != Role.ADMIN) {
                    throw new UnauthorizedTicketActionException("Solo el técnico asignado puede iniciar la atención del ticket");
                }
            }
            case RESUELTA -> {
                // Solo si estuvo previamente EN_PROCESO
                if (estadoActual != TicketStatus.EN_PROCESO) {
                    throw new InvalidStateTransitionException("Una incidencia solo puede marcarse como RESUELTA si previamente estuvo EN_PROCESO");
                }
                // Solo el técnico asignado (o admin)
                if (!isAssignedTechnician(ticket, user) && user.getRole() != Role.ADMIN) {
                    throw new UnauthorizedTicketActionException("Solo el técnico asignado puede marcar la incidencia como RESUELTA");
                }
            }
            case CERRADA -> {
                // Solo desde RESUELTA
                if (estadoActual != TicketStatus.RESUELTA) {
                    throw new InvalidStateTransitionException("Solo se puede cerrar una incidencia que se encuentre en estado RESUELTA");
                }
                // Solo el usuario solicitante (o admin)
                if (!isRequester(ticket, user) && user.getRole() != Role.ADMIN) {
                    throw new UnauthorizedTicketActionException("Solo el usuario solicitante puede cerrar un ticket en estado RESUELTA");
                }
            }
            default -> throw new InvalidStateTransitionException(
                    "Transición de estado no permitida de " + estadoActual + " a " + newStatus);
        }

        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);

        // Registro de Auditoría
        String transicionDesc = comentario != null && !comentario.isBlank()
                ? comentario
                : "Cambio de estado de " + estadoActual + " a " + newStatus;

        TicketHistory history = TicketHistory.builder()
                .ticket(updatedTicket)
                .estadoAnterior(estadoActual)
                .estadoNuevo(newStatus)
                .autor(user)
                .comentarioTransicion(transicionDesc)
                .build();
        ticketHistoryRepository.save(history);

        log.info("Ticket #{} cambió de [{}] a [{}] por [{}]", ticketId, estadoActual, newStatus, userEmail);
        return mapToResponse(updatedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketById(Long ticketId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = findTicketById(ticketId);

        // Validar acceso: ADMIN puede ver todo; USER solo sus propios tickets; TECHNICIAN asignados o todos en soporte
        if (user.getRole() == Role.USER && !isRequester(ticket, user)) {
            throw new UnauthorizedTicketActionException("No tiene permisos para consultar este ticket");
        }

        return mapToResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getTickets(TicketStatus status, TicketPriority priority, TicketCategory category, String userEmail) {
        User user = findUserByEmail(userEmail);

        Long solicitanteFilter = null;
        Long tecnicoFilter = null;

        if (user.getRole() == Role.USER) {
            solicitanteFilter = user.getId();
        } else if (user.getRole() == Role.TECHNICIAN) {
            tecnicoFilter = user.getId();
        }

        List<Ticket> tickets = ticketRepository.findWithFilters(status, priority, category, solicitanteFilter, tecnicoFilter);
        return tickets.stream().map(this::mapToResponse).toList();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }

    private Ticket findTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + id));
    }

    private boolean isAssignedTechnician(Ticket ticket, User user) {
        return ticket.getTecnico() != null && ticket.getTecnico().getId().equals(user.getId());
    }

    private boolean isRequester(Ticket ticket, User user) {
        return ticket.getSolicitante() != null && ticket.getSolicitante().getId().equals(user.getId());
    }

    private TicketResponseDTO mapToResponse(Ticket ticket) {
        UserResponseDTO solicitanteDto = ticket.getSolicitante() != null ? UserResponseDTO.builder()
                .id(ticket.getSolicitante().getId())
                .name(ticket.getSolicitante().getName())
                .email(ticket.getSolicitante().getEmail())
                .role(ticket.getSolicitante().getRole())
                .build() : null;

        UserResponseDTO tecnicoDto = ticket.getTecnico() != null ? UserResponseDTO.builder()
                .id(ticket.getTecnico().getId())
                .name(ticket.getTecnico().getName())
                .email(ticket.getTecnico().getEmail())
                .role(ticket.getTecnico().getRole())
                .build() : null;

        return TicketResponseDTO.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .solicitante(solicitanteDto)
                .tecnico(tecnicoDto)
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
