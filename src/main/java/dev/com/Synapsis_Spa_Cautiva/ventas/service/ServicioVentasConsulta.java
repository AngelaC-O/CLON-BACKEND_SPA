package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoDTO;
import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.HistorialComprasProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ProductoCompradoDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobantePendiente;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobanteSunatDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.TotalMedioPago;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentaResumenDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentasConsultaApi;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.FilaVentaMes;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioSolicitudAnulacion;
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
class ServicioVentasConsulta implements VentasConsultaApi, HistorialComprasProvider, AvisoProvider {

	/** Días antes de la fecha límite de envío a SUNAT desde los que se avisa, como ReenvioCpeJob (U2 §6). */
	private static final long DIAS_DE_ALERTA = 1;

	private final RepositorioVenta repositorioVenta;

	private final RepositorioSolicitudAnulacion repositorioSolicitudAnulacion;

	private final RepositorioComprobante repositorioComprobante;

	private final ParametroApi parametroApi;

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

	/** [It. 2 · CUS47, RN33] Las ventas CERRADAS del mes con su boleta o factura. */
	@Override
	@Transactional(readOnly = true)
	public List<VentaResumenDTO> ventasDelMes(Integer anio, Integer mes) {
		return repositorioVenta.findValidasDelMes(anio, mes).stream().map(ServicioVentasConsulta::aResumen).toList();
	}

	/** [It. 2 · CUS47, RF75, RN31] Boletas y facturas del mes con su nota de crédito. */
	@Override
	@Transactional(readOnly = true)
	public List<ComprobanteSunatDTO> comprobantesDelMes(Integer anio, Integer mes) {
		return repositorioComprobante.findDelMes(anio, mes).stream()
			.map(fila -> new ComprobanteSunatDTO(fila.getIdVenta(), fila.getTipo(), fila.getSerie(), fila.getNumero(), fila.getImporte(),
					fila.getEstadoVenta(), fila.getEstadoSunat(), fila.getNcSerie(), fila.getNcNumero(), fila.getNcEstadoSunat()))
			.toList();
	}

	/** [It. 2 · CUS47, RF76] Ventas CERRADAS del mes sin comprobante. */
	@Override
	@Transactional(readOnly = true)
	public List<VentaResumenDTO> ventasSinComprobante(Integer anio, Integer mes) {
		return repositorioVenta.findSinComprobanteDelMes(anio, mes).stream().map(ServicioVentasConsulta::aResumen).toList();
	}

	private static VentaResumenDTO aResumen(FilaVentaMes fila) {
		return new VentaResumenDTO(fila.getIdVenta(), fila.getIdSede(), fila.getFecha() == null ? null : fila.getFecha().toLocalDate(),
				fila.getTipoComprobante(), fila.getSerie(), fila.getNumero(), fila.getImporte());
	}

	/**
	 * [It. 2 · CUS50] Avisos de ventas, solo si hay algo: las solicitudes de anulación PENDIENTES de la sede (bandeja de CUS23, VENTAS:EDITAR) y
	 * los comprobantes y notas PENDIENTES de envío a SUNAT a un día o menos de su fecha límite (V_CPE_PENDIENTES, CPE_PLAZO_ENVIO_DIAS, RN46),
	 * el mismo criterio con el que ReenvioCpeJob alerta en el log.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<AvisoDTO> avisos(Long idSede) {
		List<AvisoDTO> avisos = new ArrayList<>();
		int solicitudes = repositorioSolicitudAnulacion.findPendientes(idSede).size();
		if (solicitudes > 0) {
			avisos.add(new AvisoDTO("ANULACION_PENDIENTE",
					solicitudes + (solicitudes == 1 ? " solicitud de anulación pendiente" : " solicitudes de anulación pendientes"), solicitudes, "VENTAS:EDITAR",
					"/ventas/anulaciones"));
		}
		long porVencer = repositorioComprobante.contarPendientesPorVencer(idSede, LocalDate.now().plusDays(DIAS_DE_ALERTA));
		if (porVencer > 0) {
			int plazo = parametroApi.valorNumero("CPE_PLAZO_ENVIO_DIAS").intValue();
			avisos.add(new AvisoDTO("CPE_POR_VENCER", porVencer + (porVencer == 1 ? " comprobante pendiente" : " comprobantes pendientes")
					+ " de envío a SUNAT cerca del plazo de " + plazo + " días", (int) porVencer, "VENTAS:VER", "/ventas/comprobantes"));
		}
		return avisos;
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
