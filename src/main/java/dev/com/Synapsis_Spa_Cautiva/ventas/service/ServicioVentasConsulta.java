package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.HistorialComprasProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ProductoCompradoDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobantePendiente;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.TotalMedioPago;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentasConsultaApi;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioVenta;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aparte de ServicioVenta: ServicioCaja usa esta API y ServicioVenta usa CajaAbiertaProvider, que formarían un ciclo de beans (DF-48).
 * [It. 2] También implementa HistorialComprasProvider (SPI de comun) para la ficha de la clienta de agenda (CUS03).
 */
@Service
@RequiredArgsConstructor
class ServicioVentasConsulta implements VentasConsultaApi, HistorialComprasProvider {

	private final RepositorioVenta repositorioVenta;

	@Override
	@Transactional(readOnly = true)
	public List<TotalMedioPago> totalesPorMedioPago(Long idCaja) {
		return repositorioVenta.findEsperadoPorCaja(idCaja).stream()
			.map(fila -> new TotalMedioPago(fila.getIdMedioPago(), fila.getMedioPago(), fila.getEsperado()))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ComprobantePendiente> comprobantesPendientes(Long idSede, LocalDateTime fecha) {
		return repositorioVenta.findComprobantesPendientes(idSede, fecha.toLocalDate()).stream()
			.map(fila -> new ComprobantePendiente(fila.getIdDocumento(), fila.getTipo(), fila.getSerie(), fila.getNumero(), fila.getEstadoSunat(),
					fila.getFechaLimite().toLocalDate()))
			.toList();
	}

	/** [It. 2] CUS03 (RF02): los productos que compró la clienta, de las ventas CERRADAS (V_VENTA_LINEA no trae las anuladas). */
	@Override
	@Transactional(readOnly = true)
	public List<ProductoCompradoDTO> productosComprados(Long idCliente) {
		return repositorioVenta.findProductosComprados(idCliente).stream()
			.map(fila -> new ProductoCompradoDTO(fila.getIdVenta(), fila.getFecha().toLocalDate(), fila.getIdProducto(), fila.getProducto(), fila.getCantidad(),
					fila.getPrecioUnitario(), fila.getSubtotal()))
			.toList();
	}

}
