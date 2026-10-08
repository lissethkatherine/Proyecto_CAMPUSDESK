// Requiere api.js. HTML: <ul id="history-list" class="timeline">
async function loadHistory(ticketId) {
  const list = document.getElementById("history-list");
  list.innerHTML = '<li class="loading">Cargando historial…</li>';
  try {
    const data = await apiFetch(`/api/tickets/${ticketId}/history`);
    list.innerHTML = data.length ? data.map(h => `<li>
        <b>${esc(h.changedByName)}</b> (${esc(h.changedByRole)}):
        ${h.previousStatus ? `<span class="badge">${esc(h.previousStatus)}</span> → ` : "creó el ticket → "}
        <span class="badge">${esc(h.newStatus)}</span><br><small>${fmtDate(h.changedAt)}</small></li>`).join("")
      : "<li>Sin cambios registrados.</li>";
  } catch (e) { list.innerHTML = `<li class="msg error">${esc(e.message)}</li>`; }
}
