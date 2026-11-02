-- =============================================================================
-- Cautiva Spa · v5 · Crea el usuario de prueba CAUTIVA5 (dueño del esquema v5)
-- Ejecutar UNA vez con F5, conectado como SYSTEM al PDB ORCLPDB.
-- SQL Developer pedirá la contraseña de CAUTIVA5 en una ventana: la escribe usted.
-- Usa los tablespaces de v4 (script 01); no toca el esquema CAUTIVA.
-- =============================================================================
SET DEFINE ON
SET VERIFY OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE
PROMPT Contenedor actual (debe ser ORCLPDB):
SELECT SYS_CONTEXT('USERENV', 'CON_NAME') AS contenedor FROM dual;

CREATE USER CAUTIVA5 IDENTIFIED BY "&&pwd_cautiva5"
    DEFAULT TABLESPACE TBS_CAUTIVA_DATOS
    TEMPORARY TABLESPACE TBS_CAUTIVA_TEMP
    QUOTA UNLIMITED ON TBS_CAUTIVA_DATOS
    QUOTA UNLIMITED ON TBS_CAUTIVA_INDICES
    PROFILE PRF_CAUTIVA_SERVICIO;
GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TRIGGER, CREATE TYPE, CREATE SYNONYM TO CAUTIVA5;

UNDEFINE pwd_cautiva5
SELECT username, account_status, default_tablespace FROM dba_users WHERE username = 'CAUTIVA5';
WHENEVER SQLERROR CONTINUE
PROMPT Listo: cree en SQL Developer la conexion CAUTIVA5 (localhost, 1521, servicio orclpdb).
