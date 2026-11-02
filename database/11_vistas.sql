-- =====================================================================
-- CAUTIVA SPA E.I.R.L. · BackOffice web · Base de datos Oracle (12c o superior)
-- Script 11 (v5) · Vistas para pantallas, reportes y conciliación (BUC07, RF63-RF76, RF90)
-- Se ejecuta como: dueño del esquema v5, después de 10_triggers_auditoria.sql.
--
-- Reglas comunes:
--   * Solo cuentan las ventas CERRADAS: las ANULADAS se conservan pero no se reportan (RN33)
--     y las ABIERTAS todavía no son ventas.
--   * [v5] La línea (SALON o SPA) es AREA.linea: la tabla LINEA se retiró (catálogo de 2 valores sin atributos).
--   * [v5] Nombres, documento y teléfono de clientas, personal y proveedores se leen del supertipo PERSONA.
--   * [v5] La boleta o factura de una venta es COMPROBANTE con tipo IN ('BOLETA', 'FACTURA'); su nota de crédito
--     es el COMPROBANTE NC_BOLETA o NC_FACTURA de la misma venta (más su fila en NOTA_CREDITO).
--   * [v5] Las sesiones usadas de un paquete son las atenciones con id_cliente_paquete (CONSUMO_SESION se retiró).
--   * Las vistas con costos, márgenes, comisiones o planilla son solo para la Administradora (RN24):
--     el script 16 no se las da al rol de reportes.
--   * [v4] Comprobantes electrónicos con su estado en SUNAT, pasarela de pagos y consultas DNI/RUC
--     (vistas V_CPE_PENDIENTES, V_PASARELA_SIN_USO, V_DEVOLUCIONES_PENDIENTES, V_PASARELA_LIQUIDACION).
--   * [v5] V_CONSULTAS_DOC_MES (leía CONSULTA_DOCUMENTO) pasa a V_DOCUMENTOS_VERIFICADOS (lee PERSONA y TIPO_DOCUMENTO).
-- =====================================================================
SET DEFINE OFF

PROMPT Vistas de ventas...
CREATE OR REPLACE VIEW V_VENTA_VALIDA AS
SELECT v.id_venta,
       v.id_sede,
       s.nombre                                    AS sede,
       v.id_caja,
       CAST(v.fecha_hora AS DATE)                  AS fecha_hora,
       TRUNC(v.fecha_hora)                         AS fecha,
       EXTRACT(YEAR FROM v.fecha_hora)             AS anio,
       EXTRACT(MONTH FROM v.fecha_hora)            AS mes,
       v.id_cliente,
       CASE WHEN v.id_cliente IS NULL THEN 'Cliente de paso'
            ELSE TRIM(pc.nombres || ' ' || pc.apellidos) END AS cliente,
       v.id_usuario,
       v.subtotal,
       v.igv,
       v.subtotal + v.igv                          AS importe,
       v.descuento,
       v.adelanto_aplicado,
       v.saldo_favor_aplicado,
       v.total,
       cp.tipo                                     AS tipo_comprobante,
       cp.serie,
       cp.numero
  FROM VENTA v
  JOIN SEDE s             ON s.id_sede = v.id_sede
  LEFT JOIN PERSONA pc     ON pc.id_persona = v.id_cliente
  LEFT JOIN COMPROBANTE cp ON cp.id_venta = v.id_venta AND cp.tipo IN ('BOLETA', 'FACTURA')
 WHERE v.estado = 'CERRADA';

COMMENT ON TABLE V_VENTA_VALIDA IS 'Ventas cerradas (sin anuladas ni abiertas) con su comprobante. Base de los reportes de ventas (RF68, RN33).';

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
       d.subtotal
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

COMMENT ON TABLE V_VENTA_LINEA IS 'Líneas de ventas cerradas con línea (SALON/SPA), área, servicio, producto, especialista y tipo de atención (RF68, RF73).';

CREATE OR REPLACE VIEW V_VENTAS_DIARIAS AS
SELECT id_sede,
       fecha,
       anio,
       mes,
       linea,
       tipo_item,
       COUNT(*)      AS lineas,
       SUM(cantidad) AS unidades,
       SUM(subtotal) AS importe_con_igv
  FROM V_VENTA_LINEA
 GROUP BY id_sede, fecha, anio, mes, linea, tipo_item;

COMMENT ON TABLE V_VENTAS_DIARIAS IS 'Ventas por sede, día y línea (SALON, SPA, Productos, Paquetes). Importe de las líneas, antes del descuento general de la venta (RF68).';

CREATE OR REPLACE VIEW V_PAGOS_POR_MEDIO AS
SELECT 'VENTA'                  AS origen,
       v.id_sede,
       v.id_caja,
       TRUNC(v.fecha_hora)      AS fecha,
       m.nombre                 AS medio_pago,
       p.monto,
       p.referencia,
       v.id_venta               AS id_documento,
       p.id_transaccion
  FROM VENTA_PAGO p
  JOIN VENTA v      ON v.id_venta = p.id_venta AND v.estado = 'CERRADA'
  JOIN MEDIO_PAGO m ON m.id_medio_pago = p.id_medio_pago
UNION ALL
SELECT 'ADELANTO',
       c.id_sede,
       c.id_caja,
       TRUNC(c.fecha_adelanto),
       m.nombre,
       c.adelanto_monto,
       (SELECT MAX(SUBSTR(t.codigo_operacion, 1, 30)) FROM TRANSACCION_PASARELA t
         WHERE t.id_cita = c.id_cita AND t.tipo_operacion = 'COBRO' AND t.estado = 'APROBADA'),
       c.id_cita,
       (SELECT MAX(t.id_transaccion) FROM TRANSACCION_PASARELA t
         WHERE t.id_cita = c.id_cita AND t.tipo_operacion = 'COBRO' AND t.estado = 'APROBADA')
  FROM CITA c
  JOIN MEDIO_PAGO m ON m.id_medio_pago = c.id_medio_pago
 WHERE c.adelanto_monto > 0
   AND c.id_cita_origen IS NULL;

COMMENT ON TABLE V_PAGOS_POR_MEDIO IS 'Dinero recibido por medio de pago: cobros de ventas cerradas y adelantos de citas; los digitales con su transacción de la pasarela (RF63, RF68, RF75).';

CREATE OR REPLACE VIEW V_COMPROBANTES_MES AS
SELECT EXTRACT(YEAR FROM cp.fecha_emision)  AS anio,
       EXTRACT(MONTH FROM cp.fecha_emision) AS mes,
       v.id_sede,
       cp.tipo,
       cp.serie,
       cp.numero,
       cp.fecha_emision,
       v.id_venta,
       v.subtotal + v.igv                   AS importe,
       v.estado                             AS estado_venta,
       nc.serie                             AS nc_serie,
       nc.numero                            AS nc_numero,
       nc.fecha_emision                     AS nc_fecha,
       cp.estado_sunat,
       nc.estado_sunat                      AS nc_estado_sunat
  FROM COMPROBANTE cp
  JOIN VENTA v             ON v.id_venta = cp.id_venta AND v.estado IN ('CERRADA', 'ANULADA')
  LEFT JOIN COMPROBANTE nc ON nc.id_venta = v.id_venta AND nc.tipo = 'NC_' || cp.tipo
 WHERE cp.tipo IN ('BOLETA', 'FACTURA');

COMMENT ON TABLE V_COMPROBANTES_MES IS 'Comprobantes electrónicos y sus notas de crédito con su estado en SUNAT, para la conciliación mensual (RF75, RN31).';

CREATE OR REPLACE VIEW V_VENTAS_SIN_COMPROBANTE AS
SELECT v.id_venta, v.id_sede, CAST(v.fecha_hora AS DATE) AS fecha_hora, v.estado, v.total
  FROM VENTA v
 WHERE v.estado IN ('CERRADA', 'ANULADA')
   AND NOT EXISTS (SELECT 1 FROM COMPROBANTE c WHERE c.id_venta = v.id_venta AND c.tipo IN ('BOLETA', 'FACTURA'));

COMMENT ON TABLE V_VENTAS_SIN_COMPROBANTE IS 'Ventas sin comprobante registrado (RF76). Con los triggers debe estar vacía; sirve para auditar cargas de datos.';

PROMPT Vistas de agenda y demanda...
CREATE OR REPLACE VIEW V_AGENDA_BLOQUES AS
SELECT 'CITA'                 AS origen,
       c.id_cita              AS id_origen,
       c.id_sede,
       c.id_personal,
       c.id_recurso,
       c.id_variante,
       c.fecha_hora_inicio    AS inicio,
       c.fecha_hora_fin       AS fin_con_limpieza,
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
       NVL(a.hora_fin, a.hora_inicio + NUMTODSINTERVAL(a.tiempo_declarado_min, 'MINUTE'))
           + NUMTODSINTERVAL(r.minutos_limpieza, 'MINUTE'),
       a.estado
  FROM ATENCION a
  JOIN RECURSO r ON r.id_recurso = a.id_recurso
 WHERE a.id_cita IS NULL;

COMMENT ON TABLE V_AGENDA_BLOQUES IS 'Bloques ocupados de especialistas y recursos (citas y atenciones sin cita, con limpieza). Base de la agenda y de las horas libres (RF06, RF11).';

CREATE OR REPLACE VIEW V_OCUPACION_RECURSO AS
SELECT b.id_sede,
       b.id_recurso,
       r.nombre                                           AS recurso,
       r.tipo,
       TRUNC(b.inicio)                                    AS fecha,
       TO_NUMBER(TO_CHAR(b.inicio, 'HH24'))               AS hora,
       COUNT(*)                                           AS bloques,
       SUM(ROUND((CAST(b.fin_con_limpieza AS DATE) - CAST(b.inicio AS DATE)) * 1440)) AS minutos_ocupados
  FROM V_AGENDA_BLOQUES b
  JOIN RECURSO r ON r.id_recurso = b.id_recurso
 GROUP BY b.id_sede, b.id_recurso, r.nombre, r.tipo, TRUNC(b.inicio), TO_NUMBER(TO_CHAR(b.inicio, 'HH24'));

COMMENT ON TABLE V_OCUPACION_RECURSO IS 'Ocupación de cada recurso por día y por hora de inicio (RF72).';

CREATE OR REPLACE VIEW V_DEMANDA_SERVICIOS AS
SELECT r.id_sede,
       TRUNC(a.hora_inicio)                               AS fecha,
       TRUNC(a.hora_inicio) - TRUNC(a.hora_inicio, 'IW') + 1 AS dia_semana,
       TO_NUMBER(TO_CHAR(a.hora_inicio, 'HH24'))          AS hora,
       ar.linea                                           AS linea,
       se.nombre                                          AS servicio,
       vs.nombre                                          AS variante,
       CASE WHEN a.id_cita IS NULL THEN 'SIN_CITA' ELSE 'CON_CITA' END AS tipo_atencion,
       COUNT(*)                                           AS atenciones
  FROM ATENCION a
  JOIN RECURSO r            ON r.id_recurso = a.id_recurso
  JOIN VARIANTE_SERVICIO vs ON vs.id_variante = a.id_variante
  JOIN SERVICIO se          ON se.id_servicio = vs.id_servicio
  JOIN AREA ar              ON ar.id_area = se.id_area
 GROUP BY r.id_sede, TRUNC(a.hora_inicio), TRUNC(a.hora_inicio) - TRUNC(a.hora_inicio, 'IW') + 1,
          TO_NUMBER(TO_CHAR(a.hora_inicio, 'HH24')), ar.linea, se.nombre, vs.nombre,
          CASE WHEN a.id_cita IS NULL THEN 'SIN_CITA' ELSE 'CON_CITA' END;

COMMENT ON TABLE V_DEMANDA_SERVICIOS IS 'Atenciones por día, día de la semana (1 = lunes), hora y servicio: servicios más pedidos y días pico (RF71).';

CREATE OR REPLACE VIEW V_DEMANDA_NO_ATENDIDA AS
SELECT le.id_sede,
       le.fecha_deseada,
       TRUNC(le.fecha_deseada) - TRUNC(le.fecha_deseada, 'IW') + 1 AS dia_semana,
       se.nombre AS servicio,
       le.estado,
       COUNT(*)  AS clientas
  FROM LISTA_ESPERA le
  JOIN VARIANTE_SERVICIO vs ON vs.id_variante = le.id_variante
  JOIN SERVICIO se          ON se.id_servicio = vs.id_servicio
 GROUP BY le.id_sede, le.fecha_deseada, TRUNC(le.fecha_deseada) - TRUNC(le.fecha_deseada, 'IW') + 1, se.nombre, le.estado;

COMMENT ON TABLE V_DEMANDA_NO_ATENDIDA IS 'Clientas en lista de espera por día y servicio: mide la demanda que se pierde en días pico (RF09).';

PROMPT Vistas de clientas...
CREATE OR REPLACE VIEW V_SALDO_FAVOR_CLIENTE AS
SELECT id_cliente,
       SUM(CASE tipo WHEN 'ABONO' THEN monto ELSE -monto END) AS saldo
  FROM SALDO_FAVOR_MOV
 GROUP BY id_cliente;

COMMENT ON TABLE V_SALDO_FAVOR_CLIENTE IS 'Saldo a favor de cada clienta (no se guarda: se calcula de sus movimientos; no vence, RN05).';

CREATE OR REPLACE VIEW V_SESIONES_PAQUETE AS
SELECT cp.id_cliente_paquete,
       cp.id_cliente,
       cp.id_paquete,
       p.nombre                       AS paquete,
       p.tipo,
       cp.sesiones_totales,
       NVL(u.usadas, 0)               AS sesiones_usadas,
       cp.sesiones_totales - NVL(u.usadas, 0) AS sesiones_restantes,
       cp.fecha_compra,
       cp.fecha_vencimiento,
       cp.estado
  FROM CLIENTE_PAQUETE cp
  JOIN PAQUETE p ON p.id_paquete = cp.id_paquete
  LEFT JOIN (SELECT id_cliente_paquete, COUNT(*) AS usadas
               FROM ATENCION WHERE id_cliente_paquete IS NOT NULL GROUP BY id_cliente_paquete) u
         ON u.id_cliente_paquete = cp.id_cliente_paquete;

COMMENT ON TABLE V_SESIONES_PAQUETE IS 'Sesiones usadas y restantes de cada paquete o membresía (RF02, RN36, RN37).';

CREATE OR REPLACE VIEW V_CLIENTAS_FUERA_CICLO AS
WITH visitas AS (
    SELECT id_cliente, TRUNC(hora_inicio) AS dia
      FROM ATENCION
     WHERE id_cliente IS NOT NULL
     GROUP BY id_cliente, TRUNC(hora_inicio)
), intervalos AS (
    SELECT id_cliente, dia, dia - LAG(dia) OVER (PARTITION BY id_cliente ORDER BY dia) AS dias_desde_anterior
      FROM visitas
)
SELECT i.id_cliente,
       TRIM(p.nombres || ' ' || p.apellidos)      AS clienta,
       p.telefono,
       c.acepta_comunicaciones,
       COUNT(*)                                    AS visitas,
       MAX(i.dia)                                  AS ultima_visita,
       ROUND(AVG(i.dias_desde_anterior))           AS ciclo_promedio_dias,
       TRUNC(SYSDATE) - MAX(i.dia)                 AS dias_sin_venir
  FROM intervalos i
  JOIN CLIENTE c ON c.id_cliente = i.id_cliente
  JOIN PERSONA p ON p.id_persona = c.id_cliente
 WHERE c.estado = 'ACTIVO'
 GROUP BY i.id_cliente, p.nombres, p.apellidos, p.telefono, c.acepta_comunicaciones
HAVING COUNT(*) >= 2
   AND TRUNC(SYSDATE) - MAX(i.dia) > 1.25 * AVG(i.dias_desde_anterior);

COMMENT ON TABLE V_CLIENTAS_FUERA_CICLO IS 'Clientas que superan en 25 % su ciclo habitual sin volver (RF90). Solo contactar a quienes aceptan comunicaciones (RN27).';

PROMPT Vistas de inventario...
CREATE OR REPLACE VIEW V_STOCK_ALERTA AS
SELECT i.id_sede,
       s.nombre        AS sede,
       p.id_producto,
       p.codigo,
       p.nombre        AS producto,
       p.marca,
       p.tipo_uso,
       i.stock_actual,
       i.stock_minimo,
       i.ubicacion
  FROM INVENTARIO_SEDE i
  JOIN PRODUCTO p ON p.id_producto = i.id_producto AND p.estado = 'ACTIVO'
  JOIN SEDE s     ON s.id_sede = i.id_sede
 WHERE i.stock_actual <= i.stock_minimo;

COMMENT ON TABLE V_STOCK_ALERTA IS 'Productos en o bajo su stock mínimo por sede: alerta a almacén y administración (RN16, RF47).';

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
       k.id_atencion
  FROM MOVIMIENTO_KARDEX k
  JOIN PRODUCTO p ON p.id_producto = k.id_producto
  JOIN USUARIO u  ON u.id_usuario = k.id_usuario;

COMMENT ON TABLE V_KARDEX IS 'Kardex por producto y sede con entradas, salidas, ajustes y saldo (RF50, RN18).';

PROMPT Vistas de caja...
CREATE OR REPLACE VIEW V_CAJA_ESPERADO AS
SELECT c.id_caja,
       c.id_sede,
       c.estado,
       m.id_medio_pago,
       m.nombre AS medio_pago,
       PKG_CAJA.esperado(c.id_caja, m.id_medio_pago, c.monto_inicial) AS esperado
  FROM CAJA c
  CROSS JOIN MEDIO_PAGO m
 WHERE m.estado = 'ACTIVO';

COMMENT ON TABLE V_CAJA_ESPERADO IS 'Lo esperado de cada caja por medio de pago, en tiempo real (RF63; tarjetas de la pantalla Caja).';

PROMPT Vistas solo para la Administradora (costos, márgenes, comisiones, planilla: RN24)...
CREATE OR REPLACE VIEW V_COMISIONES_MES AS
SELECT d.id_personal,
       TRIM(pe.nombres || ' ' || pe.apellidos) AS especialista,
       EXTRACT(YEAR FROM v.fecha_hora)   AS anio,
       EXTRACT(MONTH FROM v.fecha_hora)  AS mes,
       COUNT(*)                          AS servicios,
       SUM(d.subtotal)                   AS importe_servicios,
       SUM(d.monto_comision)             AS comision
  FROM VENTA_DETALLE d
  JOIN VENTA v     ON v.id_venta = d.id_venta AND v.estado = 'CERRADA'
  JOIN PERSONA pe  ON pe.id_persona = d.id_personal
 WHERE d.tipo_item = 'SERVICIO'
 GROUP BY d.id_personal, pe.nombres, pe.apellidos, EXTRACT(YEAR FROM v.fecha_hora), EXTRACT(MONTH FROM v.fecha_hora);

COMMENT ON TABLE V_COMISIONES_MES IS 'Comisión del mes por especialista: solo servicios cobrados de ventas no anuladas (RN38, RF58). Base de PLANILLA_DETALLE.comision.';

CREATE OR REPLACE VIEW V_MARGEN_PRODUCTO AS
SELECT p.id_producto,
       p.codigo,
       p.nombre                                   AS producto,
       p.costo,
       p.precio_venta,
       ROUND(p.precio_venta / (1 + PKG_SESION.parametro_num('IGV_PORCENTAJE') / 100), 2) AS precio_sin_igv,
       NVL(SUM(d.cantidad), 0)                    AS unidades_vendidas,
       NVL(SUM(d.subtotal), 0)                    AS ingreso_con_igv,
       NVL(SUM(d.cantidad), 0) * p.costo          AS costo_vendido,
       ROUND(NVL(SUM(d.subtotal), 0) / (1 + PKG_SESION.parametro_num('IGV_PORCENTAJE') / 100), 2)
           - NVL(SUM(d.cantidad), 0) * p.costo    AS margen_sin_igv,
       (SELECT NVL(SUM(i.stock_actual), 0) FROM INVENTARIO_SEDE i WHERE i.id_producto = p.id_producto) AS stock_actual
  FROM PRODUCTO p
  LEFT JOIN VENTA_DETALLE d ON d.id_producto = p.id_producto
                           AND EXISTS (SELECT 1 FROM VENTA v WHERE v.id_venta = d.id_venta AND v.estado = 'CERRADA')
 GROUP BY p.id_producto, p.codigo, p.nombre, p.costo, p.precio_venta;

COMMENT ON TABLE V_MARGEN_PRODUCTO IS 'Rotación, stock (suma de las sedes) y margen de cada producto (RF69). Solo Administradora (RN24).';

CREATE OR REPLACE VIEW V_COSTO_SERVICIO AS
SELECT vs.id_variante,
       se.nombre                                  AS servicio,
       vs.nombre                                  AS variante,
       ar.linea                                   AS linea,
       vs.precio,
       ROUND(vs.precio / (1 + PKG_SESION.parametro_num('IGV_PORCENTAJE') / 100), 2) AS precio_sin_igv,
       NVL(SUM(si.cantidad_estimada * p.costo), 0) AS costo_insumos,
       ROUND(vs.precio / (1 + PKG_SESION.parametro_num('IGV_PORCENTAJE') / 100), 2)
           - NVL(SUM(si.cantidad_estimada * p.costo), 0) AS margen_sin_igv
  FROM VARIANTE_SERVICIO vs
  JOIN SERVICIO se           ON se.id_servicio = vs.id_servicio
  JOIN AREA ar               ON ar.id_area = se.id_area
  LEFT JOIN SERVICIO_INSUMO si ON si.id_variante = vs.id_variante
  LEFT JOIN PRODUCTO p         ON p.id_producto = si.id_producto
 GROUP BY vs.id_variante, se.nombre, vs.nombre, ar.linea, vs.precio;

COMMENT ON TABLE V_COSTO_SERVICIO IS 'Costo estimado de insumos y margen de cada variante de servicio (RF49, RF70). Solo Administradora (RN24).';

CREATE OR REPLACE VIEW V_PERSONAL_BAJO_RMV AS
SELECT pl.id_personal,
       TRIM(pe.nombres || ' ' || pe.apellidos) AS persona,
       pl.modalidad_pago,
       pl.sueldo_base,
       PKG_SESION.parametro_num('RMV_MONTO') AS rmv
  FROM PERSONAL pl
  JOIN PERSONA pe ON pe.id_persona = pl.id_personal
 WHERE pl.estado = 'ACTIVO'
   AND pl.modalidad_pago <> 'COMISION'
   AND pl.sueldo_base < PKG_SESION.parametro_num('RMV_MONTO');

COMMENT ON TABLE V_PERSONAL_BAJO_RMV IS 'Aviso: personal con sueldo acordado menor a la remuneración mínima vigente. No se bloquea (RN22), solo se señala (RF61).';

CREATE OR REPLACE VIEW V_INCIDENTES_PLAZO AS
SELECT i.id_incidente,
       i.id_sede,
       i.fecha_deteccion,
       i.descripcion,
       i.datos_afectados,
       i.estado,
       i.fecha_aviso,
       i.numero_aviso,
       i.id_usuario,
       i.fecha_deteccion + NUMTODSINTERVAL(PKG_SESION.parametro_num('INCIDENTE_AVISO_HORAS'), 'HOUR') AS vence_aviso,
       CASE WHEN NVL(i.fecha_aviso, CAST(SYSTIMESTAMP AS TIMESTAMP))
                 > i.fecha_deteccion + NUMTODSINTERVAL(PKG_SESION.parametro_num('INCIDENTE_AVISO_HORAS'), 'HOUR')
            THEN 'S' ELSE 'N' END AS fuera_de_plazo
  FROM INCIDENTE_SEGURIDAD i;

COMMENT ON TABLE V_INCIDENTES_PLAZO IS 'Incidentes de seguridad con el vencimiento del aviso a la autoridad (48 h) y si se avisó o sigue fuera de plazo (RN28).';

CREATE OR REPLACE VIEW V_AUDITORIA AS
SELECT a.id_auditoria,
       CAST(a.fecha_hora AS DATE) AS fecha_hora,
       u.nombre_usuario           AS usuario,
       r.nombre                   AS rol,
       a.modulo,
       a.accion,
       a.entidad,
       a.id_entidad,
       a.detalle
  FROM AUDITORIA a
  JOIN USUARIO u ON u.id_usuario = a.id_usuario
  JOIN ROL r     ON r.id_rol = u.id_rol;

COMMENT ON TABLE V_AUDITORIA IS 'Bitácora legible: quién hizo qué, cuándo y sobre qué registro (RF80, RNF24).';

-- =====================================================================
-- [v4] Facturación electrónica, pasarela de pagos y consulta DNI/RUC (21/10/2026)
-- =====================================================================
PROMPT [v4] Vistas de comprobantes electrónicos y pasarela...
CREATE OR REPLACE VIEW V_CPE_PENDIENTES AS
SELECT CASE WHEN c.tipo IN ('NC_BOLETA', 'NC_FACTURA') THEN 'NOTA_CREDITO' ELSE 'COMPROBANTE' END AS documento,
       c.id_comprobante              AS id_documento,
       v.id_venta,
       v.id_sede,
       c.tipo,
       c.serie,
       c.numero,
       c.fecha_emision,
       c.estado_sunat,
       c.intentos_envio,
       CAST(c.fecha_envio AS DATE)   AS ultimo_envio,
       c.descripcion_respuesta,
       c.fecha_emision + PKG_SESION.parametro_num('CPE_PLAZO_ENVIO_DIAS') AS fecha_limite
  FROM COMPROBANTE c
  JOIN VENTA v ON v.id_venta = c.id_venta
 WHERE c.estado_sunat IN ('PENDIENTE', 'ENVIADO', 'OBSERVADO', 'RECHAZADO');

COMMENT ON TABLE V_CPE_PENDIENTES IS '[v5] Una sola consulta sobre el supertipo COMPROBANTE. Comprobantes y notas de crédito aún no aceptados por SUNAT (o aceptados con observaciones o rechazados) con la fecha límite de envío. La pantalla de caja la muestra al cerrar el día (RN10, RN47, RF101).';

CREATE OR REPLACE VIEW V_PASARELA_SIN_USO AS
SELECT t.id_transaccion,
       t.id_sede,
       m.nombre                   AS medio_pago,
       t.canal,
       t.codigo_operacion,
       t.monto,
       CAST(t.fecha_hora AS DATE) AS fecha_hora,
       t.id_cita,
       u.nombre_usuario           AS usuario
  FROM TRANSACCION_PASARELA t
  JOIN MEDIO_PAGO m ON m.id_medio_pago = t.id_medio_pago
  JOIN USUARIO u    ON u.id_usuario = t.id_usuario
 WHERE t.tipo_operacion = 'COBRO'
   AND t.estado = 'APROBADA'
   AND NOT EXISTS (SELECT 1 FROM VENTA_PAGO p WHERE p.id_transaccion = t.id_transaccion)
   AND NOT EXISTS (SELECT 1 FROM CITA c
                    WHERE c.id_cita = t.id_cita AND c.adelanto_monto = t.monto AND c.id_medio_pago = t.id_medio_pago);

COMMENT ON TABLE V_PASARELA_SIN_USO IS 'Cobros aprobados por la pasarela que no respaldan ningún pago de venta ni adelanto de cita: se aplican o se devuelven (RN42, RF75).';

CREATE OR REPLACE VIEW V_DEVOLUCIONES_PENDIENTES AS
SELECT v.id_venta,
       v.id_sede,
       v.id_cliente,
       cnc.serie || '-' || cnc.numero        AS nota_credito,
       m.nombre                              AS medio_pago,
       p.id_transaccion,
       p.monto                               AS monto_pagado,
       NVL((SELECT SUM(d.monto) FROM TRANSACCION_PASARELA d
             WHERE d.id_transaccion_origen = p.id_transaccion
               AND d.tipo_operacion = 'DEVOLUCION' AND d.estado = 'APROBADA'), 0) AS monto_devuelto
  FROM VENTA v
  JOIN VENTA_PAGO p         ON p.id_venta = v.id_venta AND p.id_transaccion IS NOT NULL
  JOIN MEDIO_PAGO m         ON m.id_medio_pago = p.id_medio_pago
  LEFT JOIN COMPROBANTE cnc ON cnc.id_venta = v.id_venta AND cnc.tipo IN ('NC_BOLETA', 'NC_FACTURA')
  LEFT JOIN NOTA_CREDITO nc ON nc.id_nota_credito = cnc.id_comprobante
 WHERE v.estado = 'ANULADA'
   AND NVL(nc.forma_devolucion, 'MEDIO_ORIGINAL') = 'MEDIO_ORIGINAL'
   AND p.monto > NVL((SELECT SUM(d.monto) FROM TRANSACCION_PASARELA d
                       WHERE d.id_transaccion_origen = p.id_transaccion
                         AND d.tipo_operacion = 'DEVOLUCION' AND d.estado = 'APROBADA'), 0);

COMMENT ON TABLE V_DEVOLUCIONES_PENDIENTES IS 'Pagos digitales de ventas anuladas que aún no se devuelven por la pasarela (la clienta no eligió saldo a favor) (RN44, RF106).';

CREATE OR REPLACE VIEW V_PASARELA_LIQUIDACION AS
SELECT EXTRACT(YEAR FROM t.fecha_hora)  AS anio,
       EXTRACT(MONTH FROM t.fecha_hora) AS mes,
       t.id_sede,
       m.nombre                         AS medio_pago,
       SUM(CASE WHEN t.tipo_operacion = 'COBRO' THEN t.monto ELSE 0 END)      AS cobros,
       SUM(CASE WHEN t.tipo_operacion = 'DEVOLUCION' THEN t.monto ELSE 0 END) AS devoluciones,
       SUM(NVL(t.comision, 0))                                                AS comisiones,
       SUM(CASE WHEN t.tipo_operacion = 'COBRO' THEN t.monto ELSE -t.monto END) - SUM(NVL(t.comision, 0)) AS neto_esperado,
       SUM(CASE WHEN t.fecha_abono IS NOT NULL AND t.tipo_operacion = 'COBRO' THEN t.monto - NVL(t.comision, 0) ELSE 0 END) AS neto_abonado,
       COUNT(CASE WHEN t.tipo_operacion = 'COBRO' AND t.fecha_abono IS NULL THEN 1 END)  AS cobros_sin_abono,
       COUNT(CASE WHEN t.comision IS NULL THEN 1 END)                         AS sin_comision_registrada
  FROM TRANSACCION_PASARELA t
  JOIN MEDIO_PAGO m ON m.id_medio_pago = t.id_medio_pago
 WHERE t.estado = 'APROBADA'
 GROUP BY EXTRACT(YEAR FROM t.fecha_hora), EXTRACT(MONTH FROM t.fecha_hora), t.id_sede, m.nombre;

COMMENT ON TABLE V_PASARELA_LIQUIDACION IS 'Resumen mensual de la pasarela por sede y medio: cobros, devoluciones, comisiones, neto esperado y neto abonado. Solo la Administradora (costo de la comisión, RN24, RN31, RF105).';

-- [v5] Reemplaza a V_CONSULTAS_DOC_MES: la bitácora CONSULTA_DOCUMENTO se retiró, y lo que mide el negocio
-- (cuántas fichas se verificaron con APISPERU y cuántas se ingresaron a mano) se calcula de PERSONA.
CREATE OR REPLACE VIEW V_DOCUMENTOS_VERIFICADOS AS
SELECT EXTRACT(YEAR FROM p.fecha_registro)  AS anio,
       EXTRACT(MONTH FROM p.fecha_registro) AS mes,
       NVL(td.abreviatura, 'SIN_DOC')       AS tipo_documento,
       NVL(td.consulta_api, 'N')            AS consulta_api,
       p.tipo_persona,
       p.origen_datos,
       COUNT(*)                             AS personas
  FROM PERSONA p
  LEFT JOIN TIPO_DOCUMENTO td ON td.id_tipo_documento = p.id_tipo_documento
 GROUP BY EXTRACT(YEAR FROM p.fecha_registro), EXTRACT(MONTH FROM p.fecha_registro),
          NVL(td.abreviatura, 'SIN_DOC'), NVL(td.consulta_api, 'N'), p.tipo_persona, p.origen_datos;

COMMENT ON TABLE V_DOCUMENTOS_VERIFICADOS IS 'Personas registradas por mes, tipo de documento y origen de los datos (VERIFICADO con APISPERU o MANUAL): tasa de verificación y uso de la consulta DNI/RUC (RN45, RF99).';
