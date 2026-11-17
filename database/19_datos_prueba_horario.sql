-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle · VERSIÓN 5.2
-- Script 19 · Datos de prueba del horario (Iteración 2.1, 15/11/2026)
--
-- SOLO PARA LA BASE LOCAL Y LA DE LAS PRUEBAS E2E. NUNCA SE CORRE EN PRODUCCIÓN: crea personal y usuarios de
-- prueba y reemplaza los horarios del personal de 15 y 17. Los contenedores de las pruebas Java
-- (OracleTestcontainer) tampoco lo instalan: siguen con los datos de 15 y 17.
--
-- Se ejecuta como: dueño del esquema (modo mantenimiento), después de 18_ajustes_v52.sql.
-- Se puede repetir sin error: el personal nuevo solo se crea si no existe y los horarios de las 8 personas se
-- borran y se vuelven a crear.
--
-- Por qué existe: con el horario de 15 (lunes a sábado de 09:00 a 19:00) no hay especialistas el domingo ni
-- después de las 19:00, aunque la sede abre todos los días de 09:00 a 20:00. Con este horario cada persona
-- descansa un día (sin fila en HORARIO_PERSONAL) y cada día hay al menos una especialista por área.
--
-- Personal nuevo (como lo crea 15: misma clave temporal, debe_cambiar_clave = 'S'; modalidad de pago, sueldo y
-- comisión copiados de especialista1 y especialista2):
--   especialista3   Especialista   70000007 ESPECIALISTA_TECNICA  las áreas de especialista1
--   especialista4   Especialista   70000008 ESPECIALISTA_TECNICA  las áreas de especialista2
--
-- Horario: de 09:00 a 20:00 con 60 minutos de almuerzo, todos los días menos el de descanso
-- (dia_semana: 1 = lunes ... 7 = domingo):
--   admin (domingo), recepcion (lunes), recepcionista sin usuario 70000003 (jueves), almacen (domingo),
--   especialista1 (lunes), especialista3 (jueves), especialista2 (martes), especialista4 (viernes).
-- =============================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF
SET LINESIZE 200
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT Personal y horario de prueba (solo base local y e2e)...
DECLARE
    -- Mismo hash BCrypt de la clave temporal de 15_datos_iteracion1.sql
    c_hash CONSTANT VARCHAR2(60) := '$2a$10$wo6I3ZjHiMkRO1QZOhnXzeLY65D3ueTW4VnmkyWLcc7p11N5ICftO';
    v_dni  NUMBER;

    FUNCTION personal_de_usuario(p_nombre_usuario IN VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        SELECT id_personal INTO v FROM USUARIO WHERE nombre_usuario = p_nombre_usuario;
        RETURN v;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20099, 'No existe el usuario ' || p_nombre_usuario
                || ' (lo cargan 15_datos_iteracion1.sql y 17_datos_iteracion2.sql).');
    END personal_de_usuario;

    FUNCTION personal_de_dni(p_dni IN VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        SELECT pe.id_personal INTO v
          FROM PERSONAL pe JOIN PERSONA p ON p.id_persona = pe.id_personal
         WHERE p.id_tipo_documento = v_dni AND p.numero_documento = p_dni;
        RETURN v;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN NULL;
    END personal_de_dni;

    -- Crea la especialista con los datos de pago y las áreas de su modelo, si no existe
    PROCEDURE especialista(p_usuario IN VARCHAR2, p_apellidos IN VARCHAR2, p_dni IN VARCHAR2, p_telefono IN VARCHAR2,
                           p_modelo IN VARCHAR2) IS
        v_id        NUMBER := personal_de_dni(p_dni);
        v_id_modelo NUMBER := personal_de_usuario(p_modelo);
        v_modelo    PERSONAL%ROWTYPE;
        v_n         NUMBER;
    BEGIN
        SELECT * INTO v_modelo FROM PERSONAL WHERE id_personal = v_id_modelo;
        IF v_id IS NULL THEN
            INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono)
            VALUES ('NATURAL', 'Especialista', p_apellidos, v_dni, p_dni, p_telefono)
            RETURNING id_persona INTO v_id;
            INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
            VALUES (v_id, v_modelo.id_sede, 'ESPECIALISTA_TECNICA', v_modelo.modalidad_pago, v_modelo.sueldo_base,
                    v_modelo.porcentaje_comision, TRUNC(SYSDATE));
            DBMS_OUTPUT.PUT_LINE('   + Personal ' || p_apellidos || ' (DNI de prueba ' || p_dni || '), como ' || p_modelo);
        END IF;
        INSERT INTO PERSONAL_AREA (id_personal, id_area)
        SELECT v_id, pa.id_area FROM PERSONAL_AREA pa
         WHERE pa.id_personal = v_modelo.id_personal
           AND NOT EXISTS (SELECT 1 FROM PERSONAL_AREA x WHERE x.id_personal = v_id AND x.id_area = pa.id_area);
        IF SQL%ROWCOUNT > 0 THEN
            DBMS_OUTPUT.PUT_LINE('   + ' || SQL%ROWCOUNT || ' área(s) de ' || p_modelo || ' para ' || p_usuario);
        END IF;
        SELECT COUNT(*) INTO v_n FROM USUARIO WHERE nombre_usuario = p_usuario;
        IF v_n = 0 THEN
            INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash, debe_cambiar_clave)
            SELECT v_id, id_rol, p_usuario, c_hash, 'S' FROM ROL WHERE nombre = 'ESPECIALISTA_TECNICA';
            DBMS_OUTPUT.PUT_LINE('   + Usuario ' || p_usuario || ' (ESPECIALISTA_TECNICA, clave temporal de 15)');
        END IF;
    END especialista;

    -- Borra el horario de la persona y lo vuelve a crear: de 09:00 a 20:00 todos los días menos el de descanso
    PROCEDURE horario(p_id_personal IN NUMBER, p_quien IN VARCHAR2, p_descanso IN NUMBER) IS
    BEGIN
        DELETE FROM HORARIO_PERSONAL WHERE id_personal = p_id_personal;
        FOR d IN 1 .. 7 LOOP
            IF d <> p_descanso THEN
                INSERT INTO HORARIO_PERSONAL (id_personal, dia_semana, hora_entrada, hora_salida, minutos_almuerzo)
                VALUES (p_id_personal, d, '09:00', '20:00', 60);
            END IF;
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('   Horario de ' || RPAD(p_quien, 38) || ' 09:00 a 20:00, descansa el día ' || p_descanso);
    END horario;
BEGIN
    SELECT id_tipo_documento INTO v_dni FROM TIPO_DOCUMENTO WHERE abreviatura = 'DNI';

    especialista('especialista3', 'Salón 2', '70000007', '987100007', 'especialista1');
    especialista('especialista4', 'Spa 2',   '70000008', '987100008', 'especialista2');

    IF personal_de_dni('70000003') IS NULL THEN
        RAISE_APPLICATION_ERROR(-20099, 'No existe la recepcionista sin usuario (DNI 70000003, la carga 15_datos_iteracion1.sql).');
    END IF;
    horario(personal_de_usuario('admin'),         'admin',                           7);
    horario(personal_de_usuario('recepcion'),     'recepcion',                       1);
    horario(personal_de_dni('70000003'),          'recepcionista sin usuario (70000003)', 4);
    horario(personal_de_usuario('almacen'),       'almacen',                         7);
    horario(personal_de_usuario('especialista1'), 'especialista1',                   1);
    horario(personal_de_usuario('especialista3'), 'especialista3',                   4);
    horario(personal_de_usuario('especialista2'), 'especialista2',                   2);
    horario(personal_de_usuario('especialista4'), 'especialista4',                   5);
    COMMIT;
END;
/

-- =============================================================================
-- Comprobación: cada día de la semana, al menos una especialista activa por cada área de PERSONAL_AREA
-- =============================================================================
PROMPT Comprobando que cada día haya una especialista por área...
DECLARE
    v_faltan NUMBER := 0;
    v_sede   VARCHAR2(80);
    v_ap     VARCHAR2(5);
    v_ci     VARCHAR2(5);
BEGIN
    SELECT s.nombre, s.horario_apertura, s.horario_cierre INTO v_sede, v_ap, v_ci
      FROM SEDE s JOIN PERSONAL pe ON pe.id_sede = s.id_sede JOIN USUARIO u ON u.id_personal = pe.id_personal
     WHERE u.nombre_usuario = 'especialista1';
    IF v_ap <> '09:00' OR v_ci <> '20:00' THEN
        DBMS_OUTPUT.PUT_LINE('   REVISAR: la sede ' || v_sede || ' abre de ' || v_ap || ' a ' || v_ci || ', no de 09:00 a 20:00.');
    END IF;
    FOR r IN (SELECT d.dia, a.nombre AS area,
                     (SELECT COUNT(*)
                        FROM PERSONAL_AREA pa
                        JOIN PERSONAL pe       ON pe.id_personal = pa.id_personal
                        JOIN HORARIO_PERSONAL h ON h.id_personal = pe.id_personal AND h.dia_semana = d.dia
                       WHERE pa.id_area = a.id_area
                         AND pe.cargo = 'ESPECIALISTA_TECNICA'
                         AND pe.estado = 'ACTIVO') AS especialistas
                FROM (SELECT LEVEL AS dia FROM dual CONNECT BY LEVEL <= 7) d
               CROSS JOIN AREA a
               WHERE a.id_area IN (SELECT pa.id_area FROM PERSONAL_AREA pa)
               ORDER BY d.dia, a.nombre) LOOP
        IF r.especialistas = 0 THEN
            v_faltan := v_faltan + 1;
            DBMS_OUTPUT.PUT_LINE('   FALTA: el día ' || r.dia || ' no hay especialista activa de ' || r.area || '.');
        END IF;
    END LOOP;
    IF v_faltan = 0 THEN
        DBMS_OUTPUT.PUT_LINE('   OK: los 7 días hay al menos una especialista activa por cada área de PERSONAL_AREA.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('   REVISAR: ' || v_faltan || ' combinación(es) de día y área sin especialista.');
    END IF;
END;
/

SET FEEDBACK ON
COLUMN persona FORMAT A28
COLUMN usuario FORMAT A14
COLUMN cargo FORMAT A22
COLUMN dias FORMAT A15
COLUMN descansa FORMAT A9
PROMPT Horario del personal de prueba (1 = lunes ... 7 = domingo):
SELECT p.nombres || ' ' || p.apellidos AS persona, u.nombre_usuario AS usuario, pe.cargo,
       LISTAGG(h.dia_semana, ',') WITHIN GROUP (ORDER BY h.dia_semana) AS dias,
       MIN(h.hora_entrada) || '-' || MAX(h.hora_salida) AS horas,
       (SELECT LISTAGG(d.dia, ',') WITHIN GROUP (ORDER BY d.dia)
          FROM (SELECT LEVEL AS dia FROM dual CONNECT BY LEVEL <= 7) d
         WHERE NOT EXISTS (SELECT 1 FROM HORARIO_PERSONAL x WHERE x.id_personal = pe.id_personal AND x.dia_semana = d.dia)) AS descansa
  FROM PERSONAL pe
  JOIN PERSONA p ON p.id_persona = pe.id_personal
  LEFT JOIN USUARIO u ON u.id_personal = pe.id_personal
  JOIN HORARIO_PERSONAL h ON h.id_personal = pe.id_personal
 WHERE p.numero_documento BETWEEN '70000001' AND '70000008'
 GROUP BY pe.id_personal, p.nombres, p.apellidos, u.nombre_usuario, pe.cargo, p.numero_documento
 ORDER BY p.numero_documento;
PROMPT 19_datos_prueba_horario.sql terminado.
