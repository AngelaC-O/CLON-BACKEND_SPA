-- =====================================================================
-- CAUTIVA SPA E.I.R.L. · BackOffice web · Base de datos Oracle (12c o superior)
-- Script 08 (v5) · Paquetes PL/SQL que usan los triggers de reglas y de auditoría
-- Se ejecuta como: dueño del esquema v5 (por ejemplo CAUTIVA5), después de 01 a 07.
-- [v5] Adaptado del script 12 de v4 al modelo de 55 tablas (27/10/2026):
--   * el stock vive solo en INVENTARIO_SEDE (PRODUCTO ya no tiene stock_actual ni stock_minimo);
--   * COMPROBANTE es el supertipo de BOLETA, FACTURA, NC_BOLETA y NC_FACTURA: la boleta o factura de una venta
--     se busca con tipo IN ('BOLETA', 'FACTURA') y la nota de crédito con NOTA_CREDITO.id_nota_credito = COMPROBANTE.id_comprobante;
--   * la sesión de paquete consumida es ATENCION.id_cliente_paquete (CONSUMO_SESION se retiró).
--
--   PKG_SESION     quién es el usuario del BackOffice en esta conexión, parámetros y auditoría
--   PKG_AGENDA     cruces de especialista y recurso con tiempo de limpieza (RN02, RN03, RN34)
--   PKG_INVENTARIO único camino para cambiar el stock: el kardex (RN14, RN18)
--   PKG_CAJA       lo esperado por medio de pago en el cierre (RF63)
--   PKG_VENTA      validaciones de cierre y efectos de cerrar o anular una venta (RN10, RN11, RN12)
--                  [v4] numeración de comprobantes electrónicos y estados del envío a SUNAT (RN47, RF100)
--                  [v5] la numeración sirve igual a boletas, facturas y notas de crédito: todas son filas de COMPROBANTE
--
-- Códigos de error propios: -20001 a -20099. Los mensajes dicen qué pasó y cómo corregirlo (RNF27).
-- =====================================================================
SET DEFINE OFF

-- =====================================================================
-- PKG_SESION
-- =====================================================================
PROMPT Paquete PKG_SESION...
CREATE OR REPLACE PACKAGE PKG_SESION AS
    -- El backend lo llama cada vez que toma una conexión del pool para atender a un usuario:
    --   BEGIN PKG_SESION.iniciar(:id_usuario); END;
    -- y al devolverla:  BEGIN PKG_SESION.terminar; END;
    PROCEDURE iniciar(p_id_usuario IN NUMBER);
    PROCEDURE terminar;
    FUNCTION  usuario_actual RETURN NUMBER;
    FUNCTION  es_aplicacion RETURN BOOLEAN;
    FUNCTION  es_administradora(p_id_usuario IN NUMBER) RETURN BOOLEAN;
    -- Nombre del rol del usuario de la sesión (NULL en mantenimiento)
    FUNCTION  rol_actual RETURN VARCHAR2;
    -- Exige que el usuario de la sesión sea la Administradora (RN24, RN25, RN38, RN39).
    -- En una sesión de mantenimiento (dueño o DBA, sin usuario del BackOffice) no exige nada.
    PROCEDURE exigir_administradora(p_accion IN VARCHAR2);
    FUNCTION  parametro_num(p_codigo IN VARCHAR2) RETURN NUMBER;
    -- TRUE si dos valores son distintos, tratando NULL = NULL (DECODE solo existe dentro de SQL)
    FUNCTION  distinto(a IN VARCHAR2,  b IN VARCHAR2)  RETURN BOOLEAN;
    FUNCTION  distinto(a IN NUMBER,    b IN NUMBER)    RETURN BOOLEAN;
    FUNCTION  distinto(a IN DATE,      b IN DATE)      RETURN BOOLEAN;
    FUNCTION  distinto(a IN TIMESTAMP, b IN TIMESTAMP) RETURN BOOLEAN;
    -- Auditoría
    FUNCTION  diff(p_columna IN VARCHAR2, p_anterior IN VARCHAR2, p_nuevo IN VARCHAR2) RETURN VARCHAR2;
    FUNCTION  par(p_columna IN VARCHAR2, p_valor IN VARCHAR2) RETURN VARCHAR2;
    PROCEDURE auditar(p_modulo IN VARCHAR2, p_accion IN VARCHAR2, p_entidad IN VARCHAR2,
                      p_id_entidad IN NUMBER, p_detalle IN VARCHAR2, p_actor_defecto IN NUMBER DEFAULT NULL);
END PKG_SESION;
/

CREATE OR REPLACE PACKAGE BODY PKG_SESION AS

    c_cuenta_app CONSTANT VARCHAR2(30) := 'CAUTIVA_APP';
    -- Usuario y rol de la sesión, leídos UNA vez en iniciar. Los triggers consultan estas variables
    -- y no la tabla USUARIO, para no caer en ORA-04091 (tabla mutante) cuando el trigger es de USUARIO.
    g_id_usuario NUMBER;
    g_rol        VARCHAR2(30);

    PROCEDURE iniciar(p_id_usuario IN NUMBER) IS
        v_estado USUARIO.estado%TYPE;
        v_rol    ROL.nombre%TYPE;
    BEGIN
        SELECT u.estado, r.nombre INTO v_estado, v_rol
          FROM USUARIO u JOIN ROL r ON r.id_rol = u.id_rol
         WHERE u.id_usuario = p_id_usuario;
        IF v_estado <> 'ACTIVO' THEN
            RAISE_APPLICATION_ERROR(-20004, 'El usuario ' || p_id_usuario || ' está ' || v_estado || ' y no puede operar.');
        END IF;
        g_id_usuario := p_id_usuario;
        g_rol        := v_rol;
        DBMS_SESSION.SET_IDENTIFIER(TO_CHAR(p_id_usuario));
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20004, 'El usuario ' || p_id_usuario || ' no existe.');
    END iniciar;

    PROCEDURE terminar IS
    BEGIN
        g_id_usuario := NULL;
        g_rol        := NULL;
        DBMS_SESSION.CLEAR_IDENTIFIER;
    END terminar;

    FUNCTION usuario_actual RETURN NUMBER IS
        v VARCHAR2(64) := SYS_CONTEXT('USERENV', 'CLIENT_IDENTIFIER');
    BEGIN
        IF v IS NULL OR NOT REGEXP_LIKE(v, '^[0-9]{1,15}$') THEN
            RETURN NULL;
        END IF;
        RETURN TO_NUMBER(v);
    END usuario_actual;

    FUNCTION es_aplicacion RETURN BOOLEAN IS
    BEGIN
        RETURN SYS_CONTEXT('USERENV', 'SESSION_USER') = c_cuenta_app;
    END es_aplicacion;

    FUNCTION es_administradora(p_id_usuario IN NUMBER) RETURN BOOLEAN IS
        v_n NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_n
          FROM USUARIO u
          JOIN ROL r ON r.id_rol = u.id_rol
         WHERE u.id_usuario = p_id_usuario
           AND u.estado = 'ACTIVO'
           AND r.nombre = 'ADMINISTRADORA'
           AND r.estado = 'ACTIVO';
        RETURN v_n > 0;
    END es_administradora;

    FUNCTION rol_actual RETURN VARCHAR2 IS
    BEGIN
        -- Solo vale el rol cargado por iniciar para ESE usuario; si el identificador se fijó por otro
        -- medio, no hay rol (y por lo tanto no hay permisos de Administradora).
        IF usuario_actual IS NULL OR g_id_usuario IS NULL OR usuario_actual <> g_id_usuario THEN
            RETURN NULL;
        END IF;
        RETURN g_rol;
    END rol_actual;

    PROCEDURE exigir_administradora(p_accion IN VARCHAR2) IS
        v_u NUMBER := usuario_actual;
    BEGIN
        IF v_u IS NULL THEN
            IF es_aplicacion THEN
                RAISE_APPLICATION_ERROR(-20001,
                    'La conexión no identifica al usuario del BackOffice. El backend debe llamar a PKG_SESION.iniciar.');
            END IF;
            RETURN;   -- mantenimiento del dueño o del DBA
        END IF;
        IF NVL(rol_actual, '-') <> 'ADMINISTRADORA' THEN
            RAISE_APPLICATION_ERROR(-20003, 'Solo la Administradora puede ' || p_accion || '.');
        END IF;
    END exigir_administradora;

    FUNCTION parametro_num(p_codigo IN VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        SELECT valor_numero INTO v FROM PARAMETRO WHERE codigo = p_codigo;
        IF v IS NULL THEN
            RAISE NO_DATA_FOUND;
        END IF;
        RETURN v;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20002, 'Falta el parámetro numérico ' || p_codigo || ' en la tabla PARAMETRO.');
    END parametro_num;

    FUNCTION distinto(a IN VARCHAR2, b IN VARCHAR2) RETURN BOOLEAN IS
    BEGIN
        RETURN (a IS NULL AND b IS NOT NULL) OR (a IS NOT NULL AND b IS NULL) OR a <> b;
    END distinto;

    FUNCTION distinto(a IN NUMBER, b IN NUMBER) RETURN BOOLEAN IS
    BEGIN
        RETURN (a IS NULL AND b IS NOT NULL) OR (a IS NOT NULL AND b IS NULL) OR a <> b;
    END distinto;

    FUNCTION distinto(a IN DATE, b IN DATE) RETURN BOOLEAN IS
    BEGIN
        RETURN (a IS NULL AND b IS NOT NULL) OR (a IS NOT NULL AND b IS NULL) OR a <> b;
    END distinto;

    FUNCTION distinto(a IN TIMESTAMP, b IN TIMESTAMP) RETURN BOOLEAN IS
    BEGIN
        RETURN (a IS NULL AND b IS NOT NULL) OR (a IS NOT NULL AND b IS NULL) OR a <> b;
    END distinto;

    FUNCTION diff(p_columna IN VARCHAR2, p_anterior IN VARCHAR2, p_nuevo IN VARCHAR2) RETURN VARCHAR2 IS
    BEGIN
        IF (p_anterior IS NULL AND p_nuevo IS NULL) OR p_anterior = p_nuevo THEN
            RETURN NULL;
        END IF;
        RETURN p_columna || ': ' || NVL(SUBSTR(p_anterior, 1, 60), '(vacío)') || ' -> '
               || NVL(SUBSTR(p_nuevo, 1, 60), '(vacío)') || '; ';
    END diff;

    FUNCTION par(p_columna IN VARCHAR2, p_valor IN VARCHAR2) RETURN VARCHAR2 IS
    BEGIN
        IF p_valor IS NULL THEN
            RETURN NULL;
        END IF;
        RETURN p_columna || '=' || SUBSTR(p_valor, 1, 60) || '; ';
    END par;

    PROCEDURE auditar(p_modulo IN VARCHAR2, p_accion IN VARCHAR2, p_entidad IN VARCHAR2,
                      p_id_entidad IN NUMBER, p_detalle IN VARCHAR2, p_actor_defecto IN NUMBER DEFAULT NULL) IS
        v_u NUMBER := NVL(usuario_actual, p_actor_defecto);
    BEGIN
        IF v_u IS NULL THEN
            IF es_aplicacion THEN
                RAISE_APPLICATION_ERROR(-20001,
                    'La conexión no identifica al usuario del BackOffice (RN29). El backend debe llamar a PKG_SESION.iniciar.');
            END IF;
            RETURN;   -- cambios de mantenimiento: quedan en la auditoría de Oracle, no en la del negocio
        END IF;
        INSERT INTO AUDITORIA (id_usuario, fecha_hora, modulo, accion, entidad, id_entidad, detalle)
        VALUES (v_u, SYSTIMESTAMP, SUBSTR(p_modulo, 1, 40), SUBSTR(p_accion, 1, 40), SUBSTR(p_entidad, 1, 40),
                p_id_entidad, SUBSTR(p_detalle, 1, 400));
    END auditar;

END PKG_SESION;
/

-- =====================================================================
-- PKG_AGENDA
-- =====================================================================
PROMPT Paquete PKG_AGENDA...
CREATE OR REPLACE PACKAGE PKG_AGENDA AS
    -- Fin del bloque ocupado = inicio + minutos + limpieza del recurso (RN03)
    FUNCTION  fin_con_limpieza(p_inicio IN TIMESTAMP, p_minutos IN NUMBER, p_id_recurso IN NUMBER) RETURN TIMESTAMP;
    -- Error -20010 si la especialista o el recurso ya están ocupados en [p_inicio, p_fin)
    PROCEDURE validar_cruce(p_id_personal IN NUMBER, p_id_recurso IN NUMBER,
                            p_inicio IN TIMESTAMP, p_fin IN TIMESTAMP,
                            p_excluir_cita IN NUMBER, p_excluir_atencion IN NUMBER);
    PROCEDURE validar_cita(p_id_cita IN NUMBER);
    PROCEDURE validar_atencion(p_id_atencion IN NUMBER);
    -- Valida que la especialista atienda el área del servicio de la variante (RF83)
    PROCEDURE validar_habilitacion(p_id_personal IN NUMBER, p_id_variante IN NUMBER);
END PKG_AGENDA;
/

CREATE OR REPLACE PACKAGE BODY PKG_AGENDA AS

    FUNCTION fin_con_limpieza(p_inicio IN TIMESTAMP, p_minutos IN NUMBER, p_id_recurso IN NUMBER) RETURN TIMESTAMP IS
        v_limpieza RECURSO.minutos_limpieza%TYPE;
    BEGIN
        SELECT minutos_limpieza INTO v_limpieza FROM RECURSO WHERE id_recurso = p_id_recurso;
        RETURN p_inicio + NUMTODSINTERVAL(p_minutos + v_limpieza, 'MINUTE');
    END fin_con_limpieza;

    PROCEDURE validar_cruce(p_id_personal IN NUMBER, p_id_recurso IN NUMBER,
                            p_inicio IN TIMESTAMP, p_fin IN TIMESTAMP,
                            p_excluir_cita IN NUMBER, p_excluir_atencion IN NUMBER) IS
        v_dummy NUMBER;
        v_id    NUMBER;
        v_ini   TIMESTAMP;
        v_que   VARCHAR2(20);
    BEGIN
        -- Serializa a quienes agendan a la misma especialista o el mismo recurso al mismo tiempo:
        -- la segunda sesión espera aquí hasta que la primera confirme, y entonces ve su cita.
        SELECT id_personal INTO v_dummy FROM PERSONAL WHERE id_personal = p_id_personal FOR UPDATE;
        SELECT id_recurso  INTO v_dummy FROM RECURSO  WHERE id_recurso  = p_id_recurso  FOR UPDATE;

        BEGIN
            SELECT id_cita, fecha_hora_inicio, quien
              INTO v_id, v_ini, v_que
              FROM (SELECT id_cita, fecha_hora_inicio,
                           CASE WHEN id_personal = p_id_personal THEN 'la especialista' ELSE 'el recurso' END AS quien
                      FROM CITA
                     WHERE estado IN ('PENDIENTE', 'EN_ATENCION')
                       AND (id_personal = p_id_personal OR id_recurso = p_id_recurso)
                       AND id_cita <> NVL(p_excluir_cita, -1)
                       AND fecha_hora_inicio < p_fin
                       AND fecha_hora_fin > p_inicio
                     ORDER BY fecha_hora_inicio)
             WHERE ROWNUM = 1;
            RAISE_APPLICATION_ERROR(-20010, 'Cruce de horario (RN02): ' || v_que || ' ya tiene la cita N° ' || v_id
                || ' que empieza a las ' || TO_CHAR(v_ini, 'HH24:MI') || ' (incluye el tiempo de limpieza del recurso).');
        EXCEPTION
            WHEN NO_DATA_FOUND THEN NULL;
        END;

        BEGIN
            SELECT id_atencion, hora_inicio, quien
              INTO v_id, v_ini, v_que
              FROM (SELECT a.id_atencion, a.hora_inicio,
                           CASE WHEN a.id_personal = p_id_personal THEN 'la especialista' ELSE 'el recurso' END AS quien
                      FROM ATENCION a
                      JOIN RECURSO r ON r.id_recurso = a.id_recurso
                     WHERE NVL(a.id_cita, -1) <> NVL(p_excluir_cita, -2)   -- la atención de la propia cita no choca con ella
                       AND (a.id_personal = p_id_personal OR a.id_recurso = p_id_recurso)
                       AND a.id_atencion <> NVL(p_excluir_atencion, -1)
                       AND a.hora_inicio < p_fin
                       AND NVL(a.hora_fin, a.hora_inicio + NUMTODSINTERVAL(a.tiempo_declarado_min, 'MINUTE'))
                           + NUMTODSINTERVAL(r.minutos_limpieza, 'MINUTE') > p_inicio
                     ORDER BY a.hora_inicio)
             WHERE ROWNUM = 1;
            RAISE_APPLICATION_ERROR(-20011, 'Cruce de horario (RN34): ' || v_que || ' está en la atención N° ' || v_id
                || ' desde las ' || TO_CHAR(v_ini, 'HH24:MI') || '.');
        EXCEPTION
            WHEN NO_DATA_FOUND THEN NULL;
        END;
    END validar_cruce;

    PROCEDURE validar_cita(p_id_cita IN NUMBER) IS
        r      CITA%ROWTYPE;
        v_cli  CITA.id_cliente%TYPE;
        v_adel CITA.adelanto_monto%TYPE;
    BEGIN
        SELECT * INTO r FROM CITA WHERE id_cita = p_id_cita;
        -- RN05: la cita reprogramada es de la misma clienta y hereda el adelanto completo
        IF r.id_cita_origen IS NOT NULL THEN
            SELECT id_cliente, adelanto_monto INTO v_cli, v_adel FROM CITA WHERE id_cita = r.id_cita_origen;
            IF v_cli <> r.id_cliente THEN
                RAISE_APPLICATION_ERROR(-20015, 'La cita reprogramada debe ser de la misma clienta que la cita original.');
            END IF;
            IF r.adelanto_monto <> v_adel THEN
                RAISE_APPLICATION_ERROR(-20015, 'Al reprogramar, el adelanto pasa completo a la nueva cita (RN05): S/ ' || v_adel || '.');
            END IF;
        END IF;
        IF r.estado IN ('PENDIENTE', 'EN_ATENCION') THEN
            validar_cruce(r.id_personal, r.id_recurso, r.fecha_hora_inicio, r.fecha_hora_fin, r.id_cita, NULL);
        END IF;
    END validar_cita;

    PROCEDURE validar_atencion(p_id_atencion IN NUMBER) IS
        r     ATENCION%ROWTYPE;
        v_fin TIMESTAMP;
    BEGIN
        SELECT * INTO r FROM ATENCION WHERE id_atencion = p_id_atencion;
        -- RN34 y RN41 (confirmada 18/10/2026): toda atención en curso ocupa a la especialista y al recurso por el
        -- tiempo que ella declara, más la limpieza. Si ese tiempo choca con otra cita, la BD lo impide:
        -- primero se reprograma la cita siguiente y luego se declara el tiempo. La propia cita no cuenta como cruce.
        IF r.estado = 'EN_CURSO' THEN
            v_fin := fin_con_limpieza(r.hora_inicio, r.tiempo_declarado_min, r.id_recurso);
            validar_cruce(r.id_personal, r.id_recurso, r.hora_inicio, v_fin, r.id_cita, r.id_atencion);
        END IF;
    END validar_atencion;

    PROCEDURE validar_habilitacion(p_id_personal IN NUMBER, p_id_variante IN NUMBER) IS
        v_n NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_n
          FROM VARIANTE_SERVICIO v
          JOIN SERVICIO s       ON s.id_servicio = v.id_servicio
          JOIN PERSONAL_AREA pa ON pa.id_area = s.id_area
         WHERE v.id_variante = p_id_variante
           AND pa.id_personal = p_id_personal;
        IF v_n = 0 THEN
            RAISE_APPLICATION_ERROR(-20012,
                'La especialista no está habilitada para el área de este servicio (RF83). Asígnele el área en PERSONAL_AREA.');
        END IF;
    END validar_habilitacion;

END PKG_AGENDA;
/

-- =====================================================================
-- PKG_INVENTARIO
-- =====================================================================
PROMPT Paquete PKG_INVENTARIO...
CREATE OR REPLACE PACKAGE PKG_INVENTARIO AS
    -- TRUE solo mientras el trigger del kardex actualiza el stock
    FUNCTION  desde_kardex RETURN BOOLEAN;
    -- Aplica un movimiento al stock de la sede; devuelve el saldo de la sede.
    -- [v5] Ya no hay consolidado en PRODUCTO: el stock total es SUM(INVENTARIO_SEDE.stock_actual).
    PROCEDURE aplicar_movimiento(p_id_producto IN NUMBER, p_id_sede IN NUMBER, p_delta IN NUMBER, p_saldo OUT NUMBER);
    -- Atajo para el backend: registra un movimiento de kardex (el trigger hace el resto)
    PROCEDURE registrar(p_id_producto IN NUMBER, p_id_sede IN NUMBER, p_tipo IN VARCHAR2, p_motivo IN VARCHAR2,
                        p_cantidad IN NUMBER, p_id_detalle_venta IN NUMBER DEFAULT NULL,
                        p_id_detalle_compra IN NUMBER DEFAULT NULL, p_id_atencion IN NUMBER DEFAULT NULL);
END PKG_INVENTARIO;
/

CREATE OR REPLACE PACKAGE BODY PKG_INVENTARIO AS

    g_desde_kardex BOOLEAN := FALSE;

    FUNCTION desde_kardex RETURN BOOLEAN IS
    BEGIN
        RETURN g_desde_kardex;
    END desde_kardex;

    PROCEDURE aplicar_movimiento(p_id_producto IN NUMBER, p_id_sede IN NUMBER, p_delta IN NUMBER, p_saldo OUT NUMBER) IS
        v_stock  INVENTARIO_SEDE.stock_actual%TYPE;
        v_nuevo  NUMBER;
        v_nombre PRODUCTO.nombre%TYPE;
    BEGIN
        BEGIN
            SELECT stock_actual INTO v_stock
              FROM INVENTARIO_SEDE
             WHERE id_producto = p_id_producto AND id_sede = p_id_sede
               FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                BEGIN
                    -- [v5] el stock mínimo es propio de cada sede: nace en 0 y lo fija almacén
                    INSERT INTO INVENTARIO_SEDE (id_producto, id_sede, stock_actual, stock_minimo)
                    VALUES (p_id_producto, p_id_sede, 0, 0);
                EXCEPTION
                    WHEN DUP_VAL_ON_INDEX THEN NULL;   -- otra sesión la creó al mismo tiempo
                END;
                SELECT stock_actual INTO v_stock
                  FROM INVENTARIO_SEDE
                 WHERE id_producto = p_id_producto AND id_sede = p_id_sede
                   FOR UPDATE;
        END;

        v_nuevo := v_stock + p_delta;
        IF v_nuevo < 0 THEN
            SELECT nombre INTO v_nombre FROM PRODUCTO WHERE id_producto = p_id_producto;
            RAISE_APPLICATION_ERROR(-20030, 'Stock insuficiente de "' || v_nombre || '": hay ' || v_stock
                || ' y se piden ' || ABS(p_delta) || '. No se vende sin stock: almacén registra primero la compra o el ajuste por conteo (RN14, RN15).');
        END IF;

        g_desde_kardex := TRUE;
        UPDATE INVENTARIO_SEDE SET stock_actual = v_nuevo
         WHERE id_producto = p_id_producto AND id_sede = p_id_sede;
        g_desde_kardex := FALSE;
        p_saldo := v_nuevo;
    EXCEPTION
        WHEN OTHERS THEN
            g_desde_kardex := FALSE;
            RAISE;
    END aplicar_movimiento;

    PROCEDURE registrar(p_id_producto IN NUMBER, p_id_sede IN NUMBER, p_tipo IN VARCHAR2, p_motivo IN VARCHAR2,
                        p_cantidad IN NUMBER, p_id_detalle_venta IN NUMBER DEFAULT NULL,
                        p_id_detalle_compra IN NUMBER DEFAULT NULL, p_id_atencion IN NUMBER DEFAULT NULL) IS
        v_u NUMBER := PKG_SESION.usuario_actual;
    BEGIN
        IF v_u IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Llame primero a PKG_SESION.iniciar: todo movimiento de kardex lleva su usuario (RN18).');
        END IF;
        INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, fecha_hora, id_usuario,
                                       id_detalle_venta, id_detalle_compra, id_atencion)
        VALUES (p_id_producto, p_id_sede, p_tipo, p_motivo, p_cantidad, 0, SYSTIMESTAMP, v_u,
                p_id_detalle_venta, p_id_detalle_compra, p_id_atencion);
    END registrar;

END PKG_INVENTARIO;
/

-- =====================================================================
-- PKG_CAJA
-- =====================================================================
PROMPT Paquete PKG_CAJA...
CREATE OR REPLACE PACKAGE PKG_CAJA AS
    -- Lo esperado en una caja para un medio de pago (RF63):
    --   pagos de ventas CERRADAS (las anuladas no cuentan: RN33)
    -- + adelantos de citas recibidos en esa caja (no los trasladados por reprogramación)
    -- + en EFECTIVO: monto inicial - egresos
    -- p_monto_inicial se recibe como parámetro para poder llamarla desde el trigger de CAJA.
    FUNCTION esperado(p_id_caja IN NUMBER, p_id_medio_pago IN NUMBER, p_monto_inicial IN NUMBER) RETURN NUMBER;
    FUNCTION id_medio(p_nombre IN VARCHAR2) RETURN NUMBER;
END PKG_CAJA;
/

CREATE OR REPLACE PACKAGE BODY PKG_CAJA AS

    FUNCTION id_medio(p_nombre IN VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        SELECT id_medio_pago INTO v FROM MEDIO_PAGO WHERE nombre = p_nombre;
        RETURN v;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20002, 'No existe el medio de pago ' || p_nombre || '.');
    END id_medio;

    FUNCTION esperado(p_id_caja IN NUMBER, p_id_medio_pago IN NUMBER, p_monto_inicial IN NUMBER) RETURN NUMBER IS
        v_ventas    NUMBER;
        v_adelantos NUMBER;
        v_egresos   NUMBER;
        v_nombre    MEDIO_PAGO.nombre%TYPE;
    BEGIN
        SELECT NVL(SUM(p.monto), 0) INTO v_ventas
          FROM VENTA_PAGO p
          JOIN VENTA v ON v.id_venta = p.id_venta
         WHERE v.id_caja = p_id_caja
           AND v.estado = 'CERRADA'
           AND p.id_medio_pago = p_id_medio_pago;

        SELECT NVL(SUM(adelanto_monto), 0) INTO v_adelantos
          FROM CITA
         WHERE id_caja = p_id_caja
           AND id_medio_pago = p_id_medio_pago
           AND id_cita_origen IS NULL;

        SELECT nombre INTO v_nombre FROM MEDIO_PAGO WHERE id_medio_pago = p_id_medio_pago;
        IF v_nombre = 'EFECTIVO' THEN
            SELECT NVL(SUM(monto), 0) INTO v_egresos FROM CAJA_EGRESO WHERE id_caja = p_id_caja;
            RETURN p_monto_inicial + v_ventas + v_adelantos - v_egresos;
        END IF;
        RETURN v_ventas + v_adelantos;
    END esperado;

END PKG_CAJA;
/

-- =====================================================================
-- PKG_VENTA (uso interno de los triggers de VENTA)
-- =====================================================================
PROMPT Paquete PKG_VENTA...
CREATE OR REPLACE PACKAGE PKG_VENTA AS
    -- Reglas para pasar de ABIERTA a CERRADA (RN06, RN08, RN10, RN11, RF25, RF28, RF29)
    PROCEDURE validar_cierre(p_id_venta IN NUMBER, p_subtotal IN NUMBER, p_igv IN NUMBER, p_descuento IN NUMBER,
                             p_adelanto IN NUMBER, p_saldo_favor IN NUMBER, p_total IN NUMBER);
    -- Reglas para anular (RN12, RN32)
    PROCEDURE validar_anulacion(p_id_venta IN NUMBER);
    -- Efectos al cerrar: salida de kardex de productos (RF39) y atenciones COBRADAS
    PROCEDURE al_cerrar(p_id_venta IN NUMBER, p_id_sede IN NUMBER, p_id_usuario IN NUMBER);
    -- Efectos al anular: devolución al kardex, atenciones vuelven a FINALIZADA, paquetes ANULADOS
    -- y devolución del saldo a favor usado (RN33)
    -- [v4] p_total: si la nota de crédito dice SALDO_FAVOR, lo pagado vuelve a la clienta como saldo a favor (RN44)
    PROCEDURE al_anular(p_id_venta IN NUMBER, p_id_sede IN NUMBER, p_id_cliente IN NUMBER,
                        p_saldo_favor IN NUMBER, p_id_usuario IN NUMBER, p_total IN NUMBER DEFAULT 0);
    -- [v5] TRUE solo mientras al_anular registra los abonos de saldo a favor de la venta anulada:
    -- TRG_SALDO_FAVOR_REGLAS rechaza cualquier otro abono sin cita (evita el abono doble, RN33 y RN44).
    g_anulando BOOLEAN := FALSE;

    -- [v4] Facturación electrónica (RN47, RF27, RF33, RF107)
    -- TRUE solo mientras este paquete mueve un correlativo: el trigger de SERIE_COMPROBANTE lo reconoce.
    g_numerando BOOLEAN := FALSE;
    -- Toma el siguiente número de la serie ACTIVA de la sede para BOLETA, FACTURA, NC_BOLETA o NC_FACTURA
    -- ([v5] el mismo tipo que COMPROBANTE.tipo, porque las cuatro clases son filas del supertipo).
    -- El UPDATE bloquea la fila de la serie: dos cobros simultáneos nunca reciben el mismo número.
    PROCEDURE asignar_numero(p_id_sede IN NUMBER, p_tipo IN VARCHAR2, o_serie OUT VARCHAR2, o_numero OUT VARCHAR2);
    -- Devuelve el número si el comprobante descartado era el último de su serie (no deja huecos).
    PROCEDURE liberar_numero(p_serie IN VARCHAR2, p_numero IN VARCHAR2);
    -- Transiciones válidas del envío a SUNAT ([v5] se validan solo en COMPROBANTE: la nota de crédito hereda esas columnas)
    PROCEDURE validar_estado_sunat(p_anterior IN VARCHAR2, p_nuevo IN VARCHAR2,
                                   p_intentos_ant IN NUMBER, p_intentos_nue IN NUMBER, p_documento IN VARCHAR2);
END PKG_VENTA;
/

CREATE OR REPLACE PACKAGE BODY PKG_VENTA AS

    PROCEDURE validar_cierre(p_id_venta IN NUMBER, p_subtotal IN NUMBER, p_igv IN NUMBER, p_descuento IN NUMBER,
                             p_adelanto IN NUMBER, p_saldo_favor IN NUMBER, p_total IN NUMBER) IS
        v_lineas   NUMBER;
        v_importe  NUMBER;
        v_pagos    NUMBER;
        v_uso      NUMBER;
        v_adel_max NUMBER;
        v_n        NUMBER;
        v_igv_pct  NUMBER := PKG_SESION.parametro_num('IGV_PORCENTAJE');
        v_umbral   NUMBER := PKG_SESION.parametro_num('UMBRAL_BOLETA_DOC');
        v_tipo     COMPROBANTE.tipo%TYPE;
        v_nro      COMPROBANTE.nro_doc_comprador%TYPE;
        v_nombre   COMPROBANTE.nombre_comprador%TYPE;
    BEGIN
        SELECT COUNT(*), NVL(SUM(subtotal), 0) INTO v_lineas, v_importe
          FROM VENTA_DETALLE WHERE id_venta = p_id_venta;
        IF v_lineas = 0 THEN
            RAISE_APPLICATION_ERROR(-20050, 'La venta no tiene líneas.');
        END IF;

        -- RN06: Pichanaki está en la Amazonía, exonerada del IGV (Ley 27037): IGV_PORCENTAJE = 0.
        -- Importe = líneas - descuento general = subtotal + IGV (el IGV es 0 mientras rija la exoneración).
        IF p_subtotal + p_igv <> v_importe - p_descuento THEN
            RAISE_APPLICATION_ERROR(-20051, 'Subtotal + IGV (' || TO_CHAR(p_subtotal + p_igv) || ') no coincide con las líneas menos el descuento ('
                || TO_CHAR(v_importe - p_descuento) || ').');
        END IF;
        IF ABS(p_igv - ROUND(p_subtotal * v_igv_pct / 100, 2)) > 0.01 THEN
            RAISE_APPLICATION_ERROR(-20051, 'El IGV debe ser el ' || v_igv_pct || ' % del subtotal (RN06: venta exonerada de IGV en la Amazonía).');
        END IF;

        -- RF25: el adelanto aplicado no puede superar los adelantos de las citas cobradas en esta venta
        SELECT NVL(SUM(c.adelanto_monto), 0) INTO v_adel_max
          FROM VENTA_DETALLE d
          JOIN ATENCION a ON a.id_atencion = d.id_atencion
          JOIN CITA c     ON c.id_cita = a.id_cita
         WHERE d.id_venta = p_id_venta;
        IF p_adelanto > v_adel_max THEN
            RAISE_APPLICATION_ERROR(-20052, 'El adelanto aplicado (S/ ' || p_adelanto || ') supera los adelantos de las citas cobradas (S/ '
                || v_adel_max || ').');
        END IF;

        -- RN05: el saldo a favor aplicado debe estar registrado como USO en SALDO_FAVOR_MOV
        SELECT NVL(SUM(monto), 0) INTO v_uso FROM SALDO_FAVOR_MOV WHERE id_venta = p_id_venta AND tipo = 'USO';
        IF v_uso <> p_saldo_favor THEN
            RAISE_APPLICATION_ERROR(-20053, 'El saldo a favor aplicado (S/ ' || p_saldo_favor || ') no coincide con los usos registrados (S/ '
                || v_uso || ').');
        END IF;

        -- RN11: la suma de los medios de pago es igual al total
        SELECT NVL(SUM(monto), 0) INTO v_pagos FROM VENTA_PAGO WHERE id_venta = p_id_venta;
        IF v_pagos <> p_total THEN
            RAISE_APPLICATION_ERROR(-20054, 'Los pagos registrados suman S/ ' || v_pagos || ' y el total es S/ ' || p_total || ' (RN11).');
        END IF;

        -- RN10 y RF28 [v4]: no se cierra sin su comprobante electrónico generado (serie y número los pone la BD).
        -- Si SUNAT o el proveedor no responden, el comprobante queda PENDIENTE y se reenvía: el cobro no espera.
        BEGIN
            SELECT tipo, nro_doc_comprador, nombre_comprador INTO v_tipo, v_nro, v_nombre
              FROM COMPROBANTE WHERE id_venta = p_id_venta AND tipo IN ('BOLETA', 'FACTURA');
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20055, 'Genere la boleta o la factura electrónica de la venta antes de cerrarla (RN10).');
        END;
        -- RN08: boleta mayor al umbral lleva datos del comprador
        IF v_tipo = 'BOLETA' AND p_subtotal + p_igv > v_umbral AND (v_nro IS NULL OR v_nombre IS NULL) THEN
            RAISE_APPLICATION_ERROR(-20056, 'Una boleta de más de S/ ' || v_umbral || ' lleva el nombre y el documento del comprador (RN08).');
        END IF;

        -- Una atención se cobra una sola vez: debe seguir FINALIZADA (no COBRADA en otra venta)
        SELECT COUNT(*) INTO v_n
          FROM VENTA_DETALLE d
          JOIN ATENCION a ON a.id_atencion = d.id_atencion
         WHERE d.id_venta = p_id_venta
           AND a.estado <> 'FINALIZADA';
        IF v_n > 0 THEN
            RAISE_APPLICATION_ERROR(-20057, 'Una de las atenciones de la venta ya fue cobrada o no está finalizada.');
        END IF;
    END validar_cierre;

    PROCEDURE validar_anulacion(p_id_venta IN NUMBER) IS
        v_n      NUMBER;
        v_estado COMPROBANTE.estado_sunat%TYPE;
    BEGIN
        -- [v5] La nota de crédito es el subtipo de COMPROBANTE (tipo NC_BOLETA o NC_FACTURA) de la misma venta
        SELECT COUNT(*) INTO v_n
          FROM NOTA_CREDITO nc
          JOIN COMPROBANTE c ON c.id_comprobante = nc.id_nota_credito
         WHERE c.id_venta = p_id_venta;
        IF v_n = 0 THEN
            -- [v4] Un comprobante RECHAZADO por SUNAT no tiene validez: la Administradora anula la venta sin nota de crédito
            SELECT MAX(estado_sunat) INTO v_estado FROM COMPROBANTE
             WHERE id_venta = p_id_venta AND tipo IN ('BOLETA', 'FACTURA');
            IF NVL(v_estado, 'X') <> 'RECHAZADO' THEN
                RAISE_APPLICATION_ERROR(-20058, 'Para anular la venta emita primero su nota de crédito electrónica (RN12, RN32).');
            END IF;
            PKG_SESION.exigir_administradora('anular una venta cuyo comprobante rechazó SUNAT');
        END IF;
        -- Un paquete ya usado no se puede anular sin resolver antes sus sesiones
        -- [v5] la sesión usada es la atención que apunta al paquete (ATENCION.id_cliente_paquete)
        SELECT COUNT(*) INTO v_n
          FROM VENTA_DETALLE d
          JOIN CLIENTE_PAQUETE cp ON cp.id_detalle = d.id_detalle
          JOIN ATENCION a         ON a.id_cliente_paquete = cp.id_cliente_paquete
         WHERE d.id_venta = p_id_venta;
        IF v_n > 0 THEN
            RAISE_APPLICATION_ERROR(-20059, 'La venta incluye un paquete con ' || v_n || ' sesión(es) ya consumida(s); no se puede anular completa.');
        END IF;
    END validar_anulacion;

    PROCEDURE al_cerrar(p_id_venta IN NUMBER, p_id_sede IN NUMBER, p_id_usuario IN NUMBER) IS
        v_u NUMBER := NVL(PKG_SESION.usuario_actual, p_id_usuario);
    BEGIN
        FOR d IN (SELECT id_detalle, id_producto, cantidad FROM VENTA_DETALLE
                   WHERE id_venta = p_id_venta AND tipo_item = 'PRODUCTO') LOOP
            INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, fecha_hora, id_usuario, id_detalle_venta)
            VALUES (d.id_producto, p_id_sede, 'SALIDA', 'Venta N° ' || p_id_venta, d.cantidad, 0, SYSTIMESTAMP, v_u, d.id_detalle);
        END LOOP;
        UPDATE ATENCION SET estado = 'COBRADA'
         WHERE id_atencion IN (SELECT id_atencion FROM VENTA_DETALLE WHERE id_venta = p_id_venta AND id_atencion IS NOT NULL);
    END al_cerrar;

    PROCEDURE al_anular(p_id_venta IN NUMBER, p_id_sede IN NUMBER, p_id_cliente IN NUMBER,
                        p_saldo_favor IN NUMBER, p_id_usuario IN NUMBER, p_total IN NUMBER DEFAULT 0) IS
        v_u     NUMBER := NVL(PKG_SESION.usuario_actual, p_id_usuario);
        v_forma NOTA_CREDITO.forma_devolucion%TYPE;
    BEGIN
        FOR d IN (SELECT id_detalle, id_producto, cantidad FROM VENTA_DETALLE
                   WHERE id_venta = p_id_venta AND tipo_item = 'PRODUCTO') LOOP
            INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, fecha_hora, id_usuario, id_detalle_venta)
            VALUES (d.id_producto, p_id_sede, 'ENTRADA', 'Anulación de la venta N° ' || p_id_venta, d.cantidad, 0, SYSTIMESTAMP, v_u, d.id_detalle);
        END LOOP;
        UPDATE ATENCION SET estado = 'FINALIZADA'
         WHERE estado = 'COBRADA'
           AND id_atencion IN (SELECT id_atencion FROM VENTA_DETALLE WHERE id_venta = p_id_venta AND id_atencion IS NOT NULL);
        UPDATE CLIENTE_PAQUETE SET estado = 'ANULADO'
         WHERE id_detalle IN (SELECT id_detalle FROM VENTA_DETALLE WHERE id_venta = p_id_venta);
        g_anulando := TRUE;   -- [v5] los dos abonos siguientes solo los puede hacer este procedimiento
        IF p_saldo_favor > 0 THEN
            INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_cita, id_venta, tipo, monto, fecha, motivo)
            VALUES (p_id_cliente, NULL, p_id_venta, 'ABONO', p_saldo_favor, SYSTIMESTAMP,
                    'Devolución del saldo usado en la venta anulada N° ' || p_id_venta);
        END IF;
        -- [v4] RN44: si la clienta eligió saldo a favor, lo que pagó vuelve como saldo (sin devolución por la pasarela)
        SELECT MAX(nc.forma_devolucion) INTO v_forma
          FROM NOTA_CREDITO nc
          JOIN COMPROBANTE c ON c.id_comprobante = nc.id_nota_credito
         WHERE c.id_venta = p_id_venta;
        IF v_forma = 'SALDO_FAVOR' AND p_id_cliente IS NOT NULL AND NVL(p_total, 0) > 0 THEN
            INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_cita, id_venta, tipo, monto, fecha, motivo)
            VALUES (p_id_cliente, NULL, p_id_venta, 'ABONO', p_total, SYSTIMESTAMP,
                    'Pago de la venta anulada N° ' || p_id_venta || ' devuelto como saldo a favor (RN44)');
        END IF;
        g_anulando := FALSE;
    EXCEPTION
        WHEN OTHERS THEN
            g_anulando := FALSE;
            RAISE;
    END al_anular;

    -- -----------------------------------------------------------------
    -- [v4] Facturación electrónica
    -- -----------------------------------------------------------------
    PROCEDURE asignar_numero(p_id_sede IN NUMBER, p_tipo IN VARCHAR2, o_serie OUT VARCHAR2, o_numero OUT VARCHAR2) IS
        v_n NUMBER;
    BEGIN
        g_numerando := TRUE;
        UPDATE SERIE_COMPROBANTE
           SET ultimo_numero = ultimo_numero + 1
         WHERE id_sede = p_id_sede AND tipo = p_tipo AND estado = 'ACTIVA'
        RETURNING serie, ultimo_numero INTO o_serie, v_n;
        g_numerando := FALSE;
        IF o_serie IS NULL THEN
            RAISE_APPLICATION_ERROR(-20078, 'La sede no tiene una serie ACTIVA para ' || p_tipo
                || '. La Administradora debe configurarla antes de emitir (RF107).');
        END IF;
        o_numero := TO_CHAR(v_n);
    EXCEPTION
        WHEN OTHERS THEN
            g_numerando := FALSE;
            RAISE;
    END asignar_numero;

    PROCEDURE liberar_numero(p_serie IN VARCHAR2, p_numero IN VARCHAR2) IS
    BEGIN
        g_numerando := TRUE;
        UPDATE SERIE_COMPROBANTE
           SET ultimo_numero = ultimo_numero - 1
         WHERE serie = p_serie AND ultimo_numero = TO_NUMBER(p_numero);
        g_numerando := FALSE;
        IF SQL%ROWCOUNT = 0 THEN
            RAISE_APPLICATION_ERROR(-20073, 'El comprobante ' || p_serie || '-' || p_numero
                || ' no es el último de su serie: no se elimina (dejaría un hueco en la numeración). Anule la venta con nota de crédito.');
        END IF;
    EXCEPTION
        WHEN OTHERS THEN
            g_numerando := FALSE;
            RAISE;
    END liberar_numero;

    PROCEDURE validar_estado_sunat(p_anterior IN VARCHAR2, p_nuevo IN VARCHAR2,
                                   p_intentos_ant IN NUMBER, p_intentos_nue IN NUMBER, p_documento IN VARCHAR2) IS
    BEGIN
        IF p_intentos_nue < p_intentos_ant THEN
            RAISE_APPLICATION_ERROR(-20075, 'Los intentos de envío de ' || p_documento || ' no pueden disminuir (RF101).');
        END IF;
        IF PKG_SESION.distinto(p_anterior, p_nuevo)
           AND NOT ((p_anterior = 'PENDIENTE' AND p_nuevo IN ('ENVIADO', 'ACEPTADO', 'OBSERVADO', 'RECHAZADO'))
                 OR (p_anterior = 'ENVIADO'   AND p_nuevo IN ('PENDIENTE', 'ACEPTADO', 'OBSERVADO', 'RECHAZADO'))) THEN
            RAISE_APPLICATION_ERROR(-20075, 'Cambio de estado SUNAT no permitido en ' || p_documento || ': '
                || p_anterior || ' -> ' || p_nuevo || '. ACEPTADO, OBSERVADO y RECHAZADO son finales (RF100).');
        END IF;
    END validar_estado_sunat;

END PKG_VENTA;
/
