/**
 * CAMPUSDESK - Lógica de Frontend Vanilla JS (TechNova Solutions)
 * Arquitectura: HTML5 + CSS3 + Fetch API nativa. Sin frameworks externos.
 * Ciclo de vida de incidencias, máquina de estados, métricas, comentarios y auditoría.
 */
'use strict';

(function () {
  // 1. Configuración de API y URLs
  const BASE_URL = localStorage.getItem('api_url') 
    || (window.location.port === '5506' ? window.location.origin : 'http://localhost:8056');

  // 2. Estado de la Aplicación
  const AppState = {
    currentUser: null,
    tickets: [],
    technicians: [],
    activeTicket: null,
    activeFilterStatus: '',
    activeFilterPriority: '',
    activeFilterCategory: '',
    searchQuery: ''
  };

  // 3. Utilidades y Helpers
  function getHeaders() {
    const token = AuthGuard.getToken();
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    };
  }

  function formatDate(isoStr) {
    if (!isoStr) return '—';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('es-CO', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoStr;
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function showToast(message, type = 'success') {
    const toast = document.getElementById('toastBox');
    const msg = document.getElementById('toastMessage');
    if (!toast || !msg) return;

    msg.textContent = message;
    toast.className = `campus-toast show ${type}`;

    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => {
      toast.className = 'campus-toast';
    }, 3800);
  }

  // 4. Inicialización de Usuario y Perfil
  async function initUserProfile() {
    const role = AuthGuard.getRole();
    const storedEmail = AuthGuard.getUserEmail();

    const roleBadge = document.getElementById('navUserRole');
    const emailSpan = document.getElementById('navUserEmail');

    if (roleBadge) {
      roleBadge.textContent = role;
      roleBadge.className = `role-pill ${role.toLowerCase()}`;
    }
    if (emailSpan) {
      emailSpan.textContent = storedEmail || 'Usuario';
    }

    try {
      const resp = await fetch(`${BASE_URL}/api/users/me`, {
        headers: getHeaders()
      });
      if (resp.ok) {
        const user = await resp.json();
        AppState.currentUser = user;
        if (emailSpan) emailSpan.textContent = user.name || user.email;
        if (roleBadge) {
          roleBadge.textContent = user.role;
          roleBadge.className = `role-pill ${user.role.toLowerCase()}`;
        }
      }
    } catch (e) {
      console.warn('No se pudo refrescar el perfil desde el backend:', e);
    }
  }

  // 5. Carga de Técnicos (para asignación por Administrador)
  async function loadTechnicians() {
    try {
      const resp = await fetch(`${BASE_URL}/api/users/technicians`, {
        headers: getHeaders()
      });
      if (resp.ok) {
        AppState.technicians = await resp.json();
      }
    } catch (e) {
      console.warn('Error al cargar lista de técnicos:', e);
    }
  }

  // 6. Carga de Métricas del Dashboard (RF-07)
  async function loadDashboardMetrics() {
    try {
      const resp = await fetch(`${BASE_URL}/api/dashboard/metrics`, {
        headers: getHeaders()
      });
      if (!resp.ok) return;

      const data = await resp.json();
      document.getElementById('metricTotal').textContent = data.total ?? 0;
      document.getElementById('metricAbiertas').textContent = data.abiertas ?? 0;
      document.getElementById('metricAsignadas').textContent = data.asignadas ?? 0;
      document.getElementById('metricEnProceso').textContent = data.enProceso ?? 0;
      document.getElementById('metricResueltas').textContent = data.resueltas ?? 0;
      document.getElementById('metricCerradas').textContent = data.cerradas ?? 0;
    } catch (e) {
      console.error('Error cargando métricas:', e);
    }
  }

  // 7. Carga y Filtrado de Incidencias (RF-03, RF-08)
  async function loadTickets() {
    const tbody = document.getElementById('ticketsTableBody');
    tbody.innerHTML = `
      <tr>
        <td colspan="9" style="text-align: center; padding: 30px; color: #64748B;">
          Actualizando lista de incidencias...
        </td>
      </tr>
    `;

    try {
      const params = new URLSearchParams();
      if (AppState.activeFilterStatus) params.append('status', AppState.activeFilterStatus);
      if (AppState.activeFilterPriority) params.append('priority', AppState.activeFilterPriority);
      if (AppState.activeFilterCategory) params.append('category', AppState.activeFilterCategory);

      const url = `${BASE_URL}/api/tickets?${params.toString()}`;
      const resp = await fetch(url, { headers: getHeaders() });

      if (!resp.ok) {
        tbody.innerHTML = `
          <tr>
            <td colspan="9" style="text-align: center; padding: 30px; color: #E11D48;">
              Error al obtener incidencias del servidor (${resp.status}).
            </td>
          </tr>
        `;
        return;
      }

      AppState.tickets = await resp.json();
      renderTicketsTable();
    } catch (e) {
      tbody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align: center; padding: 30px; color: #E11D48;">
            No se pudo conectar con el servicio de tickets.
          </td>
        </tr>
      `;
      console.error(e);
    }
  }

  function renderTicketsTable() {
    const tbody = document.getElementById('ticketsTableBody');
    const query = AppState.searchQuery.toLowerCase().trim();

    const filtered = AppState.tickets.filter(t => {
      if (!query) return true;
      const titleMatch = (t.title || '').toLowerCase().includes(query);
      const descMatch = (t.description || '').toLowerCase().includes(query);
      const idMatch = String(t.id).includes(query);
      return titleMatch || descMatch || idMatch;
    });

    if (filtered.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align: center; padding: 40px; color: #64748B;">
            <div style="font-size: 24px; margin-bottom: 8px;">📋</div>
            <div style="font-weight: 600; font-size: 15px; color: #334155;">No se encontraron incidencias</div>
            <div style="font-size: 13px; margin-top: 4px;">Modifica los filtros o crea una nueva incidencia con el botón superior.</div>
          </td>
        </tr>
      `;
      return;
    }

    const currentRole = AuthGuard.getRole();
    const currentEmail = AuthGuard.getUserEmail();

    tbody.innerHTML = filtered.map(t => {
      const id = t.id;
      const title = escapeHtml(t.title);
      const cat = t.category || 'OTRO';
      const prio = t.priority || 'MEDIA';
      const status = t.status || 'ABIERTA';
      const solicitante = t.solicitante ? escapeHtml(t.solicitante.name || t.solicitante.email) : '—';
      const tecnico = t.tecnico ? escapeHtml(t.tecnico.name || t.tecnico.email) : '<span style="color: #94A3B8; font-style: italic;">Sin asignar</span>';
      const fecha = formatDate(t.createdAt);

      // Acciones rápidas según rol y estado
      let shortcutBtn = '';
      if (currentRole === 'ADMIN' && status === 'ABIERTA') {
        shortcutBtn = `<button class="btn-campus-secondary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px;" onclick="window.CampusDesk.openAssignQuick(${id})">Asignar</button>`;
      } else if (currentRole === 'TECHNICIAN' && status === 'ASIGNADA' && t.tecnico && t.tecnico.email === currentEmail) {
        shortcutBtn = `<button class="btn-campus-primary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px;" onclick="window.CampusDesk.quickChangeStatus(${id}, 'EN_PROCESO', 'Iniciando atención')">Atender</button>`;
      } else if (currentRole === 'TECHNICIAN' && status === 'EN_PROCESO' && t.tecnico && t.tecnico.email === currentEmail) {
        shortcutBtn = `<button class="btn-campus-primary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px; background: #059669;" onclick="window.CampusDesk.quickChangeStatus(${id}, 'RESUELTA', 'Incidencia resuelta')">Resolver</button>`;
      } else if (status === 'RESUELTA' && (currentRole === 'ADMIN' || (t.solicitante && t.solicitante.email === currentEmail))) {
        shortcutBtn = `<button class="btn-campus-secondary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px; border-color: #065F46; color: #065F46;" onclick="window.CampusDesk.quickChangeStatus(${id}, 'CERRADA', 'Cierre confirmado')">Cerrar</button>`;
      }

      return `
        <tr>
          <td style="font-weight: 700; color: #475569;">#${id}</td>
          <td>
            <div style="font-weight: 600; color: #0F172A;">${title}</div>
          </td>
          <td><span class="badge badge-cat">${cat}</span></td>
          <td><span class="badge badge-prio-${prio}">${prio}</span></td>
          <td><span class="badge badge-status-${status}">${status.replace('_', ' ')}</span></td>
          <td style="font-size: 13px;">${solicitante}</td>
          <td style="font-size: 13px;">${tecnico}</td>
          <td style="font-size: 12.5px; color: #64748B;">${fecha}</td>
          <td style="text-align: right; white-space: nowrap;">
            ${shortcutBtn}
            <button class="btn-campus-secondary" style="padding: 5px 10px; font-size: 12px;" onclick="window.CampusDesk.openDetailModal(${id})">
              Ver Detalle
            </button>
          </td>
        </tr>
      `;
    }).join('');
  }

  // 8. Crear Nueva Incidencia (RF-03, CP-03)
  async function handleCreateTicket(e) {
    e.preventDefault();
    const btnSubmit = document.getElementById('btnSubmitCreate');
    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Guardando...';

    const payload = {
      title: document.getElementById('newTitle').value.trim(),
      category: document.getElementById('newCategory').value,
      priority: document.getElementById('newPriority').value,
      description: document.getElementById('newDescription').value.trim()
    };

    try {
      const resp = await fetch(`${BASE_URL}/api/tickets`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(payload)
      });

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        const msg = (err && (err.message || err.error)) || 'Error al crear la incidencia';
        showToast(msg, 'error');
        return;
      }

      const created = await resp.json();
      showToast(`Incidencia #${created.id} creada exitosamente`, 'success');

      closeModal('modalCreateTicket');
      document.getElementById('formCreateTicket').reset();

      // Recargar tablero
      await loadDashboardMetrics();
      await loadTickets();
    } catch (err) {
      showToast('Error de red al crear incidencia', 'error');
      console.error(err);
    } finally {
      btnSubmit.disabled = false;
      btnSubmit.textContent = 'Guardar Incidencia';
    }
  }

  // 9. Abrir Detalle Integral del Ticket (RF-04, RF-05, RF-06)
  async function openDetailModal(ticketId) {
    try {
      const resp = await fetch(`${BASE_URL}/api/tickets/${ticketId}`, {
        headers: getHeaders()
      });
      if (!resp.ok) {
        showToast('No se pudo cargar el detalle del ticket', 'error');
        return;
      }

      const ticket = await resp.json();
      AppState.activeTicket = ticket;

      // Renderizar datos básicos
      document.getElementById('detailTicketId').textContent = `#${ticket.id}`;
      document.getElementById('detailTicketTitle').textContent = ticket.title;
      document.getElementById('detailDescription').textContent = ticket.description;

      const statusBadge = `<span class="badge badge-status-${ticket.status}">${ticket.status.replace('_', ' ')}</span>`;
      const prioBadge = `<span class="badge badge-prio-${ticket.priority}">${ticket.priority}</span>`;
      document.getElementById('detailStatusBadge').innerHTML = statusBadge;
      document.getElementById('detailPriorityBadge').innerHTML = prioBadge;

      const solicitante = ticket.solicitante 
        ? `${escapeHtml(ticket.solicitante.name)} (${escapeHtml(ticket.solicitante.email)})` 
        : '—';
      const tecnico = ticket.tecnico 
        ? `${escapeHtml(ticket.tecnico.name)} (${escapeHtml(ticket.tecnico.email)})` 
        : '<span style="color: #94A3B8; font-style: italic;">Sin asignar</span>';

      document.getElementById('detailSolicitante').innerHTML = solicitante;
      document.getElementById('detailTecnico').innerHTML = tecnico;

      // Renderizar Panel de Acciones de Flujo de Estados
      renderActionPanel(ticket);

      // Cargar Comentarios e Historial
      await loadComments(ticket.id);
      await loadHistory(ticket.id);

      // Activar la primera pestaña por defecto
      switchTab('tab-info');

      // Abrir modal
      openModal('modalDetailTicket');
    } catch (e) {
      showToast('Error de comunicación con el servidor', 'error');
      console.error(e);
    }
  }

  function renderActionPanel(ticket) {
    const container = document.getElementById('dynamicActionPanel');
    const role = AuthGuard.getRole();
    const userEmail = AuthGuard.getUserEmail();

    // 1. Si el ticket está CERRADO: Inmutabilidad
    if (ticket.status === 'CERRADA') {
      container.innerHTML = `
        <div class="closed-box">
          <span style="font-size: 18px;">🔒</span>
          <div>
            <div>Esta incidencia está <strong>CERRADA</strong>.</div>
            <div style="font-size: 12px; font-weight: normal; color: #64748B; margin-top: 2px;">
              El ciclo de vida ha concluido satisfactoriamente. Los datos e historial son inmutables.
            </div>
          </div>
        </div>
      `;
      return;
    }

    let actionsHtml = '';

    // 2. Acciones para ADMINISTRADOR (Asignar técnicos)
    if (role === 'ADMIN') {
      const techOptions = AppState.technicians.map(t => {
        const isSelected = ticket.tecnico && ticket.tecnico.id === t.id ? 'selected' : '';
        return `<option value="${t.id}" ${isSelected}>${escapeHtml(t.name)} (${escapeHtml(t.email)})</option>`;
      }).join('');

      actionsHtml += `
        <div class="action-box">
          <div class="action-box-title">⚙ Administración: Asignación de Técnico Responsable</div>
          <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
            <select id="selectAssignTech" style="flex: 1; min-width: 200px; padding: 9px; border: 1px solid #CBD5E1; border-radius: 6px;">
              <option value="">-- Seleccionar Técnico --</option>
              ${techOptions}
            </select>
            <button class="btn-campus-primary" style="padding: 9px 16px; font-size: 13.5px;" onclick="window.CampusDesk.assignTechnician(${ticket.id})">
              Asignar Técnico
            </button>
          </div>
        </div>
      `;
    }

    // 3. Acciones para TÉCNICO ASIGNADO (Iniciar atención o Resolver)
    const isAssignedTech = ticket.tecnico && ticket.tecnico.email === userEmail;
    if (role === 'ADMIN' || isAssignedTech) {
      if (ticket.status === 'ASIGNADA') {
        actionsHtml += `
          <div class="action-box" style="background: #EFF6FF; border-color: #BFDBFE;">
            <div class="action-box-title" style="color: #1E40AF;">▶ Gestión Técnica: Iniciar Atención</div>
            <p style="font-size: 13px; color: #3B82F6; margin-bottom: 12px;">
              La incidencia se encuentra asignada. Presiona el botón para iniciar el diagnóstico técnico.
            </p>
            <button class="btn-campus-primary" style="background: #2563EB;" onclick="window.CampusDesk.updateStatus(${ticket.id}, 'EN_PROCESO', 'Técnico inicia atención e investigación')">
              Iniciar Atención (Pasar a EN PROCESO)
            </button>
          </div>
        `;
      } else if (ticket.status === 'EN_PROCESO') {
        actionsHtml += `
          <div class="action-box" style="background: #ECFDF5; border-color: #A7F3D0;">
            <div class="action-box-title" style="color: #065F46;">✓ Gestión Técnica: Concluir y Resolver</div>
            <p style="font-size: 13px; color: #047857; margin-bottom: 12px;">
              Indica la solución aplicada para que el usuario solicitante pueda validar y confirmar el cierre.
            </p>
            <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
              <input type="text" id="resolveCommentInput" placeholder="Detalle de solución técnica aplicada..." style="flex: 1; padding: 9px; border: 1px solid #CBD5E1; border-radius: 6px; font-size: 13px;">
              <button class="btn-campus-primary" style="background: #10B981;" onclick="window.CampusDesk.resolveTicket(${ticket.id})">
                Marcar como RESUELTA
              </button>
            </div>
          </div>
        `;
      }
    }

    // 4. Acciones para USUARIO SOLICITANTE (Cerrar ticket resuelto)
    const isRequester = ticket.solicitante && ticket.solicitante.email === userEmail;
    if (role === 'ADMIN' || isRequester) {
      if (ticket.status === 'RESUELTA') {
        actionsHtml += `
          <div class="action-box" style="background: #F8FAFC; border-color: #CBD5E1;">
            <div class="action-box-title" style="color: #334155;">🔒 Conformidad y Cierre de Incidencia</div>
            <p style="font-size: 13px; color: #64748B; margin-bottom: 12px;">
              El soporte técnico ha marcado el problema como RESUELTO. Si estás conforme, confirma el cierre definitivo.
            </p>
            <button class="btn-campus-primary" style="background: #334155;" onclick="window.CampusDesk.updateStatus(${ticket.id}, 'CERRADA', 'Solicitante confirma conformidad y cierre definitivo')">
              Confirmar Conformidad y Cerrar Incidencia
            </button>
          </div>
        `;
      }
    }

    container.innerHTML = actionsHtml || `
      <div style="padding: 12px; background: #F8FAFC; border-radius: 6px; font-size: 13px; color: #64748B; text-align: center;">
        No hay transiciones manuales pendientes para tu perfil en el estado actual (${ticket.status}).
      </div>
    `;
  }

  // 10. Asignar Técnico (RF-04)
  async function assignTechnician(ticketId) {
    const select = document.getElementById('selectAssignTech');
    if (!select || !select.value) {
      showToast('Por favor selecciona un técnico de la lista', 'error');
      return;
    }

    const tecnicoId = parseInt(select.value, 10);
    try {
      const resp = await fetch(`${BASE_URL}/api/tickets/${ticketId}/assign`, {
        method: 'PATCH',
        headers: getHeaders(),
        body: JSON.stringify({ tecnicoId })
      });

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        showToast((err && (err.message || err.error)) || 'Error al asignar técnico', 'error');
        return;
      }

      showToast('Técnico asignado exitosamente', 'success');
      await openDetailModal(ticketId);
      await loadDashboardMetrics();
      await loadTickets();
    } catch (e) {
      showToast('Error de red al asignar técnico', 'error');
      console.error(e);
    }
  }

  // 11. Cambiar Estado en la Máquina de Estados (RF-04)
  async function updateStatus(ticketId, newStatus, defaultComment = '') {
    try {
      const resp = await fetch(`${BASE_URL}/api/tickets/${ticketId}/status`, {
        method: 'PATCH',
        headers: getHeaders(),
        body: JSON.stringify({
          status: newStatus,
          comentario: defaultComment
        })
      });

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        const msg = (err && (err.message || err.error)) || 'Transición de estado no autorizada';
        showToast(msg, 'error');
        return;
      }

      showToast(`Estado actualizado a ${newStatus}`, 'success');
      await openDetailModal(ticketId);
      await loadDashboardMetrics();
      await loadTickets();
    } catch (e) {
      showToast('Error de comunicación al actualizar estado', 'error');
      console.error(e);
    }
  }

  function resolveTicket(ticketId) {
    const input = document.getElementById('resolveCommentInput');
    const comment = (input && input.value.trim()) || 'Solución técnica verificada';
    updateStatus(ticketId, 'RESUELTA', comment);
  }

  // 12. Hilo de Comentarios (RF-05)
  async function loadComments(ticketId) {
    const container = document.getElementById('commentsContainer');
    const countBadge = document.getElementById('detailCommentCount');
    const formContainer = document.getElementById('commentFormContainer');

    container.innerHTML = '<div style="color: #64748B; font-size: 13px;">Cargando notas...</div>';

    try {
      const resp = await fetch(`${BASE_URL}/api/tickets/${ticketId}/comments`, {
        headers: getHeaders()
      });

      if (!resp.ok) {
        container.innerHTML = '<div style="color: #64748B; font-size: 13px;">Sin comentarios registrados.</div>';
        return;
      }

      const comments = await resp.json();
      if (countBadge) countBadge.textContent = comments.length;

      // Si ticket está CERRADO, deshabilitar formulario de comentarios
      if (AppState.activeTicket && AppState.activeTicket.status === 'CERRADA') {
        formContainer.innerHTML = `
          <div class="closed-box" style="margin-top: 14px;">
            <span>🔒</span> No se pueden registrar comentarios adicionales en un ticket CERRADO.
          </div>
        `;
      } else {
        formContainer.innerHTML = `
          <form id="formAddComment" style="margin-top: 14px;" onsubmit="window.CampusDesk.handleAddComment(event, ${ticketId})">
            <textarea id="commentContent" rows="3" required placeholder="Escriba una nota o actualización técnica sobre esta incidencia..." style="width: 100%; padding: 10px; border: 1px solid #CBD5E1; border-radius: 6px; resize: vertical;"></textarea>
            <div style="text-align: right; margin-top: 8px;">
              <button type="submit" class="btn-campus-primary" style="padding: 8px 16px; font-size: 13.5px;" id="btnSubmitComment">
                Agregar Comentario
              </button>
            </div>
          </form>
        `;
      }

      if (comments.length === 0) {
        container.innerHTML = '<div style="color: #64748B; font-size: 13px; text-align: center; padding: 20px;">No hay comentarios aún. Escribe el primero abajo.</div>';
        return;
      }

      container.innerHTML = comments.map(c => {
        const authorName = c.author ? escapeHtml(c.author.name || c.author.email) : 'Usuario';
        const authorRole = c.author ? c.author.role : 'USER';
        const dateStr = formatDate(c.createdAt);
        const bodyStr = escapeHtml(c.content);

        return `
          <div class="comment-card">
            <div class="comment-header">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span class="comment-author">${authorName}</span>
                <span class="role-pill ${authorRole.toLowerCase()}" style="font-size: 10px; padding: 2px 6px;">${authorRole}</span>
              </div>
              <span style="color: #94A3B8;">${dateStr}</span>
            </div>
            <div class="comment-body">${bodyStr}</div>
          </div>
        `;
      }).join('');
    } catch (e) {
      console.error(e);
      container.innerHTML = '<div style="color: #E11D48; font-size: 13px;">Error al cargar comentarios.</div>';
    }
  }

  async function handleAddComment(e, ticketId) {
    e.preventDefault();
    const btn = document.getElementById('btnSubmitComment');
    const textarea = document.getElementById('commentContent');
    const content = textarea.value.trim();

    if (!content) return;

    btn.disabled = true;
    btn.textContent = 'Enviando...';

    try {
      const resp = await fetch(`${BASE_URL}/api/tickets/${ticketId}/comments`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ content })
      });

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        showToast((err && (err.message || err.error)) || 'Error al agregar comentario', 'error');
        return;
      }

      showToast('Comentario agregado con éxito', 'success');
      textarea.value = '';
      await loadComments(ticketId);
    } catch (err) {
      showToast('Error de red al agregar comentario', 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Agregar Comentario';
    }
  }

  // 13. Trazabilidad y Auditoría (RF-06)
  async function loadHistory(ticketId) {
    const timeline = document.getElementById('historyTimeline');
    timeline.innerHTML = '<div style="color: #64748B; font-size: 13px;">Cargando historial de auditoría...</div>';

    try {
      const resp = await fetch(`${BASE_URL}/api/tickets/${ticketId}/history`, {
        headers: getHeaders()
      });

      if (!resp.ok) {
        timeline.innerHTML = '<div style="color: #64748B; font-size: 13px;">Sin historial registrado.</div>';
        return;
      }

      const history = await resp.json();
      if (history.length === 0) {
        timeline.innerHTML = '<div style="color: #64748B; font-size: 13px;">Sin cambios registrados.</div>';
        return;
      }

      timeline.innerHTML = history.map(h => {
        const dateStr = formatDate(h.fechaCambio);
        const authorName = h.autor ? escapeHtml(h.autor.name || h.autor.email) : 'Sistema';
        const authorRole = h.autor ? h.autor.role : 'SYSTEM';
        const oldState = h.estadoAnterior 
          ? `<span class="badge badge-status-${h.estadoAnterior}" style="font-size: 10px;">${h.estadoAnterior}</span>` 
          : '<span style="color: #94A3B8; font-size: 11px;">INICIO</span>';
        const newState = `<span class="badge badge-status-${h.estadoNuevo}" style="font-size: 10px;">${h.estadoNuevo}</span>`;
        const comment = escapeHtml(h.comentarioTransicion || 'Cambio de estado');

        return `
          <div class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="timeline-meta">
                <span>
                  <strong>${authorName}</strong> (${authorRole})
                </span>
                <span>${dateStr}</span>
              </div>
              <div style="margin: 6px 0; display: flex; align-items: center; gap: 8px;">
                ${oldState} <span style="color: #94A3B8;">&rarr;</span> ${newState}
              </div>
              <div class="timeline-comment">${comment}</div>
            </div>
          </div>
        `;
      }).join('');
    } catch (e) {
      console.error(e);
      timeline.innerHTML = '<div style="color: #E11D48; font-size: 13px;">Error al cargar auditoría.</div>';
    }
  }

  // 14. Modales y Pestañas
  function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add('active');
  }

  function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove('active');
  }

  function switchTab(targetTabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === targetTabId);
    });
    document.querySelectorAll('.tab-content').forEach(content => {
      content.style.display = content.id === targetTabId ? 'block' : 'none';
    });
  }

  // 15. Vinculación de Eventos en el DOM
  function setupEventListeners() {
    // Logout
    document.getElementById('btnLogout').addEventListener('click', () => {
      AuthGuard.logout();
    });

    // Refresh
    document.getElementById('btnRefresh').addEventListener('click', async () => {
      await loadDashboardMetrics();
      await loadTickets();
      showToast('Tablero actualizado');
    });

    // Modal Crear
    document.getElementById('btnOpenCreateModal').addEventListener('click', () => {
      openModal('modalCreateTicket');
    });
    document.getElementById('btnCloseCreateModal').addEventListener('click', () => {
      closeModal('modalCreateTicket');
    });
    document.getElementById('btnCancelCreate').addEventListener('click', () => {
      closeModal('modalCreateTicket');
    });
    document.getElementById('formCreateTicket').addEventListener('submit', handleCreateTicket);

    // Modal Detalle
    document.getElementById('btnCloseDetailModal').addEventListener('click', () => {
      closeModal('modalDetailTicket');
    });
    document.getElementById('btnExitDetail').addEventListener('click', () => {
      closeModal('modalDetailTicket');
    });

    // Pestañas del Modal
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        switchTab(btn.getAttribute('data-tab'));
      });
    });

    // Filtros de tabla
    document.getElementById('filterStatus').addEventListener('change', e => {
      AppState.activeFilterStatus = e.target.value;
      updateActiveCard();
      loadTickets();
    });

    document.getElementById('filterPriority').addEventListener('change', e => {
      AppState.activeFilterPriority = e.target.value;
      loadTickets();
    });

    document.getElementById('filterCategory').addEventListener('change', e => {
      AppState.activeFilterCategory = e.target.value;
      loadTickets();
    });

    document.getElementById('searchInput').addEventListener('input', e => {
      AppState.searchQuery = e.target.value;
      renderTicketsTable();
    });

    document.getElementById('btnResetFilters').addEventListener('click', () => {
      AppState.activeFilterStatus = '';
      AppState.activeFilterPriority = '';
      AppState.activeFilterCategory = '';
      AppState.searchQuery = '';
      document.getElementById('filterStatus').value = '';
      document.getElementById('filterPriority').value = '';
      document.getElementById('filterCategory').value = '';
      document.getElementById('searchInput').value = '';
      updateActiveCard();
      loadTickets();
    });

    // Click en Tarjetas de Métricas para filtrar
    document.querySelectorAll('.metric-card').forEach(card => {
      card.addEventListener('click', () => {
        const filterVal = card.getAttribute('data-filter');
        AppState.activeFilterStatus = filterVal;
        document.getElementById('filterStatus').value = filterVal;
        updateActiveCard();
        loadTickets();
      });
    });

    // Cerrar modal al presionar Escape o fondo
    window.addEventListener('keydown', e => {
      if (e.key === 'Escape') {
        closeModal('modalCreateTicket');
        closeModal('modalDetailTicket');
      }
    });

    document.querySelectorAll('.modal-overlay').forEach(overlay => {
      overlay.addEventListener('click', e => {
        if (e.target === overlay) {
          overlay.classList.remove('active');
        }
      });
    });
  }

  function updateActiveCard() {
    document.querySelectorAll('.metric-card').forEach(card => {
      const filter = card.getAttribute('data-filter');
      if (AppState.activeFilterStatus === filter) {
        card.classList.add('active-filter');
      } else {
        card.classList.remove('active-filter');
      }
    });
  }

  // 16. Exposición Global para Acciones Directas en la Tabla
  window.CampusDesk = {
    openDetailModal: openDetailModal,
    openAssignQuick: function(ticketId) {
      openDetailModal(ticketId);
    },
    quickChangeStatus: function(ticketId, newStatus, comment) {
      updateStatus(ticketId, newStatus, comment);
    },
    assignTechnician: assignTechnician,
    updateStatus: updateStatus,
    resolveTicket: resolveTicket,
    handleAddComment: handleAddComment
  };

  // 17. Inicialización Principal al cargar la página
  document.addEventListener('DOMContentLoaded', async () => {
    await initUserProfile();
    await loadTechnicians();
    await loadDashboardMetrics();
    await loadTickets();
  });

})();