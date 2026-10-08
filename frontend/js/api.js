// Si Persona 1 ya tiene un helper de fetch con token, usa ese y borra este archivo.
const API_URL = "http://localhost:8080";           // ajusta puerto / CORS
const getToken = () => localStorage.getItem("token"); // ajusta la clave si Persona 1 usa otra

async function apiFetch(path, options = {}) {
  const res = await fetch(API_URL + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(getToken() ? { Authorization: "Bearer " + getToken() } : {}),
      ...(options.headers || {}),
    },
  });
  if (res.status === 401) { localStorage.removeItem("token"); window.location.href = "index.html"; return; }
  if (!res.ok) {
    let detail = "";
    try { const e = await res.json(); detail = e.message || e.error || ""; } catch {}
    throw new Error(detail || "Error " + res.status);
  }
  return res.status === 204 ? null : res.json();
}

function showMsg(el, text, type = "error") {
  el.textContent = text; el.className = "msg " + type; if (!text) el.className = "msg";
}
const fmtDate = (iso) => new Date(iso).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
// Evita inyección HTML (XSS) al pintar texto que escribió un usuario
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;" }[c]));
