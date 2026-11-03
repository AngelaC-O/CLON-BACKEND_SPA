package dev.com.Synapsis_Spa_Cautiva.pagos.service;

import java.math.BigDecimal;

/** idVenta o idCita, según lo que se cobre. canal: POS, QR, LINK o API. */
public record SolicitudCobro(Long idVenta, Long idCita, Long idMedioPago, BigDecimal monto, String canal) {
}
