package dev.com.Synapsis_Spa_Cautiva.pagos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.SesionOracle;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.AdelantoAprobado;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.EnlacePagoDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.CanalTransaccionPasarela;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.EstadoTransaccion;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.MedioPago;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.TipoOperacion;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.TransaccionPasarela;
import dev.com.Synapsis_Spa_Cautiva.pagos.repository.FilaAprobadoDelDia;
import dev.com.Synapsis_Spa_Cautiva.pagos.repository.RepositorioTransaccionPasarela;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioPago implements PagosApi {

	private final PasarelaPagosPort pasarelaPagosPort;

	private final RepositorioTransaccionPasarela repositorioTransaccionPasarela;

	private final ApplicationEventPublisher eventos;

	private final UsuarioActual usuarioActual;

	private final ParametroApi parametroApi;

	private final SesionOracle sesionOracle;

	@Override
	@Transactional(readOnly = true)
	public boolean usaPasarela(Long idMedioPago) {
		return buscarMedio(idMedioPago).getUsaPasarela();
	}

	/**
	 * [It. 2 · RN52] REQUIRES_NEW: la transacción de la pasarela se confirma aparte (TransaccionConSesionOracle llama a PKG_SESION.iniciar
	 * también en esa conexión). Una RECHAZADA se guarda y luego responde 422 (noRollbackFor); la venta que llamó hace rollback, la fila queda.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ReglaNegocioException.class)
	public TransaccionDTO cobrar(Long idVenta, Long idMedioPago, BigDecimal monto, String canal) {
		CanalTransaccionPasarela oCanal = canalDeCobro(canal);
		LocalDateTime fechaExpiracion = vencimientoDelEnlace();
		RespuestaPasarela oRespuesta = pasarelaPagosPort
			.crearCobro(new SolicitudCobro(idVenta, null, idMedioPago, monto, oCanal.name(), fechaExpiracion));
		EstadoTransaccion estado = EstadoTransaccion.valueOf(oRespuesta.estado());
		TransaccionPasarela oTransaccion = new TransaccionPasarela(usuarioActual.idSede(), idMedioPago, TipoOperacion.COBRO, oCanal, monto, null,
				usuarioActual.idUsuario());
		if (estado == EstadoTransaccion.APROBADA) {
			oTransaccion.aprobar(oRespuesta.codigoOperacion());
		}
		else if (estado == EstadoTransaccion.RECHAZADA) {
			oTransaccion.registrarCodigoOperacion(oRespuesta.codigoOperacion());
			oTransaccion.rechazar();
		}
		else {
			oTransaccion.registrarEnlace(oRespuesta.enlacePago(), fechaExpiracion, oRespuesta.codigoOperacion());
		}
		repositorioTransaccionPasarela.saveAndFlush(oTransaccion);
		if (estado == EstadoTransaccion.RECHAZADA) {
			throw new ReglaNegocioException("RF103", "La pasarela rechazó el cobro de S/ " + monto + ": " + oRespuesta.mensaje());
		}
		return aDTO(oTransaccion);
	}

	@Override
	@Transactional(readOnly = true)
	public void exigirAprobada(Long idTransaccion) {
		TransaccionPasarela oTransaccion = repositorioTransaccionPasarela.findById(idTransaccion)
			.orElseThrow(() -> new ReglaNegocioException("RN42", "La transacción " + idTransaccion + " no existe."));
		if (oTransaccion.getEstado() != EstadoTransaccion.APROBADA) {
			throw new ReglaNegocioException("RN42", "La transacción " + idTransaccion + " está " + oTransaccion.getEstado() + ": aún no respalda un pago.");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public TransaccionDTO consultarTransaccion(Long idTransaccion) {
		return repositorioTransaccionPasarela.findById(idTransaccion).map(this::aDTO)
			.orElseThrow(() -> new ReglaNegocioException("RN42", "La transacción " + idTransaccion + " no existe.", HttpStatus.NOT_FOUND));
	}

	@Override
	@Transactional
	public EnlacePagoDTO generarEnlace(Long idCita, BigDecimal adelantoMonto, Long idMedioPago, Long idSede) {
		if (!usaPasarela(idMedioPago)) {
			throw new ReglaNegocioException("RN42", "El enlace de pago es para Yape, Plin o tarjeta.");
		}
		TransaccionPasarela oTransaccion = new TransaccionPasarela(idSede, idMedioPago, TipoOperacion.COBRO, CanalTransaccionPasarela.LINK,
				adelantoMonto, idCita, usuarioActual.idUsuario());
		LocalDateTime fechaExpiracion = vencimientoDelEnlace();
		RespuestaPasarela oRespuesta = pasarelaPagosPort
			.crearEnlace(new SolicitudCobro(null, idCita, idMedioPago, adelantoMonto, CanalTransaccionPasarela.LINK.name(), fechaExpiracion));
		if (EstadoTransaccion.RECHAZADA.name().equals(oRespuesta.estado())) {
			throw new ReglaNegocioException("RF104", "La pasarela rechazó el enlace: " + oRespuesta.mensaje());
		}
		if (oRespuesta.enlacePago() == null) {
			throw new ReglaNegocioException("RF104", "La pasarela no respondió: intente de nuevo.", HttpStatus.SERVICE_UNAVAILABLE);
		}
		oTransaccion.registrarEnlace(oRespuesta.enlacePago(), fechaExpiracion, oRespuesta.codigoOperacion());
		repositorioTransaccionPasarela.save(oTransaccion);
		return new EnlacePagoDTO(oTransaccion.getIdTransaccion(), oTransaccion.getEstado().name(), oTransaccion.getCanal().name(),
				oTransaccion.getEnlacePago(), oTransaccion.getFechaExpiracion());
	}

	/** Sin sesión: la auditoría queda a nombre de quien generó la transacción. Repetir la notificación no tiene efecto (RN42). */
	@Transactional
	public void confirmar(String codigoOperacion, String estado) {
		TransaccionPasarela oTransaccion = repositorioTransaccionPasarela.findByCodigoOperacion(codigoOperacion)
			.orElseThrow(() -> new ReglaNegocioException("RN42", "No hay una transacción con ese código de operación.", HttpStatus.NOT_FOUND));
		EstadoTransaccion oEstado = EstadoTransaccion.valueOf(estado);
		if (oTransaccion.getEstado() != EstadoTransaccion.PENDIENTE || oEstado == EstadoTransaccion.PENDIENTE) {
			return;
		}
		sesionOracle.iniciar(oTransaccion.getIdUsuario());
		if (oEstado == EstadoTransaccion.APROBADA) {
			oTransaccion.aprobar(codigoOperacion);
			repositorioTransaccionPasarela.saveAndFlush(oTransaccion);
			if (oTransaccion.getIdCita() != null) {
				eventos.publishEvent(new AdelantoAprobado(oTransaccion.getIdCita(), oTransaccion.getIdMedioPago(), oTransaccion.getMonto()));
			}
		}
		else {
			oTransaccion.rechazar();
			repositorioTransaccionPasarela.save(oTransaccion);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, BigDecimal> aprobadosDelDia(Long idSede, LocalDateTime fecha) {
		return repositorioTransaccionPasarela
			.sumarAprobadosDelDia(idSede, fecha.toLocalDate().atStartOfDay(), fecha.toLocalDate().plusDays(1).atStartOfDay()).stream()
			.collect(Collectors.toMap(FilaAprobadoDelDia::getIdMedioPago, FilaAprobadoDelDia::getTotal));
	}

	private MedioPago buscarMedio(Long idMedioPago) {
		return repositorioTransaccionPasarela.findMedioPago(idMedioPago)
			.orElseThrow(() -> new ReglaNegocioException("RF26", "El medio de pago " + idMedioPago + " no existe."));
	}

	private CanalTransaccionPasarela canalDeCobro(String canal) {
		if (canal == null) {
			throw new ReglaNegocioException("RF26", "Un pago digital indica el canal: POS, QR, LINK o API.");
		}
		return CanalTransaccionPasarela.valueOf(canal);
	}

	private LocalDateTime vencimientoDelEnlace() {
		return LocalDateTime.now().plusHours(parametroApi.valorNumero("LINK_PAGO_VIGENCIA_HORAS").longValue());
	}

	private TransaccionDTO aDTO(TransaccionPasarela oTransaccion) {
		return new TransaccionDTO(oTransaccion.getIdTransaccion(), oTransaccion.getEstado().name(), oTransaccion.getCodigoOperacion(),
				oTransaccion.getEnlacePago());
	}

}
