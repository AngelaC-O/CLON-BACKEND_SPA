-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle · VERSIÓN 5
-- Script 15 · Personal y usuarios para empezar la Iteración 1 (01/11/2026)
-- Se ejecuta como: dueño del esquema (modo mantenimiento), después de 00_instalar_v5.sql.
-- No se ejecuta dos veces: si el usuario 'admin' ya existe, no hace nada.
--
-- Por qué existe: USUARIO, CITA y ATENCION exigen un PERSONAL, y agendar o atender exige que la
-- especialista tenga el área del servicio en PERSONAL_AREA (PKG_AGENDA.validar_habilitacion, -20012).
-- CUS36 Registrar Personal es de otra iteración, así que la Iteración 1 parte de este personal.
--
-- Deja (sede 'Pichanaki Centro'):
--   usuario         persona (DNI)          cargo / rol            áreas habilitadas
--   admin           Administradora 70000001 ADMINISTRADORA        -
--   recepcion       Recepcionista  70000002 RECEPCIONISTA         -
--   (sin usuario)   Recepcionista  70000003 RECEPCIONISTA         -   <- para probar CUS52 Registrar Usuario
--   especialista1   Especialista   70000004 ESPECIALISTA_TECNICA  Peluquería y Estilismo, Manicura y Pedicura
--   especialista2   Especialista   70000005 ESPECIALISTA_TECNICA  Cuidado Facial, Cabina Spa, Depilación
-- Horario del personal: lunes a sábado (dia_semana 1 a 6) de 09:00 a 19:00, 60 min de almuerzo.
--
-- CLAVE TEMPORAL de los 4 usuarios (solo para el ambiente local y de pruebas): CambiarAlEntrar2026
--   Guardada como hash BCrypt ($2a$, costo 10, compatible con BCryptPasswordEncoder de Spring).
--   Todos tienen debe_cambiar_clave = 'S': el primer inicio de sesión obliga a cambiarla (RF82).
--   En producción no se usa este script: la Administradora real se crea con su propia clave.
-- Los nombres, DNI y teléfonos son de prueba [COMPLETAR con los datos reales cuando corresponda].
-- =============================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON

PROMPT Personal y usuarios de la Iteración 1...
DECLARE
    c_hash CONSTANT VARCHAR2(60) := '$2a$10$wo6I3ZjHiMkRO1QZOhnXzeLY65D3ueTW4VnmkyWLcc7p11N5ICftO';
    v_n    NUMBER;
    v_sede NUMBER;
    v_dni  NUMBER;
    v_adm  NUMBER;
    v_rec  NUMBER;
    v_rec2 NUMBER;
    v_esp1 NUMBER;
    v_esp2 NUMBER;

    FUNCTION persona(p_nombres VARCHAR2, p_apellidos VARCHAR2, p_dni VARCHAR2, p_telefono VARCHAR2) RETURN NUMBER IS
        v_id NUMBER;
    BEGIN
        INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono)
        VALUES ('NATURAL', p_nombres, p_apellidos, v_dni, p_dni, p_telefono)
        RETURNING id_persona INTO v_id;
        RETURN v_id;
    END;

    FUNCTION personal(p_nombres VARCHAR2, p_apellidos VARCHAR2, p_dni VARCHAR2, p_telefono VARCHAR2,
                      p_cargo VARCHAR2, p_modalidad VARCHAR2, p_sueldo NUMBER, p_comision NUMBER) RETURN NUMBER IS
        v_id NUMBER := persona(p_nombres, p_apellidos, p_dni, p_telefono);
    BEGIN
        INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
        VALUES (v_id, v_sede, p_cargo, p_modalidad, p_sueldo, p_comision, TRUNC(SYSDATE));
        FOR d IN 1 .. 6 LOOP
            INSERT INTO HORARIO_PERSONAL (id_personal, dia_semana, hora_entrada, hora_salida, minutos_almuerzo)
            VALUES (v_id, d, '09:00', '19:00', 60);
        END LOOP;
        RETURN v_id;
    END;

    PROCEDURE area(p_id_personal NUMBER, p_area VARCHAR2) IS
    BEGIN
        INSERT INTO PERSONAL_AREA (id_personal, id_area)
        SELECT p_id_personal, id_area FROM AREA WHERE nombre = p_area;
        IF SQL%ROWCOUNT = 0 THEN
            RAISE_APPLICATION_ERROR(-20099, 'No existe el área ' || p_area || ' (la carga 12_datos_iniciales.sql).');
        END IF;
    END;

    PROCEDURE usuario(p_id_personal NUMBER, p_rol VARCHAR2, p_nombre_usuario VARCHAR2) IS
    BEGIN
        INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash, debe_cambiar_clave)
        SELECT p_id_personal, id_rol, p_nombre_usuario, c_hash, 'S' FROM ROL WHERE nombre = p_rol;
    END;

BEGIN
    SELECT COUNT(*) INTO v_n FROM USUARIO WHERE nombre_usuario = 'admin';
    IF v_n > 0 THEN
        DBMS_OUTPUT.PUT_LINE('El usuario admin ya existe: 15_datos_iteracion1.sql no cambia nada.');
        RETURN;
    END IF;
    SELECT id_sede INTO v_sede FROM SEDE WHERE nombre = 'Pichanaki Centro';
    SELECT id_tipo_documento INTO v_dni FROM TIPO_DOCUMENTO WHERE abreviatura = 'DNI';

    v_adm  := personal('Administradora', 'Cautiva Spa',  '70000001', '987100001', 'ADMINISTRADORA',       'FIJO',     0,    0);
    v_rec  := personal('Recepcionista',  'Uno',          '70000002', '987100002', 'RECEPCIONISTA',        'FIJO',     1230, 0);
    v_rec2 := personal('Recepcionista',  'Dos',          '70000003', '987100003', 'RECEPCIONISTA',        'FIJO',     1230, 0);
    v_esp1 := personal('Especialista',   'Salón',        '70000004', '987100004', 'ESPECIALISTA_TECNICA', 'MIXTO',    800,  20);
    v_esp2 := personal('Especialista',   'Spa',          '70000005', '987100005', 'ESPECIALISTA_TECNICA', 'COMISION', 0,    35);

    area(v_esp1, 'Peluquería y Estilismo');
    area(v_esp1, 'Manicura y Pedicura');
    area(v_esp2, 'Cuidado Facial');
    area(v_esp2, 'Cabina Spa');
    area(v_esp2, 'Depilación');

    usuario(v_adm,  'ADMINISTRADORA',       'admin');
    usuario(v_rec,  'RECEPCIONISTA',        'recepcion');
    usuario(v_esp1, 'ESPECIALISTA_TECNICA', 'especialista1');
    usuario(v_esp2, 'ESPECIALISTA_TECNICA', 'especialista2');
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Personal y usuarios de la Iteración 1 cargados.');
END;
/

PROMPT Personal de la Iteración 1:
SELECT u.nombre_usuario, r.nombre AS rol, p.nombres || ' ' || p.apellidos AS persona, p.numero_documento AS dni,
       pe.cargo, u.debe_cambiar_clave,
       (SELECT LISTAGG(a.nombre, ', ') WITHIN GROUP (ORDER BY a.nombre)
          FROM PERSONAL_AREA pa JOIN AREA a ON a.id_area = pa.id_area WHERE pa.id_personal = pe.id_personal) AS areas
  FROM PERSONAL pe
  JOIN PERSONA p ON p.id_persona = pe.id_personal
  LEFT JOIN USUARIO u ON u.id_personal = pe.id_personal
  LEFT JOIN ROL r ON r.id_rol = u.id_rol
 WHERE p.numero_documento BETWEEN '70000001' AND '70000005'
 ORDER BY p.numero_documento;
PROMPT 15_datos_iteracion1.sql terminado.
