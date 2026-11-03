# Trazabilidad de la Iteración 1

CUS → endpoint → controlador → servicio → repositorio → entidad, comparada con `docs/Iteracion_1_Cautiva_Spa.drawio`.

**Cómo se comparó.** Un script leyó las 20 páginas CUS del draw.io: los mensajes HTTP de la secuencia y las clases del diagrama de diseño con sus métodos. Luego buscó cada clase y cada método en `src/main/java`.

**Resultado:**

- Las 20 páginas tienen 35 endpoints distintos y los 35 existen en el código.
- Ninguna clase ni método del draw.io falta en el código. `save`, `findById` y similares vienen de `JpaRepository`.
- La comparación es por nombre. Las firmas que cambian se listan en la columna «Frente al draw.io», con su nota en [NOTAS_IT1.md](NOTAS_IT1.md).
- El código tiene 2 endpoints más que el draw.io (ver el final). Son 37 en total, y son los que trae [openapi.yaml](openapi.yaml).

Las clases son del módulo del controlador, salvo cuando se indica el módulo. `API` es una interfaz publicada por otro módulo; quien la implementa está en «Interfaces publicadas».

| CUS | Endpoint | Controlador | Servicio | Repositorio | Entidad | Frente al draw.io |
| --- | --- | --- | --- | --- | --- | --- |
| CUS01 | `GET /api/v1/clientes` | `ControladorCliente.buscar` | `ServicioCliente.buscar`, `PersonaApi.buscarPersonas` | `RepositorioCliente`, `RepositorioPersona` | `Cliente`, `Persona` | Coincide. `findByNombresOTelefono` no está dibujado (DF-06) |
| CUS02 | `GET /api/v1/clientes/consulta-documento` | `ControladorCliente.consultarDocumento` | `ServicioCliente.consultarDocumento`, `ServicioPersona.consultarDocumento`, `AdaptadorApisPeruConsulta.consultar` | `RepositorioTipoDocumento` | `TipoDocumento` | Agrega 202 con `NO_ENCONTRADO` o `ERROR` (DF-34) y `fechaVerificacion` (DF-53) |
| CUS02 | `POST /api/v1/clientes` | `ControladorCliente.registrar` | `ServicioCliente.registrar`, `PersonaApi.registrarPersona` | `RepositorioPersona`, `RepositorioCliente` | `Persona`, `Cliente` | Coincide |
| CUS05 | `GET /api/v1/citas/disponibilidad` | `ControladorCita.consultarDisponibilidad` | `ServicioCita.consultarDisponibilidad`, `GestorDisponibilidadAgenda.calcularHorasLibres` | `RepositorioCita.findBloques` | `V_AGENDA_BLOQUES` | Usa `findBloques` sin bloqueo en vez de `findOcupacion` (DF-29). `idSede` sale de la sesión (DF-35). Paso de las horas: D-08 |
| CUS05 | `POST /api/v1/citas` | `ControladorCita.agendar` | `ServicioCita.agendar`, `GestorDisponibilidadAgenda.validarSinCruce` | `RepositorioCita`, `RepositorioAtencion`, `RepositorioCliente` | `Cita` | Agrega comprobaciones de clienta y especialista (DF-32) |
| CUS06 | `POST /api/v1/citas/lista-espera` | `ControladorCita.registrarEspera` | `ServicioCita.registrarEspera`, `GestorDisponibilidadAgenda.calcularHorasLibres` | `RepositorioListaEspera`, `RepositorioCita` | `ListaEspera` | Coincide |
| CUS07 | `POST /api/v1/citas/{idCita}/adelanto` | `ControladorCita.registrarAdelantoEfectivo` | `ServicioCita.registrarAdelantoEfectivo`, `CajaAbiertaProvider.obtenerCajaAbierta` | `RepositorioCita` | `Cita` | Recibe `idMedioPago` (DF-38) |
| CUS07 | `POST /api/v1/citas/{idCita}/enlace-adelanto` | `ControladorCita.generarEnlaceAdelanto` | `ServicioCita.generarEnlaceAdelanto`, `ServicioPago.generarEnlace`, `AdaptadorIzipay.crearEnlace` | `RepositorioCita`, `RepositorioTransaccionPasarela` | `Cita`, `TransaccionPasarela` | Recibe `idMedioPago` (DF-38). `registrarEnlace` también guarda el código (D-05). `ServicioCita` usa `PagosApi`, no `ServicioPago` directo |
| CUS07 | `POST /api/v1/pagos/notificaciones` | `ControladorNotificacionPasarela.recibir` | `ServicioPago.confirmar`, `ManejadorAdelantoConfirmado.on` | `RepositorioTransaccionPasarela`, `RepositorioCita` | `TransaccionPasarela`, `Cita` | El manejador recibe el evento `AdelantoAprobado`; `ServicioPago` no lo conoce (`U2.md`). Sin caja abierta queda sin caja (DF-40). `confirmar` recibe el estado como texto (DF-41) |
| CUS08 | `GET /api/v1/citas/agenda` | `ControladorCita.consultarAgenda` | `ServicioCita.consultarAgenda`, `GestorDisponibilidadAgenda.armarAgenda` | `RepositorioCita.findBloques` | `V_AGENDA_BLOQUES`, `Cita`, `Atencion` | Sin bloqueo (DF-29). No descuenta el almuerzo (D-09) |
| CUS11 | `POST /api/v1/atenciones` | `ControladorAtencion.iniciar` | `ServicioAtencion.iniciar`, `GestorDisponibilidadAgenda.validarSinCruce` | `RepositorioCita`, `RepositorioAtencion` | `Cita`, `Atencion` | Sobrecarga de `validarSinCruce` que excluye la cita propia (DF-30). Cuerpo en `InicioAtencionDTO` (DF-35) |
| CUS11 | `PATCH /api/v1/atenciones/{idAtencion}/fin` | `ControladorAtencion.finalizar` | `ServicioAtencion.finalizar` | `RepositorioAtencion`, `RepositorioClientePaquete`, `RepositorioCita` | `Atencion`, `ClientePaquete`, `Cita` | `contarSesionesUsadas` lee aparte `V_SESIONES_PAQUETE` (DF-35). La rama de paquete no se ejercita (D-04) |
| CUS13 | `GET /api/v1/ventas/rapidas/libres` | `ControladorVentaRapida.libresAhora` | `ServicioVentaRapida.libresAhora`, `AtencionApi.libresAhora`, `GestorDisponibilidadAgenda.especialistasYRecursosLibres` | `RepositorioCita.findBloques` | `V_AGENDA_BLOQUES` | `idSede` sale de la sesión (DF-35) |
| CUS13 | `POST /api/v1/ventas/rapidas` | `ControladorVentaRapida.registrarYCobrar` | `ServicioVentaRapida.registrarYCobrar`, `AtencionApi.registrarSinCita`, `ServicioVenta.crearDesdeAtencion`, `registrarPagos`, `cerrar` | `RepositorioAtencion`, `RepositorioVenta` | `Atencion`, `Venta` | **Diferencia:** agrega `AtencionApi.finalizarSinCita`, porque Oracle solo cobra una atención FINALIZADA (DF-37). El pago digital no se cierra aquí (DF-51) |
| CUS18 | `POST /api/v1/ventas` | `ControladorVenta.crearDesdeAtencion` | `ServicioVenta.crearDesdeAtencion`, `CajaAbiertaProvider.obtenerCajaAbierta`, `AtencionApi.obtenerAtencionCobrable` | `RepositorioVenta` | `Venta`, `VentaDetalle` | Cuerpo en `CrearVentaDTO` (DF-42) |
| CUS18 | `PATCH /api/v1/ventas/{idVenta}/descuentos` | `ControladorVenta.aplicarDescuentos` | `ServicioVenta.aplicarDescuentos`, `SaldoFavorApi.usarSaldo`, `CalculadoraVenta.calcularTotal` | `RepositorioVenta`, `RepositorioCliente` | `Venta`, `VentaDetalle`, `Cliente`, `SaldoFavorMov` | El adelanto sale de la cita de la atención (DF-44) |
| CUS18 | `POST /api/v1/ventas/{idVenta}/pagos` | `ControladorVenta.registrarPagos` | `ServicioVenta.registrarPagos`, `PagosApi.cobrar`, `ServicioPago` | `RepositorioVenta`, `RepositorioTransaccionPasarela` | `Venta`, `VentaPago`, `TransaccionPasarela` | **Diferencia:** cobro en dos pasos, con `cobrosPendientes` y `idTransaccion` (DF-51). `usaPasarela` no está dibujado (DF-42) |
| CUS18, CUS19 | `POST /api/v1/ventas/{idVenta}/cierre` | `ControladorVenta.cerrar` | `ServicioVenta.cerrar`, `ServicioFacturacionElectronica.emitirComprobante`, `AdaptadorApisPeruFacturacion.emitir`, `AlmacenArchivosPort.guardar`, `PaqueteClienteApi.registrarPaquete` | `RepositorioVenta`, `RepositorioComprobante`, `RepositorioClientePaquete` | `Venta`, `Comprobante`, `SerieComprobante`, `ClientePaquete` | Emite en la misma transacción; `U2.md` dice después del commit (DF-43). `registrarPaquete` no se ejercita |
| CUS19 | `GET /api/v1/ventas/consulta-ruc/{ruc}` | `ControladorVenta.consultarRuc` | `ServicioVenta.consultarRuc`, `PersonaApi.consultarDocumento` | `RepositorioTipoDocumento` | `TipoDocumento` | Agrega `obtenerIdTipoDocumento` y 202 (DF-42, DF-34) |
| CUS20 | `GET /api/v1/comprobantes/{idComprobante}/representacion` | `ControladorComprobante.obtenerRepresentacion` | `ServicioFacturacionElectronica.generarRepresentacionImpresa`, `GeneradorPdfComprobante.generar` | `RepositorioComprobante` | `Comprobante` | **Diferencia:** el PDF se arma con la tabla, sin el mensaje `leer(rutaXml)` del diagrama. `leer` solo existe en `AlmacenArchivosPort` |
| CUS20 | `GET /api/v1/comprobantes/{idComprobante}/enlace` | `ControladorComprobante.obtenerEnlace` | `ServicioFacturacionElectronica.generarEnlacePublico`, `FirmaEnlaceComprobante.firmar` | `RepositorioComprobante` | `Comprobante` | `canal` es texto: el tipo del diagrama es de la pasarela (D-12) |
| CUS21 | `POST /api/v1/proformas` | `ControladorVenta.armarProforma` | `ServicioVenta.armarProforma`, `CatalogoApi.precioVigente`, `CalculadoraVenta.calcularTotal` | `RepositorioServicio` | `VarianteServicio`, `Paquete`, `Promocion` | Coincide; no guarda nada |
| CUS44 | `POST /api/v1/cajas` | `ControladorCaja.abrir` | `ServicioCaja.abrir` | `RepositorioCaja` | `Caja` | `idSede` sale de la sesión |
| CUS45 | `GET /api/v1/cajas/{idCaja}/esperado` | `ControladorCaja.calcularEsperado` | `ServicioCaja.calcularEsperado`, `VentasConsultaApi.totalesPorMedioPago`, `comprobantesPendientes`, `PagosApi.aprobadosDelDia` | `RepositorioCaja`, `RepositorioVenta`, `RepositorioTransaccionPasarela` | `Caja`, `V_CAJA_ESPERADO`, `V_CPE_PENDIENTES`, `TransaccionPasarela` | `ServicioVentasConsulta` implementa la API para evitar un ciclo (DF-48). Fechas: DF-45 |
| CUS45 | `POST /api/v1/cajas/{idCaja}/cierre` | `ControladorCaja.cerrar` | `ServicioCaja.cerrar` | `RepositorioCaja` | `Caja`, `CajaEgreso`, `CajaArqueo` | Un flush por grupo, por el orden de inserción (DF-10, DF-13) |
| CUS50 | `POST /api/v1/auth/login` | `ControladorAutenticacion.iniciarSesion` | `ServicioAutenticacion.iniciarSesion` | `RepositorioUsuario`, `RepositorioAuditoria` | `Usuario`, `Auditoria`, `Rol`, `Permiso`, `RolPermiso` | Solo audita el inicio fallido (DF-19) |
| CUS51 | `POST /api/v1/auth/logout` | `ControladorAutenticacion.cerrarSesion` | `ServicioAutenticacion.cerrarSesion` | `RepositorioAuditoria` | `Auditoria` | El id sale de la sesión (DF-21) |
| CUS52 | `GET /api/v1/roles` | `ControladorUsuario.listarRoles` | `ServicioUsuario.listarRoles` | `RepositorioRol` | `Rol` | Coincide |
| CUS52 | `POST /api/v1/usuarios` | `ControladorUsuario.registrar` | `ServicioUsuario.registrar`, `CodificadorClave` | `RepositorioUsuario` | `Usuario` | Elegir personal sin usuario sigue abierto (D-06) |
| CUS53 | `PATCH /api/v1/usuarios/{idUsuario}/rol` | `ControladorUsuario.asignarRol` | `ServicioUsuario.asignarRol` | `RepositorioUsuario`, `RepositorioRol` | `Usuario`, `Rol` | Cuerpo en `AsignarRolDTO` (DF-18) |
| CUS53 | `PUT /api/v1/roles/{idRol}/permisos` | `ControladorUsuario.configurarPermisos` | `ServicioUsuario.configurarPermisos` | `RepositorioRol`, `RepositorioPermiso` | `Rol`, `Permiso`, `RolPermiso` | Un PUT también revoca (DF-22). Sin regla para ADMINISTRADORA (D-07) |
| CUS57 | `GET /api/v1/areas` | `ControladorCatalogo.listarAreas` | `ServicioCatalogo.listarAreas` | `RepositorioArea` | `Area` | Coincide |
| CUS57 | `POST /api/v1/areas` | `ControladorCatalogo.registrarArea` | `ServicioCatalogo.registrarArea` | `RepositorioArea` | `Area` | Cuerpo en `AreaDTO` (DF-18) |
| CUS57 | `POST /api/v1/servicios` | `ControladorCatalogo.registrarServicio` | `ServicioCatalogo.registrarServicio` | `RepositorioServicio` | `Servicio`, `VarianteServicio`, `ServicioInsumo` | Guarda antes de definir insumos (DF-12) |
| CUS61 | `POST /api/v1/recursos` | `ControladorRecurso.registrarRecurso` | `ServicioRecurso.registrarRecurso` | `RepositorioRecurso` | `Recurso` | Coincide |

## Endpoints que no están en el draw.io

| Endpoint | Controlador | Servicio | Por qué existe |
| --- | --- | --- | --- |
| `POST /api/v1/auth/cambio-clave` | `ControladorAutenticacion.cambiarClave` | `ServicioAutenticacion.cambiarClave` | Pedido en B2: RF82 obliga a cambiar la clave temporal (DF-18) |
| `GET /api/v1/comprobantes/publico/{idComprobante}` | `ControladorComprobante.obtenerPublico` | `ServicioFacturacionElectronica.obtenerRepresentacionPublica` | Pedido en B5: sin él, el enlace firmado no abre sin sesión (DF-53) |

## Interfaces publicadas

| Interfaz | Módulo | La implementa | Frente al draw.io |
| --- | --- | --- | --- |
| `PersonaApi` | seguridad | `ServicioPersona` | Coincide con `U2.md` |
| `AtencionApi` | agenda | `ServicioAtencion` | La página de capas le dibuja atributos (DF-36) |
| `SaldoFavorApi` | agenda | `ServicioCliente` | Sin clase en el draw.io (DF-42) |
| `PaqueteClienteApi` | agenda | `ServicioClientePaquete` | Nombrada en `U2.md` |
| `PagosApi` | pagos | `ServicioPago` | Coincide |
| `VentasConsultaApi` | ventas | `ServicioVentasConsulta` | Clase nueva (DF-48) |
| `CatalogoApi` | catalogo | `ServicioCatalogo` | Agrega `obtenerVariante` (DF-28) |
| `RecursoApi` | sedes | `ServicioRecurso` | Sin métodos en el draw.io (DF-28) |
| `PersonalApi` | personal | `ServicioPersonal` | Sin métodos en el draw.io (DF-28) |
| `CajaAbiertaProvider` | comun | `ServicioCaja` | Agrega `buscarCajaAbierta` (DF-40) |
| `FacturacionElectronicaPort`, `ConsultaDocumentoPort`, `PasarelaPagosPort` | ventas, seguridad, pagos | Los adaptadores reales; los falsos solo en `dev` y las pruebas (DF-50) | Nombrados en `U2.md`; el draw.io dibuja directo el adaptador |

## No se usan todavía

Están en la iteración porque `ddl-auto=validate` revisa las 44 tablas, pero ningún CUS de estos los escribe en Java: `Compra`, `CompraDetalle`, `RequerimientoCompra`, `Proveedor`, `CategoriaProducto`, `Producto`, `InventarioSede`, `MovimientoKardex` (este lo escribe Oracle al cerrar una venta con productos) y `NotaCredito`.
