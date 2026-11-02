-- =============================================================================
-- Cautiva Spa · v5 · Prueba rápida después de instalar (no deja datos: termina en ROLLBACK)
-- Ejecutar con F5 conectado como CAUTIVA5. Cada caso imprime OK o FALLO.
-- =============================================================================
SET SERVEROUTPUT ON
SET DEFINE OFF
DECLARE
    v_dni   NUMBER;
    v_ruc   NUMBER;
    v_pas   NUMBER;
    v_id    NUMBER;
    v_ok    NUMBER := 0;
    v_mal   NUMBER := 0;
    PROCEDURE ok(p VARCHAR2)  IS BEGIN v_ok := v_ok + 1;  DBMS_OUTPUT.PUT_LINE('OK    ' || p); END;
    PROCEDURE mal(p VARCHAR2) IS BEGIN v_mal := v_mal + 1; DBMS_OUTPUT.PUT_LINE('FALLO ' || p); END;
    PROCEDURE espera_error(p_caso VARCHAR2, p_sql VARCHAR2, p_codigo NUMBER) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
        mal(p_caso || ' (se esperaba el error ' || p_codigo || ')');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = p_codigo THEN ok(p_caso || ' -> ' || SQLCODE);
            ELSE mal(p_caso || ' -> ' || SQLERRM); END IF;
    END;
BEGIN
    SAVEPOINT inicio;
    SELECT id_tipo_documento INTO v_dni FROM TIPO_DOCUMENTO WHERE abreviatura = 'DNI';
    SELECT id_tipo_documento INTO v_ruc FROM TIPO_DOCUMENTO WHERE abreviatura = 'RUC';
    SELECT id_tipo_documento INTO v_pas FROM TIPO_DOCUMENTO WHERE abreviatura = 'PASAPORTE';

    -- 1. Clienta con DNI válido (supertipo + subtipo)
    INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono)
    VALUES ('NATURAL', 'Ana', 'Prueba', v_dni, ' 12345678 ', '964000111') RETURNING id_persona INTO v_id;
    INSERT INTO CLIENTE (id_cliente) VALUES (v_id);
    ok('Clienta con DNI registrada (PERSONA + CLIENTE)');
    SELECT COUNT(*) INTO v_id FROM PERSONA WHERE numero_documento = '12345678';
    IF v_id = 1 THEN ok('El número se guardó sin espacios (TRIM)'); ELSE mal('TRIM del número'); END IF;

    -- 2. Clienta extranjera con pasaporte (alfanumérico)
    INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono)
    VALUES ('NATURAL', 'Lucía', 'Extranjera', v_pas, 'ab123456', '964000222') RETURNING id_persona INTO v_id;
    INSERT INTO CLIENTE (id_cliente) VALUES (v_id);
    ok('Clienta con pasaporte registrada (en mayúsculas: AB123456)');

    -- 3. Reglas que deben rechazar
    espera_error('DNI de 3 dígitos',
        'INSERT INTO PERSONA (tipo_persona, nombres, id_tipo_documento, numero_documento) VALUES (''NATURAL'', ''X'', ' || v_dni || ', ''123'')', -20097);
    espera_error('RUC con prefijo inválido',
        'INSERT INTO PERSONA (tipo_persona, razon_social, id_tipo_documento, numero_documento) VALUES (''JURIDICA'', ''X SAC'', ' || v_ruc || ', ''99123456789'')', -20097);
    espera_error('Persona jurídica sin RUC',
        'INSERT INTO PERSONA (tipo_persona, razon_social, id_tipo_documento, numero_documento) VALUES (''JURIDICA'', ''X SAC'', ' || v_dni || ', ''87654321'')', -20098);
    espera_error('Documento repetido (UNIQUE tipo + número)',
        'INSERT INTO PERSONA (tipo_persona, nombres, id_tipo_documento, numero_documento) VALUES (''NATURAL'', ''Y'', ' || v_dni || ', ''12345678'')', -1);
    espera_error('VERIFICADO con pasaporte (no se consulta en APISPERU)',
        'INSERT INTO PERSONA (tipo_persona, nombres, id_tipo_documento, numero_documento, origen_datos, fecha_verificacion) VALUES (''NATURAL'', ''Z'', ' || v_pas || ', ''ZX998877'', ''VERIFICADO'', SYSTIMESTAMP)', -20098);

    -- 4. Proveedor: exige RUC
    INSERT INTO PERSONA (tipo_persona, razon_social, id_tipo_documento, numero_documento)
    VALUES ('JURIDICA', 'Distribuidora Prueba SAC', v_ruc, '20123456789') RETURNING id_persona INTO v_id;
    INSERT INTO PROVEEDOR (id_proveedor) VALUES (v_id);
    ok('Proveedor con RUC registrado');
    SELECT id_persona INTO v_id FROM PERSONA WHERE numero_documento = '12345678';
    espera_error('Proveedor con DNI', 'INSERT INTO PROVEEDOR (id_proveedor) VALUES (' || v_id || ')', -20092);
    espera_error('Clienta persona jurídica',
        'INSERT INTO CLIENTE (id_cliente) SELECT id_persona FROM PERSONA WHERE numero_documento = ''20123456789''', -20093);

    -- 5. Inmutables y catálogo
    espera_error('Kardex no se elimina', 'DELETE FROM MOVIMIENTO_KARDEX', -20020);
    SELECT COUNT(*) INTO v_id FROM V_DOCUMENTOS_VERIFICADOS;
    IF v_id > 0 THEN ok('Vista V_DOCUMENTOS_VERIFICADOS responde'); ELSE mal('V_DOCUMENTOS_VERIFICADOS vacía'); END IF;
    SELECT COUNT(*) INTO v_id FROM V_STOCK_ALERTA;
    ok('Vista V_STOCK_ALERTA responde (' || v_id || ' filas)');

    ROLLBACK TO inicio;
    DBMS_OUTPUT.PUT_LINE('Resultado: ' || v_ok || ' OK, ' || v_mal || ' FALLO (todo se deshizo con ROLLBACK)');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FALLO inesperado: ' || SQLERRM || ' ' || DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
        ROLLBACK TO inicio;
END;
/
ROLLBACK;
