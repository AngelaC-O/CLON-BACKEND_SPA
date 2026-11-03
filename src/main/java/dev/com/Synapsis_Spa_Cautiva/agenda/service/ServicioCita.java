package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.BloqueAgendaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.CitaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.ListaEsperaDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.MapperCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ListaEspera;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioListaEspera;
import dev.com.Synapsis_Spa_Cautiva.comun.api.CajaAbiertaProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.EnlacePagoDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
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

	private final MapperCita mapperCita;

	@Transactional(readOnly = true)
	public List<HoraLibre> consultarDisponibilidad(Long idVariante, Long idSede, LocalDate fecha) {
		return gestorDisponibilidadAgenda.calcularHorasLibres(idVariante, idSede, fecha);
	}

	/** Oracle repite las reglas: cruce (-20010, -20011), área de la especialista (-20012), horario de la sede (-20014). */
	@Transactional
	public CitaDTO agendar(CitaDTO oCitaDTO) {
		if (!repositorioCliente.existsById(oCitaDTO.idCliente())) {
			throw new ReglaNegocioException("RF07", "La clienta " + oCitaDTO.idCliente() + " no existe.");
		}
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

	@Transactional(readOnly = true)
	public List<BloqueAgendaDTO> consultarAgenda(Long idSede, LocalDate fecha, Long idPersonal, Long idRecurso) {
		return gestorDisponibilidadAgenda.armarAgenda(idSede, fecha, idPersonal, idRecurso);
	}

	private Cita buscarCita(Long idCita) {
		return repositorioCita.findById(idCita)
			.orElseThrow(() -> new ReglaNegocioException("RF10", "La cita " + idCita + " no existe.", HttpStatus.NOT_FOUND));
	}

}
