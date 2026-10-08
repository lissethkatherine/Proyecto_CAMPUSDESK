-- Tablas de Persona 3. Requiere que ya existan "users" y "tickets".
-- Si usas spring.jpa.hibernate.ddl-auto=update, Hibernate las crea solo;
-- este script sirve como entrega del requisito "script SQL".

CREATE TABLE IF NOT EXISTS comments (
    id          BIGSERIAL PRIMARY KEY,
    content     VARCHAR(1000) NOT NULL,
    author_id   BIGINT NOT NULL REFERENCES users(id),
    ticket_id   BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS status_history (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    previous_status VARCHAR(20),
    new_status      VARCHAR(20) NOT NULL,
    changed_by      BIGINT NOT NULL REFERENCES users(id),
    changed_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comments_ticket ON comments(ticket_id, created_at);
CREATE INDEX IF NOT EXISTS idx_history_ticket  ON status_history(ticket_id, changed_at);

-- Consulta para comprobar CP-14 (debe coincidir con /api/reports/summary):
-- SELECT status, COUNT(*) FROM tickets GROUP BY status;
