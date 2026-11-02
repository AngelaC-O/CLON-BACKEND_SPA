-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle 19c · VERSIÓN 5 · Instalación completa
-- Ejecutar en SQL Developer (F5) conectado como el dueño de un esquema VACÍO
-- (por ejemplo un usuario nuevo CAUTIVA5, o CAUTIVA después de 99_eliminar_v5.sql
-- y de retirar los objetos v4). Todos los archivos deben estar en la misma carpeta.
--
--   01 a 05  tablas, PK, UNIQUE, CHECK, COMMENT, FOREIGN KEY e índices (modelo de 55 tablas)
--   06       reglas de las jerarquías y del documento (2 funciones y 7 triggers)
--   07       los 5 tipos de documento
--   08       paquetes PL/SQL (PKG_SESION, PKG_AGENDA, PKG_INVENTARIO, PKG_CAJA, PKG_VENTA)
--   09       triggers de reglas de negocio adaptados de v4
--   10       triggers de auditoría (uno por tabla, salvo AUDITORIA)
--   11       vistas de pantallas y reportes
--   12       datos iniciales (parámetros, roles, permisos, sede, recursos, catálogo)
--   13       permisos a los roles de la aplicación: OPCIONAL, solo si el DBA ya creó
--            ROL_CAUTIVA_APLICACION y ROL_CAUTIVA_REPORTES (script 01 de v4)
-- =============================================================================
SET DEFINE OFF
SET ECHO OFF
SPOOL instalar_v5.log
WHENEVER SQLERROR CONTINUE

@@01_nucleo_personas_seguridad.sql
@@02_personal_catalogo.sql
@@03_agenda_ventas.sql
@@04_caja_inventario.sql
@@05_claves_foraneas_indices.sql
@@06_reglas_herencia.sql
@@07_datos_tipo_documento.sql
@@08_paquetes.sql
@@09_triggers_reglas.sql
@@10_triggers_auditoria.sql
@@11_vistas.sql
@@12_datos_iniciales.sql
WHENEVER SQLERROR CONTINUE
-- @@13_permisos.sql

PROMPT ==== Errores de compilación (debe salir vacío) ====
SELECT name, type, line, position, SUBSTR(text, 1, 200) AS error
  FROM user_errors
 ORDER BY name, type, sequence;

PROMPT ==== Resumen del esquema ====
SELECT COUNT(*) AS tablas FROM user_tables;
SELECT constraint_type, COUNT(*) AS total FROM user_constraints
 WHERE constraint_name NOT LIKE 'SYS%' AND constraint_name NOT LIKE 'BIN$%'
 GROUP BY constraint_type ORDER BY constraint_type;
SELECT COUNT(*) AS indices_ix FROM user_indexes WHERE index_name LIKE 'IX\_%' ESCAPE '\';
SELECT object_type, COUNT(*) AS total FROM user_objects
 WHERE object_type IN ('FUNCTION', 'PACKAGE', 'PACKAGE BODY', 'TRIGGER', 'VIEW') GROUP BY object_type ORDER BY object_type;
SELECT object_type, object_name FROM user_objects WHERE status = 'INVALID';
SELECT codigo_sunat, abreviatura, consulta_api FROM TIPO_DOCUMENTO ORDER BY codigo_sunat;
PROMPT Esperado: 55 tablas; P = 55, R = 121, U = 33, C = 168 con nombre propio (más los NOT NULL de sistema); 97 índices IX_ (91 de FK y 6 de soporte);
PROMPT           FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 125 (7 de jerarquías, 64 de reglas y 54 de auditoría), VIEW 27;
PROMPT           5 tipos de documento; ningún error de compilación ni objeto inválido.
SPOOL OFF
