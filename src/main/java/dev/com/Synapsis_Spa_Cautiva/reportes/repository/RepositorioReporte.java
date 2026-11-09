package dev.com.Synapsis_Spa_Cautiva.reportes.repository;

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
 * Lee las vistas de reporte del script v5 (CUS48, CUS49). No es un JpaRepository porque el módulo no tiene entidades:
 * cada agrupación es una consulta fija sobre una vista, y la agrupación pedida solo elige cuál (nunca se arma con texto
 * del usuario). Las vistas ya excluyen las ventas ANULADAS (RN33); idSede nulo es el consolidado de las sedes (RF93).
 */
@Repository
@RequiredArgsConstructor
public class RepositorioReporte {

	private static final String POR_VENTA = """
			SELECT %s AS grupo, COUNT(*) AS cantidad, SUM(importe) AS importe
			FROM V_VENTA_VALIDA
			WHERE fecha >= :fechaInicio AND fecha < :fechaFin AND (:idSede IS NULL OR id_sede = :idSede)
			GROUP BY %s ORDER BY 1
			""";

	private static final String POR_LINEA = """
			SELECT NVL(%s, '(sin dato)') AS grupo, SUM(cantidad) AS cantidad, SUM(subtotal) AS importe
			FROM V_VENTA_LINEA
			WHERE fecha >= :fechaInicio AND fecha < :fechaFin AND (:idSede IS NULL OR id_sede = :idSede)
			GROUP BY NVL(%s, '(sin dato)') ORDER BY 3 DESC, 1
			""";

	private static final String POR_MEDIO_PAGO = """
			SELECT medio_pago AS grupo, COUNT(*) AS cantidad, SUM(monto) AS importe
			FROM V_PAGOS_POR_MEDIO
			WHERE origen = 'VENTA' AND fecha >= :fechaInicio AND fecha < :fechaFin AND (:idSede IS NULL OR id_sede = :idSede)
			GROUP BY medio_pago ORDER BY 3 DESC, 1
			""";

	/** Las agrupaciones del reporte de ventas de RF68. */
	private static final Map<String, String> CONSULTAS = Map.of(
			"DIA", POR_VENTA.formatted("TO_CHAR(fecha, 'YYYY-MM-DD')", "TO_CHAR(fecha, 'YYYY-MM-DD')"),
			"MES", POR_VENTA.formatted("TO_CHAR(fecha, 'YYYY-MM')", "TO_CHAR(fecha, 'YYYY-MM')"),
			"LINEA", POR_LINEA.formatted("linea", "linea"),
			"SERVICIO", POR_LINEA.formatted("servicio", "servicio"),
			"ESPECIALISTA", POR_LINEA.formatted("especialista", "especialista"),
			"TIPO_ATENCION", POR_LINEA.formatted("tipo_atencion", "tipo_atencion"),
			"MEDIO_PAGO", POR_MEDIO_PAGO);

	private final NamedParameterJdbcTemplate jdbc;

	/** RF68: ventas agrupadas entre dos fechas, incluidas. */
	public List<FilaReporteDTO> ventasAgrupadas(LocalDate fechaInicio, LocalDate fechaFin, String agrupacion, Long idSede) {
		String consulta = agrupacion == null ? null : CONSULTAS.get(agrupacion);
		if (consulta == null) {
			throw new ReglaNegocioException("RF68", "Agrupe por " + String.join(", ", CONSULTAS.keySet().stream().sorted().toList()) + ".");
		}
		MapSqlParameterSource parametros = new MapSqlParameterSource()
			.addValue("fechaInicio", fechaInicio)
			.addValue("fechaFin", fechaFin.plusDays(1))
			.addValue("idSede", idSede, Types.NUMERIC);
		return jdbc.query(consulta, parametros,
				(fila, numero) -> new FilaReporteDTO(fila.getString("grupo"), fila.getLong("cantidad"), fila.getBigDecimal("importe")));
	}

}
