package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el comprobante le manda al proveedor. codigoSunatComprador sale de TIPO_DOCUMENTO e igvPorcentaje de PARAMETRO. Los montos de las líneas
 * ya incluyen el IGV (RN06). [It. 2 · CUS23] notaCredito: solo en una NC_BOLETA o NC_FACTURA, con el comprobante que anula y el motivo.
 */
public record DocumentoElectronico(String tipo, String serie, String numero, LocalDate fechaEmision, String codigoSunatComprador, String nroDocComprador,
		String nombreComprador, BigDecimal importe, BigDecimal igvPorcentaje, List<Linea> lineas, NotaAfectada notaCredito) {

	/** Boleta o factura (CUS19). */
	public DocumentoElectronico(String tipo, String serie, String numero, LocalDate fechaEmision, String codigoSunatComprador, String nroDocComprador,
			String nombreComprador, BigDecimal importe, BigDecimal igvPorcentaje, List<Linea> lineas) {
		this(tipo, serie, numero, fechaEmision, codigoSunatComprador, nroDocComprador, nombreComprador, importe, igvPorcentaje, lineas, null);
	}

	public record Linea(String descripcion, Integer cantidad, BigDecimal subtotal) {
	}

	/** tipoAfectado: BOLETA o FACTURA; codigoMotivo: catálogo 09 de SUNAT (01, 02 o 06). */
	public record NotaAfectada(String tipoAfectado, String serieAfectada, String numeroAfectado, String codigoMotivo, String motivo) {
	}

}
