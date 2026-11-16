package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.pagos.api.CobroPendienteDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoVenta;

/**
 * cobrosPendientes: cobros digitales sin aprobar; no respaldan ningún pago (RN42). En el 202 son los de este pedido; [It. 2.1 · D-22] en
 * GET /ventas/{idVenta}, todos los PENDIENTES de la venta (PagosApi.cobrosPendientes), para retomarla en otro equipo.
 */
public record VentaDTO(Long idVenta, Long idSede, Long idCaja, Long idCliente, EstadoVenta estado, List<DetalleVentaDTO> detalles, BigDecimal subtotal,
		BigDecimal igv, BigDecimal descuento, BigDecimal adelantoAplicado, BigDecimal saldoFavorAplicado, BigDecimal total, List<PagoVentaDTO> pagos,
		BigDecimal pagado, BigDecimal pendiente, List<CobroPendienteDTO> cobrosPendientes, ComprobanteDTO comprobante) {
}
