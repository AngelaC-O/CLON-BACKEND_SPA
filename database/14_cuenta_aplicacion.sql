-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle · VERSIÓN 5
-- Script 14 · Cuenta de la aplicación (CAUTIVA_APP), roles y perfil (01/11/2026)
-- Se ejecuta como: un DBA (SYSTEM) o el dueño del esquema si tiene privilegios de DBA
-- (en el Oracle de Docker, SYNAPSIS_SPA_CAUTIVA los tiene). Se puede ejecutar varias veces.
--
-- Por qué existe: PKG_SESION reconoce al backend por la cuenta CAUTIVA_APP. Con esa cuenta,
-- toda operación exige PKG_SESION.iniciar(id_usuario) y las reglas de la Administradora se
-- aplican; si el backend entrara con la cuenta dueña, Oracle lo trataría como mantenimiento.
--
-- Crea (si no existen):
--   PRF_CAUTIVA_SERVICIO     perfil de cuenta de servicio: la clave no vence, 10 intentos
--   ROL_CAUTIVA_APLICACION   permisos del backend (los da el script 13)
--   ROL_CAUTIVA_REPORTES     solo vistas de reporte (los da el script 13)
--   CAUTIVA_APP              cuenta del backend: sin objetos propios, sin cuota, BLOQUEADA y con
--                            una clave aleatoria que nadie conoce.
--
-- DESPUÉS de este script, la persona responsable elige la clave y desbloquea la cuenta
-- (la clave nunca va en un script ni en el repositorio; va en la variable de entorno del backend):
--   ALTER USER CAUTIVA_APP IDENTIFIED BY "<su clave>" ACCOUNT UNLOCK;
--
-- El backend se conecta como CAUTIVA_APP y fija el esquema de las tablas al abrir cada conexión:
--   ALTER SESSION SET CURRENT_SCHEMA = <dueño del esquema>   (en Docker: SYNAPSIS_SPA_CAUTIVA)
-- =============================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON

PROMPT Perfil, roles y cuenta de la aplicación...
DECLARE
    v_n NUMBER;
    PROCEDURE ejecutar(p_sql IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
    END;
BEGIN
    SELECT COUNT(*) INTO v_n FROM dba_profiles WHERE profile = 'PRF_CAUTIVA_SERVICIO';
    IF v_n = 0 THEN
        ejecutar('CREATE PROFILE PRF_CAUTIVA_SERVICIO LIMIT FAILED_LOGIN_ATTEMPTS 10 PASSWORD_LOCK_TIME 1/96 '
              || 'PASSWORD_LIFE_TIME UNLIMITED PASSWORD_REUSE_MAX 5 PASSWORD_REUSE_TIME UNLIMITED');
        DBMS_OUTPUT.PUT_LINE('Perfil PRF_CAUTIVA_SERVICIO creado.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Perfil PRF_CAUTIVA_SERVICIO ya existía.');
    END IF;

    FOR r IN (SELECT 'ROL_CAUTIVA_APLICACION' AS rol FROM dual UNION ALL SELECT 'ROL_CAUTIVA_REPORTES' FROM dual) LOOP
        SELECT COUNT(*) INTO v_n FROM dba_roles WHERE role = r.rol;
        IF v_n = 0 THEN
            ejecutar('CREATE ROLE ' || r.rol);
            DBMS_OUTPUT.PUT_LINE('Rol ' || r.rol || ' creado.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('Rol ' || r.rol || ' ya existía.');
        END IF;
    END LOOP;

    SELECT COUNT(*) INTO v_n FROM dba_users WHERE username = 'CAUTIVA_APP';
    IF v_n = 0 THEN
        -- Clave aleatoria que no se muestra ni se guarda: la cuenta queda bloqueada hasta que
        -- la persona responsable ponga la suya con ALTER USER ... ACCOUNT UNLOCK.
        ejecutar('CREATE USER CAUTIVA_APP IDENTIFIED BY "A' || DBMS_RANDOM.STRING('X', 26) || 'z9" '
              || 'DEFAULT TABLESPACE USERS QUOTA 0 ON USERS PROFILE PRF_CAUTIVA_SERVICIO ACCOUNT LOCK');
        DBMS_OUTPUT.PUT_LINE('Usuario CAUTIVA_APP creado (bloqueado, sin clave conocida).');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Usuario CAUTIVA_APP ya existía: no se cambia su clave ni su estado.');
    END IF;

    ejecutar('GRANT CREATE SESSION TO CAUTIVA_APP');
    ejecutar('GRANT ROL_CAUTIVA_APLICACION TO CAUTIVA_APP');
    ejecutar('ALTER USER CAUTIVA_APP DEFAULT ROLE ALL');
END;
/

PROMPT Cuenta de la aplicación:
SELECT username, account_status, profile, default_tablespace FROM dba_users WHERE username = 'CAUTIVA_APP';
SELECT grantee, granted_role, default_role FROM dba_role_privs WHERE grantee = 'CAUTIVA_APP';
PROMPT 14_cuenta_aplicacion.sql terminado. Siga con 13_permisos.sql como dueño del esquema.
