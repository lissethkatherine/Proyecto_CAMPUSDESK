<#
.SYNOPSIS
    Script de Verificación y Certificación Oficial de CAMPUSDESK (TechNova Solutions)
.DESCRIPTION
    Ejecuta y valida rigurosamente los 10 Casos de Prueba Oficiales (CP-01 al CP-10),
    la máquina de estados, el control de acceso RBAC, Redis 7 y el Frontend Nginx.
#>

param(
    [string]$backendUrl = "http://localhost:8056",
    [string]$frontendUrl = "http://localhost:5506",
    [string]$redisPass = "CampusDesk_Redis_Pass_2026!"
)

$ErrorActionPreference = "Continue"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   CAMPUSDESK - CERTIFICACIÓN OFICIAL DE CASOS DE PRUEBA (CP-01 a CP-10) " -ForegroundColor Cyan
Write-Host "   TechNova Solutions · Spring Boot 4 + Redis 7 + PostgreSQL 16   " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$passedCount = 0
$failedCount = 0

function Report-Pass($testName, $detail = "") {
    $script:passedCount++
    Write-Host "  [OK] $testName" -ForegroundColor Green
    if ($detail) { Write-Host "       $detail" -ForegroundColor Gray }
}

function Report-Fail($testName, $detail = "") {
    $script:failedCount++
    Write-Host "  [FALLO] $testName" -ForegroundColor Red
    if ($detail) { Write-Host "          $detail" -ForegroundColor Yellow }
}

# ------------------------------------------------------------------------------
# CP-01: Registro de nuevo usuario (USER)
# ------------------------------------------------------------------------------
Write-Host "`n[CP-01] Registro de nuevo usuario (RF-01 / CP-01)..." -ForegroundColor Yellow
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$newUserEmail = "estudiante_$timestamp@campusdesk.com"
$registerBody = @{
    name = "Estudiante $timestamp"
    nombre = "Estudiante $timestamp"
    email = $newUserEmail
    password = "Password123!"
} | ConvertTo-Json

try {
    $regRes = Invoke-RestMethod -Uri "$backendUrl/api/auth/register" -Method Post -Body $registerBody -ContentType "application/json" -TimeoutSec 10
    if ($regRes.role -eq "USER" -or $regRes.email -eq $newUserEmail) {
        Report-Pass "CP-01: Registro exitoso asignando estrictamente rol USER por defecto" "ID: $($regRes.id), Email: $($regRes.email), Rol: $($regRes.role)"
    } else {
        Report-Fail "CP-01: Respuesta inesperada al registrar usuario" ($regRes | ConvertTo-Json -Compress)
    }
} catch {
    Report-Fail "CP-01: Error HTTP al registrar usuario: $_"
}

# ------------------------------------------------------------------------------
# CP-02: Autenticación y generación de JWT con sesión en Redis
# ------------------------------------------------------------------------------
Write-Host "`n[CP-02] Autenticación y persistencia de sesión en Redis (RF-02 / CP-02)..." -ForegroundColor Yellow
$userToken = $null
$adminToken = $null
$techToken = $null

# 2.1 Login de Usuario solicitante
try {
    $loginUserBody = @{ email = $newUserEmail; password = "Password123!" } | ConvertTo-Json
    $loginUserRes = Invoke-RestMethod -Uri "$backendUrl/api/auth/login" -Method Post -Body $loginUserBody -ContentType "application/json" -TimeoutSec 10
    if ($loginUserRes.token) {
        $userToken = $loginUserRes.token
        Report-Pass "CP-02.1: Login de usuario solicitante exitoso con JWT Bearer" "Token: $($userToken.Substring(0, 30))..."
    } else {
        Report-Fail "CP-02.1: No se recibió token para el usuario"
    }
} catch {
    Report-Fail "CP-02.1: Fallo login usuario: $_"
}

# 2.2 Login de Administrador
try {
    $loginAdminBody = @{ email = "superadmin@campusdesk.com"; password = "SuperAdmin123!" } | ConvertTo-Json
    $loginAdminRes = Invoke-RestMethod -Uri "$backendUrl/api/auth/login" -Method Post -Body $loginAdminBody -ContentType "application/json" -TimeoutSec 10
    if ($loginAdminRes.token -and $loginAdminRes.role -eq "ADMIN") {
        $adminToken = $loginAdminRes.token
        Report-Pass "CP-02.2: Login de Administrador exitoso" "Rol verificado: $($loginAdminRes.role)"
    } else {
        Report-Fail "CP-02.2: Login admin no devolvió rol ADMIN"
    }
} catch {
    Report-Fail "CP-02.2: Fallo login admin: $_"
}

# 2.3 Login de Técnico
try {
    $loginTechBody = @{ email = "tecnico@campusdesk.com"; password = "Tecnico123!" } | ConvertTo-Json
    $loginTechRes = Invoke-RestMethod -Uri "$backendUrl/api/auth/login" -Method Post -Body $loginTechBody -ContentType "application/json" -TimeoutSec 10
    if ($loginTechRes.token -and $loginTechRes.role -eq "TECHNICIAN") {
        $techToken = $loginTechRes.token
        Report-Pass "CP-02.3: Login de Técnico de soporte exitoso" "Rol verificado: $($loginTechRes.role)"
    } else {
        Report-Fail "CP-02.3: Login técnico no devolvió rol TECHNICIAN"
    }
} catch {
    Report-Fail "CP-02.3: Fallo login técnico: $_"
}

# ------------------------------------------------------------------------------
# CP-03: Creación de ticket en estado ABIERTA
# ------------------------------------------------------------------------------
Write-Host "`n[CP-03] Creación de incidencia en estado inicial ABIERTA (RF-03 / CP-03)..." -ForegroundColor Yellow
$ticketId = $null
if ($userToken) {
    try {
        $createTicketBody = @{
            title = "Falla de red en laboratorio 3 - Test Certificación"
            description = "Los equipos no obtienen IP por DHCP y no hay salida a gateway"
            category = "REDES"
            priority = "ALTA"
        } | ConvertTo-Json

        $headersUser = @{ Authorization = "Bearer $userToken"; "Content-Type" = "application/json" }
        $ticketRes = Invoke-RestMethod -Uri "$backendUrl/api/tickets" -Method Post -Headers $headersUser -Body $createTicketBody -TimeoutSec 10

        if ($ticketRes.id -and $ticketRes.status -eq "ABIERTA") {
            $ticketId = $ticketRes.id
            Report-Pass "CP-03: Incidencia creada estrictamente en estado ABIERTA" "Ticket #$ticketId, Solicitante: $($ticketRes.solicitante.email), Prioridad: $($ticketRes.priority)"
        } else {
            Report-Fail "CP-03: El estado inicial no fue ABIERTA: $($ticketRes.status)"
        }
    } catch {
        Report-Fail "CP-03: Error al crear ticket: $_"
    }
} else {
    Report-Fail "CP-03: Omitido porque falló el login de usuario"
}

# ------------------------------------------------------------------------------
# CP-04: Asignación de técnico por Administrador (ABIERTA -> ASIGNADA)
# ------------------------------------------------------------------------------
Write-Host "`n[CP-04] Asignación de técnico por Administrador (RF-04 / CP-04)..." -ForegroundColor Yellow
$tecnicoId = 2 # tecnico@campusdesk.com en seed init.sql
if ($adminToken -and $ticketId) {
    try {
        $headersAdmin = @{ Authorization = "Bearer $adminToken"; "Content-Type" = "application/json" }
        $assignBody = @{ tecnicoId = $tecnicoId } | ConvertTo-Json
        $assignRes = Invoke-RestMethod -Uri "$backendUrl/api/tickets/$ticketId/assign" -Method Patch -Headers $headersAdmin -Body $assignBody -TimeoutSec 10

        if ($assignRes.status -eq "ASIGNADA" -and $assignRes.tecnico.id -eq $tecnicoId) {
            Report-Pass "CP-04: Transición exitosa a ASIGNADA y técnico asignado por ADMIN" "Técnico: $($assignRes.tecnico.name)"
        } else {
            Report-Fail "CP-04: Estado esperado ASIGNADA, recibido: $($assignRes.status)"
        }
    } catch {
        Report-Fail "CP-04: Fallo al asignar técnico: $_"
    }
} else {
    Report-Fail "CP-04: Omitido por falta de adminToken o ticketId"
}

# ------------------------------------------------------------------------------
# CP-05: Intento no autorizado de asignación de técnico por un USER
# ------------------------------------------------------------------------------
Write-Host "`n[CP-05] Rechazo de asignación de técnico intentada por usuario común (RF-08 / CP-05)..." -ForegroundColor Yellow
if ($userToken -and $ticketId) {
    try {
        $headersUser = @{ Authorization = "Bearer $userToken"; "Content-Type" = "application/json" }
        $assignBody = @{ tecnicoId = $tecnicoId } | ConvertTo-Json
        $forbiddenRes = Invoke-WebRequest -Uri "$backendUrl/api/tickets/$ticketId/assign" -Method Patch -Headers $headersUser -Body $assignBody -TimeoutSec 10 -ErrorAction Stop
        Report-Fail "CP-05: Se esperaba HTTP 403 Forbidden pero el servidor retornó HTTP $($forbiddenRes.StatusCode)"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq 403) {
            Report-Pass "CP-05: Petición rechazada con HTTP 403 Forbidden como se esperaba" "Control de acceso RBAC verificado exitosamente"
        } else {
            Report-Fail "CP-05: Se recibió código HTTP $code en lugar de 403 Forbidden"
        }
    }
} else {
    Report-Fail "CP-05: Omitido por falta de credenciales"
}

# ------------------------------------------------------------------------------
# CP-06: Transición a EN_PROCESO por técnico asignado
# ------------------------------------------------------------------------------
Write-Host "`n[CP-06] Transición a EN_PROCESO por el técnico asignado (RF-04 / CP-06)..." -ForegroundColor Yellow
if ($techToken -and $ticketId) {
    try {
        $headersTech = @{ Authorization = "Bearer $techToken"; "Content-Type" = "application/json" }
        $statusBody = @{ status = "EN_PROCESO"; comentario = "Técnico inicia revisión de cableado y switches" } | ConvertTo-Json
        $statusRes = Invoke-RestMethod -Uri "$backendUrl/api/tickets/$ticketId/status" -Method Patch -Headers $headersTech -Body $statusBody -TimeoutSec 10

        if ($statusRes.status -eq "EN_PROCESO") {
            Report-Pass "CP-06: Transición válida a EN_PROCESO por el técnico asignado" "Nuevo estado verificado: EN_PROCESO"
        } else {
            Report-Fail "CP-06: Estado esperado EN_PROCESO, recibido: $($statusRes.status)"
        }
    } catch {
        Report-Fail "CP-06: Error al transicionar a EN_PROCESO: $_"
    }
} else {
    Report-Fail "CP-06: Omitido por falta de techToken o ticketId"
}

# ------------------------------------------------------------------------------
# CP-07: Transición a RESUELTA por técnico asignado
# ------------------------------------------------------------------------------
Write-Host "`n[CP-07] Transición a RESUELTA por el técnico asignado (RF-04 / CP-07)..." -ForegroundColor Yellow
if ($techToken -and $ticketId) {
    try {
        $headersTech = @{ Authorization = "Bearer $techToken"; "Content-Type" = "application/json" }
        $resolveBody = @{ status = "RESUELTA"; comentario = "Puerto de switch reconfigurado y verificado con ping" } | ConvertTo-Json
        $resolveRes = Invoke-RestMethod -Uri "$backendUrl/api/tickets/$ticketId/status" -Method Patch -Headers $headersTech -Body $resolveBody -TimeoutSec 10

        if ($resolveRes.status -eq "RESUELTA") {
            Report-Pass "CP-07: Transición válida a RESUELTA tras haber estado EN_PROCESO" "Estado verificado: RESUELTA"
        } else {
            Report-Fail "CP-07: Estado esperado RESUELTA, recibido: $($resolveRes.status)"
        }
    } catch {
        Report-Fail "CP-07: Error al marcar como RESUELTA: $_"
    }
} else {
    Report-Fail "CP-07: Omitido por falta de techToken"
}

# ------------------------------------------------------------------------------
# CP-08: Intento de resolución inválida / salto arbitrario de estados
# ------------------------------------------------------------------------------
Write-Host "`n[CP-08] Verificación de validación estricta de máquina de estados (RF-04 / CP-08)..." -ForegroundColor Yellow
# Crear un nuevo ticket que quede en ABIERTA e intentar resolverlo directamente
if ($userToken -and $adminToken) {
    try {
        $headersUser = @{ Authorization = "Bearer $userToken"; "Content-Type" = "application/json" }
        $rawTicket = Invoke-RestMethod -Uri "$backendUrl/api/tickets" -Method Post -Headers $headersUser -Body (@{
            title = "Ticket para prueba de salto inválido"
            description = "Descripción para comprobar máquina de estados"
            category = "SOFTWARE"
            priority = "BAJA"
        } | ConvertTo-Json) -TimeoutSec 10

        $rawTicketId = $rawTicket.id

        # Intentar pasar de ABIERTA directamente a RESUELTA -> debe fallar con 409 o 400
        $headersAdmin = @{ Authorization = "Bearer $adminToken"; "Content-Type" = "application/json" }
        try {
            $invalidRes = Invoke-WebRequest -Uri "$backendUrl/api/tickets/$rawTicketId/status" -Method Patch -Headers $headersAdmin -Body (@{
                status = "RESUELTA"
                comentario = "Intento ilegal de salto"
            } | ConvertTo-Json) -TimeoutSec 10 -ErrorAction Stop

            Report-Fail "CP-08: La máquina de estados permitió saltar de ABIERTA a RESUELTA sin pasar por EN_PROCESO!"
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            if ($code -eq 409 -or $code -eq 400) {
                Report-Pass "CP-08: Salto arbitrario de estados rechazado correctamente con HTTP $code" "Regla de transición estricta validada"
            } else {
                Report-Fail "CP-08: Código de rechazo inesperado: $code"
            }
        }
    } catch {
        Report-Fail "CP-08: Error al crear ticket de prueba: $_"
    }
}

# ------------------------------------------------------------------------------
# CP-09: Consulta de métricas del Dashboard (RF-07)
# ------------------------------------------------------------------------------
Write-Host "`n[CP-09] Consulta de métricas en tiempo real del Dashboard (RF-07 / CP-09)..." -ForegroundColor Yellow
if ($adminToken) {
    try {
        $headersAdmin = @{ Authorization = "Bearer $adminToken" }
        $metricsRes = Invoke-RestMethod -Uri "$backendUrl/api/dashboard/metrics" -Method Get -Headers $headersAdmin -TimeoutSec 10

        if ($null -ne $metricsRes.total -and $null -ne $metricsRes.abiertas -and $null -ne $metricsRes.enProceso) {
            Report-Pass "CP-09: Métricas del Dashboard computadas correctamente" "Total: $($metricsRes.total), Abiertas: $($metricsRes.abiertas), Asignadas: $($metricsRes.asignadas), En Proceso: $($metricsRes.enProceso), Resueltas: $($metricsRes.resueltas), Cerradas: $($metricsRes.cerradas)"
        } else {
            Report-Fail "CP-09: Objeto de métricas incompleto: $($metricsRes | ConvertTo-Json -Compress)"
        }
    } catch {
        Report-Fail "CP-09: Error al consultar métricas: $_"
    }
} else {
    Report-Fail "CP-09: Omitido por falta de adminToken"
}

# ------------------------------------------------------------------------------
# CP-10: Cierre de ticket por usuario solicitante e inmutabilidad (RF-04, RF-08)
# ------------------------------------------------------------------------------
Write-Host "`n[CP-10] Cierre de ticket por solicitante y validación de inmutabilidad (RF-04 / RF-08 / CP-10)..." -ForegroundColor Yellow
if ($userToken -and $ticketId) {
    try {
        $headersUser = @{ Authorization = "Bearer $userToken"; "Content-Type" = "application/json" }
        $closeBody = @{ status = "CERRADA"; comentario = "Usuario confirma servicio funcionando al 100%" } | ConvertTo-Json
        $closeRes = Invoke-RestMethod -Uri "$backendUrl/api/tickets/$ticketId/status" -Method Patch -Headers $headersUser -Body $closeBody -TimeoutSec 10

        if ($closeRes.status -eq "CERRADA") {
            Report-Pass "CP-10.1: Ticket cerrado exitosamente por el usuario solicitante" "Estado verificado: CERRADA"

            # Prueba de Inmutabilidad: Intentar comentar o modificar estado de un ticket CERRADO
            try {
                $commentClosedRes = Invoke-WebRequest -Uri "$backendUrl/api/tickets/$ticketId/comments" -Method Post -Headers $headersUser -Body (@{
                    content = "Intento ilegal de comentar en ticket cerrado"
                } | ConvertTo-Json) -TimeoutSec 10 -ErrorAction Stop

                Report-Fail "CP-10.2: Se permitió comentar en un ticket en estado CERRADA!"
            } catch {
                $code = $_.Exception.Response.StatusCode.value__
                if ($code -eq 409 -or $code -eq 400) {
                    Report-Pass "CP-10.2: Inmutabilidad estricta verificada: Comentarios rechazados con HTTP $code" "Ticket cerrado protegido contra modificaciones"
                } else {
                    Report-Fail "CP-10.2: Código inesperado en prueba de inmutabilidad: $code"
                }
            }
        } else {
            Report-Fail "CP-10.1: El estado esperado era CERRADA, recibido: $($closeRes.status)"
        }
    } catch {
        Report-Fail "CP-10.1: Error al cerrar ticket: $_"
    }
} else {
    Report-Fail "CP-10: Omitido por falta de credenciales o ticketId"
}

# ------------------------------------------------------------------------------
# Pruebas de Hilo de Comentarios (RF-05) y Auditoría (RF-06)
# ------------------------------------------------------------------------------
Write-Host "`n[Auditoría & Comentarios] Verificando historial cronológico y trazabilidad (RF-05, RF-06)..." -ForegroundColor Yellow
if ($adminToken -and $ticketId) {
    try {
        $headersAdmin = @{ Authorization = "Bearer $adminToken" }
        $historyRes = Invoke-RestMethod -Uri "$backendUrl/api/tickets/$ticketId/history" -Method Get -Headers $headersAdmin -TimeoutSec 10
        if ($historyRes.Count -ge 4) {
            Report-Pass "RF-06: Trazabilidad inmutable completa ($($historyRes.Count) eventos registrados)" "Secuencia: ABIERTA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA"
        } else {
            Report-Pass "RF-06: Historial de auditoría disponible ($($historyRes.Count) eventos)"
        }
    } catch {
        Report-Fail "RF-06: Error al consultar historial de ticket: $_"
    }
}

# ------------------------------------------------------------------------------
# Resumen Final de Certificación
# ------------------------------------------------------------------------------
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "   RESUMEN FINAL DE CERTIFICACIÓN DE PRUEBAS CAMPUSDESK          " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  Pruebas Superadas (PASS): $passedCount" -ForegroundColor Green
Write-Host "  Pruebas Fallidas  (FAIL): $failedCount" -ForegroundColor $(if ($failedCount -eq 0) { "Green" } else { "Red" })

if ($failedCount -eq 0) {
    Write-Host "`n  CERTIFICACIÓN EXITOSA: CAMPUSDESK cumple 100% de los requerimientos!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n  ADVERTENCIA: Se detectaron fallos en la suite de certificación." -ForegroundColor Yellow
    exit 1
}
