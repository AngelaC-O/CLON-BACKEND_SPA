# Notas de la Iteración 2 · Back-end

Desde el 08/11/2026 las dudas y decisiones van aquí. `docs/NOTAS_IT1.md` queda como historia y no se edita; los números D-xx y DF-xx de la Iteración 1 se conservan y los nuevos siguen la numeración (D-14, DF-58...).

## Dudas

Abiertas de la Iteración 1:

- **D-05. `codigo_operacion` del enlace de pago.** `ServicioPago.confirmar` busca con `findByCodigoOperacion`, pero en CUS07 ningún mensaje guarda ese código antes del pago. En B4 se agregó la sobrecarga `registrarEnlace(enlacePago, fechaExpiracion, codigoOperacion)`, porque sin el código el webhook no encuentra la transacción. Sigue sin confirmarse.
- **D-09. Almuerzo de la especialista (Iteración 3).** `HORARIO_PERSONAL.minutos_almuerzo` da los minutos, pero no a qué hora, así que las horas libres y la agenda no lo descuentan. Queda para la Iteración 3.
- **DF-52. Importe del comprobante con adelanto.** El comprobante y su PDF llevan `subtotal + igv`, no `Venta.total`, que ya resta adelanto y saldo a favor. SUNAT trata los anticipos con campos propios, que no se emiten. Hay que revisarlo con contabilidad.
- **DF-49. Reporte de liquidación y claves TEST de Izipay.** Faltan las columnas del reporte de liquidación que se importa (CUS47) y las claves TEST de Izipay, que la dueña aún no solicita. Mientras tanto se usa el adaptador falso.
- **DATOS-01. Datos del emisor.** Ya se recibieron. Se cargan solo en producción, por las variables `CAUTIVA_EMPRESA_*` y `SEDE.direccion`; en desarrollo no se tocan ni se escriben en el repositorio.

Nuevas (08/11/2026, al preparar el v5.1):

- **D-14. RN03: la especialista queda libre al terminar.** Ver DF-58: Oracle sigue bloqueando a la especialista durante la limpieza del recurso. ¿Se cambia `PKG_AGENDA` (sería otra migración) o se acepta así en esta iteración?
- **D-15. Auditoría de la reapertura.** El diccionario dice que la historia de las reaperturas «queda en AUDITORIA», pero `TRG_AUD_CAJA` (script 10, generado) no conoce las tres columnas nuevas: registra `estado: CERRADA -> ABIERTA` y el cierre que se limpia, no el motivo ni quién reabrió (eso queda solo en la fila de CAJA, y solo la última vez). La hoja «Cambios v5.1» no lo pide y no se tocó. ¿Se regenera `TRG_AUD_CAJA`?
- **D-16. «Caja del día» en la reapertura.** RN30 habla de reabrir la caja del día, pero ni la hoja «Cambios v5.1» ni `TRG_CAJA_REGLAS` lo exigen: Oracle deja reabrir cualquier caja cerrada si no hay otra ABIERTA en la sede. Por ahora lo validará Java (CUS46). ¿Se agrega también en Oracle?
- **D-17. `99_eliminar_v5.sql`.** Solo borra las 55 tablas de v5; SOLICITUD_ANULACION y CONSUMO_PENDIENTE quedan. `database/README.md` indica el `DROP TABLE` a mano. ¿Se actualiza 99 la próxima vez que se toque `database/`?
- **D-18. Base local.** La base de Docker (`localhost:1522`, dueño `SYNAPSIS_SPA_CAUTIVA`) sigue en v5: en esta tarea 17 solo se corrió en un contenedor limpio de pruebas. Para usarla con el back-end de la Iteración 2 hay que correr ahí `17_datos_iteracion2.sql` como dueño.

Nuevas (09/11/2026, entidades y repositorios):

- **D-19. Reglas de la conciliación.** El diagrama de CUS47 solo nombra `generarItems(ventas, comprobantes, transacciones)`; los criterios no están en ningún documento. Se programaron así (ver DF-70): un ítem por venta del mes, que COINCIDE si SUNAT ACEPTÓ su comprobante y sus cobros digitales tienen fecha de abono (el efectivo se respalda en caja); un ítem SIN_JUSTIFICAR por cada comprobante sin venta vigente del mes, salvo el de una venta ANULADA cuya nota de crédito SUNAT aceptó; y uno por cada transacción que no respalda una venta del mes (cobro sin venta ni cita, adelanto sin abono, devolución). ¿Se confirman con la dueña y la contadora?
- **D-20. Tipos de reporte de CUS48.** `RepositorioReporte` solo tiene `ventasAgrupadas` en el diagrama. Se implementaron las agrupaciones de RF68 (DIA, MES, LINEA, SERVICIO, ESPECIALISTA, TIPO_ATENCION, MEDIO_PAGO). Los otros reportes de la nota de CUS48 (productos y margen RF69, margen por servicio RF70, demanda RF71, ocupación RF72, ingresos por especialista RF73) leen otras vistas (V_MARGEN_PRODUCTO, V_COSTO_SERVICIO, V_DEMANDA_SERVICIOS, V_OCUPACION_RECURSO) y no tienen método en el diagrama. ¿Se agrega un método por tipo de reporte cuando se haga ServicioReporte?
- **D-21. `NOTA_CREDITO.id_usuario_solicita`.** La nota de CUS23 dice que sale de la solicitud, pero `TRG_NOTA_CREDITO_REGLAS` (script 09) lo reemplaza por el usuario de PKG_SESION, que al aprobar es la Administradora (DF-66). ¿Se acepta así o se cambia el trigger en una próxima migración? La solicitud conserva siempre quién la pidió.

Nuevas (09/11/2026, CUS modificados de la Iteración 1):

- **D-14 (sigue abierta, ahora con efecto visible).** Java ya aplica RN03 como pide la tarea: la especialista queda libre al terminar el servicio y solo el recurso queda en limpieza (`GestorDisponibilidadAgenda`, ver DF-81). Oracle no cambió: `TRG_CITA_REGLAS` guarda el fin con la limpieza y `PKG_AGENDA.validar_cruce` lo usa también para la especialista. Efecto: las horas libres y la agenda ofrecen, por ejemplo, a especialista1 a las 10:30 en otro sillón después de una cita de 10:00 a 10:30, pero si se agenda ahí Oracle responde `-20010` (409, RN02) hasta las 10:40. Lo mismo pasa con una atención que termina antes de lo declarado: Java libera a la especialista desde `hora_fin` y Oracle la sigue ocupando `hora_fin + limpieza`. Para que las dos capas coincidan hace falta una migración de `PKG_AGENDA` (y de `fecha_hora_fin` de CITA o de la parte de la especialista en `validar_cruce`). ¿Se hace en la siguiente migración?
- **D-22. Cobros PENDIENTES de una venta.** `TRANSACCION_PASARELA` sigue sin `id_venta` (segunda parte de D-13): `GET /ventas/{idVenta}` no puede listar los cobros pendientes de esa venta, solo el estado actual de las transacciones de sus pagos registrados (`PagoVentaDTO.estadoTransaccion`). El front-end guarda el `idTransaccion` que viene en `cobrosPendientes` del 202 y, cuando el webhook lo aprueba, registra el pago con `POST /ventas/{idVenta}/pagos` (`idTransaccion`) y cierra con `POST /ventas/{idVenta}/cierre`. ¿Se agrega `id_venta` a TRANSACCION_PASARELA en una migración, o un `GET` de la transacción para que el front-end sepa cuándo quedó APROBADA?
- **D-23. Firma de `AvisoProvider`.** El diagrama de CUS50 dice `AvisoProvider.avisosPara(idSede, permisos: List<Permiso>)`; la tarea pidió `avisos(idSede)` con el permiso requerido dentro de cada `AvisoDTO`, y que ServicioAviso filtre. Se siguió la tarea (DF-79). ¿Se corrige el diagrama o se cambia el código antes de que B10, B12 y B13 agreguen sus proveedores?
- **D-24. `docs/openapi.yaml`.** `ContratoOpenApiTest` exige que `docs/openapi.yaml` sea la salida de springdoc, y los 13 GET nuevos y los campos agregados la cambian. CLAUDE.md no deja tocar `docs/` (salvo este archivo), así que no se volvió a exportar y `docsOpenapiEsLaSalidaDelCodigo` queda en rojo (ver «Verificación de los CUS modificados»). Se vuelve a exportar con `./mvnw test -Dtest=ContratoOpenApiTest -Dcautiva.openapi.exportar=true`. ¿Se autoriza?

## Diferencias encontradas

- **DF-58. RN03 ajustada frente a `PKG_AGENDA`.** RN03 (08/11) dice que la especialista queda libre al terminar el servicio y que solo el recurso queda bloqueado por la limpieza. Pero `TRG_CITA_REGLAS` guarda en `CITA.fecha_hora_fin` el fin con la limpieza, y `PKG_AGENDA.validar_cruce` usa ese fin para la especialista y para el recurso; `GestorDisponibilidadAgenda` hace lo mismo en Java. La hoja «Cambios v5.1» no cambia ni `PKG_AGENDA` ni `TRG_CITA_*`, así que no se tocaron (D-14). Las pruebas Java ajustadas siguen ese comportamiento (por ejemplo, la agenda de la especialista ocupada de 10:00 a 10:40).
- **DF-59. Casos de 20 que cambian con el v5.1.** Con 17, `20_pruebas_reglas_v5.sql` da 142 OK y 2 FALLO, sin editarlo: «El fin incluye la limpieza: 10:00 + 30 + 30 = 11:00 (RN03)» (sale 10:40) y «Mismo sillón a las 10:45 con otra especialista (aún en limpieza)» (pasa). Sus recursos de prueba toman el DEFAULT de `minutos_limpieza`, que ahora es 10 (cambio 1), así que el sillón queda libre a las 10:40. Los demás casos, incluido «Reabrir la caja cerrada» (sin motivo, `-20060`) y «Vender en la caja cerrada» (`-20042`), siguen pasando.
- **DF-60. `CONSUMO_PENDIENTE.cantidad_pendiente`.** El diccionario la marca como derivada, pero la hoja «Cambios v5.1» no le pide al trigger que la calcule: `CK_CONSUMO_PEND_CANTIDADES` solo la valida. Java la envía (estimada − descontada).
- **DF-61. Aprobar una solicitud con su nota de crédito.** Como una solicitud resuelta no cambia (`-20102`), la nota de crédito se emite primero y su `id_nota_credito` va en el mismo UPDATE que pasa la solicitud a APROBADA. Si SUNAT había rechazado el comprobante, se aprueba con `id_nota_credito` nulo (lo permite `CK_SOL_ANUL_NOTA_CREDITO`).
- **DF-62. Orden de las validaciones al reabrir la caja.** `TRG_CAJA_REGLAS` revisa primero el motivo (`-20060`) y después que sea la Administradora (`-20003`). Así una reapertura sin motivo sigue siendo el `-20060` de siempre (el caso «Reabrir la caja cerrada» de 20 no cambia) y la de una recepcionista con motivo es 403.
- **DF-63. Otra caja ABIERTA al reabrir.** Que no haya otra caja ABIERTA en la sede lo garantiza el índice `UQ_CAJA_ABIERTA_SEDE` (`ORA-00001`), no el trigger: una regla por fila sobre CAJA no puede consultar CAJA (tabla mutante). Java lo valida antes y traduce el `ORA-00001` a 409.

Entidades y repositorios de la Iteración 2 (09/11/2026). Cada punto es un método o un tipo que no está igual en los diagramas de clases del diseño de `Iteracion_2_Cautiva_Spa.drawio`:

- **DF-64. `NotaCredito(oVenta, ...)`.** El diagrama de CUS23 pasa `idVenta`, pero COMPROBANTE → VENTA es una relación JPA dentro del agregado de la venta, que necesita la entidad; el servicio ya la tiene (`RepositorioVenta.findById`). La nota entra a `Venta.comprobantes` por `Venta.agregarNotaCredito` (paquete), que exige venta CERRADA, una sola nota y la clase correcta (NC_BOLETA anula BOLETA).
- **DF-65. `Venta.anular(idUsuarioAutoriza)`.** Se conserva el parámetro del diagrama, pero no se guarda: `VENTA.id_usuario_autoriza` es obsoleta (v5.1) y la entidad la mapea solo de lectura (`insertable = false, updatable = false`). El método exige venta CERRADA y su nota de crédito, o el comprobante RECHAZADO por SUNAT, como `PKG_VENTA.validar_anulacion`.
- **DF-66. `NotaCredito.idUsuarioSolicita`.** `TRG_NOTA_CREDITO_REGLAS` lo reemplaza por el usuario de PKG_SESION; la entidad lo marca `@Generated` para leer lo que guardó Oracle (D-21).
- **DF-67. `Rol.verificarPermisosMinimos(idsPermisosSeguridad)`.** El diagrama de CUS53 no le da parámetros, pero ROL_PERMISO solo guarda el id del permiso y Rol no conoce módulo ni acción: el servicio le pasa los id de SEGURIDAD:VER, SEGURIDAD:CREAR y SEGURIDAD:EDITAR (`RepositorioPermiso.findOrCreate`).
- **DF-68. `Producto.calcularPrecioVenta(margen: BigDecimal)`.** El diagrama de CUS32 dice `margen: String`, pero la secuencia lo obtiene con `ParametroApi.obtenerValorNumero` (MARGEN_PRODUCTO_PCT o MARGEN_PREMIUM_PCT, en porcentaje): precio = costo × (1 + margen / 100), a 2 decimales.
- **DF-69. Kardex de Producto.** `registrarSalida` no devuelve nada y `registrarAjuste` devuelve `boolean` (false si la diferencia es 0 y no se registra, como dice la nota de CUS34). Se agregó `Producto.ultimoMovimiento(idSede)`, que no está en el diagrama: CUS33 necesita el `idMovimiento` de la salida para regularizar el consumo pendiente, y `save()` de un producto existente hace `merge`, que guarda una copia del movimiento. `MovimientoKardex` avisa con `@PostPersist` (`alGuardar` → `Producto.alGuardarMovimiento`, de paquete) la copia guardada, con el id y el saldo que puso Oracle. `estaEnMinimo` usa ese saldo y no carga todo el kardex.
- **DF-70. Conciliación.** El diagrama de CUS47 dice `generarItems(ventas: List<VentaResumenDTO>, comprobantes: String, transacciones: String)` y `justificarItem(idItem, estado: EstadoConciliacion, justificacion)`. Se usaron los tipos de la secuencia: `List<ComprobanteSunatDTO>` (`comprobantesDelMes`), `List<TransaccionLiquidadaDTO>` (`liquidacion`) y `EstadoConciliacionItem` (JUSTIFICADA o EXCLUIDA), porque `EstadoConciliacion` es ABIERTA o CERRADA. `ConciliacionItem.justificar(texto)` y `excluir(texto)`, pedidos en la tarea, no están en el diagrama: son de paquete y los llama `Conciliacion.justificarItem`, que es el método del diagrama.
- **DF-71. DTO de la conciliación.** `VentaResumenDTO`, `ComprobanteSunatDTO` (en `ventas.api`) y `TransaccionLiquidadaDTO` (en `pagos.api`) solo aparecen como tipos en el diagrama; sus campos salen de V_VENTA_VALIDA, V_COMPROBANTES_MES y TRANSACCION_PASARELA (con `idVenta` de VENTA_PAGO). Se publican en las interfaces de los módulos que los producen y `caja` depende de ellas. Los métodos `ventasDelMes`, `comprobantesDelMes` y `liquidacion` de VentasConsultaApi y PagosApi son de la tarea de servicios.
- **DF-72. Nombres de la tarea frente al diagrama.** Donde el pedido y el diagrama difieren se usó el diagrama: `RepositorioConsumoPendiente.findPendientes(idSede)` (no `findPendientesPorSede`), `RepositorioListaEspera.findPendientes(idSede, fecha)` (no `findPendientesDelDia`), `SolicitudAnulacion.aprobar(idUsuarioResuelve, idNotaCredito)` y `rechazar(idUsuarioResuelve, motivoRechazo)`, `ConsumoPendiente.regularizar(idUsuarioResuelve, idMovimiento)` y `descartar(idUsuarioResuelve, observacion)`, y `Caja.reabrir(idUsuarioReapertura, motivoReapertura)`.
- **DF-73. Lo que devuelven los repositorios.** Como en la Iteración 1, lo que el diagrama devuelve como una entidad (`findEnCursoPorCita`, `findDelDia`) es `Optional`. Las consultas que leen una vista devuelven una proyección y no la entidad: `RepositorioProducto.findMovimientos` → `FilaKardex` (V_KARDEX, con el usuario y la cantidad con signo) y `RepositorioAuditoria.findByFiltros` → `FilaAuditoria` (V_AUDITORIA, que no tiene id_usuario: se filtra por su nombre de usuario). Los métodos `default` de los repositorios llaman a una consulta auxiliar con el rango de fechas (`findDelPersonalEntre`, `findAperturasEntre`, `findMovimientosEntre`, `findByFiltrosEntre`).
- **DF-74. Vistas sin método en el diagrama.** La tarea pide leer V_STOCK_ALERTA, V_COMPROBANTES_MES y V_PASARELA_LIQUIDACION, que el diagrama nombra en notas (CUS50 y CUS47) pero no en un repositorio: `RepositorioProducto.findEnStockMinimo(idSede)` (aviso de stock mínimo de CUS50), `RepositorioComprobante.findDelMes(anio, mes)` y `RepositorioTransaccionPasarela.findLiquidacionDelMes(anio, mes)` (CUS47). V_SALDO_FAVOR_CLIENTE y V_SESIONES_PAQUETE se leen en `RepositorioCliente.saldoFavorDisponible` y `RepositorioClientePaquete.findActivosPorCliente`; V_CPE_PENDIENTES ya se leía desde la Iteración 1.
- **DF-75. `RepositorioReporte` es una clase.** El diagrama lo dibuja «interface», pero `reportes` no tiene entidades (nota de CUS48), así que no puede ser un JpaRepository: es un `@Repository` con `NamedParameterJdbcTemplate` y una consulta fija por agrupación (nunca arma SQL con texto del usuario). `FilaReporteDTO` va en `reportes.repository`, como las proyecciones `Fila*` de los demás módulos.
- **DF-76. Constructores sin caja en el diagrama.** `ConsumoPendiente(idAtencion, idProducto, idSede, cantidadEstimada, cantidadDescontada)` (lo crea `InventarioApi.registrarConsumoAtencion` en CUS11), `CategoriaProducto(nombre)` (CUS66), `RecomendacionProducto`, `PaqueteServicio`, `PromocionServicio` y `ConciliacionItem` (de paquete, los crea su raíz) y el `SaldoFavorMov` del abono (de paquete, lo crea `Cliente.abonarSaldo`) no tienen el constructor dibujado.
- **DF-77. Métodos auxiliares de dominio.** No están en el diagrama y son de paquete o privados: `VarianteServicio.cambiarPrecio` y `definirInsumo` que actualiza si el insumo ya está (CUS58 vuelve a definir insumos), `ServicioInsumo.cambiarCantidad`, `InventarioSede.definirMinimo`, `CajaArqueo.tieneDiferencia`, `MovimientoKardex.salida` y `ajuste` y `VentaDetalle.producto`. Públicos: `Atencion.getRecomendaciones`, `Paquete.getServicios`, `Promocion.getServicios` y `Conciliacion.getItems`, como los demás agregados.

CUS modificados de la Iteración 1 (09/11/2026). Cada punto es algo que no está igual en las páginas «modificado It. 2» de `Iteracion_2_Cautiva_Spa.drawio` o que el diagrama no dibuja:

- **DF-78. `ServicioRecurso.listarRecursos` devuelve `List<DatosRecurso>`.** El diagrama de CUS61 dice `List<RecursoDTO>` y el de CUS08 `RecursoApi.listarRecursos(idSede): List<DatosRecurso>`, pero ServicioRecurso implementa RecursoApi y una clase no puede tener dos métodos con la misma firma. Queda uno solo; `ControladorRecurso` lo pasa a `RecursoDTO` con `MapperRecurso`. `DatosRecurso` suma `estado` (texto), porque CUS61 lo muestra. `GET /recursos` exige `idSede`.
- **DF-79. Avisos.** Ver D-23: `AvisoProvider.avisos(idSede)` (no `avisosPara(idSede, permisos)`), y `AvisoDTO(tipo, mensaje, cantidad, permisoRequerido, ruta)` en `comun.api`. `ServicioAviso.listarAvisos(idSede, permisos)` recibe los permisos `MODULO:ACCION` de la sesión (no `List<Permiso>`), junta los proveedores con `ObjectProvider` (sin proveedores, lista vacía) y deja los avisos sin permiso requerido o con un permiso que el rol tiene. `ControladorAviso.listarAvisos` lee los permisos de la autenticación.
- **DF-80. `ParametroApi.obtenerValorNumero`.** Los diagramas (CUS05, CUS32) la llaman `obtenerValorNumero`, pero desde la Iteración 1 se llama `valorNumero`. Se agregó `obtenerValorNumero` (la usa RN51) y `valorNumero` se conserva para los seis usos de la Iteración 1, que devuelven lo mismo. Renombrarlos obligaba a juntar en un solo commit archivos de seguridad, sedes, pagos, agenda y ventas, porque cada uno trae además cambios de esta tarea. ¿Se unifican en una tarea de limpieza?
- **DF-81. Agenda con RN03 v5.1.** `BloqueAgendaDTO` pasa a llamarse `FranjaAgendaDTO`, como en el diagrama de CUS08; los campos de la Iteración 1 no cambian (el JSON de `GET /citas/agenda` es el mismo) y se agregan `finServicio`, `idCliente`, `nombreCliente`, `nombreEspecialista` y `nombreServicio`. `RepositorioCita.findBloques` lee V_AGENDA_BLOQUES con CITA y ATENCION (el diagrama dice «V_AGENDA_BLOQUES, CITA, ATENCION»): `fin` ocupa al recurso con su limpieza y `finServicio` a la especialista sin ella; si la atención de la cita ya terminó, cuenta su `hora_fin`. `validarSinCruce` compara a la especialista sin limpieza y al recurso con ella; las horas libres y las franjas libres de la especialista empiezan en `finServicio`. Ver D-14 por lo que Oracle sigue haciendo.
- **DF-82. `UsuarioActual.idPersonal()` y `esAdministradora(idUsuario)`.** No se guardan en la sesión: se leen de USUARIO y ROL cada vez (con repositorios `@Lazy`, porque el administrador de transacciones depende de UsuarioActual). Así un cambio de rol vale enseguida y `UsuarioSesion` no cambia.
- **DF-83. «Mi agenda».** `listarMiAgenda(fecha)` recibe `LocalDate` y no `LocalDateTime` (el día), opcional: sin fecha, hoy. `CitaAtencionDTO` trae los datos de la cita y, si está EN_ATENCION, el `idAtencion` y el estado de su atención EN_CURSO, para retomarla y finalizarla. `obtenerEnCurso` responde 404 si la cita no tiene una.
- **DF-84. `AtencionApi.listarCobrables(idSede)`.** ATENCION no guarda la sede: se toman los recursos de la sede (`RecursoApi.listarRecursos`) y `RepositorioAtencion.findFinalizadasEnRecursos(idsRecurso)`, que no está en el diagrama. `ServicioVenta.listarPorCobrar` quita las atenciones que ya están en una venta ABIERTA. `PorCobrarDTO(atenciones, ventasAbiertas)`.
- **DF-85. `PersonalApi`.** El diagrama de CUS08 llama `listarEspecialistas(idSede)` y el de CUS52 `listarPersonal()`, los dos con `List<EspecialistaHorario>`. Se agregaron como métodos nuevos (el de tres parámetros de CUS05 sigue); `EspecialistaHorario` suma `cargo` (texto) y, sin un día, trae las horas vacías. Los nombres los pone seguridad o agenda con PersonaApi (DF-23); `PersonaApi.obtenerPersonas(idsPersona)` es nuevo (CUS08 y CUS52).
- **DF-86. Sesión.** `SesionDTO` suma `idPersonal`, `nombres`, `apellidos` y `rol` (el login devuelve lo mismo, así su contrato solo crece). `apellidos` no está en el pedido; se agregó porque PERSONA separa nombres y apellidos. `FiltroCambioClave` deja pasar `GET /auth/sesion` con la clave temporal: la IGU lo necesita para pedir la clave nueva al recargar.
- **DF-87. `GET /ventas/{idVenta}`.** `PagoVentaDTO` suma `estadoTransaccion`, que solo llena este GET con `PagosApi.consultarTransaccion` (nuevo en el diagrama de CUS18). Los cobros PENDIENTES no se pueden listar (D-22).
- **DF-88. `PagosApi.cobrar` con RECHAZADA.** Antes respondía 422 sin guardar nada; ahora guarda la transacción RECHAZADA (`REQUIRES_NEW` y `noRollbackFor`) y después responde 422 (RN52).
- **DF-89. RN48 al asignar rol.** `asignarRol` revisa el rol actual de la usuaria: si es ADMINISTRADORA, el rol nuevo es otro y `existeOtraAdministradoraActiva` es falso, responde 422. Volver a asignarle el mismo rol no se bloquea.
- **DF-90. `GET /comprobantes/{idComprobante}` y `GET /cajas/abierta`.** `ServicioFacturacionElectronica.obtenerComprobante` usa `MapperVenta` (el mismo `ComprobanteDTO` del cierre). `ServicioCaja.obtenerAbierta` devuelve `Optional<CajaDTO>` (el diagrama, `CajaDTO`): vacío es el 204.

## Decisiones tomadas

Dudas de la Iteración 1 que se cierran (08/11/2026):

- **D-04** (tabla `PAQUETE_SERVICIO`) se cierra con CUS59 Registrar Paquete: PAQUETE_SERVICIO entra en las tablas de la Iteración 2.
- **D-06** (elegir un PERSONAL sin usuario en CUS52) se cierra con `GET /usuarios/personal-sin-usuario` (CUS52 modificado It. 2).
- **D-07** (quitarle permisos a ADMINISTRADORA) se cierra con RN48.
- **D-08** (paso de las horas libres) se cierra con RN51 y el PARAMETRO `AGENDA_PASO_MIN` (15).
- **D-10** (citas en el pasado y atenciones de otra especialista) se cierra con RN49 y RN50.
- **D-12** (canal del enlace de CUS20) se cierra con `mailto` (CUS20 modificado It. 2, 08/11).
- **D-13 y DF-51** (cobro digital rechazado o pendiente) se cierran con RN52.
- **Duda 2 del front-end** se cierra con los GET de los CUS modificados.

CUS modificados de la Iteración 1 (09/11/2026, B9):

- **RN51 reemplaza la decisión de D-08.** En la Iteración 1 las horas libres avanzaban de duración de referencia en duración de referencia. Ahora `GestorDisponibilidadAgenda.calcularHorasLibres` avanza cada `ParametroApi.obtenerValorNumero("AGENDA_PASO_MIN")` minutos (15 en el script 17) desde la apertura de la sede; la duración sigue sirviendo para saber si el servicio cabe antes del cierre y en el horario de la especialista.
- **RN49, RN50, RN48 y RN52 en Java.** RN49: `ServicioCita.validarFechaFutura` (422) antes del cruce en CUS05 (B11 la usa en CUS09). RN50: `Cita.verificarEspecialista` al iniciar y `Atencion.verificarEspecialista` al finalizar, con `UsuarioActual.idPersonal()` y `esAdministradora` (403). RN48: `Rol.verificarPermisosMinimos` con los id de SEGURIDAD:VER, CREAR y EDITAR en `PUT /roles/{idRol}/permisos`, y la última administradora activa en `PATCH /usuarios/{idUsuario}/rol` (422).
- **D-13 y DF-51 (NOTAS_IT1), cómo quedaron.** La parte del rollback de D-13 se resolvió con RN52: `PagosApi.cobrar` corre con `@Transactional(propagation = REQUIRES_NEW)`; `TransaccionConSesionOracle` también llama a `PKG_SESION.iniciar` en esa conexión, porque inicia toda transacción nueva. Una APROBADA, RECHAZADA o PENDIENTE queda aunque la venta haga rollback (la RECHAZADA responde 422 después de guardarse). DF-51 (la venta rápida con pago digital no se podía cerrar en una operación y se revertía con 422) se resolvió así: con el cobro PENDIENTE, `POST /ventas/rapidas` guarda la atención y la venta, no llama a `cerrar` y responde 202 con la venta ABIERTA y `cobrosPendientes`; la recepcionista la retoma desde `GET /ventas/por-cobrar`, registra el pago aprobado con `idTransaccion` y cierra con `POST /ventas/{idVenta}/cierre`. Sigue abierta la otra parte de D-13: TRANSACCION_PASARELA sin `id_venta` (D-22).
- **CUS20.** El enlace firmado de `GET /comprobantes/{idComprobante}/enlace` no cambió; el correo lo abre el front-end con `mailto` (D-12). No se agregó envío de correo.
- **Fuera de esta tarea.** La lista de espera de CUS05 (`idEspera`) y CUS06 van en B11; el consumo automático de insumos de CUS11 (RN53) en B10; los proveedores de avisos en B10, B12 y B13.
- **Pruebas de la Iteración 1 que cambian por una regla nueva.** `AgendaFlujoTest.cus05OfreceHorasLibresDeLasEspecialistasDelAreaYQuitaLasOcupadas`: después de la cita de 09:00 a 09:30 la siguiente hora libre era 10:00 (paso de 30 minutos y especialista ocupada durante la limpieza) y ahora es 09:30 en otro sillón (RN51 cada 15 minutos y RN03: la especialista queda libre al terminar). `AgendaFlujoTest.cus08LaAgendaTraeBloquesOcupadosConLimpiezaYFranjasLibres`: la franja libre de la especialista empezaba a las 10:40 y ahora a las 10:30; el bloque ocupado sigue terminando a las 10:40 (limpieza del recurso). `PasarelaYFacturacionFlujoTest.cus18UnCobroRechazadoEs422YNoRegistraElPago` solo agrega que la fila RECHAZADA queda (RN52) y usa un código de operación por venta. `ContratoOpenApiTest` cuenta 50 operaciones (37 + 13 GET). Las demás pruebas de la Iteración 1 no se tocaron.

Decisiones del equipo del 08/11/2026:

- Anulación por solicitud y bandeja: la recepcionista pide anular (CUS22) y la Administradora aprueba o rechaza desde la bandeja (CUS23) con SOLICITUD_ANULACION (RN32).
- Devolución manual de Yape y Plin desde la aplicación de la Administradora, registrando el número de operación; la tarjeta por la pasarela; el efectivo desde caja; o saldo a favor (RN44).
- Reapertura de caja en lugar de `VENTA.id_usuario_autoriza` (RN30, CUS46): la columna queda obsoleta (CK_VENTA_AUTORIZA_OBSOLETA).
- Consumo automático de insumos al finalizar la atención, con CONSUMO_PENDIENTE para lo que no alcanzó (RN53, RN54).
- Limpieza de hasta 10 minutos (RN03, LIMPIEZA_MAX_MIN = 10).
- Reporte de liquidación importado para conciliar (CUS47).
- Panel de avisos (`AvisoProvider`, `GET /avisos`, CUS50).
- Reportes adelantados a la Iteración 2 (módulo `reportes`, CUS48 y CUS49).
- **JDK.** Se trabaja con el JDK 26 y el objetivo sigue siendo Java 21. Comprobado el 08/11/2026: `./mvnw -v` muestra «Java version: 26.0.2.1» (`JAVA_HOME` = `C:\Users\Laptop\.jdks\openjdk-26.0.2.1`; en esta PC no está definido de forma permanente y hubo que fijarlo en la consola), `pom.xml` sigue con `<java.version>21</java.version>` y el POM efectivo tiene `maven.compiler.release` = 21, así que Maven compila con `--release 21`. Ninguna prueba falló por el JDK 26 (ver «Verificación del cierre»).

Migración v5 → v5.1 (`database/17_datos_iteracion2.sql`, 08/11/2026):

- **Paso previo.** Antes de cambiar nada, 17 cuenta las ventas con `id_usuario_autoriza`; si hay alguna (y aún no existe CK_VENTA_AUTORIZA_OBSOLETA), se detiene con `-20100` sin tocarlas. En el contenedor de pruebas no había ninguna. Si pasa en otra base, se anota aquí y se decide antes de volver a correrlo.
- **Códigos de error nuevos** (el mayor usado en 06, 08 y 09 era `-20099`): `-20100` VENTA.id_usuario_autoriza obsoleta; `-20101` la solicitud es de una venta CERRADA (o la venta no existe); `-20102` la solicitud nace PENDIENTE, no cambia de venta y, resuelta, no cambia; `-20103` el consumo pendiente nace PENDIENTE y, resuelto, no cambia; `-20104` solo almacén o la Administradora resuelven un consumo pendiente; `-20105` regularizar exige una SALIDA del mismo insumo, sede y atención; `-20106` solo lo usa 17 al comprobar que VENTAS:EDITAR siga solo en ADMINISTRADORA. Se reutilizan `-20003`, `-20031`, `-20042` y `-20060`. También están en `database/README.md`.
- **Triggers.** `TRG_CAJA_REGLAS`, `TRG_KARDEX_APLICAR` y `TRG_VENTA_REGLAS` parten del código de 09 y solo cambian en lo que dice la hoja (cambios 11 a 13). Los triggers nuevos fijan desde PKG_SESION quién y cuándo (pide, resuelve, registra) y limpian al insertar los datos de la resolución. `TRG_AUD_SOLICITUD_ANULACION` y `TRG_AUD_CONSUMO_PENDIENTE` siguen el molde generado de 10.
- **Consumo automático (RN17, RN53).** `TRG_KARDEX_APLICAR` admite la SALIDA con `id_atencion` cuando el usuario de la sesión es la especialista de esa atención (`USUARIO.id_personal = ATENCION.id_personal`). Otra especialista, la recepcionista o la misma salida sin `id_atencion` siguen siendo `-20031`.
- **RN54.** `12_datos_iniciales.sql` no carga insumos (los 6 productos son de VENTA en UND) ni filas de SERVICIO_INSUMO, así que no hubo nada que corregir. 17 solo avisa si encuentra un insumo con una unidad que no sea ML, G o UND o un consumo estimado con decimales; no convierte datos.
- **Permisos.** 12 no daba a RECEPCIONISTA ni `CATALOGO:VER` ni `VENTAS:ANULAR`: 17 los agrega. `VENTAS:EDITAR` sigue solo en ADMINISTRADORA (17 lo comprueba y se detiene si no). ADMINISTRADORA no se toca.
- **Usuaria `almacen`.** PERSONA (DNI de prueba 70000006) + PERSONAL ENCARGADA_ALMACEN en Pichanaki Centro, con el mismo horario de 15, y USUARIO `almacen` con el hash de la clave temporal de 15 y `debe_cambiar_clave = 'S'`.
- **Objetos dependientes.** Después de los ALTER TABLE, 17 recompila lo inválido (`DBMS_UTILITY.COMPILE_SCHEMA` solo de lo inválido) para terminar con 0 inválidos.
- **Pruebas de la migración.** `database/21_pruebas_v51.sql` (58 casos, termina en ROLLBACK) y `MigracionV51Test`, que lo corre en Testcontainers después de 17 dentro de `./mvnw verify`. `OracleTestcontainer` instala 17 después de 15. `InstalacionBaseDatosTest` espera ahora 57 tablas y 131 FK.
- **16 no cambia.** `16_verificar_cuenta_aplicacion.sql` no cuenta tablas.
- **Pruebas Java ajustadas al v5.1** (solo datos de prueba, sin lógica nueva): `AgendaFlujoTest` (fin con 10 minutos de limpieza: 10:40 y 10:55; cruces a las 10:20 y 10:35; sin cruce a las 10:40), `MapeoEntidadesTest` (recurso de 10 minutos: 10:40 y 10:50) y `SeguridadCatalogoFlujoTest` (recurso de 10 minutos y rechazo con 11; la recepcionista ahora ve `GET /areas` por `CATALOGO:VER` y sigue sin poder `POST /areas`).

Entidades, métodos de negocio y repositorios de la Iteración 2 (09/11/2026):

- **51 entidades.** Las 44 de la Iteración 1 más RecomendacionProducto (agenda), PaqueteServicio y PromocionServicio (catalogo, con `@EmbeddedId` y `@MapsId`), Conciliacion y ConciliacionItem (caja), SolicitudAnulacion (ventas) y ConsumoPendiente (inventario). Quedan fuera las 6 tablas fuera de alcance. Las «solo estructura» que ahora se escriben tienen constructor y métodos: Producto, CategoriaProducto, InventarioSede, MovimientoKardex, Paquete, Promocion y NotaCredito. Compra, CompraDetalle, Proveedor y RequerimientoCompra siguen solo con columnas (sus CUS son de otra iteración).
- **Columnas que reescribe Oracle (`@Generated`).** Se agregaron, revisando los triggers de 09 y 17: Cita.idUsuario y fechaRegistro, Venta.idUsuario y fechaHora, TransaccionPasarela.idUsuario y fechaHora, CajaEgreso.idUsuario y fecha, SaldoFavorMov.fecha, Parametro.idUsuario (al insertar y al actualizar), Caja.idUsuarioApertura (al insertar) y los datos de cierre, revisión y reapertura (al actualizar), MovimientoKardex.idUsuario, fechaHora y saldo, NotaCredito.idUsuarioSolicita, SolicitudAnulacion.idUsuarioSolicita y fechaSolicitud (al insertar) e idUsuarioResuelve y fechaResolucion (al actualizar), ConsumoPendiente.fechaRegistro (al insertar) e idUsuarioResuelve y fechaResolucion (al actualizar). InventarioSede.stockActual no se escribe nunca (`insertable = false, updatable = false`): lo pone en 0 TRG_INVENTARIO_SEDE_REGLAS y lo mueve el kardex. SerieComprobante.ultimoNumero se inserta en 0 y no se actualiza, para no pisar el correlativo de PKG_VENTA.
- **`@DynamicUpdate`.** Se agregó en Cliente (TRG_CLIENTE_HERENCIA es `UPDATE OF id_cliente`), NotaCredito (TRG_NOTA_CREDITO_HERENCIA es `UPDATE OF id_nota_credito`), SerieComprobante, SolicitudAnulacion y ConsumoPendiente, además de las de la Iteración 1.
- **Reglas en el dominio.** Cada método repite antes de Oracle el CHECK o la regla que le toca, con su RN (ADR-05): Cita.cancelar exige motivo (CK_CITA_CANCELACION), Promocion su valor y su vigencia, Paquete sus sesiones, SerieComprobante su letra, MedioPago el efectivo sin pasarela, Producto el stock de la salida (409, RN15) y SolicitudAnulacion el código 01, 02 o 06. RN50 (`verificarEspecialista` de Cita y Atencion) responde 403. `Caja.reabrir` exige la caja del día (D-16) y `Caja.aprobarRevision`, un cierre con diferencia aún no revisado.
- **Recurso.** El constructor rechaza una limpieza fuera de 0 a 10 minutos con RN03. El 10 es el tope del CHECK del esquema (`CK_RECURSO_MINUTOS_LIMPIEZA`), no un valor del negocio: el tope del negocio sigue siendo LIMPIEZA_MAX_MIN de PARAMETRO, que revisa ServicioRecurso.
- **Prueba de mapeo.** `MapeoEntidadesTest` espera 51 entidades y corre contra Oracle cada consulta nueva de los repositorios. `MapeoIteracion2Test` guarda los agregados nuevos con sus métodos de negocio y comprueba lo que completa Oracle: paquete y promoción con sus servicios; ajuste y salida de kardex con su saldo; recomendación; consumo pendiente descartado; venta de producto cerrada; solicitud de anulación rechazada; caja cerrada, aprobada y reabierta; conciliación cerrada; cita reprogramada y cancelada con su abono de saldo; lista de espera asignada; y las reglas de seguridad y configuración.
- **Plan de commits.** Las entidades de `caja` usan los DTO que publican `ventas` y `pagos`, así que esos módulos van antes. Los repositorios van en el mismo commit que las entidades de su módulo, porque cada repositorio usa las entidades nuevas de ese módulo.
- **Verificación (09/11/2026).** `./mvnw -q verify` con el JDK 26: 128 pruebas, 0 fallos, 0 errores, 0 omitidas, con `ModularidadTest` en verde (el módulo nuevo `reportes` no depende de nadie más que de `comun`). Una corrida anterior falló entera al instalar `database/` en el contenedor: `ORA-03114: not connected to ORACLE` en el paso a) de 17, el mismo corte intermitente de la Iteración 1 (`NOTAS_IT1.md`, «Pendiente de vigilar»), otra vez con el contenedor `oracle-local` encendido; la corrida siguiente pasó sin ningún cambio. Ninguna prueba falla por el JDK 26.

Resultado en un contenedor limpio (`gvenzl/oracle-free:23.26.2`, `TZ=America/Lima`, 08/11/2026), en el orden 00 → 14 → 13 → 15 → 17 → 16:

| Script | Resultado |
| --- | --- |
| `00_instalar_v5.sql` | 55 tablas, 125 triggers, 0 errores (v5) |
| `14`, `13`, `15` | Sin errores |
| `17_datos_iteracion2.sql` | 57 tablas; P 57, R 131, U 35, C 183; 105 IX_; TRIGGER 129; 0 inválidos; 0 errores. Corrido dos veces: la segunda no cambió nada y terminó igual. |
| `97_prueba_rapida_v5.sql` | 14 OK, 0 FALLO |
| `20_pruebas_reglas_v5.sql` | 142 OK, 2 FALLO (DF-59: limpieza de 10 minutos; no se editó) |
| `21_pruebas_v51.sql` | 58 OK, 0 FALLO |
| `16_verificar_cuenta_aplicacion.sql` (como CAUTIVA_APP) | 11 OK, 0 FALLO |

Lo esperado eran 14, 144 y 11 OK; la diferencia en 20 son los dos casos de DF-59.

### Verificación del cierre

`./mvnw -q verify` con el JDK 26 (08/11/2026): la primera corrida, ya con 17 instalado por `OracleTestcontainer`, dio 7 pruebas en rojo, todas por los datos del v5.1 y no por el JDK: limpieza de 15 minutos rechazada por `CK_RECURSO_MINUTOS_LIMPIEZA` (`ORA-02290`) o por `LIMPIEZA_MAX_MIN` (422), fines calculados con 30 minutos de limpieza y `GET /areas` que ahora devuelve 200 a la recepcionista. Con los datos de prueba ajustados (ver arriba), la segunda corrida terminó bien: 122 pruebas, 0 fallos, 0 errores, 0 omitidas (incluye `MigracionV51Test` y `ModularidadTest`). Ninguna falla por el JDK 26.

Este repositorio es solo el back-end: `npm run build` y `npm test` del front-end no se corrieron aquí.

### Verificación de los CUS modificados

`./mvnw verify` con el JDK 26 en la rama `tineo-feature/b9-cus-modificados` (09/11/2026): 142 pruebas (128 de antes más 14 nuevas o ampliadas), 140 en verde. Quedan dos en rojo:

- `ContratoOpenApiTest.docsOpenapiEsLaSalidaDelCodigo`: `docs/openapi.yaml` no se volvió a exportar porque `docs/` no se modifica (D-24). La otra prueba del contrato (`cadaOperacionTraeEjemplosQueCoincidenConSusEsquemas`) pasa con las 50 operaciones. Se arregla exportando el archivo, si se autoriza.
- `InstalacionBaseDatosTest.instalaLas57TablasDelModeloV51SinObjetosInvalidos`: `ORA-03113` (el contenedor cerró la conexión), el mismo corte intermitente de B8 y de la Iteración 1. No depende de esta tarea: corrida sola (`./mvnw test -Dtest=InstalacionBaseDatosTest`) pasa 2 de 2, y en la corrida completa anterior también había pasado.

`ModularidadTest` en verde. Ninguna prueba falla por el JDK 26.

## Entidades y repositorios frente al draw.io

Cruce del 09/11/2026 de los diagramas de clases del diseño de las 39 páginas CUS de `docs/Iteracion_2_Cautiva_Spa.drawio` (más la página «General · Clases del diseño (capas)») con el código: los 154 métodos de entidades y repositorios que dibujan existen. «Nuevos» son los que no estaban en la Iteración 1; las diferencias de firma están en DF-64 a DF-77.

| Módulo | Clase | Métodos nuevos en la Iteración 2 (CUS) | Ya estaban |
| --- | --- | --- | --- |
| agenda | RepositorioAtencion | `findByCliente` (03), `findEnCursoPorCita` (11) | `findById`, `save` |
| agenda | RepositorioCita | `findPendientesPorCliente` (03), `findDelDiaPorPersonal` (11) | `findById`, `save`, `findOcupacion` |
| agenda | RepositorioCliente | `saldoFavorDisponible` (03) | `findById`, `save` |
| agenda | RepositorioClientePaquete | `findActivosPorCliente` (03) | `findActivoPorCliente` |
| agenda | RepositorioListaEspera | `findPendientes` (06) | `findById`, `save` |
| agenda | Atencion | `recomendarProducto` (12), `verificarEspecialista` (11) | `Atencion`, `registrarDetalle`, `finalizar`, `usarSesion` |
| agenda | Cita | `reprogramar` (09), `cancelar` (10), `verificarEspecialista` (11) | `Cita`, `iniciarAtencion`, `finalizar` |
| agenda | Cliente | `actualizarDatos` (04), `abonarSaldo` (10) | `registrarConsentimiento` |
| agenda | ClientePaquete | — | `verificarSesionDisponible` |
| agenda | ListaEspera | `asignarCita` (05), `descartar` (06) | `ListaEspera` |
| caja | RepositorioCaja | `findCerradasConDiferencia` (46), `findDelDia` (46) | `findById`, `save`, `findAbiertaBySede`, `existsAbiertaBySede` |
| caja | RepositorioConciliacion (nueva) | `save` (47), `findById` (47) | — |
| caja | Caja | `aprobarRevision` (46), `reabrir` (46) | `Caja` |
| caja | Conciliacion (nueva) | `Conciliacion` (47), `generarItems` (47), `justificarItem` (47), `cerrar` (47) | — |
| catalogo | RepositorioPaquete (nueva) | `findByTipo` (59), `save` (59) | — |
| catalogo | RepositorioPromocion (nueva) | `findVigentes` (60), `save` (60) | — |
| catalogo | RepositorioServicio | `findByAreaAndEstado` (58) | `findById`, `save` |
| catalogo | Paquete | `Paquete` (59), `incluirServicio` (59) | — |
| catalogo | Promocion | `Promocion` (60), `aplicarAServicio` (60) | — |
| catalogo | Servicio | `actualizarDatos` (58), `cambiarPrecio` (58) | `definirInsumo` |
| inventario | RepositorioCategoriaProducto (nueva) | `findAll` (32), `existsByNombre` (66), `save` (66) | — |
| inventario | RepositorioConsumoPendiente (nueva) | `findPendientes` (33), `findById` (33), `save` (33) | — |
| inventario | RepositorioProducto (nueva) | `findByFiltros` (32), `save` (32), `findById` (33, 34), `findMovimientos` (35) | — |
| inventario | CategoriaProducto | `CategoriaProducto` (66) | — |
| inventario | ConsumoPendiente (nueva) | `regularizar` (33), `descartar` (33) | — |
| inventario | Producto | `Producto` (32), `calcularPrecioVenta` (32), `definirStockMinimo` (32), `registrarSalida` (33), `estaEnMinimo` (33, 34), `registrarAjuste` (34) | — |
| pagos | RepositorioMedioPago (nueva) | `findAll` (62), `findById` (62), `save` (62) | — |
| pagos | RepositorioTransaccionPasarela | — | `findById`, `save` |
| pagos | MedioPago | `configurar` (62) | — |
| pagos | TransaccionPasarela | — | `TransaccionPasarela`, `aprobar` |
| reportes | RepositorioReporte (nueva) | `ventasAgrupadas` (48, 49) | — |
| sedes | RepositorioRecurso | `findBySede` (61) | `save` |
| sedes | Recurso | — | `Recurso` |
| seguridad | RepositorioAuditoria | `findByFiltros` (55) | `save` |
| seguridad | RepositorioParametro | — | `findAll`, `findByCodigo`, `save` |
| seguridad | RepositorioPermiso | — | `findAll`, `findOrCreate` |
| seguridad | RepositorioRol | — | `findById`, `save` |
| seguridad | RepositorioTipoDocumento | — | `findAll`, `findById`, `save` |
| seguridad | RepositorioUsuario | `findIdsPersonalConUsuario` (52), `existeOtraAdministradoraActiva` (53) | `findById`, `save`, `findByNombreUsuario`, `findPermisosPorRol`, `findRolesActivos`, `existsByNombreUsuario`, `findAll` |
| seguridad | Auditoria | — | `Auditoria` |
| seguridad | Parametro | `cambiarValor` (65) | — |
| seguridad | Rol | `verificarPermisosMinimos` (53) | `actualizar`, `concederPermiso` |
| seguridad | TipoDocumento | `configurar` (65) | — |
| seguridad | Usuario | `restablecerClave` (54) | `autenticar`, `registrarAcceso`, `Usuario`, `asignarRol` |
| ventas | RepositorioComprobante | — | `findById` |
| ventas | RepositorioNotaCredito (nueva) | `save` (23) | — |
| ventas | RepositorioSerieComprobante (nueva) | `findBySede` (63), `save` (63) | — |
| ventas | RepositorioSolicitudAnulacion (nueva) | `save` (22), `findPendientes` (23), `findById` (23) | — |
| ventas | RepositorioVenta | `findAbiertasPorSede` (18) | `findById`, `save` |
| ventas | NotaCredito | `NotaCredito` (23) | `registrarRespuestaSunat` |
| ventas | SerieComprobante | `SerieComprobante` (63) | — |
| ventas | SolicitudAnulacion (nueva) | `SolicitudAnulacion` (22), `aprobar` (23), `rechazar` (23) | — |
| ventas | Venta | `anular` (23), `agregarProducto` (26) | `Venta`, `registrarPago`, `generarComprobante`, `cerrar`, `agregarServicio`, `aplicarPromocion`, `aplicarAdelanto`, `aplicarSaldoFavor` |

Entidades del diagrama sin métodos dibujados (solo atributos): RecomendacionProducto, PaqueteServicio y PromocionServicio (los crean Atencion.recomendarProducto, Paquete.incluirServicio y Promocion.aplicarAServicio), ConciliacionItem (lo crea Conciliacion.generarItems), InventarioSede y MovimientoKardex (los crea Producto), CajaArqueo, VentaDetalle, VentaPago, Comprobante, SaldoFavorMov, ServicioInsumo, VarianteServicio, RolPermiso, PersonalArea, Permiso, Persona, Personal y Sede. El constructor de la DEVOLUCION de TransaccionPasarela (CUS24) es una sobrecarga nueva de 8 parámetros: la tabla lo cuenta como existente porque ya había un constructor con ese nombre. Métodos y consultas que no están en el diagrama: DF-69, DF-73, DF-74 y DF-77.

