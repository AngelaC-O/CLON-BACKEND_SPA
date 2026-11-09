package dev.com.Synapsis_Spa_Cautiva.catalogo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosVariante;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.AreaDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.InsumoDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.MapperCatalogo;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.ServicioDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.VarianteDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Area;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoPromocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.LineaNegocio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Promocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Servicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoDescuentoPromocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.VarianteServicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioArea;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioServicio;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioCatalogo implements CatalogoApi {

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final RepositorioArea repositorioArea;

	private final RepositorioServicio repositorioServicio;

	private final MapperCatalogo mapperCatalogo;

	@Transactional(readOnly = true)
	public List<AreaDTO> listarAreas() {
		return repositorioArea.findAll().stream().map(mapperCatalogo::aDTO).toList();
	}

	@Transactional
	public AreaDTO registrarArea(LineaNegocio linea, String nombreArea) {
		return mapperCatalogo.aDTO(repositorioArea.save(new Area(linea, nombreArea)));
	}

	/** definirInsumo pide el idVariante, que no existe hasta guardar: por eso hay un flush antes de los insumos. */
	@Transactional
	public ServicioDTO registrarServicio(ServicioDTO oServicioDTO) {
		Servicio oServicio = new Servicio(oServicioDTO.idArea(), oServicioDTO.nombre(), oServicioDTO.duracionRefMin(), oServicioDTO.tipoRecurso());
		oServicioDTO.variantes().forEach(variante -> oServicio.agregarVariante(variante.nombreVariante(), variante.precio()));
		repositorioServicio.saveAndFlush(oServicio);
		List<VarianteServicio> guardadas = oServicio.getVariantes();
		for (int i = 0; i < guardadas.size(); i++) {
			definirInsumos(oServicio, guardadas.get(i).getIdVariante(), oServicioDTO.variantes().get(i));
		}
		return mapperCatalogo.aDTO(repositorioServicio.saveAndFlush(oServicio));
	}

	@Override
	@Transactional(readOnly = true)
	public DatosVariante obtenerVariante(Long idVariante) {
		Servicio oServicio = repositorioServicio.findByVariante(idVariante)
			.orElseThrow(() -> new ReglaNegocioException("RN40", "La variante " + idVariante + " no existe.", HttpStatus.NOT_FOUND));
		return new DatosVariante(idVariante, oServicio.getIdServicio(), oServicio.getIdArea(), oServicio.getDuracionRefMin(),
				oServicio.getTipoRecurso().name(), oServicio.getNombre());
	}

	@Override
	@Transactional(readOnly = true)
	public List<DatosVariante> obtenerVariantes(List<Long> idsVariante) {
		return idsVariante.stream().distinct().map(this::obtenerVariante).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal precioVigente(Long idVariante, Long idPaquete, Long idPromocion) {
		if (idVariante != null) {
			BigDecimal precio = repositorioServicio.findVariante(idVariante)
				.orElseThrow(() -> new ReglaNegocioException("RN40", "La variante " + idVariante + " no existe.", HttpStatus.NOT_FOUND))
				.getPrecio();
			return idPromocion == null ? precio : conPromocion(precio, idPromocion);
		}
		if (idPaquete != null) {
			return repositorioServicio.findPaquete(idPaquete)
				.orElseThrow(() -> new ReglaNegocioException("RF31", "El paquete " + idPaquete + " no existe.", HttpStatus.NOT_FOUND))
				.getPrecio();
		}
		throw new ReglaNegocioException("RF31", "Indique la variante o el paquete.");
	}

	/** Mismo cálculo que TRG_VENTA_DETALLE_REGLAS (RN25). */
	private BigDecimal conPromocion(BigDecimal precio, Long idPromocion) {
		Promocion oPromocion = repositorioServicio.findPromocion(idPromocion)
			.orElseThrow(() -> new ReglaNegocioException("RN25", "La promoción " + idPromocion + " no existe.", HttpStatus.NOT_FOUND));
		LocalDate hoy = LocalDate.now();
		if (oPromocion.getEstado() != EstadoPromocion.ACTIVA || hoy.isBefore(oPromocion.getFechaInicio()) || hoy.isAfter(oPromocion.getFechaFin())) {
			throw new ReglaNegocioException("RN25", "La promoción " + oPromocion.getNombre() + " no está vigente.");
		}
		BigDecimal descuento = oPromocion.getTipoDescuento() == TipoDescuentoPromocion.PORCENTAJE
				? precio.multiply(oPromocion.getValor()).divide(CIEN, 2, RoundingMode.HALF_UP)
				: oPromocion.getValor().min(precio);
		return precio.subtract(descuento);
	}

	private void definirInsumos(Servicio oServicio, Long idVariante, VarianteDTO oVarianteDTO) {
		if (oVarianteDTO.insumos() == null) {
			return;
		}
		for (InsumoDTO insumo : oVarianteDTO.insumos()) {
			oServicio.definirInsumo(idVariante, insumo.idProducto(), insumo.cantidadEstimada());
		}
	}

}
