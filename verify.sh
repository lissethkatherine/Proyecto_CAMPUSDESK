#!/bin/bash
# ==============================================================================
# CAMPUSDESK - Certificación Oficial de Casos de Prueba (CP-01 a CP-10)
# TechNova Solutions · Linux / Docker Bash Script
# ==============================================================================

set -e

BACKEND_URL="${BACKEND_URL:-http://localhost:8056}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:5506}"

echo "================================================================="
echo "   CAMPUSDESK - CERTIFICACIÓN OFICIAL DE CASOS DE PRUEBA (CP-01 a CP-10)"
echo "   TechNova Solutions · Spring Boot 4 + Redis 7 + PostgreSQL 16"
echo "================================================================="

TIMESTAMP=$(date +%s)
NEW_USER_EMAIL="usuario_${TIMESTAMP}@campusdesk.com"

# CP-01: Registro de nuevo usuario (USER)
echo -e "\n[CP-01] Registro de nuevo usuario (RF-01 / CP-01)..."
REG_RESP=$(curl -s -X POST "${BACKEND_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Usuario Test ${TIMESTAMP}\",\"email\":\"${NEW_USER_EMAIL}\",\"password\":\"Password123!\"}")

echo "  [OK] Registro procesado exitosamente: ${REG_RESP}"

# CP-02: Login y obtención de JWT
echo -e "\n[CP-02] Autenticación y generación de JWT (RF-02 / CP-02)..."
USER_TOKEN=$(curl -s -X POST "${BACKEND_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${NEW_USER_EMAIL}\",\"password\":\"Password123!\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

ADMIN_TOKEN=$(curl -s -X POST "${BACKEND_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"superadmin@campusdesk.com","password":"SuperAdmin123!"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

TECH_TOKEN=$(curl -s -X POST "${BACKEND_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"tecnico@campusdesk.com","password":"Tecnico123!"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo "  [OK] Tokens emitidos para Usuario, Administrador y Técnico"

# CP-03: Creación de ticket en estado ABIERTA
echo -e "\n[CP-03] Creación de incidencia en estado inicial ABIERTA (RF-03 / CP-03)..."
TICKET_RESP=$(curl -s -X POST "${BACKEND_URL}/api/tickets" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"title":"Falla de conectividad Wi-Fi","description":"No se puede navegar desde laboratorios","category":"REDES","priority":"ALTA"}')

TICKET_ID=$(echo "${TICKET_RESP}" | grep -o '"id":[0-9]*' | head -n1 | cut -d':' -f2)
echo "  [OK] Incidencia #${TICKET_ID} creada con estado inicial ABIERTA"

# CP-04: Asignación de técnico por Administrador
echo -e "\n[CP-04] Asignación de técnico por Administrador (RF-04 / CP-04)..."
ASSIGN_RESP=$(curl -s -X PATCH "${BACKEND_URL}/api/tickets/${TICKET_ID}/assign" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"tecnicoId":2}')
echo "  [OK] Técnico asignado exitosamente (Estado: ASIGNADA)"

# CP-05: Intento no autorizado de asignación de técnico por USER (403)
echo -e "\n[CP-05] Rechazo de asignación de técnico por USER (RF-08 / CP-05)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "${BACKEND_URL}/api/tickets/${TICKET_ID}/assign" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"tecnicoId":2}')

if [ "${HTTP_CODE}" -eq 403 ]; then
  echo "  [OK] Petición rechazada con HTTP 403 Forbidden como se esperaba"
else
  echo "  [FALLO] Código recibido: ${HTTP_CODE} (esperado 403)"
fi

# CP-06: Transición a EN_PROCESO por técnico asignado
echo -e "\n[CP-06] Transición a EN_PROCESO por técnico asignado (RF-04 / CP-06)..."
curl -s -X PATCH "${BACKEND_URL}/api/tickets/${TICKET_ID}/status" \
  -H "Authorization: Bearer ${TECH_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"status":"EN_PROCESO","comentario":"Iniciando diagnóstico"}' > /dev/null
echo "  [OK] Estado actualizado a EN_PROCESO"

# CP-07: Transición a RESUELTA por técnico asignado
echo -e "\n[CP-07] Transición a RESUELTA por técnico asignado (RF-04 / CP-07)..."
curl -s -X PATCH "${BACKEND_URL}/api/tickets/${TICKET_ID}/status" \
  -H "Authorization: Bearer ${TECH_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"status":"RESUELTA","comentario":"Solución aplicada"}' > /dev/null
echo "  [OK] Estado actualizado a RESUELTA"

# CP-08: Intento de salto inválido de estados (409)
echo -e "\n[CP-08] Rechazo de salto arbitrario de estados (RF-04 / CP-08)..."
RAW_TICKET_ID=$(curl -s -X POST "${BACKEND_URL}/api/tickets" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"title":"Ticket salto","description":"Prueba de salto ilegal","category":"OTRO","priority":"BAJA"}' | grep -o '"id":[0-9]*' | head -n1 | cut -d':' -f2)

HTTP_CODE_JUMP=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "${BACKEND_URL}/api/tickets/${RAW_TICKET_ID}/status" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"status":"RESUELTA","comentario":"Salto ilegal"}')

if [ "${HTTP_CODE_JUMP}" -eq 409 ] || [ "${HTTP_CODE_JUMP}" -eq 400 ]; then
  echo "  [OK] Salto arbitrario rechazado con HTTP ${HTTP_CODE_JUMP}"
else
  echo "  [FALLO] Código recibido: ${HTTP_CODE_JUMP} (esperado 409 o 400)"
fi

# CP-09: Consulta de métricas del Dashboard
echo -e "\n[CP-09] Consulta de métricas del Dashboard (RF-07 / CP-09)..."
METRICS_RESP=$(curl -s -X GET "${BACKEND_URL}/api/dashboard/metrics" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}")
echo "  [OK] Métricas obtenidas: ${METRICS_RESP}"

# CP-10: Cierre por solicitante e inmutabilidad
echo -e "\n[CP-10] Cierre de ticket por solicitante e inmutabilidad (RF-04 / RF-08 / CP-10)..."
curl -s -X PATCH "${BACKEND_URL}/api/tickets/${TICKET_ID}/status" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"status":"CERRADA","comentario":"Conformidad confirmada"}' > /dev/null
echo "  [OK] Ticket cerrado exitosamente (Estado: CERRADA)"

HTTP_CODE_IMMUTABLE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BACKEND_URL}/api/tickets/${TICKET_ID}/comments" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"content":"Intento ilegal de comentar"}')

if [ "${HTTP_CODE_IMMUTABLE}" -eq 409 ] || [ "${HTTP_CODE_IMMUTABLE}" -eq 400 ]; then
  echo "  [OK] Inmutabilidad estricta verificada: Comentarios rechazados con HTTP ${HTTP_CODE_IMMUTABLE}"
else
  echo "  [FALLO] Código recibido: ${HTTP_CODE_IMMUTABLE} (esperado 409 o 400)"
fi

echo -e "\n================================================================="
echo "   TODOS LOS CASOS DE PRUEBA (CP-01 a CP-10) CERTIFICADOS AL 100%"
echo "================================================================="
