// Requiere api.js. HTML: #stats-grid y #dash-msg
async function loadSummary() {
  const grid = document.getElementById("stats-grid");
  const msg = document.getElementById("dash-msg");
  grid.innerHTML = '<p class="loading">Cargando indicadores…</p>';
  try {
    const s = await apiFetch("/api/reports/summary");
    const items = [["Total", s.total], ["Abiertos", s.open], ["Asignados", s.assigned],
                   ["En proceso", s.inProgress], ["Resueltos", s.resolved], ["Cerrados", s.closed]];
    grid.innerHTML = items.map(([l, v]) => `<div class="stat"><b>${v}</b><span>${l}</span></div>`).join("");
  } catch (e) { grid.innerHTML = ""; showMsg(msg, "No se pudieron cargar los indicadores: " + e.message); }
}
document.addEventListener("DOMContentLoaded", loadSummary);
