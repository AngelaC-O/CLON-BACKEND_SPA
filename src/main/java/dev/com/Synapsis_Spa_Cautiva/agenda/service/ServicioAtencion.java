package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionCobrable;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.AtencionDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.CitaAtencionDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.DetalleAtencionDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.MapperAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioClientePaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.DatosRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.RecursoApi;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioAtencion implements AtencionApi {

	private final RepositorioCita repositorioCita;

	private final GestorDisponibilidadAgenda gestorDisponibilidadAgenda;

	private final RepositorioAtencion repositorioAtencion;

	private final RepositorioClientePaquete repositorioClientePaquete;

	private final CatalogoApi catalogoApi;

	private final UsuarioActual usuarioActual;

	private final RecursoApi recursoApi;

	private final MapperAtencion mapperAtencion;

	/** [It. 2 · RF13] «Mi agenda» de la especialista de la sesión (UsuarioActual.idPersonal), no de un selector. Sin fecha, la de hoy. */
	@Transactional(readOnly = true)
	public List<CitaAtencionDTO> listarMiAgenda(LocalDate fecha) {
		LocalDate dia = fecha == null ? LocalDate.now() : fecha;
		return repositorioCita.findDelDiaPorPersonal(usuarioActual.idPersonal(), dia).stream().map(this::conAtencion).toList();
	}

	/** [It. 2] Retoma la atención EN_CURSO de una cita desde otro equipo (Dudas 6 y 7 del front-end); 404 si no hay. */
	@Transactional(readOnly = true)
	public AtencionDTO obtenerEnCurso(Long idCita) {
		return repositorioAtencion.findEnCursoPorCita(idCita).map(mapperAtencion::aDTO)
			.orElseThrow(() -> new ReglaNegocioException("RF13", "La cita " + idCita + " no tiene una atención EN_CURSO.", HttpStatus.NOT_FOUND));
	}

	/** saveAndFlush: la cita pasa a EN_ATENCION antes de insertar la atención, en el orden del diagrama. [It. 2] RN50: 403 si no es su especialista. */
	@Transactional
	public AtencionDTO iniciar(Long idCita, Integer tiempoDeclaradoMin) {
		Cita oCita = buscarCita(idCita);
		oCita.verificarEspecialista(usuarioActual.idPersonal(), usuarioActual.esAdministradora(usuarioActual.idUsuario()));
		gestorDisponibilidadAgenda.validarSinCruce(oCita.getIdPersonal(), oCita.getIdRecurso(), LocalDateTime.now(), tiempoDeclaradoMin, idCita);
		oCita.iniciarAtencion(tiempoDeclaradoMin);
		repositorioCita.saveAndFlush(oCita);
		Atencion oAtencion = new Atencion(idCita, oCita.getIdCliente(), oCita.getIdPersonal(), oCita.getIdRecurso(), oCita.getIdVariante(),
				tiempoDeclaradoMin);
		return mapperAtencion.aDTO(repositorioAtencion.save(oAtencion));
	}

	@Transactional
	public AtencionDTO finalizar(Long idAtencion, DetalleAtencionDTO oDetalleAtencionDTO) {
		Atencion oAtencion = buscarAtencion(idAtencion);
		oAtencion.verificarEspecialista(usuarioActual.idPersonal(), usuarioActual.esAdministradora(usuarioActual.idUsuario()));
		oAtencion.registrarDetalle(oDetalleAtencionDTO.observaciones());
		oAtencion.finalizar(LocalDateTime.now());
		usarSesionDePaquete(oAtencion);
		repositorioAtencion.saveAndFlush(oAtencion);
		if (oAtencion.getIdCita() != null) {
			Cita oCita = buscarCita(oAtencion.getIdCita());
			oCita.finalizar();
			repositorioCita.save(oCita);
		}
		return mapperAtencion.aDTO(oAtencion);
	}

	@Override
	@Transactional(readOnly = true)
	public List<HoraLibre> libresAhora(Long idVariante, Long idSede) {
		return gestorDisponibilidadAgenda.especialistasYRecursosLibres(idVariante, idSede, LocalDateTime.now());
	}

	@Override
	@Transactional
	public Long registrarSinCita(Long idCliente, String nombreClientePaso, Long idPersonal, Long idRecurso, Long idVariante,
			Integer tiempoDeclaradoMin) {
		gestorDisponibilidadAgenda.validarSinCruce(idPersonal, idRecurso, LocalDateTime.now(), tiempoDeclaradoMin);
		Atencion oAtencion = new Atencion(idCliente, nombreClientePaso, idPersonal, idRecurso, idVariante, tiempoDeclaradoMin);
		return repositorioAtencion.save(oAtencion).getIdAtencion();
	}

	@Override
	@Transactional(readOnly = true)
	public AtencionCobrable obtenerAtencionCobrable(Long idAtencion) {
		Atencion oAtencion = buscarAtencion(idAtencion);
		if (oAtencion.getEstado() == EstadoAtencion.COBRADA) {
			throw new ReglaNegocioException("RF21", "La atención " + idAtencion + " ya está COBRADA.", HttpStatus.CONFLICT);
		}
		if (oAtencion.getEstado() != EstadoAtencion.FINALIZADA) {
			throw new ReglaNegocioException("RF21", "Solo se cobra una atención FINALIZADA; esta está " + oAtencion.getEstado() + ".");
		}
		return aCobrable(oAtencion);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AtencionCobrable> listarCobrables(Long idSede) {
		List<Long> idsRecurso = recursoApi.listarRecursos(idSede).stream().map(DatosRecurso::idRecurso).toList();
		if (idsRecurso.isEmpty()) {
			return List.of();
		}
		return repositorioAtencion.findFinalizadasEnRecursos(idsRecurso).stream().map(this::aCobrable).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Long obtenerIdVariante(Long idAtencion) {
		return buscarAtencion(idAtencion).getIdVariante();
	}

	@Override
	@Transactional
	public void finalizarSinCita(Long idAtencion) {
		Atencion oAtencion = buscarAtencion(idAtencion);
		oAtencion.finalizar(oAtencion.getHoraInicio().plusMinutes(oAtencion.getTiempoDeclaradoMin()));
		repositorioAtencion.saveAndFlush(oAtencion);
	}

	/** Si el servicio está en un paquete activo con sesiones, la sesión es esta atención (RN36, RN37, RN38). TRG_ATENCION_PAQUETE lo repite. */
	private void usarSesionDePaquete(Atencion oAtencion) {
		if (oAtencion.getIdCliente() == null) {
			return;
		}
		Long idServicio = catalogoApi.obtenerVariante(oAtencion.getIdVariante()).idServicio();
		repositorioClientePaquete.findActivoPorCliente(oAtencion.getIdCliente(), idServicio)
			.filter(oClientePaquete -> oClientePaquete
				.verificarSesionDisponible(repositorioClientePaquete.contarSesionesUsadas(oClientePaquete.getIdClientePaquete())))
			.ifPresent(oClientePaquete -> oAtencion.usarSesion(oClientePaquete.getIdClientePaquete()));
	}

	private AtencionCobrable aCobrable(Atencion oAtencion) {
		BigDecimal adelantoMonto = oAtencion.getIdCita() == null ? BigDecimal.ZERO : buscarCita(oAtencion.getIdCita()).getAdelantoMonto();
		return new AtencionCobrable(oAtencion.getIdAtencion(), oAtencion.getIdCliente(), oAtencion.getNombreClientePaso(), oAtencion.getIdPersonal(),
				oAtencion.getIdVariante(), oAtencion.getIdClientePaquete(), adelantoMonto);
	}

	/** La atención EN_CURSO de la cita, para que la especialista la retome y la finalice. */
	private CitaAtencionDTO conAtencion(Cita oCita) {
		Atencion oAtencion = oCita.getEstado() == EstadoCita.EN_ATENCION ? repositorioAtencion.findEnCursoPorCita(oCita.getIdCita()).orElse(null) : null;
		return new CitaAtencionDTO(oCita.getIdCita(), oCita.getIdCliente(), oCita.getIdRecurso(), oCita.getIdVariante(), oCita.getFechaHoraInicio(),
				oCita.getDuracionMin(), oCita.getFechaHoraFin(), oCita.getEstado(), oAtencion == null ? null : oAtencion.getIdAtencion(),
				oAtencion == null ? null : oAtencion.getEstado());
	}

	private Cita buscarCita(Long idCita) {
		return repositorioCita.findById(idCita)
			.orElseThrow(() -> new ReglaNegocioException("RF13", "La cita " + idCita + " no existe.", HttpStatus.NOT_FOUND));
	}

	private Atencion buscarAtencion(Long idAtencion) {
		return repositorioAtencion.findById(idAtencion)
			.orElseThrow(() -> new ReglaNegocioException("RF13", "La atención " + idAtencion + " no existe.", HttpStatus.NOT_FOUND));
	}

}
