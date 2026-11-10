# Trazabilidad de la Iteración 2

CUS → endpoint → controlador → servicio → repositorio → entidad, comparada con `docs/Iteracion_2_Cautiva_Spa.drawio`, para los 27 CUS nuevos y los 12 CUS de la Iteración 1 con página «modificado It. 2».

**Cómo se comparó.** Un script leyó las 39 páginas CUS del draw.io. De cada página tomó los mensajes HTTP de la secuencia y las clases «control», «interface» y «entity» del diagrama de diseño, con sus métodos. Luego buscó cada clase y cada método en `src/main/java`. La columna «Controlador» sale de las anotaciones `@GetMapping`, `@PostMapping` y similares del código, y la columna «Servicio» sale de la llamada que hace cada método del controlador.

**Resultado:**

- Las 39 páginas tienen 88 endpoints distintos, y los 88 existen en el código.
- Del draw.io faltan en el código solo 2 métodos, y los dos tienen explicación:
  - `NotaCredito.registrarRespuestaSunat` se hereda de `Comprobante` (JOINED, ADR-06).
  - `AvisoProvider.avisosPara(idSede, permisos)` es `avisos(idSede)`, y el filtro por permisos lo aplica `ServicioAviso` (D-23, DF-79).
  - `save`, `findById` y similares vienen de `JpaRepository`.
- La comparación es por nombre. Las firmas que cambian se listan en la columna «Frente al draw.io», con su nota en [NOTAS_IT2.md](NOTAS_IT2.md).
- El código tiene 15 endpoints que el draw.io de la Iteración 2 no dibuja (ver el final). Son 103 en total (37 de la Iteración 1 y 66 nuevos), y son los que trae [openapi.yaml](openapi.yaml), cada uno con su `x-permiso`.

Las clases son del módulo del controlador, salvo cuando se indica el módulo. `API` es una interfaz publicada por otro módulo; quien la implementa está en «Interfaces publicadas». Los endpoints van sin el prefijo `/api/v1`.

## CUS nuevos de la Iteración 2

| CUS | Endpoint | Controlador | Servicio | Repositorio | Entidad | Frente al draw.io |
| --- | --- | --- | --- | --- | --- | --- |
| CUS03 | `GET /clientes/{idCliente}` | `ControladorCliente.obtenerFicha` | `ServicioCliente.obtenerFicha`, `PersonaApi.obtenerPersona`, `HistorialComprasProvider.productosComprados` (ventas) | `RepositorioCliente`, `RepositorioAtencion`, `RepositorioCita`, `RepositorioClientePaquete` | `Cliente`, `Atencion`, `Cita`, `ClientePaquete`, `V_SALDO_FAVOR_CLIENTE`, `V_SESIONES_PAQUETE`, `V_VENTA_LINEA` | `FichaClienteDTO` y `ProductoCompradoDTO` solo están nombrados en el diagrama (DF-101, DF-102) |
| CUS04 | `GET /clientes/consulta-documento` | `ControladorCliente.consultarDocumento` | `ServicioCliente.consultarDocumento`, `PersonaApi.consultarDocumento` | `RepositorioTipoDocumento` | `TipoDocumento` | Coincide (el mismo de CUS02) |
| CUS04 | `PUT /clientes/{idCliente}` | `ControladorCliente.actualizar` | `ServicioCliente.actualizar`, `PersonaApi.actualizarPersona` | `RepositorioCliente`, `RepositorioPersona` | `Cliente`, `Persona` | `actualizarPersona` devuelve `PersonaDTO` (DF-103). El consentimiento se registra siempre (DF-104) |
| CUS09 | `POST /citas/{idCita}/reprogramacion` | `ControladorCita.reprogramar` | `ServicioCita.reprogramar`, `GestorDisponibilidadAgenda.validarSinCruce` | `RepositorioCita` | `Cita` | Dos `save` (la cita vieja y la nueva), porque no hay cascada entre citas (DF-107). RN49 |
| CUS10 | `PATCH /citas/{idCita}/cancelacion` | `ControladorCita.cancelar` | `ServicioCita.cancelar` | `RepositorioCita`, `RepositorioCliente` | `Cita`, `Cliente`, `SaldoFavorMov` | Cuerpo en `CancelacionDTO`. Abona saldo solo con adelanto (DF-108, RN05) |
| CUS12 | `GET /atenciones/productos` | `ControladorAtencion.buscarProductos` | `ServicioAtencion.buscarProductos`, `InventarioApi.buscar` | `RepositorioProducto` (inventario) | `Producto`, `InventarioSede` | Coincide |
| CUS12 | `POST /atenciones/{idAtencion}/recomendaciones` | `ControladorAtencion.recomendarProducto` | `ServicioAtencion.recomendarProducto` | `RepositorioAtencion` | `Atencion`, `RecomendacionProducto` | Cuerpo y respuesta en `RecomendacionDTO` (DF-109) |
| CUS22 | `POST /ventas/{idVenta}/solicitudes-anulacion` | `ControladorAnulacion.solicitarAnulacion` | `ServicioAnulacion.solicitarAnulacion`, `validarAnulable` | `RepositorioVenta`, `RepositorioSolicitudAnulacion` | `Venta`, `SolicitudAnulacion` | Cuerpo y respuesta en `SolicitudAnulacionDTO` (DF-114). Oracle fija quién la pide |
| CUS23 | `GET /solicitudes-anulacion` | `ControladorAnulacion.listarPendientes` | `ServicioAnulacion.listarPendientes` | `RepositorioSolicitudAnulacion` | `SolicitudAnulacion` | Agrega el filtro `?estado` (DF-115) |
| CUS23 | `POST /solicitudes-anulacion/{idSolicitud}/aprobacion` | `ControladorAnulacion.aprobar` | `ServicioAnulacion.aprobar`, `ServicioFacturacionElectronica.emitirNotaCredito` | `RepositorioSolicitudAnulacion`, `RepositorioVenta`, `RepositorioNotaCredito` | `SolicitudAnulacion`, `Venta`, `NotaCredito`, `Comprobante` | `NotaCredito(oVenta, …)` en vez de `idVenta` (DF-64, DF-116). `Venta.anular` no guarda quién autoriza (DF-65) |
| CUS23 | `POST /solicitudes-anulacion/{idSolicitud}/rechazo` | `ControladorAnulacion.rechazar` | `ServicioAnulacion.rechazar` | `RepositorioSolicitudAnulacion` | `SolicitudAnulacion` | Coincide |
| CUS24 | `POST /ventas/{idVenta}/devolucion` | `ControladorAnulacion.devolverPago` | `ServicioAnulacion.devolverPago`, `PagosApi.devolver` | `RepositorioVenta`, `RepositorioTransaccionPasarela` (pagos) | `Venta`, `TransaccionPasarela` | Va por `PagosApi` y no por `ServicioPago` (DF-117) |
| CUS24 | `POST /ventas/{idVenta}/devolucion-manual` | `ControladorAnulacion.registrarDevolucionManual` | `ServicioAnulacion.registrarDevolucionManual`, `PagosApi.registrarDevolucionManual` | `RepositorioVenta`, `RepositorioTransaccionPasarela` (pagos) | `Venta`, `TransaccionPasarela` | Yape y Plin con número de operación (RN44, DF-117) |
| CUS25 | `GET /ventas/productos` | `ControladorVenta.buscarProductos` | `ServicioVenta.buscarProductos`, `InventarioApi.buscar` | `RepositorioProducto` (inventario) | `Producto`, `InventarioSede` | Coincide |
| CUS26 | `POST /ventas/productos` | `ControladorVenta.venderProductos` | `ServicioVenta.venderProductos`, `InventarioApi.verificarStock`, `CajaAbiertaProvider.obtenerCajaAbierta`, `PagosApi.cobrar` | `RepositorioVenta` | `Venta`, `VentaDetalle`, `VentaPago` | Cuerpo en `VentaProductosDTO` (DF-113). `verificarStock` responde 409 si no alcanza (DF-96). 202 con cobro pendiente (RN52) |
| CUS32 | `GET /productos` | `ControladorProducto.listarProductos` | `ServicioProducto.listarProductos` | `RepositorioProducto` | `Producto`, `InventarioSede` | Coincide |
| CUS32 | `GET /categorias-producto` | `ControladorProducto.listarCategorias` | `ServicioProducto.listarCategorias` | `RepositorioCategoriaProducto` | `CategoriaProducto` | Coincide |
| CUS32 | `POST /productos` | `ControladorProducto.registrar` | `ServicioProducto.registrar`, `ParametroApi.obtenerValorNumero` | `RepositorioProducto` | `Producto`, `InventarioSede` | Margen como `BigDecimal` (DF-68). Premium y margen ajustado: DF-91. RN54 en el constructor (DF-97) |
| CUS33 | `GET /consumos-pendientes` | `ControladorProducto.listarConsumosPendientes` | `ServicioProducto.listarConsumosPendientes` | `RepositorioConsumoPendiente.findPendientes` | `ConsumoPendiente` | Nombre del diagrama, no el de la tarea (DF-72) |
| CUS33 | `POST /productos/{idProducto}/salidas` | `ControladorProducto.registrarSalida` | `ServicioProducto.registrarSalida` | `RepositorioProducto` | `Producto`, `MovimientoKardex` | Agrega `Producto.ultimoMovimiento` (DF-69) |
| CUS33 | `POST /consumos-pendientes/{idConsumo}/regularizacion` | `ControladorProducto.regularizar` | `ServicioProducto.regularizar` | `RepositorioConsumoPendiente`, `RepositorioProducto` | `ConsumoPendiente`, `MovimientoKardex` | Oracle exige la SALIDA (-20105, RN53) |
| CUS33 | `PATCH /consumos-pendientes/{idConsumo}/descarte` | `ControladorProducto.descartar` | `ServicioProducto.descartar` | `RepositorioConsumoPendiente` | `ConsumoPendiente` | Cuerpo en `DescarteDTO` (DF-92) |
| CUS34 | `POST /productos/{idProducto}/ajustes` | `ControladorProducto.registrarAjuste` | `ServicioProducto.registrarAjuste` | `RepositorioProducto` | `Producto`, `MovimientoKardex` | `registrarAjuste` devuelve `boolean` (DF-69) |
| CUS35 | `GET /productos/{idProducto}/kardex` | `ControladorProducto.consultarKardex` | `ServicioProducto.consultarKardex` | `RepositorioProducto.findMovimientos` | `V_KARDEX` | Devuelve la proyección `FilaKardex` (DF-73). Sede y fechas por defecto (DF-100) |
| CUS46 | `GET /cajas/con-diferencia` | `ControladorCaja.listarConDiferencia` | `ServicioCaja.listarConDiferencia` | `RepositorioCaja.findCerradasConDiferencia` | `Caja`, `CajaArqueo` | Permisos CONCILIACION (DF-122) |
| CUS46 | `PATCH /cajas/{idCaja}/revision` | `ControladorCaja.aprobarCierre` | `ServicioCaja.aprobarCierre` | `RepositorioCaja` | `Caja` | Coincide |
| CUS46 | `GET /cajas/del-dia` | `ControladorCaja.obtenerDelDia` | `ServicioCaja.obtenerDelDia` | `RepositorioCaja.findAperturasEntre` | `Caja` | Devuelve una lista, no una caja (DF-123) |
| CUS46 | `PATCH /cajas/{idCaja}/reapertura` | `ControladorCaja.reabrir` | `ServicioCaja.reabrir` | `RepositorioCaja.findAbiertaBySede` | `Caja` | Cuerpo en `ReaperturaDTO`. Revisa la otra caja ABIERTA antes de cambiar esta (DF-123, RN30) |
| CUS47 | `POST /conciliaciones/liquidacion` | `ControladorConciliacion.importarLiquidacion` | `ServicioConciliacion.importarLiquidacion`, `LectorReporteLiquidacion.leerReporte`, `PagosApi.registrarLiquidacion` | `RepositorioTransaccionPasarela` (pagos) | `TransaccionPasarela` | Multipart con la parte `archivoLiquidacion` (DF-124, DF-125). Formato de Izipay sin verificar (D-34) |
| CUS47 | `POST /conciliaciones` | `ControladorConciliacion.conciliar` | `ServicioConciliacion.conciliar`, `VentasConsultaApi.ventasDelMes`, `comprobantesDelMes`, `ventasSinComprobante`, `PagosApi.liquidacion` | `RepositorioConciliacion.existsByAnioAndMes`, `RepositorioVenta`, `RepositorioComprobante`, `RepositorioTransaccionPasarela` | `Conciliacion`, `ConciliacionItem`, `V_VENTA_VALIDA`, `V_COMPROBANTES_MES`, `V_VENTAS_SIN_COMPROBANTE` | Tipos de `generarItems` según la secuencia (DF-70, DF-71, DF-126). Toda la empresa (D-35) |
| CUS47 | `PATCH /conciliaciones/{idConciliacion}/items/{idItem}` | `ControladorConciliacion.justificarItem` | `ServicioConciliacion.justificarItem` | `RepositorioConciliacion` | `Conciliacion`, `ConciliacionItem` | Cuerpo en `JustificacionDTO` (DF-126) |
| CUS47 | `PATCH /conciliaciones/{idConciliacion}/cierre` | `ControladorConciliacion.cerrar` | `ServicioConciliacion.cerrar` | `RepositorioConciliacion` | `Conciliacion` | Coincide |
| CUS48 | `GET /reportes/{tipoReporte}` | `ControladorReporte.generar` | `ServicioReporte.generar`, `TipoReporte.deRuta` | `RepositorioReporte` (`ventasAgrupadas`, `productos`, `margenServicios`, `demanda`, `ocupacion`, `ingresosEspecialista`) | `V_VENTA_VALIDA`, `V_VENTA_LINEA`, `V_PAGOS_POR_MEDIO`, `V_MARGEN_PRODUCTO`, `V_COSTO_SERVICIO`, `V_DEMANDA_SERVICIOS`, `V_OCUPACION_RECURSO` | `RepositorioReporte` es una clase (DF-75). Un método por tipo (DF-133). RN24: 403 (DF-135) |
| CUS49 | `GET /reportes/{tipoReporte}/exportacion` | `ControladorReporte.exportar` | `ServicioReporte.exportar`, `GeneradorExportacion.generar` | `RepositorioReporte` | Las mismas vistas de CUS48 | Excel (Apache POI) o PDF (OpenPDF), descarga con `Content-Disposition` (DF-136) |
| CUS54 | `PATCH /usuarios/{idUsuario}/clave` | `ControladorUsuario.restablecerClave` | `ServicioUsuario.restablecerClave`, `CodificadorClave` | `RepositorioUsuario` | `Usuario` | Cuerpo en `ClaveDTO` (DF-128). Cuenta INACTIVA: D-36 |
| CUS55 | `GET /auditoria` | `ControladorAuditoria.consultar` | `ServicioAuditoria.consultar` | `RepositorioAuditoria.findByFiltros` | `V_AUDITORIA` | Últimos 30 días por defecto (DF-128) |
| CUS58 | `GET /servicios` | `ControladorCatalogo.listarServicios` | `ServicioCatalogo.listarServicios` | `RepositorioServicio` | `Servicio`, `VarianteServicio` | Coincide |
| CUS58 | `PUT /servicios/{idServicio}` | `ControladorCatalogo.actualizarServicio` | `ServicioCatalogo.actualizarServicio` | `RepositorioServicio` | `Servicio`, `VarianteServicio`, `ServicioInsumo` | Usa el `ServicioDTO` de CUS57 (DF-110). No quita insumos (D-30) |
| CUS58 | `PATCH /servicios/{idServicio}/variantes/{idVariante}/precio` | `ControladorCatalogo.cambiarPrecio` | `ServicioCatalogo.cambiarPrecio` | `RepositorioServicio` | `VarianteServicio` | Cuerpo en `PrecioDTO` (DF-110) |
| CUS59 | `GET /paquetes` | `ControladorCatalogo.listarPaquetes` | `ServicioCatalogo.listarPaquetes` | `RepositorioPaquete` | `Paquete`, `PaqueteServicio` | Coincide |
| CUS59 | `POST /paquetes` | `ControladorCatalogo.registrarPaquete` | `ServicioCatalogo.registrarPaquete` | `RepositorioPaquete` | `Paquete`, `PaqueteServicio` | `idsServicio` en el DTO (DF-111). Cierra D-04 |
| CUS60 | `GET /promociones` | `ControladorCatalogo.listarPromociones` | `ServicioCatalogo.listarPromociones` | `RepositorioPromocion` | `Promocion`, `PromocionServicio` | Coincide |
| CUS60 | `POST /promociones` | `ControladorCatalogo.registrarPromocion` | `ServicioCatalogo.registrarPromocion` | `RepositorioPromocion` | `Promocion`, `PromocionServicio` | `idsServicio` en el DTO (DF-111) |
| CUS62 | `GET /medios-pago` | `ControladorMedioPago.listarMediosPago` | `ServicioPago.listarMediosPago` | `RepositorioMedioPago` | `MedioPago` | Coincide |
| CUS62 | `PATCH /medios-pago/{idMedioPago}` | `ControladorMedioPago.configurarMedioPago` | `ServicioPago.configurarMedioPago` | `RepositorioMedioPago` | `MedioPago` | Lo que no llega se conserva (DF-130) |
| CUS63 | `GET /series-comprobante` | `ControladorSerieComprobante.listarSeries` | `ServicioSerieComprobante.listarSeries` | `RepositorioSerieComprobante` | `SerieComprobante` | Coincide |
| CUS63 | `POST /series-comprobante` | `ControladorSerieComprobante.registrarSerie` | `ServicioSerieComprobante.registrarSerie` | `RepositorioSerieComprobante` | `SerieComprobante` | Sede de la sesión si no llega. Las UNIQUE las responde Oracle con 409 (DF-130) |
| CUS65 | `GET /parametros` | `ControladorParametro.listarParametros` | `ServicioParametro.listarParametros` | `RepositorioParametro` | `Parametro` | Coincide |
| CUS65 | `PATCH /parametros/{codigo}` | `ControladorParametro.cambiarValor` | `ServicioParametro.cambiarValor` | `RepositorioParametro` | `Parametro` | Valida además AGENDA_PASO_MIN y LIMPIEZA_MAX_MIN (DF-129) |
| CUS65 | `GET /tipos-documento` | `ControladorParametro.listarTiposDocumento` | `ServicioParametro.listarTiposDocumento` | `RepositorioTipoDocumento` | `TipoDocumento` | Solo exige sesión (`x-permiso: SESION`, DF-129) |
| CUS65 | `PATCH /tipos-documento/{idTipoDocumento}` | `ControladorParametro.configurarTipoDocumento` | `ServicioParametro.configurarTipoDocumento` | `RepositorioTipoDocumento` | `TipoDocumento` | `consultaApi` solo en DNI y RUC (RN45, DF-129) |
| CUS66 | `POST /categorias-producto` | `ControladorProducto.registrarCategoria` | `ServicioProducto.registrarCategoria` | `RepositorioCategoriaProducto` | `CategoriaProducto` | `CategoriaDTO` (DF-93) |

## CUS de la Iteración 1 modificados en la Iteración 2

Se listan todos los endpoints de cada página «modificado It. 2», también los que no cambiaron, porque la página los vuelve a dibujar.

| CUS | Endpoint | Controlador | Servicio | Repositorio | Entidad | Frente al draw.io |
| --- | --- | --- | --- | --- | --- | --- |
| CUS05 | `GET /citas/disponibilidad` | `ControladorCita.consultarDisponibilidad` | `ServicioCita.consultarDisponibilidad`, `GestorDisponibilidadAgenda.calcularHorasLibres`, `ParametroApi.obtenerValorNumero` | `RepositorioCita.findBloques` | `V_AGENDA_BLOQUES` | Paso AGENDA_PASO_MIN (RN51). `obtenerValorNumero` convive con `valorNumero` (DF-80) |
| CUS05 | `POST /citas` | `ControladorCita.agendar` | `ServicioCita.agendar`, `GestorDisponibilidadAgenda.validarSinCruce` | `RepositorioCita`, `RepositorioListaEspera`, `RepositorioCliente` | `Cita`, `ListaEspera` | `idEspera` opcional asigna la espera (DF-105). RN49 |
| CUS06 | `POST /citas/lista-espera` | `ControladorCita.registrarEspera` | `ServicioCita.registrarEspera` | `RepositorioListaEspera`, `RepositorioCita` | `ListaEspera` | Coincide |
| CUS06 | `GET /citas/lista-espera` | `ControladorCita.listarEspera` | `ServicioCita.listarEspera` | `RepositorioListaEspera.findPendientes` | `ListaEspera` | `ListaEsperaDTO` con nombres (DF-106, DF-72) |
| CUS06 | `PATCH /citas/lista-espera/{idEspera}/descarte` | `ControladorCita.descartarEspera` | `ServicioCita.descartarEspera` | `RepositorioListaEspera` | `ListaEspera` | Cuerpo en `DescarteDTO` (DF-106) |
| CUS08 | `GET /citas/filtros` | `ControladorCita.listarFiltros` | `ServicioCita.listarFiltros`, `PersonalApi.listarEspecialistas`, `RecursoApi.listarRecursos` | `RepositorioPersonal` (personal), `RepositorioRecurso` (sedes) | `Personal`, `Recurso` | `listarEspecialistas` es nuevo en `PersonalApi` (DF-85). `listarRecursos` devuelve `DatosRecurso` (DF-78) |
| CUS08 | `GET /citas/agenda` | `ControladorCita.consultarAgenda` | `ServicioCita.consultarAgenda`, `GestorDisponibilidadAgenda.armarAgenda` | `RepositorioCita.findBloques` | `V_AGENDA_BLOQUES`, `Cita`, `Atencion` | `FranjaAgendaDTO` en vez de `BloqueAgendaDTO`, con la limpieza de RN03 (DF-81) |
| CUS11 | `GET /atenciones/mi-agenda` | `ControladorAtencion.listarMiAgenda` | `ServicioAtencion.listarMiAgenda`, `UsuarioActual.idPersonal` | `RepositorioCita`, `RepositorioAtencion` | `Cita`, `Atencion` | Recibe `LocalDate` (DF-83). `idPersonal` se lee cada vez (DF-82) |
| CUS11 | `POST /atenciones` | `ControladorAtencion.iniciar` | `ServicioAtencion.iniciar`, `GestorDisponibilidadAgenda.validarSinCruce` | `RepositorioCita`, `RepositorioAtencion` | `Cita`, `Atencion` | RN50 (solo su especialista o la Administradora) |
| CUS11 | `GET /atenciones/en-curso` | `ControladorAtencion.obtenerEnCurso` | `ServicioAtencion.obtenerEnCurso` | `RepositorioAtencion.findEnCursoPorCita` | `Atencion` | Devuelve `Optional`; vacío es 404 (DF-73, DF-83) |
| CUS11 | `PATCH /atenciones/{idAtencion}/fin` | `ControladorAtencion.finalizar` | `ServicioAtencion.finalizar`, `CatalogoApi.insumosDeVariante`, `InventarioApi.registrarConsumoAtencion` | `RepositorioAtencion`, `RepositorioClientePaquete`, `RepositorioProducto` (inventario), `RepositorioConsumoPendiente` (inventario) | `Atencion`, `ClientePaquete`, `MovimientoKardex`, `ConsumoPendiente` | Consumo estimado en la misma transacción (RN53, DF-99). RN50 |
| CUS13 | `GET /ventas/rapidas/libres` | `ControladorVentaRapida.libresAhora` | `ServicioVentaRapida.libresAhora`, `AtencionApi.libresAhora` | `RepositorioCita.findBloques` | `V_AGENDA_BLOQUES` | Coincide |
| CUS13 | `POST /ventas/rapidas` | `ControladorVentaRapida.registrarYCobrar` | `ServicioVentaRapida.registrarYCobrar`, `AtencionApi.registrarSinCita`, `finalizarSinCita`, `ServicioVenta.crearDesdeAtencion`, `registrarPagos`, `cerrar` | `RepositorioAtencion`, `RepositorioVenta` | `Atencion`, `Venta` | Con el cobro digital pendiente responde 202 y la venta queda ABIERTA (RN52; D-13 y DF-51 cerradas) |
| CUS18 | `GET /ventas/por-cobrar` | `ControladorVenta.listarPorCobrar` | `ServicioVenta.listarPorCobrar`, `AtencionApi.listarCobrables` | `RepositorioAtencion.findFinalizadasEnRecursos`, `RepositorioVenta` | `Atencion`, `Venta` | ATENCION no guarda la sede: se toman los recursos de la sede (DF-84) |
| CUS18 | `POST /ventas` | `ControladorVenta.crearDesdeAtencion` | `ServicioVenta.crearDesdeAtencion`, `CajaAbiertaProvider.obtenerCajaAbierta`, `AtencionApi.obtenerAtencionCobrable` | `RepositorioVenta` | `Venta`, `VentaDetalle` | Coincide |
| CUS18 | `PATCH /ventas/{idVenta}/descuentos` | `ControladorVenta.aplicarDescuentos` | `ServicioVenta.aplicarDescuentos`, `SaldoFavorApi.usarSaldo` | `RepositorioVenta`, `RepositorioCliente` | `Venta`, `SaldoFavorMov` | Coincide |
| CUS18 | `POST /ventas/{idVenta}/pagos` | `ControladorVenta.registrarPagos` | `ServicioVenta.registrarPagos`, `PagosApi.cobrar` | `RepositorioVenta`, `RepositorioTransaccionPasarela` (pagos) | `Venta`, `VentaPago`, `TransaccionPasarela` | `cobrar` con `REQUIRES_NEW`; la RECHAZADA se guarda (DF-88). 202 pendiente (RN52) |
| CUS18 | `GET /ventas/{idVenta}` | `ControladorVenta.obtenerVenta` | `ServicioVenta.obtenerVenta`, `PagosApi.consultarTransaccion` | `RepositorioVenta` | `Venta`, `VentaPago` | `PagoVentaDTO` suma `estadoTransaccion` (DF-87) |
| CUS18 | `POST /ventas/{idVenta}/cierre` | `ControladorVenta.cerrar` | `ServicioVenta.cerrar`, `ServicioFacturacionElectronica.emitirComprobante` | `RepositorioVenta`, `RepositorioComprobante` | `Venta`, `Comprobante` | Exige todos los cobros digitales APROBADOS (RN52) |
| CUS20 | `GET /comprobantes/{idComprobante}` | `ControladorComprobante.obtenerComprobante` | `ServicioFacturacionElectronica.obtenerComprobante` | `RepositorioComprobante` | `Comprobante` | Usa `MapperVenta` (DF-90) |
| CUS20 | `GET /comprobantes/{idComprobante}/representacion` | `ControladorComprobante.obtenerRepresentacion` | `ServicioFacturacionElectronica.generarRepresentacionImpresa` | `RepositorioComprobante` | `Comprobante` | Igual que en la Iteración 1 |
| CUS20 | `GET /comprobantes/{idComprobante}/enlace` | `ControladorComprobante.obtenerEnlace` | `ServicioFacturacionElectronica.generarEnlacePublico` | `RepositorioComprobante` | `Comprobante` | Canal `mailto` (D-12 cerrada) |
| CUS44 | `GET /cajas/abierta` | `ControladorCaja.obtenerAbierta` | `ServicioCaja.obtenerAbierta` | `RepositorioCaja.findAbiertaBySede` | `Caja` | Devuelve `Optional`; vacío es 204 (DF-90) |
| CUS44 | `POST /cajas` | `ControladorCaja.abrir` | `ServicioCaja.abrir` | `RepositorioCaja` | `Caja` | Coincide |
| CUS50 | `GET /auth/sesion` | `ControladorAutenticacion.obtenerSesion` | `ServicioAutenticacion.obtenerSesion` | `RepositorioUsuario` | `Usuario`, `Rol` | `SesionDTO` suma datos de la persona (DF-86) |
| CUS50 | `POST /auth/login` | `ControladorAutenticacion.iniciarSesion` | `ServicioAutenticacion.iniciarSesion` | `RepositorioUsuario`, `RepositorioAuditoria` | `Usuario`, `Auditoria` | Igual que en la Iteración 1 |
| CUS50 | `GET /avisos` | `ControladorAviso.listarAvisos` | `ServicioAviso.listarAvisos`, `AvisoProvider.avisos` (ventas, inventario y caja) | `RepositorioSolicitudAnulacion`, `RepositorioComprobante`, `RepositorioProducto`, `RepositorioCaja` | `SolicitudAnulacion`, `V_CPE_PENDIENTES`, `V_STOCK_ALERTA`, `Caja` | `avisos(idSede)` en vez de `avisosPara` (D-23, DF-79, DF-95, DF-118, DF-131) |
| CUS52 | `GET /usuarios/personal-sin-usuario` | `ControladorUsuario.listarPersonalSinUsuario` | `ServicioUsuario.listarPersonalSinUsuario`, `PersonalApi.listarPersonal` | `RepositorioUsuario`, `RepositorioPersonal` (personal) | `Usuario`, `Personal` | `listarPersonal` es nuevo en `PersonalApi` (DF-85). Cierra D-06 |
| CUS52 | `GET /roles` | `ControladorUsuario.listarRoles` | `ServicioUsuario.listarRoles` | `RepositorioRol` | `Rol` | Coincide |
| CUS52 | `POST /usuarios` | `ControladorUsuario.registrar` | `ServicioUsuario.registrar`, `CodificadorClave` | `RepositorioUsuario` | `Usuario` | Coincide |
| CUS53 | `GET /usuarios` | `ControladorUsuario.listarUsuarios` | `ServicioUsuario.listarUsuarios` | `RepositorioUsuario` | `Usuario`, `Rol` | Coincide |
| CUS53 | `GET /permisos` | `ControladorUsuario.listarPermisos` | `ServicioUsuario.listarPermisos` | `RepositorioPermiso` | `Permiso` | Coincide |
| CUS53 | `PATCH /usuarios/{idUsuario}/rol` | `ControladorUsuario.asignarRol` | `ServicioUsuario.asignarRol` | `RepositorioUsuario`, `RepositorioRol` | `Usuario`, `Rol` | RN48 al cambiar el rol de la última Administradora (DF-89) |
| CUS53 | `PUT /roles/{idRol}/permisos` | `ControladorUsuario.configurarPermisos` | `ServicioUsuario.configurarPermisos` | `RepositorioRol`, `RepositorioPermiso` | `Rol`, `Permiso`, `RolPermiso` | `verificarPermisosMinimos` recibe los ids de SEGURIDAD (DF-67, RN48) |
| CUS61 | `GET /recursos` | `ControladorRecurso.listarRecursos` | `ServicioRecurso.listarRecursos` | `RepositorioRecurso` | `Recurso` | Un solo método para el controlador y `RecursoApi` (DF-78) |
| CUS61 | `POST /recursos` | `ControladorRecurso.registrarRecurso` | `ServicioRecurso.registrarRecurso` | `RepositorioRecurso` | `Recurso` | Coincide |

## Endpoints que no están en el draw.io de la Iteración 2

| Endpoint | Controlador | Por qué existe |
| --- | --- | --- |
| `GET /clientes`, `POST /clientes` | `ControladorCliente.buscar`, `registrar` | CUS01 y CUS02, sin página modificada: manda `Iteracion_1` |
| `POST /citas/{idCita}/adelanto`, `POST /citas/{idCita}/enlace-adelanto`, `POST /pagos/notificaciones` | `ControladorCita.registrarAdelantoEfectivo`, `generarEnlaceAdelanto`, `ControladorNotificacionPasarela.recibir` | CUS07 (manda `Iteracion_1`) |
| `GET /ventas/consulta-ruc/{ruc}` | `ControladorVenta.consultarRuc` | CUS19 (manda `Iteracion_1`) |
| `POST /proformas` | `ControladorVenta.armarProforma` | CUS21 (manda `Iteracion_1`) |
| `GET /cajas/{idCaja}/esperado`, `POST /cajas/{idCaja}/cierre` | `ControladorCaja.calcularEsperado`, `cerrar` | CUS45 (manda `Iteracion_1`) |
| `POST /auth/logout` | `ControladorAutenticacion.cerrarSesion` | CUS51 (manda `Iteracion_1`) |
| `GET /areas`, `POST /areas`, `POST /servicios` | `ControladorCatalogo.listarAreas`, `registrarArea`, `registrarServicio` | CUS57 (manda `Iteracion_1`) |
| `POST /auth/cambio-clave` | `ControladorAutenticacion.cambiarClave` | RF82, clave temporal (DF-18 de NOTAS_IT1) |
| `GET /comprobantes/publico/{idComprobante}` | `ControladorComprobante.obtenerPublico` | El enlace firmado abre sin sesión (DF-53 de NOTAS_IT1) |

Están en [trazabilidad_it1.md](trazabilidad_it1.md). Con los 88 de arriba son 103.

## Interfaces publicadas que usa la Iteración 2

| Interfaz | Módulo | La implementa | Frente al draw.io |
| --- | --- | --- | --- |
| `PersonaApi` | seguridad | `ServicioPersona` | `actualizarPersona` devuelve `PersonaDTO` (DF-103) |
| `AtencionApi` | agenda | `ServicioAtencion` | Agrega `listarCobrables` (DF-84) |
| `SaldoFavorApi` | agenda | `ServicioCliente` | Igual que en la Iteración 1 |
| `PagosApi` | pagos | `ServicioPago` | Agrega `devolver`, `registrarDevolucionManual`, `registrarLiquidacion`, `liquidacion` y `consultarTransaccion` (DF-87, DF-117, DF-125) |
| `VentasConsultaApi` | ventas | `ServicioVentasConsulta` | Agrega `ventasDelMes`, `comprobantesDelMes` y `ventasSinComprobante` (DF-126) |
| `CatalogoApi` | catalogo | `ServicioCatalogo` | Agrega `insumosDeVariante` (DF-94, DF-99) |
| `InventarioApi` | inventario | `ServicioProducto` | `buscar`, `verificarStock` y `registrarConsumoAtencion` (DF-96, DF-99) |
| `PersonalApi` | personal | `ServicioPersonal` | Agrega `listarEspecialistas` y `listarPersonal` (DF-85) |
| `RecursoApi` | sedes | `ServicioRecurso` | Un solo `listarRecursos` (DF-78) |
| `HistorialComprasProvider` | comun | `ServicioVentasConsulta` (ventas) | Evita el ciclo `agenda` → `ventas` (DF-101) |
| `AvisoProvider` | comun | `ServicioVentasConsulta`, `ServicioProducto`, `ServicioCaja` | `avisos(idSede)` (D-23, DF-79) |
| `ParametroApi` | comun | `ServicioParametro` (seguridad) | Agrega `obtenerValorNumero` (DF-80) |
| `UsuarioActual` | comun | `UsuarioActualSesion` (seguridad) | `idPersonal()` y `esAdministradora` se leen cada vez (DF-82) |
| `AuditoriaApi` | comun | `ServicioAuditoria` (seguridad) | Igual que en la Iteración 1 |
| `CajaAbiertaProvider` | comun | `ServicioCaja` | Igual que en la Iteración 1 |

## Tablas que no escribe ningún CUS de la Iteración 2

`ddl-auto=validate` revisa las 51 tablas de la iteración, pero ningún CUS de la Iteración 2 escribe estas en Java: `Compra`, `CompraDetalle`, `RequerimientoCompra` y `Proveedor` (CUS27 a CUS31 no son de esta iteración). `MovimientoKardex` lo escriben Java (CUS33, CUS34 y el consumo de CUS11) y Oracle (TRG_VENTA_EFECTOS al cerrar o anular una venta).
