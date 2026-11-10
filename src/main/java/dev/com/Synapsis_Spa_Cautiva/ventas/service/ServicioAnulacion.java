package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.DevolucionDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.MapperVenta;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.SolicitudAnulacionDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoVenta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.FormaDevolucion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.NotaCredito;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.SolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaPago;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioSolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioVenta;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [It. 2] La anulación de la Iteración 2 (RN32): la recepcionista la pide (CUS22), la Administradora la aprueba o la rechaza en su bandeja (CUS23)
 * y, si la clienta eligió el medio original, devuelve los pagos digitales (CUS24). Lo que Oracle hace al anular (TRG_VENTA_EFECTOS →
 * PKG_VENTA.al_anular: atención FINALIZADA, paquete ANULADO, abonos de saldo a favor y reingreso de productos) no se repite aquí.
 */
@Service
@RequiredArgsConstructor
public class ServicioAnulacion {

	private final RepositorioVenta repositorioVenta;

	private final RepositorioSolicitudAnulacion repositorioSolicitudAnulacion;

	private final ServicioFacturacionElectronica servicioFacturacionElectronica;

	private final PagosApi pagosApi;

	private final UsuarioActual usuarioActual;

	private final MapperVenta mapperVenta;

	/**
	 * CUS22 (RF32, RN32, RF106): la solicitud nace PENDIENTE; quién y cuándo los pone Oracle. La venta no cambia. Una segunda PENDIENTE de la
	 * misma venta es 409 (UQ_SOL_ANUL_PENDIENTE): saveAndFlush la detecta aquí y no al confirmar.
	 */
	@Transactional
	public SolicitudAnulacionDTO solicitarAnulacion(Long idVenta, SolicitudAnulacionDTO oSolicitudAnulacionDTO) {
		Venta oVenta = buscarVenta(idVenta);
		validarAnulable(oVenta);
		if (oSolicitudAnulacionDTO.formaDevolucion() == FormaDevolucion.SALDO_FAVOR && oVenta.getIdCliente() == null) {
			throw new ReglaNegocioException("RN44", "Un cliente de paso no tiene ficha: la devolución va por el medio original.");
		}
		SolicitudAnulacion oSolicitud = new SolicitudAnulacion(idVenta, oSolicitudAnulacionDTO.motivo(), oSolicitudAnulacionDTO.codigoMotivo(),
				oSolicitudAnulacionDTO.formaDevolucion(), usuarioActual.idUsuario());
		repositorioSolicitudAnulacion.saveAndFlush(oSolicitud);
		return mapperVenta.aDTO(oSolicitud, oVenta);
	}

	/** RN12: solo se pide anular una venta CERRADA (Oracle lo repite con -20101). */
	public void validarAnulable(Venta oVenta) {
		if (oVenta.getEstado() != EstadoVenta.CERRADA) {
			throw new ReglaNegocioException("RN12", "Solo se anula una venta CERRADA; esta está " + oVenta.getEstado() + ".");
		}
	}

	/** CUS23 (RF33): la bandeja de la sede. Sin estado, las PENDIENTES (findPendientes del diagrama). */
	@Transactional(readOnly = true)
	public List<SolicitudAnulacionDTO> listarPendientes(Long idSede, EstadoSolicitudAnulacion estado) {
		List<SolicitudAnulacion> solicitudes = estado == null || estado == EstadoSolicitudAnulacion.PENDIENTE
				? repositorioSolicitudAnulacion.findPendientes(idSede)
				: repositorioSolicitudAnulacion.findPorEstado(idSede, estado);
		return solicitudes.stream().map(oSolicitud -> mapperVenta.aDTO(oSolicitud, buscarVenta(oSolicitud.getIdVenta()))).toList();
	}

	/**
	 * CUS23 (RF32, RF33, RN12, RN32): en una transacción, la nota de crédito (salvo que SUNAT haya rechazado el comprobante), la venta ANULADA y la
	 * solicitud APROBADA con su id_nota_credito. Una solicitud resuelta es 409 antes de emitir nada.
	 */
	@Transactional
	public SolicitudAnulacionDTO aprobar(Long idSolicitud) {
		SolicitudAnulacion oSolicitud = buscarSolicitud(idSolicitud);
		oSolicitud.exigirPendiente();
		Venta oVenta = buscarVenta(oSolicitud.getIdVenta());
		Long idNotaCredito = null;
		if (!comprobanteRechazado(oVenta)) {
			idNotaCredito = servicioFacturacionElectronica.emitirNotaCredito(oVenta.getIdVenta(), oSolicitud).getIdComprobante();
		}
		Long idUsuarioAutoriza = usuarioActual.idUsuario();
		oVenta.anular(idUsuarioAutoriza);
		repositorioVenta.saveAndFlush(oVenta);
		oSolicitud.aprobar(idUsuarioAutoriza, idNotaCredito);
		repositorioSolicitudAnulacion.saveAndFlush(oSolicitud);
		return mapperVenta.aDTO(oSolicitud, oVenta);
	}

	/** CUS23 (RN32): con su motivo (422 sin él); la venta sigue CERRADA. */
	@Transactional
	public SolicitudAnulacionDTO rechazar(Long idSolicitud, String motivoRechazo) {
		SolicitudAnulacion oSolicitud = buscarSolicitud(idSolicitud);
		oSolicitud.rechazar(usuarioActual.idUsuario(), motivoRechazo);
		repositorioSolicitudAnulacion.saveAndFlush(oSolicitud);
		return mapperVenta.aDTO(oSolicitud, buscarVenta(oSolicitud.getIdVenta()));
	}

	/**
	 * CUS24 (RF106, RN44): devuelve por la pasarela cada pago con tarjeta de la venta ANULADA con nota MEDIO_ORIGINAL (PagosApi.devolver). Yape y
	 * Plin por QR van por registrarDevolucionManual y el efectivo sale de la caja. No escribe SALDO_FAVOR_MOV.
	 */
	@Transactional
	public DevolucionDTO devolverPago(Long idVenta, FormaDevolucion formaDevolucion) {
		Venta oVenta = buscarVenta(idVenta);
		NotaCredito oNotaCredito = notaDeMedioOriginal(oVenta, formaDevolucion);
		List<Long> porPasarela = oVenta.getPagos().stream()
			.map(VentaPago::getIdTransaccion)
			.filter(Objects::nonNull)
			.filter(idTransaccion -> !pagosApi.consultarTransaccion(idTransaccion).porQr())
			.toList();
		if (porPasarela.isEmpty()) {
			throw new ReglaNegocioException("RN44", "La venta no tiene pagos con tarjeta que devolver por la pasarela.");
		}
		List<TransaccionDTO> devoluciones = new ArrayList<>();
		for (Long idTransaccion : porPasarela) {
			devoluciones.add(pagosApi.devolver(idTransaccion, oNotaCredito.getIdComprobante()));
		}
		return new DevolucionDTO(idVenta, formaDevolucion, null, null, oNotaCredito.getIdComprobante(), devoluciones);
	}

	/** CUS24 (RF106, RN44): Yape o Plin pagados por QR que la Administradora ya devolvió desde la app, con su número de operación. */
	@Transactional
	public DevolucionDTO registrarDevolucionManual(Long idVenta, Long idTransaccion, String codigoOperacion) {
		Venta oVenta = buscarVenta(idVenta);
		NotaCredito oNotaCredito = notaDeMedioOriginal(oVenta, FormaDevolucion.MEDIO_ORIGINAL);
		boolean esDeLaVenta = oVenta.getPagos().stream().anyMatch(oPago -> Objects.equals(oPago.getIdTransaccion(), idTransaccion));
		if (idTransaccion == null || !esDeLaVenta) {
			throw new ReglaNegocioException("RN44", "La transacción " + idTransaccion + " no es un pago de la venta " + idVenta + ".");
		}
		TransaccionDTO oDevolucion = pagosApi.registrarDevolucionManual(idTransaccion, oNotaCredito.getIdComprobante(), codigoOperacion);
		return new DevolucionDTO(idVenta, FormaDevolucion.MEDIO_ORIGINAL, idTransaccion, codigoOperacion, oNotaCredito.getIdComprobante(),
				List.of(oDevolucion));
	}

	/** RN44: solo una venta ANULADA con su nota de crédito de forma MEDIO_ORIGINAL; con SALDO_FAVOR ya abonó Oracle al anular. */
	private static NotaCredito notaDeMedioOriginal(Venta oVenta, FormaDevolucion formaDevolucion) {
		if (formaDevolucion != FormaDevolucion.MEDIO_ORIGINAL) {
			throw new ReglaNegocioException("RN44", "Por aquí solo se devuelve por el medio original; el saldo a favor lo abona Oracle al anular.");
		}
		if (oVenta.getEstado() != EstadoVenta.ANULADA) {
			throw new ReglaNegocioException("RN44", "Solo se devuelve el pago de una venta ANULADA; esta está " + oVenta.getEstado() + ".");
		}
		NotaCredito oNotaCredito = oVenta.getComprobantes().stream()
			.filter(NotaCredito.class::isInstance)
			.map(NotaCredito.class::cast)
			.findFirst()
			.orElseThrow(() -> new ReglaNegocioException("RN44", "La venta se anuló sin nota de crédito: no hay devolución que registrar."));
		if (oNotaCredito.getFormaDevolucion() != FormaDevolucion.MEDIO_ORIGINAL) {
			throw new ReglaNegocioException("RN44", "La clienta eligió saldo a favor: Oracle ya lo abonó al anular la venta.");
		}
		return oNotaCredito;
	}

	/** Un comprobante RECHAZADO por SUNAT no tiene validez: la venta se anula sin nota de crédito (PKG_VENTA.validar_anulacion). */
	private static boolean comprobanteRechazado(Venta oVenta) {
		return oVenta.getComprobantes().stream()
			.filter(oComprobante -> oComprobante.getTipo() == TipoComprobante.BOLETA || oComprobante.getTipo() == TipoComprobante.FACTURA)
			.map(Comprobante::getEstadoSunat)
			.anyMatch(EstadoSunat.RECHAZADO::equals);
	}

	private Venta buscarVenta(Long idVenta) {
		return repositorioVenta.findById(idVenta)
			.orElseThrow(() -> new ReglaNegocioException("RF32", "La venta " + idVenta + " no existe.", HttpStatus.NOT_FOUND));
	}

	private SolicitudAnulacion buscarSolicitud(Long idSolicitud) {
		return repositorioSolicitudAnulacion.findById(idSolicitud)
			.orElseThrow(() -> new ReglaNegocioException("RN32", "La solicitud de anulación " + idSolicitud + " no existe.", HttpStatus.NOT_FOUND));
	}

}
