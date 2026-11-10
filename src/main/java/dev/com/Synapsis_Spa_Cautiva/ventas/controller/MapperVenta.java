package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.SolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaDetalle;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaPago;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapperVenta {

	DetalleVentaDTO aDTO(VentaDetalle oDetalle);

	@Mapping(target = "estadoTransaccion", ignore = true)
	PagoVentaDTO aDTO(VentaPago oPago);

	ComprobanteDTO aDTO(Comprobante oComprobante);

	List<DetalleVentaDTO> detallesADTO(List<VentaDetalle> detalles);

	List<PagoVentaDTO> pagosADTO(List<VentaPago> pagos);

	/** [It. 2 · CUS22, CUS23] La solicitud con el cliente, el total y el comprobante de su venta, para la bandeja. */
	default SolicitudAnulacionDTO aDTO(SolicitudAnulacion oSolicitud, Venta oVenta) {
		String comprobante = oVenta.getComprobantes().stream()
			.filter(oComprobante -> oComprobante.getTipo() == TipoComprobante.BOLETA || oComprobante.getTipo() == TipoComprobante.FACTURA)
			.findFirst()
			.map(oComprobante -> oComprobante.getSerie() + "-" + oComprobante.getNumero())
			.orElse(null);
		return new SolicitudAnulacionDTO(oSolicitud.getIdSolicitud(), oSolicitud.getIdVenta(), oSolicitud.getMotivo(), oSolicitud.getCodigoMotivo(),
				oSolicitud.getFormaDevolucion(), oSolicitud.getEstado(), oSolicitud.getIdUsuarioSolicita(), oSolicitud.getFechaSolicitud(),
				oSolicitud.getIdUsuarioResuelve(), oSolicitud.getFechaResolucion(), oSolicitud.getMotivoRechazo(), oSolicitud.getIdNotaCredito(),
				oVenta.getIdCliente(), oVenta.getTotal(), comprobante);
	}

	default VentaDTO aDTO(Venta oVenta) {
		return aDTO(oVenta, List.of());
	}

	default VentaDTO aDTO(Venta oVenta, List<CobroPendienteDTO> cobrosPendientes) {
		BigDecimal pagado = oVenta.getPagos().stream().map(VentaPago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
		ComprobanteDTO comprobante = oVenta.getComprobantes().stream().findFirst().map(this::aDTO).orElse(null);
		return new VentaDTO(oVenta.getIdVenta(), oVenta.getIdSede(), oVenta.getIdCaja(), oVenta.getIdCliente(), oVenta.getEstado(),
				detallesADTO(oVenta.getDetalles()), oVenta.getSubtotal(), oVenta.getIgv(), oVenta.getDescuento(), oVenta.getAdelantoAplicado(),
				oVenta.getSaldoFavorAplicado(), oVenta.getTotal(), pagosADTO(oVenta.getPagos()), pagado, oVenta.getTotal().subtract(pagado), cobrosPendientes,
				comprobante);
	}

}
