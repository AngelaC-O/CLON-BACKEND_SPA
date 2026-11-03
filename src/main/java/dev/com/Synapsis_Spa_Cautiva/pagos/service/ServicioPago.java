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

	@Override
	@Transactional
	public TransaccionDTO cobrar(Long idVenta, Long idMedioPago, BigDecimal monto, String canal) {
		CanalTransaccionPasarela oCanal = canalDeCobro(canal);
		RespuestaPasarela oRespuesta = pasarelaPagosPort.crearCobro(new SolicitudCobro(idVenta, null, idMedioPago, monto, oCanal.name()));
		EstadoTransaccion estado = EstadoTransaccion.valueOf(oRespuesta.estado());
		if (estado == EstadoTransaccion.RECHAZADA) {
			throw new ReglaNegocioException("RF103", "La pasarela rechazó el cobro de S/ " + monto + ": " + oRespuesta.mensaje());
		}
		TransaccionPasarela oTransaccion = new TransaccionPasarela(usuarioActual.idSede(), idMedioPago, TipoOperacion.COBRO, oCanal, monto, null,
				usuarioActual.idUsuario());
		if (estado == EstadoTransaccion.APROBADA) {
			oTransaccion.aprobar(oRespuesta.codigoOperacion());
		}
		else {
			oTransaccion.registrarCodigoOperacion(oRespuesta.codigoOperacion());
		}
		repositorioTransaccionPasarela.saveAndFlush(oTransaccion);
		return aDTO(oTransaccion);
	}

	@Override
	@Transactional
	public EnlacePagoDTO generarEnlace(Long idCita, BigDecimal adelantoMonto, Long idMedioPago, Long idSede) {
		if (!usaPasarela(idMedioPago)) {
			throw new ReglaNegocioException("RN42", "El enlace de pago es para Yape, Plin o tarjeta.");
		}
		TransaccionPasarela oTransaccion = new TransaccionPasarela(idSede, idMedioPago, TipoOperacion.COBRO, CanalTransaccionPasarela.LINK,
				adelantoMonto, idCita, usuarioActual.idUsuario());
		RespuestaPasarela oRespuesta = pasarelaPagosPort
			.crearEnlace(new SolicitudCobro(null, idCita, idMedioPago, adelantoMonto, CanalTransaccionPasarela.LINK.name()));
		long vigenciaHoras = parametroApi.valorNumero("LINK_PAGO_VIGENCIA_HORAS").longValue();
		oTransaccion.registrarEnlace(oRespuesta.enlacePago(), LocalDateTime.now().plusHours(vigenciaHoras), oRespuesta.codigoOperacion());
		repositorioTransaccionPasarela.save(oTransaccion);
		return new EnlacePagoDTO(oTransaccion.getIdTransaccion(), oTransaccion.getEstado().name(), oTransaccion.getCanal().name(),
				oTransaccion.getEnlacePago(), oTransaccion.getFechaExpiracion());
	}

	/** Sin sesión: la auditoría queda a nombre de quien generó la transacción. Repetir la notificación no tiene efecto (RN42). */
	@Transactional
	public void confirmar(String codigoOperacion, String estado) {
		TransaccionPasarela oTransaccion = repositorioTransaccionPasarela.findByCodigoOperacion(codigoOperacion)
			.orElseThrow(() -> new ReglaNegocioException("RN42", "No hay una transacción con ese código de operación.", HttpStatus.NOT_FOUND));
		if (oTransaccion.getEstado() != EstadoTransaccion.PENDIENTE) {
			return;
		}
		sesionOracle.iniciar(oTransaccion.getIdUsuario());
		if (EstadoTransaccion.valueOf(estado) == EstadoTransaccion.APROBADA) {
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

	/** LINK no: CK_TRANS_PAS_LINK exige fecha de expiración y el enlace es para los adelantos. */
	private CanalTransaccionPasarela canalDeCobro(String canal) {
		if (canal == null) {
			throw new ReglaNegocioException("RF26", "Un pago digital indica el canal: POS, QR o API.");
		}
		return CanalTransaccionPasarela.valueOf(canal);
	}

	private TransaccionDTO aDTO(TransaccionPasarela oTransaccion) {
		return new TransaccionDTO(oTransaccion.getIdTransaccion(), oTransaccion.getEstado().name(), oTransaccion.getCodigoOperacion());
	}

}
