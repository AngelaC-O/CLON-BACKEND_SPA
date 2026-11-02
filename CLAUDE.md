# Back-end del BackOffice web de Cautiva Spa E.I.R.L. (CS-BO-01) · Iteración 1

## Fuentes de verdad (en este orden)

1. `docs/Cautiva_Spa_Diccionario_de_Datos_v5.xlsx`: tablas, columnas, tipos, PK, FK, CHECK, jerarquías y trazabilidad CUS.
2. `database/`: el script real (triggers, paquetes PL/SQL y vistas).
3. `docs/Iteracion_1_Cautiva_Spa.drawio`: una página por CUS con secuencia, clases del análisis y clases del diseño; páginas generales y modelos de BD.
4. `docs/U2.md`: contratos REST, patrones, eventos.

Prohibido inventar tablas, columnas, actores, endpoints o módulos. Si algo falta o se contradice, no lo resuelvas por tu cuenta: anótalo en `docs/NOTAS_IT1.md` y pregunta.

`docs/` y `database/` no se modifican, salvo `docs/NOTAS_IT1.md`. Nunca leas ni copies `.local/`.

## Alcance

Solo estos 20 CUS:

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

Usa 44 de las 55 tablas (página «BD · Modelo lógico-relacional · It. 1»). CUS36 Registrar Personal no es de esta iteración: el personal, sus áreas, horarios y los usuarios iniciales los carga `database/15_datos_iteracion1.sql`.

## Stack (ADR-03)

Java 21, Spring Boot 4.1, Spring Modulith 2.x, Spring Data JPA, Spring Security, Bean Validation, RestClient para las APIs externas, springdoc-openapi (ADR-04), MapStruct, Lombok (solo `@Getter`/`@Setter`/`@RequiredArgsConstructor`; nada de `@Data` en entidades), ojdbc17; paquete raíz `dev.com.Synapsis_Spa_Cautiva`; Oracle 19c o 26ai Free.

## Arquitectura (ADR-01, ADR-02)

Monolito modular con Spring Modulith. Módulos (paquetes de primer nivel): `seguridad`, `sedes`, `catalogo`, `agenda`, `ventas`, `pagos`, `caja`, `personal`, `inventario`, `comun`.

Cuatro capas por módulo, y `api` para lo que publica:

- `controller`: ControladorX (`@RestController`). DTO y Mapper junto al controlador.
- `service`: ServicioX (`@Service`), una transacción por caso de uso.
- `repository`: RepositorioX extends JpaRepository.
- `domain`: entidades JPA y enums.
- `api`: interfaces publicadas (paquete con `@NamedInterface("api")`, para que Spring Modulith lo exponga).

Un módulo nunca usa clases internas de otro: solo sus interfaces publicadas (PersonaApi, AtencionApi, CajaAbiertaProvider, PagosApi, PaqueteClienteApi, SaldoFavorApi, VentasConsultaApi, CatalogoApi, InventarioApi, PersonalApi, RecursoApi), los SPI de `comun` (UsuarioActual, ParametroApi, AuditoriaApi, AlmacenArchivosPort) o el evento síncrono AdelantoAprobado.

Las APIs externas van por puertos y adaptadores (ADR-09): AdaptadorApisPeruConsulta, AdaptadorApisPeruFacturacion, AdaptadorIzipay.

## Nombres

Lenguaje ubicuo en español, exactamente como en los diagramas (`ServicioCita.agendar`, `Venta.cerrar`, `CajaAbiertaProvider.obtenerCajaAbierta`, `PersonaApi.consultarDocumento`...). Cada clase y método debe existir en el diagrama de clases del diseño de su CUS; si necesitas uno que no está, anótalo en `docs/NOTAS_IT1.md` antes de crearlo.

## Persistencia (ADR-06)

- Entidades 1:1 con las tablas del diccionario v5 (snake_case → camelCase, `@Table` y `@Column` con el nombre exacto), PK IDENTITY salvo en los subtipos, `ddl-auto=validate`, enums como String, CHAR(1) S/N con un AttributeConverter a Boolean.
- Herencias:
  - COMPROBANTE → NOTA_CREDITO: `@Inheritance(JOINED)` y `@PrimaryKeyJoinColumn(name = "id_nota_credito")`.
  - PERSONA (seguridad) → CLIENTE (agenda), PERSONAL (personal) y PROVEEDOR (inventario) es solapada y cruza módulos: sin `@Inheritance`; cada subtipo tiene `@Id Long` sin IDENTITY igual al idPersona que devuelve PersonaApi, y los datos comunes solo se leen y escriben por PersonaApi.
- Relaciones JPA solo dentro de los agregados (VENTA→VENTA_DETALLE, VENTA_PAGO, COMPROBANTE; CAJA→CAJA_EGRESO, CAJA_ARQUEO; SERVICIO→VARIANTE_SERVICIO; VARIANTE_SERVICIO→SERVICIO_INSUMO; CLIENTE→SALDO_FAVOR_MOV; ROL→ROL_PERMISO; PERSONAL→PERSONAL_AREA, HORARIO_PERSONAL; PRODUCTO→INVENTARIO_SEDE, MOVIMIENTO_KARDEX); entre módulos, solo el idX.
- Serie y número del comprobante los asigna Oracle: `insertable = false, updatable = false`.
- Las tablas son del dueño del esquema; el back-end nunca se conecta con la cuenta dueña.

## Reglas de negocio (ADR-05)

Se validan en el servicio y Oracle las vuelve a validar; traduce los ORA-20xxx a 409 o 422 con el mensaje de la regla (RNxx).

Lo que Oracle ya HACE no se programa en Java:

- La AUDITORÍA de todo cambio (54 triggers `TRG_AUD_*`, con el usuario de PKG_SESION).
- El BLOQUEO de la cuenta al llegar a LOGIN_INTENTOS_MAX durante LOGIN_BLOQUEO_MIN minutos (TRG_USUARIO_REGLAS: Java solo suma 1 a `intentos_fallidos` y lo pone en 0 al entrar bien).
- La serie y el número de los comprobantes (PKG_VENTA).
- TRG_VENTA_EFECTOS (atenciones COBRADAS y salida de kardex al cerrar; reversa al anular).
- TRG_KARDEX_APLICAR (stock).
- TRG_CITA_CRUCE y TRG_ATENCION_CRUCE (cruces).
- TRG_ATENCION_PAQUETE (paquete AGOTADO).
- TRG_PERSONAL_DESACTIVAR (usuario INACTIVO).

TRG_SALDO_FAVOR_REGLAS rechaza con -20063 un ABONO sin cita fuera de la anulación: Java nunca abona saldo por una venta anulada.

Errores especiales: -20001 (la conexión no llamó a PKG_SESION.iniciar) es un error del back-end (500 y log); -20003 (solo la Administradora) es 403.

## Seguridad (ADR-07, ADR-12)

Sesión de servidor (cookie HttpOnly, SameSite=Strict, CSRF con cookie XSRF-TOKEN para Angular), BCrypt (`$2a$`, costo 10), el bloqueo por intentos lo aplica Oracle, la inactividad que cierra la sesión es SESION_INACTIVIDAD_MIN de PARAMETRO, permisos módulo:acción desde PERMISO.modulo y PERMISO.accion vía ROL_PERMISO, la sede sale de la sesión. El webhook de Izipay se valida con firma HMAC y es idempotente.

## Sesión Oracle (obligatorio, la base lo exige)

En cada transacción de un usuario autenticado el back-end ejecuta primero `BEGIN PKG_SESION.iniciar(:idUsuario); END;` en la misma conexión y, al terminar la transacción (commit o rollback), `BEGIN PKG_SESION.terminar; END;` (un componente en `comun`, por ejemplo con TransactionSynchronization). Sin eso Oracle responde -20001.

Solo el inicio de sesión corre sin iniciar: leer USUARIO por `nombre_usuario` y, si la clave no coincide, sumar 1 a `intentos_fallidos`; si coincide, llamar a iniciar con su id y luego actualizar `ultimo_acceso` e `intentos_fallidos = 0`.

## Configuración (ADR-11, ADR-16, ADR-17)

Se leen de PARAMETRO con estos códigos exactos: IGV_PORCENTAJE, UMBRAL_BOLETA_DOC (S/700), CPE_PLAZO_ENVIO_DIAS, LINK_PAGO_VIGENCIA_HORAS, LOGIN_INTENTOS_MAX, LOGIN_BLOQUEO_MIN, SESION_INACTIVIDAD_MIN, LIMPIEZA_MAX_MIN; longitud y consulta de documentos de TIPO_DOCUMENTO; nunca constantes. Secretos solo en variables de entorno.

Perfiles en `src/main/resources`: `application.yaml` (común), `application-dev.yaml` (local de ADR-17, por defecto, lee `.env`) y `application-prod.yaml` (produccion de ADR-17). El sandbox se agrega en B5 como `application-sandbox.yaml`.

## Pruebas

Cada CUS tiene al menos una prueba de integración del flujo principal contra Oracle Free en Testcontainers (gvenzl/oracle-free), heredando de `OracleTestcontainer`, que instala `database/` como dice `database/README.md`: los scripts son de SQL*Plus en Windows-1252, se copian al contenedor y se ejecutan con sqlplus (`execInContainer`, `NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252`) en este orden: 00 como APP_USER, 14 como system, 13 y 15 como APP_USER, y `ALTER USER CAUTIVA_APP` como system. Nunca con ScriptUtils ni JDBC (cortan los bloques PL/SQL). Las pruebas se conectan como CAUTIVA_APP con CURRENT_SCHEMA = APP_USER, igual que la aplicación.

`ModularidadTest` verifica los límites entre módulos. `./mvnw verify` no depende de la base local ni de `.env`.

## Git

Nunca hagas commits, push ni cambios de rama; al final de cada tarea entrega el PLAN DE COMMITS (ver la sección CIERRE de cada prompt).
