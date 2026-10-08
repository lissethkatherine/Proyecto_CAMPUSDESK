# Guía Persona 3 — Comentarios, Historial y Administración/Estadísticas

## 1. Dónde va cada archivo
```
backend/src/main/java/com/technova/helpdesk/
  entity/      Comment.java, StatusHistory.java
  repository/  CommentRepository, StatusHistoryRepository, ReportRepository
  dto/         CommentRequest, CommentResponse, StatusHistoryResponse, SummaryResponse
  service/     TicketAccessService, CommentService, StatusHistoryService, ReportService
  controller/  CommentController, HistoryController, ReportController
database/persona3.sql
frontend/  dashboard.html, admin.html, ticket-detail.html, css/persona3.css,
           js/api.js, comments.js, history.js, dashboard.js, admin.js
```
Copia las carpetas dentro de tu proyecto. **Cambia el paquete** `com.technova.helpdesk`
por el real del equipo (en cada archivo y en los imports).

## 2. Qué debes adaptar (supuestos que hice)
Mi código usa clases de Persona 1 y 2. Confirma con ellos que existan con estos nombres,
o cambia mis archivos:

| Clase | Lo que uso |
|---|---|
| `User` | `getId()`, `getFullName()`, `getRole()` |
| `Role` (enum) | `ADMIN`, `TECHNICIAN`, `USER` |
| `Ticket` | `getId()`, `getStatus()`, `getRequester()`, `getTechnician()` (campos `status`, `requester`, `technician` en la entidad) |
| `TicketStatus` (enum) | `ABIERTA, ASIGNADA, EN_PROCESO, RESUELTA, CERRADA` |
| `UserRepository` | `Optional<User> findByEmail(String)` |
| `TicketRepository` | `JpaRepository<Ticket, Long>` |
| JWT (Persona 1) | el "subject" del token es el email → `authentication.getName()` |

Los errores usan `ResponseStatusException` (403, 404, 409). Si el equipo tiene excepciones propias
y un `@RestControllerAdvice`, reemplázalas para que el formato JSON sea consistente.

## 3. Integración con Persona 2 (lo más importante)
Persona 2 debe inyectar `StatusHistoryService` en su `TicketService` y llamarlo
**dentro del mismo método `@Transactional`** que cambia el estado:

```java
@Transactional
public TicketResponse changeStatus(Long id, StatusRequest req, Authentication auth) {
    User user = access.currentUser(auth);
    Ticket ticket = access.findTicket(id);
    TicketStatus previous = ticket.getStatus();
    // ... validar reglas del ciclo de vida (409 si la transición no es válida) ...
    ticket.setStatus(req.status());
    ticketRepository.save(ticket);
    historyService.record(ticket, previous, ticket.getStatus(), user); // <- tu parte
    return toResponse(ticket);
}
```
Haz lo mismo al **crear** (`record(ticket, null, ABIERTA, user)`) y al **asignar**
(`record(ticket, ABIERTA, ASIGNADA, admin)`). Así RF-04 regla 10 y RF-06 quedan cumplidos:
si `record` falla, se hace rollback del cambio de estado.

## 4. Persona 1: seguridad
- Los endpoints `/api/tickets/*/comments`, `/api/tickets/*/history` y `/api/reports/summary`
  solo deben exigir token válido (`.anyRequest().authenticated()`). Los permisos finos ya
  los valido yo en los servicios.
- Swagger debe tener el esquema `bearerAuth` para probar con el botón **Authorize**.
- CORS debe permitir el origen del frontend (ej. `http://localhost:5500`).

## 5. Orden recomendado de trabajo
1. Acordar con P1 y P2 nombres de entidades, enums y el email en el JWT.
2. Copiar entidades + repositorios y levantar la app (verifica que se creen `comments` y `status_history`).
3. Implementar y probar comentarios en Swagger.
4. Implementar historial y pedirle a P2 que lo conecte.
5. Reportes.
6. Frontend: primero `ticket-detail`, luego `dashboard`, luego `admin`.
7. Probar casos CP-11, CP-13, CP-14, CP-15 y sacar capturas.

## 6. Cómo probar (Swagger → Authorize con el token)
| Caso | Qué hacer | Esperado |
|---|---|---|
| CP-11 | Con técnico, `PATCH /status` a EN_PROCESO; luego `GET /history` | aparece ASIGNADA → EN_PROCESO |
| CP-13 | Cerrar un ticket y hacer `POST /comments` | 409 |
| Permisos | USER intenta comentar ticket ajeno | 403 |
| CP-14 | `GET /api/reports/summary` y en PostgreSQL `SELECT status, COUNT(*) FROM tickets GROUP BY status;` | los números coinciden |
| CP-15 | Abrir `ticket-detail.html?id=1` con sesión iniciada | comentarios e historial cargan |

## 7. Frontend
- Sirve la carpeta con Live Server (VS Code) o `python -m http.server 5500`.
- `api.js` guarda la URL del backend (`API_URL`) y lee el token de `localStorage["token"]`.
  Si Persona 1 usa otra clave, cámbiala. Si ya tiene un helper de `fetch`, usa el suyo.
- `admin.js` asume que `GET /api/tickets` devuelve `technicianId` y que
  `PATCH /assign` recibe `{ "technicianId": n }`. Ajusta con Persona 2 según su DTO.
- Oculta el enlace a `admin.html` si el rol no es ADMIN (el backend igual debe validarlo).
- El texto de usuarios se pinta con `esc()` para evitar XSS.

## 8. Para la sustentación, prepárate para explicar
- Por qué el autor del comentario sale del token y no del body.
- Por qué el historial y el cambio de estado van en una sola transacción (`@Transactional`).
- Por qué `StatusHistory` no tiene setters ni endpoints de edición.
- Cómo se calculan las estadísticas (consultas `count` en JPA, no datos en JavaScript).
