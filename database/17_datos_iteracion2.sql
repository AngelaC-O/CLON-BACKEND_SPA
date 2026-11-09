-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle · VERSIÓN 5.1
-- Script 17 · Migración v5 -> v5.1 y datos de la Iteración 2 (08/11/2026)
-- Se ejecuta como: dueño del esquema (modo mantenimiento), después de 15_datos_iteracion1.sql.
-- Orden completo (ver README.md): 00 -> 14 -> 13 -> 15 -> 17 -> 16.
-- Es la única migración: los scripts 01 a 15 siguen describiendo el modelo v5 y no se editan.
--
-- Se puede repetir sin error: cada paso revisa user_tables, user_tab_columns, user_constraints,
-- user_indexes o user_triggers antes de crear, y si el objeto ya existe no hace nada. Los triggers
-- se crean con CREATE OR REPLACE y los GRANT se pueden repetir. Ante cualquier error se detiene.
--
-- Definición exacta de cada objeto: docs/Cautiva_Spa_Diccionario_de_Datos_v5_1.xlsx
-- (hojas Diccionario, Restricciones y Cambios v5.1, cambios 1 a 18).
--
--   0) Paso previo: si alguna VENTA tiene id_usuario_autoriza, se detiene sin tocar nada.
--   a) RECURSO: limpieza de 0 a 10 minutos, DEFAULT 10 (RN03, cambio 1).
--   b) VENTA: id_usuario_autoriza obsoleta, CK_VENTA_AUTORIZA_OBSOLETA (cambio 2; como COMPRA.id_solicitud, O-04).
--   c) CAJA: datos de la reapertura (RN30, RF66, cambios 4 a 6 y 9).
--   d) SOLICITUD_ANULACION (RN32, cambio 7).
--   e) CONSUMO_PENDIENTE (RN53, cambio 8).
--      Descripciones nuevas de PRODUCTO.unidad, SERVICIO_INSUMO.cantidad_estimada y
--      MOVIMIENTO_KARDEX.id_atencion (RN54, cambio 3).
--   f) Triggers (cambios 11 a 16): TRG_CAJA_REGLAS, TRG_KARDEX_APLICAR y TRG_VENTA_REGLAS (parten del
--      código de 09 y cambian solo en lo que dice la hoja); TRG_SOLICITUD_ANULACION_REGLAS,
--      TRG_CONSUMO_PENDIENTE_REGLAS, TRG_AUD_SOLICITUD_ANULACION y TRG_AUD_CONSUMO_PENDIENTE (nuevos).
--   g) Permisos de ROL_CAUTIVA_APLICACION sobre las dos tablas nuevas (como 13).
--   h) Datos: PARAMETRO (cambios 17 y 18), permisos de RECEPCIONISTA, usuaria de almacén de prueba y
--      revisión de los insumos en su unidad mínima (RN54).
--   i) Resumen: 57 tablas, 131 FK, triggers y objetos inválidos.
--
-- Códigos de error nuevos (el mayor usado en 06, 08 y 09 era -20099):
--   -20100  VENTA.id_usuario_autoriza es obsoleta (TRG_VENTA_REGLAS y paso previo).
--   -20101  La solicitud de anulación es de una venta CERRADA (TRG_SOLICITUD_ANULACION_REGLAS).
--   -20102  Una solicitud nace PENDIENTE, no cambia de venta y, resuelta, no cambia.
--   -20103  Un consumo pendiente nace PENDIENTE y, resuelto, no cambia (TRG_CONSUMO_PENDIENTE_REGLAS).
--   -20104  Solo almacén o la Administradora regularizan o descartan un consumo pendiente.
--   -20105  La regularización exige una SALIDA del mismo producto, sede y atención.
--   -20106  VENTAS:EDITAR quedó en un rol distinto de ADMINISTRADORA (comprobación de los datos, paso h).
-- Se reutilizan: -20003 (solo la Administradora: reabrir la caja, resolver una solicitud), -20031 (RN17),
-- -20042 (la venta entra a una caja ABIERTA de su sede) y -20060 (reglas de estado de la caja).
--
-- Usuaria de prueba "almacen" (ENCARGADA_ALMACEN, sede Pichanaki Centro): la misma clave temporal que
-- escribe la cabecera de 15_datos_iteracion1.sql (mismo hash BCrypt $2a$, costo 10) y
-- debe_cambiar_clave = 'S'. Solo local y pruebas.
-- =============================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- =============================================================================
-- 0) Paso previo: VENTA.id_usuario_autoriza
-- =============================================================================
PROMPT 0) Revisando VENTA.id_usuario_autoriza...
DECLARE
    v_n  NUMBER;
    v_ck NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_ck FROM user_constraints WHERE constraint_name = 'CK_VENTA_AUTORIZA_OBSOLETA';
    SELECT COUNT(*) INTO v_n FROM VENTA WHERE id_usuario_autoriza IS NOT NULL;
    IF v_ck = 0 AND v_n > 0 THEN
        RAISE_APPLICATION_ERROR(-20100, 'Hay ' || v_n || ' venta(s) con VENTA.id_usuario_autoriza informado. '
            || 'El script 17 no las modifica y se detiene: anótelo en docs/NOTAS_IT2.md y decida qué hacer con ellas '
            || 'antes de volver a correrlo.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('   Ninguna venta tiene id_usuario_autoriza: se puede migrar.');
END;
/

-- =============================================================================
-- a) a e) Estructura
-- =============================================================================
PROMPT a) a e) Estructura del modelo v5.1...
DECLARE
    v_n   NUMBER;
    v_txt VARCHAR2(4000);

    PROCEDURE ejecutar(p_sql IN VARCHAR2, p_aviso IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
        DBMS_OUTPUT.PUT_LINE('   + ' || p_aviso);
    END ejecutar;

    FUNCTION hay_tabla(p_tabla IN VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v FROM user_tables WHERE table_name = p_tabla;
        RETURN v > 0;
    END hay_tabla;

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

    PROCEDURE columna(p_tabla IN VARCHAR2, p_columna IN VARCHAR2, p_tipo IN VARCHAR2) IS
    BEGIN
        IF NOT hay_columna(p_tabla, UPPER(p_columna)) THEN
            ejecutar('ALTER TABLE ' || p_tabla || ' ADD (' || p_columna || ' ' || p_tipo || ')', p_tabla || '.' || p_columna);
        END IF;
    END columna;

    PROCEDURE restriccion(p_tabla IN VARCHAR2, p_nombre IN VARCHAR2, p_definicion IN VARCHAR2) IS
    BEGIN
        IF NOT hay_restriccion(p_nombre) THEN
            ejecutar('ALTER TABLE ' || p_tabla || ' ADD CONSTRAINT ' || p_nombre || ' ' || p_definicion, p_nombre);
        END IF;
    END restriccion;

    PROCEDURE indice(p_nombre IN VARCHAR2, p_sql IN VARCHAR2) IS
    BEGIN
        IF NOT hay_indice(p_nombre) THEN
            ejecutar(p_sql, p_nombre);
        END IF;
    END indice;

    PROCEDURE comentario(p_sql IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;   -- COMMENT se puede repetir
    END comentario;
BEGIN
    -- -------------------------------------------------------------------------
    -- a) RECURSO: la limpieza dura como máximo 10 minutos (RN03, cambio 1)
    -- -------------------------------------------------------------------------
    UPDATE RECURSO SET minutos_limpieza = 10 WHERE minutos_limpieza > 10;
    DBMS_OUTPUT.PUT_LINE('   a) RECURSO: ' || SQL%ROWCOUNT || ' recurso(s) con más de 10 minutos de limpieza bajaron a 10.');
    COMMIT;
    SELECT MAX(search_condition_vc) INTO v_txt FROM user_constraints WHERE constraint_name = 'CK_RECURSO_MINUTOS_LIMPIEZA';
    IF v_txt IS NULL OR UPPER(REPLACE(v_txt, ' ')) <> 'MINUTOS_LIMPIEZABETWEEN0AND10' THEN
        IF v_txt IS NOT NULL THEN
            ejecutar('ALTER TABLE RECURSO DROP CONSTRAINT CK_RECURSO_MINUTOS_LIMPIEZA', 'CK_RECURSO_MINUTOS_LIMPIEZA anterior retirada (0 a 30)');
        END IF;
        ejecutar('ALTER TABLE RECURSO ADD CONSTRAINT CK_RECURSO_MINUTOS_LIMPIEZA CHECK (minutos_limpieza BETWEEN 0 AND 10)',
                 'CK_RECURSO_MINUTOS_LIMPIEZA (0 a 10)');
    END IF;
    SELECT data_default INTO v_txt FROM user_tab_columns WHERE table_name = 'RECURSO' AND column_name = 'MINUTOS_LIMPIEZA';
    IF NVL(REGEXP_REPLACE(v_txt, '[[:space:]]'), '-') <> '10' THEN
        ejecutar('ALTER TABLE RECURSO MODIFY (minutos_limpieza DEFAULT 10)', 'RECURSO.minutos_limpieza DEFAULT 10');
    END IF;
    comentario(q'[COMMENT ON COLUMN RECURSO.minutos_limpieza IS 'Minutos que el recurso queda bloqueado para limpieza después de cada servicio (máximo 10).']');

    -- -------------------------------------------------------------------------
    -- b) VENTA.id_usuario_autoriza obsoleta (cambio 2). La columna se queda, como COMPRA.id_solicitud (O-04).
    -- -------------------------------------------------------------------------
    restriccion('VENTA', 'CK_VENTA_AUTORIZA_OBSOLETA', 'CHECK (id_usuario_autoriza IS NULL)');
    comentario(q'[COMMENT ON COLUMN VENTA.id_usuario_autoriza IS 'Obsoleta desde v5.1: siempre nula (CK_VENTA_AUTORIZA_OBSOLETA). La venta después del cierre se registra reabriendo la caja (CAJA.id_usuario_reapertura). Se retira en la próxima versión del esquema.']');

    -- -------------------------------------------------------------------------
    -- c) CAJA: reapertura por la Administradora (RN30, RF66, cambios 4 a 6 y 9)
    -- -------------------------------------------------------------------------
    columna('CAJA', 'id_usuario_reapertura', 'NUMBER');
    columna('CAJA', 'fecha_reapertura', 'TIMESTAMP');
    columna('CAJA', 'motivo_reapertura', 'VARCHAR2(300)');
    restriccion('CAJA', 'CK_CAJA_REAPERTURA',
        'CHECK ((id_usuario_reapertura IS NULL AND fecha_reapertura IS NULL AND motivo_reapertura IS NULL) '
        || 'OR (id_usuario_reapertura IS NOT NULL AND fecha_reapertura IS NOT NULL AND motivo_reapertura IS NOT NULL))');
    restriccion('CAJA', 'FK_CAJA_USUARIO_REAPERTURA', 'FOREIGN KEY (id_usuario_reapertura) REFERENCES USUARIO (id_usuario)');
    indice('IX_CAJA_USU_REA', 'CREATE INDEX IX_CAJA_USU_REA ON CAJA (id_usuario_reapertura)');
    comentario(q'[COMMENT ON TABLE CAJA IS 'Caja diaria por sede. El arqueo por medio de pago (incluido el efectivo) está en CAJA_ARQUEO. La Administradora puede reabrirla para registrar una venta después del cierre (RF66).']');
    comentario(q'[COMMENT ON COLUMN CAJA.id_usuario_reapertura IS 'Administradora que reabrió la caja cerrada para registrar una venta (la última vez; la historia queda en AUDITORIA). (RF66, RN30)']');
    comentario(q'[COMMENT ON COLUMN CAJA.fecha_reapertura IS 'Fecha de la última reapertura. (RF66)']');
    comentario(q'[COMMENT ON COLUMN CAJA.motivo_reapertura IS 'Motivo de la última reapertura. (RN30)']');

    -- -------------------------------------------------------------------------
    -- d) SOLICITUD_ANULACION · Transaccional (RN32, RF32, RF33, cambio 7)
    -- -------------------------------------------------------------------------
    IF NOT hay_tabla('SOLICITUD_ANULACION') THEN
        ejecutar(q'[CREATE TABLE SOLICITUD_ANULACION (
    id_solicitud         NUMBER GENERATED BY DEFAULT AS IDENTITY,
    id_venta             NUMBER NOT NULL,
    motivo               VARCHAR2(200) NOT NULL,
    codigo_motivo        VARCHAR2(2) DEFAULT '01' NOT NULL,
    forma_devolucion     VARCHAR2(15) DEFAULT 'MEDIO_ORIGINAL' NOT NULL,
    id_usuario_solicita  NUMBER NOT NULL,
    fecha_solicitud      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    estado               VARCHAR2(10) DEFAULT 'PENDIENTE' NOT NULL,
    id_usuario_resuelve  NUMBER,
    fecha_resolucion     TIMESTAMP,
    motivo_rechazo       VARCHAR2(300),
    id_nota_credito      NUMBER,
    CONSTRAINT PK_SOLICITUD_ANULACION PRIMARY KEY (id_solicitud)
)]', 'tabla SOLICITUD_ANULACION');
    END IF;
    restriccion('SOLICITUD_ANULACION', 'UQ_SOL_ANUL_NOTA_CREDITO', 'UNIQUE (id_nota_credito)');
    restriccion('SOLICITUD_ANULACION', 'CK_SOL_ANUL_COD_MOTIVO', q'[CHECK (codigo_motivo IN ('01', '02', '06'))]');
    restriccion('SOLICITUD_ANULACION', 'CK_SOL_ANUL_DEVOLUCION', q'[CHECK (forma_devolucion IN ('MEDIO_ORIGINAL', 'SALDO_FAVOR'))]');
    restriccion('SOLICITUD_ANULACION', 'CK_SOL_ANUL_ESTADO', q'[CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))]');
    restriccion('SOLICITUD_ANULACION', 'CK_SOL_ANUL_RESOLUCION',
        q'[CHECK (estado = 'PENDIENTE' OR (id_usuario_resuelve IS NOT NULL AND fecha_resolucion IS NOT NULL))]');
    restriccion('SOLICITUD_ANULACION', 'CK_SOL_ANUL_RECHAZO', q'[CHECK (estado <> 'RECHAZADA' OR motivo_rechazo IS NOT NULL)]');
    restriccion('SOLICITUD_ANULACION', 'CK_SOL_ANUL_NOTA_CREDITO', q'[CHECK (estado = 'APROBADA' OR id_nota_credito IS NULL)]');
    restriccion('SOLICITUD_ANULACION', 'FK_SOL_ANUL_VENTA', 'FOREIGN KEY (id_venta) REFERENCES VENTA (id_venta)');
    restriccion('SOLICITUD_ANULACION', 'FK_SOL_ANUL_USU_SOLICITA', 'FOREIGN KEY (id_usuario_solicita) REFERENCES USUARIO (id_usuario)');
    restriccion('SOLICITUD_ANULACION', 'FK_SOL_ANUL_USU_RESUELVE', 'FOREIGN KEY (id_usuario_resuelve) REFERENCES USUARIO (id_usuario)');
    restriccion('SOLICITUD_ANULACION', 'FK_SOL_ANUL_NOTA_CREDITO', 'FOREIGN KEY (id_nota_credito) REFERENCES NOTA_CREDITO (id_nota_credito)');
    -- Una sola solicitud PENDIENTE por venta (RN32)
    indice('UQ_SOL_ANUL_PENDIENTE',
        q'[CREATE UNIQUE INDEX UQ_SOL_ANUL_PENDIENTE ON SOLICITUD_ANULACION (CASE WHEN estado = 'PENDIENTE' THEN id_venta END)]');
    -- Índices de las FK (índice de FK_SOL_ANUL_NOTA_CREDITO: ya lo cubre UQ_SOL_ANUL_NOTA_CREDITO)
    indice('IX_SOL_ANUL_VENTA', 'CREATE INDEX IX_SOL_ANUL_VENTA ON SOLICITUD_ANULACION (id_venta)');
    indice('IX_SOL_ANUL_USU_SOLICITA', 'CREATE INDEX IX_SOL_ANUL_USU_SOLICITA ON SOLICITUD_ANULACION (id_usuario_solicita)');
    indice('IX_SOL_ANUL_USU_RESUELVE', 'CREATE INDEX IX_SOL_ANUL_USU_RESUELVE ON SOLICITUD_ANULACION (id_usuario_resuelve)');
    comentario(q'[COMMENT ON TABLE SOLICITUD_ANULACION IS 'Pedido de anulación de una venta cerrada: lo registra la recepcionista (CUS22) y la Administradora lo aprueba, con lo que se emite la nota de crédito y se anula la venta, o lo rechaza con su motivo (CUS23). Conserva la historia de las solicitudes. (RN32, RF32, RF33)']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.id_solicitud IS 'Identificador de la solicitud.']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.id_venta IS 'Venta CERRADA que se pide anular. (RN32)']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.motivo IS 'Motivo de la anulación; pasa a NOTA_CREDITO.motivo al aprobarla. (RN32)']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.codigo_motivo IS 'Motivo según el catálogo 09 de SUNAT: 01 anulación de la operación, 02 error en el RUC, 06 devolución total.']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.forma_devolucion IS 'Cómo se devuelve el dinero si se aprueba. (RN44, RF106)']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.id_usuario_solicita IS 'Quien pide la anulación (la recepcionista o la Administradora). (RN32)']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.fecha_solicitud IS 'Fecha y hora de la solicitud.']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.estado IS 'Estado. Solo una solicitud PENDIENTE por venta. (RN32)']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.id_usuario_resuelve IS 'Administradora que aprueba o rechaza. (RN32, RES16)']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.fecha_resolucion IS 'Fecha de la aprobación o del rechazo.']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.motivo_rechazo IS 'Motivo del rechazo; obligatorio si se rechaza.']');
    comentario(q'[COMMENT ON COLUMN SOLICITUD_ANULACION.id_nota_credito IS 'Nota de crédito emitida al aprobar (nula si SUNAT había rechazado el comprobante y se anula sin nota). (RN12)']');

    -- -------------------------------------------------------------------------
    -- e) CONSUMO_PENDIENTE · Transaccional (RN53, RF46, cambio 8)
    -- -------------------------------------------------------------------------
    IF NOT hay_tabla('CONSUMO_PENDIENTE') THEN
        ejecutar(q'[CREATE TABLE CONSUMO_PENDIENTE (
    id_consumo           NUMBER GENERATED BY DEFAULT AS IDENTITY,
    id_atencion          NUMBER NOT NULL,
    id_producto          NUMBER NOT NULL,
    id_sede              NUMBER NOT NULL,
    cantidad_estimada    NUMBER(10) NOT NULL,
    cantidad_descontada  NUMBER(10) NOT NULL,
    cantidad_pendiente   NUMBER(10) NOT NULL,
    estado               VARCHAR2(12) DEFAULT 'PENDIENTE' NOT NULL,
    fecha_registro       TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    id_usuario_resuelve  NUMBER,
    fecha_resolucion     TIMESTAMP,
    id_movimiento        NUMBER,
    observacion          VARCHAR2(300),
    CONSTRAINT PK_CONSUMO_PENDIENTE PRIMARY KEY (id_consumo)
)]', 'tabla CONSUMO_PENDIENTE');
    END IF;
    restriccion('CONSUMO_PENDIENTE', 'UQ_CONSUMO_PEND_ATEN_PROD', 'UNIQUE (id_atencion, id_producto)');
    restriccion('CONSUMO_PENDIENTE', 'CK_CONSUMO_PEND_ESTIMADA', 'CHECK (cantidad_estimada > 0)');
    restriccion('CONSUMO_PENDIENTE', 'CK_CONSUMO_PEND_DESCONTADA', 'CHECK (cantidad_descontada >= 0)');
    restriccion('CONSUMO_PENDIENTE', 'CK_CONSUMO_PEND_CANTIDADES',
        'CHECK (cantidad_pendiente = cantidad_estimada - cantidad_descontada AND cantidad_pendiente > 0)');
    restriccion('CONSUMO_PENDIENTE', 'CK_CONSUMO_PEND_ESTADO', q'[CHECK (estado IN ('PENDIENTE', 'REGULARIZADO', 'DESCARTADO'))]');
    restriccion('CONSUMO_PENDIENTE', 'CK_CONSUMO_PEND_RESOLUCION',
        q'[CHECK (estado = 'PENDIENTE' OR (id_usuario_resuelve IS NOT NULL AND fecha_resolucion IS NOT NULL))]');
    restriccion('CONSUMO_PENDIENTE', 'CK_CONSUMO_PEND_REGULARIZADO', q'[CHECK (estado <> 'REGULARIZADO' OR id_movimiento IS NOT NULL)]');
    restriccion('CONSUMO_PENDIENTE', 'CK_CONSUMO_PEND_DESCARTADO', q'[CHECK (estado <> 'DESCARTADO' OR observacion IS NOT NULL)]');
    restriccion('CONSUMO_PENDIENTE', 'FK_CONSUMO_PEND_ATENCION', 'FOREIGN KEY (id_atencion) REFERENCES ATENCION (id_atencion)');
    restriccion('CONSUMO_PENDIENTE', 'FK_CONSUMO_PEND_PRODUCTO', 'FOREIGN KEY (id_producto) REFERENCES PRODUCTO (id_producto)');
    restriccion('CONSUMO_PENDIENTE', 'FK_CONSUMO_PEND_SEDE', 'FOREIGN KEY (id_sede) REFERENCES SEDE (id_sede)');
    restriccion('CONSUMO_PENDIENTE', 'FK_CONSUMO_PEND_USUARIO', 'FOREIGN KEY (id_usuario_resuelve) REFERENCES USUARIO (id_usuario)');
    restriccion('CONSUMO_PENDIENTE', 'FK_CONSUMO_PEND_MOVIMIENTO', 'FOREIGN KEY (id_movimiento) REFERENCES MOVIMIENTO_KARDEX (id_movimiento)');
    -- Índices de las FK (índice de FK_CONSUMO_PEND_ATENCION: ya lo cubre UQ_CONSUMO_PEND_ATEN_PROD, mismas columnas iniciales)
    indice('IX_CONSUMO_PEND_PRODUCTO', 'CREATE INDEX IX_CONSUMO_PEND_PRODUCTO ON CONSUMO_PENDIENTE (id_producto)');
    indice('IX_CONSUMO_PEND_SEDE', 'CREATE INDEX IX_CONSUMO_PEND_SEDE ON CONSUMO_PENDIENTE (id_sede)');
    indice('IX_CONSUMO_PEND_USUARIO', 'CREATE INDEX IX_CONSUMO_PEND_USUARIO ON CONSUMO_PENDIENTE (id_usuario_resuelve)');
    indice('IX_CONSUMO_PEND_MOVIMIENTO', 'CREATE INDEX IX_CONSUMO_PEND_MOVIMIENTO ON CONSUMO_PENDIENTE (id_movimiento)');
    comentario(q'[COMMENT ON TABLE CONSUMO_PENDIENTE IS 'Diferencia entre el consumo estimado de un insumo en una atención y lo que se pudo descontar del stock al finalizarla (no alcanzaba). La encargada de almacén la regulariza con una salida de kardex o la descarta con un motivo. (RN53, RF46)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.id_consumo IS 'Identificador.']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.id_atencion IS 'Atención finalizada que consumió el insumo. (RN53)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.id_producto IS 'Insumo (producto de uso interno) que faltó. (RN53)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.id_sede IS 'Sede cuyo almacén no alcanzó. (RN26)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.cantidad_estimada IS 'Consumo estimado en la unidad mínima del insumo (SERVICIO_INSUMO.cantidad_estimada redondeada hacia arriba). (RN53, RN54)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.cantidad_descontada IS 'Lo que se descontó al finalizar la atención (lo que había en stock). (RN53)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.cantidad_pendiente IS 'Lo que falta descontar. Columna derivada. (RN53)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.estado IS 'Estado. (RN53)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.fecha_registro IS 'Momento en que se registró el faltante.']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.id_usuario_resuelve IS 'Encargada de almacén o Administradora que lo regulariza o descarta. (RN17)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.fecha_resolucion IS 'Fecha de la regularización o del descarte.']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.id_movimiento IS 'Salida de kardex con la que se regularizó. (RN18)']');
    comentario(q'[COMMENT ON COLUMN CONSUMO_PENDIENTE.observacion IS 'Motivo del descarte (por ejemplo, el consumo real fue menor).']');

    -- Insumos en su unidad mínima (RN54, cambio 3: solo cambian las descripciones)
    comentario(q'[COMMENT ON COLUMN PRODUCTO.unidad IS 'Unidad de medida. Los insumos se llevan en su unidad mínima (ML, G o UND) para que el consumo estimado se descuente en unidades enteras. (RN54)']');
    comentario(q'[COMMENT ON COLUMN SERVICIO_INSUMO.cantidad_estimada IS 'Unidades del insumo, en su unidad mínima, que consume una atención de la variante; al finalizar la atención se descuenta redondeada hacia arriba. (RN53, RN54)']');
    comentario(q'[COMMENT ON COLUMN MOVIMIENTO_KARDEX.id_atencion IS 'Atención en la que se usó el insumo: la salida automática al finalizarla (CUS11) o la regularización de un consumo pendiente (CUS33). (RN53)']');
END;
/

-- =============================================================================
-- f) Triggers
-- =============================================================================
PROMPT f) Triggers de reglas v5.1...

-- TRG_VENTA_REGLAS (09, sección 8) · [v5.1] cambio 13: rechaza id_usuario_autoriza no nulo (columna obsoleta)
-- y una venta solo entra a una caja ABIERTA (después del cierre, la Administradora reabre la caja: RN30, CUS46).
CREATE OR REPLACE TRIGGER TRG_VENTA_REGLAS
BEFORE INSERT OR UPDATE OR DELETE ON VENTA
FOR EACH ROW
DECLARE
    PROCEDURE validar_caja(p_id_caja IN NUMBER, p_id_sede IN NUMBER) IS
        v_caja_estado CAJA.estado%TYPE;
        v_caja_sede   CAJA.id_sede%TYPE;
    BEGIN
        SELECT estado, id_sede INTO v_caja_estado, v_caja_sede FROM CAJA WHERE id_caja = p_id_caja;
        IF v_caja_sede <> p_id_sede THEN
            RAISE_APPLICATION_ERROR(-20042, 'La caja no pertenece a la sede de la venta.');
        END IF;
        -- [v5.1] Ya no hay venta "autorizada" en una caja cerrada: la Administradora la reabre con un motivo
        IF v_caja_estado <> 'ABIERTA' THEN
            RAISE_APPLICATION_ERROR(-20042, 'La caja del día está cerrada: para registrar la venta, la Administradora debe reabrirla con un motivo (RN30).');
        END IF;
    END validar_caja;
BEGIN
    IF DELETING THEN
        IF :OLD.estado <> 'ABIERTA' THEN
            RAISE_APPLICATION_ERROR(-20041, 'Una venta ' || :OLD.estado || ' nunca se elimina: se anula con nota de crédito (RN12).');
        END IF;
        RETURN;
    END IF;

    -- [v5.1] VENTA.id_usuario_autoriza es obsoleta (CK_VENTA_AUTORIZA_OBSOLETA)
    IF :NEW.id_usuario_autoriza IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20100, 'VENTA.id_usuario_autoriza es obsoleta desde v5.1: la venta después del cierre se registra reabriendo la caja (RN30).');
    END IF;

    IF INSERTING THEN
        IF :NEW.estado <> 'ABIERTA' THEN
            RAISE_APPLICATION_ERROR(-20040, 'Una venta nace ABIERTA y se cierra cuando tiene comprobante y pagos completos (RN10, RN11).');
        END IF;
        :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
        :NEW.fecha_hora := SYSTIMESTAMP;
        validar_caja(:NEW.id_caja, :NEW.id_sede);
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
        validar_caja(:NEW.id_caja, :NEW.id_sede);
    END IF;
    IF :NEW.estado = 'CERRADA' THEN
        PKG_VENTA.validar_cierre(:NEW.id_venta, :NEW.subtotal, :NEW.igv, :NEW.descuento,
                                 :NEW.adelanto_aplicado, :NEW.saldo_favor_aplicado, :NEW.total);
    END IF;
END;
/

-- TRG_CAJA_REGLAS (09, sección 9) · [v5.1] cambio 11: CERRADA -> ABIERTA solo para la Administradora
-- (PKG_SESION.exigir_administradora), con motivo; fija quién y cuándo la reabrió; limpia el cierre y la revisión.
-- El arqueo se vuelve a registrar al cerrar: la regla de siempre rechaza el cierre si lo esperado cambió.
-- Que no haya otra caja ABIERTA en la sede lo garantiza el índice UQ_CAJA_ABIERTA_SEDE (04_caja_inventario.sql).
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

-- TRG_KARDEX_APLICAR (09, sección 10) · [v5.1] cambio 12: RN17 admite una SALIDA con id_atencion hecha por la
-- especialista de esa atención (el consumo estimado que se descuenta al finalizarla, RN53).
CREATE OR REPLACE TRIGGER TRG_KARDEX_APLICAR
BEFORE INSERT ON MOVIMIENTO_KARDEX
FOR EACH ROW
DECLARE
    v_rol   VARCHAR2(30) := PKG_SESION.rol_actual;
    v_delta NUMBER;
    v_saldo NUMBER;

    -- [v5.1] ¿Es la salida del consumo de una atención hecha por la especialista de esa atención?
    FUNCTION es_consumo_de_su_atencion(p_tipo IN VARCHAR2, p_id_atencion IN NUMBER) RETURN BOOLEAN IS
        v_n NUMBER;
    BEGIN
        IF p_tipo <> 'SALIDA' OR p_id_atencion IS NULL THEN
            RETURN FALSE;
        END IF;
        SELECT COUNT(*) INTO v_n
          FROM ATENCION a
          JOIN USUARIO u ON u.id_personal = a.id_personal
         WHERE a.id_atencion = p_id_atencion
           AND u.id_usuario = PKG_SESION.usuario_actual;
        RETURN v_n > 0;
    END es_consumo_de_su_atencion;
BEGIN
    -- RN17: solo almacén y administración mueven inventario; la salida por venta la genera el cierre de la venta
    -- y [v5.1] la salida del consumo estimado la registra la especialista al finalizar su atención (RN53)
    IF v_rol IS NOT NULL AND v_rol NOT IN ('ENCARGADA_ALMACEN', 'ADMINISTRADORA') AND :NEW.id_detalle_venta IS NULL
       AND NOT es_consumo_de_su_atencion(:NEW.tipo, :NEW.id_atencion) THEN
        RAISE_APPLICATION_ERROR(-20031, 'Solo la encargada de almacén o la Administradora registran movimientos de inventario, '
            || 'salvo el consumo que la especialista descuenta al finalizar su atención (RN17, RN53).');
    END IF;
    :NEW.id_usuario := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario);
    :NEW.fecha_hora := SYSTIMESTAMP;
    v_delta := CASE :NEW.tipo WHEN 'SALIDA' THEN -:NEW.cantidad ELSE :NEW.cantidad END;
    PKG_INVENTARIO.aplicar_movimiento(:NEW.id_producto, :NEW.id_sede, v_delta, v_saldo);
    :NEW.saldo := v_saldo;   -- RN18: saldo después del movimiento
END;
/

-- TRG_SOLICITUD_ANULACION_REGLAS · [v5.1] cambio 14 (RN32): la venta debe estar CERRADA; solo la Administradora
-- aprueba o rechaza; una solicitud resuelta no cambia. Quién y cuándo la pidió y la resolvió los fija PKG_SESION.
-- La nota de crédito se emite antes y su id se escribe en el mismo UPDATE que la aprueba.
CREATE OR REPLACE TRIGGER TRG_SOLICITUD_ANULACION_REGLAS
BEFORE INSERT OR UPDATE ON SOLICITUD_ANULACION
FOR EACH ROW
DECLARE
    v_estado_venta VENTA.estado%TYPE;
BEGIN
    IF INSERTING THEN
        IF :NEW.estado <> 'PENDIENTE' THEN
            RAISE_APPLICATION_ERROR(-20102, 'Una solicitud de anulación nace PENDIENTE (RN32).');
        END IF;
        BEGIN
            SELECT estado INTO v_estado_venta FROM VENTA WHERE id_venta = :NEW.id_venta;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20101, 'La venta N° ' || :NEW.id_venta || ' no existe.');
        END;
        IF v_estado_venta <> 'CERRADA' THEN
            RAISE_APPLICATION_ERROR(-20101, 'Solo se pide anular una venta CERRADA: la venta N° ' || :NEW.id_venta
                || ' está ' || v_estado_venta || ' (RN32).');
        END IF;
        :NEW.id_usuario_solicita := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_solicita);
        :NEW.fecha_solicitud     := SYSTIMESTAMP;
        :NEW.id_usuario_resuelve := NULL;
        :NEW.fecha_resolucion    := NULL;
        :NEW.motivo_rechazo      := NULL;
        :NEW.id_nota_credito     := NULL;
        RETURN;
    END IF;

    -- UPDATING
    IF :OLD.estado <> 'PENDIENTE' THEN
        IF PKG_SESION.distinto(:OLD.estado, :NEW.estado)
           OR PKG_SESION.distinto(:OLD.id_venta, :NEW.id_venta)
           OR PKG_SESION.distinto(:OLD.motivo, :NEW.motivo)
           OR PKG_SESION.distinto(:OLD.codigo_motivo, :NEW.codigo_motivo)
           OR PKG_SESION.distinto(:OLD.forma_devolucion, :NEW.forma_devolucion)
           OR PKG_SESION.distinto(:OLD.id_usuario_solicita, :NEW.id_usuario_solicita)
           OR PKG_SESION.distinto(:OLD.fecha_solicitud, :NEW.fecha_solicitud)
           OR PKG_SESION.distinto(:OLD.id_usuario_resuelve, :NEW.id_usuario_resuelve)
           OR PKG_SESION.distinto(:OLD.fecha_resolucion, :NEW.fecha_resolucion)
           OR PKG_SESION.distinto(:OLD.motivo_rechazo, :NEW.motivo_rechazo)
           OR PKG_SESION.distinto(:OLD.id_nota_credito, :NEW.id_nota_credito) THEN
            RAISE_APPLICATION_ERROR(-20102, 'La solicitud de anulación N° ' || :OLD.id_solicitud || ' ya está ' || :OLD.estado
                || ' y no cambia (RN32).');
        END IF;
        RETURN;
    END IF;

    IF PKG_SESION.distinto(:OLD.id_venta, :NEW.id_venta) THEN
        RAISE_APPLICATION_ERROR(-20102, 'Una solicitud de anulación no cambia de venta (RN32).');
    END IF;
    -- Quién y cuándo la pidió no cambian
    :NEW.id_usuario_solicita := :OLD.id_usuario_solicita;
    :NEW.fecha_solicitud     := :OLD.fecha_solicitud;

    IF :NEW.estado = 'PENDIENTE' THEN
        :NEW.id_usuario_resuelve := NULL;
        :NEW.fecha_resolucion    := NULL;
        RETURN;
    END IF;

    -- PENDIENTE -> APROBADA o RECHAZADA (RN32, RES16)
    PKG_SESION.exigir_administradora('aprobar o rechazar una solicitud de anulación (RN32)');
    :NEW.id_usuario_resuelve := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_resuelve);
    :NEW.fecha_resolucion    := SYSTIMESTAMP;
END;
/

-- TRG_CONSUMO_PENDIENTE_REGLAS · [v5.1] cambio 15 (RN17, RN53): solo almacén o la Administradora lo resuelven;
-- resuelto no cambia; la regularización exige una SALIDA de kardex del mismo producto, sede y atención.
CREATE OR REPLACE TRIGGER TRG_CONSUMO_PENDIENTE_REGLAS
BEFORE INSERT OR UPDATE ON CONSUMO_PENDIENTE
FOR EACH ROW
DECLARE
    v_rol VARCHAR2(30) := PKG_SESION.rol_actual;
    v_n   NUMBER;
BEGIN
    IF INSERTING THEN
        IF :NEW.estado <> 'PENDIENTE' THEN
            RAISE_APPLICATION_ERROR(-20103, 'Un consumo pendiente nace PENDIENTE (RN53).');
        END IF;
        :NEW.fecha_registro      := SYSTIMESTAMP;
        :NEW.id_usuario_resuelve := NULL;
        :NEW.fecha_resolucion    := NULL;
        :NEW.id_movimiento       := NULL;
        RETURN;
    END IF;

    -- UPDATING
    IF :OLD.estado <> 'PENDIENTE' THEN
        IF PKG_SESION.distinto(:OLD.estado, :NEW.estado)
           OR PKG_SESION.distinto(:OLD.id_atencion, :NEW.id_atencion)
           OR PKG_SESION.distinto(:OLD.id_producto, :NEW.id_producto)
           OR PKG_SESION.distinto(:OLD.id_sede, :NEW.id_sede)
           OR PKG_SESION.distinto(:OLD.cantidad_estimada, :NEW.cantidad_estimada)
           OR PKG_SESION.distinto(:OLD.cantidad_descontada, :NEW.cantidad_descontada)
           OR PKG_SESION.distinto(:OLD.cantidad_pendiente, :NEW.cantidad_pendiente)
           OR PKG_SESION.distinto(:OLD.fecha_registro, :NEW.fecha_registro)
           OR PKG_SESION.distinto(:OLD.id_usuario_resuelve, :NEW.id_usuario_resuelve)
           OR PKG_SESION.distinto(:OLD.fecha_resolucion, :NEW.fecha_resolucion)
           OR PKG_SESION.distinto(:OLD.id_movimiento, :NEW.id_movimiento)
           OR PKG_SESION.distinto(:OLD.observacion, :NEW.observacion) THEN
            RAISE_APPLICATION_ERROR(-20103, 'El consumo pendiente N° ' || :OLD.id_consumo || ' ya está ' || :OLD.estado
                || ' y no cambia (RN53).');
        END IF;
        RETURN;
    END IF;

    IF :NEW.estado = 'PENDIENTE' THEN
        RETURN;
    END IF;

    -- PENDIENTE -> REGULARIZADO o DESCARTADO
    IF v_rol IS NOT NULL AND v_rol NOT IN ('ENCARGADA_ALMACEN', 'ADMINISTRADORA') THEN
        RAISE_APPLICATION_ERROR(-20104, 'Solo la encargada de almacén o la Administradora regularizan o descartan un consumo pendiente (RN17, RN53).');
    END IF;
    IF :NEW.estado = 'REGULARIZADO' THEN
        SELECT COUNT(*) INTO v_n
          FROM MOVIMIENTO_KARDEX
         WHERE id_movimiento = :NEW.id_movimiento
           AND tipo          = 'SALIDA'
           AND id_producto   = :NEW.id_producto
           AND id_sede       = :NEW.id_sede
           AND id_atencion   = :NEW.id_atencion;
        IF v_n = 0 THEN
            RAISE_APPLICATION_ERROR(-20105, 'Para regularizar el consumo pendiente registre una SALIDA de kardex del mismo insumo, '
                || 'sede y atención (RN53, RN18).');
        END IF;
    END IF;
    :NEW.id_usuario_resuelve := NVL(PKG_SESION.usuario_actual, :NEW.id_usuario_resuelve);
    :NEW.fecha_resolucion    := SYSTIMESTAMP;
END;
/

PROMPT f) Triggers de auditoría de las tablas nuevas (cambio 16, como 10_triggers_auditoria.sql)...
CREATE OR REPLACE TRIGGER TRG_AUD_SOLICITUD_ANULACION
AFTER INSERT OR UPDATE OR DELETE ON SOLICITUD_ANULACION
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_solicitud', TO_CHAR(:NEW.id_solicitud)) || PKG_SESION.par('id_venta', TO_CHAR(:NEW.id_venta)) || PKG_SESION.par('motivo', :NEW.motivo) || PKG_SESION.par('codigo_motivo', :NEW.codigo_motivo) || PKG_SESION.par('forma_devolucion', :NEW.forma_devolucion) || PKG_SESION.par('id_usuario_solicita', TO_CHAR(:NEW.id_usuario_solicita)) || PKG_SESION.par('fecha_solicitud', TO_CHAR(:NEW.fecha_solicitud, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('id_usuario_resuelve', TO_CHAR(:NEW.id_usuario_resuelve)) || PKG_SESION.par('fecha_resolucion', TO_CHAR(:NEW.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('motivo_rechazo', :NEW.motivo_rechazo) || PKG_SESION.par('id_nota_credito', TO_CHAR(:NEW.id_nota_credito));
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'SOLICITUD_ANULACION', :NEW.id_solicitud, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_venta', TO_CHAR(:OLD.id_venta), TO_CHAR(:NEW.id_venta));
        v_det := v_det || PKG_SESION.diff('motivo', :OLD.motivo, :NEW.motivo);
        v_det := v_det || PKG_SESION.diff('codigo_motivo', :OLD.codigo_motivo, :NEW.codigo_motivo);
        v_det := v_det || PKG_SESION.diff('forma_devolucion', :OLD.forma_devolucion, :NEW.forma_devolucion);
        v_det := v_det || PKG_SESION.diff('id_usuario_solicita', TO_CHAR(:OLD.id_usuario_solicita), TO_CHAR(:NEW.id_usuario_solicita));
        v_det := v_det || PKG_SESION.diff('fecha_solicitud', TO_CHAR(:OLD.fecha_solicitud, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_solicitud, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('id_usuario_resuelve', TO_CHAR(:OLD.id_usuario_resuelve), TO_CHAR(:NEW.id_usuario_resuelve));
        v_det := v_det || PKG_SESION.diff('fecha_resolucion', TO_CHAR(:OLD.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('motivo_rechazo', :OLD.motivo_rechazo, :NEW.motivo_rechazo);
        v_det := v_det || PKG_SESION.diff('id_nota_credito', TO_CHAR(:OLD.id_nota_credito), TO_CHAR(:NEW.id_nota_credito));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'SOLICITUD_ANULACION', :NEW.id_solicitud, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_solicitud', TO_CHAR(:OLD.id_solicitud)) || PKG_SESION.par('id_venta', TO_CHAR(:OLD.id_venta)) || PKG_SESION.par('motivo', :OLD.motivo) || PKG_SESION.par('codigo_motivo', :OLD.codigo_motivo) || PKG_SESION.par('forma_devolucion', :OLD.forma_devolucion) || PKG_SESION.par('id_usuario_solicita', TO_CHAR(:OLD.id_usuario_solicita)) || PKG_SESION.par('fecha_solicitud', TO_CHAR(:OLD.fecha_solicitud, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('id_usuario_resuelve', TO_CHAR(:OLD.id_usuario_resuelve)) || PKG_SESION.par('fecha_resolucion', TO_CHAR(:OLD.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('motivo_rechazo', :OLD.motivo_rechazo) || PKG_SESION.par('id_nota_credito', TO_CHAR(:OLD.id_nota_credito));
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'SOLICITUD_ANULACION', :OLD.id_solicitud, v_det, NULL);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_AUD_CONSUMO_PENDIENTE
AFTER INSERT OR UPDATE OR DELETE ON CONSUMO_PENDIENTE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_consumo', TO_CHAR(:NEW.id_consumo)) || PKG_SESION.par('id_atencion', TO_CHAR(:NEW.id_atencion)) || PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('cantidad_estimada', TO_CHAR(:NEW.cantidad_estimada)) || PKG_SESION.par('cantidad_descontada', TO_CHAR(:NEW.cantidad_descontada)) || PKG_SESION.par('cantidad_pendiente', TO_CHAR(:NEW.cantidad_pendiente)) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('fecha_registro', TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario_resuelve', TO_CHAR(:NEW.id_usuario_resuelve)) || PKG_SESION.par('fecha_resolucion', TO_CHAR(:NEW.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_movimiento', TO_CHAR(:NEW.id_movimiento)) || PKG_SESION.par('observacion', :NEW.observacion);
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'CONSUMO_PENDIENTE', :NEW.id_consumo, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_atencion', TO_CHAR(:OLD.id_atencion), TO_CHAR(:NEW.id_atencion));
        v_det := v_det || PKG_SESION.diff('id_producto', TO_CHAR(:OLD.id_producto), TO_CHAR(:NEW.id_producto));
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('cantidad_estimada', TO_CHAR(:OLD.cantidad_estimada), TO_CHAR(:NEW.cantidad_estimada));
        v_det := v_det || PKG_SESION.diff('cantidad_descontada', TO_CHAR(:OLD.cantidad_descontada), TO_CHAR(:NEW.cantidad_descontada));
        v_det := v_det || PKG_SESION.diff('cantidad_pendiente', TO_CHAR(:OLD.cantidad_pendiente), TO_CHAR(:NEW.cantidad_pendiente));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('id_usuario_resuelve', TO_CHAR(:OLD.id_usuario_resuelve), TO_CHAR(:NEW.id_usuario_resuelve));
        v_det := v_det || PKG_SESION.diff('fecha_resolucion', TO_CHAR(:OLD.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('id_movimiento', TO_CHAR(:OLD.id_movimiento), TO_CHAR(:NEW.id_movimiento));
        v_det := v_det || PKG_SESION.diff('observacion', :OLD.observacion, :NEW.observacion);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'CONSUMO_PENDIENTE', :NEW.id_consumo, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_consumo', TO_CHAR(:OLD.id_consumo)) || PKG_SESION.par('id_atencion', TO_CHAR(:OLD.id_atencion)) || PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('cantidad_estimada', TO_CHAR(:OLD.cantidad_estimada)) || PKG_SESION.par('cantidad_descontada', TO_CHAR(:OLD.cantidad_descontada)) || PKG_SESION.par('cantidad_pendiente', TO_CHAR(:OLD.cantidad_pendiente)) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario_resuelve', TO_CHAR(:OLD.id_usuario_resuelve)) || PKG_SESION.par('fecha_resolucion', TO_CHAR(:OLD.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_movimiento', TO_CHAR(:OLD.id_movimiento)) || PKG_SESION.par('observacion', :OLD.observacion);
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'CONSUMO_PENDIENTE', :OLD.id_consumo, v_det, NULL);
    END IF;
END;
/

-- =============================================================================
-- g) Permisos (como 13_permisos.sql: sin DELETE, la historia no se borra)
-- =============================================================================
PROMPT g) Permisos del rol de aplicación sobre las tablas nuevas...
GRANT SELECT, INSERT, UPDATE ON SOLICITUD_ANULACION TO ROL_CAUTIVA_APLICACION;
GRANT SELECT, INSERT, UPDATE ON CONSUMO_PENDIENTE TO ROL_CAUTIVA_APLICACION;

-- =============================================================================
-- h) Datos de la Iteración 2
-- =============================================================================
PROMPT h) Datos de la Iteración 2...
DECLARE
    -- Mismo hash BCrypt de la clave temporal de 15_datos_iteracion1.sql
    c_hash CONSTANT VARCHAR2(60) := '$2a$10$wo6I3ZjHiMkRO1QZOhnXzeLY65D3ueTW4VnmkyWLcc7p11N5ICftO';
    v_n    NUMBER;
    v_sede NUMBER;
    v_dni  NUMBER;
    v_alm  NUMBER;

    PROCEDURE permiso(p_rol IN VARCHAR2, p_modulo IN VARCHAR2, p_accion IN VARCHAR2) IS
    BEGIN
        INSERT INTO ROL_PERMISO (id_rol, id_permiso)
        SELECT r.id_rol, p.id_permiso
          FROM ROL r JOIN PERMISO p ON p.modulo = p_modulo AND p.accion = p_accion
         WHERE r.nombre = p_rol
           AND NOT EXISTS (SELECT 1 FROM ROL_PERMISO rp WHERE rp.id_rol = r.id_rol AND rp.id_permiso = p.id_permiso);
        IF SQL%ROWCOUNT > 0 THEN
            DBMS_OUTPUT.PUT_LINE('   + ' || p_rol || ' -> ' || p_modulo || ':' || p_accion);
        END IF;
    END permiso;
BEGIN
    -- PARAMETRO (cambios 17 y 18)
    SELECT COUNT(*) INTO v_n FROM PARAMETRO WHERE codigo = 'AGENDA_PASO_MIN';
    IF v_n = 0 THEN
        INSERT INTO PARAMETRO (codigo, descripcion, valor_numero)
        VALUES ('AGENDA_PASO_MIN', 'Cada cuántos minutos se ofrecen las horas libres de la agenda (RN51)', 15);
        DBMS_OUTPUT.PUT_LINE('   + PARAMETRO AGENDA_PASO_MIN = 15 (RN51)');
    END IF;
    UPDATE PARAMETRO SET valor_numero = 10 WHERE codigo = 'LIMPIEZA_MAX_MIN' AND (valor_numero IS NULL OR valor_numero <> 10);
    IF SQL%ROWCOUNT > 0 THEN
        DBMS_OUTPUT.PUT_LINE('   + PARAMETRO LIMPIEZA_MAX_MIN = 10 (RN03)');
    END IF;

    -- Permisos que faltan (12 no los daba). ADMINISTRADORA ya tiene todos y no se toca.
    permiso('RECEPCIONISTA', 'CATALOGO', 'VER');    -- elige servicios, paquetes y promociones (CUS05, CUS13, CUS18, CUS21)
    permiso('RECEPCIONISTA', 'VENTAS', 'ANULAR');   -- crea la solicitud de anulación (CUS22)
    SELECT COUNT(*) INTO v_n
      FROM ROL_PERMISO rp
      JOIN ROL r ON r.id_rol = rp.id_rol
      JOIN PERMISO p ON p.id_permiso = rp.id_permiso
     WHERE p.modulo = 'VENTAS' AND p.accion = 'EDITAR' AND r.nombre <> 'ADMINISTRADORA';
    IF v_n > 0 THEN
        RAISE_APPLICATION_ERROR(-20106, 'VENTAS:EDITAR debe quedar solo en ADMINISTRADORA (bandeja de anulación y devoluciones) y lo tienen '
            || v_n || ' rol(es) más. Revise ROL_PERMISO.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('   VENTAS:EDITAR sigue solo en ADMINISTRADORA.');

    -- Usuaria de prueba de la encargada de almacén (solo local y pruebas)
    SELECT COUNT(*) INTO v_n FROM USUARIO WHERE nombre_usuario = 'almacen';
    IF v_n = 0 THEN
        SELECT id_sede INTO v_sede FROM SEDE WHERE nombre = 'Pichanaki Centro';
        SELECT id_tipo_documento INTO v_dni FROM TIPO_DOCUMENTO WHERE abreviatura = 'DNI';
        INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono)
        VALUES ('NATURAL', 'Encargada', 'Almacén', v_dni, '70000006', '987100006')
        RETURNING id_persona INTO v_alm;
        INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso)
        VALUES (v_alm, v_sede, 'ENCARGADA_ALMACEN', 'FIJO', 1230, 0, TRUNC(SYSDATE));
        FOR d IN 1 .. 6 LOOP
            INSERT INTO HORARIO_PERSONAL (id_personal, dia_semana, hora_entrada, hora_salida, minutos_almuerzo)
            VALUES (v_alm, d, '09:00', '19:00', 60);
        END LOOP;
        INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash, debe_cambiar_clave)
        SELECT v_alm, id_rol, 'almacen', c_hash, 'S' FROM ROL WHERE nombre = 'ENCARGADA_ALMACEN';
        DBMS_OUTPUT.PUT_LINE('   + Usuaria almacen (ENCARGADA_ALMACEN, DNI de prueba 70000006, sede Pichanaki Centro)');
    END IF;

    -- RN54: insumos en su unidad mínima y consumo estimado en unidades enteras
    SELECT COUNT(*) INTO v_n FROM PRODUCTO WHERE tipo_uso = 'INSUMO' AND unidad NOT IN ('ML', 'G', 'UND');
    IF v_n > 0 THEN
        DBMS_OUTPUT.PUT_LINE('   REVISAR (RN54): ' || v_n || ' insumo(s) con una unidad que no es ML, G ni UND.');
    END IF;
    SELECT COUNT(*) INTO v_n FROM SERVICIO_INSUMO WHERE cantidad_estimada <> TRUNC(cantidad_estimada);
    IF v_n > 0 THEN
        DBMS_OUTPUT.PUT_LINE('   REVISAR (RN54): ' || v_n || ' consumo(s) estimado(s) con decimales en SERVICIO_INSUMO.');
    END IF;
    SELECT COUNT(*) INTO v_n FROM PRODUCTO WHERE tipo_uso = 'INSUMO';
    DBMS_OUTPUT.PUT_LINE('   RN54: ' || v_n || ' insumo(s) en PRODUCTO; 12_datos_iniciales.sql no carga insumos ni SERVICIO_INSUMO.');
    COMMIT;
END;
/

-- Las ALTER TABLE pueden dejar objetos dependientes por recompilar
BEGIN
    DBMS_UTILITY.COMPILE_SCHEMA(schema => USER, compile_all => FALSE);
END;
/

-- =============================================================================
-- i) Resumen
-- =============================================================================
WHENEVER SQLERROR CONTINUE
SET FEEDBACK ON
PROMPT ==== Errores de compilación (debe salir vacío) ====
SELECT name, type, line, position, SUBSTR(text, 1, 200) AS error
  FROM user_errors
 ORDER BY name, type, sequence;

PROMPT ==== Resumen del esquema v5.1 ====
SELECT COUNT(*) AS tablas FROM user_tables;
SELECT constraint_type, COUNT(*) AS total FROM user_constraints
 WHERE constraint_name NOT LIKE 'SYS%' AND constraint_name NOT LIKE 'BIN$%'
 GROUP BY constraint_type ORDER BY constraint_type;
SELECT COUNT(*) AS indices_ix FROM user_indexes WHERE index_name LIKE 'IX\_%' ESCAPE '\';
SELECT object_type, COUNT(*) AS total FROM user_objects
 WHERE object_type IN ('FUNCTION', 'PACKAGE', 'PACKAGE BODY', 'TRIGGER', 'VIEW') GROUP BY object_type ORDER BY object_type;
SELECT COUNT(*) AS objetos_invalidos FROM user_objects WHERE status = 'INVALID';
SELECT codigo, valor_numero FROM PARAMETRO WHERE codigo IN ('AGENDA_PASO_MIN', 'LIMPIEZA_MAX_MIN') ORDER BY codigo;
SELECT u.nombre_usuario, r.nombre AS rol, u.debe_cambiar_clave FROM USUARIO u JOIN ROL r ON r.id_rol = u.id_rol
 WHERE u.nombre_usuario = 'almacen';
PROMPT Esperado: 57 tablas; P = 57, R = 131, U = 35, C = 183 con nombre propio (más los NOT NULL de sistema);
PROMPT           105 índices IX_ (97 de v5, IX_CAJA_USU_REA, 3 de SOLICITUD_ANULACION y 4 de CONSUMO_PENDIENTE);
PROMPT           FUNCTION 2, PACKAGE 5, PACKAGE BODY 5, TRIGGER 129 (125 de v5, 2 de reglas y 2 de auditoría), VIEW 27;
PROMPT           0 objetos inválidos; AGENDA_PASO_MIN 15 y LIMPIEZA_MAX_MIN 10; usuaria almacen con ENCARGADA_ALMACEN.
PROMPT 17_datos_iteracion2.sql terminado.
