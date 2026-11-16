package dev.com.Synapsis_Spa_Cautiva.pagos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.SesionOracle;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.AdelantoAprobado;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.CobroPendienteDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.EnlacePagoDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.FilaLiquidacionDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.ResumenImportacionDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionLiquidadaDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.controller.MedioPagoDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.CanalTransaccionPasarela;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.EstadoMedioPago;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.EstadoTransaccion;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.MedioPago;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.TipoOperacion;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.TransaccionPasarela;
import dev.com.Synapsis_Spa_Cautiva.pagos.repository.FilaAprobadoDelDia;
import dev.com.Synapsis_Spa_Cautiva.pagos.repository.RepositorioMedioPago;
import dev.com.Synapsis_Spa_Cautiva.pagos.repository.RepositorioTransaccionPasarela;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioPago implements PagosApi {

	private final PasarelaPagosPort pasarelaPagosPort;

	private final RepositorioTransaccionPasarela repositorioTransaccionPasarela;

	private final RepositorioMedioPago repositorioMedioPago;

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
		oTransaccion.registrarVenta(idVenta);
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
	public Map<Long, String> nombresMediosPago(List<Long> idsMedioPago) {
		List<Long> ids = idsMedioPago.stream().filter(java.util.Objects::nonNull).distinct().toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		return repositorioMedioPago.findAllById(ids).stream().collect(Collectors.toMap(MedioPago::getIdMedioPago, MedioPago::getNombre));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CobroPendienteDTO> cobrosPendientes(Long idVenta) {
		return repositorioTransaccionPasarela.findPendientesDeVenta(idVenta).stream()
			.map(oTransaccion -> new CobroPendienteDTO(oTransaccion.getIdTransaccion(), oTransaccion.getIdMedioPago(), oTransaccion.getMonto(),
					oTransaccion.getCanal().name(), oTransaccion.getEstado().name(), oTransaccion.getEnlacePago(), oTransaccion.getFechaExpiracion()))
			.toList();
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

	/**
	 * [It. 2 · CUS24, RF106, RN44] En el orden del diagrama: el cobro origen, la DEVOLUCION por el mismo medio, canal y monto, la pasarela y
	 * aprobar. REQUIRES_NEW como el cobro: lo que la pasarela devolvió queda aunque la venta haga rollback; una RECHAZADA se guarda y responde 422.
	 * TRG_TRANS_PAS_REGLAS exige la Administradora (-20003) y que no se devuelva más que el cobro (-20077).
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ReglaNegocioException.class)
	public TransaccionDTO devolver(Long idTransaccion, Long idNotaCredito) {
		TransaccionPasarela oTransaccion = buscarTransaccion(idTransaccion);
		if (oTransaccion.getCanal() == CanalTransaccionPasarela.QR) {
			throw new ReglaNegocioException("RN44", "Yape y Plin pagados por QR se devuelven desde la app: registre la devolución manual.");
		}
		exigirSinDevolver(oTransaccion);
		TransaccionPasarela oDevolucion = new TransaccionPasarela(oTransaccion.getIdSede(), oTransaccion.getIdMedioPago(), TipoOperacion.DEVOLUCION,
				oTransaccion.getCanal(), oTransaccion.getMonto(), idTransaccion, idNotaCredito, usuarioActual.idUsuario());
		RespuestaPasarela oRespuesta = pasarelaPagosPort
			.devolver(new SolicitudDevolucion(oTransaccion.getCodigoOperacion(), oTransaccion.getIdMedioPago(), oTransaccion.getMonto()));
		EstadoTransaccion estado = EstadoTransaccion.valueOf(oRespuesta.estado());
		if (estado == EstadoTransaccion.APROBADA) {
			oDevolucion.aprobar(oRespuesta.codigoOperacion());
		}
		else if (estado == EstadoTransaccion.RECHAZADA) {
			oDevolucion.rechazar();
		}
		repositorioTransaccionPasarela.saveAndFlush(oDevolucion);
		if (estado == EstadoTransaccion.RECHAZADA) {
			throw new ReglaNegocioException("RN44", "La pasarela rechazó la devolución de S/ " + oTransaccion.getMonto() + ": " + oRespuesta.mensaje());
		}
		return aDTO(oDevolucion);
	}

	/** [It. 2 · CUS24, RF106, RN44] La DEVOLUCION de un cobro por QR que la Administradora ya hizo desde la app: canal QR, APROBADA, con su número. */
	@Override
	@Transactional
	public TransaccionDTO registrarDevolucionManual(Long idTransaccion, Long idNotaCredito, String codigoOperacion) {
		if (codigoOperacion == null || codigoOperacion.isBlank()) {
			throw new ReglaNegocioException("RN44", "Indique el número de operación de la devolución en la app.");
		}
		TransaccionPasarela oTransaccion = buscarTransaccion(idTransaccion);
		if (oTransaccion.getCanal() != CanalTransaccionPasarela.QR) {
			throw new ReglaNegocioException("RN44", "Solo Yape y Plin pagados por QR se devuelven a mano; la tarjeta se devuelve por la pasarela.");
		}
		exigirSinDevolver(oTransaccion);
		TransaccionPasarela oDevolucion = new TransaccionPasarela(oTransaccion.getIdSede(), oTransaccion.getIdMedioPago(), TipoOperacion.DEVOLUCION,
				CanalTransaccionPasarela.QR, oTransaccion.getMonto(), idTransaccion, idNotaCredito, usuarioActual.idUsuario());
		oDevolucion.aprobar(codigoOperacion.trim());
		repositorioTransaccionPasarela.saveAndFlush(oDevolucion);
		return aDTO(oDevolucion);
	}

	/**
	 * [It. 2 · CUS47, RF105] Por cada fila del reporte, el cobro APROBADO con ese codigo_operacion recibe su comisión y su fecha de abono. Una fila
	 * sin cobro (o de un cobro que no está APROBADO) no detiene la importación: vuelve en codigosSinTransaccion para revisarla.
	 */
	@Override
	@Transactional
	public ResumenImportacionDTO registrarLiquidacion(List<FilaLiquidacionDTO> filasLiquidacion) {
		List<String> sinTransaccion = new ArrayList<>();
		int registradas = 0;
		for (FilaLiquidacionDTO oFila : filasLiquidacion) {
			Optional<TransaccionPasarela> cobro = oFila.codigoOperacion() == null ? Optional.empty()
					: repositorioTransaccionPasarela.findByCodigoOperacion(oFila.codigoOperacion().trim())
						.filter(oTransaccion -> oTransaccion.getEstado() == EstadoTransaccion.APROBADA
								&& oTransaccion.getTipoOperacion() == TipoOperacion.COBRO);
			if (cobro.isEmpty()) {
				sinTransaccion.add(oFila.codigoOperacion());
				continue;
			}
			cobro.get().registrarLiquidacion(oFila.comision(), oFila.fechaAbono());
			repositorioTransaccionPasarela.save(cobro.get());
			registradas++;
		}
		return new ResumenImportacionDTO(filasLiquidacion.size(), registradas, sinTransaccion);
	}

	/** [It. 2 · CUS47, RF105] Las transacciones APROBADAS del mes, con comisión y abono. */
	@Override
	@Transactional(readOnly = true)
	public List<TransaccionLiquidadaDTO> liquidacion(Integer anio, Integer mes) {
		return repositorioTransaccionPasarela.findLiquidadasDelMes(anio, mes).stream()
			.map(fila -> new TransaccionLiquidadaDTO(fila.getIdTransaccion(), fila.getIdVenta(), fila.getIdCita(), fila.getTipoOperacion(),
					fila.getMedioPago(), fila.getMonto(), fila.getComision(), fila.getFechaAbono() == null ? null : fila.getFechaAbono().toLocalDate()))
			.toList();
	}

	/** [It. 2 · CUS62, RF88] Los medios de pago (efectivo, Yape, Plin y tarjeta) con su estado y si usan la pasarela. */
	@Transactional(readOnly = true)
	public List<MedioPagoDTO> listarMediosPago() {
		return repositorioMedioPago.findAll(Sort.by("idMedioPago")).stream().map(ServicioPago::aDTO).toList();
	}

	/** [It. 2 · CUS62, RF88, RN42] Sin estado o sin usaPasarela conserva el actual; el efectivo nunca usa la pasarela (422). */
	@Transactional
	public MedioPagoDTO configurarMedioPago(Long idMedioPago, EstadoMedioPago estado, Boolean usaPasarela) {
		MedioPago oMedioPago = repositorioMedioPago.findById(idMedioPago)
			.orElseThrow(() -> new ReglaNegocioException("RF88", "El medio de pago " + idMedioPago + " no existe.", HttpStatus.NOT_FOUND));
		oMedioPago.configurar(estado == null ? oMedioPago.getEstado() : estado, usaPasarela == null ? oMedioPago.getUsaPasarela() : usaPasarela);
		return aDTO(repositorioMedioPago.saveAndFlush(oMedioPago));
	}

	private static MedioPagoDTO aDTO(MedioPago oMedioPago) {
		return new MedioPagoDTO(oMedioPago.getIdMedioPago(), oMedioPago.getNombre(), oMedioPago.getEstado(), oMedioPago.getUsaPasarela());
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
				oTransaccion.getEnlacePago(), oTransaccion.getCanal().name());
	}

	/**
	 * RN44 antes de llamar a la pasarela: solo un cobro APROBADO y que aún no se devolvió. Oracle lo repite al insertar (-20077), pero entonces la
	 * pasarela ya habría devuelto el dinero otra vez.
	 */
	private void exigirSinDevolver(TransaccionPasarela oTransaccion) {
		if (oTransaccion.getTipoOperacion() != TipoOperacion.COBRO || oTransaccion.getEstado() != EstadoTransaccion.APROBADA) {
			throw new ReglaNegocioException("RN44", "Solo se devuelve un cobro APROBADO; la transacción " + oTransaccion.getIdTransaccion() + " está "
					+ oTransaccion.getEstado() + ".");
		}
		BigDecimal devuelto = repositorioTransaccionPasarela.sumarDevuelto(oTransaccion.getIdTransaccion());
		if (devuelto.add(oTransaccion.getMonto()).compareTo(oTransaccion.getMonto()) > 0) {
			throw new ReglaNegocioException("RN44", "El cobro " + oTransaccion.getIdTransaccion() + " ya tiene una devolución de S/ " + devuelto + ".",
					HttpStatus.CONFLICT);
		}
	}

	private TransaccionPasarela buscarTransaccion(Long idTransaccion) {
		return repositorioTransaccionPasarela.findById(idTransaccion)
			.orElseThrow(() -> new ReglaNegocioException("RN44", "La transacción " + idTransaccion + " no existe.", HttpStatus.NOT_FOUND));
	}

}
