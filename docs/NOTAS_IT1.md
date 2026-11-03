# Notas de la Iteración 1 · Back-end

## Dudas

- **D-04. Tabla `PAQUETE_SERVICIO`.** `RepositorioClientePaquete.findActivoPorCliente(idCliente, idServicio)` (CUS11, RN36) necesita saber qué servicios incluye un paquete, y eso solo está en `PAQUETE_SERVICIO`, una de las 11 tablas fuera de la iteración. Por ahora es una consulta nativa sin entidad y, sin datos en esa tabla, nunca encuentra paquete. ¿Se agrega a las tablas de la iteración o se queda así?
- **D-05. `codigo_operacion` del enlace de pago (para B4).** `ServicioPago.confirmar` busca con `findByCodigoOperacion`, pero en CUS07 ningún mensaje guarda ese código antes del pago: `registrarEnlace(enlacePago, fechaExpiracion)` no lo recibe y `aprobar(codigoOperacion)` lo escribe después de buscar. ¿Se agrega a `registrarEnlace` o la búsqueda usa otro dato de la notificación? Las clases quedan como en el diagrama.
- **D-06. Elegir un PERSONAL sin usuario (CUS52).** La pantalla necesita listar al personal sin cuenta, pero ni el diagrama ni `U2.md` §5.3 traen ese endpoint (CUS52 solo tiene `GET /roles` y `POST /usuarios`). No se creó. ¿Se agrega, por ejemplo, `GET /api/v1/personal/sin-usuario`, o la pantalla recibe el id de otra forma?
- **D-07. Quitarle permisos a ADMINISTRADORA.** `PUT /roles/{idRol}/permisos` permite dejar a ese rol sin `SEGURIDAD`, y nadie más podría devolvérselos desde la aplicación. Ninguna regla lo prohíbe y no se inventó una. ¿Se bloquea?

## Diferencias encontradas

Se corrigió con las respuestas del responsable lo que antes estaba aquí (ver «Decisiones tomadas»). Quedan:

- **O-20 (Unidad 2, abierta y opcional).** `ConsultaDocumentoService` + `ConsultaDocumentoApi` del C3 de la Unidad 1 equivalen a `ServicioPersona` + `PersonaApi` de la Unidad 2 (módulo `seguridad`). Se usan los nombres de la Unidad 2.
- **DF-05. Clases que no están en los diagramas.** `comun.api.SiNoConverter` (lo nombra `U2.md` §3.4), `comun.api.ReglaNegocioException` (excepción de las reglas; `ApiExceptionHandler` la traducirá a 409 o 422), `ventas.domain.DocumentoComprador` (solo aparece como tipo en la firma de `generarComprobante`), las cuatro `XId` de las PK compuestas y los enums de tablas sin diagrama (`FormaDevolucion`, `EstadoProveedor`, `TipoAdquisicion`, `PrioridadRequerimiento`, `EstadoRequerimientoCompra`).
- **DF-06. Métodos que no están en los diagramas.** Repositorios: `RepositorioCaja.findAbiertaBySede` (`CajaAbiertaProvider.obtenerCajaAbierta` no tiene otra lectura; `existsAbiertaBySede` la reutiliza para quedar bloqueada), `RepositorioCita.findOcupacionEntre`, `RepositorioAtencion.findOcupacionSinCita`, `RepositorioPersona.findByNombresOTelefono` (`PersonaApi.buscarPersonas` no dibuja su lectura) y `RepositorioPermiso.findByModuloAndAccion`. Entidades: `new Permiso(modulo, accion)` para `findOrCreate`; `Usuario.registrarIntentoFallido` no está en la clase, pero sí en la nota de CUS50 y en `U2.md`.
- **DF-07. Firmas de los repositorios.** Los diagramas devuelven la entidad o `Transaccion`; `findByNombreUsuario`, `findByCodigoOperacion` y `findActivoPorCliente` devuelven `Optional` y el tipo es `TransaccionPasarela`. `findByDocumentoOTelefono` devuelve `List`: el documento y el teléfono pueden ser de dos personas. `save`, `findById`, `findAll`, `findAllById` y `existsById` vienen de `JpaRepository`.
- **DF-08. `findOcupacion` y el bloqueo.** La secuencia dice `SELECT ... FROM CITA, ATENCION` y `U2.md` §5.1 le pone `@Lock(PESSIMISTIC_WRITE)`, pero una lectura de las dos tablas o de `V_AGENDA_BLOQUES` no se puede bloquear. Se separó: `findOcupacion` (citas, con `@Lock`) y `findOcupacionSinCita` (`FOR UPDATE` nativo). Ojo en B2: el mismo método lo usan CUS05 y CUS08, así que consultar la agenda también bloquea filas hasta que termine la transacción.
- **DF-09. Fin de la cita.** La nota de CUS05 dice que `fecha_hora_fin` = inicio + duración, pero `TRG_CITA_REGLAS` le suma la limpieza del recurso. Gana el script: la columna la asigna Oracle y Java la vuelve a leer.
- **DF-10. Orden de inserción en CUS45 (para el servicio de caja).** CUS45 dibuja `registrarEgreso*`, `registrarArqueo*` y un solo `save`. Hibernate cascadea por orden alfabético del atributo (`arqueos` antes que `egresos`), así que el arqueo se calcula sin el egreso. En la prueba Oracle lo rechazó con `CK_CAJA_ARQUEO_OBSERVACION`; con observación, por el código de `TRG_CAJA_REGLAS`, el cierre fallaría con -20062 (no se ejecutó). `ServicioCaja.cerrar` debe hacer `saveAndFlush` después de los egresos; `MapeoEntidadesTest` lo prueba así.
- **DF-11. Totales de la venta.** En CUS18 `calcularTotal(oVenta)` devuelve el total, pero ningún mensaje lo guarda. `Venta` mantiene `CK_VENTA_TOTAL` con un método privado al cambiar líneas, adelanto o saldo. El descuento de la promoción lo calcula Oracle al guardar la línea: `ServicioVenta` debe hacer flush antes de leer el total. Si `IGV_PORCENTAJE` deja de ser 0, hará falta un método que asigne el IGV.
- **DF-12. `definirInsumo(idVariante, ...)`.** En CUS57 las variantes son nuevas y no tienen id hasta `save`, aunque el mensaje va antes. Se dejó la firma del diagrama: solo funciona con variantes ya guardadas. `ServicioCatalogo` lo resuelve guardando antes (ver «Decisiones tomadas»).
- **DF-13. RN30 en `Caja`.** La diferencia la calcula Oracle al insertar el arqueo, así que `Caja.cerrar` solo revisa las ya calculadas. Un arqueo nuevo con diferencia y sin observación lo rechaza `CK_CAJA_ARQUEO_OBSERVACION` con ORA-02290, no con un ORA-20xxx: `ApiExceptionHandler` debe traducir ese CONSTRAINT.
- **DF-14. `TipoRecurso` en dos módulos.** `Recurso` (sedes) y `Servicio` (catalogo) usan el mismo tipo en el diagrama. Un módulo no usa clases internas de otro, así que hay un enum en cada `domain`, con los mismos valores de los CHECK.
- **DF-15. Relaciones de `U2.md` §3.3.** Lista asociaciones JPA como `Cita → citaOrigen`, `Persona → TipoDocumento`, `Atencion → Cita`, `TransaccionPasarela → MedioPago` y `Producto → CategoriaProducto`. `CLAUDE.md` y la página de dominio las tienen como `idX`; se siguió `CLAUDE.md`.
- **DF-16. `ServicioPago.confirmar(codigoOperacion, estado: EstadoCita)` (para B4).** Recibe el estado de una transacción, así que el tipo debería ser `EstadoTransaccion`. Aún no se crea.
- **DF-17. Repositorios de B1 sin commit.** Los 18 `RepositorioX` no entraron en ningún commit de `tineo-feature/b1-dominio`: el commit `2ae53ca`, con mensaje de repositorios, trae las entidades de los módulos de apoyo. La rama ya fusionada no compila sus pruebas sin esos archivos. Van en el primer commit del plan de B2.
- **DF-18. Lo que B2 agrega fuera de los diagramas.**
  - Por pedido expreso: `POST /api/v1/auth/cambio-clave` con `ControladorAutenticacion.cambiarClave`, `ServicioAutenticacion.cambiarClave`, `CambioClaveDTO` y `Usuario.cambiarClave` (este último está en `U2.md` §1.6). `AuditoriaApi.consultar` lee `V_AUDITORIA`, sin endpoint: `GET /auditoria` es CUS55, de otra iteración.
  - Nombrados en `U2.md`: `PersonaApi.registrarPersona`, `actualizarPersona` y `obtenerPersona` (§1.8), `ConsultaDocumentoPort` (§1.8), `ServicioParametro` y `Rol.revocarPermiso` (§1.6), `Persona.actualizar` (§1.6), `ApiExceptionHandler` (§5.2), `UsuarioActual`, `ParametroApi` y `AuditoriaApi` (§1.5).
  - Sin nombre en ninguna fuente: `comun.api.SesionOracle` y `RegistroAuditoria`; `comun.service.TransaccionConSesionOracle`; `seguridad.service.ConfiguracionSeguridad`, `FiltroCambioClave`, `UsuarioSesion`, `ServicioAuditoria`, `ConsultaDocumentoFalsa`, `DatosPersona`; `RepositorioParametro`; `personal`: `PersonalApi.obtenerIdSede`, `ServicioPersonal`, `RepositorioPersonal`; los DTO de cuerpos que el diagrama pasa como parámetros sueltos (`CredencialesDTO`, `AsignarRolDTO`, `AreaDTO` de entrada).
- **DF-19. Auditoría del inicio de sesión.** La secuencia de CUS50 hace `new Auditoria` también con la clave correcta, pero Java solo inserta el inicio fallido, el cierre de sesión y el 403 (ADR-08, pedido de B2). El ingreso correcto ya deja su fila: `TRG_AUD_USUARIO` registra el cambio de `ultimo_acceso`.
- **DF-20. Inicio fallido de un usuario inexistente.** `AUDITORIA.id_usuario` es obligatorio, así que un nombre desconocido no deja fila; solo responde 401, con el mismo mensaje que una clave errada.
- **DF-21. `cerrarSesion(idUsuario)`.** El diagrama lo muestra como parámetro de `POST /auth/logout`; el id sale de la sesión para que nadie cierre ni audite en nombre de otro.
- **DF-22. `PUT /roles/{idRol}/permisos`.** La secuencia solo concede, pero un PUT reemplaza: los permisos que no vienen se revocan con `Rol.revocarPermiso`. Los permisos se leen al iniciar sesión, así que un cambio vale desde el siguiente ingreso.
- **DF-23. Sede de la sesión y ciclo futuro.** La sede sale de `PERSONAL.id_sede`, así que `seguridad` usa `PersonalApi`. Cuando `personal` use `PersonaApi` (CUS36, otra iteración), habrá un ciclo `seguridad` ⇄ `personal` que `ModularidadTest` rechazará; entonces hará falta un SPI en `comun`, como `PorcentajeComisionProvider` en `U2.md` §1.8.
- **DF-24. `RecursoApi`.** La nota de CUS61 dice que `ServicioRecurso` la implementa, pero ningún diagrama de este bloque trae sus métodos. Se crea en B3, cuando agenda los pida.
- **DF-25. Verificación que viene del cliente (para B5).** `registrarPersona` marca VERIFICADO si `PersonaDTO.fechaVerificacion` viene con valor, como en la secuencia de CUS02. Oracle solo exige que el tipo tenga `consulta_api = 'S'`, así que un cliente podría declararse verificado. Hasta B5 la consulta siempre responde ERROR y los flujos quedan MANUAL.
- **DF-26. Zona horaria de Oracle y de Java.** El contenedor de pruebas, y una base local creada como dice el README, corren en UTC; Java usa la hora de Lima. Por eso el bloqueo se decide con `RepositorioUsuario.tieneBloqueoVigente`, que usa el reloj de Oracle (no está en el diagrama), y no con `Usuario.bloqueadoHasta`. Las fechas que escribe Java (`ultimo_acceso`) y las que escriben los triggers (`bloqueado_hasta`, `AUDITORIA.fecha_hora`) quedan en zonas distintas si el servidor no está en Lima: conviene fijar `TZ=America/Lima` en el servidor de Oracle. Lo mismo vale para B3, donde los triggers comparan citas con `SYSTIMESTAMP`.
- **DF-27. Orden entre validación y permiso.** `@PreAuthorize` está en el controlador, y Spring valida el cuerpo antes de invocarlo: un cuerpo inválido de alguien sin permiso recibe 400 y no 403. No expone datos; un cuerpo válido sí recibe 403 y queda auditado.

## Pendiente de vigilar

- **Corte intermitente de Oracle en las pruebas.** Una de tres corridas de `./mvnw verify` falló al instalar `database/`, con `ORA-03113` en una consulta de resumen de `00_instalar_v5.sql`, después de crear las 55 tablas; la siguiente corrida pasó sin cambios en ese código. Ocurrió con el contenedor `oracle-local` también encendido, así que puede ser falta de recursos, pero no se confirmó la causa. Si se repite, conviene mirar el log del contenedor.

## Entidades y repositorios frente al draw.io

**Entidades: 44, una por tabla de la página «BD · Modelo lógico-relacional · It. 1».** Un script comparó las 36 clases de «General · Clases del diseño (dominio)» y coinciden todos sus atributos y tipos.

| Módulo | Del draw.io de dominio | Además, de la página de BD |
| --- | --- | --- |
| seguridad | Usuario, Rol, RolPermiso, Permiso, Auditoria, TipoDocumento, Persona | Parametro |
| sedes | Sede, Recurso | |
| catalogo | Area, Servicio, VarianteServicio, ServicioInsumo, Promocion, Paquete | |
| agenda | Cliente, SaldoFavorMov, ClientePaquete, Cita, Atencion, ListaEspera | |
| ventas | Venta, VentaDetalle, VentaPago, SerieComprobante, Comprobante | NotaCredito |
| pagos | MedioPago, TransaccionPasarela | |
| caja | Caja, CajaEgreso, CajaArqueo | |
| personal | Personal, PersonalArea | HorarioPersonal |
| inventario | Producto, InventarioSede, MovimientoKardex | CategoriaProducto, Proveedor, RequerimientoCompra, Compra, CompraDetalle |

**Repositorios: los 18 del draw.io, sin agregar ni quitar.** RepositorioUsuario, RepositorioRol, RepositorioPermiso, RepositorioAuditoria, RepositorioTipoDocumento, RepositorioPersona (seguridad); RepositorioRecurso (sedes); RepositorioArea, RepositorioServicio (catalogo); RepositorioCliente, RepositorioCita, RepositorioListaEspera, RepositorioAtencion, RepositorioClientePaquete (agenda); RepositorioVenta, RepositorioComprobante (ventas); RepositorioTransaccionPasarela (pagos); RepositorioCaja (caja).

## Decisiones tomadas

- **D-01. ADR.** No se agrega el documento de la Unidad 1. Los ADR están en la presentación de la Semana 5, que es de la versión 4 (65 tablas) y tiene partes superadas por el modelo v5: `CONSULTA_DOCUMENTO` y `SEDE_CANAL` ya no existen, y la auditoría la hacen los triggers `TRG_AUD_*` de Oracle, no un `@Auditable` en Java. La versión vigente de cada ADR es la que resume `CLAUDE.md`; si una regla no está ahí, se pregunta.
- **D-02. Otro draw.io.** `docs/Diagramas_secuencia_clase_ADS.drawio` es el modelo general de los 67 CUS: se consulta solo como contexto. Para los 20 CUS de la Iteración 1 manda `docs/Iteracion_1_Cautiva_Spa.drawio`; si se contradicen, gana Iteracion_1 y se anota en «Diferencias encontradas».
- **D-03 y DF-02. `CajaAbiertaProvider`.** Es un SPI de `comun` (como dicen `docs/U2.md` y el C3 de la arquitectura) y lo implementa `caja` (ServicioCaja). `CLAUDE.md` ya lo lista así.
- **DF-01. Módulo `reportes`.** Confirmado: no se crea en esta iteración.
- **DF-03. `PKG_VENTA`.** Confirmado: solo lo usan los triggers; Java no lo llama.
- **DF-04. Cabecera de `00_instalar_v5.sql`.** Resuelto: ya dice el orden 00 → 14 → 13 → 15 → 16.
- **Las 44 tablas tienen entidad.** También las de solo estructura que aún nadie usa en Java (`Compra`, `CompraDetalle`, `RequerimientoCompra`, `Proveedor`), para que `ddl-auto=validate` revise todo el modelo de la iteración. Las semilla y de solo estructura solo tienen columnas, sin métodos de negocio.
- **Columnas que reescribe Oracle.** Con `@Generated` Hibernate las vuelve a leer al guardar: serie, número y fecha del comprobante, fin de la cita, esperado y diferencia del arqueo, y precio, descuento, subtotal, especialista y comisión de la línea de venta. Sin esto, el objeto en memoria queda con valores viejos durante la transacción.
- **`@DynamicUpdate` donde hay triggers por columna.** Sin esto Hibernate pone todas las columnas en el UPDATE: `UPDATE OF` de `TRG_CITA_CRUCE` se dispararía en cualquier cambio de estado y volvería a validar el cruce. Se usa en Usuario, Persona, Cita, Atencion, Venta, VentaDetalle, Comprobante, TransaccionPasarela, Caja y CajaArqueo.
- **Consentimiento al crear `Cliente`.** El constructor deja `aceptaComunicaciones` en falso, no en el `'S'` de la columna, para que olvidar `registrarConsentimiento` nunca dé por aceptado (RN27).
- **Sesión Oracle en el administrador de transacciones.** `TransaccionConSesionOracle` extiende `JpaTransactionManager`: llama a `PKG_SESION.iniciar` justo después de abrir la transacción, antes de cualquier sentencia, y a `terminar` antes de soltar la conexión, también si hubo rollback. `CLAUDE.md` sugiere `TransactionSynchronization` solo como ejemplo; esa vía no garantiza correr antes de la primera sentencia. `terminar` corre en toda transacción, con o sin usuario, para que ninguna conexión vuelva al pool identificada.
- **Permisos como `MODULO:ACCION`.** Las authorities salen tal cual de `PERMISO`, en mayúsculas: `SEGURIDAD:CREAR`. Por endpoint: `GET /roles` = `SEGURIDAD:VER`; `POST /usuarios` = `SEGURIDAD:CREAR`; `PATCH .../rol` y `PUT .../permisos` = `SEGURIDAD:EDITAR`; `GET /areas` = `CATALOGO:VER`; `POST /areas` y `POST /servicios` = `CATALOGO:CREAR`; `POST /recursos` = `SEDES:CREAR`.
- **CSRF.** `POST /auth/login` queda fuera porque no hay un GET público previo que entregue la cookie `XSRF-TOKEN`; esa cookie llega en la respuesta del login. Todo lo demás la exige.
- **Respuestas de error desde los filtros.** El 401, el 403 y el cambio de clave obligatorio pasan por `ApiExceptionHandler`, para que todos salgan como `ProblemDetail`.
- **Cambio de clave obligatorio.** Con `debe_cambiar_clave = 'S'`, `FiltroCambioClave` responde 403 con regla RF82 a todo salvo `cambio-clave` y `logout`. La clave nueva lleva de 8 a 72 caracteres: 72 es el límite de BCrypt y 8 es un mínimo elegido aquí, sin regla que lo fije.
- **DF-12 resuelta en `ServicioCatalogo`.** `POST /servicios` trae los insumos dentro de cada variante; el servicio guarda con `saveAndFlush` para tener el `idVariante` y luego llama a `definirInsumo`, con la firma del diagrama.
- **Adaptador falso.** `ConsultaDocumentoFalsa` se usa en todos los perfiles hasta B5, aunque el perfil `prod` declare `apisperu.modo: real`.
- **Plan de commits de B2.** El primer commit reúne los repositorios de varios módulos porque recupera lo que faltó en B1 (DF-17); es la única excepción. Desde `feat(comun)` hasta `feat(seguridad)` el proyecto compila, pero el contexto no arranca hasta que `seguridad` implementa `UsuarioActual` y `AuditoriaApi`.
- **Bloqueo vencido en `Usuario.autenticar`.** Si ya pasó `bloqueado_hasta`, la cuenta vuelve a ACTIVO antes de comparar la clave, para que los errores siguientes vuelvan a contar.
- **Plan de commits de B1.** Un commit por módulo más repositorios, pruebas y documentación son 13, y el máximo es 8. `seguridad`, `agenda` y `ventas` llevan commit propio; las entidades de `sedes`, `catalogo`, `pagos`, `caja`, `personal` e `inventario` van juntas y los repositorios también, como única excepción a no mezclar módulos.
- **Ambientes de ADR-17.** `local` = perfil `dev` y `produccion` = perfil `prod`. El ambiente `sandbox` (APIs reales en modo de pruebas) se agrega en B5 como `application-sandbox.yaml`.
- **Sin Flyway.** La base se instala con SQL*Plus desde `database/`, y CAUTIVA_APP no puede crear la tabla de historial de Flyway.
- **Paquetes con `package-info.java`.** Java y git no conservan paquetes vacíos, así que cada módulo y capa tiene su `package-info.java`. La raíz del módulo lleva `@ApplicationModule` y `api` lleva `@NamedInterface("api")`: sin esto, Spring Modulith trataría `api` como interno y otro módulo no podría usarlo.
- **Selector de adaptadores.** Los perfiles declaran `cautiva.integraciones.apisperu.modo` y `cautiva.integraciones.izipay.modo` con `falso` o `real`. Hoy nadie los lee; los adaptadores, su URL y sus credenciales (por variables de entorno) llegan en B2 y B4. El nombre de la propiedad no sale de los diagramas y se puede ajustar entonces.
- **Versiones fijadas en el pom.** Spring Modulith 2.1.2, springdoc 3.2.0, MapStruct 1.6.3, lombok-mapstruct-binding 0.2.0 y WireMock 3.13.2 (`wiremock-standalone`). Testcontainers 2.0.5 viene de Spring Boot, y en esa versión sus artefactos se llaman `testcontainers-oracle-free` y `testcontainers-junit-jupiter`.
- **Imagen de las pruebas.** `gvenzl/oracle-free:23.26.2` en `OracleTestcontainer`: etiqueta fija de la misma versión 26ai Free que la base local (SQL*Plus 23.26.2.0.0 en el contenedor `oracle-local`), en lugar de `latest`, que ya apunta a 23.26.3.
- **JDK usado al verificar.** `./mvnw verify` se corrió con el JDK 26, el único instalado en la PC; compila para Java 21 (`java.version`). Falta repetirlo con un JDK 21.
- **Plan de commits de esta tarea.** La regla de no mezclar dos módulos en un commit choca con el máximo de 8 commits, porque hay 10 módulos. El esqueleto (solo `package-info.java`, sin lógica) va en un único commit; es la única excepción.
- **Perfil de pruebas.** `src/test/resources/application-test.yaml`. `OracleTestcontainer` entrega las mismas variables que usa la aplicación (`CAUTIVA_DB_*`), sin `.env`.

## Revisión de database/

| Script | Qué hace |
| --- | --- |
| `00_instalar_v5.sql` | Instala todo: corre 01 a 12 (tablas, FK e índices, jerarquías, tipos de documento, paquetes, triggers de reglas y de auditoría, vistas y datos iniciales) e imprime errores y conteos. No corre 13 ni 14. |
| `14_cuenta_aplicacion.sql` | Como DBA: perfil `PRF_CAUTIVA_SERVICIO`, roles `ROL_CAUTIVA_APLICACION` y `ROL_CAUTIVA_REPORTES` y la cuenta `CAUTIVA_APP`, bloqueada y sin clave conocida. Se puede repetir. |
| `13_permisos.sql` | Como dueño: permisos de mínimo privilegio a los dos roles (55 tablas, 27 vistas, 4 paquetes). Se puede repetir. |
| `15_datos_iteracion1.sql` | Como dueño: 5 personas de personal con horarios y áreas, y 4 usuarios (`admin`, `recepcion`, `especialista1`, `especialista2`) en la sede Pichanaki Centro. Solo local y pruebas; no hace nada si `admin` ya existe. |

**Tablas.** 01 a 04 crean las 55 tablas (13 + 15 + 14 + 13). Las 44 de la página «BD · Modelo lógico-relacional · It. 1» están todas en los scripts. Las 11 restantes no son de la iteración: ADELANTO, ASISTENCIA, CONCILIACION, CONCILIACION_ITEM, INCIDENTE_SEGURIDAD, PAQUETE_SERVICIO, PLANILLA, PLANILLA_DETALLE, PROMOCION_SERVICIO, RECOMENDACION_PRODUCTO y REQUERIMIENTO_DETALLE.

**Cruce con la hoja «Trazabilidad CUS» del diccionario.** Los 20 CUS nombran 36 de esas 44 tablas y ninguna fuera de ellas. Las otras 8 no las nombra ningún CUS de la iteración: CATEGORIA_PRODUCTO, COMPRA, COMPRA_DETALLE, HORARIO_PERSONAL, NOTA_CREDITO, PARAMETRO, PROVEEDOR y REQUERIMIENTO_COMPRA.

**Triggers y paquete pedidos.** Todos existen:

| Objeto | Archivo |
| --- | --- |
| `TRG_USUARIO_REGLAS` | `09_triggers_reglas.sql:125` |
| `TRG_SALDO_FAVOR_REGLAS` | `09_triggers_reglas.sql:617` |
| `TRG_VENTA_EFECTOS` | `09_triggers_reglas.sql:762` |
| `TRG_CITA_ADELANTO` | `09_triggers_reglas.sql:1453` |
| `PKG_SESION` | `08_paquetes.sql:27` (especificación) y `:55` (cuerpo) |

También existen los 54 `TRG_AUD_*`, `TRG_KARDEX_APLICAR`, `TRG_CITA_CRUCE`, `TRG_ATENCION_CRUCE`, `TRG_ATENCION_PAQUETE`, `TRG_PERSONAL_DESACTIVAR` y los 8 códigos de PARAMETRO en `12_datos_iniciales.sql`.
