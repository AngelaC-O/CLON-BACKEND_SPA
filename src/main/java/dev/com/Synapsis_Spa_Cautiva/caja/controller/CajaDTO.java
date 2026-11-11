package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoCaja;

/**
 * [It. 2 · CUS46] Suma al final la revisión del cierre (RF67) y la reapertura (RF66, RN30), que pone TRG_CAJA_REGLAS: el JSON de la Iteración 1
 * solo crece.
 */
public record CajaDTO(Long idCaja, Long idSede, Long idUsuarioApertura, LocalDateTime fechaApertura, BigDecimal montoInicial, Long idUsuarioCierre,
		LocalDateTime fechaCierre, EstadoCaja estado, List<ArqueoCalculadoDTO> arqueos, Long idUsuarioRevisa, LocalDateTime fechaRevision,
		Long idUsuarioReapertura, LocalDateTime fechaReapertura, String motivoReapertura) {
}
