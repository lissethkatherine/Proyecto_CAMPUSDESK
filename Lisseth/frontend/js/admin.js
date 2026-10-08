// Requiere api.js. HTML: #admin-body (tbody), #admin-msg
// Asume que GET /api/tickets devuelve: id, title, status, technicianName, technicianId (ajusta con Persona 2)
// y que PATCH /api/tickets/{id}/assign recibe { "technicianId": n }.
let technicians = [];

async function loadAdmin() {
  const body = document.getElementById("admin-body");
  const msg = document.getElementById("admin-msg");
  body.innerHTML = '<tr><td colspan="5" class="loading">Cargando…</td></tr>';
  try {
    const [tickets, techs] = await Promise.all([apiFetch("/api/tickets"), apiFetch("/api/users/technicians")]);
    technicians = techs;
    const list = Array.isArray(tickets) ? tickets : tickets.content || [];
    body.innerHTML = list.map(t => `<tr>
      <td>#${t.id}</td><td>${esc(t.title)}</td><td><span class="badge">${esc(t.status)}</span></td>
      <td><select id="tech-${t.id}">${techOptions(t.technicianId)}</select></td>
      <td><button onclick="assign(${t.id})">${t.technicianId ? "Reasignar" : "Asignar"}</button>
          <a href="ticket-detail.html?id=${t.id}">Ver</a></td></tr>`).join("")
      || '<tr><td colspan="5">No hay tickets.</td></tr>';
  } catch (e) { body.innerHTML = ""; showMsg(msg, e.message); }
}

const techOptions = (sel) => '<option value="">Seleccionar…</option>' +
  technicians.map(x => `<option value="${x.id}" ${x.id === sel ? "selected" : ""}>${esc(x.fullName || x.name)}</option>`).join("");

async function assign(ticketId) {
  const msg = document.getElementById("admin-msg");
  const technicianId = document.getElementById("tech-" + ticketId).value;
  if (!technicianId) return showMsg(msg, "Selecciona un técnico.");
  try {
    await apiFetch(`/api/tickets/${ticketId}/assign`, { method: "PATCH", body: JSON.stringify({ technicianId: Number(technicianId) }) });
    showMsg(msg, `Ticket #${ticketId} asignado correctamente.`, "ok");
    loadAdmin();
  } catch (e) { showMsg(msg, e.message); }
}
document.addEventListener("DOMContentLoaded", loadAdmin);
