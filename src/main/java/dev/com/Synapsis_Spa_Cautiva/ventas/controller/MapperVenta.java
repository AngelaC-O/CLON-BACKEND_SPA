package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaDetalle;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaPago;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapperVenta {

	DetalleVentaDTO aDTO(VentaDetalle oDetalle);

	PagoVentaDTO aDTO(VentaPago oPago);

	ComprobanteDTO aDTO(Comprobante oComprobante);

	List<DetalleVentaDTO> detallesADTO(List<VentaDetalle> detalles);

	List<PagoVentaDTO> pagosADTO(List<VentaPago> pagos);

	default VentaDTO aDTO(Venta oVenta, int cobrosPendientes) {
		BigDecimal pagado = oVenta.getPagos().stream().map(VentaPago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
		ComprobanteDTO comprobante = oVenta.getComprobantes().stream().findFirst().map(this::aDTO).orElse(null);
		return new VentaDTO(oVenta.getIdVenta(), oVenta.getIdSede(), oVenta.getIdCaja(), oVenta.getIdCliente(), oVenta.getEstado(),
				detallesADTO(oVenta.getDetalles()), oVenta.getSubtotal(), oVenta.getIgv(), oVenta.getDescuento(), oVenta.getAdelantoAplicado(),
				oVenta.getSaldoFavorAplicado(), oVenta.getTotal(), pagosADTO(oVenta.getPagos()), pagado, oVenta.getTotal().subtract(pagado), cobrosPendientes,
				comprobante);
	}

}
