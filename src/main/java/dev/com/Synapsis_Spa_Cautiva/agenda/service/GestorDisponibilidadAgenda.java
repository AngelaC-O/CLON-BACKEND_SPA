package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.FranjaAgendaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.FilaBloqueAgenda;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosVariante;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.personal.api.EspecialistaHorario;
import dev.com.Synapsis_Spa_Cautiva.personal.api.PersonalApi;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.DatosRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.HorarioSede;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.RecursoApi;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Cruces de especialista y recurso (RN02, RN03, RN34, RN41). [It. 2 · RN03 v5.1] La limpieza solo ocupa al recurso: la especialista queda libre
 * al terminar el servicio (o desde hora_fin, si terminó antes de lo declarado). Oracle repite la validación (TRG_CITA_CRUCE,
 * TRG_ATENCION_CRUCE) y es quien resuelve dos pedidos simultáneos; PKG_AGENDA aún ocupa a la especialista durante la limpieza (DF-58, D-14).
 */
@Service
@RequiredArgsConstructor
public class GestorDisponibilidadAgenda {

	private static final String OCUPADA = "OCUPADA";

	private static final String LIBRE = "LIBRE";

	private static final String AGENDA_PASO_MIN = "AGENDA_PASO_MIN";

	private final RepositorioCita repositorioCita;

	private final RepositorioAtencion repositorioAtencion;

	private final CatalogoApi catalogoApi;

	private final RecursoApi recursoApi;

	private final PersonalApi personalApi;

	private final ParametroApi parametroApi;

	/** [It. 2 · RN51] Desde la apertura de la sede, cada AGENDA_PASO_MIN minutos de PARAMETRO (antes, la duración de referencia: D-08). */
	public List<HoraLibre> calcularHorasLibres(Long idVariante, Long idSede, LocalDate fecha) {
		DatosVariante oVariante = catalogoApi.obtenerVariante(idVariante);
		HorarioSede oHorario = recursoApi.obtenerHorarioSede(idSede);
		List<EspecialistaHorario> especialistas = personalApi.listarEspecialistas(idSede, oVariante.idArea(), fecha);
		List<DatosRecurso> recursos = recursosDelTipo(idSede, oVariante);
		List<FilaBloqueAgenda> bloques = bloquesDelDia(idSede, fecha);
		int duracion = oVariante.duracionRefMin();
		int pasoMin = parametroApi.obtenerValorNumero(AGENDA_PASO_MIN).intValue();
		if (pasoMin <= 0) {
			throw new IllegalStateException("AGENDA_PASO_MIN debe ser mayor que 0");
		}
		LocalDateTime ahora = LocalDateTime.now();
		LocalDateTime cierre = fecha.atTime(oHorario.cierre());
		List<HoraLibre> horasLibres = new ArrayList<>();
		for (LocalDateTime inicio = fecha.atTime(oHorario.apertura()); !inicio.plusMinutes(duracion).isAfter(cierre); inicio = inicio.plusMinutes(pasoMin)) {
			if (!inicio.isBefore(ahora)) {
				evaluarHora(inicio, duracion, especialistas, recursos, bloques).ifPresent(horasLibres::add);
			}
		}
		return horasLibres;
	}

	public boolean validarSinCruce(Long idPersonal, Long idRecurso, LocalDateTime fechaHoraInicio, Integer duracionMin) {
		return validarSinCruce(idPersonal, idRecurso, fechaHoraInicio, duracionMin, null);
	}

	/**
	 * CUS11: la cita que se está iniciando no choca consigo misma (DF-30). Si hay cruce, responde 409 e indica el conflicto (RF08).
	 * [It. 2 · RN03] La especialista se compara sin limpieza y el recurso con ella.
	 */
	public boolean validarSinCruce(Long idPersonal, Long idRecurso, LocalDateTime horaInicio, Integer tiempoDeclaradoMin, Long idCitaPropia) {
		if (personalApi.obtenerIdSede(idPersonal).isEmpty()) {
			throw new ReglaNegocioException("RF07", "La especialista " + idPersonal + " no existe.");
		}
		DatosRecurso oRecurso = recursoApi.obtenerRecurso(idRecurso);
		LocalDateTime finServicio = horaInicio.plusMinutes(tiempoDeclaradoMin);
		LocalDateTime finRecurso = finServicio.plusMinutes(oRecurso.minutosLimpieza());
		for (Cita oCita : repositorioCita.findOcupacion(oRecurso.idSede(), horaInicio.toLocalDate())) {
			if (!oCita.getIdCita().equals(idCitaPropia)) {
				LocalDateTime finServicioCita = oCita.getFechaHoraInicio().plusMinutes(oCita.getDuracionMin());
				exigirLibre(idPersonal.equals(oCita.getIdPersonal()) && seCruzan(horaInicio, finServicio, oCita.getFechaHoraInicio(), finServicioCita),
						idRecurso.equals(oCita.getIdRecurso()) && seCruzan(horaInicio, finRecurso, oCita.getFechaHoraInicio(), oCita.getFechaHoraFin()),
						"RN02", "la cita N° " + oCita.getIdCita());
			}
		}
		for (Atencion oAtencion : repositorioAtencion.findOcupacionSinCita(oRecurso.idSede(), horaInicio.toLocalDate())) {
			LocalDateTime finServicioAtencion = finServicio(oAtencion);
			LocalDateTime finRecursoAtencion = finServicioAtencion.plusMinutes(recursoApi.obtenerRecurso(oAtencion.getIdRecurso()).minutosLimpieza());
			exigirLibre(idPersonal.equals(oAtencion.getIdPersonal()) && seCruzan(horaInicio, finServicio, oAtencion.getHoraInicio(), finServicioAtencion),
					idRecurso.equals(oAtencion.getIdRecurso()) && seCruzan(horaInicio, finRecurso, oAtencion.getHoraInicio(), finRecursoAtencion),
					"RN34", "la atención N° " + oAtencion.getIdAtencion());
		}
		return true;
	}

	/** [It. 2 · RN03] Las franjas libres de la especialista empiezan al terminar el servicio; las del recurso, después de su limpieza. */
	public List<FranjaAgendaDTO> armarAgenda(Long idSede, LocalDate fecha, Long idPersonal, Long idRecurso) {
		List<FilaBloqueAgenda> bloques = bloquesDelDia(idSede, fecha);
		List<FranjaAgendaDTO> agenda = new ArrayList<>();
		bloques.stream()
			.filter(oBloque -> (idPersonal == null || idPersonal.equals(oBloque.getIdPersonal()))
					&& (idRecurso == null || idRecurso.equals(oBloque.getIdRecurso())))
			.map(oBloque -> new FranjaAgendaDTO(OCUPADA, oBloque.getOrigen(), oBloque.getIdOrigen(), oBloque.getIdPersonal(), oBloque.getIdRecurso(),
					oBloque.getIdVariante(), oBloque.getInicio(), oBloque.getFin(), oBloque.getEstado(), oBloque.getFinServicio(), oBloque.getIdCliente(),
					oBloque.getNombreClientePaso(), null, null))
			.forEach(agenda::add);
		if (idRecurso == null) {
			agregarFranjasDeEspecialistas(agenda, idSede, fecha, idPersonal, bloques);
		}
		if (idPersonal == null) {
			agregarFranjasDeRecursos(agenda, idSede, fecha, idRecurso, bloques);
		}
		agenda.sort(Comparator.comparing(FranjaAgendaDTO::inicio));
		return agenda;
	}

	public List<HoraLibre> especialistasYRecursosLibres(Long idVariante, Long idSede, LocalDateTime fechaHora) {
		DatosVariante oVariante = catalogoApi.obtenerVariante(idVariante);
		LocalDate fecha = fechaHora.toLocalDate();
		return evaluarHora(fechaHora, oVariante.duracionRefMin(), personalApi.listarEspecialistas(idSede, oVariante.idArea(), fecha),
				recursosDelTipo(idSede, oVariante), bloquesDelDia(idSede, fecha)).stream().toList();
	}

	private List<DatosRecurso> recursosDelTipo(Long idSede, DatosVariante oVariante) {
		return recursoApi.listarDisponibles(idSede).stream().filter(oRecurso -> oVariante.tipoRecurso().equals(oRecurso.tipo())).toList();
	}

	private List<FilaBloqueAgenda> bloquesDelDia(Long idSede, LocalDate fecha) {
		return repositorioCita.findBloques(idSede, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
	}

	/** Vacío si falta especialista o recurso. [It. 2 · RN03] El recurso se cuenta con su limpieza; la especialista, solo con el servicio. */
	private Optional<HoraLibre> evaluarHora(LocalDateTime inicio, int duracion, List<EspecialistaHorario> especialistas, List<DatosRecurso> recursos,
			List<FilaBloqueAgenda> bloques) {
		LocalDateTime finServicio = inicio.plusMinutes(duracion);
		List<DatosRecurso> recursosLibres = recursos.stream()
			.filter(oRecurso -> bloques.stream()
				.noneMatch(oBloque -> oRecurso.idRecurso().equals(oBloque.getIdRecurso())
						&& seCruzan(inicio, finServicio.plusMinutes(oRecurso.minutosLimpieza()), oBloque.getInicio(), oBloque.getFin())))
			.toList();
		if (recursosLibres.isEmpty()) {
			return Optional.empty();
		}
		List<Long> idsPersonal = especialistas.stream()
			.filter(oEspecialista -> !inicio.toLocalTime().isBefore(oEspecialista.horaEntrada())
					&& !finServicio.toLocalTime().isAfter(oEspecialista.horaSalida()))
			.filter(oEspecialista -> bloques.stream()
				.noneMatch(oBloque -> oEspecialista.idPersonal().equals(oBloque.getIdPersonal())
						&& seCruzan(inicio, finServicio, oBloque.getInicio(), oBloque.getFinServicio())))
			.map(EspecialistaHorario::idPersonal)
			.toList();
		if (idsPersonal.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new HoraLibre(inicio, duracion, idsPersonal, recursosLibres.stream().map(DatosRecurso::idRecurso).toList()));
	}

	private void agregarFranjasDeEspecialistas(List<FranjaAgendaDTO> agenda, Long idSede, LocalDate fecha, Long idPersonal,
			List<FilaBloqueAgenda> bloques) {
		for (EspecialistaHorario oEspecialista : personalApi.listarEspecialistas(idSede, null, fecha)) {
			if (idPersonal == null || idPersonal.equals(oEspecialista.idPersonal())) {
				List<FilaBloqueAgenda> ocupadas = bloques.stream().filter(oBloque -> oEspecialista.idPersonal().equals(oBloque.getIdPersonal())).toList();
				agregarFranjasLibres(agenda, fecha.atTime(oEspecialista.horaEntrada()), fecha.atTime(oEspecialista.horaSalida()), ocupadas,
						FilaBloqueAgenda::getFinServicio, oEspecialista.idPersonal(), null);
			}
		}
	}

	private void agregarFranjasDeRecursos(List<FranjaAgendaDTO> agenda, Long idSede, LocalDate fecha, Long idRecurso, List<FilaBloqueAgenda> bloques) {
		HorarioSede oHorario = recursoApi.obtenerHorarioSede(idSede);
		for (DatosRecurso oRecurso : recursoApi.listarDisponibles(idSede)) {
			if (idRecurso == null || idRecurso.equals(oRecurso.idRecurso())) {
				List<FilaBloqueAgenda> ocupadas = bloques.stream().filter(oBloque -> oRecurso.idRecurso().equals(oBloque.getIdRecurso())).toList();
				agregarFranjasLibres(agenda, fecha.atTime(oHorario.apertura()), fecha.atTime(oHorario.cierre()), ocupadas, FilaBloqueAgenda::getFin, null,
						oRecurso.idRecurso());
			}
		}
	}

	/** ocupadas viene ordenada por inicio, como la devuelve findBloques; finDe dice hasta cuándo ocupa cada bloque. */
	private void agregarFranjasLibres(List<FranjaAgendaDTO> agenda, LocalDateTime desde, LocalDateTime hasta, List<FilaBloqueAgenda> ocupadas,
			Function<FilaBloqueAgenda, LocalDateTime> finDe, Long idPersonal, Long idRecurso) {
		LocalDateTime cursor = desde;
		for (FilaBloqueAgenda oBloque : ocupadas) {
			if (oBloque.getInicio().isAfter(cursor) && cursor.isBefore(hasta)) {
				agenda.add(franjaLibre(cursor, oBloque.getInicio().isBefore(hasta) ? oBloque.getInicio() : hasta, idPersonal, idRecurso));
			}
			LocalDateTime fin = finDe.apply(oBloque);
			if (fin.isAfter(cursor)) {
				cursor = fin;
			}
		}
		if (cursor.isBefore(hasta)) {
			agenda.add(franjaLibre(cursor, hasta, idPersonal, idRecurso));
		}
	}

	private FranjaAgendaDTO franjaLibre(LocalDateTime inicio, LocalDateTime fin, Long idPersonal, Long idRecurso) {
		return new FranjaAgendaDTO(LIBRE, null, null, idPersonal, idRecurso, null, inicio, fin, null, null, null, null, null, null);
	}

	/** Sin limpieza: hasta cuándo la atención ocupa a la especialista (RN03, v5.1). */
	private static LocalDateTime finServicio(Atencion oAtencion) {
		return oAtencion.getHoraFin() != null ? oAtencion.getHoraFin() : oAtencion.getHoraInicio().plusMinutes(oAtencion.getTiempoDeclaradoMin());
	}

	private static void exigirLibre(boolean especialistaOcupada, boolean recursoOcupado, String regla, String ocupacion) {
		if (especialistaOcupada) {
			throw new ReglaNegocioException(regla, "Cruce de horario: la especialista ya tiene " + ocupacion + ".", HttpStatus.CONFLICT);
		}
		if (recursoOcupado) {
			throw new ReglaNegocioException(regla, "Cruce de horario: el recurso ya tiene " + ocupacion + " (incluye su limpieza).", HttpStatus.CONFLICT);
		}
	}

	/** Mismo criterio que PKG_AGENDA.validar_cruce: el bloque que termina justo cuando el otro empieza no choca. */
	private static boolean seCruzan(LocalDateTime inicio, LocalDateTime fin, LocalDateTime otroInicio, LocalDateTime otroFin) {
		return otroInicio.isBefore(fin) && otroFin.isAfter(inicio);
	}

}
