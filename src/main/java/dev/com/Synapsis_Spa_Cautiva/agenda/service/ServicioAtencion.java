package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionCobrable;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.AtencionDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.DetalleAtencionDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.MapperAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioClientePaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
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

	private final MapperAtencion mapperAtencion;

	/** saveAndFlush: la cita pasa a EN_ATENCION antes de insertar la atención, en el orden del diagrama. */
	@Transactional
	public AtencionDTO iniciar(Long idCita, Integer tiempoDeclaradoMin) {
		Cita oCita = buscarCita(idCita);
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
		if (oAtencion.getEstado() != EstadoAtencion.FINALIZADA) {
			throw new ReglaNegocioException("RF21", "Solo se cobra una atención FINALIZADA; esta está " + oAtencion.getEstado() + ".");
		}
		return new AtencionCobrable(oAtencion.getIdAtencion(), oAtencion.getIdCliente(), oAtencion.getNombreClientePaso(), oAtencion.getIdPersonal(),
				oAtencion.getIdVariante(), oAtencion.getIdClientePaquete());
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

	private Cita buscarCita(Long idCita) {
		return repositorioCita.findById(idCita)
			.orElseThrow(() -> new ReglaNegocioException("RF13", "La cita " + idCita + " no existe.", HttpStatus.NOT_FOUND));
	}

	private Atencion buscarAtencion(Long idAtencion) {
		return repositorioAtencion.findById(idAtencion)
			.orElseThrow(() -> new ReglaNegocioException("RF13", "La atención " + idAtencion + " no existe.", HttpStatus.NOT_FOUND));
	}

}
