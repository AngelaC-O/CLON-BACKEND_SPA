-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle · VERSIÓN 5
-- Script 16 · Verifica la cuenta del back-end (02/11/2026)
-- Se ejecuta CONECTADO COMO CAUTIVA_APP (igual que el back-end), después de 14, 13 y 15.
-- Antes de llamarlo: DEFINE esquema = <dueño del esquema>   (en Docker: SYNAPSIS_SPA_CAUTIVA)
-- No deja datos: todo termina en ROLLBACK. Cada caso imprime OK o FALLO.
--
-- Comprueba lo que el back-end necesita:
--   1. conectarse como CAUTIVA_APP y fijar CURRENT_SCHEMA (connection-init-sql de Hikari);
--   2. leer las tablas, vistas y parámetros sin escribir el esquema en cada consulta;
--   3. el inicio de sesión sin PKG_SESION.iniciar (leer USUARIO y sumar intentos_fallidos);
--   4. que sin PKG_SESION.iniciar una operación de negocio falle con -20001;
--   5. que con PKG_SESION.iniciar la operación pase y quede en AUDITORIA con el id del usuario;
--   6. que una recepcionista no pueda hacer lo de la Administradora (-20003);
--   7. que la cuenta no pueda crear objetos (sin DDL; se prueba primero).
-- =============================================================================
SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
ALTER SESSION SET CURRENT_SCHEMA = &&esquema;

DECLARE
    v_ok    PLS_INTEGER := 0;
    v_fallo PLS_INTEGER := 0;
    v_n     NUMBER;
    v_txt   VARCHAR2(200);
    v_adm   NUMBER;
    v_rec   NUMBER;
    v_area  NUMBER;
    PROCEDURE ok(p VARCHAR2)    IS BEGIN v_ok := v_ok + 1;       DBMS_OUTPUT.PUT_LINE('OK     ' || p); END;
    PROCEDURE fallo(p VARCHAR2) IS BEGIN v_fallo := v_fallo + 1; DBMS_OUTPUT.PUT_LINE('FALLO  ' || p); END;
    PROCEDURE comprobar(p VARCHAR2, c BOOLEAN) IS BEGIN IF c THEN ok(p); ELSE fallo(p); END IF; END;
    PROCEDURE debe_fallar(p VARCHAR2, p_sql VARCHAR2, p_codigo NUMBER) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
        fallo(p || ' (se esperaba ' || p_codigo || ')');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = p_codigo THEN ok(p || ' -> ' || SUBSTR(SQLERRM, 1, 110));
        ELSE fallo(p || ' -> ' || SQLERRM); END IF;
    END;
BEGIN
    -- Primero el DDL (Oracle confirma la transacción antes de un DDL): así no afecta al ROLLBACK final
    debe_fallar('La cuenta de aplicación no crea tablas', 'CREATE TABLE PRUEBA_DDL (x NUMBER)', -1031);
    SAVEPOINT inicio;
    SELECT USER || ' / ' || SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') INTO v_txt FROM dual;
    comprobar('Conectado como CAUTIVA_APP con el esquema de las tablas: ' || v_txt,
              v_txt = 'CAUTIVA_APP / ' || UPPER('&&esquema'));

    SELECT COUNT(*) INTO v_n FROM TIPO_DOCUMENTO;
    comprobar('Lee TIPO_DOCUMENTO sin escribir el esquema (' || v_n || ' tipos)', v_n = 5);
    SELECT COUNT(*) INTO v_n FROM PARAMETRO WHERE codigo IN ('IGV_PORCENTAJE', 'UMBRAL_BOLETA_DOC', 'LOGIN_INTENTOS_MAX', 'SESION_INACTIVIDAD_MIN');
    comprobar('Lee los parámetros del negocio (' || v_n || ' de 4)', v_n = 4);
    SELECT COUNT(*) INTO v_n FROM V_CAJA_ESPERADO;
    ok('Lee la vista V_CAJA_ESPERADO');

    SELECT id_usuario INTO v_adm FROM USUARIO WHERE nombre_usuario = 'admin';
    SELECT id_usuario INTO v_rec FROM USUARIO WHERE nombre_usuario = 'recepcion';
    SELECT COUNT(*) INTO v_n FROM USUARIO WHERE nombre_usuario = 'admin' AND clave_hash LIKE '$2a$10$%' AND debe_cambiar_clave = 'S';
    comprobar('Login: lee el usuario admin con su hash BCrypt y cambio de clave pendiente', v_n = 1);
    UPDATE USUARIO SET intentos_fallidos = intentos_fallidos + 1 WHERE id_usuario = v_rec;
    ok('Login: suma un intento fallido sin PKG_SESION.iniciar');

    debe_fallar('Sin PKG_SESION.iniciar, registrar un área se rechaza',
        'INSERT INTO AREA (linea, nombre) VALUES (''SALON'', ''Prueba de la cuenta de aplicación'')', -20001);

    PKG_SESION.iniciar(v_rec);
    debe_fallar('Con la recepcionista, registrar un área es solo de la Administradora',
        'INSERT INTO AREA (linea, nombre) VALUES (''SALON'', ''Prueba de la cuenta de aplicación'')', -20003);
    PKG_SESION.terminar;

    PKG_SESION.iniciar(v_adm);
    INSERT INTO AREA (linea, nombre) VALUES ('SALON', 'Prueba de la cuenta de aplicación') RETURNING id_area INTO v_area;
    ok('Con la Administradora, el área se registra');
    SELECT COUNT(*) INTO v_n FROM AUDITORIA WHERE entidad = 'AREA' AND id_entidad = v_area AND id_usuario = v_adm;
    comprobar('La auditoría la registró Oracle con el id de la Administradora', v_n = 1);
    PKG_SESION.terminar;

    ROLLBACK TO inicio;
    DBMS_OUTPUT.PUT_LINE('==============================================');
    DBMS_OUTPUT.PUT_LINE('Resultado: ' || v_ok || ' OK, ' || v_fallo || ' FALLO (todo se deshizo con ROLLBACK)');
EXCEPTION WHEN OTHERS THEN
    ROLLBACK TO inicio;
    PKG_SESION.terminar;
    DBMS_OUTPUT.PUT_LINE('FALLO  error inesperado: ' || SQLERRM);
    DBMS_OUTPUT.PUT_LINE('Resultado parcial: ' || v_ok || ' OK, ' || (v_fallo + 1) || ' FALLO');
END;
/
SET DEFINE OFF
