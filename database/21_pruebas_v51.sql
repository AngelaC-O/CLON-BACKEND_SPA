-- =====================================================================
-- CAUTIVA SPA E.I.R.L. · BackOffice web · Base de datos Oracle (19c o 23ai/26ai Free)
-- Script 21 (v5.1) · Pruebas de la migración 17_datos_iteracion2.sql (08/11/2026)
-- Se ejecuta como: dueño del esquema, después de 17_datos_iteracion2.sql.
--
-- Igual que 20_pruebas_reglas_v5.sql: crea sus propios datos de prueba (sede, personal, usuarios,
-- clienta, productos), prueba cada regla nueva del modelo v5.1 y al final hace ROLLBACK, así que la base
-- queda exactamente como estaba. Cada prueba imprime OK o FALLO. Usa PKG_SESION.iniciar para actuar
-- como recepcionista, especialista, almacén o Administradora, igual que el back-end.
--
--   0. Estructura v5.1 (tablas, triggers nuevos válidos)
--   1. Datos de la Iteración 2 (PARAMETRO, permisos, usuaria almacen)
-- [v5.2] (15/11/2026) Se corre después de 18_ajustes_v52.sql: las comprobaciones de la limpieza se retiraron
--   (RN03) y la cuenta de claves foráneas acepta FK_TRANS_PAS_VENTA. Las reglas v5.2 se prueban en 22_pruebas_v52.sql.
--   2. Consumo automático y consumo pendiente (RN17, RN53)
--   3. Solicitud de anulación (RN32)
--   4. Reapertura de la caja (RN30)
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF
SET LINESIZE 300

DECLARE
    v_ok    PLS_INTEGER := 0;
    v_fallo PLS_INTEGER := 0;

    v_sede NUMBER; v_area NUMBER; v_var NUMBER; v_cat NUMBER; v_dni NUMBER; v_efectivo NUMBER;
    v_rol_adm NUMBER; v_rol_rec NUMBER; v_rol_esp NUMBER; v_rol_alm NUMBER;
    p_adm NUMBER; p_rec NUMBER; p_esp NUMBER; p_esp2 NUMBER; p_alm NUMBER;
    u_adm NUMBER; u_rec NUMBER; u_esp NUMBER; u_esp2 NUMBER; u_alm NUMBER;
    r1 NUMBER; c1 NUMBER;
    v_prod NUMBER; v_insumo NUMBER;
    v_at NUMBER; v_cons NUMBER; v_mov NUMBER; v_mov_otro NUMBER;
    v_caja NUMBER; v_venta NUMBER; v_venta_ab NUMBER; v_venta2 NUMBER; v_sol NUMBER;
    v_n NUMBER; v_n2 NUMBER; v_txt VARCHAR2(200);

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

    FUNCTION persona(p_nombres IN VARCHAR2, p_dni IN VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono)
        VALUES ('NATURAL', p_nombres, 'Prueba v51', CASE WHEN p_dni IS NULL THEN NULL ELSE v_dni END, p_dni,
                CASE WHEN p_dni IS NULL THEN '910000999' END)
        RETURNING id_persona INTO v;
        RETURN v;
    END;

    FUNCTION personal(p_nombres IN VARCHAR2, p_dni IN VARCHAR2, p_cargo IN VARCHAR2) RETURN NUMBER IS
        v NUMBER := persona(p_nombres, p_dni);
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

    -- Venta de un producto de S/ 30 en efectivo, con boleta, cerrada (la registra la sesión actual)
    FUNCTION venta_cerrada(p_id_caja IN NUMBER) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total)
        VALUES (v_sede, p_id_caja, c1, u_rec, 0, 0, 0) RETURNING id_venta INTO v;
        INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, subtotal)
        VALUES (v, 'PRODUCTO', v_prod, 1, 30, 30);
        INSERT INTO VENTA_PAGO (id_venta, id_medio_pago, monto) VALUES (v, v_efectivo, 30);
        INSERT INTO COMPROBANTE (id_venta, tipo, serie, numero, fecha_emision) VALUES (v, 'BOLETA', 'B999', '1', SYSDATE);
        UPDATE VENTA SET subtotal = 30, igv = 0, total = 30, estado = 'CERRADA' WHERE id_venta = v;
        RETURN v;
    END;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== Preparando datos de prueba (modo mantenimiento) ===');
    PKG_SESION.terminar;

    SELECT id_rol INTO v_rol_adm FROM ROL WHERE nombre = 'ADMINISTRADORA';
    SELECT id_rol INTO v_rol_rec FROM ROL WHERE nombre = 'RECEPCIONISTA';
    SELECT id_rol INTO v_rol_esp FROM ROL WHERE nombre = 'ESPECIALISTA_TECNICA';
    SELECT id_rol INTO v_rol_alm FROM ROL WHERE nombre = 'ENCARGADA_ALMACEN';
    SELECT id_area INTO v_area FROM AREA WHERE nombre = 'Peluquería y Estilismo';
    SELECT v.id_variante INTO v_var FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio
     WHERE s.nombre = 'Corte de Cabello Caballero';
    SELECT id_medio_pago INTO v_efectivo FROM MEDIO_PAGO WHERE nombre = 'EFECTIVO';
    SELECT id_categoria INTO v_cat FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Capilar';
    SELECT id_tipo_documento INTO v_dni FROM TIPO_DOCUMENTO WHERE abreviatura = 'DNI';

    INSERT INTO SEDE (nombre, direccion, horario_apertura, horario_cierre)
    VALUES ('SEDE DE PRUEBA V51', 'Calle de prueba 51', '09:00', '20:00') RETURNING id_sede INTO v_sede;
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'BOLETA',     'B098');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'FACTURA',    'F098');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'NC_BOLETA',  'BC98');
    INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (v_sede, 'NC_FACTURA', 'FC98');

    p_adm  := personal('Ada',  '91000001', 'ADMINISTRADORA');
    p_rec  := personal('Rita', '91000002', 'RECEPCIONISTA');
    p_esp  := personal('Eva',  '91000003', 'ESPECIALISTA_TECNICA');
    p_esp2 := personal('Ena',  '91000004', 'ESPECIALISTA_TECNICA');
    p_alm  := personal('Alma', '91000005', 'ENCARGADA_ALMACEN');
    INSERT INTO PERSONAL_AREA (id_personal, id_area) VALUES (p_esp, v_area);
    u_adm  := usuario(p_adm,  v_rol_adm, 'prueba51.adm');
    u_rec  := usuario(p_rec,  v_rol_rec, 'prueba51.rec');
    u_esp  := usuario(p_esp,  v_rol_esp, 'prueba51.esp');
    u_esp2 := usuario(p_esp2, v_rol_esp, 'prueba51.esp2');
    u_alm  := usuario(p_alm,  v_rol_alm, 'prueba51.alm');

    INSERT INTO RECURSO (id_sede, nombre, tipo) VALUES (v_sede, 'Sillón V51', 'SILLON') RETURNING id_recurso INTO r1;
    c1 := persona('Clara', NULL);
    INSERT INTO CLIENTE (id_cliente) VALUES (c1);

    INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
    VALUES (v_cat, 'P51-001', 'Producto de prueba v51', 'UND', 'VENTA', 30, 20) RETURNING id_producto INTO v_prod;
    INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
    VALUES (v_cat, 'P51-002', 'Insumo de prueba v51 (ml)', 'ML', 'INSUMO', 1, 1) RETURNING id_producto INTO v_insumo;

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 0. Estructura v5.1 ===');
    SELECT COUNT(*) INTO v_n FROM user_tables;
    comprobar('57 tablas (55 de v5 + SOLICITUD_ANULACION + CONSUMO_PENDIENTE) -> ' || v_n, v_n = 57);
    SELECT COUNT(*) INTO v_n FROM user_constraints WHERE constraint_type = 'R' AND constraint_name NOT LIKE 'BIN$%';
    SELECT COUNT(*) INTO v_n2 FROM user_constraints WHERE constraint_name = 'FK_TRANS_PAS_VENTA';   -- [v5.2] la agrega 18
    comprobar('131 claves foráneas (132 con FK_TRANS_PAS_VENTA de v5.2) -> ' || v_n, v_n = 131 + v_n2);
    SELECT COUNT(*) INTO v_n FROM user_triggers
     WHERE trigger_name IN ('TRG_SOLICITUD_ANULACION_REGLAS', 'TRG_CONSUMO_PENDIENTE_REGLAS',
                            'TRG_AUD_SOLICITUD_ANULACION', 'TRG_AUD_CONSUMO_PENDIENTE') AND status = 'ENABLED';
    comprobar('Los 4 triggers nuevos existen y están habilitados', v_n = 4);
    SELECT COUNT(*) INTO v_n FROM user_objects WHERE status = 'INVALID';
    comprobar('Ningún objeto inválido', v_n = 0);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 1. Datos de la Iteración 2 (RN51, permisos) ===');
    -- retirado en v5.2 (RN03): las comprobaciones de la limpieza (DEFAULT 10, el recurso con 30 minutos y
    -- LIMPIEZA_MAX_MIN = 10). RECURSO ya no tiene minutos_limpieza ni PARAMETRO la fila LIMPIEZA_MAX_MIN (22_pruebas_v52.sql).
    SELECT NVL(MAX(CASE WHEN codigo = 'AGENDA_PASO_MIN' THEN valor_numero END), -1) INTO v_n FROM PARAMETRO;
    comprobar('PARAMETRO AGENDA_PASO_MIN = 15 (RN51) -> ' || v_n, v_n = 15);
    SELECT COUNT(*) INTO v_n FROM ROL_PERMISO rp JOIN PERMISO p ON p.id_permiso = rp.id_permiso
     WHERE rp.id_rol = v_rol_rec AND (p.modulo, p.accion) IN (('CATALOGO', 'VER'), ('VENTAS', 'ANULAR'));
    comprobar('RECEPCIONISTA tiene CATALOGO:VER y VENTAS:ANULAR', v_n = 2);
    SELECT COUNT(*) INTO v_n FROM ROL_PERMISO rp JOIN PERMISO p ON p.id_permiso = rp.id_permiso
     WHERE p.modulo = 'VENTAS' AND p.accion = 'EDITAR' AND rp.id_rol <> v_rol_adm;
    comprobar('VENTAS:EDITAR sigue solo en ADMINISTRADORA', v_n = 0);
    SELECT COUNT(*) INTO v_n FROM USUARIO u JOIN PERSONAL pe ON pe.id_personal = u.id_personal
     WHERE u.nombre_usuario = 'almacen' AND u.id_rol = v_rol_alm AND u.debe_cambiar_clave = 'S'
       AND u.clave_hash LIKE '$2a$10$%' AND pe.cargo = 'ENCARGADA_ALMACEN';
    comprobar('Usuaria almacen con ENCARGADA_ALMACEN, hash BCrypt y cambio de clave pendiente', v_n = 1);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 2. Consumo automático y consumo pendiente (RN17, RN53) ===');
    como(u_alm);
    PKG_INVENTARIO.registrar(v_insumo, v_sede, 'ENTRADA', 'Stock inicial', 10);
    PKG_INVENTARIO.registrar(v_prod, v_sede, 'ENTRADA', 'Stock inicial', 10);
    ok('Almacén registra 10 ml del insumo y 10 unidades del producto');
    como(u_esp);
    INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min)
    VALUES (c1, p_esp, r1, v_var, SYSTIMESTAMP - INTERVAL '3' HOUR, 30) RETURNING id_atencion INTO v_at;
    UPDATE ATENCION SET estado = 'FINALIZADA' WHERE id_atencion = v_at;
    ok('La especialista registra y finaliza su atención');
    debe_funcionar('Salida automática de la especialista con id_atencion (consumo estimado, RN53)',
        'INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, id_usuario, id_atencion) VALUES ('
        || v_insumo || ', ' || v_sede || ', ''SALIDA'', ''Consumo estimado de la atención'', 7, 0, ' || u_esp || ', ' || v_at || ')');
    SELECT stock_actual INTO v_n FROM INVENTARIO_SEDE WHERE id_producto = v_insumo AND id_sede = v_sede;
    comprobar('El stock del insumo bajó de 10 a 3 -> ' || v_n, v_n = 3);
    debe_fallar('La misma salida sin id_atencion (RN17)',
        'INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, id_usuario) VALUES ('
        || v_insumo || ', ' || v_sede || ', ''SALIDA'', ''Consumo estimado de la atención'', 1, 0, ' || u_esp || ')', -20031);
    debe_fallar('Una ENTRADA con id_atencion de la especialista (solo se admite la SALIDA)',
        'INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, id_usuario, id_atencion) VALUES ('
        || v_insumo || ', ' || v_sede || ', ''ENTRADA'', ''x'', 1, 0, ' || u_esp || ', ' || v_at || ')', -20031);
    como(u_esp2);
    debe_fallar('Otra especialista descuenta el consumo de una atención que no es suya',
        'INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, id_usuario, id_atencion) VALUES ('
        || v_insumo || ', ' || v_sede || ', ''SALIDA'', ''x'', 1, 0, ' || u_esp2 || ', ' || v_at || ')', -20031);
    como(u_rec);
    debe_fallar('La recepcionista descuenta el consumo de una atención',
        'INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, id_usuario, id_atencion) VALUES ('
        || v_insumo || ', ' || v_sede || ', ''SALIDA'', ''x'', 1, 0, ' || u_rec || ', ' || v_at || ')', -20031);

    como(u_esp);
    debe_fallar('Un consumo pendiente que nace REGULARIZADO',
        'INSERT INTO CONSUMO_PENDIENTE (id_atencion, id_producto, id_sede, cantidad_estimada, cantidad_descontada, cantidad_pendiente, estado) VALUES ('
        || v_at || ', ' || v_insumo || ', ' || v_sede || ', 10, 7, 3, ''REGULARIZADO'')', -20103);
    debe_fallar('Un consumo pendiente con la diferencia mal calculada',
        'INSERT INTO CONSUMO_PENDIENTE (id_atencion, id_producto, id_sede, cantidad_estimada, cantidad_descontada, cantidad_pendiente) VALUES ('
        || v_at || ', ' || v_insumo || ', ' || v_sede || ', 10, 7, 2)', -2290);
    INSERT INTO CONSUMO_PENDIENTE (id_atencion, id_producto, id_sede, cantidad_estimada, cantidad_descontada, cantidad_pendiente)
    VALUES (v_at, v_insumo, v_sede, 10, 7, 3) RETURNING id_consumo INTO v_cons;
    ok('Al finalizar, faltaron 3 ml: queda un consumo pendiente (estimado 10, descontado 7)');
    SELECT COUNT(*) INTO v_n FROM AUDITORIA WHERE entidad = 'CONSUMO_PENDIENTE' AND id_entidad = v_cons AND id_usuario = u_esp;
    comprobar('La auditoría registró el consumo pendiente con el id de la especialista', v_n = 1);
    debe_fallar('La especialista descarta el consumo pendiente (RN17)',
        'UPDATE CONSUMO_PENDIENTE SET estado = ''DESCARTADO'', observacion = ''x'' WHERE id_consumo = ' || v_cons, -20104);
    como(u_alm);
    debe_fallar('Regularizar sin SALIDA de kardex',
        'UPDATE CONSUMO_PENDIENTE SET estado = ''REGULARIZADO'' WHERE id_consumo = ' || v_cons, -20105);
    INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, id_usuario)
    VALUES (v_insumo, v_sede, 'SALIDA', 'Merma', 1, 0, u_alm) RETURNING id_movimiento INTO v_mov_otro;
    debe_fallar('Regularizar con una SALIDA que no es de esa atención',
        'UPDATE CONSUMO_PENDIENTE SET estado = ''REGULARIZADO'', id_movimiento = ' || v_mov_otro || ' WHERE id_consumo = ' || v_cons, -20105);
    INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, id_usuario, id_atencion)
    VALUES (v_insumo, v_sede, 'SALIDA', 'Regularización del consumo pendiente', 2, 0, u_alm, v_at) RETURNING id_movimiento INTO v_mov;
    debe_funcionar('Almacén regulariza con una SALIDA del mismo insumo, sede y atención (CUS33)',
        'UPDATE CONSUMO_PENDIENTE SET estado = ''REGULARIZADO'', id_movimiento = ' || v_mov || ' WHERE id_consumo = ' || v_cons);
    SELECT COUNT(*) INTO v_n FROM CONSUMO_PENDIENTE WHERE id_consumo = v_cons AND id_usuario_resuelve = u_alm AND fecha_resolucion IS NOT NULL;
    comprobar('Oracle fijó quién y cuándo lo regularizó', v_n = 1);
    debe_fallar('Un consumo regularizado no cambia',
        'UPDATE CONSUMO_PENDIENTE SET observacion = ''otro'' WHERE id_consumo = ' || v_cons, -20103);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 3. Solicitud de anulación (RN32) ===');
    como(u_rec);
    INSERT INTO CAJA (id_sede, id_usuario_apertura, monto_inicial) VALUES (v_sede, u_rec, 100) RETURNING id_caja INTO v_caja;
    ok('Recepcionista abre la caja con S/ 100');
    debe_fallar('Venta con id_usuario_autoriza (columna obsoleta desde v5.1)',
        'INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, id_usuario_autoriza, subtotal, igv, total) VALUES ('
        || v_sede || ', ' || v_caja || ', ' || c1 || ', ' || u_rec || ', ' || u_adm || ', 0, 0, 0)', -20100);
    v_venta := venta_cerrada(v_caja);
    ok('Venta de S/ 30 cerrada con boleta');
    INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total)
    VALUES (v_sede, v_caja, c1, u_rec, 0, 0, 0) RETURNING id_venta INTO v_venta_ab;
    debe_fallar('Pedir anular una venta ABIERTA',
        'INSERT INTO SOLICITUD_ANULACION (id_venta, motivo, id_usuario_solicita) VALUES (' || v_venta_ab || ', ''x'', ' || u_rec || ')', -20101);
    DELETE FROM VENTA WHERE id_venta = v_venta_ab;
    debe_fallar('Una solicitud que nace APROBADA',
        'INSERT INTO SOLICITUD_ANULACION (id_venta, motivo, id_usuario_solicita, estado) VALUES (' || v_venta || ', ''x'', ' || u_rec || ', ''APROBADA'')', -20102);
    debe_funcionar('Recepcionista pide anular la venta cerrada (CUS22)',
        'INSERT INTO SOLICITUD_ANULACION (id_venta, motivo, id_usuario_solicita) VALUES (' || v_venta || ', ''Cobro duplicado'', ' || u_adm || ')');
    SELECT id_solicitud INTO v_sol FROM SOLICITUD_ANULACION WHERE id_venta = v_venta AND estado = 'PENDIENTE';
    SELECT COUNT(*) INTO v_n FROM SOLICITUD_ANULACION WHERE id_solicitud = v_sol AND id_usuario_solicita = u_rec AND fecha_solicitud IS NOT NULL;
    comprobar('Oracle fijó quién la pidió (la recepcionista, no el id enviado) y cuándo', v_n = 1);
    debe_fallar('Segunda solicitud PENDIENTE de la misma venta (UQ_SOL_ANUL_PENDIENTE)',
        'INSERT INTO SOLICITUD_ANULACION (id_venta, motivo, id_usuario_solicita) VALUES (' || v_venta || ', ''Otra vez'', ' || u_rec || ')', -1);
    debe_fallar('Recepcionista aprueba la solicitud',
        'UPDATE SOLICITUD_ANULACION SET estado = ''APROBADA'' WHERE id_solicitud = ' || v_sol, -20003);
    como(u_adm);
    debe_fallar('La Administradora rechaza sin motivo',
        'UPDATE SOLICITUD_ANULACION SET estado = ''RECHAZADA'' WHERE id_solicitud = ' || v_sol, -2290);
    debe_funcionar('La Administradora rechaza con su motivo (CUS23)',
        'UPDATE SOLICITUD_ANULACION SET estado = ''RECHAZADA'', motivo_rechazo = ''La clienta se llevó el producto'' WHERE id_solicitud = ' || v_sol);
    SELECT COUNT(*) INTO v_n FROM SOLICITUD_ANULACION WHERE id_solicitud = v_sol AND id_usuario_resuelve = u_adm AND fecha_resolucion IS NOT NULL;
    comprobar('Oracle fijó quién y cuándo la resolvió', v_n = 1);
    debe_fallar('Una solicitud rechazada no cambia',
        'UPDATE SOLICITUD_ANULACION SET estado = ''APROBADA'' WHERE id_solicitud = ' || v_sol, -20102);
    como(u_rec);
    debe_funcionar('Después del rechazo se puede pedir otra vez',
        'INSERT INTO SOLICITUD_ANULACION (id_venta, motivo, id_usuario_solicita) VALUES (' || v_venta || ', ''Producto fallado'', ' || u_rec || ')');
    SELECT id_solicitud INTO v_sol FROM SOLICITUD_ANULACION WHERE id_venta = v_venta AND estado = 'PENDIENTE';
    como(u_adm);
    debe_funcionar('La Administradora aprueba la solicitud',
        'UPDATE SOLICITUD_ANULACION SET estado = ''APROBADA'' WHERE id_solicitud = ' || v_sol);

    -- -----------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE('=== 4. Reapertura de la caja (RN30, RF66) ===');
    como(u_rec);
    FOR m IN (SELECT id_medio_pago FROM MEDIO_PAGO WHERE estado = 'ACTIVO') LOOP
        INSERT INTO CAJA_ARQUEO (id_caja, id_medio_pago, monto_esperado, monto_contado, diferencia)
        VALUES (v_caja, m.id_medio_pago, 0, PKG_CAJA.esperado(v_caja, m.id_medio_pago, 100), 0);
    END LOOP;
    debe_funcionar('Cerrar la caja con el arqueo cuadrado',
        'UPDATE CAJA SET estado = ''CERRADA'', id_usuario_cierre = ' || u_rec || ' WHERE id_caja = ' || v_caja);
    debe_fallar('Vender en la caja cerrada (RN30)',
        'INSERT INTO VENTA (id_sede, id_caja, id_cliente, id_usuario, subtotal, igv, total) VALUES ('
        || v_sede || ', ' || v_caja || ', ' || c1 || ', ' || u_rec || ', 0, 0, 0)', -20042);
    debe_fallar('Recepcionista reabre la caja con motivo',
        'UPDATE CAJA SET estado = ''ABIERTA'', motivo_reapertura = ''Venta olvidada'' WHERE id_caja = ' || v_caja, -20003);
    como(u_adm);
    debe_fallar('La Administradora reabre la caja sin motivo',
        'UPDATE CAJA SET estado = ''ABIERTA'' WHERE id_caja = ' || v_caja, -20060);
    SAVEPOINT sp_otra_caja;
    como(u_rec);
    INSERT INTO CAJA (id_sede, id_usuario_apertura, monto_inicial) VALUES (v_sede, u_rec, 50);
    como(u_adm);
    debe_fallar('Reabrir con otra caja ABIERTA en la sede (UQ_CAJA_ABIERTA_SEDE)',
        'UPDATE CAJA SET estado = ''ABIERTA'', motivo_reapertura = ''Venta olvidada'' WHERE id_caja = ' || v_caja, -1);
    ROLLBACK TO sp_otra_caja;
    debe_funcionar('La Administradora reabre la caja con motivo (CUS46)',
        'UPDATE CAJA SET estado = ''ABIERTA'', motivo_reapertura = ''Venta de la tarde sin registrar'' WHERE id_caja = ' || v_caja);
    SELECT COUNT(*) INTO v_n FROM CAJA
     WHERE id_caja = v_caja AND id_usuario_reapertura = u_adm AND fecha_reapertura IS NOT NULL
       AND id_usuario_cierre IS NULL AND fecha_cierre IS NULL AND id_usuario_revisa IS NULL AND fecha_revision IS NULL;
    comprobar('Oracle fijó la reapertura y limpió el cierre y la revisión', v_n = 1);
    SELECT COUNT(*) INTO v_n FROM AUDITORIA WHERE entidad = 'CAJA' AND id_entidad = v_caja AND id_usuario = u_adm AND detalle LIKE '%estado: CERRADA -> ABIERTA%';
    comprobar('La reapertura quedó en AUDITORIA con el id de la Administradora', v_n = 1);
    debe_fallar('Cambiar el motivo de la reapertura sin reabrir',
        'UPDATE CAJA SET motivo_reapertura = ''Otro motivo'' WHERE id_caja = ' || v_caja, -20060);
    como(u_rec);
    v_venta2 := venta_cerrada(v_caja);
    ok('Con la caja reabierta, la recepcionista registra la venta tardía');
    debe_fallar('Cerrar sin volver a registrar el arqueo',
        'UPDATE CAJA SET estado = ''CERRADA'', id_usuario_cierre = ' || u_rec || ' WHERE id_caja = ' || v_caja, -20062);
    UPDATE CAJA_ARQUEO SET monto_contado = PKG_CAJA.esperado(v_caja, id_medio_pago, 100) WHERE id_caja = v_caja;
    debe_funcionar('Cerrar otra vez con el arqueo registrado de nuevo',
        'UPDATE CAJA SET estado = ''CERRADA'', id_usuario_cierre = ' || u_rec || ' WHERE id_caja = ' || v_caja);
    SELECT COUNT(*) INTO v_n FROM CAJA
     WHERE id_caja = v_caja AND estado = 'CERRADA' AND fecha_cierre IS NOT NULL AND id_usuario_reapertura = u_adm;
    comprobar('La caja quedó CERRADA y conserva los datos de la última reapertura', v_n = 1);
    SELECT monto_esperado INTO v_n FROM CAJA_ARQUEO WHERE id_caja = v_caja AND id_medio_pago = v_efectivo;
    comprobar('Efectivo esperado = 100 inicial + 30 + 30 de la venta tardía = 160 -> ' || v_n, v_n = 160);

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
