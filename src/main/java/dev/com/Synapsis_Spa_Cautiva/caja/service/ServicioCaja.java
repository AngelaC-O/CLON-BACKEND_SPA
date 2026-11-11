package dev.com.Synapsis_Spa_Cautiva.caja.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoDTO;
import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoProvider;
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
public class ServicioCaja implements CajaAbiertaProvider, AvisoProvider {

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

	/** [It. 2 · CUS46, RF67, RN30] Cierres CERRADOS de la sede con diferencia en algún medio, con la observación del arqueo, aún sin aprobar. */
	@Transactional(readOnly = true)
	public List<CajaDTO> listarConDiferencia(Long idSede) {
		return repositorioCaja.findCerradasConDiferencia(idSede).stream().map(mapperCaja::aDTO).toList();
	}

	/** [It. 2 · CUS46, RF67] La caja sigue CERRADA y queda revisada; quién y cuándo los vuelve a poner TRG_CAJA_REGLAS. */
	@Transactional
	public CajaDTO aprobarCierre(Long idCaja) {
		Caja oCaja = buscar(idCaja);
		oCaja.aprobarRevision(usuarioActual.idUsuario(), LocalDateTime.now());
		return mapperCaja.aDTO(repositorioCaja.saveAndFlush(oCaja));
	}

	/**
	 * [It. 2 · CUS46, RF66] Las cajas abiertas hoy en la sede, de la última a la primera. El diagrama devuelve una (findDelDia, la última); se
	 * devuelven todas las del día, porque puede haber más de una (DF-122).
	 */
	@Transactional(readOnly = true)
	public List<CajaDTO> obtenerDelDia(Long idSede) {
		LocalDateTime hoy = LocalDate.now().atStartOfDay();
		return repositorioCaja.findAperturasEntre(idSede, hoy, hoy.plusDays(1)).stream().map(mapperCaja::aDTO).toList();
	}

	/**
	 * [It. 2 · CUS46, RF66, RN30] La caja CERRADA del día vuelve a ABIERTA con su motivo, para la venta tardía; la recepcionista la vuelve a arquear
	 * y cerrar (CUS45). TRG_CAJA_REGLAS pone quién y cuándo y limpia el cierre y la revisión. Otra caja ABIERTA en la sede es 409
	 * (UQ_CAJA_ABIERTA_SEDE): se valida antes, y Oracle lo repite.
	 */
	@Transactional
	public CajaDTO reabrir(Long idCaja, String motivoReapertura) {
		Caja oCaja = buscar(idCaja);
		// Antes de cambiar la caja: con la caja ya ABIERTA en memoria, el flush automático de la consulta la encontraría a ella misma
		boolean otraAbierta = repositorioCaja.findAbiertaBySede(oCaja.getIdSede()).filter(abierta -> !abierta.getIdCaja().equals(idCaja)).isPresent();
		oCaja.reabrir(usuarioActual.idUsuario(), motivoReapertura);
		if (otraAbierta) {
			throw new ReglaNegocioException("RN30", "La sede ya tiene una caja ABIERTA: ciérrela antes de reabrir otra.", HttpStatus.CONFLICT);
		}
		return mapperCaja.aDTO(repositorioCaja.saveAndFlush(oCaja));
	}

	/** [It. 2 · CUS50] «N cierres con diferencia por aprobar», solo si hay (CONCILIACION:VER: la Administradora). */
	@Override
	@Transactional(readOnly = true)
	public List<AvisoDTO> avisos(Long idSede) {
		int porAprobar = repositorioCaja.findCerradasConDiferencia(idSede).size();
		if (porAprobar == 0) {
			return List.of();
		}
		return List.of(new AvisoDTO("CIERRE_POR_APROBAR", porAprobar + (porAprobar == 1 ? " cierre con diferencia por aprobar" : " cierres con diferencia por aprobar"),
				porAprobar, "CONCILIACION:VER", "/caja/revision"));
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
