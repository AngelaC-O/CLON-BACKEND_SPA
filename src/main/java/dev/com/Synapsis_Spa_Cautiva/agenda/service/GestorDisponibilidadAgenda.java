package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.BloqueAgendaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.FilaBloqueAgenda;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosVariante;
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
 * Cruces de especialista y recurso con la limpieza del recurso (RN02, RN03, RN34, RN41). Oracle repite la validación
 * (TRG_CITA_CRUCE, TRG_ATENCION_CRUCE) y es quien resuelve dos pedidos simultáneos.
 */
@Service
@RequiredArgsConstructor
public class GestorDisponibilidadAgenda {

	private static final String OCUPADA = "OCUPADA";

	private static final String LIBRE = "LIBRE";

	private final RepositorioCita repositorioCita;

	private final RepositorioAtencion repositorioAtencion;

	private final CatalogoApi catalogoApi;

	private final RecursoApi recursoApi;

	private final PersonalApi personalApi;

	/** Avanza desde la apertura de la sede de duración de referencia en duración de referencia (D-08). */
	public List<HoraLibre> calcularHorasLibres(Long idVariante, Long idSede, LocalDate fecha) {
		DatosVariante oVariante = catalogoApi.obtenerVariante(idVariante);
		HorarioSede oHorario = recursoApi.obtenerHorarioSede(idSede);
		List<EspecialistaHorario> especialistas = personalApi.listarEspecialistas(idSede, oVariante.idArea(), fecha);
		List<DatosRecurso> recursos = recursosDelTipo(idSede, oVariante);
		List<FilaBloqueAgenda> bloques = bloquesDelDia(idSede, fecha);
		int duracion = oVariante.duracionRefMin();
		LocalDateTime ahora = LocalDateTime.now();
		LocalDateTime cierre = fecha.atTime(oHorario.cierre());
		List<HoraLibre> horasLibres = new ArrayList<>();
		for (LocalDateTime inicio = fecha.atTime(oHorario.apertura()); !inicio.plusMinutes(duracion).isAfter(cierre); inicio = inicio.plusMinutes(duracion)) {
			if (!inicio.isBefore(ahora)) {
				evaluarHora(inicio, duracion, especialistas, recursos, bloques).ifPresent(horasLibres::add);
			}
		}
		return horasLibres;
	}

	public boolean validarSinCruce(Long idPersonal, Long idRecurso, LocalDateTime fechaHoraInicio, Integer duracionMin) {
		return validarSinCruce(idPersonal, idRecurso, fechaHoraInicio, duracionMin, null);
	}

	/** CUS11: la cita que se está iniciando no choca consigo misma (DF-30). Si hay cruce, responde 409 e indica el conflicto (RF08). */
	public boolean validarSinCruce(Long idPersonal, Long idRecurso, LocalDateTime horaInicio, Integer tiempoDeclaradoMin, Long idCitaPropia) {
		if (personalApi.obtenerIdSede(idPersonal).isEmpty()) {
			throw new ReglaNegocioException("RF07", "La especialista " + idPersonal + " no existe.");
		}
		DatosRecurso oRecurso = recursoApi.obtenerRecurso(idRecurso);
		LocalDateTime fin = horaInicio.plusMinutes((long) tiempoDeclaradoMin + oRecurso.minutosLimpieza());
		for (Cita oCita : repositorioCita.findOcupacion(oRecurso.idSede(), horaInicio.toLocalDate())) {
			if (!oCita.getIdCita().equals(idCitaPropia) && seCruzan(horaInicio, fin, oCita.getFechaHoraInicio(), oCita.getFechaHoraFin())) {
				exigirOtraPersonaYRecurso(idPersonal, idRecurso, oCita.getIdPersonal(), oCita.getIdRecurso(), "RN02",
						"la cita N° " + oCita.getIdCita());
			}
		}
		for (Atencion oAtencion : repositorioAtencion.findOcupacionSinCita(oRecurso.idSede(), horaInicio.toLocalDate())) {
			if (seCruzan(horaInicio, fin, oAtencion.getHoraInicio(), finConLimpieza(oAtencion))) {
				exigirOtraPersonaYRecurso(idPersonal, idRecurso, oAtencion.getIdPersonal(), oAtencion.getIdRecurso(), "RN34",
						"la atención N° " + oAtencion.getIdAtencion());
			}
		}
		return true;
	}

	public List<BloqueAgendaDTO> armarAgenda(Long idSede, LocalDate fecha, Long idPersonal, Long idRecurso) {
		List<FilaBloqueAgenda> bloques = bloquesDelDia(idSede, fecha);
		List<BloqueAgendaDTO> agenda = new ArrayList<>();
		bloques.stream()
			.filter(oBloque -> (idPersonal == null || idPersonal.equals(oBloque.getIdPersonal()))
					&& (idRecurso == null || idRecurso.equals(oBloque.getIdRecurso())))
			.map(oBloque -> new BloqueAgendaDTO(OCUPADA, oBloque.getOrigen(), oBloque.getIdOrigen(), oBloque.getIdPersonal(), oBloque.getIdRecurso(),
					oBloque.getIdVariante(), oBloque.getInicio(), oBloque.getFin(), oBloque.getEstado()))
			.forEach(agenda::add);
		if (idRecurso == null) {
			agregarFranjasDeEspecialistas(agenda, idSede, fecha, idPersonal, bloques);
		}
		if (idPersonal == null) {
			agregarFranjasDeRecursos(agenda, idSede, fecha, idRecurso, bloques);
		}
		agenda.sort(Comparator.comparing(BloqueAgendaDTO::inicio));
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

	/** Vacío si falta especialista o recurso. La limpieza del bloque depende del recurso: a la especialista se le exige la menor. */
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
		int limpiezaMenor = recursosLibres.stream().mapToInt(DatosRecurso::minutosLimpieza).min().orElseThrow();
		List<Long> idsPersonal = especialistas.stream()
			.filter(oEspecialista -> !inicio.toLocalTime().isBefore(oEspecialista.horaEntrada())
					&& !finServicio.toLocalTime().isAfter(oEspecialista.horaSalida()))
			.filter(oEspecialista -> bloques.stream()
				.noneMatch(oBloque -> oEspecialista.idPersonal().equals(oBloque.getIdPersonal())
						&& seCruzan(inicio, finServicio.plusMinutes(limpiezaMenor), oBloque.getInicio(), oBloque.getFin())))
			.map(EspecialistaHorario::idPersonal)
			.toList();
		if (idsPersonal.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new HoraLibre(inicio, duracion, idsPersonal, recursosLibres.stream().map(DatosRecurso::idRecurso).toList()));
	}

	private void agregarFranjasDeEspecialistas(List<BloqueAgendaDTO> agenda, Long idSede, LocalDate fecha, Long idPersonal,
			List<FilaBloqueAgenda> bloques) {
		for (EspecialistaHorario oEspecialista : personalApi.listarEspecialistas(idSede, null, fecha)) {
			if (idPersonal == null || idPersonal.equals(oEspecialista.idPersonal())) {
				List<FilaBloqueAgenda> ocupadas = bloques.stream().filter(oBloque -> oEspecialista.idPersonal().equals(oBloque.getIdPersonal())).toList();
				agregarFranjasLibres(agenda, fecha.atTime(oEspecialista.horaEntrada()), fecha.atTime(oEspecialista.horaSalida()), ocupadas,
						oEspecialista.idPersonal(), null);
			}
		}
	}

	private void agregarFranjasDeRecursos(List<BloqueAgendaDTO> agenda, Long idSede, LocalDate fecha, Long idRecurso, List<FilaBloqueAgenda> bloques) {
		HorarioSede oHorario = recursoApi.obtenerHorarioSede(idSede);
		for (DatosRecurso oRecurso : recursoApi.listarDisponibles(idSede)) {
			if (idRecurso == null || idRecurso.equals(oRecurso.idRecurso())) {
				List<FilaBloqueAgenda> ocupadas = bloques.stream().filter(oBloque -> oRecurso.idRecurso().equals(oBloque.getIdRecurso())).toList();
				agregarFranjasLibres(agenda, fecha.atTime(oHorario.apertura()), fecha.atTime(oHorario.cierre()), ocupadas, null, oRecurso.idRecurso());
			}
		}
	}

	/** ocupadas viene ordenada por inicio, como la devuelve findBloques. */
	private void agregarFranjasLibres(List<BloqueAgendaDTO> agenda, LocalDateTime desde, LocalDateTime hasta, List<FilaBloqueAgenda> ocupadas,
			Long idPersonal, Long idRecurso) {
		LocalDateTime cursor = desde;
		for (FilaBloqueAgenda oBloque : ocupadas) {
			if (oBloque.getInicio().isAfter(cursor) && cursor.isBefore(hasta)) {
				agenda.add(franjaLibre(cursor, oBloque.getInicio().isBefore(hasta) ? oBloque.getInicio() : hasta, idPersonal, idRecurso));
			}
			if (oBloque.getFin().isAfter(cursor)) {
				cursor = oBloque.getFin();
			}
		}
		if (cursor.isBefore(hasta)) {
			agenda.add(franjaLibre(cursor, hasta, idPersonal, idRecurso));
		}
	}

	private BloqueAgendaDTO franjaLibre(LocalDateTime inicio, LocalDateTime fin, Long idPersonal, Long idRecurso) {
		return new BloqueAgendaDTO(LIBRE, null, null, idPersonal, idRecurso, null, inicio, fin, null);
	}

	private LocalDateTime finConLimpieza(Atencion oAtencion) {
		LocalDateTime fin = oAtencion.getHoraFin() != null ? oAtencion.getHoraFin() : oAtencion.getHoraInicio().plusMinutes(oAtencion.getTiempoDeclaradoMin());
		return fin.plusMinutes(recursoApi.obtenerRecurso(oAtencion.getIdRecurso()).minutosLimpieza());
	}

	private void exigirOtraPersonaYRecurso(Long idPersonal, Long idRecurso, Long idPersonalOcupado, Long idRecursoOcupado, String regla, String ocupacion) {
		if (idPersonal.equals(idPersonalOcupado)) {
			throw new ReglaNegocioException(regla, "Cruce de horario: la especialista ya tiene " + ocupacion + " (incluye la limpieza del recurso).",
					HttpStatus.CONFLICT);
		}
		if (idRecurso.equals(idRecursoOcupado)) {
			throw new ReglaNegocioException(regla, "Cruce de horario: el recurso ya tiene " + ocupacion + " (incluye la limpieza del recurso).",
					HttpStatus.CONFLICT);
		}
	}

	/** Mismo criterio que PKG_AGENDA.validar_cruce: el bloque que termina justo cuando el otro empieza no choca. */
	private static boolean seCruzan(LocalDateTime inicio, LocalDateTime fin, LocalDateTime otroInicio, LocalDateTime otroFin) {
		return otroInicio.isBefore(fin) && otroFin.isAfter(inicio);
	}

}
