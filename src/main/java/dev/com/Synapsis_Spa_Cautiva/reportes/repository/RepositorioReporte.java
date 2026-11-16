package dev.com.Synapsis_Spa_Cautiva.reportes.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lee las vistas de reporte del script v5 (CUS48, CUS49). No es un JpaRepository porque el módulo no tiene entidades: cada agrupación es una
 * consulta fija sobre una vista, y la agrupación pedida solo elige cuál (nunca se arma con texto del usuario). Las vistas de ventas ya excluyen
 * las ventas ANULADAS (RN33: V_VENTA_VALIDA, V_VENTA_LINEA y V_PAGOS_POR_MEDIO solo leen VENTA CERRADA); idSede nulo es el consolidado de las
 * sedes (RF93). Las fechas son las dos incluidas. [It. 2] El diagrama solo dibuja ventasAgrupadas; los otros cinco tipos de la nota de CUS48
 * tienen su método (D-20, DF-133).
 */
@Repository
@RequiredArgsConstructor
public class RepositorioReporte {

	private static final String FILTRO = "fecha >= :fechaInicio AND fecha < :fechaFin AND (:idSede IS NULL OR id_sede = :idSede)";

	private static final String POR_VENTA = """
			SELECT %s AS grupo, COUNT(*) AS cantidad, SUM(importe) AS importe
			FROM V_VENTA_VALIDA
			WHERE %s
			GROUP BY %s ORDER BY 1
			""";

	private static final String POR_LINEA = """
			SELECT NVL(%s, '(sin dato)') AS grupo, SUM(cantidad) AS cantidad, SUM(subtotal) AS importe
			FROM V_VENTA_LINEA
			WHERE %s
			GROUP BY NVL(%s, '(sin dato)') ORDER BY 3 DESC, 1
			""";

	private static final String POR_MEDIO_PAGO = """
			SELECT medio_pago AS grupo, COUNT(*) AS cantidad, SUM(monto) AS importe
			FROM V_PAGOS_POR_MEDIO
			WHERE origen = 'VENTA' AND %s
			GROUP BY medio_pago ORDER BY 3 DESC, 1
			""".formatted(FILTRO);

	/** Las agrupaciones del reporte de ventas de RF68. */
	private static final Map<String, String> VENTAS = Map.of(
			"DIA", POR_VENTA.formatted("TO_CHAR(fecha, 'YYYY-MM-DD')", FILTRO, "TO_CHAR(fecha, 'YYYY-MM-DD')"),
			"MES", POR_VENTA.formatted("TO_CHAR(fecha, 'YYYY-MM')", FILTRO, "TO_CHAR(fecha, 'YYYY-MM')"),
			"LINEA", POR_LINEA.formatted("linea", FILTRO, "linea"),
			"SERVICIO", POR_LINEA.formatted("servicio", FILTRO, "servicio"),
			"ESPECIALISTA", POR_LINEA.formatted("especialista", FILTRO, "especialista"),
			"TIPO_ATENCION", POR_LINEA.formatted("tipo_atencion", FILTRO, "tipo_atencion"),
			"MEDIO_PAGO", POR_MEDIO_PAGO);

	/**
	 * RF69: rotación (unidades vendidas en el rango y la sede), stock y margen de cada producto. V_MARGEN_PRODUCTO da el costo, el precio sin IGV
	 * y el stock consolidado, pero no tiene fecha ni sede: las unidades y el ingreso del rango salen de V_VENTA_LINEA, que no tiene id_producto y
	 * se une por el nombre del producto (DF-133). El stock es el de todas las sedes.
	 */
	private static final String PRODUCTOS = """
			SELECT m.codigo || ' - ' || m.producto AS grupo, NVL(SUM(l.cantidad), 0) AS cantidad, NVL(SUM(l.subtotal), 0) AS importe,
				NVL(SUM(l.cantidad), 0) * NVL(m.costo, 0) AS costo,
				ROUND(NVL(SUM(l.subtotal), 0) * m.precio_sin_igv / NULLIF(m.precio_venta, 0), 2) - NVL(SUM(l.cantidad), 0) * NVL(m.costo, 0) AS margen,
				m.stock_actual AS stock
			FROM V_MARGEN_PRODUCTO m
			LEFT JOIN V_VENTA_LINEA l ON l.tipo_item = 'PRODUCTO' AND l.producto = m.producto
				AND l.fecha >= :fechaInicio AND l.fecha < :fechaFin AND (:idSede IS NULL OR l.id_sede = :idSede)
			GROUP BY m.codigo, m.producto, m.costo, m.precio_sin_igv, m.precio_venta, m.stock_actual
			ORDER BY 2 DESC, 1
			""";

	/**
	 * RF70: margen por servicio (variante) o por línea. V_COSTO_SERVICIO da el precio sin IGV y el costo de insumos de cada variante; las
	 * atenciones cobradas del rango y la sede salen de V_VENTA_LINEA (unida por servicio y variante). Margen = ingreso sin IGV - costo.
	 */
	private static final String MARGEN = """
			SELECT %s AS grupo, SUM(x.cantidad) AS cantidad, SUM(x.importe) AS importe, SUM(x.costo) AS costo, SUM(x.margen) AS margen
			FROM (SELECT c.linea AS linea, c.servicio || ' - ' || c.variante AS servicio, COUNT(l.id_detalle) AS cantidad,
					NVL(SUM(l.subtotal), 0) AS importe, COUNT(l.id_detalle) * c.costo_insumos AS costo,
					NVL(ROUND(NVL(SUM(l.subtotal), 0) * c.precio_sin_igv / NULLIF(c.precio, 0), 2), 0) - COUNT(l.id_detalle) * c.costo_insumos AS margen
				FROM V_COSTO_SERVICIO c
				LEFT JOIN V_VENTA_LINEA l ON l.tipo_item = 'SERVICIO' AND l.servicio = c.servicio AND l.variante = c.variante
					AND l.fecha >= :fechaInicio AND l.fecha < :fechaFin AND (:idSede IS NULL OR l.id_sede = :idSede)
				GROUP BY c.linea, c.servicio, c.variante, c.costo_insumos, c.precio_sin_igv, c.precio) x
			GROUP BY %s ORDER BY 5 DESC, 1
			""";

	private static final Map<String, String> MARGENES = Map.of("SERVICIO", MARGEN.formatted("x.servicio", "x.servicio"), "LINEA",
			MARGEN.formatted("x.linea", "x.linea"));

	private static final String DEMANDA = """
			SELECT %s AS grupo, SUM(atenciones) AS cantidad
			FROM V_DEMANDA_SERVICIOS
			WHERE %s
			GROUP BY %s ORDER BY %s
			""";

	private static final String DIA_SEMANA = "DECODE(dia_semana, 1, '1 Lunes', 2, '2 Martes', 3, '3 Miércoles', 4, '4 Jueves', 5, '5 Viernes',"
			+ " 6, '6 Sábado', '7 Domingo')";

	/** RF71: atenciones por día de la semana (días pico), por hora, por día, por servicio o por línea. */
	private static final Map<String, String> DEMANDAS = Map.of(
			"DIA_SEMANA", DEMANDA.formatted(DIA_SEMANA, FILTRO, DIA_SEMANA, "1"),
			"HORA", DEMANDA.formatted(hora("hora"), FILTRO, hora("hora"), "1"),
			"DIA", DEMANDA.formatted("TO_CHAR(fecha, 'YYYY-MM-DD')", FILTRO, "TO_CHAR(fecha, 'YYYY-MM-DD')", "1"),
			"SERVICIO", DEMANDA.formatted("servicio || ' - ' || variante", FILTRO, "servicio || ' - ' || variante", "2 DESC, 1"),
			"LINEA", DEMANDA.formatted("linea", FILTRO, "linea", "2 DESC, 1"));

	private static final String OCUPACION = """
			SELECT %s AS grupo, SUM(bloques) AS cantidad, SUM(minutos_ocupados) AS minutos
			FROM V_OCUPACION_RECURSO
			WHERE %s
			GROUP BY %s ORDER BY %s
			""";

	/** RF72: bloques y minutos ocupados por franja horaria o por recurso. [It. 2.1 · v5.2] V_OCUPACION_RECURSO usa el fin del servicio, sin limpieza (RN03). */
	private static final Map<String, String> OCUPACIONES = Map.of(
			"HORA", OCUPACION.formatted(hora("hora"), FILTRO, hora("hora"), "1"),
			"RECURSO", OCUPACION.formatted("recurso || ' (' || tipo || ')'", FILTRO, "recurso || ' (' || tipo || ')'", "3 DESC, 1"));

	/** RF73: lo que facturó cada especialista por sus servicios cobrados (los productos no comisionan, RN38). */
	private static final String ESPECIALISTAS = """
			SELECT NVL(especialista, '(sin dato)') AS grupo, COUNT(*) AS cantidad, SUM(subtotal) AS importe
			FROM V_VENTA_LINEA
			WHERE tipo_item = 'SERVICIO' AND %s
			GROUP BY id_personal, NVL(especialista, '(sin dato)') ORDER BY 3 DESC, 1
			""".formatted(FILTRO);

	private final NamedParameterJdbcTemplate jdbc;

	/** RF68: ventas agrupadas entre dos fechas, incluidas. */
	public List<FilaReporteDTO> ventasAgrupadas(LocalDate fechaInicio, LocalDate fechaFin, String agrupacion, Long idSede) {
		return jdbc.query(consulta(VENTAS, agrupacion, "RF68"), parametros(fechaInicio, fechaFin, idSede),
				(fila, numero) -> new FilaReporteDTO(fila.getString("grupo"), fila.getLong("cantidad"), fila.getBigDecimal("importe")));
	}

	/** [It. 2] RF69, RN24: rotación, stock y margen de los productos. */
	public List<FilaReporteDTO> productos(LocalDate fechaInicio, LocalDate fechaFin, Long idSede) {
		return jdbc.query(PRODUCTOS, parametros(fechaInicio, fechaFin, idSede),
				(fila, numero) -> new FilaReporteDTO(fila.getString("grupo"), fila.getLong("cantidad"), fila.getBigDecimal("importe"),
						fila.getBigDecimal("costo"), fila.getBigDecimal("margen"), largo(fila, "stock"), null));
	}

	/** [It. 2] RF70, RN24: margen por servicio o por línea. */
	public List<FilaReporteDTO> margenServicios(LocalDate fechaInicio, LocalDate fechaFin, String agrupacion, Long idSede) {
		return jdbc.query(consulta(MARGENES, agrupacion, "RF70"), parametros(fechaInicio, fechaFin, idSede),
				(fila, numero) -> new FilaReporteDTO(fila.getString("grupo"), fila.getLong("cantidad"), fila.getBigDecimal("importe"),
						fila.getBigDecimal("costo"), fila.getBigDecimal("margen"), null, null));
	}

	/** [It. 2] RF71: demanda de atenciones y días pico. */
	public List<FilaReporteDTO> demanda(LocalDate fechaInicio, LocalDate fechaFin, String agrupacion, Long idSede) {
		return jdbc.query(consulta(DEMANDAS, agrupacion, "RF71"), parametros(fechaInicio, fechaFin, idSede),
				(fila, numero) -> new FilaReporteDTO(fila.getString("grupo"), fila.getLong("cantidad"), null, null, null, null, null));
	}

	/** [It. 2] RF72: ocupación de recursos por franja o por recurso. */
	public List<FilaReporteDTO> ocupacion(LocalDate fechaInicio, LocalDate fechaFin, String agrupacion, Long idSede) {
		return jdbc.query(consulta(OCUPACIONES, agrupacion, "RF72"), parametros(fechaInicio, fechaFin, idSede),
				(fila, numero) -> new FilaReporteDTO(fila.getString("grupo"), fila.getLong("cantidad"), null, null, null, null, largo(fila, "minutos")));
	}

	/** [It. 2] RF73: ingresos por especialista. */
	public List<FilaReporteDTO> ingresosEspecialista(LocalDate fechaInicio, LocalDate fechaFin, Long idSede) {
		return jdbc.query(ESPECIALISTAS, parametros(fechaInicio, fechaFin, idSede),
				(fila, numero) -> new FilaReporteDTO(fila.getString("grupo"), fila.getLong("cantidad"), fila.getBigDecimal("importe")));
	}

	private static String consulta(Map<String, String> consultas, String agrupacion, String regla) {
		String consulta = agrupacion == null ? null : consultas.get(agrupacion);
		if (consulta == null) {
			throw new ReglaNegocioException(regla, "Agrupe por " + String.join(", ", consultas.keySet().stream().sorted().toList()) + ".");
		}
		return consulta;
	}

	private static MapSqlParameterSource parametros(LocalDate fechaInicio, LocalDate fechaFin, Long idSede) {
		return new MapSqlParameterSource()
			.addValue("fechaInicio", fechaInicio)
			.addValue("fechaFin", fechaFin.plusDays(1))
			.addValue("idSede", idSede, Types.NUMERIC);
	}

	private static String hora(String columna) {
		return "LPAD(" + columna + ", 2, '0') || ':00'";
	}

	private static Long largo(ResultSet fila, String columna) throws SQLException {
		long valor = fila.getLong(columna);
		return fila.wasNull() ? null : valor;
	}

}
