package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoCaja;

public record CajaDTO(Long idCaja, Long idSede, Long idUsuarioApertura, LocalDateTime fechaApertura, BigDecimal montoInicial, Long idUsuarioCierre,
		LocalDateTime fechaCierre, EstadoCaja estado, List<ArqueoCalculadoDTO> arqueos) {
}
