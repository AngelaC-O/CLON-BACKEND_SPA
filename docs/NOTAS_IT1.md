# Notas de la Iteración 1 · Back-end

## Dudas

- **D-01. ADR que no están en el repositorio.** Las reglas citan ADR-01 a ADR-17, pero `docs/U2.md` y `docs/Iteracion_1_Cautiva_Spa.drawio` no contienen ninguno. Solo se usan a través de lo escrito en `CLAUDE.md`. ¿Se agrega el documento de la Unidad 1 a `docs/`?
- **D-02. Otro drawio en docs/.** `docs/Diagramas_secuencia_clase_ADS.drawio` existe, pero no está entre las fuentes de verdad. ¿Se puede consultar o se ignora? Por ahora se ignora.
- **D-03. Dónde va `CajaAbiertaProvider`.** Ver la diferencia DF-02. Hay que confirmarlo antes de crear la interfaz.

## Diferencias encontradas

- **O-20 (Unidad 2, abierta y opcional).** `ConsultaDocumentoService` + `ConsultaDocumentoApi` del C3 de la Unidad 1 equivalen a `ServicioPersona` + `PersonaApi` de la Unidad 2 (módulo `seguridad`). Se usan los nombres de la Unidad 2.
- **DF-01. Módulo `reportes`.** `docs/U2.md` §1.5 lista `reportes` (CUS48, CUS49; 0 tablas), además de los 10 módulos pedidos, y dice «los 10 módulos» con 11 filas. No se creó: sus CUS no son de la Iteración 1.
- **DF-02. `CajaAbiertaProvider`.** Las reglas lo listan entre las interfaces publicadas por los módulos y separado de los SPI de `comun`. `docs/U2.md` (§1.5, §1.8, §2.2 y §5.1) lo define como SPI en `comun`, implementado por `caja`. No se creó ninguna interfaz todavía.
- **DF-03. Permisos de `PKG_VENTA`.** `13_permisos.sql` da EXECUTE a la aplicación sobre 4 de los 5 paquetes (PKG_SESION, PKG_AGENDA, PKG_INVENTARIO, PKG_CAJA). PKG_VENTA queda fuera: lo llaman los triggers, así que Java no puede invocarlo directamente. Es coherente con `database/README.md` («4 paquetes»).
- **DF-04. Cabecera de `00_instalar_v5.sql`.** Dice que 13 es opcional y que los roles los crea el «script 01 de v4». El orden vigente es el de `database/README.md`: 14 crea los roles y 13 es obligatorio para el back-end.

## Decisiones tomadas

- **Ambientes de ADR-17.** `local` = perfil `dev` y `produccion` = perfil `prod`. El ambiente `sandbox` (APIs reales en modo de pruebas) se agrega en B5 como `application-sandbox.yaml`.
- **Sin Flyway.** La base se instala con SQL*Plus desde `database/`, y CAUTIVA_APP no puede crear la tabla de historial de Flyway.
- **Paquetes con `package-info.java`.** Java y git no conservan paquetes vacíos, así que cada módulo y capa tiene su `package-info.java`. La raíz del módulo lleva `@ApplicationModule` y `api` lleva `@NamedInterface("api")`: sin esto, Spring Modulith trataría `api` como interno y otro módulo no podría usarlo.
- **Selector de adaptadores.** Los perfiles declaran `cautiva.integraciones.apisperu.modo` y `cautiva.integraciones.izipay.modo` con `falso` o `real`. Hoy nadie los lee; los adaptadores, su URL y sus credenciales (por variables de entorno) llegan en B2 y B4. El nombre de la propiedad no sale de los diagramas y se puede ajustar entonces.
- **Versiones fijadas en el pom.** Spring Modulith 2.1.2, springdoc 3.2.0, MapStruct 1.6.3, lombok-mapstruct-binding 0.2.0 y WireMock 3.13.2 (`wiremock-standalone`). Testcontainers 2.0.5 viene de Spring Boot, y en esa versión sus artefactos se llaman `testcontainers-oracle-free` y `testcontainers-junit-jupiter`.
- **Imagen de las pruebas.** `gvenzl/oracle-free:latest`, la misma del Oracle local (26ai Free).
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
