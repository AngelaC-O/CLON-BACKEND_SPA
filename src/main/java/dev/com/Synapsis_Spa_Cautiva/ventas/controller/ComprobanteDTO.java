package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.time.LocalDate;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;

/** Serie, número y fecha los asigna Oracle (RN47). */
public record ComprobanteDTO(Long idComprobante, TipoComprobante tipo, String serie, String numero, LocalDate fechaEmision, EstadoSunat estadoSunat,
		String codigoRespuesta) {
}
