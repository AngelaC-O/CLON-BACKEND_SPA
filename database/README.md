# database/ · Cautiva Spa · Oracle v5.2

Scripts de la base de datos del BackOffice. 01 a 15 instalan el modelo v5 (55 tablas, 121 FK, 125 triggers, 5 paquetes, 27 vistas), `17_datos_iteracion2.sql` lo lleva al modelo v5.1 (57 tablas, 131 FK, 129 triggers) y `18_ajustes_v52.sql` al **modelo v5.2** (57 tablas, 132 FK, 129 triggers). Son la fuente de verdad junto con el Diccionario de Datos v5.2 (`docs/Cautiva_Spa_Diccionario_de_Datos_v5_2.xlsx`, hojas «Cambios v5.2» y «Cambios v5.1»). **No se modifican desde el back-end**: si algo no cuadra, se anota en `docs/NOTAS_IT2.md`. La Iteración 2 tocó `database/` una vez (08/11/2026: el script 17, su prueba 21, la cabecera de 00 y este README). La Iteración 2.1 (15/11/2026) agregó 18 (migración v5.1 → v5.2), 19 (horario de prueba, solo local y e2e) y 22 (prueba de 18), y ajustó la cabecera de 00, 99, 20, 21 y este README. Los scripts 01 a 17 describen los modelos v5 y v5.1 y no se editan.

Formato: SQL*Plus (`@@`, `PROMPT`, `SET`), Windows-1252 y CRLF. Se ejecutan con SQL*Plus o SQLcl (en DBeaver: clic derecho en la conexión > Herramientas > *Execute in SQL\*Plus*). El "Ejecutar script" (Alt+X) del editor de DBeaver y `ScriptUtils` de Spring **no sirven**: cortan los bloques PL/SQL.

## Orden de instalación

| Paso | Script | Conectado como | Qué hace |
| --- | --- | --- | --- |
| 1 | `00_instalar_v5.sql` | dueño del esquema (vacío) | Corre 01 a 12: tablas, FK e índices, funciones y triggers de herencia, tipos de documento, paquetes, triggers de reglas y de auditoría, vistas y datos iniciales. Al final imprime errores (debe salir vacío) y conteos. |
| 2 | `14_cuenta_aplicacion.sql` | DBA (SYSTEM) o el dueño si es DBA | Perfil `PRF_CAUTIVA_SERVICIO`, roles `ROL_CAUTIVA_APLICACION` y `ROL_CAUTIVA_REPORTES`, y la cuenta `CAUTIVA_APP` **bloqueada y sin clave conocida**. Se puede repetir. |
| 3 | `13_permisos.sql` | dueño del esquema | Permisos de mínimo privilegio a los dos roles (55 tablas, 27 vistas, 4 paquetes). Se puede repetir. |
| 4 | `15_datos_iteracion1.sql` | dueño del esquema | Personal y usuarios para la Iteración 1 (solo local y pruebas). No hace nada si `admin` ya existe. |
| 5 | `17_datos_iteracion2.sql` | dueño del esquema | **Migración v5 → v5.1** y datos de la Iteración 2: limpieza de hasta 10 minutos (retirada en v5.2), `VENTA.id_usuario_autoriza` obsoleta, reapertura de CAJA, tablas SOLICITUD_ANULACION y CONSUMO_PENDIENTE con sus triggers de reglas y de auditoría, permisos del rol de aplicación sobre ellas, PARAMETRO `AGENDA_PASO_MIN` y `LIMPIEZA_MAX_MIN` (retirado en v5.2), permisos de RECEPCIONISTA y la usuaria `almacen`. Se puede repetir: cada paso revisa el diccionario de Oracle antes de crear. Se detiene si encuentra una VENTA con `id_usuario_autoriza` (no la toca). |
| 6 | `18_ajustes_v52.sql` | dueño del esquema | **Migración v5.1 → v5.2** (Iteración 2.1): `TRANSACCION_PASARELA.id_venta` (llena los cobros ya registrados con la venta de su VENTA_PAGO), sin tiempo de limpieza (`PKG_AGENDA`, `TRG_CITA_REGLAS`, `V_AGENDA_BLOQUES`, `V_OCUPACION_RECURSO`; recalcula `CITA.fecha_hora_fin`; borra `RECURSO.minutos_limpieza` y `LIMPIEZA_MAX_MIN`), `V_VENTA_LINEA` y `V_KARDEX` con columnas nuevas, auditoría de RECURSO, CAJA y TRANSACCION_PASARELA, y las reglas de reapertura, nota de crédito y cobros de una venta. Se puede repetir: cada paso revisa el diccionario de Oracle o PARAMETRO antes de cambiar. Se detiene ante cualquier error, también si un objeto queda con errores de compilación. |
| — | `19_datos_prueba_horario.sql` | dueño del esquema | **Solo base local y e2e, nunca producción.** Agrega `especialista3` y `especialista4` y deja el horario de prueba (ver abajo). Se puede repetir. |
| 7 | (a mano) | DBA o dueño | `ALTER USER CAUTIVA_APP IDENTIFIED BY "<clave>" ACCOUNT UNLOCK;` La clave va solo en la variable de entorno del back-end. |
| 8 | `16_verificar_cuenta_aplicacion.sql` | **CAUTIVA_APP** (con `DEFINE esquema = <dueño>`) | Comprueba lo que usará el back-end: esquema fijado, lecturas, login sin `iniciar`, `-20001` sin `iniciar`, `-20003` para la recepcionista, auditoría con el id del usuario y que la cuenta no puede crear tablas. 11 casos, termina en ROLLBACK. |

Orden completo: 00 → 14 → 13 → 15 → 17 → 18 → 16. En la base local y en la de la e2e, además, 19 después de 18. Los contenedores de las pruebas Java instalan 00, 14, 13, 15, 17 y 18, y no 19.

Pruebas (no dejan datos, terminan en ROLLBACK, se corren como dueño después de 18): `97_prueba_rapida_v5.sql` (14 casos), `20_pruebas_reglas_v5.sql` (144 casos; los de la limpieza pasaron a la regla v5.2, marcados [v5.2]), `21_pruebas_v51.sql` (55 casos de la migración v5.1; sin las comprobaciones de la limpieza, retiradas en v5.2) y `22_pruebas_v52.sql` (44 casos de la migración v5.2). Las cuatro deben dar 0 FALLO. Para reinstalar desde cero: `99_eliminar_v5.sql` (borra las 57 tablas, también SOLICITUD_ANULACION y CONSUMO_PENDIENTE) y volver al paso 1 (los roles y `CAUTIVA_APP` no se borran). `98_crear_usuario_cautiva5.sql` es solo para el Oracle 19c local (crea el esquema de prueba CAUTIVA5).

Resultado esperado del paso 1: 55 tablas; P 55, R 121, U 33, C 168; 97 índices IX_; FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 125, VIEW 27; 5 tipos de documento; ningún error ni objeto inválido.

Resultado esperado del paso 5 (modelo v5.1): 57 tablas; P 57, R 131, U 35, C 183; 105 índices IX_ (97 de v5, `IX_CAJA_USU_REA`, 3 de SOLICITUD_ANULACION y 4 de CONSUMO_PENDIENTE) más el índice único por función `UQ_SOL_ANUL_PENDIENTE`; FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 129 (125 de v5 + `TRG_SOLICITUD_ANULACION_REGLAS`, `TRG_CONSUMO_PENDIENTE_REGLAS`, `TRG_AUD_SOLICITUD_ANULACION` y `TRG_AUD_CONSUMO_PENDIENTE`), VIEW 27; 0 objetos inválidos; `AGENDA_PASO_MIN` 15 y `LIMPIEZA_MAX_MIN` 10 (este último lo borra 18).

Resultado esperado del paso 6 (modelo v5.2): 57 tablas; P 57, R 132 (más `FK_TRANS_PAS_VENTA`), U 35, C 183 (sale `CK_RECURSO_MINUTOS_LIMPIEZA` y entra `CK_TRANS_PAS_VENTA`); 106 índices IX_ (más `IX_TRANS_PAS_VENTA`); FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 129, VIEW 27; 0 objetos inválidos; RECURSO sin `minutos_limpieza`; PARAMETRO sin `LIMPIEZA_MAX_MIN` (`AGENDA_PASO_MIN` sigue en 15). 18 informa cuántos cobros llenó con `id_venta`, cuántos cobros PENDIENTES quedaron sin venta y cuántas citas cambiaron de fin. Al repetirlo no cambia nada.

## Modelo v5.2 (script 18)

Cambios de la hoja «Cambios v5.2» del diccionario (Iteración 2.1). RN03 ajustada: la especialista y el recurso quedan libres en cuanto termina el servicio, sin tiempo de limpieza (RES06: la hacen las especialistas y el sistema no la controla).

| Objeto | Cambio |
| --- | --- |
| RECURSO | Se retiran `minutos_limpieza` y su CHECK. |
| PARAMETRO | Se borra la fila de la limpieza máxima. |
| CITA | `fecha_hora_fin` = inicio + duración; 18 recalcula las citas que no coinciden. |
| TRANSACCION_PASARELA | `id_venta` (`FK_TRANS_PAS_VENTA`, `IX_TRANS_PAS_VENTA`, `CK_TRANS_PAS_VENTA`): la venta del cobro, para listar sus cobros PENDIENTES (RN52). 18 la llena con la venta del VENTA_PAGO de cada cobro. |
| `PKG_AGENDA` | `fin_atencion(inicio, minutos)` reemplaza a la función anterior; `validar_cruce` compara a la especialista y al recurso con el mismo fin. |
| `TRG_CITA_REGLAS` | `fecha_hora_fin := fecha_hora_inicio + duracion_min`. |
| `V_AGENDA_BLOQUES`, `V_OCUPACION_RECURSO` | La columna del fin pasa a llamarse `fin` y es el fin del servicio (o `hora_fin` si la atención terminó). |
| `V_VENTA_LINEA` | Suma al final `id_producto`, `id_variante` e `id_paquete`. |
| `V_KARDEX` | Suma al final `comprobante` (serie-número de la boleta o factura de la venta) y `documento_compra`. |
| `TRG_AUD_RECURSO`, `TRG_AUD_CAJA`, `TRG_AUD_TRANSACCION_PASARELA` | Regenerados con el molde de 10 y las columnas del v5.2 (la reapertura de CAJA y el `id_venta` del cobro quedan en AUDITORIA). |
| `TRG_CAJA_REGLAS` | Solo se reabre la caja del día (`-20060`). |
| `TRG_NOTA_CREDITO_REGLAS` | `id_usuario_solicita` sale de la SOLICITUD_ANULACION PENDIENTE de la venta; sin solicitud, el usuario de la sesión. |
| `TRG_TRANS_PAS_REGLAS` | Un cobro con `id_venta` es de una venta ABIERTA de la misma sede, e `id_venta` no cambia (`-20072`). |
| `TRG_VENTA_PAGO_REGLAS` | Un pago con una transacción cobrada para otra venta: `-20107`. |

Código de error nuevo: `-20107` (pago con la transacción de otra venta, RN42). Se reutilizan `-20060` (reabrir una caja de otro día), `-20072` (cobro de una venta CERRADA o de otra sede, o cambiar `id_venta`) y `-20099` (solo en los scripts: 18 se detiene si un objeto queda con errores de compilación y 19 si falta un usuario de 15 o 17).

## Modelo v5.1 (script 17)

Cambios de la hoja «Cambios v5.1» del diccionario:

| Objeto | Cambio |
| --- | --- |
| RECURSO | `CK_RECURSO_MINUTOS_LIMPIEZA` pasa a `BETWEEN 0 AND 10` y el DEFAULT a 10; los recursos con más de 10 minutos bajan a 10 (RN03). Retirado en v5.2: ya no hay tiempo de limpieza. |
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

Casos de 20 que cambiaron con el v5.2 (marcados [v5.2]): la cita de 10:00 con 30 minutos termina a las 10:30; a las 10:30 la misma especialista en otro sillón y el mismo sillón con otra especialista sí se agendan; la misma especialista vuelve a agendarse a las 10:30 en el mismo sillón (antes, a las 11:00) y la atención de 60 minutos de la cita de las 10:00 choca con esa cita (RN41).

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

Horario de 15: lunes a sábado (dia_semana 1 a 6), 09:00 a 19:00. Los cinco usuarios comparten la clave temporal escrita en la cabecera del script 15 (hash BCrypt `$2a$`, costo 10) y tienen `debe_cambiar_clave = 'S'`. En producción no se usa este script.

## Horario de prueba (script 19, solo base local y e2e)

La sede abre todos los días de 09:00 a 20:00. En la base local y en la de la e2e, 19 deja a cada persona de 09:00 a 20:00 con 60 minutos de almuerzo, todos los días menos su día de descanso (sin fila en HORARIO_PERSONAL; 1 = lunes … 7 = domingo), y agrega dos especialistas con las mismas áreas, el mismo pago y la misma clave temporal:

| Persona | Descansa |
| --- | --- |
| `admin` | domingo (7) |
| `recepcion` | lunes (1) |
| Recepcionista Dos (70000003, sin usuario) | jueves (4) |
| `almacen` | domingo (7) |
| `especialista1` | lunes (1) |
| `especialista3` (70000007, áreas de especialista1) | jueves (4) |
| `especialista2` | martes (2) |
| `especialista4` (70000008, áreas de especialista2) | viernes (5) |

Al final comprueba que cada día haya al menos una especialista activa por cada área de PERSONAL_AREA y muestra la tabla de horarios. Las pruebas Java no lo usan: siguen con el horario de 15.

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
sqlplus -s APP_USER/APP_USER_PASSWORD@//localhost:1521/FREEPDB1 @/database/18_ajustes_v52.sql
# y como system: ALTER USER CAUTIVA_APP IDENTIFIED BY "<clave de la prueba>" ACCOUNT UNLOCK;
```

Cada `sqlplus` debe terminar con `EXIT`: use un script envoltorio que haga `@archivo` y luego `EXIT`, o pase las líneas por la entrada estándar. Comprobar en la salida que no haya `ORA-`, `PLS-` ni `SP2-`.

## Ambiente local (Docker, 01/11/2026)

`localhost:1522`, servicio `FREEPDB1` (Oracle AI Database 26ai Free), dueño `SYNAPSIS_SPA_CAUTIVA`. Pasos 1 a 6 hechos el 01-02/11/2026: 0 errores, 0 inválidos, pruebas 14 OK y 144 OK, cuenta `CAUTIVA_APP` desbloqueada y verificada (11 OK). Falta correr ahí `17_datos_iteracion2.sql` (paso 5 del orden nuevo) para llevarla al v5.1; en esta tarea solo se corrió en un contenedor limpio de pruebas.

En la PC, la clave local de `CAUTIVA_APP` está en `.local/clave_cautiva_app.sql` del repositorio del back-end (carpeta ignorada por git); `.local/verificar_cautiva_app.sql` la aplica y corre el paso 6 desde DBeaver (*Execute in SQL\*Plus*). El back-end lee la conexión de `.env` (también ignorado por git; plantilla en `.env.example`) con `spring.config.import=optional:file:.env[.properties]`.

Nota para Oracle 23ai/26ai: no escribir un comentario `--` después del `;` en la misma línea de una sentencia SQL (da `ORA-03405`); en 19c sí funcionaba.
