# database/ · Cautiva Spa · Oracle v5

Scripts de la base de datos del BackOffice (modelo v5: 55 tablas, 121 FK, 125 triggers, 5 paquetes, 27 vistas). Son la fuente de verdad junto con el Diccionario de Datos v5. **No se modifican desde el back-end**: si algo no cuadra, se anota en `docs/NOTAS_IT1.md`.

Formato: SQL*Plus (`@@`, `PROMPT`, `SET`), Windows-1252 y CRLF. Se ejecutan con SQL*Plus o SQLcl (en DBeaver: clic derecho en la conexión > Herramientas > *Execute in SQL\*Plus*). El "Ejecutar script" (Alt+X) del editor de DBeaver y `ScriptUtils` de Spring **no sirven**: cortan los bloques PL/SQL.

## Orden de instalación

| Paso | Script | Conectado como | Qué hace |
| --- | --- | --- | --- |
| 1 | `00_instalar_v5.sql` | dueño del esquema (vacío) | Corre 01 a 12: tablas, FK e índices, funciones y triggers de herencia, tipos de documento, paquetes, triggers de reglas y de auditoría, vistas y datos iniciales. Al final imprime errores (debe salir vacío) y conteos. |
| 2 | `14_cuenta_aplicacion.sql` | DBA (SYSTEM) o el dueño si es DBA | Perfil `PRF_CAUTIVA_SERVICIO`, roles `ROL_CAUTIVA_APLICACION` y `ROL_CAUTIVA_REPORTES`, y la cuenta `CAUTIVA_APP` **bloqueada y sin clave conocida**. Se puede repetir. |
| 3 | `13_permisos.sql` | dueño del esquema | Permisos de mínimo privilegio a los dos roles (55 tablas, 27 vistas, 4 paquetes). Se puede repetir. |
| 4 | `15_datos_iteracion1.sql` | dueño del esquema | Personal y usuarios para la Iteración 1 (solo local y pruebas). No hace nada si `admin` ya existe. |
| 5 | (a mano) | DBA o dueño | `ALTER USER CAUTIVA_APP IDENTIFIED BY "<clave>" ACCOUNT UNLOCK;` La clave va solo en la variable de entorno del back-end. |
| 6 | `16_verificar_cuenta_aplicacion.sql` | **CAUTIVA_APP** (con `DEFINE esquema = <dueño>`) | Comprueba lo que usará el back-end: esquema fijado, lecturas, login sin `iniciar`, `-20001` sin `iniciar`, `-20003` para la recepcionista, auditoría con el id del usuario y que la cuenta no puede crear tablas. 11 casos, termina en ROLLBACK. |

Pruebas (no dejan datos, terminan en ROLLBACK): `97_prueba_rapida_v5.sql` (14 casos) y `20_pruebas_reglas_v5.sql` (144 casos). Para reinstalar desde cero: `99_eliminar_v5.sql` y volver al paso 1 (los roles y `CAUTIVA_APP` no se borran). `98_crear_usuario_cautiva5.sql` es solo para el Oracle 19c local (crea el esquema de prueba CAUTIVA5).

Resultado esperado del paso 1: 55 tablas; P 55, R 121, U 33, C 168; 97 índices IX_; FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 125, VIEW 27; 5 tipos de documento; ningún error ni objeto inválido.

## Personal de la Iteración 1 (script 15)

CUS36 Registrar Personal no es de esta iteración, pero USUARIO, CITA y ATENCION exigen un PERSONAL, y agendar o atender exige el área de la especialista en PERSONAL_AREA (`-20012`). Por eso el script 15 deja, en la sede «Pichanaki Centro»:

| Usuario | Persona (DNI de prueba) | Rol | Áreas habilitadas |
| --- | --- | --- | --- |
| `admin` | Administradora Cautiva Spa (70000001) | ADMINISTRADORA | — |
| `recepcion` | Recepcionista Uno (70000002) | RECEPCIONISTA | — |
| (sin usuario) | Recepcionista Dos (70000003) | — | — para probar CUS52 Registrar Usuario |
| `especialista1` | Especialista Salón (70000004) | ESPECIALISTA_TECNICA | Peluquería y Estilismo, Manicura y Pedicura |
| `especialista2` | Especialista Spa (70000005) | ESPECIALISTA_TECNICA | Cuidado Facial, Cabina Spa, Depilación |

Horario: lunes a sábado (dia_semana 1 a 6), 09:00 a 19:00. Los cuatro usuarios comparten la clave temporal escrita en la cabecera del script 15 (hash BCrypt `$2a$`, costo 10) y tienen `debe_cambiar_clave = 'S'`. En producción no se usa este script.

## Cómo debe conectarse el back-end

1. **Cuenta `CAUTIVA_APP`**, nunca la del dueño. `PKG_SESION` reconoce al back-end por esa cuenta; con la del dueño Oracle lo trata como mantenimiento y no exige ser Administradora ni audita al usuario.
2. **Esquema**: las tablas son del dueño. Al abrir cada conexión: `ALTER SESSION SET CURRENT_SCHEMA = <dueño>` (Hikari `connection-init-sql`), y `hibernate.default_schema = <dueño>`.
3. **Usuario de la operación**: en cada transacción de un usuario autenticado, primero `BEGIN PKG_SESION.iniciar(:id_usuario); END;` y al terminar `BEGIN PKG_SESION.terminar; END;`. Sin eso Oracle responde `-20001` («El backend debe llamar a PKG_SESION.iniciar»). Lo único que se hace sin `iniciar` es el inicio de sesión: leer USUARIO por `nombre_usuario` y sumar 1 a `intentos_fallidos` si la clave no coincide.
4. **Lo que ya hace Oracle (no se programa en Java)**: auditoría de todo cambio (54 triggers `TRG_AUD_*`, con el usuario de `PKG_SESION`); bloqueo de la cuenta al llegar a `LOGIN_INTENTOS_MAX` por `LOGIN_BLOQUEO_MIN` minutos; serie y número de comprobantes (`PKG_VENTA`); efectos de cerrar o anular una venta (`TRG_VENTA_EFECTOS`); stock por kardex (`TRG_KARDEX_APLICAR`); cruces de agenda (`TRG_CITA_CRUCE`, `TRG_ATENCION_CRUCE`); abonos de saldo a favor por venta anulada (`TRG_SALDO_FAVOR_REGLAS` rechaza con `-20063` cualquier otro).

## Testcontainers (gvenzl/oracle-free)

Los scripts no se pueden correr por JDBC. En la prueba: copiar `database/` al contenedor (`withCopyFileToContainer`) y ejecutar con `execInContainer`, con `NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252` (los archivos están en Windows-1252):

```bash
# dueño = APP_USER del contenedor (lo crea con CONNECT, RESOURCE y CREATE VIEW y cuota en USERS)
sqlplus -s APP_USER/APP_USER_PASSWORD@//localhost:1521/FREEPDB1 @/database/00_instalar_v5.sql
sqlplus -s system/ORACLE_PASSWORD@//localhost:1521/FREEPDB1 @/database/14_cuenta_aplicacion.sql
sqlplus -s APP_USER/APP_USER_PASSWORD@//localhost:1521/FREEPDB1 @/database/13_permisos.sql
sqlplus -s APP_USER/APP_USER_PASSWORD@//localhost:1521/FREEPDB1 @/database/15_datos_iteracion1.sql
# y como system: ALTER USER CAUTIVA_APP IDENTIFIED BY "<clave de la prueba>" ACCOUNT UNLOCK;
```

Cada `sqlplus` debe terminar con `EXIT`: use un script envoltorio que haga `@archivo` y luego `EXIT`, o pase las líneas por la entrada estándar. Comprobar en la salida que no haya `ORA-`, `PLS-` ni `SP2-`.

## Ambiente local (Docker, 01/11/2026)

`localhost:1522`, servicio `FREEPDB1` (Oracle AI Database 26ai Free), dueño `SYNAPSIS_SPA_CAUTIVA`. Pasos 1 a 6 hechos el 01-02/11/2026: 0 errores, 0 inválidos, pruebas 14 OK y 144 OK, cuenta `CAUTIVA_APP` desbloqueada y verificada (11 OK).

En la PC, la clave local de `CAUTIVA_APP` está en `.local/clave_cautiva_app.sql` del repositorio del back-end (carpeta ignorada por git); `.local/verificar_cautiva_app.sql` la aplica y corre el paso 6 desde DBeaver (*Execute in SQL\*Plus*). El back-end lee la conexión de `.env` (también ignorado por git; plantilla en `.env.example`) con `spring.config.import=optional:file:.env[.properties]`.

Nota para Oracle 23ai/26ai: no escribir un comentario `--` después del `;` en la misma línea de una sentencia SQL (da `ORA-03405`); en 19c sí funcionaba.
