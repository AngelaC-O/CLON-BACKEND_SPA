package dev.com.Synapsis_Spa_Cautiva.pagos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** idVenta o idCita, según lo que se cobre. canal: POS, QR, LINK o API. Nunca lleva datos de tarjeta (RN43). */
public record SolicitudCobro(Long idVenta, Long idCita, Long idMedioPago, BigDecimal monto, String canal, LocalDateTime fechaExpiracion) {
}
