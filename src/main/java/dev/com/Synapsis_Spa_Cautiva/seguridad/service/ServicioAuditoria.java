package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AuditoriaApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.RegistroAuditoria;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Auditoria;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioAuditoria;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ServicioAuditoria implements AuditoriaApi {

	static final String MODULO = "Seguridad y auditoría";

	private final RepositorioAuditoria repositorioAuditoria;

	@Override
	@Transactional
	public void registrar(Long idUsuario, String accion, String entidad, Integer idEntidad, String detalle) {
		repositorioAuditoria.save(new Auditoria(idUsuario, LocalDateTime.now(), MODULO, accion, entidad, idEntidad, detalle));
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
