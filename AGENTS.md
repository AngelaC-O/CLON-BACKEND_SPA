# Back-end del BackOffice web de Cautiva Spa E.I.R.L. (CS-BO-01) · Iteración 2

## Fuentes de verdad (en este orden)

1. `docs/Cautiva_Spa_Diccionario_de_Datos_v5_1.xlsx`: tablas, columnas, tipos, PK, FK, CHECK, jerarquías y trazabilidad CUS. Reemplaza al v5; la hoja «Cambios v5.1» lista los 18 cambios.
2. `database/`: el script real (triggers, paquetes PL/SQL y vistas). El modelo v5.1 sale de 01 a 15 más la migración `17_datos_iteracion2.sql`.
3. `docs/Iteracion_2_Cautiva_Spa.drawio` y `docs/Iteracion_1_Cautiva_Spa.drawio`: una página por CUS con secuencia, clases del análisis y clases del diseño; páginas generales y modelos de BD.
4. `docs/U2.md`: contratos REST, patrones, eventos.

Qué draw.io manda:

- `Iteracion_2` para los 27 CUS de la Iteración 2 y para los 12 CUS de la Iteración 1 que tienen página «modificado It. 2» (CUS05, CUS06, CUS08, CUS11, CUS13, CUS18, CUS20, CUS44, CUS50, CUS52, CUS53 y CUS61).
- `Iteracion_1` para los otros 8 CUS de la Iteración 1 (CUS01, CUS02, CUS07, CUS19, CUS21, CUS45, CUS51 y CUS57).
- Si se contradicen, gana `Iteracion_2` y se anota en «Diferencias encontradas».

`docs/Diagramas_secuencia_clase_ADS.drawio` es el modelo general de los 67 CUS: solo se consulta como contexto (por ejemplo, para CUS de otras iteraciones). Si contradice a `Iteracion_1` o `Iteracion_2`, ganan estos y se anota.

Prohibido inventar tablas, columnas, actores, endpoints o módulos. Si algo falta o se contradice, no lo resuelvas por tu cuenta: anótalo en `docs/NOTAS_IT2.md` y pregunta.

La versión vigente de cada ADR es la que resume este archivo. La presentación de la Semana 5 es de la versión 4 (65 tablas) y tiene partes superadas por el modelo v5.1; si una regla no está aquí, pregunta.

`docs/` y `database/` no se modifican, salvo `docs/NOTAS_IT2.md`. `docs/NOTAS_IT1.md` queda como historia y no se edita. `database/` se tocó una sola vez en la iteración (08/11/2026: script 17, su prueba 21, la cabecera de 00 y el README). Nunca leas ni copies `.local/`.

## Alcance

Los 20 CUS de la Iteración 1 (ya hechos):

| CUS | Caso de uso | CUS | Caso de uso |
| --- | --- | --- | --- |
| CUS01 | Buscar Clienta | CUS19 | Emitir Comprobante Electrónico |
| CUS02 | Registrar Clienta | CUS20 | Entregar Comprobante |
| CUS05 | Programar Cita | CUS21 | Emitir Proforma |
| CUS06 | Registrar Lista de Espera | CUS44 | Abrir Caja |
| CUS07 | Registrar Adelanto de Reserva | CUS45 | Cerrar Caja |
| CUS08 | Consultar Agenda | CUS50 | Iniciar Sesión |
| CUS11 | Registrar Atención | CUS51 | Cerrar Sesión |
| CUS13 | Registrar Atención sin Cita | CUS52 | Registrar Usuario |
| CUS18 | Cobrar Venta | CUS53 | Asignar Rol |
| CUS57 | Registrar Servicio | CUS61 | Registrar Recurso |

Más estos 27 de la Iteración 2:

| CUS | Caso de uso | CUS | Caso de uso |
| --- | --- | --- | --- |
| CUS03 | Consultar Ficha de Clienta | CUS46 | Aprobar Cierre de Caja |
| CUS04 | Actualizar Clienta | CUS47 | Conciliar Ventas del Mes |
| CUS09 | Reprogramar Cita | CUS48 | Generar Reporte Gerencial |
| CUS10 | Cancelar Cita | CUS49 | Exportar Reporte |
| CUS12 | Registrar Recomendación de Producto | CUS54 | Restablecer Contraseña |
| CUS22 | Anular Venta | CUS55 | Consultar Auditoría |
| CUS23 | Emitir Nota de Crédito | CUS58 | Actualizar Servicio |
| CUS24 | Devolver Pago Digital | CUS59 | Registrar Paquete |
| CUS25 | Buscar Producto | CUS60 | Registrar Promoción |
| CUS26 | Vender Producto | CUS62 | Configurar Medio de Pago |
| CUS32 | Registrar Producto | CUS63 | Configurar Serie de Comprobante |
| CUS33 | Registrar Salida de Insumos | CUS65 | Configurar Parámetro del Negocio |
| CUS34 | Registrar Ajuste de Inventario | CUS66 | Registrar Categoría de Producto |
| CUS35 | Consultar Kardex | | |

Y los cambios de CUS05, CUS06, CUS08, CUS11, CUS13, CUS18, CUS20, CUS44, CUS50, CUS52, CUS53 y CUS61 (páginas «modificado It. 2»).

Usa 51 de las 57 tablas (página «BD · Modelo lógico-relacional · It. 2»). Fuera de alcance: ADELANTO, ASISTENCIA, INCIDENTE_SEGURIDAD, PLANILLA, PLANILLA_DETALLE y REQUERIMIENTO_DETALLE. CUS36 Registrar Personal tampoco es de esta iteración: el personal, sus áreas, horarios y los usuarios iniciales los cargan `database/15_datos_iteracion1.sql` y `database/17_datos_iteracion2.sql` (la usuaria `almacen`).

## Stack (ADR-03)

Java 21, Spring Boot 4.1, Spring Modulith 2.x, Spring Data JPA, Spring Security, Bean Validation, RestClient para las APIs externas, springdoc-openapi (ADR-04), MapStruct, Lombok (solo `@Getter`/`@Setter`/`@RequiredArgsConstructor`; nada de `@Data` en entidades), ojdbc17; paquete raíz `dev.com.Synapsis_Spa_Cautiva`; Oracle 19c o 26ai Free.

## Entorno

Decisión del 08/11/2026: se trabaja con el JDK 26 (`JAVA_HOME` en `C:\Users\Laptop\.jdks\openjdk-26.0.2.1`; `./mvnw -v` debe mostrar Java 26) y el objetivo sigue siendo Java 21: `pom.xml` mantiene `<java.version>21</java.version>` y Maven compila con `--release 21` (`maven.compiler.release` = 21). No subas `java.version` ni uses APIs posteriores a Java 21. Si una prueba falla solo por el JDK 26, anótalo en `docs/NOTAS_IT2.md` con el error exacto.

## Arquitectura (ADR-01, ADR-02)

Monolito modular con Spring Modulith. Módulos (paquetes de primer nivel): `seguridad`, `sedes`, `catalogo`, `agenda`, `ventas`, `pagos`, `caja`, `personal`, `inventario`, `reportes`, `comun`.

Cuatro capas por módulo, y `api` para lo que publica:

- `controller`: ControladorX (`@RestController`). DTO y Mapper junto al controlador.
- `service`: ServicioX (`@Service`), una transacción por caso de uso.
- `repository`: RepositorioX extends JpaRepository.
- `domain`: entidades JPA y enums.
- `api`: interfaces publicadas (paquete con `@NamedInterface("api")`, para que Spring Modulith lo exponga).

Un módulo nunca usa clases internas de otro: solo sus interfaces publicadas (PersonaApi, AtencionApi, PagosApi, PaqueteClienteApi, SaldoFavorApi, VentasConsultaApi, CatalogoApi, InventarioApi, PersonalApi, RecursoApi), los SPI de `comun` (UsuarioActual, ParametroApi, CajaAbiertaProvider, AuditoriaApi, AlmacenArchivosPort, HistorialComprasProvider, AvisoProvider) o el evento síncrono AdelantoAprobado.

- CajaAbiertaProvider lo implementa `caja` (ServicioCaja).
- `comun.api.HistorialComprasProvider` lo implementa `ventas` y lo usa `agenda` en CUS03. `agenda` NO puede usar VentasConsultaApi: `ventas` ya depende de `agenda` y sería un ciclo.
- `comun.api.AvisoProvider`: cada módulo publica sus avisos y `seguridad` los junta en `GET /avisos` (CUS50).
- Módulo nuevo `reportes`: solo lee las vistas de reporte del script v5 y depende solo de `comun`.
- DF-23 sigue: `personal` no usa PersonaApi (los nombres del personal los pone `seguridad`).

Las APIs externas van por puertos y adaptadores (ADR-09): AdaptadorApisPeruConsulta, AdaptadorApisPeruFacturacion, AdaptadorIzipay.

## Nombres

Lenguaje ubicuo en español, exactamente como en los diagramas (`ServicioCita.agendar`, `Venta.cerrar`, `CajaAbiertaProvider.obtenerCajaAbierta`, `PersonaApi.consultarDocumento`...). Cada clase y método debe existir en el diagrama de clases del diseño de su CUS; si necesitas uno que no está, anótalo en `docs/NOTAS_IT2.md` antes de crearlo.

## Persistencia (ADR-06)

- Entidades 1:1 con las tablas del diccionario v5.1 (snake_case → camelCase, `@Table` y `@Column` con el nombre exacto), PK IDENTITY salvo en los subtipos, `ddl-auto=validate`, enums como String, CHAR(1) S/N con un AttributeConverter a Boolean.
- Herencias:
  - COMPROBANTE → NOTA_CREDITO: `@Inheritance(JOINED)` y `@PrimaryKeyJoinColumn(name = "id_nota_credito")`.
  - PERSONA (seguridad) → CLIENTE (agenda), PERSONAL (personal) y PROVEEDOR (inventario) es solapada y cruza módulos: sin `@Inheritance`; cada subtipo tiene `@Id Long` sin IDENTITY igual al idPersona que devuelve PersonaApi, y los datos comunes solo se leen y escriben por PersonaApi.
- Relaciones JPA solo dentro de los agregados (VENTA→VENTA_DETALLE, VENTA_PAGO, COMPROBANTE; CAJA→CAJA_EGRESO, CAJA_ARQUEO; SERVICIO→VARIANTE_SERVICIO; VARIANTE_SERVICIO→SERVICIO_INSUMO; CLIENTE→SALDO_FAVOR_MOV; ROL→ROL_PERMISO; PERSONAL→PERSONAL_AREA, HORARIO_PERSONAL; PRODUCTO→INVENTARIO_SEDE, MOVIMIENTO_KARDEX; y desde la Iteración 2 ATENCION→RECOMENDACION_PRODUCTO, PAQUETE→PAQUETE_SERVICIO, PROMOCION→PROMOCION_SERVICIO y CONCILIACION→CONCILIACION_ITEM); entre módulos, solo el idX. SOLICITUD_ANULACION (ventas) y CONSUMO_PENDIENTE (inventario) son raíz de su propio agregado y guardan la venta, la nota, la atención y el producto por id.
- `save()` de una raíz que ya existe hace `merge`: Hibernate guarda una copia del hijo nuevo, no el objeto que se agregó. Para leer lo que Oracle completó en el hijo, se lee de la colección del agregado después de guardar (o, en el kardex, con `Producto.ultimoMovimiento`).
- Serie y número del comprobante los asigna Oracle: `insertable = false, updatable = false`.
- `VENTA.id_usuario_autoriza` es obsoleta desde v5.1 (CK_VENTA_AUTORIZA_OBSOLETA): siempre nula. La venta después del cierre se registra reabriendo la caja (CAJA.id_usuario_reapertura, CUS46).
- Las tablas son del dueño del esquema; el back-end nunca se conecta con la cuenta dueña.

## Reglas de negocio (ADR-05)

Se validan en el servicio y Oracle las vuelve a validar; traduce los ORA-20xxx a 409 o 422 con el mensaje de la regla (RNxx). La lista de RN sale de la matriz; en la Iteración 2 se agregan RN48 a RN54 y cambian RN03, RN05, RN17, RN24, RN30, RN32 y RN44.

Reglas nuevas:

- RN48: El rol de la Administradora conserva siempre sus permisos del módulo de seguridad y siempre queda al menos una usuaria activa con ese rol.
- RN49: No se agenda ni se reprograma una cita en una fecha y hora que ya pasaron.
- RN50: Solo la especialista asignada a la cita, o la administradora, inicia y finaliza su atención.
- RN51: Las horas libres de la agenda se ofrecen cada cierto número de minutos, que la administradora define como parámetro del negocio (PARAMETRO AGENDA_PASO_MIN, 15 al inicio).
- RN52: Un cobro digital queda registrado aunque la venta no llegue a cerrarse, y la venta queda abierta hasta que la pasarela apruebe todos sus pagos digitales.
- RN53: Al finalizar una atención, el sistema descuenta del inventario el consumo estimado de los insumos del servicio; si el stock no alcanza, descuenta lo disponible y registra la diferencia como consumo pendiente, que la encargada de almacén regulariza o descarta con motivo.
- RN54: Los insumos se registran en su unidad mínima (ml, g o unidad), para que su consumo estimado se descuente en unidades enteras.

RN48 a RN52 se validan en Java (Oracle no las repite, porque `20_pruebas_reglas_v5.sql` inserta citas en el pasado a propósito). RN53 la protege también Oracle (TRG_KARDEX_APLICAR y TRG_CONSUMO_PENDIENTE_REGLAS del script 17).

Reglas ajustadas (08/11/2026):

- RN03: después de cada servicio, el recurso queda bloqueado por su tiempo de limpieza, de hasta 10 minutos; la especialista queda libre al terminar.
- RN05: al cancelar, también cuando la clienta no se presenta, el adelanto pasa a saldo a favor (CITA no tiene estado NO_ASISTIO: se cancela con el motivo «No se presentó»).
- RN17: solo almacén y la administradora registran movimientos, salvo la salida automática del consumo estimado al finalizar una atención (RN53).
- RN24: solo la dueña ve márgenes, reportes de costos y planilla; la encargada de almacén ve el costo de lo que compra, no el margen.
- RN30: para registrar una venta después del cierre, la administradora reabre la caja del día con un motivo y luego se vuelve a arquear y cerrar.
- RN32: anular requiere una solicitud con motivo; la administradora la aprueba (nota de crédito + venta ANULADA) o la rechaza con su motivo.
- RN44: la tarjeta se devuelve por la pasarela; Yape y Plin desde la aplicación de la administradora (se registra el número de operación); el efectivo desde caja; o saldo a favor si la clienta lo elige.

Lo que Oracle ya HACE no se programa en Java:

- La AUDITORÍA de todo cambio (56 triggers `TRG_AUD_*`, con el usuario de PKG_SESION).
- El BLOQUEO de la cuenta al llegar a LOGIN_INTENTOS_MAX durante LOGIN_BLOQUEO_MIN minutos (TRG_USUARIO_REGLAS: Java solo suma 1 a `intentos_fallidos` y lo pone en 0 al entrar bien).
- La serie y el número de los comprobantes (PKG_VENTA).
- TRG_VENTA_EFECTOS (atenciones COBRADAS y salida de kardex al cerrar; reversa al anular).
- TRG_KARDEX_APLICAR (stock).
- TRG_CITA_CRUCE y TRG_ATENCION_CRUCE (cruces).
- TRG_ATENCION_PAQUETE (paquete AGOTADO).
- TRG_PERSONAL_DESACTIVAR (usuario INACTIVO).
- TRG_CAJA_REGLAS: al reabrir (CERRADA → ABIERTA, solo la Administradora y con motivo) fija `id_usuario_reapertura` y `fecha_reapertura` y limpia el cierre y la revisión.
- TRG_SOLICITUD_ANULACION_REGLAS: fija `id_usuario_solicita`, `fecha_solicitud` y los datos de la resolución; una solicitud resuelta no cambia.
- TRG_CONSUMO_PENDIENTE_REGLAS: fija `fecha_registro` y los datos de la resolución; la regularización exige una SALIDA del mismo producto, sede y atención.

TRG_SALDO_FAVOR_REGLAS rechaza con -20063 un ABONO sin cita fuera de la anulación: Java nunca abona saldo por una venta anulada.

Errores especiales: -20001 (la conexión no llamó a PKG_SESION.iniciar) es un error del back-end (500 y log); -20003 (solo la Administradora) es 403. Códigos nuevos del script 17: -20100 (VENTA.id_usuario_autoriza obsoleta), -20101 (solicitud de una venta que no está CERRADA), -20102 (solicitud que no nace PENDIENTE, cambia de venta o ya está resuelta), -20103 (consumo pendiente que no nace PENDIENTE o ya está resuelto), -20104 (solo almacén o la Administradora resuelven un consumo pendiente), -20105 (regularizar sin la SALIDA correspondiente) y -20106 (solo lo usa el script 17 al comprobar VENTAS:EDITAR). Reabrir la caja sin motivo es -20060 y con otra caja ABIERTA en la sede es ORA-00001 (UQ_CAJA_ABIERTA_SEDE).

## Seguridad (ADR-07, ADR-12)

Sesión de servidor (cookie HttpOnly, SameSite=Strict, CSRF con cookie XSRF-TOKEN para Angular), BCrypt (`$2a$`, costo 10), el bloqueo por intentos lo aplica Oracle, la inactividad que cierra la sesión es SESION_INACTIVIDAD_MIN de PARAMETRO, permisos módulo:acción desde PERMISO.modulo y PERMISO.accion vía ROL_PERMISO, la sede sale de la sesión. El webhook de Izipay se valida con firma HMAC y es idempotente.

Permisos de la Iteración 2:

- `VENTAS:EDITAR` queda solo para la administradora y protege la bandeja de anulación y las devoluciones.
- `CONCILIACION:VER`, `CONCILIACION:CREAR` y `CONCILIACION:EDITAR` protegen aprobar y reabrir caja y conciliar.
- `REPORTES:VER` y `REPORTES:EXPORTAR` protegen los reportes.
- RECEPCIONISTA recibe `CATALOGO:VER` (elige servicios, paquetes y promociones en CUS05, CUS13, CUS18 y CUS21) y `VENTAS:ANULAR` (crea la solicitud de CUS22); los carga el script 17.

## Sesión Oracle (obligatorio, la base lo exige)

En cada transacción de un usuario autenticado el back-end ejecuta primero `BEGIN PKG_SESION.iniciar(:idUsuario); END;` en la misma conexión y, al terminar la transacción (commit o rollback), `BEGIN PKG_SESION.terminar; END;` (un componente en `comun`, por ejemplo con TransactionSynchronization). Sin eso Oracle responde -20001.

Solo el inicio de sesión corre sin iniciar: leer USUARIO por `nombre_usuario` y, si la clave no coincide, sumar 1 a `intentos_fallidos`; si coincide, llamar a iniciar con su id y luego actualizar `ultimo_acceso` e `intentos_fallidos = 0`.

## Configuración (ADR-11, ADR-16, ADR-17)

Se leen de PARAMETRO con estos códigos exactos: IGV_PORCENTAJE, UMBRAL_BOLETA_DOC (S/700), CPE_PLAZO_ENVIO_DIAS, LINK_PAGO_VIGENCIA_HORAS, LOGIN_INTENTOS_MAX, LOGIN_BLOQUEO_MIN, SESION_INACTIVIDAD_MIN, LIMPIEZA_MAX_MIN (10 desde v5.1), AGENDA_PASO_MIN (15, RN51), MARGEN_PRODUCTO_PCT y MARGEN_PREMIUM_PCT; longitud y consulta de documentos de TIPO_DOCUMENTO; nunca constantes. Secretos solo en variables de entorno.

Perfiles en `src/main/resources`: `application.yaml` (común), `application-dev.yaml` (local de ADR-17, por defecto, lee `.env`), `application-sandbox.yaml` (APIs reales en modo de pruebas) y `application-prod.yaml` (produccion de ADR-17).

## Pruebas

Cada CUS tiene al menos una prueba de integración del flujo principal contra Oracle Free en Testcontainers (gvenzl/oracle-free), heredando de `OracleTestcontainer`, que instala `database/` como dice `database/README.md`: los scripts son de SQL*Plus en Windows-1252, se copian al contenedor y se ejecutan con sqlplus (`execInContainer`, `NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252`) en este orden: 00 como APP_USER, 14 como system, 13, 15 y 17 como APP_USER, y `ALTER USER CAUTIVA_APP` como system. Nunca con ScriptUtils ni JDBC (cortan los bloques PL/SQL). Las pruebas se conectan como CAUTIVA_APP con CURRENT_SCHEMA = APP_USER, igual que la aplicación.

Los scripts de prueba de la base terminan en ROLLBACK: `97_prueba_rapida_v5.sql`, `20_pruebas_reglas_v5.sql`, `21_pruebas_v51.sql` (la migración v5.1) y `16_verificar_cuenta_aplicacion.sql` (como CAUTIVA_APP).

`ModularidadTest` verifica los límites entre módulos. `./mvnw verify` no depende de la base local ni de `.env`.

## Notas

Desde la Iteración 2 las dudas y decisiones van en `docs/NOTAS_IT2.md` (secciones Dudas, Diferencias encontradas y Decisiones tomadas). `docs/NOTAS_IT1.md` queda como historia y no se edita.

## Git

Nunca hagas commits, push ni cambios de rama; al final de cada tarea entrega el PLAN DE COMMITS (ver la sección CIERRE de cada prompt).
