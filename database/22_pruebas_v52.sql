-- =====================================================================
-- CAUTIVA SPA E.I.R.L. · BackOffice web · Base de datos Oracle (19c o 23ai/26ai Free)
-- Script 22 (v5.2) · Pruebas de la migración 18_ajustes_v52.sql (15/11/2026)
-- Se ejecuta como: dueño del esquema, después de 18_ajustes_v52.sql.
--
-- Igual que 21_pruebas_v51.sql: crea sus propios datos de prueba (dos sedes, personal, usuarios, clientas,
-- recursos y un producto), prueba cada cambio del modelo v5.2 y al final hace ROLLBACK, así que la base
-- queda exactamente como estaba. Cada prueba imprime OK o FALLO. Usa PKG_SESION.iniciar para actuar
-- como recepcionista, especialista, almacén o Administradora, igual que el back-end.
--
--   0. Estructura v5.2 (RECURSO, PARAMETRO, V_AGENDA_BLOQUES, TRANSACCION_PASARELA.id_venta)
--   1. Agenda sin tiempo de limpieza (RN03, D-14)
--   2. Reapertura de la caja del día y su auditoría (RN30, D-15, D-16)
--   3. Nota de crédito de una solicitud y vistas V_VENTA_LINEA y V_KARDEX (RN32, D-21, D-37)
--   4. Cobros de una venta por la pasarela (RN42, RN52, D-22)
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF
SET LINESIZE 300

DECLARE
    v_ok    PLS_INTEGER := 0;
    v_fallo PLS_INTEGER := 0;

    v_sede NUMBER; v_sede2 NUMBER; v_area NUMBER; v_var NUMBER; v_cat NUMBER; v_dni NUMBER;
    v_efectivo NUMBER; v_yape NUMBER;
    v_rol_adm NUMBER; v_rol_rec NUMBER; v_rol_esp NUMBER; v_rol_alm NUMBER;
    p_adm NUMBER; p_rec NUMBER; p_esp NUMBER; p_esp2 NUMBER; p_alm NUMBER;
    u_adm NUMBER; u_rec NUMBER; u_esp NUMBER; u_alm NUMBER;
    r1 NUMBER; r2 NUMBER; r3 NUMBER; c1 NUMBER; c2 NUMBER;
    v_prod NUMBER;
    v_manana VARCHAR2(80);
    v_cita NUMBER; v_at NUMBER;
    v_caja_ayer NUMBER; v_caja NUMBER;
    v_venta NUMBER; v_venta_x NUMBER; v_venta_y NUMBER; v_det_prod NUMBER; v_nc NUMBER; v_tr NUMBER;
    v_n NUMBER; v_n2 NUMBER; v_txt VARCHAR2(200); v_txt2 VARCHAR2(200);
    v_fin TIMESTAMP; v_fin2 TIMESTAMP;

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

    FUNCTION persona(p_nombres IN VARCHAR2, p_dni IN VARCHAR2, p_telefono IN VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono)
        VALUES ('NATURAL', p_nombres, 'Prueba v52', CASE WHEN p_dni IS NULL THEN NULL ELSE v_dni END, p_dni, p_telefono)
        RETURNING id_persona INTO v;
        RETURN v;
    END;

    FUNCTION personal(p_nombres IN VARCHAR2, p_dni IN VARCHAR2, p_cargo IN VARCHAR2) RETURN NUMBER IS
        v NUMBER := persona(p_nombres, p_dni, NULL);
    BEGIN
        INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
        VALUES (v, v_sede, p_cargo, 'FIJO', 1230, 0, SYSDATE);
        RETURN v;
    END;

    FUNCTION usuario(p_id_personal IN NUMBER, p_id_rol IN NUMBER, p_nombre IN VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash)
        VALUES (p_id_personal, p_id_rol, p_nombre, 'x') RETURNING id_usuario INTO v;
        RETURN v;
    END;

    -- Arqueo cuadrado de todos los medios activos y cierre de la caja (la sesión actual)
    PROCEDURE cerrar_caja(p_id_caja IN NUMBER) IS
    BEGIN
        FOR m IN (SELECT id_medio_pago FROM MEDIO_PAGO WHERE estado = 'ACTIVO') LOOP
            INSERT INTO CAJA_ARQUEO (id_caja, id_medio_pago, monto_esperado, monto_contado, diferencia)
            VALUES (p_id_caja, m.id_medio_pago, 0, PKG_CAJA.esperado(p_id_caja, m.id_medio_pago, 100), 0);
        END LOOP;
        UPDATE CAJA SET estado = 'CERRADA', id_usuario_cierre = u_rec WHERE id_caja = p_id_caja;
    END;

    FUNCTION venta_abierta RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total)
        VALUES (v_sede, v_caja, c1, u_rec, 0, 0, 0) RETURNING id_venta INTO v;
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
    SELECT id_area INTO v_area FROM AREA WHERE nombre = 'Peluquería y Estilismo';
    SELECT v.id_variante INTO v_var FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio
     WHERE s.nombre = 'Corte de Cabello Caballero';
    SELECT id_medio_pago INTO v_efectivo FROM MEDIO_PAGO WHERE nombre = 'EFECTIVO';
    SELECT id_medio_pago INTO v_yape FROM MEDIO_PAGO WHERE nombre = 'YAPE';
    SELECT id_categoria INTO v_cat FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Capilar';
    SELECT id_tipo_documento INTO v_dni FROM TIPO_DOCUMENTO WHERE abreviatura = 'DNI';

    INSERT INTO SEDE (nombre, direccion, horario_apertura, horario_cierre)
    VALUES ('SEDE DE PRUEBA V52', 'Calle de prueba 52', '09:00', '20:00') RETURNING id_sede INTO v_sede;
    INSERT INTO SEDE (nombre, direccion, horario_apertura, horario_cierre)
    VALUES ('SEDE DE PRUEBA V52-2', 'Calle de prueba 52-2', '09:00', '20:00') RETURNING id_sede INTO v_sede2;
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'BOLETA',     'B097');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'FACTURA',    'F097');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'NC_BOLETA',  'BC97');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'NC_FACTURA', 'FC97');

    p_adm  := personal('Ada',  '92000001', 'ADMINISTRADORA');
    p_rec  := personal('Rita', '92000002', 'RECEPCIONISTA');
    p_esp  := personal('Eva',  '92000003', 'ESPECIALISTA_TECNICA');
    p_esp2 := personal('Ena',  '92000004', 'ESPECIALISTA_TECNICA');
    p_alm  := personal('Alma', '92000005', 'ENCARGADA_ALMACEN');
    INSERT INTO PERSONAL_AREA (id_personal, id_area) VALUES (p_esp, v_area);
    INSERT INTO PERSONAL_AREA (id_personal, id_area) VALUES (p_esp2, v_area);
    u_adm := usuario(p_adm, v_rol_adm, 'prueba52.adm');
    u_rec := usuario(p_rec, v_rol_rec, 'prueba52.rec');
    u_esp := usuario(p_esp, v_rol_esp, 'prueba52.esp');
    u_alm := usuario(p_alm, v_rol_alm, 'prueba52.alm');

    INSERT INTO RECURSO (id_sede, nombre, tipo) VALUES (v_sede, 'Sillón V52-1', 'SILLON') RETURNING id_recurso INTO r1;
    INSERT INTO RECURSO (id_sede, nombre, tipo) VALUES (v_sede, 'Sillón V52-2', 'SILLON') RETURNING id_recurso INTO r2;
    INSERT INTO RECURSO (id_sede, nombre, tipo) VALUES (v_sede, 'Sillón V52-3', 'SILLON') RETURNING id_recurso INTO r3;
    c1 := persona('Clara', NULL, '920000111');
    INSERT INTO CLIENTE (id_cliente) VALUES (c1);
    c2 := persona('Dora', NULL, '920000222');
    INSERT INTO CLIENTE (id_cliente) VALUES (c2);
    INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
    VALUES (v_cat, 'P52-001', 'Producto de prueba v52', 'UND', 'VENTA', 30, 20) RETURNING id_producto INTO v_prod;

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 0. Estructura v5.2 ===');
    SELECT COUNT(*) INTO v_n FROM user_tab_columns WHERE table_name = 'RECURSO' AND column_name = 'MINUTOS_LIMPIEZA';
    comprobar('RECURSO ya no tiene minutos_limpieza (RN03)', v_n = 0);
    SELECT COUNT(*) INTO v_n FROM user_constraints WHERE constraint_name = 'CK_RECURSO_MINUTOS_LIMPIEZA';
    comprobar('CK_RECURSO_MINUTOS_LIMPIEZA se retiró', v_n = 0);
    SELECT COUNT(*) INTO v_n FROM PARAMETRO WHERE codigo = 'LIMPIEZA_MAX_MIN';
    comprobar('PARAMETRO ya no tiene LIMPIEZA_MAX_MIN', v_n = 0);
    SELECT COUNT(CASE WHEN column_name = 'FIN' THEN 1 END), COUNT(CASE WHEN column_name = 'FIN_CON_LIMPIEZA' THEN 1 END)
      INTO v_n, v_n2 FROM user_tab_columns WHERE table_name = 'V_AGENDA_BLOQUES';
    comprobar('V_AGENDA_BLOQUES tiene la columna FIN y ya no FIN_CON_LIMPIEZA', v_n = 1 AND v_n2 = 0);
    SELECT COUNT(*) INTO v_n FROM user_constraints
     WHERE constraint_name IN ('FK_TRANS_PAS_VENTA', 'CK_TRANS_PAS_VENTA') AND table_name = 'TRANSACCION_PASARELA';
    SELECT COUNT(*) INTO v_n2 FROM user_indexes WHERE index_name = 'IX_TRANS_PAS_VENTA';
    comprobar('TRANSACCION_PASARELA.id_venta con FK_TRANS_PAS_VENTA, CK_TRANS_PAS_VENTA e IX_TRANS_PAS_VENTA', v_n = 2 AND v_n2 = 1);
    SELECT COUNT(*) INTO v_n FROM user_constraints WHERE constraint_type = 'R' AND constraint_name NOT LIKE 'BIN$%';
    comprobar('132 claves foráneas -> ' || v_n, v_n = 132);
    SELECT COUNT(*) INTO v_n FROM user_objects WHERE status = 'INVALID';
    comprobar('Ningún objeto inválido', v_n = 0);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 1. Agenda sin tiempo de limpieza (RN03) ===');
    como(u_rec);
    debe_funcionar('Cita mañana 10:00, 30 min, especialista Eva, Sillón V52-1',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c1 || ', ' || v_sede || ', ' || p_esp || ', ' || r1 || ', ' || v_var || ', ' || v_manana || ' + INTERVAL ''10'' HOUR, 30)');
    SELECT MAX(id_cita) INTO v_cita FROM CITA WHERE id_recurso = r1;
    SELECT TO_CHAR(fecha_hora_fin, 'HH24:MI') INTO v_txt FROM CITA WHERE id_cita = v_cita;
    comprobar('El fin de la cita es inicio + duración: 10:00 + 30 = 10:30 -> ' || v_txt, v_txt = '10:30');
    SELECT TO_CHAR(fin, 'HH24:MI') INTO v_txt FROM V_AGENDA_BLOQUES WHERE origen = 'CITA' AND id_origen = v_cita;
    comprobar('V_AGENDA_BLOQUES.fin de la cita = 10:30 -> ' || v_txt, v_txt = '10:30');
    SAVEPOINT sp_fin;
    debe_funcionar('Misma especialista a la hora de fin (10:30) en otro sillón',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c2 || ', ' || v_sede || ', ' || p_esp || ', ' || r2 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(630, ''MINUTE''), 30)');
    ROLLBACK TO sp_fin;
    debe_funcionar('Mismo sillón a la hora de fin (10:30) con otra especialista',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c2 || ', ' || v_sede || ', ' || p_esp2 || ', ' || r1 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(630, ''MINUTE''), 30)');
    ROLLBACK TO sp_fin;
    debe_fallar('Misma especialista a las 10:15 en otro sillón (dentro del servicio)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c2 || ', ' || v_sede || ', ' || p_esp || ', ' || r2 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(615, ''MINUTE''), 30)', -20010);
    debe_fallar('Mismo sillón a las 10:15 con otra especialista (dentro del servicio)',
        'INSERT INTO CITA (id_cliente, id_sede, id_personal, id_recurso, id_variante, fecha_hora_inicio, duracion_min) VALUES ('
        || c2 || ', ' || v_sede || ', ' || p_esp2 || ', ' || r1 || ', ' || v_var || ', ' || v_manana || ' + NUMTODSINTERVAL(615, ''MINUTE''), 30)', -20010);

    -- Atención sin cita que termina antes de lo declarado: declaró 60 minutos y termina a los 20
    como(u_esp);
    INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min)
    VALUES (c1, p_esp, r2, v_var, SYSTIMESTAMP - INTERVAL '20' MINUTE, 60) RETURNING id_atencion INTO v_at;
    debe_fallar('Con la atención EN_CURSO (60 min declarados), el mismo sillón con otra especialista',
        'INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES ('
        || c2 || ', ' || p_esp2 || ', ' || r2 || ', ' || v_var || ', SYSTIMESTAMP, 30)', -20011);
    UPDATE ATENCION SET estado = 'FINALIZADA' WHERE id_atencion = v_at;
    SELECT hora_fin, hora_inicio + NUMTODSINTERVAL(tiempo_declarado_min, 'MINUTE') INTO v_fin, v_fin2 FROM ATENCION WHERE id_atencion = v_at;
    comprobar('La atención terminó antes de lo declarado (hora_fin antes de inicio + 60)', v_fin < v_fin2);
    SELECT COUNT(*) INTO v_n FROM V_AGENDA_BLOQUES WHERE origen = 'ATENCION_SIN_CITA' AND id_origen = v_at AND fin = v_fin;
    comprobar('V_AGENDA_BLOQUES.fin de la atención es su hora_fin (sin limpieza)', v_n = 1);
    SAVEPOINT sp_hora_fin;
    debe_funcionar('Desde hora_fin, la misma especialista inicia otra atención en otro sillón',
        'INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES ('
        || c2 || ', ' || p_esp || ', ' || r3 || ', ' || v_var || ', SYSTIMESTAMP, 30)');
    ROLLBACK TO sp_hora_fin;
    debe_funcionar('Desde hora_fin, el mismo sillón recibe otra atención de otra especialista',
        'INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES ('
        || c2 || ', ' || p_esp2 || ', ' || r2 || ', ' || v_var || ', SYSTIMESTAMP, 30)');
    ROLLBACK TO sp_hora_fin;

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 2. Reapertura de la caja del día (RN30) ===');
    como(u_rec);
    INSERT INTO CAJA (id_sede, id_usuario_apertura, monto_inicial, fecha_apertura)
    VALUES (v_sede, u_rec, 100, SYSTIMESTAMP - INTERVAL '1' DAY) RETURNING id_caja INTO v_caja_ayer;
    cerrar_caja(v_caja_ayer);
    ok('La caja de ayer quedó cerrada con el arqueo cuadrado');
    como(u_adm);
    debe_fallar('La Administradora reabre con motivo una caja de otro día',
        'UPDATE CAJA SET estado = ''ABIERTA'', motivo_reapertura = ''Venta olvidada'' WHERE id_caja = ' || v_caja_ayer, -20060);
    como(u_rec);
    INSERT INTO CAJA (id_sede, id_usuario_apertura, monto_inicial) VALUES (v_sede, u_rec, 100) RETURNING id_caja INTO v_caja;
    cerrar_caja(v_caja);
    ok('La caja de hoy quedó cerrada con el arqueo cuadrado');
    como(u_adm);
    debe_funcionar('La Administradora reabre con motivo la caja de hoy (CUS46)',
        'UPDATE CAJA SET estado = ''ABIERTA'', motivo_reapertura = ''Venta de la tarde sin registrar'' WHERE id_caja = ' || v_caja);
    SELECT COUNT(*) INTO v_n FROM AUDITORIA
     WHERE entidad = 'CAJA' AND id_entidad = v_caja AND id_usuario = u_adm
       AND detalle LIKE '%motivo_reapertura:%Venta de la tarde sin registrar%'
       AND detalle LIKE '%id_usuario_reapertura:%' AND detalle LIKE '%fecha_reapertura:%';
    comprobar('AUDITORIA de CAJA guarda el motivo, quién y cuándo se reabrió (D-15)', v_n = 1);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 3. Nota de crédito de una solicitud y vistas de venta y kardex ===');
    como(u_alm);
    PKG_INVENTARIO.registrar(v_prod, v_sede, 'ENTRADA', 'Stock inicial', 10);
    ok('Almacén registra 10 unidades del producto');
    como(u_rec);
    v_venta := venta_abierta;
    INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_atencion, cantidad, precio_unitario, subtotal)
    VALUES (v_venta, 'SERVICIO', v_at, 1, 0, 0);
    INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, subtotal)
    VALUES (v_venta, 'PRODUCTO', v_prod, 1, 30, 30) RETURNING id_detalle INTO v_det_prod;
    SELECT SUM(subtotal) INTO v_n FROM VENTA_DETALLE WHERE id_venta = v_venta;
    INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto) VALUES (v_venta, v_efectivo, v_n);
    INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (v_venta, 'BOLETA', 'B999', '1', SYSDATE);
    debe_funcionar('Venta con el servicio de la atención y un producto, cerrada con boleta',
        'UPDATE VENTA SET subtotal = ' || v_n || ', igv = 0, total = ' || v_n || ', estado = ''CERRADA'' WHERE id_venta = ' || v_venta);
    SELECT COUNT(*) INTO v_n FROM V_VENTA_LINEA
     WHERE id_venta = v_venta AND ((tipo_item = 'PRODUCTO' AND id_producto = v_prod) OR (tipo_item = 'SERVICIO' AND id_variante = v_var));
    comprobar('V_VENTA_LINEA trae id_producto (producto) e id_variante (servicio) (D-37)', v_n = 2);
    SELECT serie || '-' || numero INTO v_txt2 FROM COMPROBANTE WHERE id_venta = v_venta AND tipo = 'BOLETA';
    SELECT MAX(comprobante) INTO v_txt FROM V_KARDEX WHERE id_detalle_venta = v_det_prod AND tipo = 'SALIDA';
    comprobar('V_KARDEX trae el comprobante en la salida de la venta -> ' || v_txt, v_txt = v_txt2);
    SELECT COUNT(*) INTO v_n FROM V_KARDEX WHERE id_producto = v_prod AND tipo = 'ENTRADA' AND comprobante IS NULL AND documento_compra IS NULL;
    comprobar('V_KARDEX deja comprobante y documento_compra nulos en una entrada manual -> ' || v_n, v_n = 1);

    INSERT INTO SOLICITUD_ANULACION (id_venta, motivo, id_usuario_solicita) VALUES (v_venta, 'Cobro duplicado', u_rec);
    ok('La recepcionista pide anular la venta (CUS22)');
    UPDATE COMPROBANTE
       SET estado_sunat = 'ACEPTADO', intentos_envio = 1, fecha_envio = SYSTIMESTAMP, fecha_respuesta = SYSTIMESTAMP,
           codigo_respuesta = '0', descripcion_respuesta = 'Aceptada', ruta_cdr = 'cdr/R-prueba52.zip'
     WHERE id_venta = v_venta AND tipo = 'BOLETA';
    como(u_adm);
    INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (v_venta, 'NC_BOLETA', 'X', 'X', SYSDATE)
    RETURNING id_comprobante INTO v_nc;
    debe_funcionar('La Administradora emite la nota de crédito de la solicitud (CUS23)',
        'INSERT INTO NOTA_CREDITO (id_nota_credito, motivo, id_usuario_solicita, id_usuario_autoriza) VALUES ('
        || v_nc || ', ''Cobro duplicado'', ' || u_adm || ', ' || u_adm || ')');
    SELECT id_usuario_solicita INTO v_n FROM NOTA_CREDITO WHERE id_nota_credito = v_nc;
    comprobar('NOTA_CREDITO.id_usuario_solicita es la recepcionista de la solicitud, no la Administradora (D-21)', v_n = u_rec);
    como(u_rec);
    v_venta_x := venta_abierta;
    INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, subtotal)
    VALUES (v_venta_x, 'PRODUCTO', v_prod, 1, 30, 30);
    INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto) VALUES (v_venta_x, v_efectivo, 30);
    INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (v_venta_x, 'BOLETA', 'B999', '1', SYSDATE);
    UPDATE VENTA SET subtotal = 30, igv = 0, total = 30, estado = 'CERRADA' WHERE id_venta = v_venta_x;
    UPDATE COMPROBANTE
       SET estado_sunat = 'ACEPTADO', intentos_envio = 1, fecha_envio = SYSTIMESTAMP, fecha_respuesta = SYSTIMESTAMP,
           codigo_respuesta = '0', descripcion_respuesta = 'Aceptada', ruta_cdr = 'cdr/R-prueba52b.zip'
     WHERE id_venta = v_venta_x AND tipo = 'BOLETA';
    como(u_adm);
    INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (v_venta_x, 'NC_BOLETA', 'X', 'X', SYSDATE)
    RETURNING id_comprobante INTO v_nc;
    debe_funcionar('Nota de crédito de otra venta, sin solicitud',
        'INSERT INTO NOTA_CREDITO (id_nota_credito, motivo, id_usuario_solicita, id_usuario_autoriza) VALUES ('
        || v_nc || ', ''Error'', ' || u_rec || ', ' || u_adm || ')');
    SELECT id_usuario_solicita INTO v_n FROM NOTA_CREDITO WHERE id_nota_credito = v_nc;
    comprobar('Sin solicitud, id_usuario_solicita es la Administradora de la sesión', v_n = u_adm);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 4. Cobros de una venta por la pasarela (RN42, RN52) ===');
    como(u_rec);
    v_venta_x := venta_abierta;
    v_venta_y := venta_abierta;
    debe_fallar('Cobro con id_venta de una venta de otra sede',
        'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, id_venta, monto, estado, id_usuario) VALUES ('
        || v_sede2 || ', ' || v_yape || ', ''QR'', ' || v_venta_x || ', 30, ''PENDIENTE'', ' || u_rec || ')', -20072);
    debe_fallar('Cobro con id_venta de una venta CERRADA',
        'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, id_venta, monto, estado, id_usuario) VALUES ('
        || v_sede || ', ' || v_yape || ', ''QR'', ' || v_venta || ', 30, ''PENDIENTE'', ' || u_rec || ')', -20072);
    debe_fallar('Cobro con id_venta y con id_cita (CK_TRANS_PAS_VENTA)',
        'INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, id_cita, id_venta, monto, estado, id_usuario) VALUES ('
        || v_sede || ', ' || v_yape || ', ''QR'', ' || v_cita || ', ' || v_venta_x || ', 30, ''PENDIENTE'', ' || u_rec || ')', -2290);
    INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, canal, id_venta, monto, estado, id_usuario)
    VALUES (v_sede, v_yape, 'QR', v_venta_x, 30, 'PENDIENTE', u_rec) RETURNING id_transaccion INTO v_tr;
    ok('Cobro PENDIENTE de S/ 30 con Yape para la venta abierta X (RN52)');
    SELECT COUNT(*) INTO v_n FROM TRANSACCION_PASARELA WHERE id_venta = v_venta_x AND estado = 'PENDIENTE';
    comprobar('Los cobros PENDIENTES de la venta X se listan por id_venta (D-22)', v_n = 1);
    SELECT COUNT(*) INTO v_n FROM AUDITORIA WHERE entidad = 'TRANSACCION_PASARELA' AND id_entidad = v_tr AND detalle LIKE '%id_venta=' || v_venta_x || ';%';
    comprobar('AUDITORIA de TRANSACCION_PASARELA guarda id_venta', v_n = 1);
    debe_fallar('Cambiar el id_venta del cobro',
        'UPDATE TRANSACCION_PASARELA SET id_venta = ' || v_venta_y || ' WHERE id_transaccion = ' || v_tr, -20072);
    UPDATE TRANSACCION_PASARELA SET estado = 'APROBADA', codigo_operacion = 'P52-QR-001', fecha_respuesta = SYSTIMESTAMP
     WHERE id_transaccion = v_tr;
    ok('La pasarela aprueba el cobro');
    debe_fallar('Pago de la venta Y con la transacción cobrada para la venta X',
        'INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto, id_transaccion) VALUES (' || v_venta_y || ', ' || v_yape || ', 30, ' || v_tr || ')', -20107);
    debe_funcionar('Pago de la venta X con su transacción',
        'INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto, id_transaccion) VALUES (' || v_venta_x || ', ' || v_yape || ', 30, ' || v_tr || ')');

    PKG_SESION.terminar;
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('==============================================');
    DBMS_OUTPUT.PUT_LINE('Resultado: ' || v_ok || ' OK, ' || v_fallo || ' FALLO (todo se deshizo con ROLLBACK)');
EXCEPTION
    WHEN OTHERS THEN
        PKG_SESION.terminar;
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('FALLO  error inesperado: ' || SQLERRM);
        DBMS_OUTPUT.PUT_LINE(DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
        DBMS_OUTPUT.PUT_LINE('Resultado parcial: ' || v_ok || ' OK, ' || (v_fallo + 1) || ' FALLO');
END;
/
SET FEEDBACK ON
