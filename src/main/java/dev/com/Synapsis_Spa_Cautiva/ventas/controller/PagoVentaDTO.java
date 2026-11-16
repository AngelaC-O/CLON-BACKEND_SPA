package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;

/**
 * [It. 2] estadoTransaccion: el estado actual del cobro digital en la pasarela; solo lo llena GET /ventas/{idVenta} (CUS18, RN52).
 * [It. 2.1 · nombres] nombreMedioPago (PagosApi.nombresMediosPago).
 */
public record PagoVentaDTO(Long idPago, Long idMedioPago, BigDecimal monto, Long idTransaccion, String referencia, String estadoTransaccion,
		String nombreMedioPago) {

	public PagoVentaDTO conEstadoTransaccion(String estadoTransaccion) {
		return new PagoVentaDTO(idPago, idMedioPago, monto, idTransaccion, referencia, estadoTransaccion, nombreMedioPago);
	}

	public PagoVentaDTO conNombreMedioPago(String nombreMedioPago) {
		return new PagoVentaDTO(idPago, idMedioPago, monto, idTransaccion, referencia, estadoTransaccion, nombreMedioPago);
	}

}
