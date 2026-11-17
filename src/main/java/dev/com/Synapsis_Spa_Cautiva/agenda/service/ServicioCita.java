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
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.ReprogramacionDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;
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
import dev.com.Synapsis_Spa_Cautiva.sedes.api.DatosRecurso;
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

	private static final int MAX_MOTIVO_SALDO = 150;

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

	/** [It. 2.1 · nombres] Clienta, especialista, servicio y recurso de cada cita y nombre del recurso de cada franja, en lote. */
	private final NombresAgenda nombresAgenda;

	@Transactional(readOnly = true)
	public List<HoraLibre> consultarDisponibilidad(Long idVariante, Long idSede, LocalDate fecha) {
		return gestorDisponibilidadAgenda.calcularHorasLibres(idVariante, idSede, fecha);
	}

	/**
	 * [It. 2] RN49: la fecha y hora no pasaron (solo Java). Oracle repite las reglas: cruce (-20010, -20011), área de la especialista (-20012),
	 * horario de la sede (-20014). Si la cita sale de una espera (idEspera), en la misma transacción la espera pasa a ASIGNADA con su id_cita
	 * (RF09, CK_LISTA_ESPERA_ASIGNADA); si la espera ya no está PENDIENTE responde 409 y la cita no se guarda.
	 */
	@Transactional
	public CitaDTO agendar(CitaDTO oCitaDTO) {
		if (!repositorioCliente.existsById(oCitaDTO.idCliente())) {
			throw new ReglaNegocioException("RF07", "La clienta " + oCitaDTO.idCliente() + " no existe.");
		}
		validarFechaFutura(oCitaDTO.fechaHoraInicio());
		gestorDisponibilidadAgenda.validarSinCruce(oCitaDTO.idPersonal(), oCitaDTO.idRecurso(), oCitaDTO.fechaHoraInicio(), oCitaDTO.duracionMin());
		Cita oCita = new Cita(oCitaDTO.idCliente(), usuarioActual.idSede(), oCitaDTO.idPersonal(), oCitaDTO.idRecurso(), oCitaDTO.idVariante(),
				oCitaDTO.fechaHoraInicio(), oCitaDTO.duracionMin(), usuarioActual.idUsuario());
		repositorioCita.save(oCita);
		if (oCitaDTO.idEspera() == null) {
			return conNombres(mapperCita.aDTO(oCita));
		}
		ListaEspera oListaEspera = buscarEspera(oCitaDTO.idEspera());
		oListaEspera.asignarCita(oCita.getIdCita());
		repositorioListaEspera.save(oListaEspera);
		return conNombres(mapperCita.aDTO(oCita, oListaEspera.getIdEspera()));
	}

	/**
	 * [It. 2 · CUS09, RF12] La fecha futura (RN49) y el cruce (RN02, sin contar la propia cita) se validan antes; Cita.reprogramar deja la origen
	 * REPROGRAMADA y crea otra con id_cita_origen y el adelanto (RN05). La origen se guarda primero, para que Oracle no la cuente en el cruce.
	 */
	@Transactional
	public CitaDTO reprogramar(Long idCita, ReprogramacionDTO oReprogramacionDTO) {
		Cita oCita = buscarCita(idCita);
		validarFechaFutura(oReprogramacionDTO.fechaHoraInicio());
		gestorDisponibilidadAgenda.validarSinCruce(oReprogramacionDTO.idPersonal(), oReprogramacionDTO.idRecurso(), oReprogramacionDTO.fechaHoraInicio(),
				oCita.getDuracionMin(), idCita);
		Cita oCitaNueva = oCita.reprogramar(oReprogramacionDTO.fechaHoraInicio(), oReprogramacionDTO.idPersonal(), oReprogramacionDTO.idRecurso());
		repositorioCita.saveAndFlush(oCita);
		return conNombres(mapperCita.aDTO(repositorioCita.save(oCitaNueva)));
	}

	/**
	 * [It. 2 · CUS10, RF12] Cita.cancelar con motivo, también cuando la clienta no se presenta (motivo «No se presentó»: no hay estado
	 * NO_ASISTIO). Si tenía adelanto, pasa a saldo a favor con el id de la cita (RN05): TRG_SALDO_FAVOR_REGLAS solo acepta un ABONO con cita.
	 */
	@Transactional
	public CitaDTO cancelar(Long idCita, String motivoCancelacion) {
		Cita oCita = buscarCita(idCita);
		oCita.cancelar(motivoCancelacion);
		repositorioCita.saveAndFlush(oCita);
		if (oCita.getAdelantoMonto().signum() > 0) {
			Cliente oCliente = repositorioCliente.findById(oCita.getIdCliente())
				.orElseThrow(() -> new ReglaNegocioException("RF12", "La clienta " + oCita.getIdCliente() + " no existe.", HttpStatus.NOT_FOUND));
			oCliente.abonarSaldo(oCita.getAdelantoMonto(), idCita, motivoSaldo(idCita, oCita.getMotivoCancelacion()));
			repositorioCliente.save(oCliente);
		}
		return conNombres(mapperCita.aDTO(oCita));
	}

	/** [It. 2 · CUS06, RF09] Las esperas PENDIENTES del día con el nombre de la clienta (PersonaApi) y del servicio (CatalogoApi). */
	@Transactional(readOnly = true)
	public List<ListaEsperaDTO> listarEspera(Long idSede, LocalDate fecha) {
		List<ListaEspera> esperas = repositorioListaEspera.findPendientes(idSede, fecha);
		Map<Long, PersonaDTO> personas = personasPorId(esperas.stream().map(ListaEspera::getIdCliente).toList());
		Map<Long, DatosVariante> variantes = catalogoApi.obtenerVariantes(esperas.stream().map(ListaEspera::getIdVariante).toList()).stream()
			.collect(Collectors.toMap(DatosVariante::idVariante, Function.identity()));
		return esperas.stream()
			.map(oListaEspera -> mapperCita.aDTO(oListaEspera, nombreDe(personas.get(oListaEspera.getIdCliente())),
					variantes.containsKey(oListaEspera.getIdVariante()) ? variantes.get(oListaEspera.getIdVariante()).nombre() : null))
			.toList();
	}

	/** [It. 2 · CUS06, RF09] La espera queda DESCARTADA; si ya se asignó o descartó responde 409. */
	@Transactional
	public ListaEsperaDTO descartarEspera(Long idEspera) {
		ListaEspera oListaEspera = buscarEspera(idEspera);
		oListaEspera.descartar();
		return mapperCita.aDTO(repositorioListaEspera.save(oListaEspera), (List<HoraLibre>) null);
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
		return conNombres(mapperCita.aDTO(repositorioCita.saveAndFlush(oCita)));
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
		// [It. 2.1 · nombres] El nombre del recurso de cada franja: una sola llamada con los recursos de la sede
		Map<Long, String> recursos = recursoApi.listarRecursos(idSede).stream().collect(Collectors.toMap(DatosRecurso::idRecurso, DatosRecurso::nombre));
		return agenda.stream()
			.map(oFranja -> oFranja.conNombres(
					oFranja.idCliente() == null ? oFranja.nombreCliente() : nombreDe(personas.get(oFranja.idCliente())),
					oFranja.idPersonal() == null ? null : nombreDe(personas.get(oFranja.idPersonal())),
					oFranja.idVariante() == null || !variantes.containsKey(oFranja.idVariante()) ? null : variantes.get(oFranja.idVariante()).nombre(),
					oFranja.idRecurso() == null ? null : recursos.get(oFranja.idRecurso())))
			.toList();
	}

	/** [It. 2.1 · nombres] La cita de la respuesta con clienta, especialista, «Servicio · Variante» y recurso. */
	private CitaDTO conNombres(CitaDTO oCitaDTO) {
		return conNombres(List.of(oCitaDTO)).getFirst();
	}

	/** [It. 2.1 · nombres] Las citas de una respuesta (también las pendientes de la ficha, CUS03), con una búsqueda por tipo de nombre. */
	List<CitaDTO> conNombres(List<CitaDTO> citas) {
		NombresAgenda.Nombres nombres = nombresAgenda.buscar(
				citas.stream().flatMap(oCitaDTO -> Stream.of(oCitaDTO.idCliente(), oCitaDTO.idPersonal())).toList(),
				citas.stream().map(CitaDTO::idVariante).toList(), citas.stream().map(CitaDTO::idRecurso).toList(), List.of(), List.of());
		return citas.stream()
			.map(oCitaDTO -> oCitaDTO.conNombres(nombres.persona(oCitaDTO.idCliente()), nombres.persona(oCitaDTO.idPersonal()),
					nombres.servicio(oCitaDTO.idVariante()), nombres.recurso(oCitaDTO.idRecurso())))
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

	private ListaEspera buscarEspera(Long idEspera) {
		return repositorioListaEspera.findById(idEspera)
			.orElseThrow(() -> new ReglaNegocioException("RF09", "La espera " + idEspera + " no existe.", HttpStatus.NOT_FOUND));
	}

	/** SALDO_FAVOR_MOV.motivo admite 150 caracteres y CITA.motivo_cancelacion, 200. */
	private static String motivoSaldo(Long idCita, String motivoCancelacion) {
		String motivo = "Adelanto de la cita " + idCita + " cancelada: " + motivoCancelacion;
		return motivo.length() <= MAX_MOTIVO_SALDO ? motivo : motivo.substring(0, MAX_MOTIVO_SALDO);
	}

}
