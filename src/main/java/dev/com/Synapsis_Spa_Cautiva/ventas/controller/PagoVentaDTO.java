package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;

/** [It. 2] estadoTransaccion: el estado actual del cobro digital en la pasarela; solo lo llena GET /ventas/{idVenta} (CUS18, RN52). */
public record PagoVentaDTO(Long idPago, Long idMedioPago, BigDecimal monto, Long idTransaccion, String referencia, String estadoTransaccion) {

	public PagoVentaDTO conEstadoTransaccion(String estadoTransaccion) {
		return new PagoVentaDTO(idPago, idMedioPago, monto, idTransaccion, referencia, estadoTransaccion);
	}

}
