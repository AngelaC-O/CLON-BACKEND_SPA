-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle 19c · VERSIÓN 5
-- 06 · Reglas de las jerarquías de herencia y del catálogo TIPO_DOCUMENTO
-- Un CHECK no puede leer otra tabla; por eso las reglas que cruzan el supertipo
-- y sus subtipos, o un número con su TIPO_DOCUMENTO, se aseguran con triggers.
-- Errores nuevos: -20090 a -20098 (v4 llegó hasta -20081).
-- Códigos del Catálogo N° 06 de SUNAT usados aquí: 1 DNI, 4 Carnet de Extranjería, 6 RUC.
-- =============================================================================
SET DEFINE OFF

-- -----------------------------------------------------------------------------
-- Validación de un número de documento contra su TIPO_DOCUMENTO
-- (longitud mínima y máxima, solo dígitos, prefijo del RUC y tipo ACTIVO).
-- La usan PERSONA y COMPROBANTE. Devuelve el codigo_sunat del tipo.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_VALIDAR_DOCUMENTO (
    p_id_tipo_documento IN NUMBER,
    p_numero            IN VARCHAR2
) RETURN VARCHAR2
IS
    v_tipo TIPO_DOCUMENTO%ROWTYPE;
BEGIN
    SELECT * INTO v_tipo FROM TIPO_DOCUMENTO WHERE id_tipo_documento = p_id_tipo_documento;
    IF v_tipo.estado <> 'ACTIVO' THEN
        RAISE_APPLICATION_ERROR(-20097, 'El tipo de documento ' || v_tipo.abreviatura || ' no está activo.');
    END IF;
    IF LENGTH(p_numero) NOT BETWEEN v_tipo.longitud_min AND v_tipo.longitud_max THEN
        RAISE_APPLICATION_ERROR(-20097, 'El ' || v_tipo.abreviatura || ' debe tener entre ' || v_tipo.longitud_min
                                        || ' y ' || v_tipo.longitud_max || ' caracteres.');
    END IF;
    IF v_tipo.solo_numeros = 'S' AND NOT REGEXP_LIKE(p_numero, '^[0-9]+$') THEN
        RAISE_APPLICATION_ERROR(-20097, 'El ' || v_tipo.abreviatura || ' solo admite dígitos.');
    END IF;
    IF v_tipo.codigo_sunat = '6' AND NOT REGEXP_LIKE(p_numero, '^(10|15|17|20)[0-9]{9}$') THEN
        RAISE_APPLICATION_ERROR(-20097, 'El RUC debe empezar con 10, 15, 17 o 20 y tener 11 dígitos.');
    END IF;
    RETURN v_tipo.codigo_sunat;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20097, 'El tipo de documento indicado no existe.');
END;
/

-- Devuelve el codigo_sunat del documento de una persona (nulo si no tiene documento).
CREATE OR REPLACE FUNCTION FN_CODIGO_DOC_PERSONA (p_id_persona IN NUMBER) RETURN VARCHAR2
IS
    v_codigo TIPO_DOCUMENTO.codigo_sunat%TYPE;
BEGIN
    SELECT t.codigo_sunat INTO v_codigo
      FROM PERSONA p LEFT JOIN TIPO_DOCUMENTO t ON t.id_tipo_documento = p.id_tipo_documento
     WHERE p.id_persona = p_id_persona;
    RETURN v_codigo;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20093, 'Primero registre a la persona en PERSONA.');
END;
/

-- -----------------------------------------------------------------------------
-- Jerarquía PERSONA (total y solapada): CLIENTE, PERSONAL, PROVEEDOR
-- -----------------------------------------------------------------------------

-- Al insertar el subtipo CLIENTE: la persona debe ser NATURAL y tener teléfono (RN01, RF03).
-- Su documento es opcional (RN35) y puede ser de cualquier tipo (DNI, carnet, pasaporte...).
CREATE OR REPLACE TRIGGER TRG_CLIENTE_HERENCIA
BEFORE INSERT OR UPDATE OF id_cliente ON CLIENTE
FOR EACH ROW
DECLARE
    v_tipo     PERSONA.tipo_persona%TYPE;
    v_telefono PERSONA.telefono%TYPE;
BEGIN
    SELECT tipo_persona, telefono INTO v_tipo, v_telefono
      FROM PERSONA WHERE id_persona = :NEW.id_cliente;
    IF v_tipo <> 'NATURAL' THEN
        RAISE_APPLICATION_ERROR(-20093, 'Una clienta debe ser una persona NATURAL.');
    END IF;
    IF v_telefono IS NULL THEN
        RAISE_APPLICATION_ERROR(-20090, 'La clienta debe tener teléfono registrado en PERSONA (RN01).');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20093, 'Primero registre a la persona en PERSONA (id_persona = id_cliente).');
END;
/

-- Al insertar el subtipo PERSONAL: la persona debe ser NATURAL y tener DNI o carnet de extranjería (RF51, RF97).
CREATE OR REPLACE TRIGGER TRG_PERSONAL_HERENCIA
BEFORE INSERT OR UPDATE OF id_personal ON PERSONAL
FOR EACH ROW
DECLARE
    v_tipo PERSONA.tipo_persona%TYPE;
BEGIN
    SELECT tipo_persona INTO v_tipo FROM PERSONA WHERE id_persona = :NEW.id_personal;
    IF v_tipo <> 'NATURAL' THEN
        RAISE_APPLICATION_ERROR(-20093, 'Una persona del personal debe ser una persona NATURAL.');
    END IF;
    IF NVL(FN_CODIGO_DOC_PERSONA(:NEW.id_personal), '-') NOT IN ('1', '4') THEN
        RAISE_APPLICATION_ERROR(-20091, 'La persona del personal debe tener DNI o carnet de extranjería en PERSONA (RF51).');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20093, 'Primero registre a la persona en PERSONA (id_persona = id_personal).');
END;
/

-- Al insertar el subtipo PROVEEDOR: la persona debe tener RUC (RF40, RF98).
CREATE OR REPLACE TRIGGER TRG_PROVEEDOR_HERENCIA
BEFORE INSERT OR UPDATE OF id_proveedor ON PROVEEDOR
FOR EACH ROW
BEGIN
    IF NVL(FN_CODIGO_DOC_PERSONA(:NEW.id_proveedor), '-') <> '6' THEN
        RAISE_APPLICATION_ERROR(-20092, 'El proveedor debe tener RUC en PERSONA (RF40).');
    END IF;
END;
/

-- Al registrar o cambiar el supertipo PERSONA:
--  1) el número debe cumplir la longitud y el formato de su TIPO_DOCUMENTO; la persona JURIDICA usa RUC;
--  2) si el documento cambia sin una verificación nueva, la ficha vuelve a MANUAL (RN45, RF99);
--  3) un tipo sin consulta en el servicio DNI/RUC (carnet, pasaporte, cédula) no puede quedar VERIFICADO;
--  4) no se puede dejar sin teléfono a una clienta, sin DNI o carnet al personal ni sin RUC al proveedor.
CREATE OR REPLACE TRIGGER TRG_PERSONA_REGLAS
BEFORE INSERT OR UPDATE ON PERSONA
FOR EACH ROW
DECLARE
    v_codigo TIPO_DOCUMENTO.codigo_sunat%TYPE;
    v_api    TIPO_DOCUMENTO.consulta_api%TYPE;
    v_n      NUMBER;
BEGIN
    IF :NEW.numero_documento IS NOT NULL THEN
        :NEW.numero_documento := UPPER(TRIM(:NEW.numero_documento));
        v_codigo := FN_VALIDAR_DOCUMENTO(:NEW.id_tipo_documento, :NEW.numero_documento);
        SELECT consulta_api INTO v_api FROM TIPO_DOCUMENTO WHERE id_tipo_documento = :NEW.id_tipo_documento;
    END IF;

    IF :NEW.tipo_persona = 'JURIDICA' AND NVL(v_codigo, '-') <> '6' THEN
        RAISE_APPLICATION_ERROR(-20098, 'Una persona JURIDICA se identifica con RUC.');
    END IF;

    IF UPDATING
       AND (NVL(:NEW.id_tipo_documento, -1) <> NVL(:OLD.id_tipo_documento, -1)
            OR NVL(:NEW.numero_documento, '-') <> NVL(:OLD.numero_documento, '-'))
       AND NOT (:NEW.origen_datos = 'VERIFICADO'
                AND :NEW.fecha_verificacion IS NOT NULL
                AND (:OLD.fecha_verificacion IS NULL OR :NEW.fecha_verificacion > :OLD.fecha_verificacion)) THEN
        :NEW.origen_datos := 'MANUAL';
        :NEW.fecha_verificacion := NULL;
    END IF;

    IF :NEW.origen_datos = 'VERIFICADO' AND NVL(v_api, 'N') <> 'S' THEN
        RAISE_APPLICATION_ERROR(-20098, 'Solo un DNI o un RUC consultado en el servicio DNI/RUC puede quedar VERIFICADO (RN45).');
    END IF;

    IF UPDATING THEN
        SELECT COUNT(*) INTO v_n FROM CLIENTE WHERE id_cliente = :NEW.id_persona;
        IF v_n > 0 AND (:NEW.telefono IS NULL OR :NEW.tipo_persona <> 'NATURAL') THEN
            RAISE_APPLICATION_ERROR(-20090, 'La persona es clienta: debe ser NATURAL y conservar su teléfono (RN01).');
        END IF;

        SELECT COUNT(*) INTO v_n FROM PERSONAL WHERE id_personal = :NEW.id_persona;
        IF v_n > 0 AND (NVL(v_codigo, '-') NOT IN ('1', '4') OR :NEW.tipo_persona <> 'NATURAL') THEN
            RAISE_APPLICATION_ERROR(-20091, 'La persona es del personal: debe ser NATURAL y conservar su DNI o carnet (RF51).');
        END IF;

        SELECT COUNT(*) INTO v_n FROM PROVEEDOR WHERE id_proveedor = :NEW.id_persona;
        IF v_n > 0 AND NVL(v_codigo, '-') <> '6' THEN
            RAISE_APPLICATION_ERROR(-20092, 'La persona es proveedor: debe conservar su RUC (RF40).');
        END IF;
    END IF;
END;
/

-- -----------------------------------------------------------------------------
-- Jerarquía COMPROBANTE (total y disjunta): BOLETA, FACTURA (discriminador) y NOTA_CREDITO (tabla)
-- -----------------------------------------------------------------------------

-- El documento del comprador cumple su TIPO_DOCUMENTO y la factura exige RUC (RN08).
CREATE OR REPLACE TRIGGER TRG_COMPROBANTE_COMPRADOR
BEFORE INSERT OR UPDATE OF id_tipo_doc_comprador, nro_doc_comprador, tipo ON COMPROBANTE
FOR EACH ROW
DECLARE
    v_codigo TIPO_DOCUMENTO.codigo_sunat%TYPE;
BEGIN
    IF :NEW.nro_doc_comprador IS NOT NULL THEN
        :NEW.nro_doc_comprador := UPPER(TRIM(:NEW.nro_doc_comprador));
        v_codigo := FN_VALIDAR_DOCUMENTO(:NEW.id_tipo_doc_comprador, :NEW.nro_doc_comprador);
    END IF;
    IF :NEW.tipo = 'FACTURA' AND NVL(v_codigo, '-') <> '6' THEN
        RAISE_APPLICATION_ERROR(-20098, 'La factura exige el RUC del comprador (RN08).');
    END IF;
END;
/

-- La fila de NOTA_CREDITO solo puede colgar de un COMPROBANTE de tipo NC_BOLETA o NC_FACTURA,
-- y la venta debe tener emitido el documento que se anula (NC_BOLETA -> BOLETA, NC_FACTURA -> FACTURA).
CREATE OR REPLACE TRIGGER TRG_NOTA_CREDITO_HERENCIA
BEFORE INSERT OR UPDATE OF id_nota_credito ON NOTA_CREDITO
FOR EACH ROW
DECLARE
    v_tipo  COMPROBANTE.tipo%TYPE;
    v_venta COMPROBANTE.id_venta%TYPE;
    v_n     NUMBER;
BEGIN
    SELECT tipo, id_venta INTO v_tipo, v_venta
      FROM COMPROBANTE WHERE id_comprobante = :NEW.id_nota_credito;
    IF v_tipo NOT IN ('NC_BOLETA', 'NC_FACTURA') THEN
        RAISE_APPLICATION_ERROR(-20094, 'NOTA_CREDITO solo admite comprobantes de tipo NC_BOLETA o NC_FACTURA.');
    END IF;
    SELECT COUNT(*) INTO v_n
      FROM COMPROBANTE
     WHERE id_venta = v_venta
       AND tipo = CASE v_tipo WHEN 'NC_BOLETA' THEN 'BOLETA' ELSE 'FACTURA' END;
    IF v_n = 0 THEN
        RAISE_APPLICATION_ERROR(-20095, 'La venta no tiene emitido el documento que la nota de crédito anula (NC_BOLETA anula BOLETA; NC_FACTURA anula FACTURA).');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20094, 'Primero registre el comprobante (id_comprobante = id_nota_credito).');
END;
/

-- El discriminador y la venta de un comprobante emitido no cambian (un comprobante enviado a SUNAT es inmutable).
CREATE OR REPLACE TRIGGER TRG_COMPROBANTE_INMUTABLE
BEFORE UPDATE OF tipo, id_venta, serie, numero ON COMPROBANTE
FOR EACH ROW
BEGIN
    IF :NEW.tipo <> :OLD.tipo OR :NEW.id_venta <> :OLD.id_venta
       OR :NEW.serie <> :OLD.serie OR :NEW.numero <> :OLD.numero THEN
        RAISE_APPLICATION_ERROR(-20096, 'El tipo, la venta, la serie y el número de un comprobante emitido no se modifican.');
    END IF;
END;
/

-- Nota: que todo COMPROBANTE NC_* tenga su fila en NOTA_CREDITO (jerarquía total) se asegura en la
-- misma transacción que emite la nota (paquete de ventas): primero COMPROBANTE, luego NOTA_CREDITO, y COMMIT.

PROMPT 06_reglas_herencia.sql terminado: 2 funciones y 7 triggers.
