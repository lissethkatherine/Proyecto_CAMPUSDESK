|`╔══════════════════════════════════════════════════════════════════════╗`<br>`║                         GESTIÓN DE TICKETS                           ║`<br>`╠══════════════════════╦═══════════════════════════════════════════════╣`<br>|
|---|
|`║ Campo                ║ Descripción                                   ║`<br>`╠══════════════════════╬═══════════════════════════════════════════════╣`|
|`║ Identificador        ║ Código único de la solicitud                 ║`<br>|
|`║ Título               ║ Nombre descriptivo del problema              ║`<br>|
|`║ Descripción          ║ Explicación detallada                        ║`<br>|
|`║ Categoría            ║ Tipo de incidencia                           ║`<br>|
|`║ Prioridad            ║ Nivel de urgencia                            ║`<br>|
|`║ Estado               ║ Situación actual                             ║`<br>|
|`║ Solicitante          ║ Usuario que creó el ticket                   ║`<br>|
|`║ Técnico              ║ Responsable asignado, si existe             ║`<br>|
|`║ Fecha de creación    ║ Momento del registro                         ║`<br>|
|`║ Fecha de actualización║ Última modificación                        ║`<br>`╚══════════════════════╩═══════════════════════════════════════════════╝`|



|`╔══════════════════════════════════════════════════════════════════════╗`<br>`║                            PERSISTENCIA                             ║`<br>`╠══════════════════════╦═══════════════════════════════════════════════╣`<br>|
|---|
|`║ Entidad              ║ Responsabilidad                              ║`<br>|
|`╠══════════════════════╬═══════════════════════════════════════════════╣`<br>`║ User                 ║ Información y rol de los usuarios            ║`|
|<br>`║ Ticket               ║ Registro de incidencias                      ║`|
|<br>`║ Comment              ║ Comentarios de cada ticket                   ║`<br>|
|`║ StatusHistory        ║ Historial de cambios de estado               ║`<br>`╚══════════════════════╩═══════════════════════════════════════════════╝`|



|`╔══════════════════════════════════════════════════════════════════════╗`<br>`║                              API REST                               ║`<br>`╠════════╦══════════════════════════════╦═══════════════════════════════╣`<br>|
|---|
|`║ Método ║ Endpoint                     ║ Funcionalidad                 ║`<br>`╠════════╬══════════════════════════════╬═══════════════════════════════╣`|
|`║ POST   ║ /api/auth/register           ║ Registrar usuario             ║`<br>|
|`║ POST   ║ /api/auth/login              ║ Iniciar sesión                ║`<br>|
|`║ GET    ║ /api/users/technicians       ║ Listar técnicos               ║`<br>|
|`║ GET    ║ /api/tickets                 ║ Listar tickets autorizados    ║`<br>|
|`║ GET    ║ /api/tickets/{id}            ║ Consultar detalle             ║`<br>|
|`║ POST   ║ /api/tickets                 ║ Crear solicitud               ║`|
|<br>`║ PUT    ║ /api/tickets/{id}            ║ Actualizar solicitud permitida║`<br>|
|`║ PATCH  ║ /api/tickets/{id}/assign     ║ Asignar técnico               ║`<br>|
|`║ PATCH  ║ /api/tickets/{id}/status     ║ Cambiar estado                ║`<br>|
|`║ GET    ║ /api/tickets/{id}/comments   ║ Obtener comentarios           ║`<br>|
|`║ POST   ║ /api/tickets/{id}/comments   ║ Registrar comentario          ║`<br>|
|`║ GET    ║ /api/tickets/{id}/history    ║ Consultar historial           ║`<br>|
|`║ GET    ║ /api/reports/summary         ║ Obtener indicadores            ║`<br>`╚════════╩══════════════════════════════╩═══════════════════════════════╝`|



|`╔══════════════════════════════════════════════════════════════════════╗`<br>`║                    VALIDACIONES Y EXCEPCIONES                       ║`<br>`╠═════════════════════════════════════════════════════════════════════╣`|
|---|
|`╦`<br>`║ Código ║ Situación                                                   ║`<br>`╠════════╬═════════════════════════════════════════════════════════════╣`|
|`║ 200    ║ Consulta u operación exitosa                               ║`<br>`║ 201    ║ Recurso creado                                               ║`<br>|
|`║ 400    ║ Solicitud o validación incorrecta                           ║`<br>|
|`║ 401    ║ Autenticación requerida o inválida                          ║`<br>|
|`║ 403    ║ Acceso denegado                                              ║`<br>|
|`║ 404    ║ Recurso no encontrado                                        ║`|
|<br>`║ 409    ║ Conflicto con las reglas del negocio                        ║`<br>|
|`║ 500    ║ Error interno inesperado                                     ║`|



```
╚════════╩═════════════════════════════════════════════════════════════╝
```

|`╔══════════════════════════════════════════════════════════════════════╗`<br>`║                    CASOS DE PRUEBA OBLIGATORIOS                    ║`<br>`╠════════╦══════════════════════════════════════╦═══════════════════════╣`<br>|
|---|
|`║ Código ║ Escenario                            ║ Resultado esperado    ║`<br>`╠════════╬══════════════════════════════════════╬═══════════════════════╣`|
|`║ CP-01  ║ Registrar un usuario válido          ║ Cuenta creada USER    ║`<br>`║ CP-02  ║ Registrar un usuario duplicado       ║ Rechazo operación     ║`<br>`║ CP-03  ║ Iniciar sesión correctamente         ║ JWT válido             ║`<br>`║ CP-04  ║ Endpoint protegido sin token         ║ HTTP 401               ║`<br>`║ CP-05  ║ Crear un ticket                      ║ Estado ABIERTA         ║`<br>`║ CP-06  ║ Consultar ticket ajeno como USER     ║ HTTP 403 o 404         ║`<br>`║ CP-07  ║ Asignar técnico como ADMIN           ║ Ticket ASIGNADA        ║`<br>`║ CP-08  ║ Intentar asignar como USER           ║ HTTP 403               ║`<br>`║ CP-09  ║ Modificar ticket ajeno como técnico  ║ Operación rechazada    ║`<br>`║ CP-10  ║ Resolver sin pasar por EN_PROCESO    ║ HTTP 409               ║`<br>`║ CP-11  ║ Registrar cambio de estado           ║ Registro histórico     ║`<br>`║ CP-12  ║ Cerrar ticket RESUELTO               ║ Estado CERRADA         ║`<br>`║ CP-13  ║ Comentar ticket CERRADA              ║ Operación rechazada    ║`<br>`║ CP-14  ║ Consultar estadísticas               ║ Coincide PostgreSQL    ║`<br>`║ CP-15  ║ Consumir API desde frontend          ║ Comunicación exitosa   ║`<br>`║ CP-16  ║ JWT inválido o expirado              ║ HTTP 401               ║`<br>`╚════════╩══════════════════════════════════════╩═══════════════════════╝`|



