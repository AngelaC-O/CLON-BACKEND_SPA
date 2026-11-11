package dev.com.Synapsis_Spa_Cautiva.reportes.repository;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Una fila de un reporte gerencial (CUS48, CUS49): el grupo (día, mes, línea, servicio, especialista, producto, franja, recurso...) y sus
 * valores. Cada tipo de reporte llena solo los suyos (ReporteDTO.columnas dice cuáles) y los demás quedan nulos y no salen en el JSON:
 * cantidad (ventas, unidades, atenciones o bloques), importe (S/ con IGV), costo y margen (sin IGV, solo para la Administradora, RN24), stock
 * y minutos ocupados. Las ventas ANULADAS no se cuentan (RN33). [It. 2] Antes solo grupo, cantidad e importe (reporte de ventas).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilaReporteDTO(String grupo, Long cantidad, BigDecimal importe, BigDecimal costo, BigDecimal margen, Long stock, Long minutos) {

	/** Reporte de ventas y de ingresos por especialista. */
	public FilaReporteDTO(String grupo, Long cantidad, BigDecimal importe) {
		this(grupo, cantidad, importe, null, null, null, null);
	}

}
