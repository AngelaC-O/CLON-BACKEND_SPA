package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.pagos.api.CobroPendienteDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoVenta;

/**
 * cobrosPendientes: cobros digitales sin aprobar; no respaldan ningún pago (RN42). En el 202 son los de este pedido; [It. 2.1 · D-22] en
 * GET /ventas/{idVenta}, todos los PENDIENTES de la venta (PagosApi.cobrosPendientes), para retomarla en otro equipo. [It. 2.1 · nombres]
 * nombreCliente; las líneas y los pagos traen sus nombres (NombresVenta).
 */
public record VentaDTO(Long idVenta, Long idSede, Long idCaja, Long idCliente, EstadoVenta estado, List<DetalleVentaDTO> detalles, BigDecimal subtotal,
		BigDecimal igv, BigDecimal descuento, BigDecimal adelantoAplicado, BigDecimal saldoFavorAplicado, BigDecimal total, List<PagoVentaDTO> pagos,
		BigDecimal pagado, BigDecimal pendiente, List<CobroPendienteDTO> cobrosPendientes, ComprobanteDTO comprobante, String nombreCliente) {

	public VentaDTO conNombres(String nombreCliente, List<DetalleVentaDTO> detalles, List<PagoVentaDTO> pagos) {
		return new VentaDTO(idVenta, idSede, idCaja, idCliente, estado, detalles, subtotal, igv, descuento, adelantoAplicado, saldoFavorAplicado, total, pagos,
				pagado, pendiente, cobrosPendientes, comprobante, nombreCliente);
	}

}
