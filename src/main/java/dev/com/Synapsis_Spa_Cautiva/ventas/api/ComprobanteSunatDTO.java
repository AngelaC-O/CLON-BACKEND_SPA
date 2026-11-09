package dev.com.Synapsis_Spa_Cautiva.ventas.api;

import java.math.BigDecimal;

/**
 * Boleta o factura del mes con su estado en SUNAT y su nota de crédito, si la tiene (CUS47, RF75, RN31). estadoVenta y
 * estadoSunat van como texto porque los enums del dominio son internos. Los campos salen de V_COMPROBANTES_MES (DF-71).
 */
public record ComprobanteSunatDTO(Long idVenta, String tipo, String serie, String numero, BigDecimal importe, String estadoVenta,
		String estadoSunat, String ncSerie, String ncNumero, String ncEstadoSunat) {

	private static final String ACEPTADO = "ACEPTADO";

	public boolean aceptado() {
		return ACEPTADO.equals(estadoSunat);
	}

	public String referencia() {
		return serie + "-" + numero;
	}

}
