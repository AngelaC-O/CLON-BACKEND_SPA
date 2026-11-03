package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el comprobante le manda al proveedor. codigoSunatComprador sale de TIPO_DOCUMENTO e igvPorcentaje de PARAMETRO. Los montos de las líneas
 * ya incluyen el IGV (RN06).
 */
public record DocumentoElectronico(String tipo, String serie, String numero, LocalDate fechaEmision, String codigoSunatComprador, String nroDocComprador,
		String nombreComprador, BigDecimal importe, BigDecimal igvPorcentaje, List<Linea> lineas) {

	public record Linea(String descripcion, Integer cantidad, BigDecimal subtotal) {
	}

}
