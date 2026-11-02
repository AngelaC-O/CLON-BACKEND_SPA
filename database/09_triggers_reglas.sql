-- =====================================================================
-- CAUTIVA SPA E.I.R.L. · BackOffice web · Base de datos Oracle (12c o superior)
-- Script 09 (v5) · Triggers de reglas de negocio
-- Se ejecuta como: dueño del esquema v5 (por ejemplo CAUTIVA5), después de 08_paquetes.sql.
--
-- La base de datos hace cumplir las reglas aunque falle o se salte el backend.
-- Las comparaciones usan PKG_SESION.distinto(:OLD.x, :NEW.x) ("cambió el valor", tratando NULL = NULL),
-- no UPDATING('x'), porque un ORM como JPA/Hibernate envía todas las columnas en cada UPDATE.
-- Los triggers que necesitan consultar su propia tabla son COMPOUND (evitan ORA-04091 tabla mutante).
-- [v4] Sección 13: facturación electrónica con SUNAT, pasarela de pagos y consulta DNI/RUC (21/10/2026).
-- Códigos de error nuevos: -20070 a -20081.
-- [v5] Adaptado del script 13 de v4 (27/10/2026). Cambios:
--   * Se retiran los triggers de HISTORIAL_PRECIO, CONSUMO_SESION, CORRECCION_ASISTENCIA, SOLICITUD_REPOSICION,
--     CONSULTA_DOCUMENTO y los TRG_*_VERIFICADO: esas tablas ya no existen, y la verificación del documento la hace
--     TRG_PERSONA_REGLAS (script 06) sobre el supertipo PERSONA.
--   * La sesión de un paquete se descuenta en ATENCION.id_cliente_paquete: TRG_ATENCION_PAQUETE reemplaza a
--     TRG_CONSUMO_SESION_REGLAS con las mismas reglas y códigos (-20067 a -20069).
--   * La nota de crédito es una fila de COMPROBANTE (NC_BOLETA o NC_FACTURA) más su fila en NOTA_CREDITO:
--     TRG_COMPROBANTE_REGLAS numera las cuatro clases y TRG_NOTA_CREDITO_REGLAS valida la anulación.
--   * CAJA ya no guarda el efectivo esperado ni el contado: el cierre exige el arqueo de cada medio (CAJA_ARQUEO).
--   * PRODUCTO ya no tiene stock: solo INVENTARIO_SEDE.
--   * Nuevo TRG_REQ_DETALLE_REGLAS: el detalle de un requerimiento solo cambia mientras esté PENDIENTE.
-- =====================================================================
SET DEFINE OFF

-- =====================================================================
-- 1. Datos que nunca se modifican ni se eliminan
-- =====================================================================
PROMPT Triggers de inmutabilidad...
CREATE OR REPLACE TRIGGER TRG_AUDITORIA_INMUTABLE
BEFORE UPDATE OR DELETE ON AUDITORIA
BEGIN
    RAISE_APPLICATION_ERROR(-20020, 'La bitácora de auditoría no se modifica ni se elimina (RN29).');
END;
/
CREATE OR REPLACE TRIGGER TRG_KARDEX_INMUTABLE
BEFORE UPDATE OR DELETE ON MOVIMIENTO_KARDEX
BEGIN
    RAISE_APPLICATION_ERROR(-20020, 'Un movimiento de kardex no se modifica ni se elimina: corrija con un AJUSTE (RN18).');
END;
/
CREATE OR REPLACE TRIGGER TRG_SALDO_FAVOR_INMUTABLE
BEFORE UPDATE OR DELETE ON SALDO_FAVOR_MOV
BEGIN
    RAISE_APPLICATION_ERROR(-20020, 'Un movimiento de saldo a favor no se modifica ni se elimina: registre otro movimiento.');
END;
/
CREATE OR REPLACE TRIGGER TRG_COMPRA_DETALLE_INMUTABLE
BEFORE UPDATE OR DELETE ON COMPRA_DETALLE
BEGIN
    RAISE_APPLICATION_ERROR(-20020, 'Una línea de compra ya entró al kardex: corrija con un AJUSTE de inventario.');
END;
/
CREATE OR REPLACE TRIGGER TRG_PERSONAL_NO_ELIMINAR
BEFORE DELETE ON PERSONAL
BEGIN
    RAISE_APPLICATION_ERROR(-20021, 'El personal no se elimina: cambie su estado a INACTIVO (CUS11).');
END;
/
CREATE OR REPLACE TRIGGER TRG_CITA_NO_ELIMINAR
BEFORE DELETE ON CITA
BEGIN
    RAISE_APPLICATION_ERROR(-20021, 'Una cita no se elimina: se cancela o se reprograma (RF12).');
END;
/
CREATE OR REPLACE TRIGGER TRG_CAJA_NO_ELIMINAR
BEFORE DELETE ON CAJA
BEGIN
    RAISE_APPLICATION_ERROR(-20021, 'Una caja no se elimina.');
END;
/

-- =====================================================================
-- 2. Operaciones reservadas a la Administradora (RN24, RN25, RN28, RN38, RN39, RES28)
--    Trigger de sentencia: se ejecuta una vez por INSERT, UPDATE o DELETE.
-- =====================================================================
PROMPT Triggers de operaciones de la Administradora...
DECLARE
    TYPE t_tabla IS TABLE OF VARCHAR2(200) INDEX BY VARCHAR2(30);
    v t_tabla;
    k VARCHAR2(30);
BEGIN
    v('ROL')                := 'modificar roles';
    v('PERMISO')            := 'modificar permisos';
    v('ROL_PERMISO')        := 'asignar permisos a los roles';
    v('TIPO_DOCUMENTO')     := 'configurar los tipos de documento de identidad';   -- [v5]
    v('PARAMETRO')          := 'cambiar los parámetros del negocio';
    v('SEDE')               := 'crear o modificar sedes';
    v('RECURSO')            := 'configurar los recursos';
    v('PERSONAL')           := 'registrar o modificar al personal';
    v('PERSONAL_AREA')      := 'asignar áreas al personal';
    v('HORARIO_PERSONAL')   := 'definir los horarios del personal';
    v('PLANILLA')           := 'calcular la planilla';
    v('PLANILLA_DETALLE')   := 'calcular la planilla';
    v('ADELANTO')           := 'registrar adelantos de sueldo';
    v('AREA')               := 'modificar las áreas';
    v('SERVICIO')           := 'modificar el catálogo de servicios';
    v('VARIANTE_SERVICIO')  := 'cambiar precios o variantes';
    v('SERVICIO_INSUMO')    := 'definir el consumo de insumos';
    v('PROMOCION')          := 'crear promociones y descuentos';
    v('PROMOCION_SERVICIO') := 'crear promociones y descuentos';
    v('PAQUETE')            := 'crear paquetes y membresías';
    v('PAQUETE_SERVICIO')   := 'crear paquetes y membresías';
    v('MEDIO_PAGO')         := 'configurar los medios de pago';
    v('CONCILIACION')       := 'conciliar las ventas del mes';
    v('INCIDENTE_SEGURIDAD') := 'registrar incidentes de seguridad y su aviso (RN28)';
    v('CONCILIACION_ITEM')  := 'conciliar las ventas del mes';
    k := v.FIRST;
    WHILE k IS NOT NULL LOOP
        EXECUTE IMMEDIATE
            'CREATE OR REPLACE TRIGGER TRG_ADM_' || k || CHR(10) ||
            'BEFORE INSERT OR UPDATE OR DELETE ON ' || k || CHR(10) ||
            'BEGIN' || CHR(10) ||
            '    PKG_SESION.exigir_administradora(''' || REPLACE(v(k), '''', '''''') || ''');' || CHR(10) ||
            'END;';
        k := v.NEXT(k);
    END LOOP;
END;
/

-- =====================================================================
-- 3. Seguridad
-- =====================================================================
PROMPT Triggers de USUARIO...
CREATE OR REPLACE TRIGGER TRG_USUARIO_REGLAS
BEFORE INSERT OR UPDATE ON USUARIO
FOR EACH ROW
DECLARE
    v_estado_personal PERSONAL.estado%TYPE;
    v_actor           NUMBER := PKG_SESION.usuario_actual;
BEGIN
    IF INSERTING THEN
        PKG_SESION.exigir_administradora('crear usuarios y asignar roles (RN39)');
        SELECT estado INTO v_estado_personal FROM PERSONAL WHERE id_personal = :NEW.id_personal;
        IF v_estado_personal <> 'ACTIVO' THEN
            RAISE_APPLICATION_ERROR(-20005, 'No se crea una cuenta para una persona INACTIVA.');
        END IF;
        :NEW.fecha_creacion    := SYSTIMESTAMP;
        :NEW.intentos_fallidos := 0;
        RETURN;
    END IF;

    -- Rol, persona y nombre de usuario: solo la Administradora (RN39)
    IF PKG_SESION.distinto(:OLD.id_rol, :NEW.id_rol)
       OR PKG_SESION.distinto(:OLD.id_personal, :NEW.id_personal)
       OR PKG_SESION.distinto(:OLD.nombre_usuario, :NEW.nombre_usuario) THEN
        PKG_SESION.exigir_administradora('cambiar el rol o la identidad de un usuario (RN39)');
    END IF;

    -- RF77: el backend solo suma 1 a intentos_fallidos en cada contraseña errada (y lo pone en 0 al entrar bien).
    -- Al llegar a LOGIN_INTENTOS_MAX la BD bloquea la cuenta LOGIN_BLOQUEO_MIN minutos.
    IF :NEW.intentos_fallidos > :OLD.intentos_fallidos AND :NEW.estado = 'ACTIVO'
       AND :NEW.intentos_fallidos >= PKG_SESION.parametro_num('LOGIN_INTENTOS_MAX') THEN
        :NEW.estado          := 'BLOQUEADO';
        :NEW.bloqueado_hasta := CAST(SYSTIMESTAMP AS TIMESTAMP)
                                + NUMTODSINTERVAL(PKG_SESION.parametro_num('LOGIN_BLOQUEO_MIN'), 'MINUTE');
    END IF;
    -- Desbloquear: el login lo hace cuando ya pasó bloqueado_hasta; antes, solo la Administradora.
    IF :OLD.estado = 'BLOQUEADO' AND :NEW.estado = 'ACTIVO' THEN
        IF :OLD.bloqueado_hasta IS NULL OR CAST(SYSTIMESTAMP AS TIMESTAMP) < :OLD.bloqueado_hasta THEN
            PKG_SESION.exigir_administradora('desbloquear una cuenta antes de que venza su bloqueo (RF77)');
        END IF;
        :NEW.intentos_fallidos := 0;
        :NEW.bloqueado_hasta   := NULL;
    END IF;

    -- Activar o desactivar cuentas: solo la Administradora.
    IF PKG_SESION.distinto(:OLD.estado, :NEW.estado) AND 'INACTIVO' IN (:OLD.estado, :NEW.estado) THEN
        PKG_SESION.exigir_administradora('activar o desactivar cuentas (RF53)');
    END IF;

    -- Contraseña: la cambia el propio usuario; la de otro, solo la Administradora (RF82)
    IF PKG_SESION.distinto(:OLD.clave_hash, :NEW.clave_hash) THEN
        IF v_actor IS NULL AND PKG_SESION.es_aplicacion THEN
            RAISE_APPLICATION_ERROR(-20001, 'Llame a PKG_SESION.iniciar antes de cambiar una contraseña.');
        ELSIF v_actor IS NOT NULL AND v_actor <> :NEW.id_usuario THEN
            PKG_SESION.exigir_administradora('restablecer la contraseña de otro usuario (RF82)');
            :NEW.debe_cambiar_clave := 'S';
        END IF;
        :NEW.intentos_fallidos := 0;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_PARAMETRO_USUARIO
BEFORE INSERT OR UPDATE ON PARAMETRO
FOR EACH ROW
BEGIN
    :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
END;
/

-- =====================================================================
-- 4. Personal, asistencia y planilla
-- =====================================================================
PROMPT Triggers de personal y asistencia...
CREATE OR REPLACE TRIGGER TRG_PERSONAL_DESACTIVAR
AFTER UPDATE OF estado ON PERSONAL
FOR EACH ROW
WHEN (OLD.estado = 'ACTIVO' AND NEW.estado = 'INACTIVO')
BEGIN
    -- RF53: al desactivar a una persona se revoca su acceso al sistema
    UPDATE USUARIO SET estado = 'INACTIVO' WHERE id_personal = :NEW.id_personal AND estado <> 'INACTIVO';
END;
/

CREATE OR REPLACE TRIGGER TRG_ASISTENCIA_REGLAS
BEFORE INSERT OR UPDATE ON ASISTENCIA
FOR EACH ROW
DECLARE
    v_he     HORARIO_PERSONAL.hora_entrada%TYPE;
    v_prog   DATE;
    v_horas  NUMBER;
    v_dia    NUMBER;
BEGIN
    :NEW.fecha := TRUNC(:NEW.fecha);
    IF INSERTING THEN
        :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
    END IF;

    -- RF56: corregir una marca de un día anterior es tarea de la Administradora
    IF UPDATING AND :OLD.fecha < TRUNC(SYSDATE)
       AND (PKG_SESION.distinto(:OLD.hora_entrada, :NEW.hora_entrada)
            OR PKG_SESION.distinto(:OLD.hora_salida, :NEW.hora_salida)
            OR PKG_SESION.distinto(:OLD.hora_inicio_almuerzo, :NEW.hora_inicio_almuerzo)
            OR PKG_SESION.distinto(:OLD.hora_fin_almuerzo, :NEW.hora_fin_almuerzo)
            OR PKG_SESION.distinto(:OLD.estado, :NEW.estado)) THEN
        PKG_SESION.exigir_administradora('corregir marcas de días anteriores (RF56; el cambio queda en AUDITORIA)');
    END IF;

    -- RF55: minutos de tardanza contra el horario del día (1 = lunes ... 7 = domingo, sin depender de NLS)
    IF :NEW.hora_entrada IS NOT NULL THEN
        v_dia := TRUNC(:NEW.fecha) - TRUNC(:NEW.fecha, 'IW') + 1;
        BEGIN
            SELECT hora_entrada INTO v_he
              FROM HORARIO_PERSONAL
             WHERE id_personal = :NEW.id_personal AND dia_semana = v_dia;
            v_prog := TRUNC(:NEW.fecha) + TO_NUMBER(SUBSTR(v_he, 1, 2)) / 24 + TO_NUMBER(SUBSTR(v_he, 4, 2)) / 1440;
            :NEW.minutos_tardanza := GREATEST(0, ROUND((CAST(:NEW.hora_entrada AS DATE) - v_prog) * 1440));
            IF :NEW.minutos_tardanza > 0 AND :NEW.estado = 'PRESENTE' THEN
                :NEW.estado := 'TARDANZA';
            END IF;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                :NEW.minutos_tardanza := 0;   -- sin horario definido para ese día
        END;
    END IF;

    -- Horas trabajadas (derivada): salida - entrada - almuerzo
    IF :NEW.hora_entrada IS NOT NULL AND :NEW.hora_salida IS NOT NULL THEN
        v_horas := (CAST(:NEW.hora_salida AS DATE) - CAST(:NEW.hora_entrada AS DATE)) * 24;
        IF :NEW.hora_inicio_almuerzo IS NOT NULL AND :NEW.hora_fin_almuerzo IS NOT NULL THEN
            v_horas := v_horas - (CAST(:NEW.hora_fin_almuerzo AS DATE) - CAST(:NEW.hora_inicio_almuerzo AS DATE)) * 24;
        END IF;
        :NEW.horas_trabajadas := ROUND(GREATEST(v_horas, 0), 2);
    ELSE
        :NEW.horas_trabajadas := NULL;
    END IF;
END;
/



CREATE OR REPLACE TRIGGER TRG_PLANILLA_BLOQUEADA
BEFORE UPDATE ON PLANILLA
FOR EACH ROW
WHEN (OLD.estado = 'BLOQUEADA')
BEGIN
    RAISE_APPLICATION_ERROR(-20023, 'La planilla ' || :OLD.mes || '/' || :OLD.anio || ' está BLOQUEADA y no se recalcula.');
END;
/

CREATE OR REPLACE TRIGGER TRG_PLANILLA_DETALLE_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON PLANILLA_DETALLE
FOR EACH ROW
DECLARE
    v_estado PLANILLA.estado%TYPE;
    v_id     NUMBER := CASE WHEN DELETING THEN :OLD.id_planilla ELSE :NEW.id_planilla END;
BEGIN
    SELECT estado INTO v_estado FROM PLANILLA WHERE id_planilla = v_id;
    IF v_estado = 'BLOQUEADA' THEN
        RAISE_APPLICATION_ERROR(-20023, 'La planilla está BLOQUEADA y no se recalcula.');
    END IF;
    IF NOT DELETING THEN
        -- Neto derivado (desnormalización controlada del diccionario)
        :NEW.neto := :NEW.sueldo_base + :NEW.comision - :NEW.descuento_faltas - :NEW.descuento_tardanzas - :NEW.adelantos;
    END IF;
END;
/


-- =====================================================================
-- 6. Agenda: citas y atenciones (RN01, RN02, RN03, RN05, RN34, RN41, RF83)
-- =====================================================================
PROMPT Triggers de CITA...
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

        -- RN03: el bloque incluye la limpieza del recurso
        :NEW.fecha_hora_fin := :NEW.fecha_hora_inicio + NUMTODSINTERVAL(:NEW.duracion_min + v_rec.minutos_limpieza, 'MINUTE');
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

CREATE OR REPLACE TRIGGER TRG_CITA_CRUCE
FOR INSERT OR UPDATE OF fecha_hora_inicio, duracion_min, id_personal, id_recurso ON CITA
COMPOUND TRIGGER
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    g_ids t_ids;

    AFTER EACH ROW IS
    BEGIN
        IF :NEW.estado IN ('PENDIENTE', 'EN_ATENCION') THEN
            g_ids(g_ids.COUNT + 1) := :NEW.id_cita;
        END IF;
    END AFTER EACH ROW;

    AFTER STATEMENT IS
    BEGIN
        FOR i IN 1 .. g_ids.COUNT LOOP
            PKG_AGENDA.validar_cita(g_ids(i));
        END LOOP;
        g_ids.DELETE;
    END AFTER STATEMENT;
END TRG_CITA_CRUCE;
/

PROMPT Triggers de ATENCION...
CREATE OR REPLACE TRIGGER TRG_ATENCION_REGLAS
BEFORE INSERT OR UPDATE ON ATENCION
FOR EACH ROW
DECLARE
    v_rec_estado RECURSO.estado%TYPE;
    v_per_estado PERSONAL.estado%TYPE;
    v_per_cargo  PERSONAL.cargo%TYPE;
    v_cita_cli   CITA.id_cliente%TYPE;
    v_cita_est   CITA.estado%TYPE;
BEGIN
    IF INSERTING THEN
        IF :NEW.estado <> 'EN_CURSO' THEN
            RAISE_APPLICATION_ERROR(-20024, 'Una atención nueva nace EN_CURSO.');
        END IF;
        IF :NEW.id_cita IS NOT NULL THEN
            SELECT id_cliente, estado INTO v_cita_cli, v_cita_est FROM CITA WHERE id_cita = :NEW.id_cita;
            IF v_cita_est NOT IN ('PENDIENTE', 'EN_ATENCION') THEN
                RAISE_APPLICATION_ERROR(-20024, 'La cita N° ' || :NEW.id_cita || ' está ' || v_cita_est || ' y no se puede atender.');
            END IF;
            :NEW.id_cliente          := v_cita_cli;   -- la atención es de la clienta de la cita
            :NEW.nombre_cliente_paso := NULL;          -- [v5] sin es_cliente_paso: lo dice id_cliente
        END IF;
    ELSE
        IF PKG_SESION.distinto(:OLD.estado, :NEW.estado)
           AND NOT ((:OLD.estado = 'EN_CURSO'   AND :NEW.estado = 'FINALIZADA')
                 OR (:OLD.estado = 'FINALIZADA' AND :NEW.estado = 'COBRADA')
                 OR (:OLD.estado = 'COBRADA'    AND :NEW.estado = 'FINALIZADA')) THEN
            RAISE_APPLICATION_ERROR(-20024, 'Cambio de estado no permitido: ' || :OLD.estado || ' -> ' || :NEW.estado || '.');
        END IF;
        IF :OLD.estado <> 'EN_CURSO'
           AND (PKG_SESION.distinto(:OLD.id_personal, :NEW.id_personal)
                OR PKG_SESION.distinto(:OLD.id_variante, :NEW.id_variante)
                OR PKG_SESION.distinto(:OLD.id_cliente, :NEW.id_cliente)
                OR PKG_SESION.distinto(:OLD.id_cita, :NEW.id_cita)
                OR (:OLD.estado = 'COBRADA' AND PKG_SESION.distinto(:OLD.id_cliente_paquete, :NEW.id_cliente_paquete))) THEN
            RAISE_APPLICATION_ERROR(-20024, 'Una atención finalizada no cambia de especialista, servicio ni clienta,'
                || ' y una atención cobrada no cambia el paquete que la cubre.');
        END IF;
        IF :NEW.estado = 'FINALIZADA' AND :OLD.estado = 'EN_CURSO' THEN
            :NEW.hora_fin := NVL(:NEW.hora_fin, SYSTIMESTAMP);   -- RF13
        END IF;
    END IF;

    IF INSERTING OR PKG_SESION.distinto(:OLD.id_personal, :NEW.id_personal)
                 OR PKG_SESION.distinto(:OLD.id_recurso, :NEW.id_recurso)
                 OR PKG_SESION.distinto(:OLD.id_variante, :NEW.id_variante) THEN
        SELECT estado INTO v_rec_estado FROM RECURSO WHERE id_recurso = :NEW.id_recurso;
        IF v_rec_estado <> 'DISPONIBLE' THEN
            RAISE_APPLICATION_ERROR(-20017, 'El recurso está ' || v_rec_estado || '.');
        END IF;
        SELECT estado, cargo INTO v_per_estado, v_per_cargo FROM PERSONAL WHERE id_personal = :NEW.id_personal;
        IF v_per_estado <> 'ACTIVO' OR v_per_cargo NOT IN ('ESPECIALISTA_TECNICA', 'ADMINISTRADORA') THEN
            RAISE_APPLICATION_ERROR(-20018, 'Solo una especialista técnica (o la Administradora) activa realiza atenciones.');
        END IF;
        PKG_AGENDA.validar_habilitacion(:NEW.id_personal, :NEW.id_variante);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_ATENCION_CRUCE
FOR INSERT OR UPDATE OF hora_inicio, tiempo_declarado_min, id_personal, id_recurso ON ATENCION
COMPOUND TRIGGER
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    g_ids t_ids;

    AFTER EACH ROW IS
    BEGIN
        IF :NEW.estado = 'EN_CURSO' THEN   -- con o sin cita (RN34, RN41)
            g_ids(g_ids.COUNT + 1) := :NEW.id_atencion;
        END IF;
    END AFTER EACH ROW;

    AFTER STATEMENT IS
    BEGIN
        FOR i IN 1 .. g_ids.COUNT LOOP
            PKG_AGENDA.validar_atencion(g_ids(i));
        END LOOP;
        g_ids.DELETE;
    END AFTER STATEMENT;
END TRG_ATENCION_CRUCE;
/

-- =====================================================================
-- 7. Paquetes, sesiones y saldo a favor (RN05, RN36, RN37)
--    [v5] la sesión consumida es ATENCION.id_cliente_paquete
-- =====================================================================
PROMPT Triggers de paquetes y saldo a favor...
CREATE OR REPLACE TRIGGER TRG_CLIENTE_PAQUETE_REGLAS
BEFORE INSERT ON CLIENTE_PAQUETE
FOR EACH ROW
DECLARE
    v_tipo     VENTA_DETALLE.tipo_item%TYPE;
    v_paquete  VENTA_DETALLE.id_paquete%TYPE;
    v_cantidad VENTA_DETALLE.cantidad%TYPE;
    v_cliente  VENTA.id_cliente%TYPE;
    v_estado   VENTA.estado%TYPE;
    v_sesiones PAQUETE.num_sesiones%TYPE;
    v_vigencia PAQUETE.vigencia_dias%TYPE;
BEGIN
    SELECT d.tipo_item, d.id_paquete, d.cantidad, v.id_cliente, v.estado
      INTO v_tipo, v_paquete, v_cantidad, v_cliente, v_estado
      FROM VENTA_DETALLE d
      JOIN VENTA v ON v.id_venta = d.id_venta
     WHERE d.id_detalle = :NEW.id_detalle;
    IF v_tipo <> 'PAQUETE' OR v_paquete <> :NEW.id_paquete THEN
        RAISE_APPLICATION_ERROR(-20066, 'La línea de venta no corresponde a ese paquete.');
    END IF;
    IF v_cliente IS NULL OR v_cliente <> :NEW.id_cliente THEN
        RAISE_APPLICATION_ERROR(-20066, 'El paquete pertenece a la clienta de la venta (un cliente de paso no compra paquetes).');
    END IF;
    IF v_estado = 'ANULADA' THEN
        RAISE_APPLICATION_ERROR(-20066, 'La venta del paquete está anulada.');
    END IF;
    SELECT num_sesiones, vigencia_dias INTO v_sesiones, v_vigencia FROM PAQUETE WHERE id_paquete = :NEW.id_paquete;
    -- RN36, RN37: sesiones y vencimiento los calcula la base de datos
    :NEW.sesiones_totales := v_sesiones * v_cantidad;
    :NEW.fecha_compra     := NVL(:NEW.fecha_compra, TRUNC(SYSDATE));
    :NEW.fecha_vencimiento := CASE WHEN v_vigencia IS NULL THEN NULL ELSE :NEW.fecha_compra + v_vigencia END;
    :NEW.estado           := 'ACTIVO';
END;
/

-- [v5] La sesión usada es la propia atención: ATENCION.id_cliente_paquete (antes, una fila de CONSUMO_SESION).
-- Las sesiones usadas se cuentan, no se guardan: COUNT(ATENCION) del paquete (RN36, RN37).
CREATE OR REPLACE TRIGGER TRG_ATENCION_PAQUETE
FOR INSERT OR UPDATE OF id_cliente_paquete ON ATENCION
COMPOUND TRIGGER
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    g_ids      t_ids;   -- paquetes que ganan una sesión en esta sentencia
    g_liberados t_ids;  -- paquetes que pierden una sesión (se quitó o cambió el paquete)

    BEFORE EACH ROW IS
        v_cp  CLIENTE_PAQUETE%ROWTYPE;
        v_n   NUMBER;
        v_cli ATENCION.id_cliente%TYPE := :NEW.id_cliente;
    BEGIN
        IF UPDATING AND :OLD.id_cliente_paquete IS NOT NULL
           AND PKG_SESION.distinto(:OLD.id_cliente_paquete, :NEW.id_cliente_paquete) THEN
            g_liberados(g_liberados.COUNT + 1) := :OLD.id_cliente_paquete;
        END IF;
        IF :NEW.id_cliente_paquete IS NOT NULL
           AND (INSERTING OR PKG_SESION.distinto(:OLD.id_cliente_paquete, :NEW.id_cliente_paquete)) THEN
            -- Bloquea el paquete: dos recepcionistas no pueden gastar la última sesión a la vez
            SELECT * INTO v_cp FROM CLIENTE_PAQUETE WHERE id_cliente_paquete = :NEW.id_cliente_paquete FOR UPDATE;
            IF v_cp.estado <> 'ACTIVO' THEN
                RAISE_APPLICATION_ERROR(-20067, 'El paquete está ' || v_cp.estado || ' y no tiene sesiones disponibles.');
            END IF;
            IF v_cp.fecha_vencimiento IS NOT NULL AND v_cp.fecha_vencimiento < TRUNC(SYSDATE) THEN
                RAISE_APPLICATION_ERROR(-20067, 'El paquete venció el ' || TO_CHAR(v_cp.fecha_vencimiento, 'DD/MM/YYYY') || '.');
            END IF;
            -- La clienta de una atención con cita es la de la cita (no depende del orden de los triggers)
            IF v_cli IS NULL AND :NEW.id_cita IS NOT NULL THEN
                SELECT id_cliente INTO v_cli FROM CITA WHERE id_cita = :NEW.id_cita;
            END IF;
            IF v_cli IS NULL OR v_cli <> v_cp.id_cliente THEN
                RAISE_APPLICATION_ERROR(-20068, 'La atención no es de la clienta dueña del paquete.');
            END IF;
            IF :NEW.estado = 'COBRADA' THEN
                RAISE_APPLICATION_ERROR(-20068, 'La atención ya fue cobrada; indique el paquete antes de cobrar.');
            END IF;
            SELECT COUNT(*) INTO v_n
              FROM PAQUETE_SERVICIO ps
              JOIN VARIANTE_SERVICIO v ON v.id_servicio = ps.id_servicio
             WHERE ps.id_paquete = v_cp.id_paquete AND v.id_variante = :NEW.id_variante;
            IF v_n = 0 THEN
                RAISE_APPLICATION_ERROR(-20068, 'El servicio atendido no está incluido en el paquete (RN36).');
            END IF;
            g_ids(g_ids.COUNT + 1) := :NEW.id_cliente_paquete;
        END IF;
    END BEFORE EACH ROW;

    AFTER STATEMENT IS
        v_usadas NUMBER;
        v_total  NUMBER;
    BEGIN
        FOR i IN 1 .. g_ids.COUNT LOOP
            SELECT COUNT(*) INTO v_usadas FROM ATENCION WHERE id_cliente_paquete = g_ids(i);
            SELECT sesiones_totales INTO v_total FROM CLIENTE_PAQUETE WHERE id_cliente_paquete = g_ids(i);
            IF v_usadas > v_total THEN
                RAISE_APPLICATION_ERROR(-20069, 'El paquete no tiene sesiones disponibles (' || v_total || ' de ' || v_total || ' usadas).');
            ELSIF v_usadas = v_total THEN
                UPDATE CLIENTE_PAQUETE SET estado = 'AGOTADO'
                 WHERE id_cliente_paquete = g_ids(i) AND estado = 'ACTIVO';
            END IF;
        END LOOP;
        -- Una sesión devuelta reactiva el paquete agotado (si no venció)
        FOR i IN 1 .. g_liberados.COUNT LOOP
            UPDATE CLIENTE_PAQUETE cp SET estado = 'ACTIVO'
             WHERE cp.id_cliente_paquete = g_liberados(i)
               AND cp.estado = 'AGOTADO'
               AND (cp.fecha_vencimiento IS NULL OR cp.fecha_vencimiento >= TRUNC(SYSDATE))
               AND (SELECT COUNT(*) FROM ATENCION a WHERE a.id_cliente_paquete = cp.id_cliente_paquete) < cp.sesiones_totales;
        END LOOP;
        g_ids.DELETE;
        g_liberados.DELETE;
    END AFTER STATEMENT;
END TRG_ATENCION_PAQUETE;
/

CREATE OR REPLACE TRIGGER TRG_SALDO_FAVOR_REGLAS
FOR INSERT ON SALDO_FAVOR_MOV
COMPOUND TRIGGER
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    g_clientes t_ids;
    g_citas    t_ids;

    BEFORE EACH ROW IS
        v_dummy   NUMBER;
        v_estado  VARCHAR2(15);
        v_cliente NUMBER;
    BEGIN
        -- Bloquea a la clienta: serializa los usos simultáneos de su saldo
        SELECT id_cliente INTO v_dummy FROM CLIENTE WHERE id_cliente = :NEW.id_cliente FOR UPDATE;
        IF :NEW.tipo = 'ABONO' AND :NEW.id_cita IS NOT NULL THEN
            SELECT estado, id_cliente INTO v_estado, v_cliente FROM CITA WHERE id_cita = :NEW.id_cita;
            IF v_estado <> 'CANCELADA' THEN
                RAISE_APPLICATION_ERROR(-20063, 'Solo el adelanto de una cita CANCELADA pasa a saldo a favor (RN05).');
            END IF;
            IF v_cliente <> :NEW.id_cliente THEN
                RAISE_APPLICATION_ERROR(-20063, 'La cita es de otra clienta.');
            END IF;
            g_citas(g_citas.COUNT + 1) := :NEW.id_cita;
        ELSIF :NEW.tipo = 'ABONO' THEN
            -- [v5] El abono de una venta anulada (saldo usado o pago devuelto como saldo) lo registra la BD
            -- al anular la venta (PKG_VENTA.al_anular). Uno hecho a mano sumaría el dinero dos veces.
            IF NOT PKG_VENTA.g_anulando THEN
                RAISE_APPLICATION_ERROR(-20063, 'El abono de una venta anulada lo registra la BD al anular la venta'
                    || ' (RN33, RN44): no se registra a mano, porque se abonaría dos veces.');
            END IF;
        ELSIF :NEW.tipo = 'USO' THEN
            SELECT estado, id_cliente INTO v_estado, v_cliente FROM VENTA WHERE id_venta = :NEW.id_venta;
            IF v_estado <> 'ABIERTA' THEN
                RAISE_APPLICATION_ERROR(-20063, 'El saldo a favor se aplica a una venta ABIERTA.');
            END IF;
            IF v_cliente IS NULL OR v_cliente <> :NEW.id_cliente THEN
                RAISE_APPLICATION_ERROR(-20063, 'El saldo a favor solo lo usa su dueña.');
            END IF;
        END IF;
        :NEW.fecha := SYSTIMESTAMP;
        g_clientes(g_clientes.COUNT + 1) := :NEW.id_cliente;
    END BEFORE EACH ROW;

    AFTER STATEMENT IS
        v_saldo   NUMBER;
        v_abonos  NUMBER;
        v_adelant NUMBER;
    BEGIN
        FOR i IN 1 .. g_clientes.COUNT LOOP
            SELECT NVL(SUM(CASE tipo WHEN 'ABONO' THEN monto ELSE -monto END), 0) INTO v_saldo
              FROM SALDO_FAVOR_MOV WHERE id_cliente = g_clientes(i);
            IF v_saldo < 0 THEN
                RAISE_APPLICATION_ERROR(-20064, 'El saldo a favor de la clienta no alcanza (faltan S/ ' || ABS(v_saldo) || ').');
            END IF;
        END LOOP;
        FOR i IN 1 .. g_citas.COUNT LOOP
            SELECT NVL(SUM(monto), 0) INTO v_abonos FROM SALDO_FAVOR_MOV WHERE id_cita = g_citas(i) AND tipo = 'ABONO';
            SELECT adelanto_monto INTO v_adelant FROM CITA WHERE id_cita = g_citas(i);
            IF v_abonos > v_adelant THEN
                RAISE_APPLICATION_ERROR(-20065, 'El abono (S/ ' || v_abonos || ') supera el adelanto pagado en la cita (S/ ' || v_adelant || ').');
            END IF;
        END LOOP;
        g_clientes.DELETE;
        g_citas.DELETE;
    END AFTER STATEMENT;
END TRG_SALDO_FAVOR_REGLAS;
/

-- =====================================================================
-- 8. Ventas y comprobantes (RN06-RN12, RN25, RN30, RN32, RN33, RN38)
-- =====================================================================
PROMPT Triggers de VENTA...
CREATE OR REPLACE TRIGGER TRG_VENTA_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON VENTA
FOR EACH ROW
DECLARE
    PROCEDURE validar_caja(p_id_caja IN NUMBER, p_id_sede IN NUMBER, p_autoriza IN NUMBER) IS
        v_caja_estado CAJA.estado%TYPE;
        v_caja_sede   CAJA.id_sede%TYPE;
    BEGIN
        SELECT estado, id_sede INTO v_caja_estado, v_caja_sede FROM CAJA WHERE id_caja = p_id_caja;
        IF v_caja_sede <> p_id_sede THEN
            RAISE_APPLICATION_ERROR(-20042, 'La caja no pertenece a la sede de la venta.');
        END IF;
        IF v_caja_estado = 'CERRADA' AND (p_autoriza IS NULL OR NOT PKG_SESION.es_administradora(p_autoriza)) THEN
            RAISE_APPLICATION_ERROR(-20042, 'La caja del día está cerrada: registrar una venta en ella requiere autorización de la Administradora (RN30).');
        END IF;
    END validar_caja;
BEGIN
    IF DELETING THEN
        IF :OLD.estado <> 'ABIERTA' THEN
            RAISE_APPLICATION_ERROR(-20041, 'Una venta ' || :OLD.estado || ' nunca se elimina: se anula con nota de crédito (RN12).');
        END IF;
        RETURN;
    END IF;

    IF INSERTING THEN
        IF :NEW.estado <> 'ABIERTA' THEN
            RAISE_APPLICATION_ERROR(-20040, 'Una venta nace ABIERTA y se cierra cuando tiene comprobante y pagos completos (RN10, RN11).');
        END IF;
        :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
        :NEW.fecha_hora := SYSTIMESTAMP;
        validar_caja(:NEW.id_caja, :NEW.id_sede, :NEW.id_usuario_autoriza);
        RETURN;
    END IF;

    -- UPDATING
    IF :OLD.estado = 'ANULADA' THEN
        RAISE_APPLICATION_ERROR(-20043, 'La venta N° ' || :OLD.id_venta || ' está ANULADA y no se modifica (RN33).');
    END IF;

    IF :OLD.estado = 'CERRADA' THEN
        IF :NEW.estado <> 'ANULADA'
           OR PKG_SESION.distinto(:OLD.id_sede, :NEW.id_sede)
           OR PKG_SESION.distinto(:OLD.id_caja, :NEW.id_caja)
           OR PKG_SESION.distinto(:OLD.id_cliente, :NEW.id_cliente)
           OR PKG_SESION.distinto(:OLD.id_usuario, :NEW.id_usuario)
           OR PKG_SESION.distinto(:OLD.id_usuario_autoriza, :NEW.id_usuario_autoriza)
           OR PKG_SESION.distinto(:OLD.fecha_hora, :NEW.fecha_hora)
           OR PKG_SESION.distinto(:OLD.subtotal, :NEW.subtotal)
           OR PKG_SESION.distinto(:OLD.igv, :NEW.igv)
           OR PKG_SESION.distinto(:OLD.descuento, :NEW.descuento)
           OR PKG_SESION.distinto(:OLD.adelanto_aplicado, :NEW.adelanto_aplicado)
           OR PKG_SESION.distinto(:OLD.saldo_favor_aplicado, :NEW.saldo_favor_aplicado)
           OR PKG_SESION.distinto(:OLD.total, :NEW.total) THEN
            RAISE_APPLICATION_ERROR(-20044, 'Una venta cerrada no se modifica: solo se anula con nota de crédito (RN12).');
        END IF;
        PKG_VENTA.validar_anulacion(:NEW.id_venta);
        RETURN;
    END IF;

    -- :OLD.estado = 'ABIERTA'
    IF :NEW.estado = 'ANULADA' THEN
        RAISE_APPLICATION_ERROR(-20040, 'Una venta ABIERTA no se anula: se descarta (se elimina con sus líneas).');
    END IF;
    IF PKG_SESION.distinto(:OLD.id_caja, :NEW.id_caja) OR PKG_SESION.distinto(:OLD.id_sede, :NEW.id_sede) OR :NEW.estado = 'CERRADA' THEN
        validar_caja(:NEW.id_caja, :NEW.id_sede, :NEW.id_usuario_autoriza);
    END IF;
    IF :NEW.estado = 'CERRADA' THEN
        PKG_VENTA.validar_cierre(:NEW.id_venta, :NEW.subtotal, :NEW.igv, :NEW.descuento,
                                 :NEW.adelanto_aplicado, :NEW.saldo_favor_aplicado, :NEW.total);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_VENTA_EFECTOS
AFTER UPDATE OF estado ON VENTA
FOR EACH ROW
BEGIN
    IF :OLD.estado = 'ABIERTA' AND :NEW.estado = 'CERRADA' THEN
        PKG_VENTA.al_cerrar(:NEW.id_venta, :NEW.id_sede, :NEW.id_usuario);
    ELSIF :OLD.estado = 'CERRADA' AND :NEW.estado = 'ANULADA' THEN
        PKG_VENTA.al_anular(:NEW.id_venta, :NEW.id_sede, :NEW.id_cliente, :NEW.saldo_favor_aplicado, :NEW.id_usuario, :NEW.total);
    END IF;
END;
/

PROMPT Trigger de VENTA_DETALLE...
CREATE OR REPLACE TRIGGER TRG_VENTA_DETALLE_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON VENTA_DETALLE
FOR EACH ROW
DECLARE
    v_venta_estado VENTA.estado%TYPE;
    v_venta_cli    VENTA.id_cliente%TYPE;
    v_venta_sede   VENTA.id_sede%TYPE;
    v_precio       NUMBER;
    v_estado       VARCHAR2(15);
    v_tipo_uso     PRODUCTO.tipo_uso%TYPE;
    v_at_variante  ATENCION.id_variante%TYPE;
    v_at_personal  ATENCION.id_personal%TYPE;
    v_at_cliente   ATENCION.id_cliente%TYPE;
    v_at_estado    ATENCION.estado%TYPE;
    v_at_paquete   ATENCION.id_cliente_paquete%TYPE;
    v_pct          NUMBER;
    v_promo        PROMOCION%ROWTYPE;
    v_n            NUMBER;
    v_id_venta     NUMBER := CASE WHEN DELETING THEN :OLD.id_venta ELSE :NEW.id_venta END;
BEGIN
    SELECT estado, id_cliente, id_sede INTO v_venta_estado, v_venta_cli, v_venta_sede FROM VENTA WHERE id_venta = v_id_venta;
    IF v_venta_estado <> 'ABIERTA' THEN
        RAISE_APPLICATION_ERROR(-20045, 'La venta está ' || v_venta_estado || ': sus líneas ya no se modifican (RN12).');
    END IF;
    IF DELETING THEN
        RETURN;
    END IF;

    :NEW.porcentaje_comision := 0;
    :NEW.monto_comision      := 0;

    IF :NEW.tipo_item = 'SERVICIO' THEN
        -- Un servicio se cobra desde una atención realizada (RF21) y con el precio fijo de su variante (RN25, RN40)
        IF :NEW.id_atencion IS NULL THEN
            RAISE_APPLICATION_ERROR(-20046, 'Un servicio se cobra desde su atención (RF21): indique id_atencion.');
        END IF;
        -- [v5] VENTA_DETALLE ya no guarda id_variante: la variante es la de la atención
        SELECT id_variante, id_personal, id_cliente, estado, id_cliente_paquete
          INTO v_at_variante, v_at_personal, v_at_cliente, v_at_estado, v_at_paquete
          FROM ATENCION WHERE id_atencion = :NEW.id_atencion;
        IF v_at_estado <> 'FINALIZADA' THEN
            RAISE_APPLICATION_ERROR(-20046, 'La atención N° ' || :NEW.id_atencion || ' está ' || v_at_estado || ': se cobra una atención FINALIZADA.');
        END IF;
        IF PKG_SESION.distinto(v_at_cliente, v_venta_cli) THEN
            RAISE_APPLICATION_ERROR(-20046, 'La atención es de otra clienta.');
        END IF;
        :NEW.id_personal := v_at_personal;   -- la comisión es de quien atendió
        :NEW.cantidad    := 1;

        IF v_at_paquete IS NOT NULL THEN
            -- RN36 y RN38: cubierto con sesión de paquete o membresía: no se cobra ni genera comisión
            :NEW.precio_unitario := 0;
            :NEW.descuento       := 0;
            :NEW.id_promocion    := NULL;
        ELSE
            SELECT precio INTO v_precio FROM VARIANTE_SERVICIO WHERE id_variante = v_at_variante;
            :NEW.precio_unitario := v_precio;
            :NEW.descuento       := 0;
            IF :NEW.id_promocion IS NOT NULL THEN
                SELECT * INTO v_promo FROM PROMOCION WHERE id_promocion = :NEW.id_promocion;
                SELECT COUNT(*) INTO v_n
                  FROM PROMOCION_SERVICIO ps
                  JOIN VARIANTE_SERVICIO v ON v.id_servicio = ps.id_servicio
                 WHERE ps.id_promocion = :NEW.id_promocion AND v.id_variante = v_at_variante;
                IF v_promo.estado <> 'ACTIVA' OR TRUNC(SYSDATE) NOT BETWEEN v_promo.fecha_inicio AND v_promo.fecha_fin OR v_n = 0 THEN
                    RAISE_APPLICATION_ERROR(-20047, 'La promoción "' || v_promo.nombre || '" no está vigente o no aplica a este servicio.');
                END IF;
                -- RN25: el descuento lo calcula la base de datos según la promoción de la Administradora
                IF v_promo.tipo_descuento = 'PORCENTAJE' THEN
                    :NEW.descuento := ROUND(:NEW.cantidad * v_precio * v_promo.valor / 100, 2);
                ELSE
                    :NEW.descuento := LEAST(v_promo.valor * :NEW.cantidad, :NEW.cantidad * v_precio);
                END IF;
            END IF;
            SELECT porcentaje_comision INTO v_pct FROM PERSONAL WHERE id_personal = :NEW.id_personal;
            :NEW.porcentaje_comision := v_pct;
        END IF;

    ELSIF :NEW.tipo_item = 'PRODUCTO' THEN
        SELECT precio_venta, tipo_uso, estado INTO v_precio, v_tipo_uso, v_estado
          FROM PRODUCTO WHERE id_producto = :NEW.id_producto;
        IF v_tipo_uso <> 'VENTA' OR v_estado <> 'ACTIVO' THEN
            RAISE_APPLICATION_ERROR(-20048, 'El producto es de uso interno o está inactivo: no se vende (RN13).');
        END IF;
        -- RN15 (confirmada 18/10/2026): no se vende un producto sin stock en la sede, ni con autorización;
        -- primero almacén lo reabastece. Aviso temprano: la garantía final es el kardex al cerrar (-20030).
        SELECT NVL(MAX(stock_actual), 0) INTO v_n FROM INVENTARIO_SEDE
         WHERE id_producto = :NEW.id_producto AND id_sede = v_venta_sede;
        IF v_n < NVL(:NEW.cantidad, 1) THEN
            RAISE_APPLICATION_ERROR(-20035, 'Sin stock suficiente en la sede: hay ' || v_n || ' y se piden '
                || NVL(:NEW.cantidad, 1) || '. No se vende hasta que almacén lo reabastezca (RN15).');
        END IF;
        :NEW.precio_unitario := v_precio;
        :NEW.descuento       := 0;   -- las promociones aplican solo a servicios

    ELSE -- PAQUETE
        IF v_venta_cli IS NULL THEN
            RAISE_APPLICATION_ERROR(-20049, 'Un paquete o membresía se vende a una clienta registrada (RN36).');
        END IF;
        SELECT precio, estado INTO v_precio, v_estado FROM PAQUETE WHERE id_paquete = :NEW.id_paquete;
        IF v_estado <> 'ACTIVO' THEN
            RAISE_APPLICATION_ERROR(-20049, 'El paquete está inactivo.');
        END IF;
        :NEW.precio_unitario := v_precio;
        :NEW.descuento       := 0;
        :NEW.id_personal     := NULL;
    END IF;

    :NEW.subtotal       := :NEW.cantidad * :NEW.precio_unitario - :NEW.descuento;
    :NEW.monto_comision := ROUND(:NEW.subtotal * :NEW.porcentaje_comision / 100, 2);   -- RN38
END;
/

PROMPT Triggers de VENTA_PAGO, COMPROBANTE y NOTA_CREDITO...
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

-- [v4] El sistema emite el comprobante electrónico: la BD asigna serie, número y fecha; después
-- solo cambia el seguimiento del envío a SUNAT (estado, intentos, CDR, hash, rutas del XML y del CDR).
-- [v5] COMPROBANTE es el supertipo: BOLETA y FACTURA se emiten con la venta ABIERTA; NC_BOLETA y NC_FACTURA
-- anulan una venta CERRADA cuyo comprobante aceptó SUNAT. Las cuatro clases toman su número aquí.
-- La validación del documento del comprador la hace TRG_COMPROBANTE_COMPRADOR (script 06).
CREATE OR REPLACE TRIGGER TRG_COMPROBANTE_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON COMPROBANTE
FOR EACH ROW
FOLLOWS TRG_COMPROBANTE_INMUTABLE   -- orden fijo: primero la inmutabilidad del supertipo (script 06, -20096)
DECLARE
    v_estado   VENTA.estado%TYPE;
    v_sede     VENTA.id_sede%TYPE;
    v_id_venta NUMBER := CASE WHEN DELETING THEN :OLD.id_venta ELSE :NEW.id_venta END;
    v_serie    COMPROBANTE.serie%TYPE;
    v_numero   COMPROBANTE.numero%TYPE;
    v_orig     COMPROBANTE%ROWTYPE;
    v_es_nc    BOOLEAN;
BEGIN
    SELECT estado, id_sede INTO v_estado, v_sede FROM VENTA WHERE id_venta = v_id_venta;
    v_es_nc := (CASE WHEN DELETING THEN :OLD.tipo ELSE :NEW.tipo END) IN ('NC_BOLETA', 'NC_FACTURA');

    IF DELETING THEN
        IF v_es_nc THEN
            RAISE_APPLICATION_ERROR(-20020, 'Una nota de crédito no se modifica ni se elimina.');
        END IF;
        -- Solo se descarta un comprobante aún no enviado de una venta ABIERTA, y si es el último de su serie
        IF v_estado <> 'ABIERTA' OR :OLD.estado_sunat <> 'PENDIENTE' OR :OLD.intentos_envio > 0 THEN
            RAISE_APPLICATION_ERROR(-20073, 'El comprobante ' || :OLD.serie || '-' || :OLD.numero
                || ' ya se envió a SUNAT o su venta está ' || v_estado || ': no se elimina, se anula con nota de crédito (RN12).');
        END IF;
        PKG_VENTA.liberar_numero(:OLD.serie, :OLD.numero);
        RETURN;
    END IF;

    IF INSERTING THEN
        IF v_es_nc THEN
            -- La nota anula el comprobante de la misma clase de esa venta (NC_BOLETA -> BOLETA, NC_FACTURA -> FACTURA)
            IF v_estado <> 'CERRADA' THEN
                RAISE_APPLICATION_ERROR(-20058, 'Solo se anula una venta CERRADA (la venta está ' || v_estado || ').');
            END IF;
            BEGIN
                SELECT * INTO v_orig FROM COMPROBANTE
                 WHERE id_venta = :NEW.id_venta AND tipo = SUBSTR(:NEW.tipo, 4);
            EXCEPTION
                WHEN NO_DATA_FOUND THEN
                    RAISE_APPLICATION_ERROR(-20058, 'La venta no tiene ' || LOWER(SUBSTR(:NEW.tipo, 4))
                        || ': la nota de crédito debe referirse al comprobante de la misma venta.');
            END;
            -- [v4] SUNAT solo admite la nota de un comprobante que ya aceptó
            IF v_orig.estado_sunat NOT IN ('ACEPTADO', 'OBSERVADO') THEN
                RAISE_APPLICATION_ERROR(-20076, 'El comprobante está ' || v_orig.estado_sunat
                    || ' en SUNAT: la nota de crédito se emite cuando SUNAT lo acepta'
                    || CASE WHEN v_orig.estado_sunat = 'RECHAZADO' THEN ' (si fue RECHAZADO, la Administradora anula la venta sin nota).' ELSE '.' END);
            END IF;
            -- La nota lleva el mismo comprador que el comprobante que anula
            :NEW.id_tipo_doc_comprador := v_orig.id_tipo_doc_comprador;
            :NEW.nro_doc_comprador     := v_orig.nro_doc_comprador;
            :NEW.nombre_comprador      := v_orig.nombre_comprador;
        ELSIF v_estado <> 'ABIERTA' THEN
            RAISE_APPLICATION_ERROR(-20045, 'El comprobante de una venta ' || v_estado || ' no se modifica: se emite nota de crédito.');
        END IF;
        PKG_VENTA.asignar_numero(v_sede, :NEW.tipo, v_serie, v_numero);   -- RN47: correlativo sin saltos
        :NEW.serie                 := v_serie;
        :NEW.numero                := v_numero;
        :NEW.fecha_emision         := TRUNC(SYSDATE);
        :NEW.estado_sunat          := 'PENDIENTE';
        :NEW.intentos_envio        := 0;
        :NEW.fecha_envio           := NULL;
        :NEW.fecha_respuesta       := NULL;
        :NEW.codigo_respuesta      := NULL;
        :NEW.descripcion_respuesta := NULL;
        RETURN;
    END IF;

    -- UPDATING (TRG_COMPROBANTE_INMUTABLE del script 06 también protege tipo, venta, serie y número)
    IF PKG_SESION.distinto(:OLD.id_venta, :NEW.id_venta) OR PKG_SESION.distinto(:OLD.tipo, :NEW.tipo)
       OR PKG_SESION.distinto(:OLD.serie, :NEW.serie) OR PKG_SESION.distinto(:OLD.numero, :NEW.numero)
       OR PKG_SESION.distinto(:OLD.fecha_emision, :NEW.fecha_emision) THEN
        RAISE_APPLICATION_ERROR(-20074, 'El tipo, la serie, el número y la fecha del comprobante los asigna la BD y no se cambian;'
            || ' si hay un error, anule la venta con nota de crédito (RN12).');
    END IF;
    IF PKG_SESION.distinto(:OLD.id_tipo_doc_comprador, :NEW.id_tipo_doc_comprador)
       OR PKG_SESION.distinto(:OLD.nro_doc_comprador, :NEW.nro_doc_comprador)
       OR PKG_SESION.distinto(:OLD.nombre_comprador, :NEW.nombre_comprador) THEN
        IF v_es_nc OR v_estado <> 'ABIERTA' OR :OLD.estado_sunat <> 'PENDIENTE' OR :OLD.intentos_envio > 0 THEN
            RAISE_APPLICATION_ERROR(-20074, 'Los datos del comprador no se cambian después de cerrar la venta o de enviar el comprobante a SUNAT.');
        END IF;
    END IF;
    PKG_VENTA.validar_estado_sunat(:OLD.estado_sunat, :NEW.estado_sunat, :OLD.intentos_envio, :NEW.intentos_envio,
                                   CASE WHEN v_es_nc THEN 'la nota de crédito ' ELSE 'el comprobante ' END
                                   || :OLD.serie || '-' || :OLD.numero);
END;
/

-- [v5] Subtipo NOTA_CREDITO: motivo, forma de devolución y quién la pide y la autoriza.
-- Se inserta en la misma transacción, después de su fila NC_* en COMPROBANTE (id_nota_credito = id_comprobante).
CREATE OR REPLACE TRIGGER TRG_NOTA_CREDITO_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON NOTA_CREDITO
FOR EACH ROW
DECLARE
    v_estado  VENTA.estado%TYPE;
    v_cliente VENTA.id_cliente%TYPE;
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
    :NEW.id_usuario_solicita := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_solicita);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20094, 'Primero registre el comprobante NC_BOLETA o NC_FACTURA (id_comprobante = id_nota_credito).');
END;
/

-- =====================================================================
-- 9. Caja (RN30, RF62-RF67)
-- =====================================================================
PROMPT Triggers de CAJA...
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
        RETURN;
    END IF;

    IF :OLD.estado = 'CERRADA' THEN
        IF :NEW.estado <> 'CERRADA' THEN
            RAISE_APPLICATION_ERROR(-20060, 'Una caja cerrada no se reabre (RN30).');
        END IF;
        IF PKG_SESION.distinto(:OLD.monto_inicial, :NEW.monto_inicial)
           OR PKG_SESION.distinto(:OLD.fecha_cierre, :NEW.fecha_cierre)
           OR PKG_SESION.distinto(:OLD.id_usuario_cierre, :NEW.id_usuario_cierre) THEN
            RAISE_APPLICATION_ERROR(-20060, 'Una caja cerrada no se modifica; solo la revisa la Administradora (RF67).');
        END IF;
        IF PKG_SESION.distinto(:OLD.id_usuario_revisa, :NEW.id_usuario_revisa) THEN
            PKG_SESION.exigir_administradora('aprobar cierres de caja con diferencia (RF67)');
            :NEW.id_usuario_revisa := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_revisa);
            :NEW.fecha_revision    := SYSTIMESTAMP;
        END IF;
        RETURN;
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
        -- El arqueo debe estar al día: nada se movió después de registrarlo
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

CREATE OR REPLACE TRIGGER TRG_CAJA_ARQUEO_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON CAJA_ARQUEO
FOR EACH ROW
DECLARE
    v_estado  CAJA.estado%TYPE;
    v_inicial CAJA.monto_inicial%TYPE;
    v_id_caja NUMBER := CASE WHEN DELETING THEN :OLD.id_caja ELSE :NEW.id_caja END;
BEGIN
    SELECT estado, monto_inicial INTO v_estado, v_inicial FROM CAJA WHERE id_caja = v_id_caja;
    IF v_estado <> 'ABIERTA' THEN
        RAISE_APPLICATION_ERROR(-20062, 'El arqueo se registra o corrige antes de cerrar la caja.');
    END IF;
    IF NOT DELETING THEN
        :NEW.monto_esperado := PKG_CAJA.esperado(:NEW.id_caja, :NEW.id_medio_pago, v_inicial);
        :NEW.diferencia     := :NEW.monto_contado - :NEW.monto_esperado;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_CAJA_EGRESO_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON CAJA_EGRESO
FOR EACH ROW
DECLARE
    v_estado  CAJA.estado%TYPE;
    v_id_caja NUMBER := CASE WHEN DELETING THEN :OLD.id_caja ELSE :NEW.id_caja END;
BEGIN
    SELECT estado INTO v_estado FROM CAJA WHERE id_caja = v_id_caja;
    IF v_estado <> 'ABIERTA' THEN
        RAISE_APPLICATION_ERROR(-20060, 'Los egresos se registran en una caja ABIERTA.');
    END IF;
    IF INSERTING THEN
        :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
        :NEW.fecha      := SYSTIMESTAMP;
    END IF;
END;
/

-- =====================================================================
-- 10. Inventario y compras (RN13-RN19)
-- =====================================================================
PROMPT Triggers de inventario...
CREATE OR REPLACE TRIGGER TRG_PRODUCTO_REGLAS
BEFORE INSERT OR UPDATE ON PRODUCTO
FOR EACH ROW
BEGIN
    -- [v5] PRODUCTO ya no tiene stock: lo controla TRG_INVENTARIO_SEDE_REGLAS
    IF INSERTING THEN
        RETURN;
    END IF;
    IF PKG_SESION.distinto(:OLD.costo, :NEW.costo)
       OR PKG_SESION.distinto(:OLD.precio_venta, :NEW.precio_venta)
       OR PKG_SESION.distinto(:OLD.es_premium, :NEW.es_premium) THEN
        PKG_SESION.exigir_administradora('cambiar costos y precios de productos (RN24, RF87)');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_INVENTARIO_SEDE_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON INVENTARIO_SEDE
FOR EACH ROW
BEGIN
    IF DELETING THEN
        IF :OLD.stock_actual <> 0 THEN
            RAISE_APPLICATION_ERROR(-20032, 'No se elimina un inventario con stock; llévelo a cero con un ajuste.');
        END IF;
        RETURN;
    END IF;
    IF INSERTING THEN
        :NEW.stock_actual := 0;
    ELSIF PKG_SESION.distinto(:OLD.stock_actual, :NEW.stock_actual) AND NOT PKG_INVENTARIO.desde_kardex THEN
        RAISE_APPLICATION_ERROR(-20032, 'El stock solo cambia con un movimiento de kardex (RN14, RN18).');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_KARDEX_APLICAR
BEFORE INSERT ON MOVIMIENTO_KARDEX
FOR EACH ROW
DECLARE
    v_rol   VARCHAR2(30) := PKG_SESION.rol_actual;
    v_delta NUMBER;
    v_saldo NUMBER;
BEGIN
    -- RN17: solo almacén y administración mueven inventario (la salida por venta la genera el cierre de la venta)
    IF v_rol IS NOT NULL AND v_rol NOT IN ('ENCARGADA_ALMACEN', 'ADMINISTRADORA') AND :NEW.id_detalle_venta IS NULL THEN
        RAISE_APPLICATION_ERROR(-20031, 'Solo la encargada de almacén o la Administradora registran movimientos de inventario (RN17).');
    END IF;
    :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
    :NEW.fecha_hora := SYSTIMESTAMP;
    v_delta := CASE :NEW.tipo WHEN 'SALIDA' THEN -:NEW.cantidad ELSE :NEW.cantidad END;
    PKG_INVENTARIO.aplicar_movimiento(:NEW.id_producto, :NEW.id_sede, v_delta, v_saldo);
    :NEW.saldo := v_saldo;   -- RN18: saldo después del movimiento
END;
/

CREATE OR REPLACE TRIGGER TRG_COMPRA_REGLAS
BEFORE INSERT OR UPDATE ON COMPRA
FOR EACH ROW
DECLARE
    v_estado REQUERIMIENTO_COMPRA.estado%TYPE;
    v_sede   REQUERIMIENTO_COMPRA.id_sede%TYPE;
BEGIN
    IF :NEW.id_requerimiento IS NOT NULL
       AND (INSERTING OR PKG_SESION.distinto(:OLD.id_requerimiento, :NEW.id_requerimiento)) THEN
        SELECT estado, id_sede INTO v_estado, v_sede FROM REQUERIMIENTO_COMPRA WHERE id_requerimiento = :NEW.id_requerimiento;
        IF v_estado NOT IN ('APROBADO', 'ATENDIDO') THEN
            RAISE_APPLICATION_ERROR(-20033, 'Solo se compra un requerimiento APROBADO por la Administradora (RN19).');
        END IF;
        IF v_sede <> :NEW.id_sede THEN
            RAISE_APPLICATION_ERROR(-20033, 'La compra entra a la sede que hizo el requerimiento.');
        END IF;
    END IF;
    IF INSERTING THEN
        :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_COMPRA_DETALLE_KARDEX
AFTER INSERT ON COMPRA_DETALLE
FOR EACH ROW
DECLARE
    v_sede      COMPRA.id_sede%TYPE;
    v_usuario   COMPRA.id_usuario%TYPE;
    v_documento COMPRA.documento%TYPE;
BEGIN
    -- RF43: la compra suma las unidades al inventario de su sede
    SELECT id_sede, id_usuario, documento INTO v_sede, v_usuario, v_documento FROM COMPRA WHERE id_compra = :NEW.id_compra;
    INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, fecha_hora, id_usuario, id_detalle_compra)
    VALUES (:NEW.id_producto, v_sede, 'ENTRADA', 'Compra ' || v_documento, :NEW.cantidad_recibida, 0, SYSTIMESTAMP,
            NVL(PKG_SESION.usuario_actual, v_usuario), :NEW.id_detalle_compra);
END;
/

CREATE OR REPLACE TRIGGER TRG_REQ_COMPRA_REGLAS
BEFORE INSERT OR UPDATE ON REQUERIMIENTO_COMPRA
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        IF :NEW.estado <> 'PENDIENTE' THEN
            RAISE_APPLICATION_ERROR(-20034, 'Un requerimiento nace PENDIENTE.');
        END IF;
        :NEW.id_usuario_solicita := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_solicita);
        :NEW.fecha_solicitud     := SYSTIMESTAMP;
        RETURN;
    END IF;
    IF PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN
        IF NOT ((:OLD.estado = 'PENDIENTE' AND :NEW.estado IN ('APROBADO', 'RECHAZADO'))
             OR (:OLD.estado = 'APROBADO'  AND :NEW.estado = 'ATENDIDO')) THEN
            RAISE_APPLICATION_ERROR(-20034, 'Cambio de estado no permitido: ' || :OLD.estado || ' -> ' || :NEW.estado || '.');
        END IF;
        IF :NEW.estado IN ('APROBADO', 'RECHAZADO') THEN
            PKG_SESION.exigir_administradora('aprobar o rechazar requerimientos de compra (RN19, RF42)');
            :NEW.id_usuario_aprueba := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_aprueba);
            :NEW.fecha_resolucion   := SYSTIMESTAMP;
        END IF;
    END IF;
END;
/

-- [v5] REQUERIMIENTO_DETALLE (antes SOLICITUD_REPOSICION) no tiene aprobación propia:
-- la Administradora aprueba el requerimiento completo y el detalle ya no cambia.
CREATE OR REPLACE TRIGGER TRG_REQ_DETALLE_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON REQUERIMIENTO_DETALLE
FOR EACH ROW
DECLARE
    v_estado REQUERIMIENTO_COMPRA.estado%TYPE;
    -- DELETING es un booleano de PL/SQL: se resuelve aquí, no dentro del SELECT (ORA-00920)
    v_id     NUMBER := CASE WHEN DELETING THEN :OLD.id_requerimiento ELSE :NEW.id_requerimiento END;
BEGIN
    SELECT estado INTO v_estado FROM REQUERIMIENTO_COMPRA WHERE id_requerimiento = v_id;
    IF v_estado <> 'PENDIENTE' THEN
        RAISE_APPLICATION_ERROR(-20034, 'El requerimiento está ' || v_estado || ': su detalle ya no se modifica (RN19).');
    END IF;
END;
/

-- =====================================================================
-- 13. [v4] Integraciones: facturación electrónica (SUNAT), pasarela de pagos y consulta DNI/RUC
--     Cambio de alcance pedido por la dueña el 21/10/2026.
-- =====================================================================
PROMPT [v4] Trigger de SERIE_COMPROBANTE...
CREATE OR REPLACE TRIGGER TRG_SERIE_COMPR_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON SERIE_COMPROBANTE
FOR EACH ROW
BEGIN
    IF INSERTING OR DELETING THEN
        PKG_SESION.exigir_administradora('configurar las series de los comprobantes electrónicos (RF107)');
        IF DELETING AND :OLD.ultimo_numero > 0 THEN
            RAISE_APPLICATION_ERROR(-20079, 'La serie ' || :OLD.serie || ' ya se usó: no se elimina, se desactiva.');
        END IF;
        RETURN;
    END IF;
    IF PKG_SESION.distinto(:OLD.id_sede, :NEW.id_sede) OR PKG_SESION.distinto(:OLD.tipo, :NEW.tipo)
       OR PKG_SESION.distinto(:OLD.serie, :NEW.serie) THEN
        IF :OLD.ultimo_numero > 0 THEN
            RAISE_APPLICATION_ERROR(-20079, 'La serie ' || :OLD.serie || ' ya se usó: no cambia de sede, de tipo ni de letras; cree otra serie.');
        END IF;
        PKG_SESION.exigir_administradora('configurar las series de los comprobantes electrónicos (RF107)');
    END IF;
    IF PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN
        PKG_SESION.exigir_administradora('activar o desactivar series de comprobantes (RF107)');
    END IF;
    -- El correlativo solo lo mueve PKG_VENTA al emitir (RN47); ajustarlo a mano es de la Administradora.
    IF PKG_SESION.distinto(:OLD.ultimo_numero, :NEW.ultimo_numero) AND NOT PKG_VENTA.g_numerando THEN
        PKG_SESION.exigir_administradora('ajustar el correlativo de una serie (RF107)');
    END IF;
END;
/

PROMPT [v4] Trigger de TRANSACCION_PASARELA...
CREATE OR REPLACE TRIGGER TRG_TRANS_PAS_REGLAS
FOR INSERT OR UPDATE OR DELETE ON TRANSACCION_PASARELA
COMPOUND TRIGGER
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    g_origen t_ids;   -- cobros que reciben una devolución en esta sentencia
    g_medio  t_ids;   -- medio de cada devolución

    BEFORE EACH ROW IS
        v_medio  MEDIO_PAGO%ROWTYPE;
        v_cita   CITA%ROWTYPE;
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

PROMPT [v4] Trigger del adelanto de CITA (efectivo en caja, digital por la pasarela)...
CREATE OR REPLACE TRIGGER TRG_CITA_ADELANTO
BEFORE INSERT OR UPDATE ON CITA
FOR EACH ROW
DECLARE
    v_medio MEDIO_PAGO%ROWTYPE;
    v_n     NUMBER;
BEGIN
    IF :NEW.adelanto_monto > 0 AND :NEW.id_cita_origen IS NULL
       AND (INSERTING OR PKG_SESION.distinto(:OLD.adelanto_monto, :NEW.adelanto_monto)
                      OR PKG_SESION.distinto(:OLD.id_medio_pago, :NEW.id_medio_pago)
                      OR PKG_SESION.distinto(:OLD.id_caja, :NEW.id_caja)) THEN
        SELECT * INTO v_medio FROM MEDIO_PAGO WHERE id_medio_pago = :NEW.id_medio_pago;
        IF v_medio.usa_pasarela = 'N' AND :NEW.id_caja IS NULL THEN
            RAISE_APPLICATION_ERROR(-20070, 'Un adelanto en efectivo se registra en la caja ABIERTA de la sede (RN04, RN30).');
        END IF;
        IF v_medio.usa_pasarela = 'S' THEN
            IF INSERTING THEN
                RAISE_APPLICATION_ERROR(-20070, 'Un adelanto con ' || v_medio.nombre || ' se registra cuando la pasarela confirma el cobro:'
                    || ' primero agende la cita, luego cobre el adelanto (enlace de pago o QR) a nombre de la cita (RN04, RF104).');
            END IF;
            SELECT COUNT(*) INTO v_n
              FROM TRANSACCION_PASARELA
             WHERE id_cita = :NEW.id_cita
               AND tipo_operacion = 'COBRO'
               AND estado = 'APROBADA'
               AND id_medio_pago = :NEW.id_medio_pago
               AND monto = :NEW.adelanto_monto;
            IF v_n = 0 THEN
                RAISE_APPLICATION_ERROR(-20070, 'El adelanto de S/ ' || :NEW.adelanto_monto || ' con ' || v_medio.nombre
                    || ' no tiene un cobro APROBADO por la pasarela para esta cita (RN04, RN42).');
            END IF;
        END IF;
    END IF;
END;
/

-- [v5] Los triggers TRG_CONSULTA_DOC_REGLAS y TRG_CLIENTE/PERSONAL/PROVEEDOR_VERIFICADO se retiran:
-- el documento y su verificación viven en PERSONA y los controla TRG_PERSONA_REGLAS (script 06).

PROMPT Triggers de reglas creados.
