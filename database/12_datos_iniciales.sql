-- =====================================================================
-- CAUTIVA SPA E.I.R.L. · BackOffice web · Base de datos Oracle (12c o superior)
-- Script 12 (v5) · Datos iniciales (catálogos y parámetros)
-- Se ejecuta como: dueño del esquema v5, una sola vez, después de los scripts 01 a 11.
-- [v5] Los tipos de documento los carga 07_datos_tipo_documento.sql. Cambios respecto del script 18 de v4:
--   * AREA.linea guarda SALON o SPA (la tabla LINEA se retiró);
--   * cada SERVICIO indica el tipo de recurso que ocupa (SERVICIO.tipo_recurso, antes SERVICIO_RECURSO);
--   * el stock mínimo es por sede (INVENTARIO_SEDE.stock_minimo; PRODUCTO ya no lo guarda);
--   * la primera cuenta se crea como PERSONA (con su DNI en TIPO_DOCUMENTO) + PERSONAL + USUARIO.
--
-- Fuente: caso Cautiva Spa, matriz (hoja 7. CATÁLOGO MUESTRA) y reglas de negocio.
-- Los valores marcados con [COMPLETAR] no están en los documentos: revíselos con la dueña.
-- Como se ejecuta sin usuario del BackOffice (modo mantenimiento), no deja filas en AUDITORIA.
-- =====================================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT Parámetros del negocio...
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('IGV_PORCENTAJE',        'IGV: 0 % por la exoneración de la Amazonía, Ley 27037 (RN06)', 0);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('RMV_MONTO',             'RMV desde el 01/10/2026 (D.S. 015-2026-TR); solo para el aviso RF61 (RN22)', 1230);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('UMBRAL_BOLETA_DOC',     'Boleta mayor a este monto exige datos del comprador (RN08)', 700);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('LIMPIEZA_MAX_MIN',      'Tiempo máximo de limpieza de un recurso en minutos (RN03)', 30);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('SESION_INACTIVIDAD_MIN','Cierre de sesión por inactividad en minutos (RF79)', 30);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('LOGIN_INTENTOS_MAX',    'Intentos fallidos seguidos antes del bloqueo temporal (RF77)', 5);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('LOGIN_BLOQUEO_MIN',     'Minutos de bloqueo tras los intentos fallidos (RF77)', 30);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('MARGEN_PRODUCTO_PCT',   'Margen sobre el costo de un producto normal (RN14)', 50);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('MARGEN_PREMIUM_PCT',    'Margen sobre el costo de un producto premium (RN14)', 100);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('INCIDENTE_AVISO_HORAS', 'Plazo para avisar a la autoridad un incidente de seguridad (RN28)', 48);
-- [v4] Integraciones (21/10/2026)
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('CPE_PLAZO_ENVIO_DIAS',  'Días calendario para que SUNAT reciba un comprobante o nota desde su emisión (RN47)', 3);
INSERT INTO PARAMETRO (codigo, descripcion, valor_numero) VALUES ('LINK_PAGO_VIGENCIA_HORAS', 'Horas de vigencia del enlace de pago de un adelanto (RF104)', 24);
INSERT INTO PARAMETRO (codigo, descripcion, valor_texto)  VALUES ('CPE_PROVEEDOR',         'Proveedor de facturación electrónica que envía los comprobantes a SUNAT (RES11)', 'APISPERU');
INSERT INTO PARAMETRO (codigo, descripcion, valor_texto)  VALUES ('CONSULTA_DOC_PROVEEDOR','Servicio de consulta DNI/RUC (RN45)', 'APISPERU');
INSERT INTO PARAMETRO (codigo, descripcion, valor_texto)  VALUES ('PASARELA_PROVEEDOR',    'Pasarela de pagos para tarjeta, Yape y Plin (RES08)', 'IZIPAY');

PROMPT Roles y permisos (BUC08, RES03, RES15, RES16, RES17)...
INSERT INTO ROL (nombre, descripcion) VALUES ('ADMINISTRADORA',       'Dueña: acceso total, define precios, comisiones y usuarios.');
INSERT INTO ROL (nombre, descripcion) VALUES ('RECEPCIONISTA',        'Clientas, agenda, atención sin cita, cobros, comprobantes y caja.');
INSERT INTO ROL (nombre, descripcion) VALUES ('ESPECIALISTA_TECNICA', 'Vista limitada: su agenda y el registro de sus atenciones.');
INSERT INTO ROL (nombre, descripcion) VALUES ('ENCARGADA_ALMACEN',    'Productos, kardex, compras y requerimientos.');

INSERT INTO PERMISO (modulo, accion, descripcion)
SELECT m.modulo, a.accion, a.accion || ' en ' || m.modulo
  FROM (SELECT 'AGENDA' modulo FROM dual UNION ALL SELECT 'CLIENTAS' FROM dual UNION ALL SELECT 'ATENCION' FROM dual
        UNION ALL SELECT 'VENTAS' FROM dual UNION ALL SELECT 'CAJA' FROM dual UNION ALL SELECT 'PRODUCTOS' FROM dual
        UNION ALL SELECT 'KARDEX' FROM dual UNION ALL SELECT 'COMPRAS' FROM dual UNION ALL SELECT 'PERSONAL' FROM dual
        UNION ALL SELECT 'ASISTENCIA' FROM dual UNION ALL SELECT 'PLANILLA' FROM dual UNION ALL SELECT 'CATALOGO' FROM dual
        UNION ALL SELECT 'REPORTES' FROM dual UNION ALL SELECT 'CONCILIACION' FROM dual UNION ALL SELECT 'SEGURIDAD' FROM dual
        UNION ALL SELECT 'SEDES' FROM dual UNION ALL SELECT 'RECORDATORIOS' FROM dual) m
  CROSS JOIN
       (SELECT 'VER' accion FROM dual UNION ALL SELECT 'CREAR' FROM dual UNION ALL SELECT 'EDITAR' FROM dual
        UNION ALL SELECT 'ANULAR' FROM dual UNION ALL SELECT 'EXPORTAR' FROM dual) a;

-- Administradora: todo
INSERT INTO ROL_PERMISO (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM ROL r CROSS JOIN PERMISO p WHERE r.nombre = 'ADMINISTRADORA';

-- Recepcionista
INSERT INTO ROL_PERMISO (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
  FROM ROL r JOIN PERMISO p ON (p.modulo, p.accion) IN (
        ('AGENDA','VER'),('AGENDA','CREAR'),('AGENDA','EDITAR'),
        ('CLIENTAS','VER'),('CLIENTAS','CREAR'),('CLIENTAS','EDITAR'),
        ('ATENCION','VER'),('ATENCION','CREAR'),
        ('VENTAS','VER'),('VENTAS','CREAR'),
        ('CAJA','VER'),('CAJA','CREAR'),('CAJA','EDITAR'),
        ('PRODUCTOS','VER'),('ASISTENCIA','VER'),('ASISTENCIA','CREAR'),
        ('RECORDATORIOS','VER'),('RECORDATORIOS','CREAR'))
 WHERE r.nombre = 'RECEPCIONISTA';

-- Especialista técnica (RES03: sin precios, costos, márgenes ni planilla)
INSERT INTO ROL_PERMISO (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
  FROM ROL r JOIN PERMISO p ON (p.modulo, p.accion) IN (
        ('AGENDA','VER'),('CLIENTAS','VER'),('ATENCION','VER'),('ATENCION','CREAR'),('ATENCION','EDITAR'),('PRODUCTOS','VER'))
 WHERE r.nombre = 'ESPECIALISTA_TECNICA';

-- Encargada de almacén (RES17)
INSERT INTO ROL_PERMISO (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
  FROM ROL r JOIN PERMISO p ON (p.modulo, p.accion) IN (
        ('PRODUCTOS','VER'),('PRODUCTOS','CREAR'),('PRODUCTOS','EDITAR'),
        ('KARDEX','VER'),('KARDEX','CREAR'),('COMPRAS','VER'),('COMPRAS','CREAR'),('COMPRAS','EDITAR'))
 WHERE r.nombre = 'ENCARGADA_ALMACEN';

PROMPT Sede y recursos (caso: 10 sillones, 5 estaciones, 3 camillas, 2 cabinas)...
INSERT INTO SEDE (nombre, direccion, horario_apertura, horario_cierre)
VALUES ('Pichanaki Centro', 'Pichanaki, Junín [COMPLETAR dirección]', '09:00', '20:00');

INSERT INTO RECURSO (id_sede, nombre, tipo)
SELECT s.id_sede, 'Sillón ' || n.n, 'SILLON' FROM SEDE s CROSS JOIN (SELECT LEVEL n FROM dual CONNECT BY LEVEL <= 10) n
 WHERE s.nombre = 'Pichanaki Centro';
INSERT INTO RECURSO (id_sede, nombre, tipo)
SELECT s.id_sede, 'Estación ' || n.n, 'ESTACION' FROM SEDE s CROSS JOIN (SELECT LEVEL n FROM dual CONNECT BY LEVEL <= 5) n
 WHERE s.nombre = 'Pichanaki Centro';
INSERT INTO RECURSO (id_sede, nombre, tipo)
SELECT s.id_sede, 'Camilla ' || n.n, 'CAMILLA' FROM SEDE s CROSS JOIN (SELECT LEVEL n FROM dual CONNECT BY LEVEL <= 3) n
 WHERE s.nombre = 'Pichanaki Centro';
INSERT INTO RECURSO (id_sede, nombre, tipo)
SELECT s.id_sede, 'Cabina facial ' || n.n, 'CABINA' FROM SEDE s CROSS JOIN (SELECT LEVEL n FROM dual CONNECT BY LEVEL <= 2) n
 WHERE s.nombre = 'Pichanaki Centro';
-- minutos_limpieza queda en 30 (máximo de RN03) [COMPLETAR: la dueña puede bajarlo por tipo de recurso]

PROMPT Áreas (con su línea) y medios de pago...
INSERT INTO AREA (linea, nombre) VALUES ('SALON', 'Peluquería y Estilismo');
INSERT INTO AREA (linea, nombre) VALUES ('SALON', 'Manicura y Pedicura');
INSERT INTO AREA (linea, nombre) VALUES ('SALON', 'Depilación');
INSERT INTO AREA (linea, nombre) VALUES ('SPA',   'Cuidado Facial');
INSERT INTO AREA (linea, nombre) VALUES ('SPA',   'Cabina Spa');

-- [v4] RN42: tarjeta, Yape y Plin se cobran por la pasarela; el efectivo en caja
INSERT INTO MEDIO_PAGO (nombre, usa_pasarela) VALUES ('EFECTIVO', 'N');
INSERT INTO MEDIO_PAGO (nombre, usa_pasarela) VALUES ('YAPE',     'S');
INSERT INTO MEDIO_PAGO (nombre, usa_pasarela) VALUES ('PLIN',     'S');
INSERT INTO MEDIO_PAGO (nombre, usa_pasarela) VALUES ('TARJETA',  'S');

PROMPT [v4] Series de comprobantes electrónicos de la sede (RF107)...
-- Series nuevas confirmadas por la contadora (21/10/2026), distintas de las usadas en SEE-SOL. Se firman con el CDT de la empresa.
INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) SELECT id_sede, 'BOLETA',     'B001' FROM SEDE WHERE nombre = 'Pichanaki Centro';
INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) SELECT id_sede, 'FACTURA',    'F001' FROM SEDE WHERE nombre = 'Pichanaki Centro';
INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) SELECT id_sede, 'NC_BOLETA',  'BC01' FROM SEDE WHERE nombre = 'Pichanaki Centro';
INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) SELECT id_sede, 'NC_FACTURA', 'FC01' FROM SEDE WHERE nombre = 'Pichanaki Centro';

PROMPT Servicios del catálogo de muestra (una variante Estándar con el precio base)...
-- [v5] Tipo de recurso por área [COMPLETAR: revisar con la dueña]: Peluquería -> SILLON, Manicura y Pedicura -> ESTACION,
-- Depilación y Cabina Spa -> CAMILLA, Cuidado Facial -> CABINA (las 2 cabinas faciales del caso).
DECLARE
    v_id_area     AREA.id_area%TYPE;
    v_id_servicio SERVICIO.id_servicio%TYPE;
BEGIN
    -- RTRIM: los literales de un UNION ALL se rellenan con espacios hasta el más largo
    FOR r IN (SELECT RTRIM(nombre) nombre, RTRIM(area) area, minutos, precio
                FROM (SELECT 'Corte de Cabello Dama (incluye lavado y secado)' nombre, 'Peluquería y Estilismo' area, 45 minutos, 60 precio FROM dual
                  UNION ALL SELECT 'Corte de Cabello Caballero', 'Peluquería y Estilismo', 30, 35 FROM dual
                  UNION ALL SELECT 'Tinte Completo / Color Global', 'Peluquería y Estilismo', 120, 120 FROM dual
                  UNION ALL SELECT 'Mechas Balayage / Iluminación', 'Peluquería y Estilismo', 240, 350 FROM dual
                  UNION ALL SELECT 'Alisado Brasileño de Keratina', 'Peluquería y Estilismo', 180, 250 FROM dual
                  UNION ALL SELECT 'Tratamiento Capilar (Bótox / Hidratación)', 'Peluquería y Estilismo', 90, 140 FROM dual
                  UNION ALL SELECT 'Cepillado y Planchado', 'Peluquería y Estilismo', 45, 40 FROM dual
                  UNION ALL SELECT 'Manicura Clásica', 'Manicura y Pedicura', 40, 25 FROM dual
                  UNION ALL SELECT 'Pedicura Clásica', 'Manicura y Pedicura', 50, 35 FROM dual
                  UNION ALL SELECT 'Esmaltado Semipermanente (Gel)', 'Manicura y Pedicura', 60, 60 FROM dual
                  UNION ALL SELECT 'Uñas Esculpidas (Acrílico o Soft Gel)', 'Manicura y Pedicura', 120, 120 FROM dual
                  UNION ALL SELECT 'Limpieza Facial Profunda Estándar', 'Cuidado Facial', 60, 80 FROM dual
                  UNION ALL SELECT 'Tratamiento Avanzado (Punta de Diamante / Hidrafacial)', 'Cuidado Facial', 75, 150 FROM dual
                  UNION ALL SELECT 'Perfilado de Cejas', 'Cuidado Facial', 20, 20 FROM dual
                  UNION ALL SELECT 'Lifting de Pestañas', 'Cuidado Facial', 60, 70 FROM dual
                  UNION ALL SELECT 'Depilación de Bozo (Cera o Hilo)', 'Depilación', 15, 15 FROM dual
                  UNION ALL SELECT 'Depilación Piernas Completas (Cera)', 'Depilación', 45, 60 FROM dual
                  UNION ALL SELECT 'Masaje Relajante o Descontracturante (Cuerpo completo)', 'Cabina Spa', 60, 90 FROM dual
                  UNION ALL SELECT 'Tratamiento Corporal (Exfoliación + Envoltura)', 'Cabina Spa', 90, 150 FROM dual
                  UNION ALL SELECT 'Paquete Full Day Spa / Circuito Relajación', 'Cabina Spa', 180, 220 FROM dual)) LOOP
        SELECT id_area INTO v_id_area FROM AREA WHERE nombre = r.area;
        INSERT INTO SERVICIO (id_area, nombre, duracion_ref_min, tipo_recurso)
        VALUES (v_id_area, r.nombre, r.minutos,
                CASE r.area WHEN 'Peluquería y Estilismo' THEN 'SILLON'
                            WHEN 'Manicura y Pedicura'    THEN 'ESTACION'
                            WHEN 'Cuidado Facial'         THEN 'CABINA'
                            ELSE 'CAMILLA' END)
        RETURNING id_servicio INTO v_id_servicio;
        INSERT INTO VARIANTE_SERVICIO (id_servicio, nombre, precio) VALUES (v_id_servicio, 'Estándar', r.precio);
    END LOOP;
END;
/

PROMPT Categorías y productos de muestra...
INSERT INTO CATEGORIA_PRODUCTO (nombre) VALUES ('Cuidado Capilar');
INSERT INTO CATEGORIA_PRODUCTO (nombre) VALUES ('Cuidado de Manos');
INSERT INTO CATEGORIA_PRODUCTO (nombre) VALUES ('Cuidado Facial');
INSERT INTO CATEGORIA_PRODUCTO (nombre) VALUES ('Insumos de servicio');

-- Costo = precio / 1.5 por RN14 (margen normal 50 %) [COMPLETAR con el costo real de la factura]
-- Stock mínimo = 25 % del stock inicial (el caso: se compra cuando el producto baja del 25 %); [v5] va en INVENTARIO_SEDE
-- El stock inicial NO se inserta aquí: entra con un movimiento de kardex (ENTRADA 'Stock inicial')
-- registrado por la encargada de almacén, para que quede trazado (RN18).
INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
SELECT id_categoria, 'CAP-001', 'Champú Post-Alisado Libre de Sulfatos 400ml', 'UND', 'VENTA', 65, ROUND(65 / 1.5, 2) FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Capilar';
INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
SELECT id_categoria, 'CAP-002', 'Acondicionador Reparación Intensa 400ml', 'UND', 'VENTA', 60, 40 FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Capilar';
INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
SELECT id_categoria, 'CAP-003', 'Óleo de Argán Tratamiento Hidratante 50ml', 'UND', 'VENTA', 50, ROUND(50 / 1.5, 2) FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Capilar';
INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
SELECT id_categoria, 'MAN-001', 'Crema de Manos Hidratante Intense Repair 75g', 'UND', 'VENTA', 28, ROUND(28 / 1.5, 2) FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado de Manos';
INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
SELECT id_categoria, 'MAN-002', 'Aceite para Cutículas Hidratante en Roll-on', 'UND', 'VENTA', 18, 12 FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado de Manos';
INSERT INTO PRODUCTO (id_categoria, codigo, nombre, unidad, tipo_uso, precio_venta, costo)
SELECT id_categoria, 'FAC-001', 'Sérum Facial de Ácido Hialurónico 30ml', 'UND', 'VENTA', 85, ROUND(85 / 1.5, 2) FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Facial';

-- Inventario de la sede (mínimo por sede; el stock lo pone el kardex)
INSERT INTO INVENTARIO_SEDE (id_producto, id_sede, stock_actual, stock_minimo)
SELECT p.id_producto, s.id_sede, 0,
       CASE p.codigo WHEN 'CAP-001' THEN 5 WHEN 'CAP-002' THEN 4 WHEN 'CAP-003' THEN 3
                     WHEN 'MAN-001' THEN 7 WHEN 'MAN-002' THEN 8 WHEN 'FAC-001' THEN 3 ELSE 0 END
  FROM PRODUCTO p CROSS JOIN SEDE s;

COMMIT;

-- =====================================================================
-- Primera cuenta: la Administradora
-- Descomente, complete los datos reales y ejecute. La clave_hash la genera el backend
-- (BCrypt o Argon2 con sal, RNF07): nunca guarde una contraseña en texto plano.
-- =====================================================================
-- [v5] Primero la persona (supertipo), luego su rol de personal (subtipo) y su cuenta.
-- INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono, correo)
-- SELECT 'NATURAL', 'Odalis', 'Tineo', id_tipo_documento, '[DNI]', '[TELÉFONO]', '[CORREO]'
--   FROM TIPO_DOCUMENTO WHERE abreviatura = 'DNI';
-- INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, fecha_ingreso)
-- SELECT p.id_persona, s.id_sede, 'ADMINISTRADORA', 'FIJO', 0, DATE '2026-01-01'
--   FROM PERSONA p, SEDE s WHERE p.numero_documento = '[DNI]' AND s.nombre = 'Pichanaki Centro';
-- INSERT INTO PERSONAL_AREA (id_personal, id_area)
-- SELECT p.id_persona, a.id_area FROM PERSONA p CROSS JOIN AREA a WHERE p.numero_documento = '[DNI]';
-- INSERT INTO USUARIO (id_personal, id_rol, nombre_usuario, clave_hash, debe_cambiar_clave)
-- SELECT p.id_persona, r.id_rol, 'odalis.tineo', '[HASH BCRYPT GENERADO POR EL BACKEND]', 'S'
--   FROM PERSONA p, ROL r WHERE p.numero_documento = '[DNI]' AND r.nombre = 'ADMINISTRADORA';
-- COMMIT;

PROMPT Datos iniciales cargados.
SELECT 'SERVICIOS' item, COUNT(*) total FROM SERVICIO UNION ALL
SELECT 'VARIANTES', COUNT(*) FROM VARIANTE_SERVICIO UNION ALL
SELECT 'RECURSOS',  COUNT(*) FROM RECURSO UNION ALL
SELECT 'PRODUCTOS', COUNT(*) FROM PRODUCTO UNION ALL
SELECT 'PERMISOS',  COUNT(*) FROM PERMISO UNION ALL
SELECT 'SERIES',    COUNT(*) FROM SERIE_COMPROBANTE UNION ALL
SELECT 'TIPOS_DOC', COUNT(*) FROM TIPO_DOCUMENTO UNION ALL
SELECT 'ROL_PERMISO', COUNT(*) FROM ROL_PERMISO;
WHENEVER SQLERROR CONTINUE
