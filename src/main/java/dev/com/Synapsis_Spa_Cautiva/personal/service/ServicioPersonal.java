package dev.com.Synapsis_Spa_Cautiva.personal.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import dev.com.Synapsis_Spa_Cautiva.personal.api.EspecialistaHorario;
import dev.com.Synapsis_Spa_Cautiva.personal.api.PersonalApi;
import dev.com.Synapsis_Spa_Cautiva.personal.domain.CargoPersonal;
import dev.com.Synapsis_Spa_Cautiva.personal.domain.EstadoPersonal;
import dev.com.Synapsis_Spa_Cautiva.personal.domain.Personal;
import dev.com.Synapsis_Spa_Cautiva.personal.repository.RepositorioPersonal;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ServicioPersonal implements PersonalApi {

	/** Los mismos cargos que TRG_CITA_REGLAS y TRG_ATENCION_REGLAS dejan atender. */
	private static final Set<CargoPersonal> CARGOS_QUE_ATIENDEN = Set.of(CargoPersonal.ESPECIALISTA_TECNICA, CargoPersonal.ADMINISTRADORA);

	private final RepositorioPersonal repositorioPersonal;

	@Override
	@Transactional(readOnly = true)
	public Optional<Long> obtenerIdSede(Long idPersonal) {
		return repositorioPersonal.findById(idPersonal).map(Personal::getIdSede);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EspecialistaHorario> listarEspecialistas(Long idSede, Long idArea, LocalDate fecha) {
		return repositorioPersonal.findByIdSedeAndEstadoAndCargoIn(idSede, EstadoPersonal.ACTIVO, CARGOS_QUE_ATIENDEN).stream()
			.filter(oPersonal -> oPersonal.getAreas().stream().anyMatch(area -> idArea == null || idArea.equals(area.getIdArea())))
			.flatMap(oPersonal -> horarioDelDia(oPersonal, fecha))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<EspecialistaHorario> listarEspecialistas(Long idSede) {
		return repositorioPersonal.findByIdSedeAndEstadoAndCargoIn(idSede, EstadoPersonal.ACTIVO, CARGOS_QUE_ATIENDEN).stream()
			.map(oPersonal -> new EspecialistaHorario(oPersonal.getIdPersonal(), null, null, oPersonal.getCargo().name()))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<EspecialistaHorario> listarPersonal() {
		return repositorioPersonal.findAll().stream()
			.filter(oPersonal -> oPersonal.getEstado() == EstadoPersonal.ACTIVO)
			.map(oPersonal -> new EspecialistaHorario(oPersonal.getIdPersonal(), null, null, oPersonal.getCargo().name()))
			.toList();
	}

	/** dia_semana: 1 = lunes, igual que DayOfWeek. */
	private Stream<EspecialistaHorario> horarioDelDia(Personal oPersonal, LocalDate fecha) {
		return oPersonal.getHorarios().stream()
			.filter(horario -> horario.getDiaSemana() == fecha.getDayOfWeek().getValue())
			.map(horario -> new EspecialistaHorario(oPersonal.getIdPersonal(), LocalTime.parse(horario.getHoraEntrada()),
					LocalTime.parse(horario.getHoraSalida()), oPersonal.getCargo().name()));
	}

}
