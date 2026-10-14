# Seguimiento de tickets: comentarios, historial y estadísticas

Los módulos están en `modules/comment`, `modules/history`, `modules/report` y
`modules/access`. Siguen la misma estructura que clientes, productos y ventas:
entidad, repositorio, DTO, servicio transaccional y controlador con
`@PreAuthorize`. El usuario se toma del JWT (`Authentication`), nunca del cuerpo
de la petición.

## Endpoints y permisos

| Ruta | Permiso | Descripción |
| --- | --- | --- |
| GET /api/tickets/{id}/comments | COMMENT_READ | Comentarios en orden cronológico |
| POST /api/tickets/{id}/comments | COMMENT_CREATE | Registra un comentario (201). Ticket CERRADA: 409 |
| GET /api/tickets/{id}/history | HISTORY_READ | Historial de estados, solo lectura |
| GET /api/reports/summary | REPORT_READ | Total y conteo por estado |

Los cinco permisos nuevos (`COMMENT_READ`, `COMMENT_CREATE`, `HISTORY_READ`,
`REPORT_READ` y `TICKET_VIEW_ALL`) están en `UserRoles.Authority`. Al arrancar,
el bootstrap se los agrega a ADMIN; los demás roles los reciben desde
`POST /api/roles` o `POST /api/user/assignPermission`.

## Acceso a cada ticket

Además del permiso de la ruta, el servicio comprueba el ticket (`TicketAccess`):

| Cuenta | Puede ver y comentar |
| --- | --- |
| Con `TICKET_VIEW_ALL` (ADMIN) | Cualquier ticket |
| Quien registró el ticket (`createdBy`) | Sus tickets |
| Técnico asignado (`assignedTo`) | Los tickets asignados a él |

Sin acceso: 403. Ticket inexistente: 404. Comentar un ticket CERRADA: 409.
`/api/reports/summary` aplica la misma regla: con `TICKET_VIEW_ALL` cuenta todo;
sin él, solo los tickets propios o asignados.

## Roles de ejemplo

Crea con `POST /api/roles` (token ADMIN):

```json
{"name":"SOLICITANTE","permissions":["COMMENT_READ","COMMENT_CREATE","HISTORY_READ","REPORT_READ"]}
```
```json
{"name":"TECNICO","permissions":["COMMENT_READ","COMMENT_CREATE","HISTORY_READ","REPORT_READ"]}
```

Asígnalos con `POST /api/user/assignRole`, por ejemplo
`{"username":"ana","role":"SOLICITANTE"}`.

## Registrar el historial desde el servicio de tickets

`StatusHistoryService.record(ticket, estadoAnterior, estadoNuevo, usuario)` debe
llamarse dentro del mismo método `@Transactional` que crea, asigna o cambia el
estado. Si el registro falla, el cambio de estado se revierte.

```java
@Transactional
public TicketResponse changeStatus(Long id, StatusRequest request, String actor) {
    Ticket ticket = tickets.findById(id).orElseThrow(/* 404 */);
    TicketStatus previous = ticket.getStatus();
    // validar la transición; 409 si no es válida
    ticket.setStatus(request.status());
    tickets.save(ticket);
    statusHistory.record(ticket, previous, ticket.getStatus(), actor);
    return toResponse(ticket);
}
```

Al crear: `record(ticket, null, TicketStatus.ABIERTA, actor)`. Al asignar un
técnico: `record(ticket, ABIERTA, ASIGNADA, actor)`.

## Entidad Ticket requerida

Estos módulos leen `modules/ticket/Ticket` con los campos `id`, `status`
(`TicketStatus`), `createdBy` y `assignedTo` (usernames). El paquete `modules/ticket`
incluye una versión mínima para que el proyecto compile. Si el módulo de tickets
real usa otros nombres, ajusta `TicketAccess` y `ReportRepository`.

## Probar sin un módulo de tickets completo

Hibernate crea las tablas `tickets`, `ticket_comment` y `status_history` al
arrancar. Para tener un ticket de prueba:

```sql
INSERT INTO tickets (title, status, created_by, assigned_to, created_at)
VALUES ('Sin internet en el aula 3', 'ASIGNADA', 'ana', 'carlos', NOW());
```

Con un token de ana, carlos o superadmin: `GET /api/tickets/1/comments`.
Comparación de estadísticas con la base:

```sql
SELECT status, COUNT(*) FROM tickets GROUP BY status;
```

## Pruebas

`UsermanagementApplicationTests` cuenta las rutas de OpenAPI; ahora son 24 por
las tres rutas nuevas (`/api/tickets/{ticketId}/comments`,
`/api/tickets/{ticketId}/history` y `/api/reports/summary`). Si el módulo de
tickets suma rutas, actualiza ese número.
