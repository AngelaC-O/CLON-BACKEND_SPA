package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.CitaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.FiltrosAgendaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.FranjaAgendaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.ListaEsperaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.MapperCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ListaEspera;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioListaEspera;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosVariante;
import dev.com.Synapsis_Spa_Cautiva.comun.api.CajaAbiertaProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.EnlacePagoDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.personal.api.EspecialistaHorario;
import dev.com.Synapsis_Spa_Cautiva.personal.api.PersonalApi;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.RecursoApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioCita {

	private final GestorDisponibilidadAgenda gestorDisponibilidadAgenda;

	private final RepositorioCita repositorioCita;

	private final RepositorioListaEspera repositorioListaEspera;

	private final RepositorioCliente repositorioCliente;

	private final UsuarioActual usuarioActual;

	private final CajaAbiertaProvider cajaAbiertaProvider;

	private final PagosApi pagosApi;

	private final PersonalApi personalApi;

	private final RecursoApi recursoApi;

	private final PersonaApi personaApi;

	private final CatalogoApi catalogoApi;

	private final MapperCita mapperCita;

	@Transactional(readOnly = true)
	public List<HoraLibre> consultarDisponibilidad(Long idVariante, Long idSede, LocalDate fecha) {
		return gestorDisponibilidadAgenda.calcularHorasLibres(idVariante, idSede, fecha);
	}

	/** [It. 2] RN49: la fecha y hora no pasaron (solo Java). Oracle repite las reglas: cruce (-20010, -20011), área de la especialista (-20012), horario de la sede (-20014). */
	@Transactional
	public CitaDTO agendar(CitaDTO oCitaDTO) {
		if (!repositorioCliente.existsById(oCitaDTO.idCliente())) {
			throw new ReglaNegocioException("RF07", "La clienta " + oCitaDTO.idCliente() + " no existe.");
		}
		validarFechaFutura(oCitaDTO.fechaHoraInicio());
		gestorDisponibilidadAgenda.validarSinCruce(oCitaDTO.idPersonal(), oCitaDTO.idRecurso(), oCitaDTO.fechaHoraInicio(), oCitaDTO.duracionMin());
		Cita oCita = new Cita(oCitaDTO.idCliente(), usuarioActual.idSede(), oCitaDTO.idPersonal(), oCitaDTO.idRecurso(), oCitaDTO.idVariante(),
				oCitaDTO.fechaHoraInicio(), oCitaDTO.duracionMin(), usuarioActual.idUsuario());
		return mapperCita.aDTO(repositorioCita.save(oCita));
	}

	@Transactional
	public ListaEsperaDTO registrarEspera(ListaEsperaDTO oListaEsperaDTO) {
		Long idSede = usuarioActual.idSede();
		List<HoraLibre> horasLibres = gestorDisponibilidadAgenda.calcularHorasLibres(oListaEsperaDTO.idVariante(), idSede,
				oListaEsperaDTO.fechaDeseada());
		ListaEspera oListaEspera = new ListaEspera(oListaEsperaDTO.idCliente(), idSede, oListaEsperaDTO.idVariante(), oListaEsperaDTO.fechaDeseada(),
				oListaEsperaDTO.horaDesde(), oListaEsperaDTO.horaHasta(), usuarioActual.idUsuario());
		return mapperCita.aDTO(repositorioListaEspera.save(oListaEspera), horasLibres);
	}

	@Transactional
	public CitaDTO registrarAdelantoEfectivo(Long idCita, BigDecimal adelantoMonto, Long idMedioPago) {
		if (pagosApi.usaPasarela(idMedioPago)) {
			throw new ReglaNegocioException("RN42", "Yape, Plin y tarjeta se cobran con el enlace de pago.");
		}
		Long idCaja = cajaAbiertaProvider.obtenerCajaAbierta(usuarioActual.idSede());
		Cita oCita = buscarCita(idCita);
		oCita.registrarAdelanto(adelantoMonto, idMedioPago, LocalDateTime.now(), idCaja);
		return mapperCita.aDTO(repositorioCita.saveAndFlush(oCita));
	}

	/** Aquí la cita no cambia: el adelanto queda registrado cuando la pasarela confirma (RN04). */
	@Transactional
	public EnlacePagoDTO generarEnlaceAdelanto(Long idCita, BigDecimal adelantoMonto, Long idMedioPago) {
		Cita oCita = buscarCita(idCita);
		return pagosApi.generarEnlace(idCita, adelantoMonto, idMedioPago, oCita.getIdSede());
	}

	/** [It. 2] RN49: no se agenda ni se reprograma en una fecha y hora que ya pasaron (422). Oracle no lo repite (D-10). */
	public void validarFechaFutura(LocalDateTime fechaHoraInicio) {
		if (!fechaHoraInicio.isAfter(LocalDateTime.now())) {
			throw new ReglaNegocioException("RN49", "La fecha y hora de la cita ya pasaron.");
		}
	}

	/** [It. 2] CUS08: las especialistas (PersonalApi, con su nombre de PersonaApi) y los recursos (RecursoApi) de la sede. */
	@Transactional(readOnly = true)
	public FiltrosAgendaDTO listarFiltros(Long idSede) {
		List<EspecialistaHorario> especialistas = personalApi.listarEspecialistas(idSede);
		Map<Long, PersonaDTO> personas = personasPorId(especialistas.stream().map(EspecialistaHorario::idPersonal).toList());
		List<FiltrosAgendaDTO.Especialista> filtroEspecialistas = especialistas.stream().map(oEspecialista -> {
			PersonaDTO oPersonaDTO = personas.get(oEspecialista.idPersonal());
			return new FiltrosAgendaDTO.Especialista(oEspecialista.idPersonal(), oPersonaDTO == null ? null : oPersonaDTO.nombres(),
					oPersonaDTO == null ? null : oPersonaDTO.apellidos());
		}).toList();
		List<FiltrosAgendaDTO.Recurso> filtroRecursos = recursoApi.listarRecursos(idSede).stream()
			.map(oRecurso -> new FiltrosAgendaDTO.Recurso(oRecurso.idRecurso(), oRecurso.nombre(), oRecurso.tipo(), oRecurso.estado()))
			.toList();
		return new FiltrosAgendaDTO(filtroEspecialistas, filtroRecursos);
	}

	/** [It. 2] Cada franja trae el nombre de la clienta, de la especialista y del servicio (PersonaApi, CatalogoApi), no solo sus id. */
	@Transactional(readOnly = true)
	public List<FranjaAgendaDTO> consultarAgenda(Long idSede, LocalDate fecha, Long idPersonal, Long idRecurso) {
		List<FranjaAgendaDTO> agenda = gestorDisponibilidadAgenda.armarAgenda(idSede, fecha, idPersonal, idRecurso);
		Map<Long, PersonaDTO> personas = personasPorId(agenda.stream()
			.flatMap(oFranja -> Stream.of(oFranja.idCliente(), oFranja.idPersonal()))
			.filter(Objects::nonNull)
			.toList());
		Map<Long, DatosVariante> variantes = catalogoApi.obtenerVariantes(agenda.stream().map(FranjaAgendaDTO::idVariante).filter(Objects::nonNull).toList())
			.stream()
			.collect(Collectors.toMap(DatosVariante::idVariante, Function.identity()));
		return agenda.stream()
			.map(oFranja -> oFranja.conNombres(
					oFranja.idCliente() == null ? oFranja.nombreCliente() : nombreDe(personas.get(oFranja.idCliente())),
					oFranja.idPersonal() == null ? null : nombreDe(personas.get(oFranja.idPersonal())),
					oFranja.idVariante() == null || !variantes.containsKey(oFranja.idVariante()) ? null : variantes.get(oFranja.idVariante()).nombre()))
			.toList();
	}

	private Map<Long, PersonaDTO> personasPorId(Collection<Long> idsPersona) {
		return personaApi.obtenerPersonas(List.copyOf(idsPersona)).stream().collect(Collectors.toMap(PersonaDTO::idPersona, Function.identity()));
	}

	private static String nombreDe(PersonaDTO oPersonaDTO) {
		if (oPersonaDTO == null) {
			return null;
		}
		return oPersonaDTO.apellidos() == null ? oPersonaDTO.nombres() : oPersonaDTO.nombres() + " " + oPersonaDTO.apellidos();
	}

	private Cita buscarCita(Long idCita) {
		return repositorioCita.findById(idCita)
			.orElseThrow(() -> new ReglaNegocioException("RF10", "La cita " + idCita + " no existe.", HttpStatus.NOT_FOUND));
	}

}
