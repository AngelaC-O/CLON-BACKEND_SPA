-- =====================================================================
-- CAUTIVA SPA E.I.R.L. · BackOffice web · Base de datos Oracle (12c o superior)
-- Script 20 (v5) · Pruebas de las reglas de negocio (casos que DEBEN fallar y casos que DEBEN funcionar)
-- Se ejecuta como: dueño del esquema v5 (CAUTIVA5), después de 00_instalar_v5.sql.
--
-- Crea datos de prueba (sede, personal, usuarios, clientas...), prueba cada regla y al final hace
-- ROLLBACK: la base queda exactamente como estaba. Cada prueba imprime OK o FALLO.
-- Usa PKG_SESION.iniciar para actuar como recepcionista, almacén o Administradora, igual que el backend.
-- [v4] Incluye las pruebas de facturación electrónica, pasarela de pagos y consulta DNI/RUC (sección 10a).
-- [v5] Adaptado del script 20 de v4 (27/10/2026): las personas se crean en PERSONA y luego en su subtipo
--      (CLIENTE, PERSONAL), el historial de precios se comprueba en AUDITORIA, la sesión de paquete es
--      ATENCION.id_cliente_paquete, la nota de crédito es un COMPROBANTE NC_BOLETA más su fila en NOTA_CREDITO,
--      el stock solo vive en INVENTARIO_SEDE y la verificación del documento usa TIPO_DOCUMENTO.
-- [v5.2] (15/11/2026, 18_ajustes_v52.sql) Ya no hay tiempo de limpieza (RN03): los casos de la sección 3 que
--      dependían de ella pasan a la regla v5.2 y se marcan [v5.2]. Los demás casos no cambian.
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF

DECLARE
    v_ok    PLS_INTEGER := 0;
    v_fallo PLS_INTEGER := 0;

    -- Datos de prueba
    v_sede NUMBER; v_area NUMBER; v_area2 NUMBER; v_var NUMBER; v_var2 NUMBER;
    v_rol_adm NUMBER; v_rol_rec NUMBER; v_rol_esp NUMBER; v_rol_alm NUMBER;
    p_adm NUMBER; p_rec NUMBER; p_esp NUMBER; p_esp2 NUMBER; p_alm NUMBER;
    u_adm NUMBER; u_rec NUMBER; u_esp NUMBER; u_alm NUMBER;
    r1 NUMBER; r2 NUMBER; r3 NUMBER;
    c1 NUMBER; c2 NUMBER;
    v_caja NUMBER; v_efectivo NUMBER; v_yape NUMBER;
    v_prod NUMBER; v_insumo NUMBER; v_cat NUMBER;
    v_cita NUMBER; v_cita2 NUMBER; v_at NUMBER; v_at2 NUMBER; v_venta NUMBER; v_venta2 NUMBER; v_venta3 NUMBER;
    v_det NUMBER; v_comp NUMBER; v_paq NUMBER; v_cp NUMBER;
    v_n NUMBER; v_n2 NUMBER; v_txt VARCHAR2(200);
    v_manana VARCHAR2(80);
    v_bloq   VARCHAR2(1);
    v_tr NUMBER; v_tr2 NUMBER; v_venta4 NUMBER; v_comp4 NUMBER;   -- [v4]
    v_dni NUMBER; v_pas NUMBER; v_p NUMBER;                       -- [v5]

    PROCEDURE ok(p IN VARCHAR2) IS
    BEGIN
        v_ok := v_ok + 1;
        DBMS_OUTPUT.PUT_LINE('OK     ' || p);
    END;

    PROCEDURE fallo(p IN VARCHAR2) IS
    BEGIN
        v_fallo := v_fallo + 1;
        DBMS_OUTPUT.PUT_LINE('FALLO  ' || p);
    END;

    PROCEDURE como(p_usuario IN NUMBER) IS
    BEGIN
        IF p_usuario IS NULL THEN
            PKG_SESION.terminar;
        ELSE
            PKG_SESION.iniciar(p_usuario);
        END IF;
    END;

    PROCEDURE debe_fallar(p_nombre IN VARCHAR2, p_sql IN VARCHAR2, p_codigo IN NUMBER) IS
    BEGIN
        SAVEPOINT sp_prueba;
        EXECUTE IMMEDIATE p_sql;
        ROLLBACK TO sp_prueba;
        fallo(p_nombre || ' -> se esperaba el error ' || p_codigo || ' y la operación pasó');
    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK TO sp_prueba;
            IF SQLCODE = p_codigo THEN
                ok(p_nombre || ' -> ' || SUBSTR(REGEXP_REPLACE(SQLERRM, 'ORA-0[0-9]+: line.*', ''), 1, 140));
            ELSE
                fallo(p_nombre || ' -> se esperaba ' || p_codigo || ' y salió: ' || SUBSTR(SQLERRM, 1, 160));
            END IF;
    END;

    PROCEDURE debe_funcionar(p_nombre IN VARCHAR2, p_sql IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
        ok(p_nombre);
    EXCEPTION
        WHEN OTHERS THEN
            fallo(p_nombre || ' -> ' || SUBSTR(SQLERRM, 1, 200));
            RAISE;   -- una prueba "debe funcionar" que falla rompe las siguientes: se detiene
    END;

    PROCEDURE comprobar(p_nombre IN VARCHAR2, p_condicion IN BOOLEAN) IS
    BEGIN
        IF p_condicion THEN ok(p_nombre); ELSE fallo(p_nombre); END IF;
    END;

    -- [v5] Supertipo PERSONA: primero la persona, luego su subtipo con el mismo id
    FUNCTION persona(p_nombres IN VARCHAR2, p_apellidos IN VARCHAR2, p_dni IN VARCHAR2, p_telefono IN VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono)
        VALUES ('NATURAL', p_nombres, p_apellidos, CASE WHEN p_dni IS NULL THEN NULL ELSE v_dni END, p_dni, p_telefono)
        RETURNING id_persona INTO v;
        RETURN v;
    END;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== Preparando datos de prueba (modo mantenimiento) ===');
    PKG_SESION.terminar;
    v_manana := 'CAST(TRUNC(SYSDATE) + 1 AS TIMESTAMP)';

    SELECT id_rol INTO v_rol_adm FROM ROL WHERE nombre = 'ADMINISTRADORA';
    SELECT id_rol INTO v_rol_rec FROM ROL WHERE nombre = 'RECEPCIONISTA';
    SELECT id_rol INTO v_rol_esp FROM ROL WHERE nombre = 'ESPECIALISTA_TECNICA';
    SELECT id_rol INTO v_rol_alm FROM ROL WHERE nombre = 'ENCARGADA_ALMACEN';
    SELECT id_area INTO v_area  FROM AREA WHERE nombre = 'Peluquería y Estilismo';
    SELECT id_area INTO v_area2 FROM AREA WHERE nombre = 'Cabina Spa';
    SELECT v.id_variante INTO v_var FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio
     WHERE s.nombre = 'Corte de Cabello Caballero';
    SELECT v.id_variante INTO v_var2 FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio
     WHERE s.nombre LIKE 'Masaje Relajante%';
    SELECT id_medio_pago INTO v_efectivo FROM MEDIO_PAGO WHERE nombre = 'EFECTIVO';
    SELECT id_medio_pago INTO v_yape FROM MEDIO_PAGO WHERE nombre = 'YAPE';
    SELECT id_categoria INTO v_cat FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Capilar';
    SELECT id_tipo_documento INTO v_dni FROM TIPO_DOCUMENTO WHERE abreviatura = 'DNI';
    SELECT id_tipo_documento INTO v_pas FROM TIPO_DOCUMENTO WHERE abreviatura = 'PASAPORTE';

    INSERT INTO SEDE (nombre, direccion, horario_apertura, horario_cierre)
    VALUES ('SEDE DE PRUEBA', 'Calle de prueba 123', '09:00', '20:00') RETURNING id_sede INTO v_sede;
    -- [v4] Series de prueba de la sede (la BD numera los comprobantes con ellas)
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'BOLETA',     'B099');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'FACTURA',    'F099');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'NC_BOLETA',  'BC99');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'NC_FACTURA', 'FC99');

    p_adm := persona('Ada', 'Prueba', '90000001', NULL);
    INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
    VALUES (p_adm, v_sede, 'ADMINISTRADORA', 'FIJO', 3000, 0, SYSDATE);
    p_rec := persona('Rita', 'Prueba', '90000002', NULL);
    INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
    VALUES (p_rec, v_sede, 'RECEPCIONISTA', 'FIJO', 1500, 0, SYSDATE);
    p_esp := persona('Eva', 'Prueba', '90000003', NULL);
    INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
    VALUES (p_esp, v_sede, 'ESPECIALISTA_TECNICA', 'MIXTO', 1230, 10, SYSDATE);
    p_esp2 := persona('Sin', 'Area', '90000004', NULL);
    INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
    VALUES (p_esp2, v_sede, 'ESPECIALISTA_TECNICA', 'COMISION', 0, 20, SYSDATE);
    p_alm := persona('Alma', 'Prueba', '90000005', NULL);
    INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
    VALUES (p_alm, v_sede, 'ENCARGADA_ALMACEN', 'FIJO', 1300, 0, SYSDATE);
    INSERT INTO PERSONAL_AREA (id_personal, id_area) VALUES (p_esp, v_area);
    INSERT INTO PERSONAL_AREA (id_personal, id_area) VALUES (p_adm, v_area);

    INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash) VALUES (p_adm, v_rol_adm, 'prueba.adm', 'x') RETURNING id_usuario INTO u_adm;
    INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash) VALUES (p_rec, v_rol_rec, 'prueba.rec', 'x') RETURNING id_usuario INTO u_rec;
    INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash) VALUES (p_esp, v_rol_esp, 'prueba.esp', 'x') RETURNING id_usuario INTO u_esp;
    INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash) VALUES (p_alm, v_rol_alm, 'prueba.alm', 'x') RETURNING id_usuario INTO u_alm;

    INSERT INTO RECURSO (id_sede, nombre, tipo) VALUES (v_sede, 'Sillón P1', 'SILLON') RETURNING id_recurso INTO r1;
    INSERT INTO RECURSO (id_sede, nombre, tipo) VALUES (v_sede, 'Sillón P2', 'SILLON') RETURNING id_recurso INTO r2;
    INSERT INTO RECURSO (id_sede, nombre, tipo, estado) VALUES (v_sede, 'Sillón P3', 'SILLON', 'MANTENIMIENTO') RETURNING id_recurso INTO r3;

    c1 := persona('Clara', 'Prueba', NULL, '900000111');
    INSERT INTO CLIENTE (id_cliente) VALUES (c1);
    c2 := persona('Dora', 'Prueba', NULL, '900000222');
    INSERT INTO CLIENTE (id_cliente) VALUES (c2);

    INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
    VALUES (v_cat, 'PRU-001', 'Producto de prueba', 'UND', 'VENTA', 30, 20) RETURNING id_producto INTO v_prod;
    INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
    VALUES (v_cat, 'PRU-002', 'Insumo de prueba', 'UND', 'INSUMO', 10, 10) RETURNING id_producto INTO v_insumo;

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 1. Seguridad por rol, incidentes y bloqueo de cuentas (RN24, RN25, RN28, RN39, RF77) ===');
    como(u_rec);
    debe_fallar('Recepcionista cambia un precio del catálogo',
        'UPDATE VARIANTE_SERVICIO SET precio = 99 WHERE id_variante = ' || v_var, -20003);
    debe_fallar('Recepcionista crea un usuario',
        'INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash) VALUES (' || p_esp2 || ', ' || v_rol_rec || ', ''hack'', ''x'')', -20003);
    debe_fallar('Recepcionista se cambia el rol a Administradora',
        'UPDATE USUARIO SET id_rol = ' || v_rol_adm || ' WHERE id_usuario = ' || u_rec, -20003);
    debe_fallar('Recepcionista cambia la comisión de una especialista',
        'UPDATE PERSONAL SET porcentaje_comision = 50 WHERE id_personal = ' || p_esp, -20003);
    como(u_adm);
    debe_funcionar('Administradora cambia el precio del corte de 35 a 40',
        'UPDATE VARIANTE_SERVICIO SET precio = 40 WHERE id_variante = ' || v_var);
    SELECT COUNT(*) INTO v_n2 FROM AUDITORIA
     WHERE entidad = 'VARIANTE_SERVICIO' AND id_entidad = v_var AND detalle LIKE '%precio: 35 -> 40%';
    comprobar('[v5] El precio anterior y el nuevo quedaron en AUDITORIA (reemplaza a HISTORIAL_PRECIO)', v_n2 = 1);
    debe_fallar('Ni la Administradora elimina personal (se desactiva)',
        'DELETE FROM PERSONAL WHERE id_personal = ' || p_esp2, -20021);
    SELECT COUNT(*) INTO v_n FROM AUDITORIA WHERE entidad = 'VARIANTE_SERVICIO' AND id_entidad = v_var AND id_usuario = u_adm;
    comprobar('La auditoría registró quién cambió el precio (RN29)', v_n >= 1);
    debe_fallar('Nadie modifica la auditoría',
        'UPDATE AUDITORIA SET detalle = ''borrado'' WHERE id_usuario = ' || u_adm, -20020);
    debe_funcionar('Administradora registra un incidente de seguridad detectado hace 50 horas (RN28)',
        'INSERT INTO INCIDENTE_SEGURIDAD (fecha_deteccion, descripcion, datos_afectados, id_usuario) VALUES ('
        || 'CAST(SYSTIMESTAMP AS TIMESTAMP) - INTERVAL ''50'' HOUR, ''Prueba: se perdió una hoja con teléfonos'', ''Teléfonos de 30 clientas'', ' || u_adm || ')');
    SELECT COUNT(*) INTO v_n FROM V_INCIDENTES_PLAZO WHERE id_usuario = u_adm AND fuera_de_plazo = 'S';
    comprobar('Sin aviso a las 50 horas, el incidente aparece fuera del plazo de 48 h', v_n = 1);
    debe_fallar('Marcarlo AVISADO sin número de aviso',
        'UPDATE INCIDENTE_SEGURIDAD SET estado = ''AVISADO'', fecha_aviso = CAST(SYSTIMESTAMP AS TIMESTAMP) WHERE id_usuario = ' || u_adm, -2290);
    como(u_rec);
    debe_fallar('Recepcionista registra un incidente (solo la Administradora)',
        'INSERT INTO INCIDENTE_SEGURIDAD (fecha_deteccion, descripcion, datos_afectados, id_usuario) VALUES ('
        || 'CAST(SYSTIMESTAMP AS TIMESTAMP), ''x'', ''x'', ' || u_rec || ')', -20003);

    -- Inicio de sesión: 5 intentos fallidos bloquean la cuenta (RF77)
    como(NULL);   -- el login ocurre antes de iniciar sesión
    FOR i IN 1 .. 4 LOOP
        UPDATE USUARIO SET intentos_fallidos = intentos_fallidos + 1 WHERE id_usuario = u_alm;
    END LOOP;
    SELECT estado INTO v_txt FROM USUARIO WHERE id_usuario = u_alm;
    comprobar('Con 4 intentos fallidos la cuenta sigue ACTIVA', v_txt = 'ACTIVO');
    UPDATE USUARIO SET intentos_fallidos = intentos_fallidos + 1 WHERE id_usuario = u_alm;
    SELECT estado, CASE WHEN bloqueado_hasta > CAST(SYSTIMESTAMP AS TIMESTAMP) THEN 'S' ELSE 'N' END
      INTO v_txt, v_bloq FROM USUARIO WHERE id_usuario = u_alm;
    comprobar('Al 5.º intento la BD la bloquea 30 minutos (RF77)', v_txt = 'BLOQUEADO' AND v_bloq = 'S');
    debe_fallar('Un usuario bloqueado no opera en la BD',
        'BEGIN PKG_SESION.iniciar(' || u_alm || '); END;', -20004);
    como(u_rec);
    debe_fallar('La recepcionista desbloquea la cuenta antes de tiempo',
        'UPDATE USUARIO SET estado = ''ACTIVO'' WHERE id_usuario = ' || u_alm, -20003);
    como(u_adm);
    debe_funcionar('La Administradora la desbloquea',
        'UPDATE USUARIO SET estado = ''ACTIVO'' WHERE id_usuario = ' || u_alm);
    SELECT intentos_fallidos INTO v_n FROM USUARIO WHERE id_usuario = u_alm;
    comprobar('Al desbloquear, los intentos vuelven a 0', v_n = 0);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 2. Caja (RF62, RN30) ===');
    como(u_rec);
    INSERT INTO CAJA (id_sede, id_usuario_apertura, monto_inicial) VALUES (v_sede, u_rec, 100) RETURNING id_caja INTO v_caja;
    ok('Recepcionista abre la caja con S/ 100');
    debe_fallar('Una segunda caja ABIERTA en la misma sede',
        'INSERT INTO CAJA (id_sede, id_usuario_apertura, monto_inicial) VALUES (' || v_sede || ', ' || u_rec || ', 50)', -1);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 3. Agenda sin cruces (RN02, RN03, RN34, RN41, RF83) ===');
    debe_funcionar('Cita mañana 10:00, 30 min, especialista Eva, Sillón P1',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c1 || ', ' || v_sede || ', ' || p_esp || ', ' || r1 || ', ' || v_var || ', ' || v_manana || ' + INTERVAL ''10'' HOUR, 30)');
    SELECT MAX(id_cita) INTO v_cita FROM CITA WHERE id_recurso = r1;
    SELECT TO_CHAR(fecha_hora_fin, 'HH24:MI') INTO v_txt FROM CITA WHERE id_cita = v_cita;
    comprobar('[v5.2] El fin es el del servicio: 10:00 + 30 = 10:30, sin limpieza (RN03) -> ' || v_txt, v_txt = '10:30');
    SAVEPOINT sp_v52;
    debe_funcionar('[v5.2] Misma especialista a las 10:30 en otro sillón (queda libre al terminar, RN03)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c2 || ', ' || v_sede || ', ' || p_esp || ', ' || r2 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(630, ''MINUTE''), 30)');
    ROLLBACK TO sp_v52;
    debe_funcionar('[v5.2] Mismo sillón a las 10:30 con otra especialista (sin limpieza, RN03)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c2 || ', ' || v_sede || ', ' || p_adm || ', ' || r1 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(630, ''MINUTE''), 30)');
    ROLLBACK TO sp_v52;
    debe_funcionar('[v5.2] Misma especialista a las 10:30 en el mismo sillón (justo al terminar el servicio)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c2 || ', ' || v_sede || ', ' || p_esp || ', ' || r1 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(630, ''MINUTE''), 30)');
    debe_fallar('[v5.2] La especialista declara 60 min en la cita de las 10:00 y choca con la de las 10:30 (RN41: se reprograma primero)',
        'INSERT INTO ATENCION (id_cita, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES ('
        || v_cita || ', ' || p_esp || ', ' || r1 || ', ' || v_var || ', ' || v_manana || ' + INTERVAL ''10'' HOUR, 60)', -20010);
    SAVEPOINT sp_rn41;
    debe_funcionar('La atención de la cita con sus 30 min no choca con su propia cita',
        'INSERT INTO ATENCION (id_cita, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES ('
        || v_cita || ', ' || p_esp || ', ' || r1 || ', ' || v_var || ', ' || v_manana || ' + INTERVAL ''10'' HOUR, 30)');
    ROLLBACK TO sp_rn41;
    debe_fallar('Cita que termina después del cierre (19:45 + 30 min)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c1 || ', ' || v_sede || ', ' || p_esp || ', ' || r2 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(1185, ''MINUTE''), 30)', -20014);
    debe_fallar('Especialista sin el área del servicio (RF83)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c1 || ', ' || v_sede || ', ' || p_esp2 || ', ' || r2 || ', ' || v_var || ', ' || v_manana || ' + INTERVAL ''15'' HOUR, 30)', -20012);
    debe_fallar('Recurso en mantenimiento',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c1 || ', ' || v_sede || ', ' || p_esp || ', ' || r3 || ', ' || v_var || ', ' || v_manana || ' + INTERVAL ''16'' HOUR, 30)', -20017);
    debe_fallar('Atención sin cita que choca con una cita agendada (RN34)',
        'INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES ('
        || c2 || ', ' || p_esp || ', ' || r2 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(615, ''MINUTE''), 30)', -20010);
    debe_fallar('Cliente de paso sin nombre (RN35)',
        'INSERT INTO ATENCION (id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES ('
        || p_esp || ', ' || r2 || ', ' || v_var || ', SYSTIMESTAMP - INTERVAL ''3'' HOUR, 30)', -2290);
    debe_fallar('Eliminar una cita (se cancela)',
        'DELETE FROM CITA WHERE id_cita = ' || v_cita, -20021);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 4. Adelanto, cancelación y saldo a favor (RN04, RN05) ===');
    debe_fallar('[v4] Adelanto con Yape al agendar, sin cobro de la pasarela (RN42)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min, adelanto_monto, id_medio_pago, fecha_adelanto, id_caja) VALUES ('
        || c1 || ', ' || v_sede || ', ' || p_esp || ', ' || r2 || ', ' || v_var || ', ' || v_manana || ' + INTERVAL ''14'' HOUR, 30, 20, '
        || v_yape || ', SYSTIMESTAMP, ' || v_caja || ')', -20070);
    debe_funcionar('Agendar la cita de mañana 14:00 (el adelanto se paga con enlace de pago)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c1 || ', ' || v_sede || ', ' || p_esp || ', ' || r2 || ', ' || v_var || ', ' || v_manana || ' + INTERVAL ''14'' HOUR, 30)');
    SELECT MAX(id_cita) INTO v_cita2 FROM CITA WHERE id_recurso = r2;
    INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, id_cita, monto, estado, id_usuario, enlace_pago, fecha_expiracion)
    VALUES (v_sede, v_yape, 'LINK', v_cita2, 20, 'PENDIENTE', u_rec, 'https://pago.prueba/cautiva/abc',
            CAST(SYSTIMESTAMP AS TIMESTAMP) + INTERVAL '24' HOUR) RETURNING id_transaccion INTO v_tr;
    ok('[v4] Se genera el enlace de pago del adelanto de S/ 20 (RF104)');
    debe_fallar('[v4] Registrar el adelanto con el enlace aún PENDIENTE',
        'UPDATE CITA SET adelanto_monto = 20, id_medio_pago = ' || v_yape || ', fecha_adelanto = SYSTIMESTAMP, id_caja = ' || v_caja
        || ' WHERE id_cita = ' || v_cita2, -20070);
    UPDATE TRANSACCION_PASARELA SET estado = 'APROBADA', codigo_operacion = 'PRU-LINK-001', fecha_respuesta = SYSTIMESTAMP
     WHERE id_transaccion = v_tr;
    debe_funcionar('[v4] La pasarela aprueba el enlace: el adelanto de S/ 20 asegura la cita (RN04)',
        'UPDATE CITA SET adelanto_monto = 20, id_medio_pago = ' || v_yape || ', fecha_adelanto = SYSTIMESTAMP, id_caja = ' || v_caja
        || ' WHERE id_cita = ' || v_cita2);
    debe_fallar('Abono de saldo con la cita aún PENDIENTE',
        'INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_cita, tipo, monto) VALUES (' || c1 || ', ' || v_cita2 || ', ''ABONO'', 20)', -20063);
    debe_fallar('Cancelar sin motivo',
        'UPDATE CITA SET estado = ''CANCELADA'' WHERE id_cita = ' || v_cita2, -2290);
    debe_funcionar('Cancelar la cita con motivo',
        'UPDATE CITA SET estado = ''CANCELADA'', motivo_cancelacion = ''Viaje'' WHERE id_cita = ' || v_cita2);
    debe_fallar('Abono mayor que el adelanto',
        'INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_cita, tipo, monto) VALUES (' || c1 || ', ' || v_cita2 || ', ''ABONO'', 25)', -20065);
    debe_funcionar('El adelanto pasa a saldo a favor (S/ 20)',
        'INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_cita, tipo, monto) VALUES (' || c1 || ', ' || v_cita2 || ', ''ABONO'', 20)');
    debe_fallar('Reabrir una cita cancelada',
        'UPDATE CITA SET estado = ''PENDIENTE'' WHERE id_cita = ' || v_cita2, -20013);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 5. Inventario y kardex (RN13-RN18) ===');
    como(u_rec);
    debe_fallar('Recepcionista registra una entrada de inventario (RN17)',
        'BEGIN PKG_INVENTARIO.registrar(' || v_prod || ', ' || v_sede || ', ''ENTRADA'', ''prueba'', 10); END;', -20031);
    como(u_alm);
    debe_funcionar('Almacén registra la entrada inicial de 10 unidades',
        'BEGIN PKG_INVENTARIO.registrar(' || v_prod || ', ' || v_sede || ', ''ENTRADA'', ''Stock inicial'', 10); END;');
    SELECT stock_actual INTO v_n FROM INVENTARIO_SEDE WHERE id_producto = v_prod AND id_sede = v_sede;
    comprobar('[v5] Stock de la sede = 10 (el stock solo vive en INVENTARIO_SEDE)', v_n = 10);
    debe_fallar('Salida mayor al stock (nunca negativo)',
        'BEGIN PKG_INVENTARIO.registrar(' || v_prod || ', ' || v_sede || ', ''SALIDA'', ''prueba'', 11); END;', -20030);
    debe_fallar('Cambiar el stock a mano',
        'UPDATE INVENTARIO_SEDE SET stock_actual = 999 WHERE id_producto = ' || v_prod || ' AND id_sede = ' || v_sede, -20032);
    debe_fallar('Borrar un movimiento de kardex',
        'DELETE FROM MOVIMIENTO_KARDEX WHERE id_producto = ' || v_prod, -20020);
    debe_fallar('Almacén cambia el precio de venta (RN24)',
        'UPDATE PRODUCTO SET precio_venta = 50 WHERE id_producto = ' || v_prod, -20003);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 6. Atención sin cita, cobro y comprobante (RN06-RN11, RN38) ===');
    como(u_esp);
    INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min)
    VALUES (c1, p_esp, r2, v_var, SYSTIMESTAMP - INTERVAL '3' HOUR, 30) RETURNING id_atencion INTO v_at;
    ok('Especialista registra una atención sin cita');
    como(u_rec);
    INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total)
    VALUES (v_sede, v_caja, c1, u_rec, 0, 0, 0) RETURNING id_venta INTO v_venta;
    debe_fallar('Cobrar una atención que sigue EN_CURSO',
        'INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_atencion, cantidad, precio_unitario, subtotal) VALUES ('
        || v_venta || ', ''SERVICIO'', ' || v_at || ', 1, 0, 0)', -20046);
    UPDATE ATENCION SET estado = 'FINALIZADA' WHERE id_atencion = v_at;
    debe_funcionar('Línea de servicio con precio digitado 1 (la BD pone el del catálogo)',
        'INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_atencion, cantidad, precio_unitario, subtotal) VALUES ('
        || v_venta || ', ''SERVICIO'', ' || v_at || ', 1, 1, 1)');
    SELECT id_detalle, precio_unitario, monto_comision INTO v_det, v_n, v_n2 FROM VENTA_DETALLE WHERE id_venta = v_venta;
    comprobar('Precio fijo S/ 40 y comisión 10 % = S/ 4 calculados por la BD (RN25, RN38) -> ' || v_n || ' / ' || v_n2, v_n = 40 AND v_n2 = 4);
    -- Importe 40, exonerado de IGV (RN06): subtotal 40 + IGV 0
    debe_fallar('Cerrar con totales que no cuadran',
        'UPDATE VENTA SET subtotal = 35, igv = 0, total = 35, estado = ''CERRADA'' WHERE id_venta = ' || v_venta, -20051);
    debe_fallar('Cobrar IGV 18 % en una venta exonerada (RN06)',
        'UPDATE VENTA SET subtotal = 33.90, igv = 6.10, total = 40, estado = ''CERRADA'' WHERE id_venta = ' || v_venta, -20051);
    INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto) VALUES (v_venta, v_efectivo, 30);
    debe_fallar('Cerrar con pagos que no suman el total (RN11)',
        'UPDATE VENTA SET subtotal = 40, igv = 0, total = 40, estado = ''CERRADA'' WHERE id_venta = ' || v_venta, -20054);
    debe_fallar('[v4] Pago con Yape sin transacción de la pasarela (RN42)',
        'INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto) VALUES (' || v_venta || ', ' || v_yape || ', 10)', -20071);
    INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, monto, estado, codigo_operacion, fecha_respuesta, id_usuario)
    VALUES (v_sede, v_yape, 'QR', 10, 'APROBADA', 'PRU-QR-123', SYSTIMESTAMP, u_rec) RETURNING id_transaccion INTO v_tr2;
    INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto, id_transaccion) VALUES (v_venta, v_yape, 10, v_tr2);
    SELECT referencia INTO v_txt FROM VENTA_PAGO WHERE id_transaccion = v_tr2;
    comprobar('[v4] Pago con Yape respaldado por la pasarela; la referencia la copia la BD -> ' || v_txt, v_txt = 'PRU-QR-123');
    debe_fallar('Cerrar sin comprobante electrónico (RN10)',
        'UPDATE VENTA SET subtotal = 40, igv = 0, total = 40, estado = ''CERRADA'' WHERE id_venta = ' || v_venta, -20055);
    debe_fallar('Factura sin RUC del comprador (RN08)',
        'INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (' || v_venta || ', ''FACTURA'', ''F999'', ''1'', SYSDATE)', -20098);
    debe_fallar('[v5] Factura con DNI en lugar de RUC (RN08)',
        'INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision, id_tipo_doc_comprador, nro_doc_comprador, nombre_comprador) VALUES ('
        || v_venta || ', ''FACTURA'', ''F999'', ''1'', SYSDATE, ' || v_dni || ', ''45678912'', ''Clara Prueba'')', -20098);
    INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (v_venta, 'BOLETA', 'B999', '99999901', SYSDATE)
    RETURNING id_comprobante INTO v_comp;
    SELECT serie || '-' || numero || ' ' || estado_sunat INTO v_txt FROM COMPROBANTE WHERE id_comprobante = v_comp;
    comprobar('[v4] La BD asignó la serie y el número (se envió B999-99999901, quedó ' || v_txt || ') (RN47)', v_txt = 'B099-1 PENDIENTE');
    debe_funcionar('Cerrar la venta: comprobante y pagos completos, IGV 0',
        'UPDATE VENTA SET subtotal = 40, igv = 0, total = 40, estado = ''CERRADA'' WHERE id_venta = ' || v_venta);
    SELECT estado INTO v_txt FROM ATENCION WHERE id_atencion = v_at;
    comprobar('La atención quedó COBRADA', v_txt = 'COBRADA');
    debe_fallar('Modificar el total de una venta cerrada',
        'UPDATE VENTA SET total = 1 WHERE id_venta = ' || v_venta, -20044);
    debe_fallar('Agregar una línea a una venta cerrada',
        'INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, subtotal) VALUES ('
        || v_venta || ', ''PRODUCTO'', ' || v_prod || ', 1, 30, 30)', -20045);
    debe_fallar('Eliminar una venta cerrada (RN12)',
        'DELETE FROM VENTA WHERE id_venta = ' || v_venta, -20041);
    debe_fallar('Volver a cobrar la atención dentro de la venta ya cerrada',
        'INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_atencion, cantidad, precio_unitario, subtotal) SELECT id_venta, ''SERVICIO'', ' || v_at
        || ', 1, 0, 0 FROM VENTA WHERE id_venta = ' || v_venta, -20045);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 7. Anulación con nota de crédito (RN12, RN32, RN33) ===');
    debe_fallar('Anular sin nota de crédito',
        'UPDATE VENTA SET estado = ''ANULADA'' WHERE id_venta = ' || v_venta, -20058);
    debe_fallar('[v4] Nota de crédito con el comprobante aún PENDIENTE en SUNAT',
        'INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (' || v_venta || ', ''NC_BOLETA'', ''X'', ''X'', SYSDATE)', -20076);
    UPDATE COMPROBANTE
       SET estado_sunat = 'ACEPTADO', intentos_envio = 1, fecha_envio = SYSTIMESTAMP, fecha_respuesta = SYSTIMESTAMP,
           codigo_respuesta = '0', descripcion_respuesta = 'La Boleta numero B099-1, ha sido aceptada', ruta_cdr = 'cdr/R-B099-1.zip'
     WHERE id_comprobante = v_comp;
    ok('[v4] SUNAT acepta la boleta con la venta ya cerrada: solo cambia el seguimiento del envío (RF100)');
    debe_fallar('[v5] NC_FACTURA para una venta que tiene boleta (NC_BOLETA anula BOLETA)',
        'INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (' || v_venta || ', ''NC_FACTURA'', ''X'', ''X'', SYSDATE)', -20058);
    debe_fallar('[v5] Fila de NOTA_CREDITO colgada de la BOLETA (debe ser un comprobante NC_*)',
        'INSERT INTO NOTA_CREDITO (id_nota_credito, motivo, id_usuario_solicita, id_usuario_autoriza) VALUES ('
        || v_comp || ', ''Error de digitación'', ' || u_rec || ', ' || u_adm || ')', -20094);
    debe_fallar('Nota de crédito autorizada por la recepcionista',
        'DECLARE c NUMBER; BEGIN INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (' || v_venta
        || ', ''NC_BOLETA'', ''X'', ''X'', SYSDATE) RETURNING id_comprobante INTO c; '
        || 'INSERT INTO NOTA_CREDITO (id_nota_credito, motivo, id_usuario_solicita, id_usuario_autoriza) VALUES (c, ''Error de digitación'', '
        || u_rec || ', ' || u_rec || '); END;', -20058);
    debe_funcionar('Nota de crédito autorizada por la Administradora (COMPROBANTE NC_BOLETA + NOTA_CREDITO)',
        'DECLARE c NUMBER; BEGIN INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (' || v_venta
        || ', ''NC_BOLETA'', ''X'', ''X'', SYSDATE) RETURNING id_comprobante INTO c; '
        || 'INSERT INTO NOTA_CREDITO (id_nota_credito, motivo, id_usuario_solicita, id_usuario_autoriza) VALUES (c, ''Error de digitación'', '
        || u_rec || ', ' || u_adm || '); END;');
    debe_fallar('[v5] Segunda nota de crédito para la misma venta (índice UQ_COMPROBANTE_VENTA_CLASE)',
        'INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (' || v_venta || ', ''NC_BOLETA'', ''X'', ''X'', SYSDATE)', -1);
    debe_funcionar('Anular la venta',
        'UPDATE VENTA SET estado = ''ANULADA'' WHERE id_venta = ' || v_venta);
    SELECT estado INTO v_txt FROM ATENCION WHERE id_atencion = v_at;
    comprobar('La atención volvió a FINALIZADA para cobrarse bien', v_txt = 'FINALIZADA');
    SELECT COUNT(*) INTO v_n FROM V_VENTA_VALIDA WHERE id_venta = v_venta;
    comprobar('La venta anulada no aparece en los reportes (RN33)', v_n = 0);
    SELECT serie || '-' || numero || ' ' || NVL(nro_doc_comprador, '-') INTO v_txt
      FROM COMPROBANTE WHERE id_venta = v_venta AND tipo = 'NC_BOLETA';
    comprobar('[v4] La nota de crédito tomó su propia serie: ' || v_txt, v_txt = 'BC99-1 -');
    debe_fallar('[v5] Eliminar la nota de crédito emitida',
        'DELETE FROM COMPROBANTE WHERE id_venta = ' || v_venta || ' AND tipo = ''NC_BOLETA''', -20020);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 8. Venta de productos, stock y saldo a favor (RN13, RN15, RF39, RN05) ===');
    INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total)
    VALUES (v_sede, v_caja, c1, u_rec, 0, 0, 0) RETURNING id_venta INTO v_venta2;
    debe_fallar('Vender un insumo de uso interno',
        'INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, subtotal) VALUES ('
        || v_venta2 || ', ''PRODUCTO'', ' || v_insumo || ', 1, 10, 10)', -20048);
    debe_fallar('Vender 11 unidades con 10 en stock: no se vende sin stock (RN15)',
        'INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, subtotal) VALUES ('
        || v_venta2 || ', ''PRODUCTO'', ' || v_prod || ', 11, 30, 330)', -20035);
    INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, subtotal)
    VALUES (v_venta2, 'PRODUCTO', v_prod, 3, 30, 90);
    debe_fallar('Usar más saldo a favor del que tiene (tiene S/ 20)',
        'INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_venta, tipo, monto) VALUES (' || c1 || ', ' || v_venta2 || ', ''USO'', 25)', -20064);
    INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_venta, tipo, monto) VALUES (c1, v_venta2, 'USO', 20);
    -- Importe 90 (exonerado de IGV); saldo aplicado 20; total a pagar 70
    INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto) VALUES (v_venta2, v_efectivo, 70);
    INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (v_venta2, 'BOLETA', 'B999', '99999903', SYSDATE);
    debe_funcionar('Cerrar la venta de 3 productos con S/ 20 de saldo a favor',
        'UPDATE VENTA SET subtotal = 90, igv = 0, saldo_favor_aplicado = 20, total = 70, estado = ''CERRADA'' WHERE id_venta = ' || v_venta2);
    SELECT stock_actual INTO v_n FROM INVENTARIO_SEDE WHERE id_producto = v_prod AND id_sede = v_sede;
    comprobar('El cierre descontó 3 unidades del kardex: stock 10 -> 7 (RF39)', v_n = 7);
    SELECT saldo INTO v_n FROM V_SALDO_FAVOR_CLIENTE WHERE id_cliente = c1;
    comprobar('El saldo a favor de la clienta quedó en 0', v_n = 0);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 9. Paquetes y sesiones (RN36, RN38) ===');
    como(u_adm);
    INSERT INTO PAQUETE (nombre, tipo, num_sesiones, precio) VALUES ('Paquete prueba 1 corte', 'PAQUETE', 1, 30) RETURNING id_paquete INTO v_paq;
    INSERT INTO PAQUETE_SERVICIO (id_paquete, id_servicio) SELECT v_paq, id_servicio FROM VARIANTE_SERVICIO WHERE id_variante = v_var;
    como(u_rec);
    debe_fallar('Vender un paquete a un cliente de paso',
        'DECLARE v NUMBER; BEGIN INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total) VALUES ('
        || v_sede || ', ' || v_caja || ', NULL, ' || u_rec || ', 0, 0, 0) RETURNING id_venta INTO v; '
        || 'INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_paquete, cantidad) VALUES (v, ''PAQUETE'', ' || v_paq || ', 1); END;', -20049);
    debe_funcionar('La clienta compra el paquete',
        'DECLARE v NUMBER; d NUMBER; BEGIN '
        || 'INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total) VALUES (' || v_sede || ', ' || v_caja || ', ' || c2 || ', ' || u_rec || ', 0, 0, 0) RETURNING id_venta INTO v; '
        || 'INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_paquete, cantidad, precio_unitario, subtotal) VALUES (v, ''PAQUETE'', ' || v_paq || ', 1, 30, 30) RETURNING id_detalle INTO d; '
        || 'INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto) VALUES (v, ' || v_efectivo || ', 30); '
        || 'INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (v, ''BOLETA'', ''B999'', ''99999904'', SYSDATE); '
        || 'UPDATE VENTA SET subtotal = 30, igv = 0, total = 30, estado = ''CERRADA'' WHERE id_venta = v; '
        || 'INSERT INTO CLIENTE_PAQUETE (id_cliente, id_paquete, id_detalle, sesiones_totales, fecha_compra) VALUES (' || c2 || ', ' || v_paq || ', d, 99, SYSDATE); END;');
    SELECT id_cliente_paquete, sesiones_totales INTO v_cp, v_n FROM CLIENTE_PAQUETE WHERE id_paquete = v_paq;
    comprobar('Las sesiones las calcula la BD (se envió 99, quedó ' || v_n || ')', v_n = 1);
    como(u_esp);
    INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min)
    VALUES (c2, p_esp, r2, v_var, SYSTIMESTAMP - INTERVAL '5' HOUR, 30) RETURNING id_atencion INTO v_at2;
    UPDATE ATENCION SET estado = 'FINALIZADA', hora_fin = SYSTIMESTAMP - NUMTODSINTERVAL(270, 'MINUTE') WHERE id_atencion = v_at2;
    como(u_rec);
    debe_fallar('Usar el paquete de Dora en la atención de Clara',
        'UPDATE ATENCION SET id_cliente_paquete = ' || v_cp || ' WHERE id_atencion = ' || v_at, -20068);
    debe_funcionar('[v5] Dora usa su sesión (ATENCION.id_cliente_paquete)',
        'UPDATE ATENCION SET id_cliente_paquete = ' || v_cp || ' WHERE id_atencion = ' || v_at2);
    SELECT estado INTO v_txt FROM CLIENTE_PAQUETE WHERE id_cliente_paquete = v_cp;
    comprobar('El paquete quedó AGOTADO', v_txt = 'AGOTADO');
    SELECT sesiones_usadas INTO v_n FROM V_SESIONES_PAQUETE WHERE id_cliente_paquete = v_cp;
    comprobar('[v5] V_SESIONES_PAQUETE cuenta 1 sesión usada', v_n = 1);
    INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total)
    VALUES (v_sede, v_caja, c2, u_rec, 0, 0, 0) RETURNING id_venta INTO v_venta3;
    INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_atencion, cantidad, precio_unitario, subtotal) VALUES (v_venta3, 'SERVICIO', v_at2, 1, 0, 0);
    SELECT precio_unitario, monto_comision INTO v_n, v_n2 FROM VENTA_DETALLE WHERE id_venta = v_venta3;
    comprobar('Servicio cubierto por sesión: precio 0 y sin comisión (RN38)', v_n = 0 AND v_n2 = 0);
    DELETE FROM VENTA_DETALLE WHERE id_venta = v_venta3;
    DELETE FROM VENTA WHERE id_venta = v_venta3;
    ok('Una venta ABIERTA se puede descartar');

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 10a. [v4] Facturación electrónica, pasarela de pagos y consulta DNI/RUC ===');
    como(u_rec);
    SELECT serie || '-' || numero INTO v_txt FROM COMPROBANTE WHERE id_venta = v_venta2;
    comprobar('Correlativo sin saltos: la segunda boleta es B099-2 -> ' || v_txt, v_txt = 'B099-2');
    debe_fallar('Cambiar el número de un comprobante (RN47)',
        'UPDATE COMPROBANTE SET numero = ''5'' WHERE id_comprobante = ' || v_comp, -20096);
    debe_fallar('Volver un comprobante ACEPTADO a PENDIENTE (RF100)',
        'UPDATE COMPROBANTE SET estado_sunat = ''PENDIENTE'' WHERE id_comprobante = ' || v_comp, -20075);
    debe_fallar('Marcar ACEPTADO sin la constancia (CDR) de SUNAT',
        'UPDATE COMPROBANTE SET estado_sunat = ''ACEPTADO'', codigo_respuesta = ''0'', fecha_respuesta = SYSTIMESTAMP WHERE id_venta = ' || v_venta2, -2290);
    debe_fallar('Eliminar un comprobante ya enviado (RN12)',
        'DELETE FROM COMPROBANTE WHERE id_comprobante = ' || v_comp, -20073);
    SELECT COUNT(*) INTO v_n FROM V_CPE_PENDIENTES WHERE id_venta = v_venta2;
    comprobar('La boleta aún no aceptada aparece en V_CPE_PENDIENTES con su fecha límite (RF101)', v_n = 1);
    debe_fallar('Recepcionista ajusta el correlativo de una serie (RF107)',
        'UPDATE SERIE_COMPROBANTE SET ultimo_numero = 0 WHERE serie = ''B099''', -20003);
    debe_fallar('Cobrar efectivo por la pasarela (RN42)',
        'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, monto, id_usuario) VALUES ('
        || v_sede || ', ' || v_efectivo || ', ''POS'', 10, ' || u_rec || ')', -20072);
    debe_fallar('Pago con Yape respaldado por una transacción RECHAZADA (RN42)',
        'DECLARE v NUMBER; t NUMBER; BEGIN '
        || 'INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total) VALUES (' || v_sede || ', ' || v_caja || ', ' || c1 || ', ' || u_rec || ', 0, 0, 0) RETURNING id_venta INTO v; '
        || 'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, monto, estado, mensaje_respuesta, id_usuario) VALUES (' || v_sede || ', ' || v_yape || ', ''QR'', 15, ''RECHAZADA'', ''Operación rechazada'', ' || u_rec || ') RETURNING id_transaccion INTO t; '
        || 'INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto, id_transaccion) VALUES (v, ' || v_yape || ', 15, t); END;', -20071);
    debe_fallar('Pago con Yape por un monto distinto al aprobado',
        'DECLARE v NUMBER; t NUMBER; BEGIN '
        || 'INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total) VALUES (' || v_sede || ', ' || v_caja || ', ' || c1 || ', ' || u_rec || ', 0, 0, 0) RETURNING id_venta INTO v; '
        || 'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, monto, estado, codigo_operacion, fecha_respuesta, id_usuario) VALUES (' || v_sede || ', ' || v_yape || ', ''QR'', 15, ''APROBADA'', ''PRU-QR-999'', SYSTIMESTAMP, ' || u_rec || ') RETURNING id_transaccion INTO t; '
        || 'INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto, id_transaccion) VALUES (v, ' || v_yape || ', 20, t); END;', -20071);
    debe_fallar('Usar dos veces la misma transacción aprobada',
        'DECLARE v NUMBER; BEGIN '
        || 'INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total) VALUES (' || v_sede || ', ' || v_caja || ', ' || c1 || ', ' || u_rec || ', 0, 0, 0) RETURNING id_venta INTO v; '
        || 'INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto, id_transaccion) VALUES (v, ' || v_yape || ', 10, ' || v_tr2 || '); END;', -1);
    debe_fallar('Pago en efectivo con transacción de la pasarela',
        'DECLARE v NUMBER; BEGIN '
        || 'INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total) VALUES (' || v_sede || ', ' || v_caja || ', ' || c1 || ', ' || u_rec || ', 0, 0, 0) RETURNING id_venta INTO v; '
        || 'INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto, id_transaccion) VALUES (v, ' || v_efectivo || ', 10, ' || v_tr2 || '); END;', -20071);
    debe_fallar('Cambiar el monto de una transacción de la pasarela',
        'UPDATE TRANSACCION_PASARELA SET monto = 1 WHERE id_transaccion = ' || v_tr2, -20072);
    debe_fallar('Eliminar una transacción de la pasarela',
        'DELETE FROM TRANSACCION_PASARELA WHERE id_transaccion = ' || v_tr2, -20021);

    SELECT COUNT(*) INTO v_n FROM V_DEVOLUCIONES_PENDIENTES WHERE id_venta = v_venta;
    comprobar('Los S/ 10 de Yape de la venta anulada quedan pendientes de devolución (RN44)', v_n = 1);
    debe_fallar('Recepcionista devuelve un pago por la pasarela (RN44)',
        'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, tipo_operacion, canal, id_transaccion_origen, monto, id_usuario) VALUES ('
        || v_sede || ', ' || v_yape || ', ''DEVOLUCION'', ''API'', ' || v_tr2 || ', 10, ' || u_rec || ')', -20003);
    como(u_adm);
    debe_fallar('Devolver más de lo cobrado (RN44)',
        'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, tipo_operacion, canal, id_transaccion_origen, monto, id_usuario) VALUES ('
        || v_sede || ', ' || v_yape || ', ''DEVOLUCION'', ''API'', ' || v_tr2 || ', 15, ' || u_adm || ')', -20077);
    debe_funcionar('La Administradora devuelve los S/ 10 de Yape por la pasarela (RF106)',
        'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, tipo_operacion, canal, id_transaccion_origen, id_nota_credito, monto, estado, codigo_operacion, fecha_respuesta, id_usuario) VALUES ('
        || v_sede || ', ' || v_yape || ', ''DEVOLUCION'', ''API'', ' || v_tr2 || ', (SELECT nc.id_nota_credito FROM NOTA_CREDITO nc JOIN COMPROBANTE c ON c.id_comprobante = nc.id_nota_credito WHERE c.id_venta = ' || v_venta
        || '), 10, ''APROBADA'', ''PRU-DEV-001'', SYSTIMESTAMP, ' || u_adm || ')');
    SELECT COUNT(*) INTO v_n FROM V_DEVOLUCIONES_PENDIENTES WHERE id_venta = v_venta;
    comprobar('Con la devolución aprobada no queda nada pendiente', v_n = 0);

    -- La clienta elige saldo a favor en lugar de la devolución (RN44)
    como(u_rec);
    INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total)
    VALUES (v_sede, v_caja, c2, u_rec, 0, 0, 0) RETURNING id_venta INTO v_venta4;
    INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, subtotal) VALUES (v_venta4, 'PRODUCTO', v_prod, 1, 30, 30);
    INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto) VALUES (v_venta4, v_efectivo, 30);
    INSERT INTO COMPROBANTE (id_venta, tipo) VALUES (v_venta4, 'BOLETA') RETURNING id_comprobante INTO v_comp4;
    UPDATE VENTA SET subtotal = 30, igv = 0, total = 30, estado = 'CERRADA' WHERE id_venta = v_venta4;
    SELECT serie || '-' || numero INTO v_txt FROM COMPROBANTE WHERE id_comprobante = v_comp4;
    comprobar('Boleta electrónica generada sin digitar serie ni número -> ' || v_txt, v_txt = 'B099-4');
    UPDATE COMPROBANTE
       SET estado_sunat = 'ACEPTADO', intentos_envio = 1, fecha_envio = SYSTIMESTAMP, fecha_respuesta = SYSTIMESTAMP,
           codigo_respuesta = '0', ruta_cdr = 'cdr/R-B099-4.zip'
     WHERE id_comprobante = v_comp4;
    SELECT NVL(SUM(saldo), 0) INTO v_n FROM V_SALDO_FAVOR_CLIENTE WHERE id_cliente = c2;
    debe_funcionar('Anular con nota de crédito: la clienta elige saldo a favor (RN44)',
        'DECLARE c NUMBER; BEGIN INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (' || v_venta4
        || ', ''NC_BOLETA'', ''X'', ''X'', SYSDATE) RETURNING id_comprobante INTO c; '
        || 'INSERT INTO NOTA_CREDITO (id_nota_credito, motivo, forma_devolucion, id_usuario_solicita, id_usuario_autoriza) VALUES (c, ''Prefiere otro producto'', ''SALDO_FAVOR'', '
        || u_rec || ', ' || u_adm || '); '
        || 'UPDATE VENTA SET estado = ''ANULADA'' WHERE id_venta = ' || v_venta4 || '; END;');
    SELECT NVL(SUM(saldo), 0) INTO v_n2 FROM V_SALDO_FAVOR_CLIENTE WHERE id_cliente = c2;
    comprobar('Los S/ 30 pagados pasaron a saldo a favor de la clienta (' || v_n || ' -> ' || v_n2 || ')', v_n2 = v_n + 30);
    debe_fallar('[v5] Registrar a mano el abono de la venta anulada (sería un abono doble)',
        'INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_venta, tipo, monto) VALUES (' || c2 || ', ' || v_venta4 || ', ''ABONO'', 30)', -20063);
    SELECT COUNT(*) INTO v_n FROM SALDO_FAVOR_MOV WHERE id_venta = v_venta4 AND tipo = 'ABONO';
    comprobar('[v5] La venta anulada tiene un solo abono de saldo a favor (' || v_n || ')', v_n = 1);

    -- [v5] Documento por tipo (TIPO_DOCUMENTO) y verificación con APISPERU (RN45, RF97-RF99)
    debe_funcionar('Con el DNI consultado en APISPERU la ficha queda VERIFICADO',
        'UPDATE PERSONA SET id_tipo_documento = ' || v_dni || ', numero_documento = ''45678912'', origen_datos = ''VERIFICADO'', '
        || 'fecha_verificacion = SYSTIMESTAMP WHERE id_persona = ' || c2);
    debe_funcionar('Corregir el DNI a mano',
        'UPDATE PERSONA SET numero_documento = ''45678913'' WHERE id_persona = ' || c2);
    SELECT origen_datos INTO v_txt FROM PERSONA WHERE id_persona = c2;
    comprobar('Al cambiar el DNI sin consultar, la ficha vuelve a MANUAL (RN45)', v_txt = 'MANUAL');
    debe_fallar('[v5] Marcar VERIFICADO un pasaporte (APISPERU solo consulta DNI y RUC)',
        'UPDATE PERSONA SET id_tipo_documento = ' || v_pas || ', numero_documento = ''AB123456'', origen_datos = ''VERIFICADO'', '
        || 'fecha_verificacion = SYSTIMESTAMP WHERE id_persona = ' || c2, -20098);
    debe_fallar('Registrar un DNI de 7 dígitos',
        'UPDATE PERSONA SET numero_documento = ''4567891'' WHERE id_persona = ' || c2, -20097);
    debe_funcionar('[v5] Clienta extranjera con pasaporte (se ingresa a mano)',
        'UPDATE PERSONA SET id_tipo_documento = ' || v_pas || ', numero_documento = ''ab123456'' WHERE id_persona = ' || c2);
    SELECT numero_documento || ' ' || origen_datos INTO v_txt FROM PERSONA WHERE id_persona = c2;
    comprobar('[v5] El pasaporte se guardó en mayúsculas y MANUAL -> ' || v_txt, v_txt = 'AB123456 MANUAL');
    debe_fallar('[v5] Personal con pasaporte (el personal exige DNI o carnet de extranjería)',
        'UPDATE PERSONA SET id_tipo_documento = ' || v_pas || ', numero_documento = ''AB654321'' WHERE id_persona = ' || p_rec, -20091);
    debe_fallar('[v5] Mismo documento para dos personas',
        'UPDATE PERSONA SET id_tipo_documento = ' || v_dni || ', numero_documento = ''90000001'' WHERE id_persona = ' || c1, -1);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 10. Cierre de caja con arqueo por medio de pago (RF63-RF66) ===');
    debe_fallar('Cerrar la caja sin arqueo',
        'UPDATE CAJA SET estado = ''CERRADA'', id_usuario_cierre = ' || u_rec || ' WHERE id_caja = ' || v_caja, -20062);
    FOR m IN (SELECT id_medio_pago FROM MEDIO_PAGO WHERE estado = 'ACTIVO') LOOP
        INSERT INTO CAJA_ARQUEO (id_caja, id_medio_pago, monto_esperado, monto_contado, diferencia)
        VALUES (v_caja, m.id_medio_pago, 0, PKG_CAJA.esperado(v_caja, m.id_medio_pago, 100), 0);
    END LOOP;
    SELECT monto_esperado INTO v_n FROM CAJA_ARQUEO WHERE id_caja = v_caja AND id_medio_pago = v_efectivo;
    comprobar('Efectivo esperado = 100 inicial + 70 de productos + 30 del paquete = 200 (la venta anulada no cuenta) -> ' || v_n, v_n = 200);
    SELECT monto_esperado INTO v_n FROM CAJA_ARQUEO WHERE id_caja = v_caja AND id_medio_pago = v_yape;
    comprobar('Yape esperado = 20 del adelanto (los 10 de la venta anulada no cuentan) -> ' || v_n, v_n = 20);
    debe_fallar('Diferencia en el arqueo sin motivo',
        'UPDATE CAJA_ARQUEO SET monto_contado = 190 WHERE id_caja = ' || v_caja || ' AND id_medio_pago = ' || v_efectivo, -2290);
    debe_funcionar('Cerrar la caja con el arqueo cuadrado',
        'UPDATE CAJA SET estado = ''CERRADA'', id_usuario_cierre = ' || u_rec || ' WHERE id_caja = ' || v_caja);
    debe_fallar('Vender en la caja cerrada sin autorización (RN30)',
        'INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total) VALUES ('
        || v_sede || ', ' || v_caja || ', ' || c1 || ', ' || u_rec || ', 0, 0, 0)', -20042);
    debe_fallar('Reabrir la caja cerrada',
        'UPDATE CAJA SET estado = ''ABIERTA'' WHERE id_caja = ' || v_caja, -20060);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 11. Sueldo acordado y desactivación del personal (RN22, RF61, RF53) ===');
    como(u_adm);
    UPDATE PERSONAL SET estado = 'INACTIVO' WHERE id_personal = p_esp2;
    v_p := persona('Lia', 'Prueba', '90000006', NULL);
    debe_funcionar('Sueldo acordado de S/ 800, menor a la RMV: se registra (RN22)',
        'INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso) VALUES ('
        || v_p || ', ' || v_sede || ', ''RECEPCIONISTA'', ''FIJO'', 800, 0, SYSDATE)');
    SELECT COUNT(*) INTO v_n FROM V_PERSONAL_BAJO_RMV p JOIN PERSONA x ON x.id_persona = p.id_personal WHERE x.numero_documento = '90000006';
    comprobar('Y aparece en el aviso de sueldos bajo la RMV (RF61)', v_n = 1);
    debe_funcionar('Desactivar a la especialista Eva', 'UPDATE PERSONAL SET estado = ''INACTIVO'' WHERE id_personal = ' || p_esp);
    SELECT estado INTO v_txt FROM USUARIO WHERE id_usuario = u_esp;
    comprobar('Su usuario quedó INACTIVO', v_txt = 'INACTIVO');
    debe_fallar('Un usuario INACTIVO no puede iniciar sesión en la BD',
        'BEGIN PKG_SESION.iniciar(' || u_esp || '); END;', -20004);

    PKG_SESION.terminar;
    DBMS_OUTPUT.PUT_LINE('=====================================================');
    DBMS_OUTPUT.PUT_LINE('Resultado: ' || v_ok || ' OK, ' || v_fallo || ' FALLO');
    DBMS_OUTPUT.PUT_LINE('=====================================================');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Prueba detenida: ' || SQLERRM);
        DBMS_OUTPUT.PUT_LINE(DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
        DBMS_OUTPUT.PUT_LINE('Resultado parcial: ' || v_ok || ' OK, ' || v_fallo || ' FALLO');
        PKG_SESION.terminar;
        ROLLBACK;
END;
/
SET FEEDBACK ON
