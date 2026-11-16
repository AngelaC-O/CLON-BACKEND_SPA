package dev.com.Synapsis_Spa_Cautiva.pagos.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [It. 2.1 · D-22, RN52] Un cobro digital de la venta que la pasarela aún no aprueba (PENDIENTE). enlacePago: la URL que la clienta paga o
 * escanea como QR; vacía si la pasarela no respondió. canal y estado son texto porque los enums del dominio son internos. Antes estaba en
 * ventas.controller con solo idTransaccion y enlacePago: ahora lo arma PagosApi.cobrosPendientes (DF-161).
 */
public record CobroPendienteDTO(Long idTransaccion, Long idMedioPago, BigDecimal monto, String canal, String estado, String enlacePago,
		LocalDateTime fechaExpiracion) {
}
