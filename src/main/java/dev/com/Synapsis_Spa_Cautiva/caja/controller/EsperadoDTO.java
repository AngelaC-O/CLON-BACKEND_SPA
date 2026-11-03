package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoCaja;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobantePendiente;

/** comprobantesPendientes: el aviso del cierre (RN46). */
public record EsperadoDTO(Long idCaja, EstadoCaja estado, BigDecimal montoInicial, List<EsperadoMedioDTO> medios,
		List<ComprobantePendiente> comprobantesPendientes) {
}
