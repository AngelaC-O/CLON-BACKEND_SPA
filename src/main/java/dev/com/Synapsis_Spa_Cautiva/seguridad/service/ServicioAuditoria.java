package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AuditoriaApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.RegistroAuditoria;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.AuditoriaDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Auditoria;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioAuditoria;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioAuditoria implements AuditoriaApi {

	static final String MODULO = "Seguridad y auditoría";

	/** Sin fechas, los últimos 30 días hasta hoy. */
	private static final long DIAS_POR_DEFECTO = 30;

	private final RepositorioAuditoria repositorioAuditoria;

	@Override
	@Transactional
	public void registrar(Long idUsuario, String accion, String entidad, Integer idEntidad, String detalle) {
		repositorioAuditoria.save(new Auditoria(idUsuario, LocalDateTime.now(), MODULO, accion, entidad, idEntidad, detalle));
	}

	/**
	 * [It. 2 · CUS55, RF80, RN29] Lee V_AUDITORIA con filtros opcionales; nada se inserta: los 56 triggers TRG_AUD_* ya auditan (también
	 * SOLICITUD_ANULACION y CONSUMO_PENDIENTE). Sin fechas, los últimos 30 días; un inicio posterior al fin es 422.
	 */
	@Transactional(readOnly = true)
	public List<AuditoriaDTO> consultar(Long idUsuario, String modulo, String entidad, LocalDate fechaInicio, LocalDate fechaFin) {
		LocalDate fin = fechaFin == null ? LocalDate.now() : fechaFin;
		LocalDate inicio = fechaInicio == null ? fin.minusDays(DIAS_POR_DEFECTO) : fechaInicio;
		if (inicio.isAfter(fin)) {
			throw new ReglaNegocioException("RF80", "La fecha de inicio es posterior a la de fin.");
		}
		return repositorioAuditoria.findByFiltros(idUsuario, vacioANull(modulo), vacioANull(entidad), inicio, fin).stream()
			.map(fila -> new AuditoriaDTO(fila.getIdAuditoria(), fila.getFechaHora(), fila.getUsuario(), fila.getRol(), fila.getModulo(), fila.getAccion(),
					fila.getEntidad(), fila.getIdEntidad(), fila.getDetalle()))
			.toList();
	}

	private static String vacioANull(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

	@Override
	@Transactional(readOnly = true)
	public List<RegistroAuditoria> consultar(LocalDateTime desde, LocalDateTime hasta) {
		return repositorioAuditoria.consultar(desde, hasta).stream()
			.map(fila -> new RegistroAuditoria(fila.getIdAuditoria(), fila.getFechaHora(), fila.getUsuario(), fila.getRol(), fila.getModulo(),
					fila.getAccion(), fila.getEntidad(), fila.getIdEntidad(), fila.getDetalle()))
			.toList();
	}

}
