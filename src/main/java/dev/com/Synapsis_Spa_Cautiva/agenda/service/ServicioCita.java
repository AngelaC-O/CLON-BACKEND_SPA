package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.time.LocalDate;
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
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import lombok.RequiredArgsConstructor;

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

	@Transactional(readOnly = true)
	public List<BloqueAgendaDTO> consultarAgenda(Long idSede, LocalDate fecha, Long idPersonal, Long idRecurso) {
		return gestorDisponibilidadAgenda.armarAgenda(idSede, fecha, idPersonal, idRecurso);
	}

}
