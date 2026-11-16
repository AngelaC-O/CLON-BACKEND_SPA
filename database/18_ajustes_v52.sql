-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle · VERSIÓN 5.2
-- Script 18 · Migración v5.1 -> v5.2 (Iteración 2.1, ajustes, 15/11/2026)
-- Se ejecuta como: dueño del esquema (modo mantenimiento), después de 17_datos_iteracion2.sql.
-- Orden completo (ver README.md): 00 -> 14 -> 13 -> 15 -> 17 -> 18 -> 16 (y 19 solo en la base local y en la e2e).
-- Los scripts 01 a 17 no se editan: describen el modelo v5 y su migración a v5.1.
--
-- Se puede repetir sin error: cada paso revisa user_tab_columns, user_constraints, user_indexes o PARAMETRO
-- antes de cambiar, y si ya está hecho no hace nada. Los paquetes, triggers y vistas van con CREATE OR REPLACE.
-- Ante cualquier error se detiene.
--
-- Definición exacta de cada objeto: docs/Cautiva_Spa_Diccionario_de_Datos_v5_2.xlsx
-- (hojas Diccionario, Restricciones y Cambios v5.2, cambios 1 a 22). El código de cada paquete, trigger
-- o vista parte de su versión vigente (08, 09, 10, 11 o 17) y cambia solo en lo que dice la hoja ([v5.2]).
--
--   a) TRANSACCION_PASARELA.id_venta: columna, llenado de los cobros ya registrados, FK_TRANS_PAS_VENTA,
--      IX_TRANS_PAS_VENTA y CK_TRANS_PAS_VENTA (cambio 4, D-22).
--   b) PKG_AGENDA: fin_atencion reemplaza a fin_con_limpieza; validar_cruce sin limpieza (cambio 8, D-14).
--   c) TRG_CITA_REGLAS: fecha_hora_fin = inicio + duración (cambio 9).
--   d) Vistas: V_AGENDA_BLOQUES (fin), V_OCUPACION_RECURSO, V_VENTA_LINEA (id_producto, id_variante, id_paquete)
--      y V_KARDEX (comprobante, documento_compra) (cambios 10, 11, 19 y 20; D-37).
--   e) Triggers de auditoría regenerados con el molde de 10: TRG_AUD_RECURSO, TRG_AUD_CAJA (D-15) y
--      TRG_AUD_TRANSACCION_PASARELA (cambios 12, 13 y 17).
--   f) Triggers de reglas: TRG_CAJA_REGLAS (D-16), TRG_NOTA_CREDITO_REGLAS (D-21), TRG_TRANS_PAS_REGLAS y
--      TRG_VENTA_PAGO_REGLAS (D-22) (cambios 14 a 16 y 18).
--   g) CITA: recalcula fecha_hora_fin sin la limpieza (cambio 3).
--   h) PARAMETRO: borra LIMPIEZA_MAX_MIN (cambio 7).
--   i) RECURSO: retira CK_RECURSO_MINUTOS_LIMPIEZA y minutos_limpieza; comentarios de RECURSO, PARAMETRO y CITA
--      (cambios 1, 2, 5 y 22).
--   j) Recompila lo inválido.
--   k) Resumen: 57 tablas, 132 FK, 129 triggers, 27 vistas, 5 paquetes y 0 objetos inválidos.
--
-- Código de error nuevo (el mayor usado en 17 era -20106):
--   -20107  Un pago con una transacción cobrada para otra venta (TRG_VENTA_PAGO_REGLAS).
-- Se reutilizan: -20060 (solo se reabre la caja del día) y -20072 (cobro de una venta CERRADA o de otra sede,
-- o cambiar TRANSACCION_PASARELA.id_venta).
-- =============================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- =============================================================================
-- a) TRANSACCION_PASARELA.id_venta (cambio 4, D-22)
-- =============================================================================
PROMPT a) TRANSACCION_PASARELA.id_venta...
DECLARE
    v_n NUMBER;

    PROCEDURE ejecutar(p_sql IN VARCHAR2, p_aviso IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
        DBMS_OUTPUT.PUT_LINE('   + ' || p_aviso);
    END ejecutar;

    FUNCTION hay_columna(p_tabla IN VARCHAR2, p_columna IN VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v FROM user_tab_columns WHERE table_name = p_tabla AND column_name = p_columna;
        RETURN v > 0;
    END hay_columna;

    FUNCTION hay_restriccion(p_nombre IN VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v FROM user_constraints WHERE constraint_name = p_nombre;
        RETURN v > 0;
    END hay_restriccion;

    FUNCTION hay_indice(p_nombre IN VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v FROM user_indexes WHERE index_name = p_nombre;
        RETURN v > 0;
    END hay_indice;
BEGIN
    IF NOT hay_columna('TRANSACCION_PASARELA', 'ID_VENTA') THEN
        ejecutar('ALTER TABLE TRANSACCION_PASARELA ADD (id_venta NUMBER)', 'TRANSACCION_PASARELA.id_venta');
    END IF;
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN TRANSACCION_PASARELA.id_venta IS 'Venta cuyo pago se cobra por la pasarela. Nulo en adelantos de cita (id_cita) y en devoluciones. La guarda PagosApi.cobrar (CUS13, CUS18, CUS26) y permite listar los cobros PENDIENTES de una venta abierta. (RN52)']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN TRANSACCION_PASARELA.id_cita IS 'Cita cuyo adelanto se cobra. Nulo en cobros de ventas (esas llevan id_venta). (RN04, RF104)']';

    -- Llenado ANTES de reemplazar TRG_TRANS_PAS_REGLAS (desde f, id_venta ya no cambia): la venta del VENTA_PAGO
    -- que tiene esa transacción, solo en cobros sin cita y sin venta. Dinámico: la columna puede ser de este mismo paso.
    EXECUTE IMMEDIATE q'[
        UPDATE TRANSACCION_PASARELA t
           SET t.id_venta = (SELECT p.id_venta FROM VENTA_PAGO p WHERE p.id_transaccion = t.id_transaccion)
         WHERE t.tipo_operacion = 'COBRO'
           AND t.id_cita IS NULL
           AND t.id_venta IS NULL
           AND EXISTS (SELECT 1 FROM VENTA_PAGO p WHERE p.id_transaccion = t.id_transaccion)]';
    DBMS_OUTPUT.PUT_LINE('   a) ' || SQL%ROWCOUNT || ' cobro(s) quedaron con la venta de su VENTA_PAGO (id_venta).');
    COMMIT;
    EXECUTE IMMEDIATE q'[
        SELECT COUNT(*) FROM TRANSACCION_PASARELA
         WHERE tipo_operacion = 'COBRO' AND estado = 'PENDIENTE' AND id_cita IS NULL AND id_venta IS NULL]' INTO v_n;
    DBMS_OUTPUT.PUT_LINE('   a) ' || v_n || ' cobro(s) PENDIENTE(S) sin cita quedaron sin venta (no tienen VENTA_PAGO).');

    IF NOT hay_restriccion('FK_TRANS_PAS_VENTA') THEN
        ejecutar('ALTER TABLE TRANSACCION_PASARELA ADD CONSTRAINT FK_TRANS_PAS_VENTA FOREIGN KEY (id_venta) REFERENCES VENTA (id_venta)',
                 'FK_TRANS_PAS_VENTA');
    END IF;
    IF NOT hay_indice('IX_TRANS_PAS_VENTA') THEN
        ejecutar('CREATE INDEX IX_TRANS_PAS_VENTA ON TRANSACCION_PASARELA (id_venta)', 'IX_TRANS_PAS_VENTA');
    END IF;
    IF NOT hay_restriccion('CK_TRANS_PAS_VENTA') THEN
        ejecutar(q'[ALTER TABLE TRANSACCION_PASARELA ADD CONSTRAINT CK_TRANS_PAS_VENTA CHECK (id_venta IS NULL OR (tipo_operacion = 'COBRO' AND id_cita IS NULL))]',
                 'CK_TRANS_PAS_VENTA');
    END IF;
END;
/

-- =============================================================================
-- b) PKG_AGENDA (08) · [v5.2] cambio 8: sin tiempo de limpieza (RN03, D-14)
-- =============================================================================
PROMPT b) PKG_AGENDA...
CREATE OR REPLACE PACKAGE PKG_AGENDA AS
    -- [v5.2] Fin del bloque ocupado = inicio + minutos: la especialista y el recurso quedan libres al terminar (RN03)
    FUNCTION  fin_atencion(p_inicio IN TIMESTAMP, p_minutos IN NUMBER) RETURN TIMESTAMP;
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

    -- [v5.2] Sin tiempo de limpieza (RN03): el bloque termina con el servicio
    FUNCTION fin_atencion(p_inicio IN TIMESTAMP, p_minutos IN NUMBER) RETURN TIMESTAMP IS
    BEGIN
        RETURN p_inicio + NUMTODSINTERVAL(p_minutos, 'MINUTE');
    END fin_atencion;

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
                || ' que empieza a las ' || TO_CHAR(v_ini, 'HH24:MI') || '.');   -- [v5.2] sin limpieza (RN03)
        EXCEPTION
            WHEN NO_DATA_FOUND THEN NULL;
        END;

        BEGIN
            SELECT id_atencion, hora_inicio, quien
              INTO v_id, v_ini, v_que
              FROM (SELECT a.id_atencion, a.hora_inicio,
                           CASE WHEN a.id_personal = p_id_personal THEN 'la especialista' ELSE 'el recurso' END AS quien
                      FROM ATENCION a
                     WHERE NVL(a.id_cita, -1) <> NVL(p_excluir_cita, -2)   -- la atención de la propia cita no choca con ella
                       AND (a.id_personal = p_id_personal OR a.id_recurso = p_id_recurso)
                       AND a.id_atencion <> NVL(p_excluir_atencion, -1)
                       AND a.hora_inicio < p_fin
                       -- [v5.2] la especialista y el recurso, con el mismo fin (sin limpieza, RN03)
                       AND NVL(a.hora_fin, a.hora_inicio + NUMTODSINTERVAL(a.tiempo_declarado_min, 'MINUTE')) > p_inicio
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
        -- tiempo que ella declara ([v5.2] sin limpieza). Si ese tiempo choca con otra cita, la BD lo impide:
        -- primero se reprograma la cita siguiente y luego se declara el tiempo. La propia cita no cuenta como cruce.
        IF r.estado = 'EN_CURSO' THEN
            v_fin := fin_atencion(r.hora_inicio, r.tiempo_declarado_min);   -- [v5.2]
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

-- =============================================================================
-- c) TRG_CITA_REGLAS (09, sección 6) · [v5.2] cambio 9: fecha_hora_fin = inicio + duración. El resto, igual que en 09.
-- =============================================================================
PROMPT c) TRG_CITA_REGLAS...
CREATE OR REPLACE TRIGGER TRG_CITA_REGLAS
BEFORE INSERT OR UPDATE ON CITA
FOR EACH ROW
DECLARE
    v_rec          RECURSO%ROWTYPE;
    v_per_estado   PERSONAL.estado%TYPE;
    v_per_cargo    PERSONAL.cargo%TYPE;
    v_cli_estado   CLIENTE.estado%TYPE;
    v_apertura     SEDE.horario_apertura%TYPE;
    v_cierre       SEDE.horario_cierre%TYPE;
    v_caja_estado  CAJA.estado%TYPE;
    v_caja_sede    CAJA.id_sede%TYPE;
    v_fin_servicio TIMESTAMP;
    v_cambia       BOOLEAN;
BEGIN
    IF INSERTING THEN
        IF :NEW.estado <> 'PENDIENTE' THEN
            RAISE_APPLICATION_ERROR(-20013, 'Una cita nueva nace PENDIENTE.');
        END IF;
        :NEW.fecha_registro := SYSTIMESTAMP;
        -- [v5] toda cita la registra recepción (la reserva en línea quedó fuera de alcance: RF94)
        :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
        SELECT estado INTO v_cli_estado FROM CLIENTE WHERE id_cliente = :NEW.id_cliente;
        IF v_cli_estado <> 'ACTIVO' THEN
            RAISE_APPLICATION_ERROR(-20016, 'La clienta está INACTIVA (RN01).');
        END IF;
    ELSE
        -- Una cita cerrada no se mueve
        IF :OLD.estado IN ('FINALIZADA', 'CANCELADA', 'REPROGRAMADA')
           AND (PKG_SESION.distinto(:OLD.estado, :NEW.estado)
                OR PKG_SESION.distinto(:OLD.fecha_hora_inicio, :NEW.fecha_hora_inicio)
                OR PKG_SESION.distinto(:OLD.duracion_min, :NEW.duracion_min)
                OR PKG_SESION.distinto(:OLD.id_personal, :NEW.id_personal)
                OR PKG_SESION.distinto(:OLD.id_recurso, :NEW.id_recurso)
                OR PKG_SESION.distinto(:OLD.id_cliente, :NEW.id_cliente)
                OR PKG_SESION.distinto(:OLD.adelanto_monto, :NEW.adelanto_monto)) THEN
            RAISE_APPLICATION_ERROR(-20013, 'La cita N° ' || :OLD.id_cita || ' ya está ' || :OLD.estado
                || ' y no se modifica. Para moverla registre una nueva cita con id_cita_origen.');
        END IF;
        -- Transiciones válidas de estado
        IF PKG_SESION.distinto(:OLD.estado, :NEW.estado)
           AND NOT ((:OLD.estado = 'PENDIENTE'   AND :NEW.estado IN ('EN_ATENCION', 'CANCELADA', 'REPROGRAMADA'))
                 OR (:OLD.estado = 'EN_ATENCION' AND :NEW.estado = 'FINALIZADA')) THEN
            RAISE_APPLICATION_ERROR(-20013, 'Cambio de estado no permitido: ' || :OLD.estado || ' -> ' || :NEW.estado || '.');
        END IF;
        IF PKG_SESION.distinto(:OLD.id_cliente, :NEW.id_cliente) THEN
            RAISE_APPLICATION_ERROR(-20013, 'Una cita no cambia de clienta.');
        END IF;
    END IF;

    v_cambia := INSERTING
             OR PKG_SESION.distinto(:OLD.fecha_hora_inicio, :NEW.fecha_hora_inicio)
             OR PKG_SESION.distinto(:OLD.duracion_min, :NEW.duracion_min)
             OR PKG_SESION.distinto(:OLD.id_personal, :NEW.id_personal)
             OR PKG_SESION.distinto(:OLD.id_recurso, :NEW.id_recurso)
             OR PKG_SESION.distinto(:OLD.id_variante, :NEW.id_variante)
             OR PKG_SESION.distinto(:OLD.id_sede, :NEW.id_sede);

    IF v_cambia THEN
        SELECT * INTO v_rec FROM RECURSO WHERE id_recurso = :NEW.id_recurso;
        IF v_rec.id_sede <> :NEW.id_sede THEN
            RAISE_APPLICATION_ERROR(-20017, 'El recurso "' || v_rec.nombre || '" no pertenece a la sede de la cita (RN26).');
        END IF;
        IF v_rec.estado <> 'DISPONIBLE' THEN
            RAISE_APPLICATION_ERROR(-20017, 'El recurso "' || v_rec.nombre || '" está ' || v_rec.estado || ' y no se puede agendar.');
        END IF;
        SELECT estado, cargo INTO v_per_estado, v_per_cargo FROM PERSONAL WHERE id_personal = :NEW.id_personal;
        IF v_per_estado <> 'ACTIVO' OR v_per_cargo NOT IN ('ESPECIALISTA_TECNICA', 'ADMINISTRADORA') THEN
            RAISE_APPLICATION_ERROR(-20018, 'Solo una especialista técnica (o la Administradora) activa puede atender una cita.');
        END IF;
        PKG_AGENDA.validar_habilitacion(:NEW.id_personal, :NEW.id_variante);

        -- Dentro del horario de la sede y en el mismo día
        SELECT horario_apertura, horario_cierre INTO v_apertura, v_cierre FROM SEDE WHERE id_sede = :NEW.id_sede;
        v_fin_servicio := :NEW.fecha_hora_inicio + NUMTODSINTERVAL(:NEW.duracion_min, 'MINUTE');
        IF TO_CHAR(:NEW.fecha_hora_inicio, 'HH24:MI') < v_apertura
           OR TO_CHAR(v_fin_servicio, 'HH24:MI') > v_cierre
           OR TRUNC(v_fin_servicio) <> TRUNC(:NEW.fecha_hora_inicio) THEN
            RAISE_APPLICATION_ERROR(-20014, 'La cita debe estar dentro del horario de la sede (' || v_apertura || ' a ' || v_cierre || ').');
        END IF;

        -- [v5.2] RN03: el bloque termina con el servicio (sin tiempo de limpieza)
        :NEW.fecha_hora_fin := :NEW.fecha_hora_inicio + NUMTODSINTERVAL(:NEW.duracion_min, 'MINUTE');
    END IF;

    -- RN04, RF10: el adelanto entra a una caja abierta de la misma sede
    -- (una cita reprogramada hereda el adelanto y la caja original: no se vuelve a cobrar)
    IF :NEW.id_caja IS NOT NULL AND :NEW.id_cita_origen IS NULL
       AND (INSERTING OR PKG_SESION.distinto(:OLD.id_caja, :NEW.id_caja)
                      OR PKG_SESION.distinto(:OLD.adelanto_monto, :NEW.adelanto_monto)) THEN
        SELECT estado, id_sede INTO v_caja_estado, v_caja_sede FROM CAJA WHERE id_caja = :NEW.id_caja;
        IF v_caja_estado <> 'ABIERTA' OR v_caja_sede <> :NEW.id_sede THEN
            RAISE_APPLICATION_ERROR(-20019, 'El adelanto se registra en la caja ABIERTA de la sede de la cita (RN30).');
        END IF;
    END IF;
END;
/

-- =============================================================================
-- d) Vistas (11) · [v5.2] cambios 10, 11, 19 y 20
-- =============================================================================
PROMPT d) Vistas V_AGENDA_BLOQUES, V_OCUPACION_RECURSO, V_VENTA_LINEA y V_KARDEX...

-- [v5.2] cambio 10: la columna fin_con_limpieza pasa a llamarse fin y es el fin del servicio (sin limpieza del recurso)
CREATE OR REPLACE VIEW V_AGENDA_BLOQUES AS
SELECT 'CITA'                 AS origen,
       c.id_cita              AS id_origen,
       c.id_sede,
       c.id_personal,
       c.id_recurso,
       c.id_variante,
       c.fecha_hora_inicio    AS inicio,
       c.fecha_hora_fin       AS fin,
       c.estado
  FROM CITA c
 WHERE c.estado IN ('PENDIENTE', 'EN_ATENCION', 'FINALIZADA')
UNION ALL
SELECT 'ATENCION_SIN_CITA',
       a.id_atencion,
       r.id_sede,
       a.id_personal,
       a.id_recurso,
       a.id_variante,
       a.hora_inicio,
       NVL(a.hora_fin, a.hora_inicio + NUMTODSINTERVAL(a.tiempo_declarado_min, 'MINUTE')),
       a.estado
  FROM ATENCION a
  JOIN RECURSO r ON r.id_recurso = a.id_recurso
 WHERE a.id_cita IS NULL;

COMMENT ON TABLE V_AGENDA_BLOQUES IS 'Bloques ocupados de especialistas y recursos (citas y atenciones sin cita). Cada bloque termina con el servicio: sin tiempo de limpieza (RN03). Base de la agenda y de las horas libres (RF06, RF11).';

-- [v5.2] cambio 11: minutos_ocupados con b.fin
CREATE OR REPLACE VIEW V_OCUPACION_RECURSO AS
SELECT b.id_sede,
       b.id_recurso,
       r.nombre                                           AS recurso,
       r.tipo,
       TRUNC(b.inicio)                                    AS fecha,
       TO_NUMBER(TO_CHAR(b.inicio, 'HH24'))               AS hora,
       COUNT(*)                                           AS bloques,
       SUM(ROUND((CAST(b.fin AS DATE) - CAST(b.inicio AS DATE)) * 1440)) AS minutos_ocupados
  FROM V_AGENDA_BLOQUES b
  JOIN RECURSO r ON r.id_recurso = b.id_recurso
 GROUP BY b.id_sede, b.id_recurso, r.nombre, r.tipo, TRUNC(b.inicio), TO_NUMBER(TO_CHAR(b.inicio, 'HH24'));

COMMENT ON TABLE V_OCUPACION_RECURSO IS 'Ocupación de cada recurso por día y por hora de inicio, con el fin del servicio (RF72, RN03).';

-- [v5.2] cambio 19 (D-37): suma al final id_producto, id_variante (de la atención) e id_paquete
CREATE OR REPLACE VIEW V_VENTA_LINEA AS
SELECT d.id_detalle,
       v.id_venta,
       v.id_sede,
       TRUNC(v.fecha_hora)                         AS fecha,
       EXTRACT(YEAR FROM v.fecha_hora)             AS anio,
       EXTRACT(MONTH FROM v.fecha_hora)            AS mes,
       d.tipo_item,
       CASE d.tipo_item WHEN 'SERVICIO' THEN ar.linea
                        WHEN 'PRODUCTO' THEN 'Productos'
                        ELSE 'Paquetes' END        AS linea,
       ar.nombre                                   AS area,
       se.nombre                                   AS servicio,
       vs.nombre                                   AS variante,
       pr.nombre                                   AS producto,
       cat.nombre                                  AS categoria,
       pq.nombre                                   AS paquete,
       d.id_personal,
       TRIM(pe.nombres || ' ' || pe.apellidos)     AS especialista,
       CASE WHEN at.id_atencion IS NULL THEN NULL
            WHEN at.id_cita IS NULL THEN 'SIN_CITA'
            ELSE 'CON_CITA' END                    AS tipo_atencion,
       CASE WHEN at.id_cliente_paquete IS NULL THEN 'N' ELSE 'S' END AS cubierto_por_paquete,
       d.id_promocion,
       d.cantidad,
       d.precio_unitario,
       d.descuento,
       d.subtotal,
       d.id_producto,
       at.id_variante,
       d.id_paquete
  FROM VENTA_DETALLE d
  JOIN VENTA v                     ON v.id_venta = d.id_venta AND v.estado = 'CERRADA'
  LEFT JOIN ATENCION at            ON at.id_atencion = d.id_atencion
  LEFT JOIN VARIANTE_SERVICIO vs   ON vs.id_variante = at.id_variante
  LEFT JOIN SERVICIO se            ON se.id_servicio = vs.id_servicio
  LEFT JOIN AREA ar                ON ar.id_area = se.id_area
  LEFT JOIN PRODUCTO pr            ON pr.id_producto = d.id_producto
  LEFT JOIN CATEGORIA_PRODUCTO cat ON cat.id_categoria = pr.id_categoria
  LEFT JOIN PAQUETE pq             ON pq.id_paquete = d.id_paquete
  LEFT JOIN PERSONA pe             ON pe.id_persona = d.id_personal;

COMMENT ON TABLE V_VENTA_LINEA IS 'Líneas de ventas cerradas con línea (SALON/SPA), área, servicio, producto, especialista y tipo de atención, y los id del producto, la variante y el paquete (RF68, RF73).';

-- [v5.2] cambio 20: suma al final el comprobante de la venta (serie-número de la BOLETA o FACTURA) y el documento
-- de la compra; nulos si el movimiento no viene de una venta o de una compra
CREATE OR REPLACE VIEW V_KARDEX AS
SELECT k.id_movimiento,
       k.id_sede,
       k.id_producto,
       p.codigo,
       p.nombre                         AS producto,
       CAST(k.fecha_hora AS DATE)       AS fecha_hora,
       k.tipo,
       k.motivo,
       CASE k.tipo WHEN 'SALIDA' THEN -k.cantidad ELSE k.cantidad END AS cantidad_con_signo,
       k.saldo,
       u.nombre_usuario                 AS usuario,
       k.id_detalle_venta,
       k.id_detalle_compra,
       k.id_atencion,
       CASE WHEN cp.id_comprobante IS NOT NULL THEN cp.serie || '-' || cp.numero END AS comprobante,
       co.documento                     AS documento_compra
  FROM MOVIMIENTO_KARDEX k
  JOIN PRODUCTO p ON p.id_producto = k.id_producto
  JOIN USUARIO u  ON u.id_usuario = k.id_usuario
  LEFT JOIN VENTA_DETALLE vd  ON vd.id_detalle = k.id_detalle_venta
  LEFT JOIN COMPROBANTE cp    ON cp.id_venta = vd.id_venta AND cp.tipo IN ('BOLETA', 'FACTURA')
  LEFT JOIN COMPRA_DETALLE cd ON cd.id_detalle_compra = k.id_detalle_compra
  LEFT JOIN COMPRA co         ON co.id_compra = cd.id_compra;

COMMENT ON TABLE V_KARDEX IS 'Kardex por producto y sede con entradas, salidas, ajustes y saldo, y el comprobante de la venta o el documento de la compra que lo originó (RF50, RN18).';

-- =============================================================================
-- e) Triggers de auditoría (10) · [v5.2] cambios 12, 13 y 17, con el molde de 10_triggers_auditoria.sql
-- =============================================================================
PROMPT e) Triggers de auditoría de RECURSO, CAJA y TRANSACCION_PASARELA...
PROMPT Trigger TRG_AUD_RECURSO...
CREATE OR REPLACE TRIGGER TRG_AUD_RECURSO
AFTER INSERT OR UPDATE OR DELETE ON RECURSO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_recurso', TO_CHAR(:NEW.id_recurso)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('tipo', :NEW.tipo) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Sedes y recursos', 'CREAR', 'RECURSO', :NEW.id_recurso, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('tipo', :OLD.tipo, :NEW.tipo);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Sedes y recursos', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'RECURSO', :NEW.id_recurso, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_recurso', TO_CHAR(:OLD.id_recurso)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('tipo', :OLD.tipo) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Sedes y recursos', 'ELIMINAR', 'RECURSO', :OLD.id_recurso, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CAJA...
CREATE OR REPLACE TRIGGER TRG_AUD_CAJA
AFTER INSERT OR UPDATE OR DELETE ON CAJA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_caja', TO_CHAR(:NEW.id_caja)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('id_usuario_apertura', TO_CHAR(:NEW.id_usuario_apertura)) || PKG_SESION.par('fecha_apertura', TO_CHAR(:NEW.fecha_apertura, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('monto_inicial', TO_CHAR(:NEW.monto_inicial)) || PKG_SESION.par('id_usuario_cierre', TO_CHAR(:NEW.id_usuario_cierre)) || PKG_SESION.par('fecha_cierre', TO_CHAR(:NEW.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario_revisa', TO_CHAR(:NEW.id_usuario_revisa)) || PKG_SESION.par('fecha_revision', TO_CHAR(:NEW.fecha_revision, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('id_usuario_reapertura', TO_CHAR(:NEW.id_usuario_reapertura)) || PKG_SESION.par('fecha_reapertura', TO_CHAR(:NEW.fecha_reapertura, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('motivo_reapertura', :NEW.motivo_reapertura);
        PKG_SESION.auditar('Caja y conciliación', 'CREAR', 'CAJA', :NEW.id_caja, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('id_usuario_apertura', TO_CHAR(:OLD.id_usuario_apertura), TO_CHAR(:NEW.id_usuario_apertura));
        v_det := v_det || PKG_SESION.diff('fecha_apertura', TO_CHAR(:OLD.fecha_apertura, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_apertura, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('monto_inicial', TO_CHAR(:OLD.monto_inicial), TO_CHAR(:NEW.monto_inicial));
        v_det := v_det || PKG_SESION.diff('id_usuario_cierre', TO_CHAR(:OLD.id_usuario_cierre), TO_CHAR(:NEW.id_usuario_cierre));
        v_det := v_det || PKG_SESION.diff('fecha_cierre', TO_CHAR(:OLD.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('id_usuario_revisa', TO_CHAR(:OLD.id_usuario_revisa), TO_CHAR(:NEW.id_usuario_revisa));
        v_det := v_det || PKG_SESION.diff('fecha_revision', TO_CHAR(:OLD.fecha_revision, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_revision, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('id_usuario_reapertura', TO_CHAR(:OLD.id_usuario_reapertura), TO_CHAR(:NEW.id_usuario_reapertura));
        v_det := v_det || PKG_SESION.diff('fecha_reapertura', TO_CHAR(:OLD.fecha_reapertura, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_reapertura, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('motivo_reapertura', :OLD.motivo_reapertura, :NEW.motivo_reapertura);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Caja y conciliación', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'CAJA', :NEW.id_caja, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_caja', TO_CHAR(:OLD.id_caja)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('id_usuario_apertura', TO_CHAR(:OLD.id_usuario_apertura)) || PKG_SESION.par('fecha_apertura', TO_CHAR(:OLD.fecha_apertura, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('monto_inicial', TO_CHAR(:OLD.monto_inicial)) || PKG_SESION.par('id_usuario_cierre', TO_CHAR(:OLD.id_usuario_cierre)) || PKG_SESION.par('fecha_cierre', TO_CHAR(:OLD.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario_revisa', TO_CHAR(:OLD.id_usuario_revisa)) || PKG_SESION.par('fecha_revision', TO_CHAR(:OLD.fecha_revision, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('id_usuario_reapertura', TO_CHAR(:OLD.id_usuario_reapertura)) || PKG_SESION.par('fecha_reapertura', TO_CHAR(:OLD.fecha_reapertura, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('motivo_reapertura', :OLD.motivo_reapertura);
        PKG_SESION.auditar('Caja y conciliación', 'ELIMINAR', 'CAJA', :OLD.id_caja, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_TRANSACCION_PASARELA...
CREATE OR REPLACE TRIGGER TRG_AUD_TRANSACCION_PASARELA
AFTER INSERT OR UPDATE OR DELETE ON TRANSACCION_PASARELA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_transaccion', TO_CHAR(:NEW.id_transaccion)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:NEW.id_medio_pago)) || PKG_SESION.par('tipo_operacion', :NEW.tipo_operacion) || PKG_SESION.par('canal', :NEW.canal) || PKG_SESION.par('id_cita', TO_CHAR(:NEW.id_cita)) || PKG_SESION.par('id_venta', TO_CHAR(:NEW.id_venta)) || PKG_SESION.par('id_transaccion_origen', TO_CHAR(:NEW.id_transaccion_origen)) || PKG_SESION.par('id_nota_credito', TO_CHAR(:NEW.id_nota_credito)) || PKG_SESION.par('codigo_operacion', :NEW.codigo_operacion) || PKG_SESION.par('enlace_pago', :NEW.enlace_pago) || PKG_SESION.par('fecha_expiracion', TO_CHAR(:NEW.fecha_expiracion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('monto', TO_CHAR(:NEW.monto)) || PKG_SESION.par('comision', TO_CHAR(:NEW.comision)) || PKG_SESION.par('fecha_abono', TO_CHAR(:NEW.fecha_abono, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('mensaje_respuesta', :NEW.mensaje_respuesta) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('fecha_hora', TO_CHAR(:NEW.fecha_hora, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_respuesta', TO_CHAR(:NEW.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'TRANSACCION_PASARELA', :NEW.id_transaccion, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('id_medio_pago', TO_CHAR(:OLD.id_medio_pago), TO_CHAR(:NEW.id_medio_pago));
        v_det := v_det || PKG_SESION.diff('tipo_operacion', :OLD.tipo_operacion, :NEW.tipo_operacion);
        v_det := v_det || PKG_SESION.diff('canal', :OLD.canal, :NEW.canal);
        v_det := v_det || PKG_SESION.diff('id_cita', TO_CHAR(:OLD.id_cita), TO_CHAR(:NEW.id_cita));
        v_det := v_det || PKG_SESION.diff('id_venta', TO_CHAR(:OLD.id_venta), TO_CHAR(:NEW.id_venta));
        v_det := v_det || PKG_SESION.diff('id_transaccion_origen', TO_CHAR(:OLD.id_transaccion_origen), TO_CHAR(:NEW.id_transaccion_origen));
        v_det := v_det || PKG_SESION.diff('id_nota_credito', TO_CHAR(:OLD.id_nota_credito), TO_CHAR(:NEW.id_nota_credito));
        v_det := v_det || PKG_SESION.diff('codigo_operacion', :OLD.codigo_operacion, :NEW.codigo_operacion);
        v_det := v_det || PKG_SESION.diff('enlace_pago', :OLD.enlace_pago, :NEW.enlace_pago);
        v_det := v_det || PKG_SESION.diff('fecha_expiracion', TO_CHAR(:OLD.fecha_expiracion, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_expiracion, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('monto', TO_CHAR(:OLD.monto), TO_CHAR(:NEW.monto));
        v_det := v_det || PKG_SESION.diff('comision', TO_CHAR(:OLD.comision), TO_CHAR(:NEW.comision));
        v_det := v_det || PKG_SESION.diff('fecha_abono', TO_CHAR(:OLD.fecha_abono, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_abono, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('mensaje_respuesta', :OLD.mensaje_respuesta, :NEW.mensaje_respuesta);
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('fecha_hora', TO_CHAR(:OLD.fecha_hora, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_hora, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('fecha_respuesta', TO_CHAR(:OLD.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS'));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'TRANSACCION_PASARELA', :NEW.id_transaccion, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_transaccion', TO_CHAR(:OLD.id_transaccion)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:OLD.id_medio_pago)) || PKG_SESION.par('tipo_operacion', :OLD.tipo_operacion) || PKG_SESION.par('canal', :OLD.canal) || PKG_SESION.par('id_cita', TO_CHAR(:OLD.id_cita)) || PKG_SESION.par('id_venta', TO_CHAR(:OLD.id_venta)) || PKG_SESION.par('id_transaccion_origen', TO_CHAR(:OLD.id_transaccion_origen)) || PKG_SESION.par('id_nota_credito', TO_CHAR(:OLD.id_nota_credito)) || PKG_SESION.par('codigo_operacion', :OLD.codigo_operacion) || PKG_SESION.par('enlace_pago', :OLD.enlace_pago) || PKG_SESION.par('fecha_expiracion', TO_CHAR(:OLD.fecha_expiracion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('monto', TO_CHAR(:OLD.monto)) || PKG_SESION.par('comision', TO_CHAR(:OLD.comision)) || PKG_SESION.par('fecha_abono', TO_CHAR(:OLD.fecha_abono, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('mensaje_respuesta', :OLD.mensaje_respuesta) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('fecha_hora', TO_CHAR(:OLD.fecha_hora, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_respuesta', TO_CHAR(:OLD.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'TRANSACCION_PASARELA', :OLD.id_transaccion, v_det, NULL);
    END IF;
END;
/

-- =============================================================================
-- f) Triggers de reglas · [v5.2] cambios 14, 15, 16 y 18
-- =============================================================================
PROMPT f) Triggers de reglas v5.2...

-- TRG_CAJA_REGLAS (17) · [v5.2] cambio 14 (D-16): la reapertura solo se permite con la caja del día (RN30).
CREATE OR REPLACE TRIGGER TRG_CAJA_REGLAS
BEFORE INSERT OR UPDATE ON CAJA
FOR EACH ROW
DECLARE
    v_n NUMBER;
BEGIN
    IF INSERTING THEN
        IF :NEW.estado <> 'ABIERTA' THEN
            RAISE_APPLICATION_ERROR(-20060, 'Una caja nace ABIERTA (RF62).');
        END IF;
        :NEW.id_usuario_apertura := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_apertura);
        :NEW.fecha_apertura      := NVL(:NEW.fecha_apertura, SYSTIMESTAMP);
        :NEW.id_usuario_cierre   := NULL;
        :NEW.fecha_cierre        := NULL;
        :NEW.id_usuario_revisa   := NULL;
        :NEW.fecha_revision      := NULL;
        :NEW.id_usuario_reapertura := NULL;   -- [v5.1]
        :NEW.fecha_reapertura      := NULL;
        :NEW.motivo_reapertura     := NULL;
        RETURN;
    END IF;

    IF :OLD.estado = 'CERRADA' THEN
        IF :NEW.estado <> 'CERRADA' THEN
            -- [v5.1] Reapertura de la caja cerrada para registrar una venta (RN30, RF66, CUS46)
            IF TRIM(:NEW.motivo_reapertura) IS NULL THEN
                RAISE_APPLICATION_ERROR(-20060, 'Una caja cerrada solo se reabre con un motivo (RN30).');
            END IF;
            PKG_SESION.exigir_administradora('reabrir la caja cerrada (RN30, RF66)');
            -- [v5.2] D-16: solo la caja del día
            IF TRUNC(CAST(:OLD.fecha_apertura AS DATE)) <> TRUNC(SYSDATE) THEN
                RAISE_APPLICATION_ERROR(-20060, 'Solo se reabre la caja del día (RN30).');
            END IF;
            IF PKG_SESION.distinto(:OLD.monto_inicial, :NEW.monto_inicial) THEN
                RAISE_APPLICATION_ERROR(-20060, 'Al reabrir la caja no cambia el monto inicial.');
            END IF;
            :NEW.id_usuario_reapertura := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_reapertura);
            :NEW.fecha_reapertura      := SYSTIMESTAMP;
            :NEW.id_usuario_cierre     := NULL;
            :NEW.fecha_cierre          := NULL;
            :NEW.id_usuario_revisa     := NULL;
            :NEW.fecha_revision        := NULL;
            RETURN;
        END IF;
        IF PKG_SESION.distinto(:OLD.monto_inicial, :NEW.monto_inicial)
           OR PKG_SESION.distinto(:OLD.fecha_cierre, :NEW.fecha_cierre)
           OR PKG_SESION.distinto(:OLD.id_usuario_cierre, :NEW.id_usuario_cierre) THEN
            RAISE_APPLICATION_ERROR(-20060, 'Una caja cerrada no se modifica; solo la revisa la Administradora (RF67).');
        END IF;
        IF PKG_SESION.distinto(:OLD.id_usuario_reapertura, :NEW.id_usuario_reapertura)
           OR PKG_SESION.distinto(:OLD.fecha_reapertura, :NEW.fecha_reapertura)
           OR PKG_SESION.distinto(:OLD.motivo_reapertura, :NEW.motivo_reapertura) THEN
            RAISE_APPLICATION_ERROR(-20060, 'Los datos de la reapertura solo cambian al reabrir la caja (RN30).');
        END IF;
        IF PKG_SESION.distinto(:OLD.id_usuario_revisa, :NEW.id_usuario_revisa) THEN
            PKG_SESION.exigir_administradora('aprobar cierres de caja con diferencia (RF67)');
            :NEW.id_usuario_revisa := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_revisa);
            :NEW.fecha_revision    := SYSTIMESTAMP;
        END IF;
        RETURN;
    END IF;

    -- [v5.1] Con la caja ABIERTA, los datos de la última reapertura no cambian
    IF PKG_SESION.distinto(:OLD.id_usuario_reapertura, :NEW.id_usuario_reapertura)
       OR PKG_SESION.distinto(:OLD.fecha_reapertura, :NEW.fecha_reapertura)
       OR PKG_SESION.distinto(:OLD.motivo_reapertura, :NEW.motivo_reapertura) THEN
        RAISE_APPLICATION_ERROR(-20060, 'Los datos de la reapertura solo cambian al reabrir la caja (RN30).');
    END IF;

    IF :NEW.estado = 'CERRADA' THEN
        -- RF66: no se cierra con ventas a medio cobrar
        SELECT COUNT(*) INTO v_n FROM VENTA WHERE id_caja = :NEW.id_caja AND estado = 'ABIERTA';
        IF v_n > 0 THEN
            RAISE_APPLICATION_ERROR(-20061, 'Hay ' || v_n || ' venta(s) ABIERTA(S) en la caja: ciérrelas o descártelas antes del cierre.');
        END IF;
        -- RF64: arqueo de todos los medios de pago habilitados
        SELECT COUNT(*) INTO v_n
          FROM MEDIO_PAGO m
         WHERE m.estado = 'ACTIVO'
           AND NOT EXISTS (SELECT 1 FROM CAJA_ARQUEO a WHERE a.id_caja = :NEW.id_caja AND a.id_medio_pago = m.id_medio_pago);
        IF v_n > 0 THEN
            RAISE_APPLICATION_ERROR(-20062, 'Registre en CAJA_ARQUEO lo contado o recibido de cada medio de pago antes de cerrar (RF64).');
        END IF;
        -- El arqueo debe estar al día: nada se movió después de registrarlo (también después de una reapertura)
        FOR a IN (SELECT id_medio_pago, monto_esperado FROM CAJA_ARQUEO WHERE id_caja = :NEW.id_caja) LOOP
            IF a.monto_esperado <> PKG_CAJA.esperado(:NEW.id_caja, a.id_medio_pago, :NEW.monto_inicial) THEN
                RAISE_APPLICATION_ERROR(-20062, 'Hubo movimientos después del arqueo: vuelva a registrar el arqueo antes de cerrar.');
            END IF;
        END LOOP;
        -- RF63, RF65 [v5]: lo esperado, lo contado y la diferencia de cada medio (también el efectivo)
        -- viven solo en CAJA_ARQUEO; la caja no repite esos montos.
        :NEW.fecha_cierre      := SYSTIMESTAMP;
        :NEW.id_usuario_cierre := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_cierre);
    END IF;
END;
/

-- TRG_NOTA_CREDITO_REGLAS (09, sección 8) · [v5.2] cambio 15 (D-21): id_usuario_solicita sale de la solicitud.
CREATE OR REPLACE TRIGGER TRG_NOTA_CREDITO_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON NOTA_CREDITO
FOR EACH ROW
DECLARE
    v_estado  VENTA.estado%TYPE;
    v_cliente VENTA.id_cliente%TYPE;
    v_solicita SOLICITUD_ANULACION.id_usuario_solicita%TYPE;   -- [v5.2]
BEGIN
    IF DELETING THEN
        RAISE_APPLICATION_ERROR(-20020, 'Una nota de crédito no se modifica ni se elimina.');
    END IF;

    IF UPDATING THEN
        -- Su envío a SUNAT se sigue en COMPROBANTE; aquí nada cambia
        IF PKG_SESION.distinto(:OLD.id_nota_credito, :NEW.id_nota_credito)
           OR PKG_SESION.distinto(:OLD.motivo, :NEW.motivo)
           OR PKG_SESION.distinto(:OLD.codigo_motivo, :NEW.codigo_motivo)
           OR PKG_SESION.distinto(:OLD.forma_devolucion, :NEW.forma_devolucion)
           OR PKG_SESION.distinto(:OLD.id_usuario_solicita, :NEW.id_usuario_solicita)
           OR PKG_SESION.distinto(:OLD.id_usuario_autoriza, :NEW.id_usuario_autoriza) THEN
            RAISE_APPLICATION_ERROR(-20020, 'Una nota de crédito no se modifica: su envío a SUNAT se actualiza en COMPROBANTE.');
        END IF;
        RETURN;
    END IF;

    SELECT v.estado, v.id_cliente INTO v_estado, v_cliente
      FROM COMPROBANTE c
      JOIN VENTA v ON v.id_venta = c.id_venta
     WHERE c.id_comprobante = :NEW.id_nota_credito;
    IF v_estado <> 'CERRADA' THEN
        RAISE_APPLICATION_ERROR(-20058, 'Solo se anula una venta CERRADA (la venta está ' || v_estado || ').');
    END IF;
    IF NOT PKG_SESION.es_administradora(:NEW.id_usuario_autoriza) THEN
        RAISE_APPLICATION_ERROR(-20058, 'La anulación la autoriza la Administradora (RN32, RES16).');
    END IF;
    IF :NEW.forma_devolucion = 'SALDO_FAVOR' AND v_cliente IS NULL THEN
        RAISE_APPLICATION_ERROR(-20077, 'Un cliente de paso no tiene ficha: la devolución va por el medio original (RN44).');
    END IF;
    -- [v5.2] D-21: quien pidió la anulación es quien firmó la SOLICITUD_ANULACION PENDIENTE de la venta (RN32);
    -- sin solicitud, el usuario de la sesión, como antes.
    SELECT MAX(s.id_usuario_solicita) INTO v_solicita
      FROM COMPROBANTE c
      JOIN SOLICITUD_ANULACION s ON s.id_venta = c.id_venta
     WHERE c.id_comprobante = :NEW.id_nota_credito
       AND s.estado = 'PENDIENTE';
    IF v_solicita IS NOT NULL THEN
        :NEW.id_usuario_solicita := v_solicita;
    ELSE
        :NEW.id_usuario_solicita := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_solicita);
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20094, 'Primero registre el comprobante NC_BOLETA o NC_FACTURA (id_comprobante = id_nota_credito).');
END;
/

-- TRG_TRANS_PAS_REGLAS (09, sección 13) · [v5.2] cambio 16 (D-22): el cobro de una venta es de una venta ABIERTA
-- de la misma sede, e id_venta no cambia.
CREATE OR REPLACE TRIGGER TRG_TRANS_PAS_REGLAS
FOR INSERT OR UPDATE OR DELETE ON TRANSACCION_PASARELA
COMPOUND TRIGGER
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    g_origen t_ids;   -- cobros que reciben una devolución en esta sentencia
    g_medio  t_ids;   -- medio de cada devolución

    BEFORE EACH ROW IS
        v_medio  MEDIO_PAGO%ROWTYPE;
        v_cita   CITA%ROWTYPE;
        v_venta_sede   VENTA.id_sede%TYPE;   -- [v5.2]
        v_venta_estado VENTA.estado%TYPE;
        v_n      NUMBER;
    BEGIN
        IF DELETING THEN
            RAISE_APPLICATION_ERROR(-20021, 'Una transacción de la pasarela no se elimina (RN31).');
        END IF;

        IF INSERTING THEN
            SELECT * INTO v_medio FROM MEDIO_PAGO WHERE id_medio_pago = :NEW.id_medio_pago;
            IF v_medio.usa_pasarela <> 'S' THEN
                RAISE_APPLICATION_ERROR(-20072, 'El medio ' || v_medio.nombre
                    || ' no se cobra por la pasarela: el efectivo se cobra y se cuenta en caja (RN42).');
            END IF;
            IF :NEW.estado NOT IN ('PENDIENTE', 'APROBADA', 'RECHAZADA') THEN
                RAISE_APPLICATION_ERROR(-20072, 'Una transacción nace PENDIENTE, APROBADA o RECHAZADA.');
            END IF;
            :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
            :NEW.fecha_hora := SYSTIMESTAMP;
            IF :NEW.id_cita IS NOT NULL THEN
                SELECT * INTO v_cita FROM CITA WHERE id_cita = :NEW.id_cita;
                IF v_cita.id_sede <> :NEW.id_sede OR v_cita.estado <> 'PENDIENTE' THEN
                    RAISE_APPLICATION_ERROR(-20072, 'El adelanto se cobra a una cita PENDIENTE de la misma sede (RN04).');
                END IF;
            END IF;
            -- [v5.2] D-22: el cobro de una venta es de una venta ABIERTA de la misma sede (RN52)
            IF :NEW.id_venta IS NOT NULL THEN
                SELECT id_sede, estado INTO v_venta_sede, v_venta_estado FROM VENTA WHERE id_venta = :NEW.id_venta;
                IF v_venta_sede <> :NEW.id_sede OR v_venta_estado <> 'ABIERTA' THEN
                    RAISE_APPLICATION_ERROR(-20072, 'El cobro es de una venta ABIERTA de la misma sede: la venta N° ' || :NEW.id_venta
                        || ' está ' || v_venta_estado || CASE WHEN v_venta_sede <> :NEW.id_sede THEN ' y es de otra sede' END || ' (RN52).');
                END IF;
            END IF;
            IF :NEW.tipo_operacion = 'DEVOLUCION' THEN
                PKG_SESION.exigir_administradora('autorizar la devolución de un pago (RN44)');
                g_origen(g_origen.COUNT + 1) := :NEW.id_transaccion_origen;
                g_medio(g_medio.COUNT + 1)   := :NEW.id_medio_pago;
            END IF;
        ELSE
        -- UPDATING: los datos del cobro no cambian; sí su estado, la respuesta, la comisión y el abono
        IF PKG_SESION.distinto(:OLD.id_sede, :NEW.id_sede) OR PKG_SESION.distinto(:OLD.id_medio_pago, :NEW.id_medio_pago)
           OR PKG_SESION.distinto(:OLD.tipo_operacion, :NEW.tipo_operacion) OR PKG_SESION.distinto(:OLD.canal, :NEW.canal)
           OR PKG_SESION.distinto(:OLD.id_cita, :NEW.id_cita) OR PKG_SESION.distinto(:OLD.id_transaccion_origen, :NEW.id_transaccion_origen)
           OR PKG_SESION.distinto(:OLD.id_venta, :NEW.id_venta)   -- [v5.2] D-22
           OR PKG_SESION.distinto(:OLD.id_nota_credito, :NEW.id_nota_credito) OR PKG_SESION.distinto(:OLD.monto, :NEW.monto)
           OR PKG_SESION.distinto(:OLD.id_usuario, :NEW.id_usuario) OR PKG_SESION.distinto(:OLD.fecha_hora, :NEW.fecha_hora)
           OR (:OLD.codigo_operacion IS NOT NULL AND PKG_SESION.distinto(:OLD.codigo_operacion, :NEW.codigo_operacion)) THEN
            RAISE_APPLICATION_ERROR(-20072, 'Los datos de una transacción de la pasarela no se cambian;'
                || ' solo su estado, la respuesta, la comisión y la fecha de abono.');
        END IF;
        IF PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN
            IF NOT ((:OLD.estado = 'PENDIENTE' AND :NEW.estado IN ('APROBADA', 'RECHAZADA', 'EXPIRADA'))
                 OR (:OLD.estado = 'APROBADA'  AND :NEW.estado = 'ANULADA')) THEN
                RAISE_APPLICATION_ERROR(-20072, 'Cambio de estado no permitido en la transacción: ' || :OLD.estado || ' -> ' || :NEW.estado || '.');
            END IF;
            IF :NEW.estado = 'ANULADA' THEN
                PKG_SESION.exigir_administradora('anular una transacción de la pasarela (RN44)');
                SELECT COUNT(*) INTO v_n
                  FROM VENTA_PAGO p JOIN VENTA v ON v.id_venta = p.id_venta
                 WHERE p.id_transaccion = :OLD.id_transaccion AND v.estado <> 'ANULADA';
                IF v_n > 0 THEN
                    RAISE_APPLICATION_ERROR(-20072, 'La transacción respalda el pago de una venta vigente: primero se anula la venta (RN32).');
                END IF;
            END IF;
            IF :NEW.estado <> 'PENDIENTE' AND :NEW.fecha_respuesta IS NULL THEN
                :NEW.fecha_respuesta := SYSTIMESTAMP;
            END IF;
        END IF;
        END IF;   -- INSERTING / UPDATING (un trigger compuesto no admite RETURN)
    END BEFORE EACH ROW;

    AFTER STATEMENT IS
        v_orig TRANSACCION_PASARELA%ROWTYPE;
        v_dev  NUMBER;
    BEGIN
        FOR i IN 1 .. g_origen.COUNT LOOP
            SELECT * INTO v_orig FROM TRANSACCION_PASARELA WHERE id_transaccion = g_origen(i);
            IF v_orig.tipo_operacion <> 'COBRO' OR v_orig.estado <> 'APROBADA' THEN
                RAISE_APPLICATION_ERROR(-20077, 'Solo se devuelve un cobro APROBADO (la transacción ' || g_origen(i) || ' está ' || v_orig.estado || ').');
            END IF;
            IF v_orig.id_medio_pago <> g_medio(i) THEN
                RAISE_APPLICATION_ERROR(-20077, 'La devolución va por el mismo medio con que se pagó (RN44).');
            END IF;
            SELECT NVL(SUM(monto), 0) INTO v_dev
              FROM TRANSACCION_PASARELA
             WHERE id_transaccion_origen = g_origen(i)
               AND tipo_operacion = 'DEVOLUCION'
               AND estado IN ('PENDIENTE', 'APROBADA');
            IF v_dev > v_orig.monto THEN
                RAISE_APPLICATION_ERROR(-20077, 'Las devoluciones (S/ ' || v_dev || ') superan el cobro original (S/ ' || v_orig.monto || ').');
            END IF;
        END LOOP;
        g_origen.DELETE;
        g_medio.DELETE;
    END AFTER STATEMENT;
END TRG_TRANS_PAS_REGLAS;
/

-- TRG_VENTA_PAGO_REGLAS (09, sección 8) · [v5.2] cambio 18 (D-22): una transacción cobrada para una venta solo
-- respalda un pago de esa venta (-20107).
CREATE OR REPLACE TRIGGER TRG_VENTA_PAGO_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON VENTA_PAGO
FOR EACH ROW
DECLARE
    v_estado       VENTA.estado%TYPE;
    v_sede         VENTA.id_sede%TYPE;
    v_medio        MEDIO_PAGO%ROWTYPE;
    v_tr           TRANSACCION_PASARELA%ROWTYPE;
    v_id_venta     NUMBER := CASE WHEN DELETING THEN :OLD.id_venta ELSE :NEW.id_venta END;
BEGIN
    SELECT estado, id_sede INTO v_estado, v_sede FROM VENTA WHERE id_venta = v_id_venta;
    IF v_estado <> 'ABIERTA' THEN
        RAISE_APPLICATION_ERROR(-20045, 'La venta está ' || v_estado || ': sus pagos ya no se modifican.');
    END IF;
    IF DELETING THEN
        RETURN;
    END IF;
    SELECT * INTO v_medio FROM MEDIO_PAGO WHERE id_medio_pago = :NEW.id_medio_pago;
    IF v_medio.estado <> 'ACTIVO' THEN
        RAISE_APPLICATION_ERROR(-20045, 'Ese medio de pago no está habilitado (RF88).');
    END IF;

    -- [v4] RN42: tarjeta, Yape y Plin se cobran por la pasarela; solo cuenta lo que la pasarela APROBÓ.
    IF v_medio.usa_pasarela = 'S' THEN
        IF :NEW.id_transaccion IS NULL THEN
            RAISE_APPLICATION_ERROR(-20071, 'Un pago con ' || v_medio.nombre
                || ' se cobra por la pasarela de pagos: indique la transacción APROBADA (RN42).');
        END IF;
        SELECT * INTO v_tr FROM TRANSACCION_PASARELA WHERE id_transaccion = :NEW.id_transaccion;
        IF v_tr.estado <> 'APROBADA' OR v_tr.tipo_operacion <> 'COBRO' THEN
            RAISE_APPLICATION_ERROR(-20071, 'La transacción ' || :NEW.id_transaccion || ' está ' || v_tr.estado
                || ': solo un cobro APROBADO por la pasarela respalda un pago (RN42).');
        END IF;
        IF v_tr.id_cita IS NOT NULL THEN
            RAISE_APPLICATION_ERROR(-20071, 'La transacción ' || :NEW.id_transaccion || ' pagó el adelanto de una cita, no esta venta.');
        END IF;
        -- [v5.2] D-22: la transacción cobrada para una venta solo respalda un pago de esa venta (RN42)
        IF v_tr.id_venta IS NOT NULL AND v_tr.id_venta <> :NEW.id_venta THEN
            RAISE_APPLICATION_ERROR(-20107, 'La transacción ' || :NEW.id_transaccion || ' cobró la venta N° ' || v_tr.id_venta
                || ', no la venta N° ' || :NEW.id_venta || ' (RN42).');
        END IF;
        IF v_tr.id_medio_pago <> :NEW.id_medio_pago OR v_tr.monto <> :NEW.monto OR v_tr.id_sede <> v_sede THEN
            RAISE_APPLICATION_ERROR(-20071, 'El pago (' || v_medio.nombre || ', S/ ' || :NEW.monto
                || ') no coincide con la transacción aprobada (S/ ' || v_tr.monto || ', mismo medio y sede).');
        END IF;
        :NEW.referencia := SUBSTR(v_tr.codigo_operacion, 1, 30);
    ELSIF :NEW.id_transaccion IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20071, 'El efectivo se cobra y se cuenta en caja: no lleva transacción de la pasarela.');
    END IF;
END;
/

-- Un CREATE OR REPLACE con errores de compilación no es un SQLERROR para SQL*Plus: aquí se detiene igual.
DECLARE
    v_lista VARCHAR2(4000);
BEGIN
    SELECT LISTAGG(DISTINCT name, ', ') WITHIN GROUP (ORDER BY name) INTO v_lista
      FROM user_errors
     WHERE name IN ('PKG_AGENDA', 'TRG_CITA_REGLAS', 'TRG_AUD_RECURSO', 'TRG_AUD_CAJA', 'TRG_AUD_TRANSACCION_PASARELA',
                    'TRG_CAJA_REGLAS', 'TRG_NOTA_CREDITO_REGLAS', 'TRG_TRANS_PAS_REGLAS', 'TRG_VENTA_PAGO_REGLAS');
    IF v_lista IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20099, 'Objetos v5.2 con errores de compilación: ' || v_lista || '. Revise user_errors.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('   b) a f) Paquete, triggers y vistas v5.2 compilados sin errores.');
END;
/

-- =============================================================================
-- g) a i) Datos y estructura
-- =============================================================================
PROMPT g) a i) CITA, PARAMETRO y RECURSO...
DECLARE
    v_n NUMBER;

    PROCEDURE ejecutar(p_sql IN VARCHAR2, p_aviso IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
        DBMS_OUTPUT.PUT_LINE('   + ' || p_aviso);
    END ejecutar;
BEGIN
    -- -------------------------------------------------------------------------
    -- g) CITA: el fin es el del servicio (cambio 3). TRG_CITA_CRUCE no se dispara (no cambian ni el inicio
    --    ni la duración) y TRG_CITA_REGLAS no recalcula (no cambia nada de lo que lo obliga).
    -- -------------------------------------------------------------------------
    UPDATE CITA
       SET fecha_hora_fin = fecha_hora_inicio + NUMTODSINTERVAL(duracion_min, 'MINUTE')
     WHERE fecha_hora_fin <> fecha_hora_inicio + NUMTODSINTERVAL(duracion_min, 'MINUTE');
    DBMS_OUTPUT.PUT_LINE('   g) CITA: ' || SQL%ROWCOUNT || ' cita(s) con el fin recalculado sin la limpieza.');
    COMMIT;

    -- -------------------------------------------------------------------------
    -- h) PARAMETRO: ya no hay tiempo de limpieza que limitar (cambio 7)
    -- -------------------------------------------------------------------------
    DELETE FROM PARAMETRO WHERE codigo = 'LIMPIEZA_MAX_MIN';
    DBMS_OUTPUT.PUT_LINE('   h) PARAMETRO: ' || SQL%ROWCOUNT || ' fila(s) LIMPIEZA_MAX_MIN borrada(s).');
    COMMIT;

    -- -------------------------------------------------------------------------
    -- i) RECURSO: sin tiempo de limpieza (cambios 1 y 2) y comentarios sin la limpieza (cambios 5 y 22)
    -- -------------------------------------------------------------------------
    SELECT COUNT(*) INTO v_n FROM user_constraints WHERE constraint_name = 'CK_RECURSO_MINUTOS_LIMPIEZA';
    IF v_n > 0 THEN
        ejecutar('ALTER TABLE RECURSO DROP CONSTRAINT CK_RECURSO_MINUTOS_LIMPIEZA', 'CK_RECURSO_MINUTOS_LIMPIEZA retirada');
    END IF;
    SELECT COUNT(*) INTO v_n FROM user_tab_columns WHERE table_name = 'RECURSO' AND column_name = 'MINUTOS_LIMPIEZA';
    IF v_n > 0 THEN
        ejecutar('ALTER TABLE RECURSO DROP COLUMN minutos_limpieza', 'RECURSO.minutos_limpieza retirada');
    END IF;
    EXECUTE IMMEDIATE q'[COMMENT ON TABLE RECURSO IS 'Recursos físicos por sede: sillones, estaciones, camillas, cabinas. Quedan libres en cuanto termina el servicio (sin tiempo de limpieza, RN03).']';
    EXECUTE IMMEDIATE q'[COMMENT ON TABLE PARAMETRO IS 'Parámetros del negocio que define la Administradora (IGV, RMV, umbral de boleta, márgenes, paso de la agenda...). Evita valores fijos en el código.']';
    EXECUTE IMMEDIATE q'[COMMENT ON TABLE CITA IS 'Citas agendadas, sin cruces de especialista ni recurso, con adelanto. El bloque termina con el servicio (sin limpieza, RN03).']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN CITA.fecha_hora_fin IS 'Fin del servicio = inicio + duración. La especialista y el recurso quedan libres desde ese momento (sin tiempo de limpieza). Columna derivada: la calcula TRG_CITA_REGLAS. (RN03, RN41)']';
END;
/

-- =============================================================================
-- j) Recompila lo inválido (las ALTER TABLE dejan objetos dependientes por recompilar)
-- =============================================================================
BEGIN
    DBMS_UTILITY.COMPILE_SCHEMA(schema => USER, compile_all => FALSE);
END;
/

-- =============================================================================
-- k) Resumen
-- =============================================================================
WHENEVER SQLERROR CONTINUE
SET FEEDBACK ON
PROMPT ==== Errores de compilación (debe salir vacío) ====
SELECT name, type, line, position, SUBSTR(text, 1, 200) AS error
  FROM user_errors
 ORDER BY name, type, sequence;

PROMPT ==== Resumen del esquema v5.2 ====
SELECT COUNT(*) AS tablas FROM user_tables;
SELECT constraint_type, COUNT(*) AS total FROM user_constraints
 WHERE constraint_name NOT LIKE 'SYS%' AND constraint_name NOT LIKE 'BIN$%'
 GROUP BY constraint_type ORDER BY constraint_type;
SELECT COUNT(*) AS indices_ix FROM user_indexes WHERE index_name LIKE 'IX\_%' ESCAPE '\';
SELECT object_type, COUNT(*) AS total FROM user_objects
 WHERE object_type IN ('FUNCTION', 'PACKAGE', 'PACKAGE BODY', 'TRIGGER', 'VIEW') GROUP BY object_type ORDER BY object_type;
SELECT COUNT(*) AS objetos_invalidos FROM user_objects WHERE status = 'INVALID';
SELECT COUNT(*) AS recurso_minutos_limpieza FROM user_tab_columns WHERE table_name = 'RECURSO' AND column_name = 'MINUTOS_LIMPIEZA';
SELECT COUNT(*) AS parametro_limpieza_max_min FROM PARAMETRO WHERE codigo = 'LIMPIEZA_MAX_MIN';
SELECT COUNT(*) AS cobros_con_venta FROM TRANSACCION_PASARELA WHERE id_venta IS NOT NULL;
SELECT COUNT(*) AS citas_con_otro_fin FROM CITA WHERE fecha_hora_fin <> fecha_hora_inicio + NUMTODSINTERVAL(duracion_min, 'MINUTE');
PROMPT Esperado: 57 tablas; P = 57, R = 132, U = 35, C = 183 con nombre propio (sale CK_RECURSO_MINUTOS_LIMPIEZA y entra
PROMPT           CK_TRANS_PAS_VENTA); 106 índices IX_ (105 de v5.1 e IX_TRANS_PAS_VENTA);
PROMPT           FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 129, VIEW 27; 0 objetos inválidos;
PROMPT           RECURSO sin minutos_limpieza (0), PARAMETRO sin LIMPIEZA_MAX_MIN (0) y 0 citas con otro fin.
PROMPT           Las filas llenadas en a) (id_venta) y g) (fin de CITA) se informan arriba.
PROMPT 18_ajustes_v52.sql terminado.
