package dev.com.Synapsis_Spa_Cautiva.caja.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.com.Synapsis_Spa_Cautiva.caja.controller.ConciliacionDTO;
import dev.com.Synapsis_Spa_Cautiva.caja.controller.MapperConciliacion;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.Conciliacion;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoConciliacionItem;
import dev.com.Synapsis_Spa_Cautiva.caja.repository.RepositorioConciliacion;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.FilaLiquidacionDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.ResumenImportacionDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionLiquidadaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobanteSunatDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentaResumenDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentasConsultaApi;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * [It. 2 · CUS47, RF75, RF76, RF105, RN31, RN33] La conciliación del mes: primero la Administradora sube el reporte de liquidación de Izipay
 * (no hay API de liquidación ni job, DF-49), después se genera la del mes, se justifican o excluyen las diferencias y se cierra.
 */
@Service
@RequiredArgsConstructor
public class ServicioConciliacion {

	private final LectorReporteLiquidacion lectorReporteLiquidacion;

	private final PagosApi pagosApi;

	private final VentasConsultaApi ventasConsultaApi;

	private final RepositorioConciliacion repositorioConciliacion;

	private final UsuarioActual usuarioActual;

	private final MapperConciliacion mapperConciliacion;

	/** RF105: lee el reporte y completa comisión y abono de cada cobro por su codigo_operacion; responde cuántas filas casaron y cuáles no. */
	@Transactional
	public ResumenImportacionDTO importarLiquidacion(MultipartFile archivoLiquidacion) {
		List<FilaLiquidacionDTO> filasLiquidacion = lectorReporteLiquidacion.leerReporte(archivoLiquidacion);
		return pagosApi.registrarLiquidacion(filasLiquidacion);
	}

	/**
	 * RF75, RF76, RN31, RN33: una por mes (409). Las ventas CERRADAS del mes (las anuladas no cuentan), las que no tienen comprobante (RF76;
	 * con los triggers, ninguna, y ya vienen en ventasDelMes: se agregan sin repetir), los comprobantes con su nota y las transacciones con su
	 * liquidación; generarItems arma las diferencias (DF-70).
	 */
	@Transactional
	public ConciliacionDTO conciliar(Integer anio, Integer mes) {
		if (anio != null && mes != null && repositorioConciliacion.existsByAnioAndMes(anio, mes)) {
			throw new ReglaNegocioException("RF75", "Ya existe la conciliación de " + mes + "/" + anio + ".", HttpStatus.CONFLICT);
		}
		Conciliacion oConciliacion = new Conciliacion(anio, mes, usuarioActual.idUsuario());
		List<VentaResumenDTO> ventas = new ArrayList<>(ventasConsultaApi.ventasDelMes(anio, mes));
		List<ComprobanteSunatDTO> comprobantes = ventasConsultaApi.comprobantesDelMes(anio, mes);
		List<TransaccionLiquidadaDTO> transacciones = pagosApi.liquidacion(anio, mes);
		Set<Long> yaIncluidas = new HashSet<>(ventas.stream().map(VentaResumenDTO::idVenta).toList());
		ventasConsultaApi.ventasSinComprobante(anio, mes).stream().filter(oVenta -> yaIncluidas.add(oVenta.idVenta())).forEach(ventas::add);
		oConciliacion.generarItems(ventas, comprobantes, transacciones);
		return mapperConciliacion.aDTO(repositorioConciliacion.saveAndFlush(oConciliacion));
	}

	/** RF75, RN31: un ítem SIN_JUSTIFICAR pasa a JUSTIFICADA (con justificación) o a EXCLUIDA; una conciliación CERRADA ya no cambia (422). */
	@Transactional
	public ConciliacionDTO justificarItem(Long idConciliacion, Long idItem, EstadoConciliacionItem estado, String justificacion) {
		Conciliacion oConciliacion = buscar(idConciliacion);
		if (estado == null) {
			throw new ReglaNegocioException("RF75", "Indique si la diferencia queda JUSTIFICADA o EXCLUIDA.");
		}
		oConciliacion.justificarItem(idItem, estado, justificacion);
		return mapperConciliacion.aDTO(repositorioConciliacion.saveAndFlush(oConciliacion));
	}

	/** RF75: solo sin diferencias SIN_JUSTIFICAR (422); queda CERRADA con su fecha (CK_CONCILIACION_CIERRE). */
	@Transactional
	public ConciliacionDTO cerrar(Long idConciliacion) {
		Conciliacion oConciliacion = buscar(idConciliacion);
		oConciliacion.cerrar(LocalDateTime.now());
		return mapperConciliacion.aDTO(repositorioConciliacion.saveAndFlush(oConciliacion));
	}

	private Conciliacion buscar(Long idConciliacion) {
		return repositorioConciliacion.findById(idConciliacion)
			.orElseThrow(() -> new ReglaNegocioException("RF75", "La conciliación " + idConciliacion + " no existe.", HttpStatus.NOT_FOUND));
	}

}
