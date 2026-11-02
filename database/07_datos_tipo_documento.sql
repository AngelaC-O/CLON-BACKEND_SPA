-- =============================================================================
-- Cautiva Spa E.I.R.L. · Base de datos Oracle 19c · VERSIÓN 5
-- 07 · Datos fijos del catálogo TIPO_DOCUMENTO (Catálogo N° 06 de SUNAT)
-- Las longitudes de carnet, pasaporte y cédula son datos: se ajustan con un UPDATE
-- sin tocar el esquema. Solo DNI y RUC se consultan en APISPERU (consulta_api = 'S').
-- =============================================================================
SET DEFINE OFF

INSERT INTO TIPO_DOCUMENTO (codigo_sunat, nombre, abreviatura, longitud_min, longitud_max, solo_numeros, consulta_api)
VALUES ('1', 'Documento Nacional de Identidad', 'DNI', 8, 8, 'S', 'S');
INSERT INTO TIPO_DOCUMENTO (codigo_sunat, nombre, abreviatura, longitud_min, longitud_max, solo_numeros, consulta_api)
VALUES ('4', 'Carnet de Extranjería', 'CE', 8, 12, 'N', 'N');
INSERT INTO TIPO_DOCUMENTO (codigo_sunat, nombre, abreviatura, longitud_min, longitud_max, solo_numeros, consulta_api)
VALUES ('6', 'Registro Único de Contribuyentes', 'RUC', 11, 11, 'S', 'S');
INSERT INTO TIPO_DOCUMENTO (codigo_sunat, nombre, abreviatura, longitud_min, longitud_max, solo_numeros, consulta_api)
VALUES ('7', 'Pasaporte', 'PASAPORTE', 6, 12, 'N', 'N');
INSERT INTO TIPO_DOCUMENTO (codigo_sunat, nombre, abreviatura, longitud_min, longitud_max, solo_numeros, consulta_api)
VALUES ('A', 'Cédula Diplomática de Identidad', 'CDI', 6, 15, 'N', 'N');
COMMIT;

PROMPT 07_datos_tipo_documento.sql terminado: 5 tipos de documento.
