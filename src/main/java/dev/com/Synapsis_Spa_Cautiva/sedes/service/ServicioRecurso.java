package dev.com.Synapsis_Spa_Cautiva.sedes.service;

import java.time.LocalTime;
import java.util.List;

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

	private final MapperRecurso mapperRecurso;

	/** CUS61 (RF85, RN26). [It. 2.1 · v5.2] Sin tiempo de limpieza: ya no lee LIMPIEZA_MAX_MIN (RN03). */
	@Transactional
	public RecursoDTO registrarRecurso(RecursoDTO oRecursoDTO) {
		Recurso oRecurso = new Recurso(oRecursoDTO.idSede(), oRecursoDTO.nombre(), oRecursoDTO.tipo());
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

	/** [It. 2] CUS61 (con GET /recursos) y CUS08: un solo método, porque ServicioRecurso también implementa RecursoApi (DF-78). */
	@Override
	@Transactional(readOnly = true)
	public List<DatosRecurso> listarRecursos(Long idSede) {
		return repositorioRecurso.findBySede(idSede).stream().map(this::aDatos).toList();
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
				oRecurso.getEstado().name());
	}

}
