package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.FormaDevolucion;
import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS22, CUS23, RN32] En la petición solo van motivo, codigoMotivo (01, 02 o 06) y formaDevolucion; el dominio los valida (422). Lo demás
 * lo completan Oracle y el servidor: quién la pidió y cuándo (TRG_SOLICITUD_ANULACION_REGLAS), la resolución y, para la bandeja, el cliente, el
 * total y el comprobante de la venta. [It. 2.1 · nombres, D-31] nombreCliente, nombreUsuarioSolicita y nombreUsuarioResuelve (Duda 14).
 */
public record SolicitudAnulacionDTO(Long idSolicitud, Long idVenta, @Size(max = 200) String motivo, String codigoMotivo, FormaDevolucion formaDevolucion,
		EstadoSolicitudAnulacion estado, Long idUsuarioSolicita, LocalDateTime fechaSolicitud, Long idUsuarioResuelve, LocalDateTime fechaResolucion,
		String motivoRechazo, Long idNotaCredito, Long idCliente, BigDecimal totalVenta, String comprobante, String nombreCliente,
		String nombreUsuarioSolicita, String nombreUsuarioResuelve) {

	public SolicitudAnulacionDTO conNombres(String nombreCliente, String nombreUsuarioSolicita, String nombreUsuarioResuelve) {
		return new SolicitudAnulacionDTO(idSolicitud, idVenta, motivo, codigoMotivo, formaDevolucion, estado, idUsuarioSolicita, fechaSolicitud,
				idUsuarioResuelve, fechaResolucion, motivoRechazo, idNotaCredito, idCliente, totalVenta, comprobante, nombreCliente, nombreUsuarioSolicita,
				nombreUsuarioResuelve);
	}

}
