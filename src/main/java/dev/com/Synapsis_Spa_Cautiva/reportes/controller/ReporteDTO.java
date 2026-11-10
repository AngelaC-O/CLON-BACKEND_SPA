package dev.com.Synapsis_Spa_Cautiva.reportes.controller;

import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.reportes.repository.FilaReporteDTO;

/**
 * [It. 2 · CUS48, CUS49] Un reporte gerencial: qué se pidió (tipo, agrupación, rango y sede; idSede nulo = consolidado, RF93), las columnas que
 * llena ese tipo, en orden, y las filas. El diagrama solo nombra el tipo. La exportación (GeneradorExportacion) usa las mismas columnas.
 */
public record ReporteDTO(String tipoReporte, String titulo, String agrupacion, LocalDate fechaInicio, LocalDate fechaFin, Long idSede,
		List<Columna> columnas, List<FilaReporteDTO> filas) {

	/** campo: el componente de FilaReporteDTO (grupo, cantidad, importe, costo, margen, stock o minutos); titulo: el encabezado. */
	public record Columna(String campo, String titulo) {
	}

}
