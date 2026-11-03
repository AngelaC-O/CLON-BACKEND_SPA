package dev.com.Synapsis_Spa_Cautiva.sedes.service;

import java.time.LocalTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.DatosRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.HorarioSede;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.RecursoApi;
import dev.com.Synapsis_Spa_Cautiva.sedes.controller.MapperRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.controller.RecursoDTO;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.EstadoRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Recurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Sede;
import dev.com.Synapsis_Spa_Cautiva.sedes.repository.RepositorioRecurso;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioRecurso implements RecursoApi {

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

	@Override
	@Transactional(readOnly = true)
	public DatosRecurso obtenerRecurso(Long idRecurso) {
		return repositorioRecurso.findById(idRecurso)
			.map(this::aDatos)
			.orElseThrow(() -> new ReglaNegocioException("RF85", "El recurso " + idRecurso + " no existe."));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DatosRecurso> listarDisponibles(Long idSede) {
		return repositorioRecurso.findByIdSedeAndEstado(idSede, EstadoRecurso.DISPONIBLE).stream().map(this::aDatos).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public HorarioSede obtenerHorarioSede(Long idSede) {
		Sede oSede = repositorioRecurso.findSede(idSede)
			.orElseThrow(() -> new ReglaNegocioException("RN26", "La sede " + idSede + " no existe."));
		return new HorarioSede(LocalTime.parse(oSede.getHorarioApertura()), LocalTime.parse(oSede.getHorarioCierre()));
	}

	private DatosRecurso aDatos(Recurso oRecurso) {
		return new DatosRecurso(oRecurso.getIdRecurso(), oRecurso.getIdSede(), oRecurso.getNombre(), oRecurso.getTipo().name(),
				oRecurso.getMinutosLimpieza());
	}

}
