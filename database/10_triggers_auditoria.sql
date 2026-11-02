-- =====================================================================
-- CAUTIVA SPA E.I.R.L. · BackOffice web · Base de datos Oracle (12c o superior)
-- Script 10 (v5) · Triggers de auditoría (RN29, RF80, RNF24)
-- Se ejecuta como: dueño del esquema v5, después de 09_triggers_reglas.sql.
-- Cada tabla registra en AUDITORIA quién creó, cambió, anuló o eliminó un dato, cuándo, y el valor
-- anterior y el nuevo. El usuario lo identifica el backend con PKG_SESION.iniciar(id_usuario).
-- [v5] 54 tablas (las 55 del modelo menos AUDITORIA). En la jerarquía PERSONA el supertipo y cada
-- subtipo tienen su propio trigger: un alta de clienta deja dos filas (PERSONA y CLIENTE) con el mismo id.
-- Generado automáticamente desde gen/model.py con gen/gen_auditoria.py: no editar a mano.
-- =====================================================================
SET DEFINE OFF

PROMPT Trigger TRG_AUD_SEDE...
CREATE OR REPLACE TRIGGER TRG_AUD_SEDE
AFTER INSERT OR UPDATE OR DELETE ON SEDE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('direccion', :NEW.direccion) || PKG_SESION.par('horario_apertura', :NEW.horario_apertura) || PKG_SESION.par('horario_cierre', :NEW.horario_cierre) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Sedes y recursos', 'CREAR', 'SEDE', :NEW.id_sede, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('direccion', :OLD.direccion, :NEW.direccion);
        v_det := v_det || PKG_SESION.diff('horario_apertura', :OLD.horario_apertura, :NEW.horario_apertura);
        v_det := v_det || PKG_SESION.diff('horario_cierre', :OLD.horario_cierre, :NEW.horario_cierre);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Sedes y recursos', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'SEDE', :NEW.id_sede, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('direccion', :OLD.direccion) || PKG_SESION.par('horario_apertura', :OLD.horario_apertura) || PKG_SESION.par('horario_cierre', :OLD.horario_cierre) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Sedes y recursos', 'ELIMINAR', 'SEDE', :OLD.id_sede, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_ROL...
CREATE OR REPLACE TRIGGER TRG_AUD_ROL
AFTER INSERT OR UPDATE OR DELETE ON ROL
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_rol', TO_CHAR(:NEW.id_rol)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('descripcion', :NEW.descripcion) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Seguridad y auditoría', 'CREAR', 'ROL', :NEW.id_rol, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('descripcion', :OLD.descripcion, :NEW.descripcion);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Seguridad y auditoría', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'ROL', :NEW.id_rol, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_rol', TO_CHAR(:OLD.id_rol)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('descripcion', :OLD.descripcion) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Seguridad y auditoría', 'ELIMINAR', 'ROL', :OLD.id_rol, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PERMISO...
CREATE OR REPLACE TRIGGER TRG_AUD_PERMISO
AFTER INSERT OR UPDATE OR DELETE ON PERMISO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_permiso', TO_CHAR(:NEW.id_permiso)) || PKG_SESION.par('modulo', :NEW.modulo) || PKG_SESION.par('accion', :NEW.accion) || PKG_SESION.par('descripcion', :NEW.descripcion);
        PKG_SESION.auditar('Seguridad y auditoría', 'CREAR', 'PERMISO', :NEW.id_permiso, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('modulo', :OLD.modulo, :NEW.modulo);
        v_det := v_det || PKG_SESION.diff('accion', :OLD.accion, :NEW.accion);
        v_det := v_det || PKG_SESION.diff('descripcion', :OLD.descripcion, :NEW.descripcion);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Seguridad y auditoría', 'EDITAR', 'PERMISO', :NEW.id_permiso, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_permiso', TO_CHAR(:OLD.id_permiso)) || PKG_SESION.par('modulo', :OLD.modulo) || PKG_SESION.par('accion', :OLD.accion) || PKG_SESION.par('descripcion', :OLD.descripcion);
        PKG_SESION.auditar('Seguridad y auditoría', 'ELIMINAR', 'PERMISO', :OLD.id_permiso, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_ROL_PERMISO...
CREATE OR REPLACE TRIGGER TRG_AUD_ROL_PERMISO
AFTER INSERT OR UPDATE OR DELETE ON ROL_PERMISO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_rol', TO_CHAR(:NEW.id_rol)) || PKG_SESION.par('id_permiso', TO_CHAR(:NEW.id_permiso));
        PKG_SESION.auditar('Seguridad y auditoría', 'CREAR', 'ROL_PERMISO', :NEW.id_rol, v_det, NULL);
    ELSIF UPDATING THEN
        NULL;
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Seguridad y auditoría', 'EDITAR', 'ROL_PERMISO', :NEW.id_rol, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_rol', TO_CHAR(:OLD.id_rol)) || PKG_SESION.par('id_permiso', TO_CHAR(:OLD.id_permiso));
        PKG_SESION.auditar('Seguridad y auditoría', 'ELIMINAR', 'ROL_PERMISO', :OLD.id_rol, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_TIPO_DOCUMENTO...
CREATE OR REPLACE TRIGGER TRG_AUD_TIPO_DOCUMENTO
AFTER INSERT OR UPDATE OR DELETE ON TIPO_DOCUMENTO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_tipo_documento', TO_CHAR(:NEW.id_tipo_documento)) || PKG_SESION.par('codigo_sunat', :NEW.codigo_sunat) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('abreviatura', :NEW.abreviatura) || PKG_SESION.par('longitud_min', TO_CHAR(:NEW.longitud_min)) || PKG_SESION.par('longitud_max', TO_CHAR(:NEW.longitud_max)) || PKG_SESION.par('solo_numeros', :NEW.solo_numeros) || PKG_SESION.par('consulta_api', :NEW.consulta_api) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Personas', 'CREAR', 'TIPO_DOCUMENTO', :NEW.id_tipo_documento, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('codigo_sunat', :OLD.codigo_sunat, :NEW.codigo_sunat);
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('abreviatura', :OLD.abreviatura, :NEW.abreviatura);
        v_det := v_det || PKG_SESION.diff('longitud_min', TO_CHAR(:OLD.longitud_min), TO_CHAR(:NEW.longitud_min));
        v_det := v_det || PKG_SESION.diff('longitud_max', TO_CHAR(:OLD.longitud_max), TO_CHAR(:NEW.longitud_max));
        v_det := v_det || PKG_SESION.diff('solo_numeros', :OLD.solo_numeros, :NEW.solo_numeros);
        v_det := v_det || PKG_SESION.diff('consulta_api', :OLD.consulta_api, :NEW.consulta_api);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personas', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'TIPO_DOCUMENTO', :NEW.id_tipo_documento, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_tipo_documento', TO_CHAR(:OLD.id_tipo_documento)) || PKG_SESION.par('codigo_sunat', :OLD.codigo_sunat) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('abreviatura', :OLD.abreviatura) || PKG_SESION.par('longitud_min', TO_CHAR(:OLD.longitud_min)) || PKG_SESION.par('longitud_max', TO_CHAR(:OLD.longitud_max)) || PKG_SESION.par('solo_numeros', :OLD.solo_numeros) || PKG_SESION.par('consulta_api', :OLD.consulta_api) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Personas', 'ELIMINAR', 'TIPO_DOCUMENTO', :OLD.id_tipo_documento, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PERSONA...
CREATE OR REPLACE TRIGGER TRG_AUD_PERSONA
AFTER INSERT OR UPDATE OR DELETE ON PERSONA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_persona', TO_CHAR(:NEW.id_persona)) || PKG_SESION.par('tipo_persona', :NEW.tipo_persona) || PKG_SESION.par('nombres', :NEW.nombres) || PKG_SESION.par('apellidos', :NEW.apellidos) || PKG_SESION.par('razon_social', :NEW.razon_social) || PKG_SESION.par('id_tipo_documento', TO_CHAR(:NEW.id_tipo_documento)) || PKG_SESION.par('numero_documento', :NEW.numero_documento) || PKG_SESION.par('telefono', :NEW.telefono) || PKG_SESION.par('correo', :NEW.correo) || PKG_SESION.par('origen_datos', :NEW.origen_datos) || PKG_SESION.par('fecha_verificacion', TO_CHAR(:NEW.fecha_verificacion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_registro', TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Personas', 'CREAR', 'PERSONA', :NEW.id_persona, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('tipo_persona', :OLD.tipo_persona, :NEW.tipo_persona);
        v_det := v_det || PKG_SESION.diff('nombres', :OLD.nombres, :NEW.nombres);
        v_det := v_det || PKG_SESION.diff('apellidos', :OLD.apellidos, :NEW.apellidos);
        v_det := v_det || PKG_SESION.diff('razon_social', :OLD.razon_social, :NEW.razon_social);
        v_det := v_det || PKG_SESION.diff('id_tipo_documento', TO_CHAR(:OLD.id_tipo_documento), TO_CHAR(:NEW.id_tipo_documento));
        v_det := v_det || PKG_SESION.diff('numero_documento', :OLD.numero_documento, :NEW.numero_documento);
        v_det := v_det || PKG_SESION.diff('telefono', :OLD.telefono, :NEW.telefono);
        v_det := v_det || PKG_SESION.diff('correo', :OLD.correo, :NEW.correo);
        v_det := v_det || PKG_SESION.diff('origen_datos', :OLD.origen_datos, :NEW.origen_datos);
        v_det := v_det || PKG_SESION.diff('fecha_verificacion', TO_CHAR(:OLD.fecha_verificacion, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_verificacion, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personas', 'EDITAR', 'PERSONA', :NEW.id_persona, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_persona', TO_CHAR(:OLD.id_persona)) || PKG_SESION.par('tipo_persona', :OLD.tipo_persona) || PKG_SESION.par('nombres', :OLD.nombres) || PKG_SESION.par('apellidos', :OLD.apellidos) || PKG_SESION.par('razon_social', :OLD.razon_social) || PKG_SESION.par('id_tipo_documento', TO_CHAR(:OLD.id_tipo_documento)) || PKG_SESION.par('numero_documento', :OLD.numero_documento) || PKG_SESION.par('telefono', :OLD.telefono) || PKG_SESION.par('correo', :OLD.correo) || PKG_SESION.par('origen_datos', :OLD.origen_datos) || PKG_SESION.par('fecha_verificacion', TO_CHAR(:OLD.fecha_verificacion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Personas', 'ELIMINAR', 'PERSONA', :OLD.id_persona, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CLIENTE...
CREATE OR REPLACE TRIGGER TRG_AUD_CLIENTE
AFTER INSERT OR UPDATE OR DELETE ON CLIENTE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_cliente', TO_CHAR(:NEW.id_cliente)) || PKG_SESION.par('fecha_cumpleanos', TO_CHAR(:NEW.fecha_cumpleanos, 'YYYY-MM-DD')) || PKG_SESION.par('acepta_comunicaciones', :NEW.acepta_comunicaciones) || PKG_SESION.par('fecha_consentimiento', TO_CHAR(:NEW.fecha_consentimiento, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('observaciones', :NEW.observaciones) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Clientas, agenda y atención', 'CREAR', 'CLIENTE', :NEW.id_cliente, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('fecha_cumpleanos', TO_CHAR(:OLD.fecha_cumpleanos, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_cumpleanos, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('acepta_comunicaciones', :OLD.acepta_comunicaciones, :NEW.acepta_comunicaciones);
        v_det := v_det || PKG_SESION.diff('fecha_consentimiento', TO_CHAR(:OLD.fecha_consentimiento, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_consentimiento, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('observaciones', :OLD.observaciones, :NEW.observaciones);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Clientas, agenda y atención', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'CLIENTE', :NEW.id_cliente, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_cliente', TO_CHAR(:OLD.id_cliente)) || PKG_SESION.par('fecha_cumpleanos', TO_CHAR(:OLD.fecha_cumpleanos, 'YYYY-MM-DD')) || PKG_SESION.par('acepta_comunicaciones', :OLD.acepta_comunicaciones) || PKG_SESION.par('fecha_consentimiento', TO_CHAR(:OLD.fecha_consentimiento, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('observaciones', :OLD.observaciones) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Clientas, agenda y atención', 'ELIMINAR', 'CLIENTE', :OLD.id_cliente, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PERSONAL...
CREATE OR REPLACE TRIGGER TRG_AUD_PERSONAL
AFTER INSERT OR UPDATE OR DELETE ON PERSONAL
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('cargo', :NEW.cargo) || PKG_SESION.par('modalidad_pago', :NEW.modalidad_pago) || PKG_SESION.par('sueldo_base', TO_CHAR(:NEW.sueldo_base)) || PKG_SESION.par('porcentaje_comision', TO_CHAR(:NEW.porcentaje_comision)) || PKG_SESION.par('fecha_ingreso', TO_CHAR(:NEW.fecha_ingreso, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Personal, asistencia y planilla', 'CREAR', 'PERSONAL', :NEW.id_personal, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('cargo', :OLD.cargo, :NEW.cargo);
        v_det := v_det || PKG_SESION.diff('modalidad_pago', :OLD.modalidad_pago, :NEW.modalidad_pago);
        v_det := v_det || PKG_SESION.diff('sueldo_base', TO_CHAR(:OLD.sueldo_base), TO_CHAR(:NEW.sueldo_base));
        v_det := v_det || PKG_SESION.diff('porcentaje_comision', TO_CHAR(:OLD.porcentaje_comision), TO_CHAR(:NEW.porcentaje_comision));
        v_det := v_det || PKG_SESION.diff('fecha_ingreso', TO_CHAR(:OLD.fecha_ingreso, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_ingreso, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personal, asistencia y planilla', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'PERSONAL', :NEW.id_personal, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('cargo', :OLD.cargo) || PKG_SESION.par('modalidad_pago', :OLD.modalidad_pago) || PKG_SESION.par('sueldo_base', TO_CHAR(:OLD.sueldo_base)) || PKG_SESION.par('porcentaje_comision', TO_CHAR(:OLD.porcentaje_comision)) || PKG_SESION.par('fecha_ingreso', TO_CHAR(:OLD.fecha_ingreso, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Personal, asistencia y planilla', 'ELIMINAR', 'PERSONAL', :OLD.id_personal, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PROVEEDOR...
CREATE OR REPLACE TRIGGER TRG_AUD_PROVEEDOR
AFTER INSERT OR UPDATE OR DELETE ON PROVEEDOR
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_proveedor', TO_CHAR(:NEW.id_proveedor)) || PKG_SESION.par('contacto', :NEW.contacto) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'PROVEEDOR', :NEW.id_proveedor, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('contacto', :OLD.contacto, :NEW.contacto);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'PROVEEDOR', :NEW.id_proveedor, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_proveedor', TO_CHAR(:OLD.id_proveedor)) || PKG_SESION.par('contacto', :OLD.contacto) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'PROVEEDOR', :OLD.id_proveedor, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_USUARIO...
CREATE OR REPLACE TRIGGER TRG_AUD_USUARIO
AFTER INSERT OR UPDATE OR DELETE ON USUARIO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('id_rol', TO_CHAR(:NEW.id_rol)) || PKG_SESION.par('nombre_usuario', :NEW.nombre_usuario) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('intentos_fallidos', TO_CHAR(:NEW.intentos_fallidos)) || PKG_SESION.par('debe_cambiar_clave', :NEW.debe_cambiar_clave) || PKG_SESION.par('bloqueado_hasta', TO_CHAR(:NEW.bloqueado_hasta, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('ultimo_acceso', TO_CHAR(:NEW.ultimo_acceso, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_creacion', TO_CHAR(:NEW.fecha_creacion, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Seguridad y auditoría', 'CREAR', 'USUARIO', :NEW.id_usuario, v_det, :NEW.id_usuario);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_personal', TO_CHAR(:OLD.id_personal), TO_CHAR(:NEW.id_personal));
        v_det := v_det || PKG_SESION.diff('id_rol', TO_CHAR(:OLD.id_rol), TO_CHAR(:NEW.id_rol));
        v_det := v_det || PKG_SESION.diff('nombre_usuario', :OLD.nombre_usuario, :NEW.nombre_usuario);
        IF PKG_SESION.distinto(:OLD.clave_hash, :NEW.clave_hash) THEN v_det := v_det || 'clave_hash: (cambiada); '; END IF;
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('intentos_fallidos', TO_CHAR(:OLD.intentos_fallidos), TO_CHAR(:NEW.intentos_fallidos));
        v_det := v_det || PKG_SESION.diff('debe_cambiar_clave', :OLD.debe_cambiar_clave, :NEW.debe_cambiar_clave);
        v_det := v_det || PKG_SESION.diff('bloqueado_hasta', TO_CHAR(:OLD.bloqueado_hasta, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.bloqueado_hasta, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('ultimo_acceso', TO_CHAR(:OLD.ultimo_acceso, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.ultimo_acceso, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('fecha_creacion', TO_CHAR(:OLD.fecha_creacion, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_creacion, 'YYYY-MM-DD HH24:MI:SS'));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Seguridad y auditoría', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'USUARIO', :NEW.id_usuario, v_det, :NEW.id_usuario);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('id_rol', TO_CHAR(:OLD.id_rol)) || PKG_SESION.par('nombre_usuario', :OLD.nombre_usuario) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('intentos_fallidos', TO_CHAR(:OLD.intentos_fallidos)) || PKG_SESION.par('debe_cambiar_clave', :OLD.debe_cambiar_clave) || PKG_SESION.par('bloqueado_hasta', TO_CHAR(:OLD.bloqueado_hasta, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('ultimo_acceso', TO_CHAR(:OLD.ultimo_acceso, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_creacion', TO_CHAR(:OLD.fecha_creacion, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Seguridad y auditoría', 'ELIMINAR', 'USUARIO', :OLD.id_usuario, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PARAMETRO...
CREATE OR REPLACE TRIGGER TRG_AUD_PARAMETRO
AFTER INSERT OR UPDATE OR DELETE ON PARAMETRO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_parametro', TO_CHAR(:NEW.id_parametro)) || PKG_SESION.par('codigo', :NEW.codigo) || PKG_SESION.par('descripcion', :NEW.descripcion) || PKG_SESION.par('valor_numero', TO_CHAR(:NEW.valor_numero)) || PKG_SESION.par('valor_texto', :NEW.valor_texto) || PKG_SESION.par('fecha_vigencia', TO_CHAR(:NEW.fecha_vigencia, 'YYYY-MM-DD')) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario));
        PKG_SESION.auditar('Seguridad y auditoría', 'CREAR', 'PARAMETRO', :NEW.id_parametro, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('codigo', :OLD.codigo, :NEW.codigo);
        v_det := v_det || PKG_SESION.diff('descripcion', :OLD.descripcion, :NEW.descripcion);
        v_det := v_det || PKG_SESION.diff('valor_numero', TO_CHAR(:OLD.valor_numero), TO_CHAR(:NEW.valor_numero));
        v_det := v_det || PKG_SESION.diff('valor_texto', :OLD.valor_texto, :NEW.valor_texto);
        v_det := v_det || PKG_SESION.diff('fecha_vigencia', TO_CHAR(:OLD.fecha_vigencia, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_vigencia, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Seguridad y auditoría', 'EDITAR', 'PARAMETRO', :NEW.id_parametro, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_parametro', TO_CHAR(:OLD.id_parametro)) || PKG_SESION.par('codigo', :OLD.codigo) || PKG_SESION.par('descripcion', :OLD.descripcion) || PKG_SESION.par('valor_numero', TO_CHAR(:OLD.valor_numero)) || PKG_SESION.par('valor_texto', :OLD.valor_texto) || PKG_SESION.par('fecha_vigencia', TO_CHAR(:OLD.fecha_vigencia, 'YYYY-MM-DD')) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario));
        PKG_SESION.auditar('Seguridad y auditoría', 'ELIMINAR', 'PARAMETRO', :OLD.id_parametro, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_INCIDENTE_SEGURIDAD...
CREATE OR REPLACE TRIGGER TRG_AUD_INCIDENTE_SEGURIDAD
AFTER INSERT OR UPDATE OR DELETE ON INCIDENTE_SEGURIDAD
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_incidente', TO_CHAR(:NEW.id_incidente)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('fecha_deteccion', TO_CHAR(:NEW.fecha_deteccion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('descripcion', :NEW.descripcion) || PKG_SESION.par('datos_afectados', :NEW.datos_afectados) || PKG_SESION.par('medidas', :NEW.medidas) || PKG_SESION.par('fecha_aviso', TO_CHAR(:NEW.fecha_aviso, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('numero_aviso', :NEW.numero_aviso) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('fecha_registro', TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Seguridad y auditoría', 'CREAR', 'INCIDENTE_SEGURIDAD', :NEW.id_incidente, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('fecha_deteccion', TO_CHAR(:OLD.fecha_deteccion, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_deteccion, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('descripcion', :OLD.descripcion, :NEW.descripcion);
        v_det := v_det || PKG_SESION.diff('datos_afectados', :OLD.datos_afectados, :NEW.datos_afectados);
        v_det := v_det || PKG_SESION.diff('medidas', :OLD.medidas, :NEW.medidas);
        v_det := v_det || PKG_SESION.diff('fecha_aviso', TO_CHAR(:OLD.fecha_aviso, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_aviso, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('numero_aviso', :OLD.numero_aviso, :NEW.numero_aviso);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Seguridad y auditoría', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'INCIDENTE_SEGURIDAD', :NEW.id_incidente, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_incidente', TO_CHAR(:OLD.id_incidente)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('fecha_deteccion', TO_CHAR(:OLD.fecha_deteccion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('descripcion', :OLD.descripcion) || PKG_SESION.par('datos_afectados', :OLD.datos_afectados) || PKG_SESION.par('medidas', :OLD.medidas) || PKG_SESION.par('fecha_aviso', TO_CHAR(:OLD.fecha_aviso, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('numero_aviso', :OLD.numero_aviso) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Seguridad y auditoría', 'ELIMINAR', 'INCIDENTE_SEGURIDAD', :OLD.id_incidente, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PERSONAL_AREA...
CREATE OR REPLACE TRIGGER TRG_AUD_PERSONAL_AREA
AFTER INSERT OR UPDATE OR DELETE ON PERSONAL_AREA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('id_area', TO_CHAR(:NEW.id_area));
        PKG_SESION.auditar('Personal, asistencia y planilla', 'CREAR', 'PERSONAL_AREA', :NEW.id_personal, v_det, NULL);
    ELSIF UPDATING THEN
        NULL;
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personal, asistencia y planilla', 'EDITAR', 'PERSONAL_AREA', :NEW.id_personal, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('id_area', TO_CHAR(:OLD.id_area));
        PKG_SESION.auditar('Personal, asistencia y planilla', 'ELIMINAR', 'PERSONAL_AREA', :OLD.id_personal, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_HORARIO_PERSONAL...
CREATE OR REPLACE TRIGGER TRG_AUD_HORARIO_PERSONAL
AFTER INSERT OR UPDATE OR DELETE ON HORARIO_PERSONAL
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_horario', TO_CHAR(:NEW.id_horario)) || PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('dia_semana', TO_CHAR(:NEW.dia_semana)) || PKG_SESION.par('hora_entrada', :NEW.hora_entrada) || PKG_SESION.par('hora_salida', :NEW.hora_salida) || PKG_SESION.par('minutos_almuerzo', TO_CHAR(:NEW.minutos_almuerzo));
        PKG_SESION.auditar('Personal, asistencia y planilla', 'CREAR', 'HORARIO_PERSONAL', :NEW.id_horario, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_personal', TO_CHAR(:OLD.id_personal), TO_CHAR(:NEW.id_personal));
        v_det := v_det || PKG_SESION.diff('dia_semana', TO_CHAR(:OLD.dia_semana), TO_CHAR(:NEW.dia_semana));
        v_det := v_det || PKG_SESION.diff('hora_entrada', :OLD.hora_entrada, :NEW.hora_entrada);
        v_det := v_det || PKG_SESION.diff('hora_salida', :OLD.hora_salida, :NEW.hora_salida);
        v_det := v_det || PKG_SESION.diff('minutos_almuerzo', TO_CHAR(:OLD.minutos_almuerzo), TO_CHAR(:NEW.minutos_almuerzo));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personal, asistencia y planilla', 'EDITAR', 'HORARIO_PERSONAL', :NEW.id_horario, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_horario', TO_CHAR(:OLD.id_horario)) || PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('dia_semana', TO_CHAR(:OLD.dia_semana)) || PKG_SESION.par('hora_entrada', :OLD.hora_entrada) || PKG_SESION.par('hora_salida', :OLD.hora_salida) || PKG_SESION.par('minutos_almuerzo', TO_CHAR(:OLD.minutos_almuerzo));
        PKG_SESION.auditar('Personal, asistencia y planilla', 'ELIMINAR', 'HORARIO_PERSONAL', :OLD.id_horario, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_ASISTENCIA...
CREATE OR REPLACE TRIGGER TRG_AUD_ASISTENCIA
AFTER INSERT OR UPDATE OR DELETE ON ASISTENCIA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_asistencia', TO_CHAR(:NEW.id_asistencia)) || PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('fecha', TO_CHAR(:NEW.fecha, 'YYYY-MM-DD')) || PKG_SESION.par('hora_entrada', TO_CHAR(:NEW.hora_entrada, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('hora_inicio_almuerzo', TO_CHAR(:NEW.hora_inicio_almuerzo, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('hora_fin_almuerzo', TO_CHAR(:NEW.hora_fin_almuerzo, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('hora_salida', TO_CHAR(:NEW.hora_salida, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('minutos_tardanza', TO_CHAR(:NEW.minutos_tardanza)) || PKG_SESION.par('horas_trabajadas', TO_CHAR(:NEW.horas_trabajadas)) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario));
        PKG_SESION.auditar('Personal, asistencia y planilla', 'CREAR', 'ASISTENCIA', :NEW.id_asistencia, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_personal', TO_CHAR(:OLD.id_personal), TO_CHAR(:NEW.id_personal));
        v_det := v_det || PKG_SESION.diff('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('hora_entrada', TO_CHAR(:OLD.hora_entrada, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.hora_entrada, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('hora_inicio_almuerzo', TO_CHAR(:OLD.hora_inicio_almuerzo, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.hora_inicio_almuerzo, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('hora_fin_almuerzo', TO_CHAR(:OLD.hora_fin_almuerzo, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.hora_fin_almuerzo, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('hora_salida', TO_CHAR(:OLD.hora_salida, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.hora_salida, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('minutos_tardanza', TO_CHAR(:OLD.minutos_tardanza), TO_CHAR(:NEW.minutos_tardanza));
        v_det := v_det || PKG_SESION.diff('horas_trabajadas', TO_CHAR(:OLD.horas_trabajadas), TO_CHAR(:NEW.horas_trabajadas));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personal, asistencia y planilla', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'ASISTENCIA', :NEW.id_asistencia, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_asistencia', TO_CHAR(:OLD.id_asistencia)) || PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD')) || PKG_SESION.par('hora_entrada', TO_CHAR(:OLD.hora_entrada, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('hora_inicio_almuerzo', TO_CHAR(:OLD.hora_inicio_almuerzo, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('hora_fin_almuerzo', TO_CHAR(:OLD.hora_fin_almuerzo, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('hora_salida', TO_CHAR(:OLD.hora_salida, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('minutos_tardanza', TO_CHAR(:OLD.minutos_tardanza)) || PKG_SESION.par('horas_trabajadas', TO_CHAR(:OLD.horas_trabajadas)) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario));
        PKG_SESION.auditar('Personal, asistencia y planilla', 'ELIMINAR', 'ASISTENCIA', :OLD.id_asistencia, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PLANILLA...
CREATE OR REPLACE TRIGGER TRG_AUD_PLANILLA
AFTER INSERT OR UPDATE OR DELETE ON PLANILLA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_planilla', TO_CHAR(:NEW.id_planilla)) || PKG_SESION.par('anio', TO_CHAR(:NEW.anio)) || PKG_SESION.par('mes', TO_CHAR(:NEW.mes)) || PKG_SESION.par('fecha_calculo', TO_CHAR(:NEW.fecha_calculo, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Personal, asistencia y planilla', 'CREAR', 'PLANILLA', :NEW.id_planilla, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('anio', TO_CHAR(:OLD.anio), TO_CHAR(:NEW.anio));
        v_det := v_det || PKG_SESION.diff('mes', TO_CHAR(:OLD.mes), TO_CHAR(:NEW.mes));
        v_det := v_det || PKG_SESION.diff('fecha_calculo', TO_CHAR(:OLD.fecha_calculo, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_calculo, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personal, asistencia y planilla', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'PLANILLA', :NEW.id_planilla, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_planilla', TO_CHAR(:OLD.id_planilla)) || PKG_SESION.par('anio', TO_CHAR(:OLD.anio)) || PKG_SESION.par('mes', TO_CHAR(:OLD.mes)) || PKG_SESION.par('fecha_calculo', TO_CHAR(:OLD.fecha_calculo, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Personal, asistencia y planilla', 'ELIMINAR', 'PLANILLA', :OLD.id_planilla, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PLANILLA_DETALLE...
CREATE OR REPLACE TRIGGER TRG_AUD_PLANILLA_DETALLE
AFTER INSERT OR UPDATE OR DELETE ON PLANILLA_DETALLE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_detalle_planilla', TO_CHAR(:NEW.id_detalle_planilla)) || PKG_SESION.par('id_planilla', TO_CHAR(:NEW.id_planilla)) || PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('sueldo_base', TO_CHAR(:NEW.sueldo_base)) || PKG_SESION.par('comision', TO_CHAR(:NEW.comision)) || PKG_SESION.par('descuento_faltas', TO_CHAR(:NEW.descuento_faltas)) || PKG_SESION.par('descuento_tardanzas', TO_CHAR(:NEW.descuento_tardanzas)) || PKG_SESION.par('adelantos', TO_CHAR(:NEW.adelantos)) || PKG_SESION.par('neto', TO_CHAR(:NEW.neto));
        PKG_SESION.auditar('Personal, asistencia y planilla', 'CREAR', 'PLANILLA_DETALLE', :NEW.id_detalle_planilla, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_planilla', TO_CHAR(:OLD.id_planilla), TO_CHAR(:NEW.id_planilla));
        v_det := v_det || PKG_SESION.diff('id_personal', TO_CHAR(:OLD.id_personal), TO_CHAR(:NEW.id_personal));
        v_det := v_det || PKG_SESION.diff('sueldo_base', TO_CHAR(:OLD.sueldo_base), TO_CHAR(:NEW.sueldo_base));
        v_det := v_det || PKG_SESION.diff('comision', TO_CHAR(:OLD.comision), TO_CHAR(:NEW.comision));
        v_det := v_det || PKG_SESION.diff('descuento_faltas', TO_CHAR(:OLD.descuento_faltas), TO_CHAR(:NEW.descuento_faltas));
        v_det := v_det || PKG_SESION.diff('descuento_tardanzas', TO_CHAR(:OLD.descuento_tardanzas), TO_CHAR(:NEW.descuento_tardanzas));
        v_det := v_det || PKG_SESION.diff('adelantos', TO_CHAR(:OLD.adelantos), TO_CHAR(:NEW.adelantos));
        v_det := v_det || PKG_SESION.diff('neto', TO_CHAR(:OLD.neto), TO_CHAR(:NEW.neto));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personal, asistencia y planilla', 'EDITAR', 'PLANILLA_DETALLE', :NEW.id_detalle_planilla, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_detalle_planilla', TO_CHAR(:OLD.id_detalle_planilla)) || PKG_SESION.par('id_planilla', TO_CHAR(:OLD.id_planilla)) || PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('sueldo_base', TO_CHAR(:OLD.sueldo_base)) || PKG_SESION.par('comision', TO_CHAR(:OLD.comision)) || PKG_SESION.par('descuento_faltas', TO_CHAR(:OLD.descuento_faltas)) || PKG_SESION.par('descuento_tardanzas', TO_CHAR(:OLD.descuento_tardanzas)) || PKG_SESION.par('adelantos', TO_CHAR(:OLD.adelantos)) || PKG_SESION.par('neto', TO_CHAR(:OLD.neto));
        PKG_SESION.auditar('Personal, asistencia y planilla', 'ELIMINAR', 'PLANILLA_DETALLE', :OLD.id_detalle_planilla, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_ADELANTO...
CREATE OR REPLACE TRIGGER TRG_AUD_ADELANTO
AFTER INSERT OR UPDATE OR DELETE ON ADELANTO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_adelanto', TO_CHAR(:NEW.id_adelanto)) || PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('id_planilla', TO_CHAR(:NEW.id_planilla)) || PKG_SESION.par('fecha', TO_CHAR(:NEW.fecha, 'YYYY-MM-DD')) || PKG_SESION.par('monto', TO_CHAR(:NEW.monto)) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('observacion', :NEW.observacion);
        PKG_SESION.auditar('Personal, asistencia y planilla', 'CREAR', 'ADELANTO', :NEW.id_adelanto, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_personal', TO_CHAR(:OLD.id_personal), TO_CHAR(:NEW.id_personal));
        v_det := v_det || PKG_SESION.diff('id_planilla', TO_CHAR(:OLD.id_planilla), TO_CHAR(:NEW.id_planilla));
        v_det := v_det || PKG_SESION.diff('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('monto', TO_CHAR(:OLD.monto), TO_CHAR(:NEW.monto));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('observacion', :OLD.observacion, :NEW.observacion);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Personal, asistencia y planilla', 'EDITAR', 'ADELANTO', :NEW.id_adelanto, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_adelanto', TO_CHAR(:OLD.id_adelanto)) || PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('id_planilla', TO_CHAR(:OLD.id_planilla)) || PKG_SESION.par('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD')) || PKG_SESION.par('monto', TO_CHAR(:OLD.monto)) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('observacion', :OLD.observacion);
        PKG_SESION.auditar('Personal, asistencia y planilla', 'ELIMINAR', 'ADELANTO', :OLD.id_adelanto, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_RECURSO...
CREATE OR REPLACE TRIGGER TRG_AUD_RECURSO
AFTER INSERT OR UPDATE OR DELETE ON RECURSO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_recurso', TO_CHAR(:NEW.id_recurso)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('tipo', :NEW.tipo) || PKG_SESION.par('minutos_limpieza', TO_CHAR(:NEW.minutos_limpieza)) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Sedes y recursos', 'CREAR', 'RECURSO', :NEW.id_recurso, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('tipo', :OLD.tipo, :NEW.tipo);
        v_det := v_det || PKG_SESION.diff('minutos_limpieza', TO_CHAR(:OLD.minutos_limpieza), TO_CHAR(:NEW.minutos_limpieza));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Sedes y recursos', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'RECURSO', :NEW.id_recurso, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_recurso', TO_CHAR(:OLD.id_recurso)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('tipo', :OLD.tipo) || PKG_SESION.par('minutos_limpieza', TO_CHAR(:OLD.minutos_limpieza)) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Sedes y recursos', 'ELIMINAR', 'RECURSO', :OLD.id_recurso, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_AREA...
CREATE OR REPLACE TRIGGER TRG_AUD_AREA
AFTER INSERT OR UPDATE OR DELETE ON AREA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_area', TO_CHAR(:NEW.id_area)) || PKG_SESION.par('linea', :NEW.linea) || PKG_SESION.par('nombre', :NEW.nombre);
        PKG_SESION.auditar('Catálogo comercial', 'CREAR', 'AREA', :NEW.id_area, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('linea', :OLD.linea, :NEW.linea);
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Catálogo comercial', 'EDITAR', 'AREA', :NEW.id_area, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_area', TO_CHAR(:OLD.id_area)) || PKG_SESION.par('linea', :OLD.linea) || PKG_SESION.par('nombre', :OLD.nombre);
        PKG_SESION.auditar('Catálogo comercial', 'ELIMINAR', 'AREA', :OLD.id_area, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_SERVICIO...
CREATE OR REPLACE TRIGGER TRG_AUD_SERVICIO
AFTER INSERT OR UPDATE OR DELETE ON SERVICIO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_servicio', TO_CHAR(:NEW.id_servicio)) || PKG_SESION.par('id_area', TO_CHAR(:NEW.id_area)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('duracion_ref_min', TO_CHAR(:NEW.duracion_ref_min)) || PKG_SESION.par('tipo_recurso', :NEW.tipo_recurso) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Catálogo comercial', 'CREAR', 'SERVICIO', :NEW.id_servicio, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_area', TO_CHAR(:OLD.id_area), TO_CHAR(:NEW.id_area));
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('duracion_ref_min', TO_CHAR(:OLD.duracion_ref_min), TO_CHAR(:NEW.duracion_ref_min));
        v_det := v_det || PKG_SESION.diff('tipo_recurso', :OLD.tipo_recurso, :NEW.tipo_recurso);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Catálogo comercial', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'SERVICIO', :NEW.id_servicio, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_servicio', TO_CHAR(:OLD.id_servicio)) || PKG_SESION.par('id_area', TO_CHAR(:OLD.id_area)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('duracion_ref_min', TO_CHAR(:OLD.duracion_ref_min)) || PKG_SESION.par('tipo_recurso', :OLD.tipo_recurso) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Catálogo comercial', 'ELIMINAR', 'SERVICIO', :OLD.id_servicio, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_VARIANTE_SERVICIO...
CREATE OR REPLACE TRIGGER TRG_AUD_VARIANTE_SERVICIO
AFTER INSERT OR UPDATE OR DELETE ON VARIANTE_SERVICIO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_variante', TO_CHAR(:NEW.id_variante)) || PKG_SESION.par('id_servicio', TO_CHAR(:NEW.id_servicio)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('precio', TO_CHAR(:NEW.precio)) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Catálogo comercial', 'CREAR', 'VARIANTE_SERVICIO', :NEW.id_variante, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_servicio', TO_CHAR(:OLD.id_servicio), TO_CHAR(:NEW.id_servicio));
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('precio', TO_CHAR(:OLD.precio), TO_CHAR(:NEW.precio));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Catálogo comercial', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'VARIANTE_SERVICIO', :NEW.id_variante, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_variante', TO_CHAR(:OLD.id_variante)) || PKG_SESION.par('id_servicio', TO_CHAR(:OLD.id_servicio)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('precio', TO_CHAR(:OLD.precio)) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Catálogo comercial', 'ELIMINAR', 'VARIANTE_SERVICIO', :OLD.id_variante, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_SERVICIO_INSUMO...
CREATE OR REPLACE TRIGGER TRG_AUD_SERVICIO_INSUMO
AFTER INSERT OR UPDATE OR DELETE ON SERVICIO_INSUMO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_variante', TO_CHAR(:NEW.id_variante)) || PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('cantidad_estimada', TO_CHAR(:NEW.cantidad_estimada));
        PKG_SESION.auditar('Catálogo comercial', 'CREAR', 'SERVICIO_INSUMO', :NEW.id_variante, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('cantidad_estimada', TO_CHAR(:OLD.cantidad_estimada), TO_CHAR(:NEW.cantidad_estimada));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Catálogo comercial', 'EDITAR', 'SERVICIO_INSUMO', :NEW.id_variante, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_variante', TO_CHAR(:OLD.id_variante)) || PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('cantidad_estimada', TO_CHAR(:OLD.cantidad_estimada));
        PKG_SESION.auditar('Catálogo comercial', 'ELIMINAR', 'SERVICIO_INSUMO', :OLD.id_variante, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PROMOCION...
CREATE OR REPLACE TRIGGER TRG_AUD_PROMOCION
AFTER INSERT OR UPDATE OR DELETE ON PROMOCION
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_promocion', TO_CHAR(:NEW.id_promocion)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('tipo_descuento', :NEW.tipo_descuento) || PKG_SESION.par('valor', TO_CHAR(:NEW.valor)) || PKG_SESION.par('fecha_inicio', TO_CHAR(:NEW.fecha_inicio, 'YYYY-MM-DD')) || PKG_SESION.par('fecha_fin', TO_CHAR(:NEW.fecha_fin, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario));
        PKG_SESION.auditar('Promociones y paquetes', 'CREAR', 'PROMOCION', :NEW.id_promocion, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('tipo_descuento', :OLD.tipo_descuento, :NEW.tipo_descuento);
        v_det := v_det || PKG_SESION.diff('valor', TO_CHAR(:OLD.valor), TO_CHAR(:NEW.valor));
        v_det := v_det || PKG_SESION.diff('fecha_inicio', TO_CHAR(:OLD.fecha_inicio, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_inicio, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('fecha_fin', TO_CHAR(:OLD.fecha_fin, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_fin, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Promociones y paquetes', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'PROMOCION', :NEW.id_promocion, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_promocion', TO_CHAR(:OLD.id_promocion)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('tipo_descuento', :OLD.tipo_descuento) || PKG_SESION.par('valor', TO_CHAR(:OLD.valor)) || PKG_SESION.par('fecha_inicio', TO_CHAR(:OLD.fecha_inicio, 'YYYY-MM-DD')) || PKG_SESION.par('fecha_fin', TO_CHAR(:OLD.fecha_fin, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario));
        PKG_SESION.auditar('Promociones y paquetes', 'ELIMINAR', 'PROMOCION', :OLD.id_promocion, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PROMOCION_SERVICIO...
CREATE OR REPLACE TRIGGER TRG_AUD_PROMOCION_SERVICIO
AFTER INSERT OR UPDATE OR DELETE ON PROMOCION_SERVICIO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_promocion', TO_CHAR(:NEW.id_promocion)) || PKG_SESION.par('id_servicio', TO_CHAR(:NEW.id_servicio));
        PKG_SESION.auditar('Promociones y paquetes', 'CREAR', 'PROMOCION_SERVICIO', :NEW.id_promocion, v_det, NULL);
    ELSIF UPDATING THEN
        NULL;
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Promociones y paquetes', 'EDITAR', 'PROMOCION_SERVICIO', :NEW.id_promocion, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_promocion', TO_CHAR(:OLD.id_promocion)) || PKG_SESION.par('id_servicio', TO_CHAR(:OLD.id_servicio));
        PKG_SESION.auditar('Promociones y paquetes', 'ELIMINAR', 'PROMOCION_SERVICIO', :OLD.id_promocion, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PAQUETE...
CREATE OR REPLACE TRIGGER TRG_AUD_PAQUETE
AFTER INSERT OR UPDATE OR DELETE ON PAQUETE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_paquete', TO_CHAR(:NEW.id_paquete)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('tipo', :NEW.tipo) || PKG_SESION.par('num_sesiones', TO_CHAR(:NEW.num_sesiones)) || PKG_SESION.par('precio', TO_CHAR(:NEW.precio)) || PKG_SESION.par('vigencia_dias', TO_CHAR(:NEW.vigencia_dias)) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Promociones y paquetes', 'CREAR', 'PAQUETE', :NEW.id_paquete, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('tipo', :OLD.tipo, :NEW.tipo);
        v_det := v_det || PKG_SESION.diff('num_sesiones', TO_CHAR(:OLD.num_sesiones), TO_CHAR(:NEW.num_sesiones));
        v_det := v_det || PKG_SESION.diff('precio', TO_CHAR(:OLD.precio), TO_CHAR(:NEW.precio));
        v_det := v_det || PKG_SESION.diff('vigencia_dias', TO_CHAR(:OLD.vigencia_dias), TO_CHAR(:NEW.vigencia_dias));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Promociones y paquetes', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'PAQUETE', :NEW.id_paquete, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_paquete', TO_CHAR(:OLD.id_paquete)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('tipo', :OLD.tipo) || PKG_SESION.par('num_sesiones', TO_CHAR(:OLD.num_sesiones)) || PKG_SESION.par('precio', TO_CHAR(:OLD.precio)) || PKG_SESION.par('vigencia_dias', TO_CHAR(:OLD.vigencia_dias)) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Promociones y paquetes', 'ELIMINAR', 'PAQUETE', :OLD.id_paquete, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PAQUETE_SERVICIO...
CREATE OR REPLACE TRIGGER TRG_AUD_PAQUETE_SERVICIO
AFTER INSERT OR UPDATE OR DELETE ON PAQUETE_SERVICIO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_paquete', TO_CHAR(:NEW.id_paquete)) || PKG_SESION.par('id_servicio', TO_CHAR(:NEW.id_servicio));
        PKG_SESION.auditar('Promociones y paquetes', 'CREAR', 'PAQUETE_SERVICIO', :NEW.id_paquete, v_det, NULL);
    ELSIF UPDATING THEN
        NULL;
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Promociones y paquetes', 'EDITAR', 'PAQUETE_SERVICIO', :NEW.id_paquete, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_paquete', TO_CHAR(:OLD.id_paquete)) || PKG_SESION.par('id_servicio', TO_CHAR(:OLD.id_servicio));
        PKG_SESION.auditar('Promociones y paquetes', 'ELIMINAR', 'PAQUETE_SERVICIO', :OLD.id_paquete, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CLIENTE_PAQUETE...
CREATE OR REPLACE TRIGGER TRG_AUD_CLIENTE_PAQUETE
AFTER INSERT OR UPDATE OR DELETE ON CLIENTE_PAQUETE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_cliente_paquete', TO_CHAR(:NEW.id_cliente_paquete)) || PKG_SESION.par('id_cliente', TO_CHAR(:NEW.id_cliente)) || PKG_SESION.par('id_paquete', TO_CHAR(:NEW.id_paquete)) || PKG_SESION.par('id_detalle', TO_CHAR(:NEW.id_detalle)) || PKG_SESION.par('sesiones_totales', TO_CHAR(:NEW.sesiones_totales)) || PKG_SESION.par('fecha_compra', TO_CHAR(:NEW.fecha_compra, 'YYYY-MM-DD')) || PKG_SESION.par('fecha_vencimiento', TO_CHAR(:NEW.fecha_vencimiento, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Clientas, agenda y atención', 'CREAR', 'CLIENTE_PAQUETE', :NEW.id_cliente_paquete, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_cliente', TO_CHAR(:OLD.id_cliente), TO_CHAR(:NEW.id_cliente));
        v_det := v_det || PKG_SESION.diff('id_paquete', TO_CHAR(:OLD.id_paquete), TO_CHAR(:NEW.id_paquete));
        v_det := v_det || PKG_SESION.diff('id_detalle', TO_CHAR(:OLD.id_detalle), TO_CHAR(:NEW.id_detalle));
        v_det := v_det || PKG_SESION.diff('sesiones_totales', TO_CHAR(:OLD.sesiones_totales), TO_CHAR(:NEW.sesiones_totales));
        v_det := v_det || PKG_SESION.diff('fecha_compra', TO_CHAR(:OLD.fecha_compra, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_compra, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('fecha_vencimiento', TO_CHAR(:OLD.fecha_vencimiento, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_vencimiento, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Clientas, agenda y atención', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'CLIENTE_PAQUETE', :NEW.id_cliente_paquete, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_cliente_paquete', TO_CHAR(:OLD.id_cliente_paquete)) || PKG_SESION.par('id_cliente', TO_CHAR(:OLD.id_cliente)) || PKG_SESION.par('id_paquete', TO_CHAR(:OLD.id_paquete)) || PKG_SESION.par('id_detalle', TO_CHAR(:OLD.id_detalle)) || PKG_SESION.par('sesiones_totales', TO_CHAR(:OLD.sesiones_totales)) || PKG_SESION.par('fecha_compra', TO_CHAR(:OLD.fecha_compra, 'YYYY-MM-DD')) || PKG_SESION.par('fecha_vencimiento', TO_CHAR(:OLD.fecha_vencimiento, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Clientas, agenda y atención', 'ELIMINAR', 'CLIENTE_PAQUETE', :OLD.id_cliente_paquete, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CITA...
CREATE OR REPLACE TRIGGER TRG_AUD_CITA
AFTER INSERT OR UPDATE OR DELETE ON CITA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_cita', TO_CHAR(:NEW.id_cita)) || PKG_SESION.par('id_cliente', TO_CHAR(:NEW.id_cliente)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('id_recurso', TO_CHAR(:NEW.id_recurso)) || PKG_SESION.par('id_variante', TO_CHAR(:NEW.id_variante)) || PKG_SESION.par('fecha_hora_inicio', TO_CHAR(:NEW.fecha_hora_inicio, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('duracion_min', TO_CHAR(:NEW.duracion_min)) || PKG_SESION.par('fecha_hora_fin', TO_CHAR(:NEW.fecha_hora_fin, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('adelanto_monto', TO_CHAR(:NEW.adelanto_monto)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:NEW.id_medio_pago)) || PKG_SESION.par('fecha_adelanto', TO_CHAR(:NEW.fecha_adelanto, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_caja', TO_CHAR(:NEW.id_caja)) || PKG_SESION.par('id_cita_origen', TO_CHAR(:NEW.id_cita_origen)) || PKG_SESION.par('motivo_cancelacion', :NEW.motivo_cancelacion) || PKG_SESION.par('fecha_recordatorio', TO_CHAR(:NEW.fecha_recordatorio, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('fecha_registro', TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Clientas, agenda y atención', 'CREAR', 'CITA', :NEW.id_cita, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_cliente', TO_CHAR(:OLD.id_cliente), TO_CHAR(:NEW.id_cliente));
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('id_personal', TO_CHAR(:OLD.id_personal), TO_CHAR(:NEW.id_personal));
        v_det := v_det || PKG_SESION.diff('id_recurso', TO_CHAR(:OLD.id_recurso), TO_CHAR(:NEW.id_recurso));
        v_det := v_det || PKG_SESION.diff('id_variante', TO_CHAR(:OLD.id_variante), TO_CHAR(:NEW.id_variante));
        v_det := v_det || PKG_SESION.diff('fecha_hora_inicio', TO_CHAR(:OLD.fecha_hora_inicio, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_hora_inicio, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('duracion_min', TO_CHAR(:OLD.duracion_min), TO_CHAR(:NEW.duracion_min));
        v_det := v_det || PKG_SESION.diff('fecha_hora_fin', TO_CHAR(:OLD.fecha_hora_fin, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_hora_fin, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('adelanto_monto', TO_CHAR(:OLD.adelanto_monto), TO_CHAR(:NEW.adelanto_monto));
        v_det := v_det || PKG_SESION.diff('id_medio_pago', TO_CHAR(:OLD.id_medio_pago), TO_CHAR(:NEW.id_medio_pago));
        v_det := v_det || PKG_SESION.diff('fecha_adelanto', TO_CHAR(:OLD.fecha_adelanto, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_adelanto, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('id_caja', TO_CHAR(:OLD.id_caja), TO_CHAR(:NEW.id_caja));
        v_det := v_det || PKG_SESION.diff('id_cita_origen', TO_CHAR(:OLD.id_cita_origen), TO_CHAR(:NEW.id_cita_origen));
        v_det := v_det || PKG_SESION.diff('motivo_cancelacion', :OLD.motivo_cancelacion, :NEW.motivo_cancelacion);
        v_det := v_det || PKG_SESION.diff('fecha_recordatorio', TO_CHAR(:OLD.fecha_recordatorio, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_recordatorio, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Clientas, agenda y atención', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'CITA', :NEW.id_cita, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_cita', TO_CHAR(:OLD.id_cita)) || PKG_SESION.par('id_cliente', TO_CHAR(:OLD.id_cliente)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('id_recurso', TO_CHAR(:OLD.id_recurso)) || PKG_SESION.par('id_variante', TO_CHAR(:OLD.id_variante)) || PKG_SESION.par('fecha_hora_inicio', TO_CHAR(:OLD.fecha_hora_inicio, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('duracion_min', TO_CHAR(:OLD.duracion_min)) || PKG_SESION.par('fecha_hora_fin', TO_CHAR(:OLD.fecha_hora_fin, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('adelanto_monto', TO_CHAR(:OLD.adelanto_monto)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:OLD.id_medio_pago)) || PKG_SESION.par('fecha_adelanto', TO_CHAR(:OLD.fecha_adelanto, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_caja', TO_CHAR(:OLD.id_caja)) || PKG_SESION.par('id_cita_origen', TO_CHAR(:OLD.id_cita_origen)) || PKG_SESION.par('motivo_cancelacion', :OLD.motivo_cancelacion) || PKG_SESION.par('fecha_recordatorio', TO_CHAR(:OLD.fecha_recordatorio, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Clientas, agenda y atención', 'ELIMINAR', 'CITA', :OLD.id_cita, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_ATENCION...
CREATE OR REPLACE TRIGGER TRG_AUD_ATENCION
AFTER INSERT OR UPDATE OR DELETE ON ATENCION
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_atencion', TO_CHAR(:NEW.id_atencion)) || PKG_SESION.par('id_cita', TO_CHAR(:NEW.id_cita)) || PKG_SESION.par('id_cliente', TO_CHAR(:NEW.id_cliente)) || PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('id_recurso', TO_CHAR(:NEW.id_recurso)) || PKG_SESION.par('id_variante', TO_CHAR(:NEW.id_variante)) || PKG_SESION.par('id_cliente_paquete', TO_CHAR(:NEW.id_cliente_paquete)) || PKG_SESION.par('nombre_cliente_paso', :NEW.nombre_cliente_paso) || PKG_SESION.par('hora_inicio', TO_CHAR(:NEW.hora_inicio, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('hora_fin', TO_CHAR(:NEW.hora_fin, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('tiempo_declarado_min', TO_CHAR(:NEW.tiempo_declarado_min)) || PKG_SESION.par('observaciones', :NEW.observaciones) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Clientas, agenda y atención', 'CREAR', 'ATENCION', :NEW.id_atencion, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_cita', TO_CHAR(:OLD.id_cita), TO_CHAR(:NEW.id_cita));
        v_det := v_det || PKG_SESION.diff('id_cliente', TO_CHAR(:OLD.id_cliente), TO_CHAR(:NEW.id_cliente));
        v_det := v_det || PKG_SESION.diff('id_personal', TO_CHAR(:OLD.id_personal), TO_CHAR(:NEW.id_personal));
        v_det := v_det || PKG_SESION.diff('id_recurso', TO_CHAR(:OLD.id_recurso), TO_CHAR(:NEW.id_recurso));
        v_det := v_det || PKG_SESION.diff('id_variante', TO_CHAR(:OLD.id_variante), TO_CHAR(:NEW.id_variante));
        v_det := v_det || PKG_SESION.diff('id_cliente_paquete', TO_CHAR(:OLD.id_cliente_paquete), TO_CHAR(:NEW.id_cliente_paquete));
        v_det := v_det || PKG_SESION.diff('nombre_cliente_paso', :OLD.nombre_cliente_paso, :NEW.nombre_cliente_paso);
        v_det := v_det || PKG_SESION.diff('hora_inicio', TO_CHAR(:OLD.hora_inicio, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.hora_inicio, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('hora_fin', TO_CHAR(:OLD.hora_fin, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.hora_fin, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('tiempo_declarado_min', TO_CHAR(:OLD.tiempo_declarado_min), TO_CHAR(:NEW.tiempo_declarado_min));
        v_det := v_det || PKG_SESION.diff('observaciones', :OLD.observaciones, :NEW.observaciones);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Clientas, agenda y atención', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'ATENCION', :NEW.id_atencion, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_atencion', TO_CHAR(:OLD.id_atencion)) || PKG_SESION.par('id_cita', TO_CHAR(:OLD.id_cita)) || PKG_SESION.par('id_cliente', TO_CHAR(:OLD.id_cliente)) || PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('id_recurso', TO_CHAR(:OLD.id_recurso)) || PKG_SESION.par('id_variante', TO_CHAR(:OLD.id_variante)) || PKG_SESION.par('id_cliente_paquete', TO_CHAR(:OLD.id_cliente_paquete)) || PKG_SESION.par('nombre_cliente_paso', :OLD.nombre_cliente_paso) || PKG_SESION.par('hora_inicio', TO_CHAR(:OLD.hora_inicio, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('hora_fin', TO_CHAR(:OLD.hora_fin, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('tiempo_declarado_min', TO_CHAR(:OLD.tiempo_declarado_min)) || PKG_SESION.par('observaciones', :OLD.observaciones) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Clientas, agenda y atención', 'ELIMINAR', 'ATENCION', :OLD.id_atencion, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_LISTA_ESPERA...
CREATE OR REPLACE TRIGGER TRG_AUD_LISTA_ESPERA
AFTER INSERT OR UPDATE OR DELETE ON LISTA_ESPERA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_espera', TO_CHAR(:NEW.id_espera)) || PKG_SESION.par('id_cliente', TO_CHAR(:NEW.id_cliente)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('id_variante', TO_CHAR(:NEW.id_variante)) || PKG_SESION.par('fecha_deseada', TO_CHAR(:NEW.fecha_deseada, 'YYYY-MM-DD')) || PKG_SESION.par('hora_desde', :NEW.hora_desde) || PKG_SESION.par('hora_hasta', :NEW.hora_hasta) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('id_cita', TO_CHAR(:NEW.id_cita)) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('fecha_registro', TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Clientas, agenda y atención', 'CREAR', 'LISTA_ESPERA', :NEW.id_espera, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_cliente', TO_CHAR(:OLD.id_cliente), TO_CHAR(:NEW.id_cliente));
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('id_variante', TO_CHAR(:OLD.id_variante), TO_CHAR(:NEW.id_variante));
        v_det := v_det || PKG_SESION.diff('fecha_deseada', TO_CHAR(:OLD.fecha_deseada, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_deseada, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('hora_desde', :OLD.hora_desde, :NEW.hora_desde);
        v_det := v_det || PKG_SESION.diff('hora_hasta', :OLD.hora_hasta, :NEW.hora_hasta);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('id_cita', TO_CHAR(:OLD.id_cita), TO_CHAR(:NEW.id_cita));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Clientas, agenda y atención', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'LISTA_ESPERA', :NEW.id_espera, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_espera', TO_CHAR(:OLD.id_espera)) || PKG_SESION.par('id_cliente', TO_CHAR(:OLD.id_cliente)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('id_variante', TO_CHAR(:OLD.id_variante)) || PKG_SESION.par('fecha_deseada', TO_CHAR(:OLD.fecha_deseada, 'YYYY-MM-DD')) || PKG_SESION.par('hora_desde', :OLD.hora_desde) || PKG_SESION.par('hora_hasta', :OLD.hora_hasta) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('id_cita', TO_CHAR(:OLD.id_cita)) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('fecha_registro', TO_CHAR(:OLD.fecha_registro, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Clientas, agenda y atención', 'ELIMINAR', 'LISTA_ESPERA', :OLD.id_espera, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_RECOMENDACION_PRODUCTO...
CREATE OR REPLACE TRIGGER TRG_AUD_RECOMENDACION_PRODUCTO
AFTER INSERT OR UPDATE OR DELETE ON RECOMENDACION_PRODUCTO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_recomendacion', TO_CHAR(:NEW.id_recomendacion)) || PKG_SESION.par('id_atencion', TO_CHAR(:NEW.id_atencion)) || PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('observacion', :NEW.observacion) || PKG_SESION.par('fecha', TO_CHAR(:NEW.fecha, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Clientas, agenda y atención', 'CREAR', 'RECOMENDACION_PRODUCTO', :NEW.id_recomendacion, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_atencion', TO_CHAR(:OLD.id_atencion), TO_CHAR(:NEW.id_atencion));
        v_det := v_det || PKG_SESION.diff('id_producto', TO_CHAR(:OLD.id_producto), TO_CHAR(:NEW.id_producto));
        v_det := v_det || PKG_SESION.diff('observacion', :OLD.observacion, :NEW.observacion);
        v_det := v_det || PKG_SESION.diff('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha, 'YYYY-MM-DD HH24:MI:SS'));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Clientas, agenda y atención', 'EDITAR', 'RECOMENDACION_PRODUCTO', :NEW.id_recomendacion, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_recomendacion', TO_CHAR(:OLD.id_recomendacion)) || PKG_SESION.par('id_atencion', TO_CHAR(:OLD.id_atencion)) || PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('observacion', :OLD.observacion) || PKG_SESION.par('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Clientas, agenda y atención', 'ELIMINAR', 'RECOMENDACION_PRODUCTO', :OLD.id_recomendacion, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_SALDO_FAVOR_MOV...
CREATE OR REPLACE TRIGGER TRG_AUD_SALDO_FAVOR_MOV
AFTER INSERT OR UPDATE OR DELETE ON SALDO_FAVOR_MOV
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_movimiento', TO_CHAR(:NEW.id_movimiento)) || PKG_SESION.par('id_cliente', TO_CHAR(:NEW.id_cliente)) || PKG_SESION.par('id_cita', TO_CHAR(:NEW.id_cita)) || PKG_SESION.par('id_venta', TO_CHAR(:NEW.id_venta)) || PKG_SESION.par('tipo', :NEW.tipo) || PKG_SESION.par('monto', TO_CHAR(:NEW.monto)) || PKG_SESION.par('fecha', TO_CHAR(:NEW.fecha, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('motivo', :NEW.motivo);
        PKG_SESION.auditar('Clientas, agenda y atención', 'CREAR', 'SALDO_FAVOR_MOV', :NEW.id_movimiento, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_cliente', TO_CHAR(:OLD.id_cliente), TO_CHAR(:NEW.id_cliente));
        v_det := v_det || PKG_SESION.diff('id_cita', TO_CHAR(:OLD.id_cita), TO_CHAR(:NEW.id_cita));
        v_det := v_det || PKG_SESION.diff('id_venta', TO_CHAR(:OLD.id_venta), TO_CHAR(:NEW.id_venta));
        v_det := v_det || PKG_SESION.diff('tipo', :OLD.tipo, :NEW.tipo);
        v_det := v_det || PKG_SESION.diff('monto', TO_CHAR(:OLD.monto), TO_CHAR(:NEW.monto));
        v_det := v_det || PKG_SESION.diff('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('motivo', :OLD.motivo, :NEW.motivo);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Clientas, agenda y atención', 'EDITAR', 'SALDO_FAVOR_MOV', :NEW.id_movimiento, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_movimiento', TO_CHAR(:OLD.id_movimiento)) || PKG_SESION.par('id_cliente', TO_CHAR(:OLD.id_cliente)) || PKG_SESION.par('id_cita', TO_CHAR(:OLD.id_cita)) || PKG_SESION.par('id_venta', TO_CHAR(:OLD.id_venta)) || PKG_SESION.par('tipo', :OLD.tipo) || PKG_SESION.par('monto', TO_CHAR(:OLD.monto)) || PKG_SESION.par('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('motivo', :OLD.motivo);
        PKG_SESION.auditar('Clientas, agenda y atención', 'ELIMINAR', 'SALDO_FAVOR_MOV', :OLD.id_movimiento, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_MEDIO_PAGO...
CREATE OR REPLACE TRIGGER TRG_AUD_MEDIO_PAGO
AFTER INSERT OR UPDATE OR DELETE ON MEDIO_PAGO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_medio_pago', TO_CHAR(:NEW.id_medio_pago)) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('usa_pasarela', :NEW.usa_pasarela);
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'MEDIO_PAGO', :NEW.id_medio_pago, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('usa_pasarela', :OLD.usa_pasarela, :NEW.usa_pasarela);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'MEDIO_PAGO', :NEW.id_medio_pago, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_medio_pago', TO_CHAR(:OLD.id_medio_pago)) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('usa_pasarela', :OLD.usa_pasarela);
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'MEDIO_PAGO', :OLD.id_medio_pago, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_VENTA...
CREATE OR REPLACE TRIGGER TRG_AUD_VENTA
AFTER INSERT OR UPDATE OR DELETE ON VENTA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_venta', TO_CHAR(:NEW.id_venta)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('id_caja', TO_CHAR(:NEW.id_caja)) || PKG_SESION.par('id_cliente', TO_CHAR(:NEW.id_cliente)) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('id_usuario_autoriza', TO_CHAR(:NEW.id_usuario_autoriza)) || PKG_SESION.par('fecha_hora', TO_CHAR(:NEW.fecha_hora, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('subtotal', TO_CHAR(:NEW.subtotal)) || PKG_SESION.par('igv', TO_CHAR(:NEW.igv)) || PKG_SESION.par('descuento', TO_CHAR(:NEW.descuento)) || PKG_SESION.par('adelanto_aplicado', TO_CHAR(:NEW.adelanto_aplicado)) || PKG_SESION.par('saldo_favor_aplicado', TO_CHAR(:NEW.saldo_favor_aplicado)) || PKG_SESION.par('total', TO_CHAR(:NEW.total)) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'VENTA', :NEW.id_venta, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('id_caja', TO_CHAR(:OLD.id_caja), TO_CHAR(:NEW.id_caja));
        v_det := v_det || PKG_SESION.diff('id_cliente', TO_CHAR(:OLD.id_cliente), TO_CHAR(:NEW.id_cliente));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('id_usuario_autoriza', TO_CHAR(:OLD.id_usuario_autoriza), TO_CHAR(:NEW.id_usuario_autoriza));
        v_det := v_det || PKG_SESION.diff('fecha_hora', TO_CHAR(:OLD.fecha_hora, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_hora, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('subtotal', TO_CHAR(:OLD.subtotal), TO_CHAR(:NEW.subtotal));
        v_det := v_det || PKG_SESION.diff('igv', TO_CHAR(:OLD.igv), TO_CHAR(:NEW.igv));
        v_det := v_det || PKG_SESION.diff('descuento', TO_CHAR(:OLD.descuento), TO_CHAR(:NEW.descuento));
        v_det := v_det || PKG_SESION.diff('adelanto_aplicado', TO_CHAR(:OLD.adelanto_aplicado), TO_CHAR(:NEW.adelanto_aplicado));
        v_det := v_det || PKG_SESION.diff('saldo_favor_aplicado', TO_CHAR(:OLD.saldo_favor_aplicado), TO_CHAR(:NEW.saldo_favor_aplicado));
        v_det := v_det || PKG_SESION.diff('total', TO_CHAR(:OLD.total), TO_CHAR(:NEW.total));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'VENTA', :NEW.id_venta, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_venta', TO_CHAR(:OLD.id_venta)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('id_caja', TO_CHAR(:OLD.id_caja)) || PKG_SESION.par('id_cliente', TO_CHAR(:OLD.id_cliente)) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('id_usuario_autoriza', TO_CHAR(:OLD.id_usuario_autoriza)) || PKG_SESION.par('fecha_hora', TO_CHAR(:OLD.fecha_hora, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('subtotal', TO_CHAR(:OLD.subtotal)) || PKG_SESION.par('igv', TO_CHAR(:OLD.igv)) || PKG_SESION.par('descuento', TO_CHAR(:OLD.descuento)) || PKG_SESION.par('adelanto_aplicado', TO_CHAR(:OLD.adelanto_aplicado)) || PKG_SESION.par('saldo_favor_aplicado', TO_CHAR(:OLD.saldo_favor_aplicado)) || PKG_SESION.par('total', TO_CHAR(:OLD.total)) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'VENTA', :OLD.id_venta, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_VENTA_DETALLE...
CREATE OR REPLACE TRIGGER TRG_AUD_VENTA_DETALLE
AFTER INSERT OR UPDATE OR DELETE ON VENTA_DETALLE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_detalle', TO_CHAR(:NEW.id_detalle)) || PKG_SESION.par('id_venta', TO_CHAR(:NEW.id_venta)) || PKG_SESION.par('tipo_item', :NEW.tipo_item) || PKG_SESION.par('id_atencion', TO_CHAR(:NEW.id_atencion)) || PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('id_paquete', TO_CHAR(:NEW.id_paquete)) || PKG_SESION.par('id_promocion', TO_CHAR(:NEW.id_promocion)) || PKG_SESION.par('cantidad', TO_CHAR(:NEW.cantidad)) || PKG_SESION.par('precio_unitario', TO_CHAR(:NEW.precio_unitario)) || PKG_SESION.par('descuento', TO_CHAR(:NEW.descuento)) || PKG_SESION.par('subtotal', TO_CHAR(:NEW.subtotal)) || PKG_SESION.par('id_personal', TO_CHAR(:NEW.id_personal)) || PKG_SESION.par('porcentaje_comision', TO_CHAR(:NEW.porcentaje_comision)) || PKG_SESION.par('monto_comision', TO_CHAR(:NEW.monto_comision));
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'VENTA_DETALLE', :NEW.id_detalle, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_venta', TO_CHAR(:OLD.id_venta), TO_CHAR(:NEW.id_venta));
        v_det := v_det || PKG_SESION.diff('tipo_item', :OLD.tipo_item, :NEW.tipo_item);
        v_det := v_det || PKG_SESION.diff('id_atencion', TO_CHAR(:OLD.id_atencion), TO_CHAR(:NEW.id_atencion));
        v_det := v_det || PKG_SESION.diff('id_producto', TO_CHAR(:OLD.id_producto), TO_CHAR(:NEW.id_producto));
        v_det := v_det || PKG_SESION.diff('id_paquete', TO_CHAR(:OLD.id_paquete), TO_CHAR(:NEW.id_paquete));
        v_det := v_det || PKG_SESION.diff('id_promocion', TO_CHAR(:OLD.id_promocion), TO_CHAR(:NEW.id_promocion));
        v_det := v_det || PKG_SESION.diff('cantidad', TO_CHAR(:OLD.cantidad), TO_CHAR(:NEW.cantidad));
        v_det := v_det || PKG_SESION.diff('precio_unitario', TO_CHAR(:OLD.precio_unitario), TO_CHAR(:NEW.precio_unitario));
        v_det := v_det || PKG_SESION.diff('descuento', TO_CHAR(:OLD.descuento), TO_CHAR(:NEW.descuento));
        v_det := v_det || PKG_SESION.diff('subtotal', TO_CHAR(:OLD.subtotal), TO_CHAR(:NEW.subtotal));
        v_det := v_det || PKG_SESION.diff('id_personal', TO_CHAR(:OLD.id_personal), TO_CHAR(:NEW.id_personal));
        v_det := v_det || PKG_SESION.diff('porcentaje_comision', TO_CHAR(:OLD.porcentaje_comision), TO_CHAR(:NEW.porcentaje_comision));
        v_det := v_det || PKG_SESION.diff('monto_comision', TO_CHAR(:OLD.monto_comision), TO_CHAR(:NEW.monto_comision));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', 'EDITAR', 'VENTA_DETALLE', :NEW.id_detalle, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_detalle', TO_CHAR(:OLD.id_detalle)) || PKG_SESION.par('id_venta', TO_CHAR(:OLD.id_venta)) || PKG_SESION.par('tipo_item', :OLD.tipo_item) || PKG_SESION.par('id_atencion', TO_CHAR(:OLD.id_atencion)) || PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('id_paquete', TO_CHAR(:OLD.id_paquete)) || PKG_SESION.par('id_promocion', TO_CHAR(:OLD.id_promocion)) || PKG_SESION.par('cantidad', TO_CHAR(:OLD.cantidad)) || PKG_SESION.par('precio_unitario', TO_CHAR(:OLD.precio_unitario)) || PKG_SESION.par('descuento', TO_CHAR(:OLD.descuento)) || PKG_SESION.par('subtotal', TO_CHAR(:OLD.subtotal)) || PKG_SESION.par('id_personal', TO_CHAR(:OLD.id_personal)) || PKG_SESION.par('porcentaje_comision', TO_CHAR(:OLD.porcentaje_comision)) || PKG_SESION.par('monto_comision', TO_CHAR(:OLD.monto_comision));
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'VENTA_DETALLE', :OLD.id_detalle, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_VENTA_PAGO...
CREATE OR REPLACE TRIGGER TRG_AUD_VENTA_PAGO
AFTER INSERT OR UPDATE OR DELETE ON VENTA_PAGO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_pago', TO_CHAR(:NEW.id_pago)) || PKG_SESION.par('id_venta', TO_CHAR(:NEW.id_venta)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:NEW.id_medio_pago)) || PKG_SESION.par('monto', TO_CHAR(:NEW.monto)) || PKG_SESION.par('referencia', :NEW.referencia) || PKG_SESION.par('id_transaccion', TO_CHAR(:NEW.id_transaccion));
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'VENTA_PAGO', :NEW.id_pago, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_venta', TO_CHAR(:OLD.id_venta), TO_CHAR(:NEW.id_venta));
        v_det := v_det || PKG_SESION.diff('id_medio_pago', TO_CHAR(:OLD.id_medio_pago), TO_CHAR(:NEW.id_medio_pago));
        v_det := v_det || PKG_SESION.diff('monto', TO_CHAR(:OLD.monto), TO_CHAR(:NEW.monto));
        v_det := v_det || PKG_SESION.diff('referencia', :OLD.referencia, :NEW.referencia);
        v_det := v_det || PKG_SESION.diff('id_transaccion', TO_CHAR(:OLD.id_transaccion), TO_CHAR(:NEW.id_transaccion));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', 'EDITAR', 'VENTA_PAGO', :NEW.id_pago, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_pago', TO_CHAR(:OLD.id_pago)) || PKG_SESION.par('id_venta', TO_CHAR(:OLD.id_venta)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:OLD.id_medio_pago)) || PKG_SESION.par('monto', TO_CHAR(:OLD.monto)) || PKG_SESION.par('referencia', :OLD.referencia) || PKG_SESION.par('id_transaccion', TO_CHAR(:OLD.id_transaccion));
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'VENTA_PAGO', :OLD.id_pago, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_SERIE_COMPROBANTE...
CREATE OR REPLACE TRIGGER TRG_AUD_SERIE_COMPROBANTE
AFTER INSERT OR UPDATE OR DELETE ON SERIE_COMPROBANTE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_serie', TO_CHAR(:NEW.id_serie)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('tipo', :NEW.tipo) || PKG_SESION.par('serie', :NEW.serie) || PKG_SESION.par('ultimo_numero', TO_CHAR(:NEW.ultimo_numero)) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'SERIE_COMPROBANTE', :NEW.id_serie, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('tipo', :OLD.tipo, :NEW.tipo);
        v_det := v_det || PKG_SESION.diff('serie', :OLD.serie, :NEW.serie);
        v_det := v_det || PKG_SESION.diff('ultimo_numero', TO_CHAR(:OLD.ultimo_numero), TO_CHAR(:NEW.ultimo_numero));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'SERIE_COMPROBANTE', :NEW.id_serie, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_serie', TO_CHAR(:OLD.id_serie)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('tipo', :OLD.tipo) || PKG_SESION.par('serie', :OLD.serie) || PKG_SESION.par('ultimo_numero', TO_CHAR(:OLD.ultimo_numero)) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'SERIE_COMPROBANTE', :OLD.id_serie, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_COMPROBANTE...
CREATE OR REPLACE TRIGGER TRG_AUD_COMPROBANTE
AFTER INSERT OR UPDATE OR DELETE ON COMPROBANTE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_comprobante', TO_CHAR(:NEW.id_comprobante)) || PKG_SESION.par('id_venta', TO_CHAR(:NEW.id_venta)) || PKG_SESION.par('tipo', :NEW.tipo) || PKG_SESION.par('serie', :NEW.serie) || PKG_SESION.par('numero', :NEW.numero) || PKG_SESION.par('fecha_emision', TO_CHAR(:NEW.fecha_emision, 'YYYY-MM-DD')) || PKG_SESION.par('id_tipo_doc_comprador', TO_CHAR(:NEW.id_tipo_doc_comprador)) || PKG_SESION.par('nro_doc_comprador', :NEW.nro_doc_comprador) || PKG_SESION.par('nombre_comprador', :NEW.nombre_comprador) || PKG_SESION.par('estado_sunat', :NEW.estado_sunat) || PKG_SESION.par('intentos_envio', TO_CHAR(:NEW.intentos_envio)) || PKG_SESION.par('fecha_envio', TO_CHAR(:NEW.fecha_envio, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_respuesta', TO_CHAR(:NEW.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('codigo_respuesta', :NEW.codigo_respuesta) || PKG_SESION.par('descripcion_respuesta', :NEW.descripcion_respuesta) || PKG_SESION.par('hash_cpe', :NEW.hash_cpe) || PKG_SESION.par('ruta_xml', :NEW.ruta_xml) || PKG_SESION.par('ruta_cdr', :NEW.ruta_cdr);
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'COMPROBANTE', :NEW.id_comprobante, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_venta', TO_CHAR(:OLD.id_venta), TO_CHAR(:NEW.id_venta));
        v_det := v_det || PKG_SESION.diff('tipo', :OLD.tipo, :NEW.tipo);
        v_det := v_det || PKG_SESION.diff('serie', :OLD.serie, :NEW.serie);
        v_det := v_det || PKG_SESION.diff('numero', :OLD.numero, :NEW.numero);
        v_det := v_det || PKG_SESION.diff('fecha_emision', TO_CHAR(:OLD.fecha_emision, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha_emision, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('id_tipo_doc_comprador', TO_CHAR(:OLD.id_tipo_doc_comprador), TO_CHAR(:NEW.id_tipo_doc_comprador));
        v_det := v_det || PKG_SESION.diff('nro_doc_comprador', :OLD.nro_doc_comprador, :NEW.nro_doc_comprador);
        v_det := v_det || PKG_SESION.diff('nombre_comprador', :OLD.nombre_comprador, :NEW.nombre_comprador);
        v_det := v_det || PKG_SESION.diff('estado_sunat', :OLD.estado_sunat, :NEW.estado_sunat);
        v_det := v_det || PKG_SESION.diff('intentos_envio', TO_CHAR(:OLD.intentos_envio), TO_CHAR(:NEW.intentos_envio));
        v_det := v_det || PKG_SESION.diff('fecha_envio', TO_CHAR(:OLD.fecha_envio, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_envio, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('fecha_respuesta', TO_CHAR(:OLD.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('codigo_respuesta', :OLD.codigo_respuesta, :NEW.codigo_respuesta);
        v_det := v_det || PKG_SESION.diff('descripcion_respuesta', :OLD.descripcion_respuesta, :NEW.descripcion_respuesta);
        v_det := v_det || PKG_SESION.diff('hash_cpe', :OLD.hash_cpe, :NEW.hash_cpe);
        v_det := v_det || PKG_SESION.diff('ruta_xml', :OLD.ruta_xml, :NEW.ruta_xml);
        v_det := v_det || PKG_SESION.diff('ruta_cdr', :OLD.ruta_cdr, :NEW.ruta_cdr);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', 'EDITAR', 'COMPROBANTE', :NEW.id_comprobante, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_comprobante', TO_CHAR(:OLD.id_comprobante)) || PKG_SESION.par('id_venta', TO_CHAR(:OLD.id_venta)) || PKG_SESION.par('tipo', :OLD.tipo) || PKG_SESION.par('serie', :OLD.serie) || PKG_SESION.par('numero', :OLD.numero) || PKG_SESION.par('fecha_emision', TO_CHAR(:OLD.fecha_emision, 'YYYY-MM-DD')) || PKG_SESION.par('id_tipo_doc_comprador', TO_CHAR(:OLD.id_tipo_doc_comprador)) || PKG_SESION.par('nro_doc_comprador', :OLD.nro_doc_comprador) || PKG_SESION.par('nombre_comprador', :OLD.nombre_comprador) || PKG_SESION.par('estado_sunat', :OLD.estado_sunat) || PKG_SESION.par('intentos_envio', TO_CHAR(:OLD.intentos_envio)) || PKG_SESION.par('fecha_envio', TO_CHAR(:OLD.fecha_envio, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_respuesta', TO_CHAR(:OLD.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('codigo_respuesta', :OLD.codigo_respuesta) || PKG_SESION.par('descripcion_respuesta', :OLD.descripcion_respuesta) || PKG_SESION.par('hash_cpe', :OLD.hash_cpe) || PKG_SESION.par('ruta_xml', :OLD.ruta_xml) || PKG_SESION.par('ruta_cdr', :OLD.ruta_cdr);
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'COMPROBANTE', :OLD.id_comprobante, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_NOTA_CREDITO...
CREATE OR REPLACE TRIGGER TRG_AUD_NOTA_CREDITO
AFTER INSERT OR UPDATE OR DELETE ON NOTA_CREDITO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_nota_credito', TO_CHAR(:NEW.id_nota_credito)) || PKG_SESION.par('motivo', :NEW.motivo) || PKG_SESION.par('codigo_motivo', :NEW.codigo_motivo) || PKG_SESION.par('forma_devolucion', :NEW.forma_devolucion) || PKG_SESION.par('id_usuario_solicita', TO_CHAR(:NEW.id_usuario_solicita)) || PKG_SESION.par('id_usuario_autoriza', TO_CHAR(:NEW.id_usuario_autoriza));
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'NOTA_CREDITO', :NEW.id_nota_credito, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('motivo', :OLD.motivo, :NEW.motivo);
        v_det := v_det || PKG_SESION.diff('codigo_motivo', :OLD.codigo_motivo, :NEW.codigo_motivo);
        v_det := v_det || PKG_SESION.diff('forma_devolucion', :OLD.forma_devolucion, :NEW.forma_devolucion);
        v_det := v_det || PKG_SESION.diff('id_usuario_solicita', TO_CHAR(:OLD.id_usuario_solicita), TO_CHAR(:NEW.id_usuario_solicita));
        v_det := v_det || PKG_SESION.diff('id_usuario_autoriza', TO_CHAR(:OLD.id_usuario_autoriza), TO_CHAR(:NEW.id_usuario_autoriza));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Ventas y comprobantes', 'EDITAR', 'NOTA_CREDITO', :NEW.id_nota_credito, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_nota_credito', TO_CHAR(:OLD.id_nota_credito)) || PKG_SESION.par('motivo', :OLD.motivo) || PKG_SESION.par('codigo_motivo', :OLD.codigo_motivo) || PKG_SESION.par('forma_devolucion', :OLD.forma_devolucion) || PKG_SESION.par('id_usuario_solicita', TO_CHAR(:OLD.id_usuario_solicita)) || PKG_SESION.par('id_usuario_autoriza', TO_CHAR(:OLD.id_usuario_autoriza));
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'NOTA_CREDITO', :OLD.id_nota_credito, v_det, NULL);
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
        v_det := PKG_SESION.par('id_transaccion', TO_CHAR(:NEW.id_transaccion)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:NEW.id_medio_pago)) || PKG_SESION.par('tipo_operacion', :NEW.tipo_operacion) || PKG_SESION.par('canal', :NEW.canal) || PKG_SESION.par('id_cita', TO_CHAR(:NEW.id_cita)) || PKG_SESION.par('id_transaccion_origen', TO_CHAR(:NEW.id_transaccion_origen)) || PKG_SESION.par('id_nota_credito', TO_CHAR(:NEW.id_nota_credito)) || PKG_SESION.par('codigo_operacion', :NEW.codigo_operacion) || PKG_SESION.par('enlace_pago', :NEW.enlace_pago) || PKG_SESION.par('fecha_expiracion', TO_CHAR(:NEW.fecha_expiracion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('monto', TO_CHAR(:NEW.monto)) || PKG_SESION.par('comision', TO_CHAR(:NEW.comision)) || PKG_SESION.par('fecha_abono', TO_CHAR(:NEW.fecha_abono, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('mensaje_respuesta', :NEW.mensaje_respuesta) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('fecha_hora', TO_CHAR(:NEW.fecha_hora, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_respuesta', TO_CHAR(:NEW.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Ventas y comprobantes', 'CREAR', 'TRANSACCION_PASARELA', :NEW.id_transaccion, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('id_medio_pago', TO_CHAR(:OLD.id_medio_pago), TO_CHAR(:NEW.id_medio_pago));
        v_det := v_det || PKG_SESION.diff('tipo_operacion', :OLD.tipo_operacion, :NEW.tipo_operacion);
        v_det := v_det || PKG_SESION.diff('canal', :OLD.canal, :NEW.canal);
        v_det := v_det || PKG_SESION.diff('id_cita', TO_CHAR(:OLD.id_cita), TO_CHAR(:NEW.id_cita));
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
        v_det := PKG_SESION.par('id_transaccion', TO_CHAR(:OLD.id_transaccion)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:OLD.id_medio_pago)) || PKG_SESION.par('tipo_operacion', :OLD.tipo_operacion) || PKG_SESION.par('canal', :OLD.canal) || PKG_SESION.par('id_cita', TO_CHAR(:OLD.id_cita)) || PKG_SESION.par('id_transaccion_origen', TO_CHAR(:OLD.id_transaccion_origen)) || PKG_SESION.par('id_nota_credito', TO_CHAR(:OLD.id_nota_credito)) || PKG_SESION.par('codigo_operacion', :OLD.codigo_operacion) || PKG_SESION.par('enlace_pago', :OLD.enlace_pago) || PKG_SESION.par('fecha_expiracion', TO_CHAR(:OLD.fecha_expiracion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('monto', TO_CHAR(:OLD.monto)) || PKG_SESION.par('comision', TO_CHAR(:OLD.comision)) || PKG_SESION.par('fecha_abono', TO_CHAR(:OLD.fecha_abono, 'YYYY-MM-DD')) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('mensaje_respuesta', :OLD.mensaje_respuesta) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('fecha_hora', TO_CHAR(:OLD.fecha_hora, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('fecha_respuesta', TO_CHAR(:OLD.fecha_respuesta, 'YYYY-MM-DD HH24:MI:SS'));
        PKG_SESION.auditar('Ventas y comprobantes', 'ELIMINAR', 'TRANSACCION_PASARELA', :OLD.id_transaccion, v_det, NULL);
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
        v_det := PKG_SESION.par('id_caja', TO_CHAR(:NEW.id_caja)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('id_usuario_apertura', TO_CHAR(:NEW.id_usuario_apertura)) || PKG_SESION.par('fecha_apertura', TO_CHAR(:NEW.fecha_apertura, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('monto_inicial', TO_CHAR(:NEW.monto_inicial)) || PKG_SESION.par('id_usuario_cierre', TO_CHAR(:NEW.id_usuario_cierre)) || PKG_SESION.par('fecha_cierre', TO_CHAR(:NEW.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario_revisa', TO_CHAR(:NEW.id_usuario_revisa)) || PKG_SESION.par('fecha_revision', TO_CHAR(:NEW.fecha_revision, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :NEW.estado);
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
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Caja y conciliación', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'CAJA', :NEW.id_caja, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_caja', TO_CHAR(:OLD.id_caja)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('id_usuario_apertura', TO_CHAR(:OLD.id_usuario_apertura)) || PKG_SESION.par('fecha_apertura', TO_CHAR(:OLD.fecha_apertura, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('monto_inicial', TO_CHAR(:OLD.monto_inicial)) || PKG_SESION.par('id_usuario_cierre', TO_CHAR(:OLD.id_usuario_cierre)) || PKG_SESION.par('fecha_cierre', TO_CHAR(:OLD.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario_revisa', TO_CHAR(:OLD.id_usuario_revisa)) || PKG_SESION.par('fecha_revision', TO_CHAR(:OLD.fecha_revision, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Caja y conciliación', 'ELIMINAR', 'CAJA', :OLD.id_caja, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CAJA_EGRESO...
CREATE OR REPLACE TRIGGER TRG_AUD_CAJA_EGRESO
AFTER INSERT OR UPDATE OR DELETE ON CAJA_EGRESO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_egreso', TO_CHAR(:NEW.id_egreso)) || PKG_SESION.par('id_caja', TO_CHAR(:NEW.id_caja)) || PKG_SESION.par('concepto', :NEW.concepto) || PKG_SESION.par('monto', TO_CHAR(:NEW.monto)) || PKG_SESION.par('fecha', TO_CHAR(:NEW.fecha, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario));
        PKG_SESION.auditar('Caja y conciliación', 'CREAR', 'CAJA_EGRESO', :NEW.id_egreso, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_caja', TO_CHAR(:OLD.id_caja), TO_CHAR(:NEW.id_caja));
        v_det := v_det || PKG_SESION.diff('concepto', :OLD.concepto, :NEW.concepto);
        v_det := v_det || PKG_SESION.diff('monto', TO_CHAR(:OLD.monto), TO_CHAR(:NEW.monto));
        v_det := v_det || PKG_SESION.diff('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Caja y conciliación', 'EDITAR', 'CAJA_EGRESO', :NEW.id_egreso, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_egreso', TO_CHAR(:OLD.id_egreso)) || PKG_SESION.par('id_caja', TO_CHAR(:OLD.id_caja)) || PKG_SESION.par('concepto', :OLD.concepto) || PKG_SESION.par('monto', TO_CHAR(:OLD.monto)) || PKG_SESION.par('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario));
        PKG_SESION.auditar('Caja y conciliación', 'ELIMINAR', 'CAJA_EGRESO', :OLD.id_egreso, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CAJA_ARQUEO...
CREATE OR REPLACE TRIGGER TRG_AUD_CAJA_ARQUEO
AFTER INSERT OR UPDATE OR DELETE ON CAJA_ARQUEO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_arqueo', TO_CHAR(:NEW.id_arqueo)) || PKG_SESION.par('id_caja', TO_CHAR(:NEW.id_caja)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:NEW.id_medio_pago)) || PKG_SESION.par('monto_esperado', TO_CHAR(:NEW.monto_esperado)) || PKG_SESION.par('monto_contado', TO_CHAR(:NEW.monto_contado)) || PKG_SESION.par('diferencia', TO_CHAR(:NEW.diferencia)) || PKG_SESION.par('observacion', :NEW.observacion);
        PKG_SESION.auditar('Caja y conciliación', 'CREAR', 'CAJA_ARQUEO', :NEW.id_arqueo, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_caja', TO_CHAR(:OLD.id_caja), TO_CHAR(:NEW.id_caja));
        v_det := v_det || PKG_SESION.diff('id_medio_pago', TO_CHAR(:OLD.id_medio_pago), TO_CHAR(:NEW.id_medio_pago));
        v_det := v_det || PKG_SESION.diff('monto_esperado', TO_CHAR(:OLD.monto_esperado), TO_CHAR(:NEW.monto_esperado));
        v_det := v_det || PKG_SESION.diff('monto_contado', TO_CHAR(:OLD.monto_contado), TO_CHAR(:NEW.monto_contado));
        v_det := v_det || PKG_SESION.diff('diferencia', TO_CHAR(:OLD.diferencia), TO_CHAR(:NEW.diferencia));
        v_det := v_det || PKG_SESION.diff('observacion', :OLD.observacion, :NEW.observacion);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Caja y conciliación', 'EDITAR', 'CAJA_ARQUEO', :NEW.id_arqueo, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_arqueo', TO_CHAR(:OLD.id_arqueo)) || PKG_SESION.par('id_caja', TO_CHAR(:OLD.id_caja)) || PKG_SESION.par('id_medio_pago', TO_CHAR(:OLD.id_medio_pago)) || PKG_SESION.par('monto_esperado', TO_CHAR(:OLD.monto_esperado)) || PKG_SESION.par('monto_contado', TO_CHAR(:OLD.monto_contado)) || PKG_SESION.par('diferencia', TO_CHAR(:OLD.diferencia)) || PKG_SESION.par('observacion', :OLD.observacion);
        PKG_SESION.auditar('Caja y conciliación', 'ELIMINAR', 'CAJA_ARQUEO', :OLD.id_arqueo, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CONCILIACION...
CREATE OR REPLACE TRIGGER TRG_AUD_CONCILIACION
AFTER INSERT OR UPDATE OR DELETE ON CONCILIACION
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_conciliacion', TO_CHAR(:NEW.id_conciliacion)) || PKG_SESION.par('anio', TO_CHAR(:NEW.anio)) || PKG_SESION.par('mes', TO_CHAR(:NEW.mes)) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('fecha_cierre', TO_CHAR(:NEW.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Caja y conciliación', 'CREAR', 'CONCILIACION', :NEW.id_conciliacion, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('anio', TO_CHAR(:OLD.anio), TO_CHAR(:NEW.anio));
        v_det := v_det || PKG_SESION.diff('mes', TO_CHAR(:OLD.mes), TO_CHAR(:NEW.mes));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('fecha_cierre', TO_CHAR(:OLD.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Caja y conciliación', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'CONCILIACION', :NEW.id_conciliacion, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_conciliacion', TO_CHAR(:OLD.id_conciliacion)) || PKG_SESION.par('anio', TO_CHAR(:OLD.anio)) || PKG_SESION.par('mes', TO_CHAR(:OLD.mes)) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('fecha_cierre', TO_CHAR(:OLD.fecha_cierre, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Caja y conciliación', 'ELIMINAR', 'CONCILIACION', :OLD.id_conciliacion, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CONCILIACION_ITEM...
CREATE OR REPLACE TRIGGER TRG_AUD_CONCILIACION_ITEM
AFTER INSERT OR UPDATE OR DELETE ON CONCILIACION_ITEM
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_item', TO_CHAR(:NEW.id_item)) || PKG_SESION.par('id_conciliacion', TO_CHAR(:NEW.id_conciliacion)) || PKG_SESION.par('id_venta', TO_CHAR(:NEW.id_venta)) || PKG_SESION.par('referencia_sunat', :NEW.referencia_sunat) || PKG_SESION.par('id_transaccion', TO_CHAR(:NEW.id_transaccion)) || PKG_SESION.par('aceptado_sunat', :NEW.aceptado_sunat) || PKG_SESION.par('pago_respaldado', :NEW.pago_respaldado) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('justificacion', :NEW.justificacion);
        PKG_SESION.auditar('Caja y conciliación', 'CREAR', 'CONCILIACION_ITEM', :NEW.id_item, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_conciliacion', TO_CHAR(:OLD.id_conciliacion), TO_CHAR(:NEW.id_conciliacion));
        v_det := v_det || PKG_SESION.diff('id_venta', TO_CHAR(:OLD.id_venta), TO_CHAR(:NEW.id_venta));
        v_det := v_det || PKG_SESION.diff('referencia_sunat', :OLD.referencia_sunat, :NEW.referencia_sunat);
        v_det := v_det || PKG_SESION.diff('id_transaccion', TO_CHAR(:OLD.id_transaccion), TO_CHAR(:NEW.id_transaccion));
        v_det := v_det || PKG_SESION.diff('aceptado_sunat', :OLD.aceptado_sunat, :NEW.aceptado_sunat);
        v_det := v_det || PKG_SESION.diff('pago_respaldado', :OLD.pago_respaldado, :NEW.pago_respaldado);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('justificacion', :OLD.justificacion, :NEW.justificacion);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Caja y conciliación', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'CONCILIACION_ITEM', :NEW.id_item, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_item', TO_CHAR(:OLD.id_item)) || PKG_SESION.par('id_conciliacion', TO_CHAR(:OLD.id_conciliacion)) || PKG_SESION.par('id_venta', TO_CHAR(:OLD.id_venta)) || PKG_SESION.par('referencia_sunat', :OLD.referencia_sunat) || PKG_SESION.par('id_transaccion', TO_CHAR(:OLD.id_transaccion)) || PKG_SESION.par('aceptado_sunat', :OLD.aceptado_sunat) || PKG_SESION.par('pago_respaldado', :OLD.pago_respaldado) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('justificacion', :OLD.justificacion);
        PKG_SESION.auditar('Caja y conciliación', 'ELIMINAR', 'CONCILIACION_ITEM', :OLD.id_item, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_CATEGORIA_PRODUCTO...
CREATE OR REPLACE TRIGGER TRG_AUD_CATEGORIA_PRODUCTO
AFTER INSERT OR UPDATE OR DELETE ON CATEGORIA_PRODUCTO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_categoria', TO_CHAR(:NEW.id_categoria)) || PKG_SESION.par('nombre', :NEW.nombre);
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'CATEGORIA_PRODUCTO', :NEW.id_categoria, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', 'EDITAR', 'CATEGORIA_PRODUCTO', :NEW.id_categoria, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_categoria', TO_CHAR(:OLD.id_categoria)) || PKG_SESION.par('nombre', :OLD.nombre);
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'CATEGORIA_PRODUCTO', :OLD.id_categoria, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_PRODUCTO...
CREATE OR REPLACE TRIGGER TRG_AUD_PRODUCTO
AFTER INSERT OR UPDATE OR DELETE ON PRODUCTO
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('id_categoria', TO_CHAR(:NEW.id_categoria)) || PKG_SESION.par('codigo', :NEW.codigo) || PKG_SESION.par('nombre', :NEW.nombre) || PKG_SESION.par('marca', :NEW.marca) || PKG_SESION.par('unidad', :NEW.unidad) || PKG_SESION.par('tipo_uso', :NEW.tipo_uso) || PKG_SESION.par('precio_venta', TO_CHAR(:NEW.precio_venta)) || PKG_SESION.par('costo', TO_CHAR(:NEW.costo)) || PKG_SESION.par('es_premium', :NEW.es_premium) || PKG_SESION.par('estado', :NEW.estado);
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'PRODUCTO', :NEW.id_producto, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_categoria', TO_CHAR(:OLD.id_categoria), TO_CHAR(:NEW.id_categoria));
        v_det := v_det || PKG_SESION.diff('codigo', :OLD.codigo, :NEW.codigo);
        v_det := v_det || PKG_SESION.diff('nombre', :OLD.nombre, :NEW.nombre);
        v_det := v_det || PKG_SESION.diff('marca', :OLD.marca, :NEW.marca);
        v_det := v_det || PKG_SESION.diff('unidad', :OLD.unidad, :NEW.unidad);
        v_det := v_det || PKG_SESION.diff('tipo_uso', :OLD.tipo_uso, :NEW.tipo_uso);
        v_det := v_det || PKG_SESION.diff('precio_venta', TO_CHAR(:OLD.precio_venta), TO_CHAR(:NEW.precio_venta));
        v_det := v_det || PKG_SESION.diff('costo', TO_CHAR(:OLD.costo), TO_CHAR(:NEW.costo));
        v_det := v_det || PKG_SESION.diff('es_premium', :OLD.es_premium, :NEW.es_premium);
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'PRODUCTO', :NEW.id_producto, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('id_categoria', TO_CHAR(:OLD.id_categoria)) || PKG_SESION.par('codigo', :OLD.codigo) || PKG_SESION.par('nombre', :OLD.nombre) || PKG_SESION.par('marca', :OLD.marca) || PKG_SESION.par('unidad', :OLD.unidad) || PKG_SESION.par('tipo_uso', :OLD.tipo_uso) || PKG_SESION.par('precio_venta', TO_CHAR(:OLD.precio_venta)) || PKG_SESION.par('costo', TO_CHAR(:OLD.costo)) || PKG_SESION.par('es_premium', :OLD.es_premium) || PKG_SESION.par('estado', :OLD.estado);
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'PRODUCTO', :OLD.id_producto, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_INVENTARIO_SEDE...
CREATE OR REPLACE TRIGGER TRG_AUD_INVENTARIO_SEDE
AFTER INSERT OR UPDATE OR DELETE ON INVENTARIO_SEDE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('stock_actual', TO_CHAR(:NEW.stock_actual)) || PKG_SESION.par('stock_minimo', TO_CHAR(:NEW.stock_minimo)) || PKG_SESION.par('ubicacion', :NEW.ubicacion);
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'INVENTARIO_SEDE', :NEW.id_producto, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('stock_actual', TO_CHAR(:OLD.stock_actual), TO_CHAR(:NEW.stock_actual));
        v_det := v_det || PKG_SESION.diff('stock_minimo', TO_CHAR(:OLD.stock_minimo), TO_CHAR(:NEW.stock_minimo));
        v_det := v_det || PKG_SESION.diff('ubicacion', :OLD.ubicacion, :NEW.ubicacion);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', 'EDITAR', 'INVENTARIO_SEDE', :NEW.id_producto, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('stock_actual', TO_CHAR(:OLD.stock_actual)) || PKG_SESION.par('stock_minimo', TO_CHAR(:OLD.stock_minimo)) || PKG_SESION.par('ubicacion', :OLD.ubicacion);
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'INVENTARIO_SEDE', :OLD.id_producto, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_MOVIMIENTO_KARDEX...
CREATE OR REPLACE TRIGGER TRG_AUD_MOVIMIENTO_KARDEX
AFTER INSERT OR UPDATE OR DELETE ON MOVIMIENTO_KARDEX
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_movimiento', TO_CHAR(:NEW.id_movimiento)) || PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('tipo', :NEW.tipo) || PKG_SESION.par('motivo', :NEW.motivo) || PKG_SESION.par('cantidad', TO_CHAR(:NEW.cantidad)) || PKG_SESION.par('saldo', TO_CHAR(:NEW.saldo)) || PKG_SESION.par('fecha_hora', TO_CHAR(:NEW.fecha_hora, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('id_detalle_venta', TO_CHAR(:NEW.id_detalle_venta)) || PKG_SESION.par('id_detalle_compra', TO_CHAR(:NEW.id_detalle_compra)) || PKG_SESION.par('id_atencion', TO_CHAR(:NEW.id_atencion));
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'MOVIMIENTO_KARDEX', :NEW.id_movimiento, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_producto', TO_CHAR(:OLD.id_producto), TO_CHAR(:NEW.id_producto));
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('tipo', :OLD.tipo, :NEW.tipo);
        v_det := v_det || PKG_SESION.diff('motivo', :OLD.motivo, :NEW.motivo);
        v_det := v_det || PKG_SESION.diff('cantidad', TO_CHAR(:OLD.cantidad), TO_CHAR(:NEW.cantidad));
        v_det := v_det || PKG_SESION.diff('saldo', TO_CHAR(:OLD.saldo), TO_CHAR(:NEW.saldo));
        v_det := v_det || PKG_SESION.diff('fecha_hora', TO_CHAR(:OLD.fecha_hora, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_hora, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('id_detalle_venta', TO_CHAR(:OLD.id_detalle_venta), TO_CHAR(:NEW.id_detalle_venta));
        v_det := v_det || PKG_SESION.diff('id_detalle_compra', TO_CHAR(:OLD.id_detalle_compra), TO_CHAR(:NEW.id_detalle_compra));
        v_det := v_det || PKG_SESION.diff('id_atencion', TO_CHAR(:OLD.id_atencion), TO_CHAR(:NEW.id_atencion));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', 'EDITAR', 'MOVIMIENTO_KARDEX', :NEW.id_movimiento, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_movimiento', TO_CHAR(:OLD.id_movimiento)) || PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('tipo', :OLD.tipo) || PKG_SESION.par('motivo', :OLD.motivo) || PKG_SESION.par('cantidad', TO_CHAR(:OLD.cantidad)) || PKG_SESION.par('saldo', TO_CHAR(:OLD.saldo)) || PKG_SESION.par('fecha_hora', TO_CHAR(:OLD.fecha_hora, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('id_detalle_venta', TO_CHAR(:OLD.id_detalle_venta)) || PKG_SESION.par('id_detalle_compra', TO_CHAR(:OLD.id_detalle_compra)) || PKG_SESION.par('id_atencion', TO_CHAR(:OLD.id_atencion));
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'MOVIMIENTO_KARDEX', :OLD.id_movimiento, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_REQUERIMIENTO_COMPRA...
CREATE OR REPLACE TRIGGER TRG_AUD_REQUERIMIENTO_COMPRA
AFTER INSERT OR UPDATE OR DELETE ON REQUERIMIENTO_COMPRA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_requerimiento', TO_CHAR(:NEW.id_requerimiento)) || PKG_SESION.par('numero', :NEW.numero) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('area_solicitante', :NEW.area_solicitante) || PKG_SESION.par('linea_produccion', :NEW.linea_produccion) || PKG_SESION.par('tipo_adquisicion', :NEW.tipo_adquisicion) || PKG_SESION.par('prioridad', :NEW.prioridad) || PKG_SESION.par('id_usuario_solicita', TO_CHAR(:NEW.id_usuario_solicita)) || PKG_SESION.par('fecha_solicitud', TO_CHAR(:NEW.fecha_solicitud, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :NEW.estado) || PKG_SESION.par('id_usuario_aprueba', TO_CHAR(:NEW.id_usuario_aprueba)) || PKG_SESION.par('fecha_resolucion', TO_CHAR(:NEW.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('motivo_rechazo', :NEW.motivo_rechazo);
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'REQUERIMIENTO_COMPRA', :NEW.id_requerimiento, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('numero', :OLD.numero, :NEW.numero);
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('area_solicitante', :OLD.area_solicitante, :NEW.area_solicitante);
        v_det := v_det || PKG_SESION.diff('linea_produccion', :OLD.linea_produccion, :NEW.linea_produccion);
        v_det := v_det || PKG_SESION.diff('tipo_adquisicion', :OLD.tipo_adquisicion, :NEW.tipo_adquisicion);
        v_det := v_det || PKG_SESION.diff('prioridad', :OLD.prioridad, :NEW.prioridad);
        v_det := v_det || PKG_SESION.diff('id_usuario_solicita', TO_CHAR(:OLD.id_usuario_solicita), TO_CHAR(:NEW.id_usuario_solicita));
        v_det := v_det || PKG_SESION.diff('fecha_solicitud', TO_CHAR(:OLD.fecha_solicitud, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_solicitud, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('estado', :OLD.estado, :NEW.estado);
        v_det := v_det || PKG_SESION.diff('id_usuario_aprueba', TO_CHAR(:OLD.id_usuario_aprueba), TO_CHAR(:NEW.id_usuario_aprueba));
        v_det := v_det || PKG_SESION.diff('fecha_resolucion', TO_CHAR(:OLD.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(:NEW.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS'));
        v_det := v_det || PKG_SESION.diff('motivo_rechazo', :OLD.motivo_rechazo, :NEW.motivo_rechazo);
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', CASE WHEN :NEW.estado IN ('ANULADA', 'ANULADO', 'INACTIVO', 'INACTIVA', 'CANCELADA', 'RECHAZADA', 'RECHAZADO') AND PKG_SESION.distinto(:OLD.estado, :NEW.estado) THEN 'ANULAR/DESACTIVAR' ELSE 'EDITAR' END, 'REQUERIMIENTO_COMPRA', :NEW.id_requerimiento, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_requerimiento', TO_CHAR(:OLD.id_requerimiento)) || PKG_SESION.par('numero', :OLD.numero) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('area_solicitante', :OLD.area_solicitante) || PKG_SESION.par('linea_produccion', :OLD.linea_produccion) || PKG_SESION.par('tipo_adquisicion', :OLD.tipo_adquisicion) || PKG_SESION.par('prioridad', :OLD.prioridad) || PKG_SESION.par('id_usuario_solicita', TO_CHAR(:OLD.id_usuario_solicita)) || PKG_SESION.par('fecha_solicitud', TO_CHAR(:OLD.fecha_solicitud, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('estado', :OLD.estado) || PKG_SESION.par('id_usuario_aprueba', TO_CHAR(:OLD.id_usuario_aprueba)) || PKG_SESION.par('fecha_resolucion', TO_CHAR(:OLD.fecha_resolucion, 'YYYY-MM-DD HH24:MI:SS')) || PKG_SESION.par('motivo_rechazo', :OLD.motivo_rechazo);
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'REQUERIMIENTO_COMPRA', :OLD.id_requerimiento, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_REQUERIMIENTO_DETALLE...
CREATE OR REPLACE TRIGGER TRG_AUD_REQUERIMIENTO_DETALLE
AFTER INSERT OR UPDATE OR DELETE ON REQUERIMIENTO_DETALLE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_requerimiento', TO_CHAR(:NEW.id_requerimiento)) || PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('id_proveedor', TO_CHAR(:NEW.id_proveedor)) || PKG_SESION.par('cantidad', TO_CHAR(:NEW.cantidad));
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'REQUERIMIENTO_DETALLE', :NEW.id_requerimiento, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_proveedor', TO_CHAR(:OLD.id_proveedor), TO_CHAR(:NEW.id_proveedor));
        v_det := v_det || PKG_SESION.diff('cantidad', TO_CHAR(:OLD.cantidad), TO_CHAR(:NEW.cantidad));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', 'EDITAR', 'REQUERIMIENTO_DETALLE', :NEW.id_requerimiento, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_requerimiento', TO_CHAR(:OLD.id_requerimiento)) || PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('id_proveedor', TO_CHAR(:OLD.id_proveedor)) || PKG_SESION.par('cantidad', TO_CHAR(:OLD.cantidad));
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'REQUERIMIENTO_DETALLE', :OLD.id_requerimiento, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_COMPRA...
CREATE OR REPLACE TRIGGER TRG_AUD_COMPRA
AFTER INSERT OR UPDATE OR DELETE ON COMPRA
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_compra', TO_CHAR(:NEW.id_compra)) || PKG_SESION.par('id_sede', TO_CHAR(:NEW.id_sede)) || PKG_SESION.par('id_proveedor', TO_CHAR(:NEW.id_proveedor)) || PKG_SESION.par('id_requerimiento', TO_CHAR(:NEW.id_requerimiento)) || PKG_SESION.par('documento', :NEW.documento) || PKG_SESION.par('fecha', TO_CHAR(:NEW.fecha, 'YYYY-MM-DD')) || PKG_SESION.par('id_usuario', TO_CHAR(:NEW.id_usuario)) || PKG_SESION.par('total', TO_CHAR(:NEW.total));
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'COMPRA', :NEW.id_compra, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_sede', TO_CHAR(:OLD.id_sede), TO_CHAR(:NEW.id_sede));
        v_det := v_det || PKG_SESION.diff('id_proveedor', TO_CHAR(:OLD.id_proveedor), TO_CHAR(:NEW.id_proveedor));
        v_det := v_det || PKG_SESION.diff('id_requerimiento', TO_CHAR(:OLD.id_requerimiento), TO_CHAR(:NEW.id_requerimiento));
        v_det := v_det || PKG_SESION.diff('documento', :OLD.documento, :NEW.documento);
        v_det := v_det || PKG_SESION.diff('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD'), TO_CHAR(:NEW.fecha, 'YYYY-MM-DD'));
        v_det := v_det || PKG_SESION.diff('id_usuario', TO_CHAR(:OLD.id_usuario), TO_CHAR(:NEW.id_usuario));
        v_det := v_det || PKG_SESION.diff('total', TO_CHAR(:OLD.total), TO_CHAR(:NEW.total));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', 'EDITAR', 'COMPRA', :NEW.id_compra, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_compra', TO_CHAR(:OLD.id_compra)) || PKG_SESION.par('id_sede', TO_CHAR(:OLD.id_sede)) || PKG_SESION.par('id_proveedor', TO_CHAR(:OLD.id_proveedor)) || PKG_SESION.par('id_requerimiento', TO_CHAR(:OLD.id_requerimiento)) || PKG_SESION.par('documento', :OLD.documento) || PKG_SESION.par('fecha', TO_CHAR(:OLD.fecha, 'YYYY-MM-DD')) || PKG_SESION.par('id_usuario', TO_CHAR(:OLD.id_usuario)) || PKG_SESION.par('total', TO_CHAR(:OLD.total));
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'COMPRA', :OLD.id_compra, v_det, NULL);
    END IF;
END;
/

PROMPT Trigger TRG_AUD_COMPRA_DETALLE...
CREATE OR REPLACE TRIGGER TRG_AUD_COMPRA_DETALLE
AFTER INSERT OR UPDATE OR DELETE ON COMPRA_DETALLE
FOR EACH ROW
DECLARE
    v_det VARCHAR2(32767);
BEGIN
    IF INSERTING THEN
        v_det := PKG_SESION.par('id_detalle_compra', TO_CHAR(:NEW.id_detalle_compra)) || PKG_SESION.par('id_compra', TO_CHAR(:NEW.id_compra)) || PKG_SESION.par('id_producto', TO_CHAR(:NEW.id_producto)) || PKG_SESION.par('cantidad_recibida', TO_CHAR(:NEW.cantidad_recibida)) || PKG_SESION.par('costo_unitario', TO_CHAR(:NEW.costo_unitario));
        PKG_SESION.auditar('Inventario y compras', 'CREAR', 'COMPRA_DETALLE', :NEW.id_detalle_compra, v_det, NULL);
    ELSIF UPDATING THEN
        v_det := v_det || PKG_SESION.diff('id_compra', TO_CHAR(:OLD.id_compra), TO_CHAR(:NEW.id_compra));
        v_det := v_det || PKG_SESION.diff('id_producto', TO_CHAR(:OLD.id_producto), TO_CHAR(:NEW.id_producto));
        v_det := v_det || PKG_SESION.diff('cantidad_recibida', TO_CHAR(:OLD.cantidad_recibida), TO_CHAR(:NEW.cantidad_recibida));
        v_det := v_det || PKG_SESION.diff('costo_unitario', TO_CHAR(:OLD.costo_unitario), TO_CHAR(:NEW.costo_unitario));
        IF v_det IS NOT NULL THEN
            PKG_SESION.auditar('Inventario y compras', 'EDITAR', 'COMPRA_DETALLE', :NEW.id_detalle_compra, v_det, NULL);
        END IF;
    ELSE
        v_det := PKG_SESION.par('id_detalle_compra', TO_CHAR(:OLD.id_detalle_compra)) || PKG_SESION.par('id_compra', TO_CHAR(:OLD.id_compra)) || PKG_SESION.par('id_producto', TO_CHAR(:OLD.id_producto)) || PKG_SESION.par('cantidad_recibida', TO_CHAR(:OLD.cantidad_recibida)) || PKG_SESION.par('costo_unitario', TO_CHAR(:OLD.costo_unitario));
        PKG_SESION.auditar('Inventario y compras', 'ELIMINAR', 'COMPRA_DETALLE', :OLD.id_detalle_compra, v_det, NULL);
    END IF;
END;
/

PROMPT 54 triggers de auditoría creados.
