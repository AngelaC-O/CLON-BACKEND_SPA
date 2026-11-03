package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobantePendiente;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.TotalMedioPago;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentasConsultaApi;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioVenta;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aparte de ServicioVenta: ServicioCaja usa esta API y ServicioVenta usa CajaAbiertaProvider, que formarían un ciclo de beans (DF-48). */
@Service
@RequiredArgsConstructor
class ServicioVentasConsulta implements VentasConsultaApi {

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

}
