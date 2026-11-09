# database/ · Cautiva Spa · Oracle v5.1

Scripts de la base de datos del BackOffice. 01 a 15 instalan el modelo v5 (55 tablas, 121 FK, 125 triggers, 5 paquetes, 27 vistas) y `17_datos_iteracion2.sql` lo lleva al **modelo v5.1** (57 tablas, 131 FK, 129 triggers). Son la fuente de verdad junto con el Diccionario de Datos v5.1 (`docs/Cautiva_Spa_Diccionario_de_Datos_v5_1.xlsx`, hoja «Cambios v5.1»). **No se modifican desde el back-end**: si algo no cuadra, se anota en `docs/NOTAS_IT2.md`. La Iteración 2 tocó `database/` una sola vez (08/11/2026): el script 17, su prueba 21, la cabecera de 00 y este README; 17 es la única migración y los scripts 01 a 15 no se editaron.

Formato: SQL*Plus (`@@`, `PROMPT`, `SET`), Windows-1252 y CRLF. Se ejecutan con SQL*Plus o SQLcl (en DBeaver: clic derecho en la conexión > Herramientas > *Execute in SQL\*Plus*). El "Ejecutar script" (Alt+X) del editor de DBeaver y `ScriptUtils` de Spring **no sirven**: cortan los bloques PL/SQL.

## Orden de instalación

| Paso | Script | Conectado como | Qué hace |
| --- | --- | --- | --- |
| 1 | `00_instalar_v5.sql` | dueño del esquema (vacío) | Corre 01 a 12: tablas, FK e índices, funciones y triggers de herencia, tipos de documento, paquetes, triggers de reglas y de auditoría, vistas y datos iniciales. Al final imprime errores (debe salir vacío) y conteos. |
| 2 | `14_cuenta_aplicacion.sql` | DBA (SYSTEM) o el dueño si es DBA | Perfil `PRF_CAUTIVA_SERVICIO`, roles `ROL_CAUTIVA_APLICACION` y `ROL_CAUTIVA_REPORTES`, y la cuenta `CAUTIVA_APP` **bloqueada y sin clave conocida**. Se puede repetir. |
| 3 | `13_permisos.sql` | dueño del esquema | Permisos de mínimo privilegio a los dos roles (55 tablas, 27 vistas, 4 paquetes). Se puede repetir. |
| 4 | `15_datos_iteracion1.sql` | dueño del esquema | Personal y usuarios para la Iteración 1 (solo local y pruebas). No hace nada si `admin` ya existe. |
| 5 | `17_datos_iteracion2.sql` | dueño del esquema | **Migración v5 → v5.1** y datos de la Iteración 2: limpieza de hasta 10 minutos, `VENTA.id_usuario_autoriza` obsoleta, reapertura de CAJA, tablas SOLICITUD_ANULACION y CONSUMO_PENDIENTE con sus triggers de reglas y de auditoría, permisos del rol de aplicación sobre ellas, PARAMETRO `AGENDA_PASO_MIN` y `LIMPIEZA_MAX_MIN`, permisos de RECEPCIONISTA y la usuaria `almacen`. Se puede repetir: cada paso revisa el diccionario de Oracle antes de crear. Se detiene si encuentra una VENTA con `id_usuario_autoriza` (no la toca). |
| 6 | (a mano) | DBA o dueño | `ALTER USER CAUTIVA_APP IDENTIFIED BY "<clave>" ACCOUNT UNLOCK;` La clave va solo en la variable de entorno del back-end. |
| 7 | `16_verificar_cuenta_aplicacion.sql` | **CAUTIVA_APP** (con `DEFINE esquema = <dueño>`) | Comprueba lo que usará el back-end: esquema fijado, lecturas, login sin `iniciar`, `-20001` sin `iniciar`, `-20003` para la recepcionista, auditoría con el id del usuario y que la cuenta no puede crear tablas. 11 casos, termina en ROLLBACK. |

Orden completo: 00 → 14 → 13 → 15 → 17 → 16.

Pruebas (no dejan datos, terminan en ROLLBACK, se corren como dueño después de 17): `97_prueba_rapida_v5.sql` (14 casos), `20_pruebas_reglas_v5.sql` (144 casos; con el v5.1 dan 142 OK y 2 FALLO esperados, ver abajo) y `21_pruebas_v51.sql` (58 casos de la migración v5.1). Para reinstalar desde cero: `99_eliminar_v5.sql` y volver al paso 1 (los roles y `CAUTIVA_APP` no se borran). 99 solo conoce las 55 tablas de v5: antes de correrlo, borre a mano las dos de v5.1 (`DROP TABLE SOLICITUD_ANULACION CASCADE CONSTRAINTS PURGE;` y `DROP TABLE CONSUMO_PENDIENTE CASCADE CONSTRAINTS PURGE;`). `98_crear_usuario_cautiva5.sql` es solo para el Oracle 19c local (crea el esquema de prueba CAUTIVA5).

Resultado esperado del paso 1: 55 tablas; P 55, R 121, U 33, C 168; 97 índices IX_; FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 125, VIEW 27; 5 tipos de documento; ningún error ni objeto inválido.

Resultado esperado del paso 5 (modelo v5.1): 57 tablas; P 57, R 131, U 35, C 183; 105 índices IX_ (97 de v5, `IX_CAJA_USU_REA`, 3 de SOLICITUD_ANULACION y 4 de CONSUMO_PENDIENTE) más el índice único por función `UQ_SOL_ANUL_PENDIENTE`; FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 129 (125 de v5 + `TRG_SOLICITUD_ANULACION_REGLAS`, `TRG_CONSUMO_PENDIENTE_REGLAS`, `TRG_AUD_SOLICITUD_ANULACION` y `TRG_AUD_CONSUMO_PENDIENTE`), VIEW 27; 0 objetos inválidos; `AGENDA_PASO_MIN` 15 y `LIMPIEZA_MAX_MIN` 10.

## Modelo v5.1 (script 17)

Cambios de la hoja «Cambios v5.1» del diccionario:

| Objeto | Cambio |
| --- | --- |
| RECURSO | `CK_RECURSO_MINUTOS_LIMPIEZA` pasa a `BETWEEN 0 AND 10` y el DEFAULT a 10; los recursos con más de 10 minutos bajan a 10 (RN03). |
| VENTA | `CK_VENTA_AUTORIZA_OBSOLETA` (`id_usuario_autoriza IS NULL`). La columna se queda, como `COMPRA.id_solicitud` (O-04). |
| CAJA | `id_usuario_reapertura`, `fecha_reapertura` y `motivo_reapertura`, con `FK_CAJA_USUARIO_REAPERTURA` y `CK_CAJA_REAPERTURA` (RN30, RF66). |
| SOLICITUD_ANULACION | Tabla nueva (RN32): una sola PENDIENTE por venta (`UQ_SOL_ANUL_PENDIENTE`). |
| CONSUMO_PENDIENTE | Tabla nueva (RN53): lo que faltó descontar del consumo estimado de una atención. |
| `TRG_CAJA_REGLAS` | CERRADA → ABIERTA solo para la Administradora y con motivo; fija quién y cuándo reabrió y limpia el cierre y la revisión. Al volver a cerrar, el arqueo tiene que estar al día. |
| `TRG_KARDEX_APLICAR` | RN17 admite la SALIDA con `id_atencion` que registra la especialista de esa atención (consumo automático). |
| `TRG_VENTA_REGLAS` | Rechaza `id_usuario_autoriza` y solo deja entrar una venta a una caja ABIERTA. |
| `TRG_SOLICITUD_ANULACION_REGLAS` | La venta debe estar CERRADA; solo la Administradora aprueba o rechaza; resuelta no cambia. |
| `TRG_CONSUMO_PENDIENTE_REGLAS` | Solo almacén o la Administradora lo resuelven; resuelto no cambia; regularizar exige una SALIDA del mismo producto, sede y atención. |

Códigos de error nuevos (el mayor usado en 06, 08 y 09 era `-20099`):

| Código | Regla |
| --- | --- |
| `-20100` | `VENTA.id_usuario_autoriza` es obsoleta (también detiene el paso previo de 17 si hay ventas con ese dato). |
| `-20101` | La solicitud de anulación es de una venta CERRADA (o la venta no existe). |
| `-20102` | Una solicitud nace PENDIENTE, no cambia de venta y, resuelta, no cambia. |
| `-20103` | Un consumo pendiente nace PENDIENTE y, resuelto, no cambia. |
| `-20104` | Solo la encargada de almacén o la Administradora regularizan o descartan un consumo pendiente. |
| `-20105` | Regularizar exige una SALIDA de kardex del mismo insumo, sede y atención. |
| `-20106` | Solo lo usa 17 al comprobar que `VENTAS:EDITAR` siga solo en ADMINISTRADORA. |

Se reutilizan `-20003` (reabrir la caja y resolver una solicitud son de la Administradora), `-20031` (RN17), `-20042` (la venta entra a una caja ABIERTA de su sede) y `-20060` (reabrir sin motivo o cambiar los datos de la reapertura). Reabrir con otra caja ABIERTA en la sede lo rechaza el índice `UQ_CAJA_ABIERTA_SEDE` (`ORA-00001`).

Casos de 20 que cambian con el v5.1 (no se editan): «El fin incluye la limpieza: 10:00 + 30 + 30 = 11:00» y «Mismo sillón a las 10:45 con otra especialista (aún en limpieza)». Los recursos de prueba toman el DEFAULT de `minutos_limpieza`, que ahora es 10: la cita termina a las 10:40 y a las 10:45 el sillón ya está libre (RN03).

## Personal de la Iteración 1 (script 15)

CUS36 Registrar Personal no es de esta iteración, pero USUARIO, CITA y ATENCION exigen un PERSONAL, y agendar o atender exige el área de la especialista en PERSONAL_AREA (`-20012`). Por eso el script 15 deja, en la sede «Pichanaki Centro»:

| Usuario | Persona (DNI de prueba) | Rol | Áreas habilitadas |
| --- | --- | --- | --- |
| `admin` | Administradora Cautiva Spa (70000001) | ADMINISTRADORA | — |
| `recepcion` | Recepcionista Uno (70000002) | RECEPCIONISTA | — |
| (sin usuario) | Recepcionista Dos (70000003) | — | — para probar CUS52 Registrar Usuario |
| `especialista1` | Especialista Salón (70000004) | ESPECIALISTA_TECNICA | Peluquería y Estilismo, Manicura y Pedicura |
| `especialista2` | Especialista Spa (70000005) | ESPECIALISTA_TECNICA | Cuidado Facial, Cabina Spa, Depilación |
| `almacen` | Encargada Almacén (70000006) | ENCARGADA_ALMACEN | — (la agrega 17, Iteración 2) |

Horario: lunes a sábado (dia_semana 1 a 6), 09:00 a 19:00. Los cinco usuarios comparten la clave temporal escrita en la cabecera del script 15 (hash BCrypt `$2a$`, costo 10) y tienen `debe_cambiar_clave = 'S'`. En producción no se usa este script.

## Cómo debe conectarse el back-end

1. **Cuenta `CAUTIVA_APP`**, nunca la del dueño. `PKG_SESION` reconoce al back-end por esa cuenta; con la del dueño Oracle lo trata como mantenimiento y no exige ser Administradora ni audita al usuario.
2. **Esquema**: las tablas son del dueño. Al abrir cada conexión: `ALTER SESSION SET CURRENT_SCHEMA = <dueño>` (Hikari `connection-init-sql`), y `hibernate.default_schema = <dueño>`.
3. **Usuario de la operación**: en cada transacción de un usuario autenticado, primero `BEGIN PKG_SESION.iniciar(:id_usuario); END;` y al terminar `BEGIN PKG_SESION.terminar; END;`. Sin eso Oracle responde `-20001` («El backend debe llamar a PKG_SESION.iniciar»). Lo único que se hace sin `iniciar` es el inicio de sesión: leer USUARIO por `nombre_usuario` y sumar 1 a `intentos_fallidos` si la clave no coincide.
4. **Lo que ya hace Oracle (no se programa en Java)**: auditoría de todo cambio (54 triggers `TRG_AUD_*`, con el usuario de `PKG_SESION`); bloqueo de la cuenta al llegar a `LOGIN_INTENTOS_MAX` por `LOGIN_BLOQUEO_MIN` minutos; serie y número de comprobantes (`PKG_VENTA`); efectos de cerrar o anular una venta (`TRG_VENTA_EFECTOS`); stock por kardex (`TRG_KARDEX_APLICAR`); cruces de agenda (`TRG_CITA_CRUCE`, `TRG_ATENCION_CRUCE`); abonos de saldo a favor por venta anulada (`TRG_SALDO_FAVOR_REGLAS` rechaza con `-20063` cualquier otro). [v5.1] Quién y cuándo reabre la caja (`TRG_CAJA_REGLAS`), pide o resuelve una solicitud de anulación (`TRG_SOLICITUD_ANULACION_REGLAS`) y resuelve un consumo pendiente (`TRG_CONSUMO_PENDIENTE_REGLAS`); auditoría de las dos tablas nuevas (56 triggers `TRG_AUD_*`).

## Testcontainers (gvenzl/oracle-free)

Los scripts no se pueden correr por JDBC. En la prueba: copiar `database/` al contenedor (`withCopyFileToContainer`) y ejecutar con `execInContainer`, con `NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252` (los archivos están en Windows-1252):

```bash
# dueño = APP_USER del contenedor (lo crea con CONNECT, RESOURCE y CREATE VIEW y cuota en USERS)
sqlplus -s APP_USER/APP_USER_PASSWORD@//localhost:1521/FREEPDB1 @/database/00_instalar_v5.sql
sqlplus -s system/ORACLE_PASSWORD@//localhost:1521/FREEPDB1 @/database/14_cuenta_aplicacion.sql
sqlplus -s APP_USER/APP_USER_PASSWORD@//localhost:1521/FREEPDB1 @/database/13_permisos.sql
sqlplus -s APP_USER/APP_USER_PASSWORD@//localhost:1521/FREEPDB1 @/database/15_datos_iteracion1.sql
sqlplus -s APP_USER/APP_USER_PASSWORD@//localhost:1521/FREEPDB1 @/database/17_datos_iteracion2.sql
# y como system: ALTER USER CAUTIVA_APP IDENTIFIED BY "<clave de la prueba>" ACCOUNT UNLOCK;
```

Cada `sqlplus` debe terminar con `EXIT`: use un script envoltorio que haga `@archivo` y luego `EXIT`, o pase las líneas por la entrada estándar. Comprobar en la salida que no haya `ORA-`, `PLS-` ni `SP2-`.

## Ambiente local (Docker, 01/11/2026)

`localhost:1522`, servicio `FREEPDB1` (Oracle AI Database 26ai Free), dueño `SYNAPSIS_SPA_CAUTIVA`. Pasos 1 a 6 hechos el 01-02/11/2026: 0 errores, 0 inválidos, pruebas 14 OK y 144 OK, cuenta `CAUTIVA_APP` desbloqueada y verificada (11 OK). Falta correr ahí `17_datos_iteracion2.sql` (paso 5 del orden nuevo) para llevarla al v5.1; en esta tarea solo se corrió en un contenedor limpio de pruebas.

En la PC, la clave local de `CAUTIVA_APP` está en `.local/clave_cautiva_app.sql` del repositorio del back-end (carpeta ignorada por git); `.local/verificar_cautiva_app.sql` la aplica y corre el paso 6 desde DBeaver (*Execute in SQL\*Plus*). El back-end lee la conexión de `.env` (también ignorado por git; plantilla en `.env.example`) con `spring.config.import=optional:file:.env[.properties]`.

Nota para Oracle 23ai/26ai: no escribir un comentario `--` después del `;` en la misma línea de una sentencia SQL (da `ORA-03405`); en 19c sí funcionaba.
