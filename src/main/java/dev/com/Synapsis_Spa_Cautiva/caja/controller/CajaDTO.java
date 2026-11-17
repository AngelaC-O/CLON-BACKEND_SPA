package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoCaja;

/**
 * [It. 2 · CUS46] Suma al final la revisión del cierre (RF67) y la reapertura (RF66, RN30), que pone TRG_CAJA_REGLAS: el JSON de la Iteración 1
 * solo crece. [It. 2.1 · nombres] nombreUsuarioApertura, nombreUsuarioCierre, nombreUsuarioRevisa y nombreUsuarioReapertura
 * (UsuarioApi.nombresUsuarios, Duda 25).
 */
public record CajaDTO(Long idCaja, Long idSede, Long idUsuarioApertura, LocalDateTime fechaApertura, BigDecimal montoInicial, Long idUsuarioCierre,
		LocalDateTime fechaCierre, EstadoCaja estado, List<ArqueoCalculadoDTO> arqueos, Long idUsuarioRevisa, LocalDateTime fechaRevision,
		Long idUsuarioReapertura, LocalDateTime fechaReapertura, String motivoReapertura, String nombreUsuarioApertura, String nombreUsuarioCierre,
		String nombreUsuarioRevisa, String nombreUsuarioReapertura) {

	public CajaDTO conNombres(Map<Long, String> usuarios) {
		return new CajaDTO(idCaja, idSede, idUsuarioApertura, fechaApertura, montoInicial, idUsuarioCierre, fechaCierre, estado, arqueos, idUsuarioRevisa,
				fechaRevision, idUsuarioReapertura, fechaReapertura, motivoReapertura, nombre(usuarios, idUsuarioApertura), nombre(usuarios, idUsuarioCierre),
				nombre(usuarios, idUsuarioRevisa), nombre(usuarios, idUsuarioReapertura));
	}

	private static String nombre(Map<Long, String> usuarios, Long idUsuario) {
		return idUsuario == null ? null : usuarios.get(idUsuario);
	}

}
