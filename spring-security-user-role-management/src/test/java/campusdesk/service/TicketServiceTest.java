package campusdesk.service;

import campusdesk.dto.CreateTicketDTO;
import campusdesk.dto.TicketResponseDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketHistoryRepository ticketHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private User requester;
    private User technician;
    private User admin;
    private User otherUser;
    private Ticket ticketAbierta;

    @BeforeEach
    void setUp() {
        requester = User.builder().id(1L).email("user@campusdesk.com").name("Carlos User").role(Role.USER).build();
        technician = User.builder().id(2L).email("tech@campusdesk.com").name("Ana Tech").role(Role.TECHNICIAN).build();
        admin = User.builder().id(3L).email("admin@campusdesk.com").name("Super Admin").role(Role.ADMIN).build();
        otherUser = User.builder().id(4L).email("other@campusdesk.com").name("Otro User").role(Role.USER).build();

        ticketAbierta = Ticket.builder()
                .id(10L)
                .title("Falla de red en laboratorio 3")
                .description("No hay conectividad a Internet")
                .solicitante(requester)
                .tecnico(null)
                .status(TicketStatus.ABIERTA)
                .priority(TicketPriority.ALTA)
                .category(TicketCategory.REDES)
                .build();
    }

    @Test
    @DisplayName("CP-03 / RF-03: Crear ticket inicia estrictamente en ABIERTA y genera auditoría")
    void testCreateTicketSuccess() {
        CreateTicketDTO dto = CreateTicketDTO.builder()
                .title("Falla de red en laboratorio 3")
                .description("No hay conectividad a Internet")
                .priority(TicketPriority.ALTA)
                .category(TicketCategory.REDES)
                .build();

        when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setId(10L);
            return t;
        });

        TicketResponseDTO response = ticketService.createTicket(dto, requester.getEmail());

        assertNotNull(response);
        assertEquals(TicketStatus.ABIERTA, response.getStatus());
        assertEquals("Falla de red en laboratorio 3", response.getTitle());
        verify(ticketHistoryRepository, times(1)).save(any(TicketHistory.class));
    }

    @Test
    @DisplayName("RF-04: Admin asigna técnico con éxito transicionando a ASIGNADA")
    void testAssignTechnicianByAdmin() {
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));
        when(userRepository.findById(technician.getId())).thenReturn(Optional.of(technician));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponseDTO response = ticketService.assignTechnician(10L, technician.getId(), admin.getEmail());

        assertNotNull(response);
        assertEquals(TicketStatus.ASIGNADA, response.getStatus());
        assertEquals(technician.getId(), response.getTecnico().getId());
        verify(ticketHistoryRepository, times(1)).save(any(TicketHistory.class));
    }

    @Test
    @DisplayName("RF-04: Solo ADMIN puede asignar técnicos; USER genera UnauthorizedTicketActionException")
    void testAssignTechnicianUnauthorized() {
        when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));

        assertThrows(UnauthorizedTicketActionException.class, () ->
                ticketService.assignTechnician(10L, technician.getId(), requester.getEmail()));
    }

    @Test
    @DisplayName("RF-04: Transición a EN_PROCESO permitida para el técnico asignado")
    void testUpdateStatusToEnProcesoSuccess() {
        ticketAbierta.setStatus(TicketStatus.ASIGNADA);
        ticketAbierta.setTecnico(technician);

        when(userRepository.findByEmail(technician.getEmail())).thenReturn(Optional.of(technician));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponseDTO response = ticketService.updateStatus(10L, TicketStatus.EN_PROCESO, "Iniciando diagnóstico", technician.getEmail());

        assertEquals(TicketStatus.EN_PROCESO, response.getStatus());
        verify(ticketHistoryRepository, times(1)).save(any(TicketHistory.class));
    }

    @Test
    @DisplayName("RF-04: Transición a EN_PROCESO rechazada si el ticket no está en ASIGNADA")
    void testUpdateStatusToEnProcesoInvalidState() {
        // Ticket todavía en ABIERTA
        when(userRepository.findByEmail(technician.getEmail())).thenReturn(Optional.of(technician));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));

        assertThrows(InvalidStateTransitionException.class, () ->
                ticketService.updateStatus(10L, TicketStatus.EN_PROCESO, "Iniciando", technician.getEmail()));
    }

    @Test
    @DisplayName("RF-04: Transición a RESUELTA solo si estuvo EN_PROCESO y ejecutada por el técnico asignado")
    void testUpdateStatusToResueltaSuccess() {
        ticketAbierta.setStatus(TicketStatus.EN_PROCESO);
        ticketAbierta.setTecnico(technician);

        when(userRepository.findByEmail(technician.getEmail())).thenReturn(Optional.of(technician));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponseDTO response = ticketService.updateStatus(10L, TicketStatus.RESUELTA, "Cable reemplazado y probado", technician.getEmail());

        assertEquals(TicketStatus.RESUELTA, response.getStatus());
        verify(ticketHistoryRepository, times(1)).save(any(TicketHistory.class));
    }

    @Test
    @DisplayName("RF-04: Transición a RESUELTA rechazada si intenta saltarse de ASIGNADA a RESUELTA directamente")
    void testUpdateStatusToResueltaSkipStateFails() {
        ticketAbierta.setStatus(TicketStatus.ASIGNADA);
        ticketAbierta.setTecnico(technician);

        when(userRepository.findByEmail(technician.getEmail())).thenReturn(Optional.of(technician));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));

        assertThrows(InvalidStateTransitionException.class, () ->
                ticketService.updateStatus(10L, TicketStatus.RESUELTA, "Directo a resuelta", technician.getEmail()));
    }

    @Test
    @DisplayName("RF-04: Transición a CERRADA permitida al solicitante cuando el ticket está RESUELTA")
    void testUpdateStatusToCerradaByRequesterSuccess() {
        ticketAbierta.setStatus(TicketStatus.RESUELTA);
        ticketAbierta.setTecnico(technician);

        when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponseDTO response = ticketService.updateStatus(10L, TicketStatus.CERRADA, "Conforme con la solución", requester.getEmail());

        assertEquals(TicketStatus.CERRADA, response.getStatus());
        verify(ticketHistoryRepository, times(1)).save(any(TicketHistory.class));
    }

    @Test
    @DisplayName("RF-04: Transición a CERRADA rechazada si el ticket no está RESUELTA (ej. aún EN_PROCESO)")
    void testUpdateStatusToCerradaPrematureFails() {
        ticketAbierta.setStatus(TicketStatus.EN_PROCESO);
        ticketAbierta.setTecnico(technician);

        when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));

        assertThrows(InvalidStateTransitionException.class, () ->
                ticketService.updateStatus(10L, TicketStatus.CERRADA, "Cerrar ya", requester.getEmail()));
    }

    @Test
    @DisplayName("RF-04: Transición a CERRADA rechazada si otro usuario no solicitante intenta cerrarla")
    void testUpdateStatusToCerradaUnauthorizedUser() {
        ticketAbierta.setStatus(TicketStatus.RESUELTA);
        ticketAbierta.setTecnico(technician);

        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));

        assertThrows(UnauthorizedTicketActionException.class, () ->
                ticketService.updateStatus(10L, TicketStatus.CERRADA, "Intruso cerrando", otherUser.getEmail()));
    }

    @Test
    @DisplayName("RF-04 / RF-08: Ticket en estado CERRADA es estrictamente inmutable")
    void testTicketClosedIsImmutable() {
        ticketAbierta.setStatus(TicketStatus.CERRADA);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));

        assertThrows(TicketClosedException.class, () ->
                ticketService.updateStatus(10L, TicketStatus.EN_PROCESO, "Reabrir intento", admin.getEmail()));
    }

    @Test
    @DisplayName("RF-08: Usuario común no puede ver ticket de otro usuario")
    void testGetTicketByIdForbiddenForOtherUser() {
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketAbierta));

        assertThrows(UnauthorizedTicketActionException.class, () ->
                ticketService.getTicketById(10L, otherUser.getEmail()));
    }
}
