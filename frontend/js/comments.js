// Requiere api.js. HTML: #comments-list, #comment-text, #comment-send, #comment-msg
async function loadComments(ticketId) {
  const list = document.getElementById("comments-list");
  list.innerHTML = '<p class="loading">Cargando comentarios…</p>';
  try {
    const data = await apiFetch(`/api/tickets/${ticketId}/comments`);
    list.innerHTML = data.length
      ? data.map(c => `<div class="comment"><b>${esc(c.authorName)}</b> <span class="badge">${esc(c.authorRole)}</span>
          <p>${esc(c.content)}</p><small>${fmtDate(c.createdAt)}</small></div>`).join("")
      : "<p>Aún no hay comentarios.</p>";
  } catch (e) { list.innerHTML = `<p class="msg error">${esc(e.message)}</p>`; }
}

function initCommentForm(ticketId) {
  const btn = document.getElementById("comment-send");
  const txt = document.getElementById("comment-text");
  const msg = document.getElementById("comment-msg");
  btn.addEventListener("click", async () => {
    if (!txt.value.trim()) return showMsg(msg, "Escribe un comentario.");
    btn.disabled = true;
    try {
      await apiFetch(`/api/tickets/${ticketId}/comments`, { method: "POST", body: JSON.stringify({ content: txt.value }) });
      txt.value = ""; showMsg(msg, "Comentario enviado.", "ok");
      await loadComments(ticketId);
    } catch (e) { showMsg(msg, e.message); }   // ej. 409 si el ticket está CERRADA
    finally { btn.disabled = false; }
  });
}
