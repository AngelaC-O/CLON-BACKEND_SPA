package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.FormaDevolucion;
import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS22, CUS23, RN32] En la petición solo van motivo, codigoMotivo (01, 02 o 06) y formaDevolucion; el dominio los valida (422). Lo demás
 * lo completan Oracle y el servidor: quién la pidió y cuándo (TRG_SOLICITUD_ANULACION_REGLAS), la resolución y, para la bandeja, el cliente, el
 * total y el comprobante de la venta.
 */
public record SolicitudAnulacionDTO(Long idSolicitud, Long idVenta, @Size(max = 200) String motivo, String codigoMotivo, FormaDevolucion formaDevolucion,
		EstadoSolicitudAnulacion estado, Long idUsuarioSolicita, LocalDateTime fechaSolicitud, Long idUsuarioResuelve, LocalDateTime fechaResolucion,
		String motivoRechazo, Long idNotaCredito, Long idCliente, BigDecimal totalVenta, String comprobante) {
}
