package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoVenta;

/** cobrosPendientes: cobros digitales sin aprobar de este pedido; no respaldan ningún pago (RN42). */
public record VentaDTO(Long idVenta, Long idSede, Long idCaja, Long idCliente, EstadoVenta estado, List<DetalleVentaDTO> detalles, BigDecimal subtotal,
		BigDecimal igv, BigDecimal descuento, BigDecimal adelantoAplicado, BigDecimal saldoFavorAplicado, BigDecimal total, List<PagoVentaDTO> pagos,
		BigDecimal pagado, BigDecimal pendiente, List<CobroPendienteDTO> cobrosPendientes, ComprobanteDTO comprobante) {
}
