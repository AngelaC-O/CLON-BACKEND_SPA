package dev.com.Synapsis_Spa_Cautiva.catalogo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosVariante;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.InsumoEstimadoDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.AreaDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.InsumoDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.MapperCatalogo;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.PaqueteDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.PromocionDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.ServicioDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.ServicioIncluidoDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.controller.VarianteDTO;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Area;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoPromocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoServicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.LineaNegocio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Paquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Promocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Servicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoDescuentoPromocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.VarianteServicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioArea;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioPromocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioServicio;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioCatalogo implements CatalogoApi {

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final RepositorioArea repositorioArea;

	private final RepositorioServicio repositorioServicio;

	private final RepositorioPaquete repositorioPaquete;

	private final RepositorioPromocion repositorioPromocion;

	private final UsuarioActual usuarioActual;

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

	/** [It. 2 · CUS58, RF83] Servicios con sus variantes, precios e insumos; cada filtro es opcional. Lo usan CUS05, CUS06, CUS13 y CUS21. */
	@Transactional(readOnly = true)
	public List<ServicioDTO> listarServicios(Long idArea, EstadoServicio estado) {
		return repositorioServicio.findByAreaAndEstado(idArea, estado).stream().map(mapperCatalogo::aDTO).toList();
	}

	/**
	 * [It. 2 · CUS58, RF83] Cambia nombre, duración, tipo de recurso y estado (desactivar = INACTIVO, no se borra) y vuelve a definir los
	 * insumos de cada variante que los trae (RN54). El área, el nombre y el precio de las variantes no cambian aquí: el precio es cambiarPrecio.
	 * Sin estado, conserva el actual.
	 */
	@Transactional
	public ServicioDTO actualizarServicio(Long idServicio, ServicioDTO oServicioDTO) {
		Servicio oServicio = buscarServicio(idServicio);
		oServicio.actualizarDatos(oServicioDTO.nombre(), oServicioDTO.duracionRefMin(), oServicioDTO.tipoRecurso(),
				oServicioDTO.estado() == null ? oServicio.getEstado() : oServicioDTO.estado());
		for (VarianteDTO oVarianteDTO : oServicioDTO.variantes()) {
			if (oVarianteDTO.idVariante() != null) {
				definirInsumos(oServicio, oVarianteDTO.idVariante(), oVarianteDTO);
			}
		}
		return mapperCatalogo.aDTO(repositorioServicio.saveAndFlush(oServicio));
	}

	/**
	 * [It. 2 · CUS58, RF83, RN25] Solo la Administradora (CATALOGO:EDITAR no lo tiene otro rol). El precio anterior y el nuevo los guarda
	 * TRG_AUD_VARIANTE_SERVICIO en AUDITORIA: no hay auditoría en Java.
	 */
	@Transactional
	public ServicioDTO cambiarPrecio(Long idServicio, Long idVariante, BigDecimal precioNuevo) {
		Servicio oServicio = buscarServicio(idServicio);
		oServicio.cambiarPrecio(idVariante, precioNuevo);
		return mapperCatalogo.aDTO(repositorioServicio.saveAndFlush(oServicio));
	}

	/** [It. 2 · CUS59, RF84] Paquetes o membresías con sus servicios; sin tipo, todos. Lo usan CUS18 y CUS21. */
	@Transactional(readOnly = true)
	public List<PaqueteDTO> listarPaquetes(TipoPaquete tipo) {
		List<PaqueteDTO> paquetes = repositorioPaquete.findByTipo(tipo).stream().map(mapperCatalogo::aDTO).toList();
		Map<Long, String> nombres = nombresServicios(paquetes.stream().flatMap(oPaqueteDTO -> oPaqueteDTO.idsServicio().stream()).toList());
		return paquetes.stream().map(oPaqueteDTO -> oPaqueteDTO.conServicios(incluidos(oPaqueteDTO.idsServicio(), nombres))).toList();
	}

	/**
	 * [It. 2 · CUS59, RF84] El paquete con sus servicios en PAQUETE_SERVICIO: con esto CUS11 encuentra el paquete de la clienta
	 * (RepositorioClientePaquete.findActivoPorCliente) y descuenta la sesión (RN36). Solo la Administradora (TRG_ADM_PAQUETE).
	 */
	@Transactional
	public PaqueteDTO registrarPaquete(PaqueteDTO oPaqueteDTO) {
		Paquete oPaquete = new Paquete(oPaqueteDTO.nombre(), oPaqueteDTO.tipo(), oPaqueteDTO.numSesiones(), oPaqueteDTO.precio(),
				oPaqueteDTO.vigenciaDias());
		if (oPaqueteDTO.idsServicio() != null) {
			oPaqueteDTO.idsServicio().forEach(oPaquete::incluirServicio);
		}
		PaqueteDTO oPaqueteGuardado = mapperCatalogo.aDTO(repositorioPaquete.saveAndFlush(oPaquete));
		return oPaqueteGuardado.conServicios(incluidos(oPaqueteGuardado.idsServicio(), nombresServicios(oPaqueteGuardado.idsServicio())));
	}

	/** [It. 2 · CUS60, RF84] Con vigentes = true, solo las ACTIVAS cuya vigencia incluye hoy; si no, todas. */
	@Transactional(readOnly = true)
	public List<PromocionDTO> listarPromociones(boolean vigentes) {
		List<Promocion> promociones = vigentes ? repositorioPromocion.findVigentes(LocalDate.now())
				: repositorioPromocion.findAll(Sort.by("fechaInicio").descending());
		List<PromocionDTO> lista = promociones.stream().map(mapperCatalogo::aDTO).toList();
		Map<Long, String> nombres = nombresServicios(lista.stream().flatMap(oPromocionDTO -> oPromocionDTO.idsServicio().stream()).toList());
		return lista.stream().map(oPromocionDTO -> oPromocionDTO.conServicios(incluidos(oPromocionDTO.idsServicio(), nombres))).toList();
	}

	/** [It. 2 · CUS60, RF84] La promoción (PORCENTAJE o MONTO) con su vigencia y sus servicios. Solo la Administradora (TRG_ADM_PROMOCION). */
	@Transactional
	public PromocionDTO registrarPromocion(PromocionDTO oPromocionDTO) {
		Promocion oPromocion = new Promocion(oPromocionDTO.nombre(), oPromocionDTO.tipoDescuento(), oPromocionDTO.valor(), oPromocionDTO.fechaInicio(),
				oPromocionDTO.fechaFin(), usuarioActual.idUsuario());
		if (oPromocionDTO.idsServicio() != null) {
			oPromocionDTO.idsServicio().forEach(oPromocion::aplicarAServicio);
		}
		PromocionDTO oPromocionGuardada = mapperCatalogo.aDTO(repositorioPromocion.saveAndFlush(oPromocion));
		return oPromocionGuardada.conServicios(incluidos(oPromocionGuardada.idsServicio(), nombresServicios(oPromocionGuardada.idsServicio())));
	}

	@Override
	@Transactional(readOnly = true)
	public DatosVariante obtenerVariante(Long idVariante) {
		Servicio oServicio = repositorioServicio.findByVariante(idVariante)
			.orElseThrow(() -> new ReglaNegocioException("RN40", "La variante " + idVariante + " no existe.", HttpStatus.NOT_FOUND));
		return datosVariante(oServicio, idVariante);
	}

	/** [It. 2.1 · nombres] En lote: una consulta para todas las variantes (antes, una por variante). */
	@Override
	@Transactional(readOnly = true)
	public List<DatosVariante> obtenerVariantes(List<Long> idsVariante) {
		List<Long> ids = idsVariante.stream().distinct().toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		return repositorioServicio.findByVariantes(ids).stream()
			.flatMap(oServicio -> oServicio.getVariantes().stream()
				.filter(oVariante -> ids.contains(oVariante.getIdVariante()))
				.map(oVariante -> datosVariante(oServicio, oVariante.getIdVariante())))
			.toList();
	}

	/** [It. 2.1 · nombres] CUS03 y la venta: el nombre de cada paquete, en una consulta. */
	@Override
	@Transactional(readOnly = true)
	public List<DatosPaquete> obtenerPaquetes(List<Long> idsPaquete) {
		List<Long> ids = idsPaquete.stream().distinct().toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		return repositorioPaquete.findAllById(ids).stream()
			.map(oPaquete -> new DatosPaquete(oPaquete.getIdPaquete(), oPaquete.getNombre(), oPaquete.getTipo().name()))
			.toList();
	}

	private static DatosVariante datosVariante(Servicio oServicio, Long idVariante) {
		String nombreVariante = oServicio.getVariantes().stream()
			.filter(oVariante -> oVariante.getIdVariante().equals(idVariante))
			.map(VarianteServicio::getNombre)
			.findFirst()
			.orElse(null);
		return new DatosVariante(idVariante, oServicio.getIdServicio(), oServicio.getIdArea(), oServicio.getDuracionRefMin(),
				oServicio.getTipoRecurso().name(), oServicio.getNombre(), nombreVariante);
	}

	/** [It. 2.1 · nombres] Los nombres de los servicios de los paquetes o promociones de una respuesta, en una consulta. */
	private Map<Long, String> nombresServicios(Collection<Long> idsServicio) {
		if (idsServicio.isEmpty()) {
			return Map.of();
		}
		return repositorioServicio.findAllById(idsServicio.stream().distinct().toList()).stream()
			.collect(Collectors.toMap(Servicio::getIdServicio, Servicio::getNombre));
	}

	private static List<ServicioIncluidoDTO> incluidos(List<Long> idsServicio, Map<Long, String> nombres) {
		return idsServicio == null ? List.of()
				: idsServicio.stream().map(idServicio -> new ServicioIncluidoDTO(idServicio, nombres.get(idServicio))).toList();
	}

	/** RN54: cantidad_estimada va en la unidad mínima del insumo; si tuviera decimales se descuenta redondeada hacia arriba. */
	@Override
	@Transactional(readOnly = true)
	public List<InsumoEstimadoDTO> insumosDeVariante(Long idVariante) {
		VarianteServicio oVariante = repositorioServicio.findVariante(idVariante)
			.orElseThrow(() -> new ReglaNegocioException("RN40", "La variante " + idVariante + " no existe.", HttpStatus.NOT_FOUND));
		return oVariante.getInsumos().stream()
			.map(insumo -> new InsumoEstimadoDTO(insumo.getIdProducto(),
					insumo.getCantidadEstimada().setScale(0, RoundingMode.CEILING).intValueExact()))
			.toList();
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

	private Servicio buscarServicio(Long idServicio) {
		return repositorioServicio.findById(idServicio)
			.orElseThrow(() -> new ReglaNegocioException("RF83", "El servicio " + idServicio + " no existe.", HttpStatus.NOT_FOUND));
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
