package dev.com.Synapsis_Spa_Cautiva.caja.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.caja.controller.ArqueoDTO;
import dev.com.Synapsis_Spa_Cautiva.caja.controller.CajaDTO;
import dev.com.Synapsis_Spa_Cautiva.caja.controller.EsperadoDTO;
import dev.com.Synapsis_Spa_Cautiva.caja.controller.EsperadoMedioDTO;
import dev.com.Synapsis_Spa_Cautiva.caja.controller.MapperCaja;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.Caja;
import dev.com.Synapsis_Spa_Cautiva.caja.repository.RepositorioCaja;
import dev.com.Synapsis_Spa_Cautiva.comun.api.CajaAbiertaProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobantePendiente;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.TotalMedioPago;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentasConsultaApi;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioCaja implements CajaAbiertaProvider {

	private final RepositorioCaja repositorioCaja;

	private final VentasConsultaApi ventasConsultaApi;

	private final PagosApi pagosApi;

	private final UsuarioActual usuarioActual;

	private final MapperCaja mapperCaja;

	/** [It. 2] CUS44: la caja ABIERTA de la sede, para la cabecera y para retomar el cierre (Duda 10 del front-end). Vacío si no hay. */
	@Transactional(readOnly = true)
	public Optional<CajaDTO> obtenerAbierta(Long idSede) {
		return repositorioCaja.findAbiertaBySede(idSede).map(mapperCaja::aDTO);
	}

	@Transactional
	public CajaDTO abrir(Long idSede, BigDecimal montoInicial) {
		if (repositorioCaja.existsAbiertaBySede(idSede)) {
			throw new ReglaNegocioException("RN30", "La sede ya tiene una caja abierta.", HttpStatus.CONFLICT);
		}
		Caja oCaja = new Caja(idSede, usuarioActual.idUsuario(), montoInicial);
		return mapperCaja.aDTO(repositorioCaja.save(oCaja));
	}

	@Transactional(readOnly = true)
	public EsperadoDTO calcularEsperado(Long idCaja) {
		Caja oCaja = buscar(idCaja);
		List<TotalMedioPago> totales = ventasConsultaApi.totalesPorMedioPago(idCaja);
		Map<Long, BigDecimal> aprobados = pagosApi.aprobadosDelDia(oCaja.getIdSede(), oCaja.getFechaApertura());
		List<ComprobantePendiente> pendientes = ventasConsultaApi.comprobantesPendientes(oCaja.getIdSede(), LocalDateTime.now());
		List<EsperadoMedioDTO> medios = totales.stream()
			.map(total -> new EsperadoMedioDTO(total.idMedioPago(), total.medioPago(), total.esperado(),
					aprobados.getOrDefault(total.idMedioPago(), BigDecimal.ZERO)))
			.toList();
		return new EsperadoDTO(idCaja, oCaja.getEstado(), oCaja.getMontoInicial(), medios, pendientes);
	}

	/** Un flush por grupo: el arqueo se calcula con los egresos ya guardados y cerrar lee la diferencia que puso Oracle (DF-10, DF-13). */
	@Transactional
	public CajaDTO cerrar(Long idCaja, ArqueoDTO oArqueoDTO) {
		Caja oCaja = buscar(idCaja);
		Long idUsuario = usuarioActual.idUsuario();
		if (oArqueoDTO.egresos() != null) {
			oArqueoDTO.egresos().forEach(egreso -> oCaja.registrarEgreso(egreso.concepto(), egreso.monto(), idUsuario));
			repositorioCaja.saveAndFlush(oCaja);
		}
		oArqueoDTO.arqueos().forEach(arqueo -> oCaja.registrarArqueo(arqueo.idMedioPago(), arqueo.montoContado(), arqueo.observacion()));
		repositorioCaja.saveAndFlush(oCaja);
		oCaja.cerrar(idUsuario);
		return mapperCaja.aDTO(repositorioCaja.saveAndFlush(oCaja));
	}

	@Override
	@Transactional
	public Long obtenerCajaAbierta(Long idSede) {
		return buscarCajaAbierta(idSede).orElseThrow(() -> new ReglaNegocioException("RN30", "La sede no tiene una caja abierta."));
	}

	@Override
	@Transactional
	public Optional<Long> buscarCajaAbierta(Long idSede) {
		return repositorioCaja.findAbiertaBySede(idSede).map(Caja::getIdCaja);
	}

	private Caja buscar(Long idCaja) {
		return repositorioCaja.findById(idCaja)
			.orElseThrow(() -> new ReglaNegocioException("RN30", "La caja " + idCaja + " no existe.", HttpStatus.NOT_FOUND));
	}

}
