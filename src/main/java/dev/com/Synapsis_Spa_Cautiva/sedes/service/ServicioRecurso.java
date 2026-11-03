package dev.com.Synapsis_Spa_Cautiva.sedes.service;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.sedes.controller.MapperRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.controller.RecursoDTO;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Recurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.repository.RepositorioRecurso;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioRecurso {

	private final RepositorioRecurso repositorioRecurso;

	private final ParametroApi parametroApi;

	private final MapperRecurso mapperRecurso;

	@Transactional
	public RecursoDTO registrarRecurso(RecursoDTO oRecursoDTO) {
		int limpiezaMaxima = parametroApi.valorNumero("LIMPIEZA_MAX_MIN").intValue();
		if (oRecursoDTO.minutosLimpieza() < 0 || oRecursoDTO.minutosLimpieza() > limpiezaMaxima) {
			throw new ReglaNegocioException("RN03", "El tiempo de limpieza va de 0 a " + limpiezaMaxima + " minutos.");
		}
		Recurso oRecurso = new Recurso(oRecursoDTO.idSede(), oRecursoDTO.nombre(), oRecursoDTO.tipo(), oRecursoDTO.minutosLimpieza());
		return mapperRecurso.aDTO(repositorioRecurso.save(oRecurso));
	}

}
