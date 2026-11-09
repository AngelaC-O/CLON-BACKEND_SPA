package dev.com.Synapsis_Spa_Cautiva.inventario.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.catalogo.api.InsumoEstimadoDTO;
import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoDTO;
import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.ConsumoPendienteDTO;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.InventarioApi;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.ProductoStockDTO;
import dev.com.Synapsis_Spa_Cautiva.inventario.controller.AjusteDTO;
import dev.com.Synapsis_Spa_Cautiva.inventario.controller.CategoriaProductoDTO;
import dev.com.Synapsis_Spa_Cautiva.inventario.controller.MovimientoDTO;
import dev.com.Synapsis_Spa_Cautiva.inventario.controller.MovimientoKardexDTO;
import dev.com.Synapsis_Spa_Cautiva.inventario.controller.ProductoDTO;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.CategoriaProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.ConsumoPendiente;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.EstadoProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.InventarioSede;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.MovimientoKardex;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.Producto;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.TipoUsoProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.repository.FilaKardex;
import dev.com.Synapsis_Spa_Cautiva.inventario.repository.RepositorioCategoriaProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.repository.RepositorioConsumoPendiente;
import dev.com.Synapsis_Spa_Cautiva.inventario.repository.RepositorioProducto;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [It. 2] CUS66, CUS32, CUS33, CUS34 y CUS35, la interfaz publicada InventarioApi (CUS25, CUS26, CUS11, CUS12) y los avisos de almacén
 * (CUS50). Java solo inserta MOVIMIENTO_KARDEX: TRG_KARDEX_APLICAR mueve el stock y rechaza el saldo negativo (RN15) y a otros roles (RN17).
 */
@Service
@RequiredArgsConstructor
public class ServicioProducto implements InventarioApi, AvisoProvider {

	private static final String MARGEN_PRODUCTO = "MARGEN_PRODUCTO_PCT";

	private static final String MARGEN_PREMIUM = "MARGEN_PREMIUM_PCT";

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final RepositorioProducto repositorioProducto;

	private final RepositorioCategoriaProducto repositorioCategoriaProducto;

	private final RepositorioConsumoPendiente repositorioConsumoPendiente;

	private final ParametroApi parametroApi;

	private final UsuarioActual usuarioActual;

	// ---------- CUS66 Registrar Categoría de Producto ----------

	@Transactional
	public CategoriaProductoDTO registrarCategoria(CategoriaProductoDTO oCategoriaDTO) {
		String nombre = oCategoriaDTO.nombre() == null ? "" : oCategoriaDTO.nombre().trim();
		if (nombre.isEmpty()) {
			throw new ReglaNegocioException("RF45", "Indique el nombre de la categoría.");
		}
		if (repositorioCategoriaProducto.existsByNombre(nombre)) {
			throw new ReglaNegocioException("RF45", "Ya existe la categoría «" + nombre + "».", HttpStatus.CONFLICT);
		}
		CategoriaProducto oCategoria = repositorioCategoriaProducto.save(new CategoriaProducto(nombre));
		return new CategoriaProductoDTO(oCategoria.getIdCategoria(), oCategoria.getNombre());
	}

	// ---------- CUS32 Registrar Producto ----------

	/** [It. 2] Productos e insumos con su stock de la sede de la sesión (CUS33 a CUS35 y el registro de insumos de CUS57). */
	@Transactional(readOnly = true)
	public List<ProductoDTO> listarProductos(String nombre, Long idCategoria) {
		Long idSede = usuarioActual.idSede();
		boolean verMargen = esAdministradora();
		return repositorioProducto.findByFiltros(vacioANull(nombre), idCategoria, idSede).stream()
			.map(oProducto -> aDTO(oProducto, idSede, verMargen))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<CategoriaProductoDTO> listarCategorias() {
		return repositorioCategoriaProducto.findAll().stream()
			.map(oCategoria -> new CategoriaProductoDTO(oCategoria.getIdCategoria(), oCategoria.getNombre()))
			.sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()))
			.toList();
	}

	/**
	 * RF87, RN14: precio_venta = costo × (1 + margen). El margen sale de PARAMETRO (MARGEN_PREMIUM_PCT si es premium, si no
	 * MARGEN_PRODUCTO_PCT) salvo que la encargada lo ajuste antes de guardar. RF86, RN16: mínimo y ubicación en la sede de la sesión.
	 */
	@Transactional
	public ProductoDTO registrar(ProductoDTO oProductoDTO) {
		Long idSede = usuarioActual.idSede();
		BigDecimal margen = oProductoDTO.margen() != null ? oProductoDTO.margen()
				: parametroApi.obtenerValorNumero(Boolean.TRUE.equals(oProductoDTO.esPremium()) ? MARGEN_PREMIUM : MARGEN_PRODUCTO);
		Producto oProducto = new Producto(oProductoDTO.codigo(), oProductoDTO.nombre(), oProductoDTO.marca(), oProductoDTO.idCategoria(),
				oProductoDTO.unidad(), oProductoDTO.tipoUso(), oProductoDTO.costo());
		oProducto.definirPremium(oProductoDTO.esPremium());
		oProducto.calcularPrecioVenta(margen);
		oProducto.definirStockMinimo(idSede, oProductoDTO.stockMinimo() == null ? 0 : oProductoDTO.stockMinimo(), oProductoDTO.ubicacion());
		repositorioProducto.saveAndFlush(oProducto);
		return aDTO(oProducto, idSede, esAdministradora());
	}

	// ---------- CUS33 Registrar Salida de Insumos ----------

	@Transactional(readOnly = true)
	public List<ConsumoPendienteDTO> listarConsumosPendientes(Long idSede) {
		List<ConsumoPendiente> pendientes = repositorioConsumoPendiente.findPendientes(idSede);
		Map<Long, Producto> productos = productosPorId(pendientes.stream().map(ConsumoPendiente::getIdProducto).toList());
		return pendientes.stream().map(oConsumo -> aDTO(oConsumo, productos.get(oConsumo.getIdProducto()))).toList();
	}

	/** RF46, RN15, RN18: la salida queda ligada a la atención si se indica. RF47: avisa si quedó en el stock mínimo. */
	@Transactional
	public MovimientoDTO registrarSalida(Long idProducto, MovimientoDTO oMovimientoDTO) {
		exigirMotivo(oMovimientoDTO.motivo(), "RF46", "Indique el motivo de la salida.");
		Long idSede = usuarioActual.idSede();
		Producto oProducto = buscarProducto(idProducto);
		oProducto.registrarSalida(idSede, oMovimientoDTO.cantidad(), oMovimientoDTO.motivo().trim(), oMovimientoDTO.idAtencion());
		repositorioProducto.saveAndFlush(oProducto);
		return aDTO(oProducto, idSede);
	}

	/** RN53: SALIDA del mismo producto, sede y atención por lo pendiente; 409 si aún no hay stock (RN15). */
	@Transactional
	public ConsumoPendienteDTO regularizar(Long idConsumo) {
		ConsumoPendiente oConsumoPendiente = buscarConsumo(idConsumo);
		Producto oProducto = buscarProducto(oConsumoPendiente.getIdProducto());
		oProducto.registrarSalida(oConsumoPendiente.getIdSede(), oConsumoPendiente.getCantidadPendiente(),
				"Regularización del consumo pendiente N° " + idConsumo + " de la atención N° " + oConsumoPendiente.getIdAtencion(),
				oConsumoPendiente.getIdAtencion());
		repositorioProducto.saveAndFlush(oProducto);
		Long idMovimiento = oProducto.ultimoMovimiento(oConsumoPendiente.getIdSede()).map(MovimientoKardex::getIdMovimiento).orElseThrow();
		oConsumoPendiente.regularizar(usuarioActual.idUsuario(), idMovimiento);
		return aDTO(repositorioConsumoPendiente.saveAndFlush(oConsumoPendiente), oProducto);
	}

	/** RN53: lo descarta con su motivo (CK_CONSUMO_PEND_DESCARTADO, 422 si falta); uno ya resuelto no cambia (409). */
	@Transactional
	public ConsumoPendienteDTO descartar(Long idConsumo, String observacion) {
		ConsumoPendiente oConsumoPendiente = buscarConsumo(idConsumo);
		oConsumoPendiente.descartar(usuarioActual.idUsuario(), observacion);
		ConsumoPendiente guardado = repositorioConsumoPendiente.saveAndFlush(oConsumoPendiente);
		return aDTO(guardado, buscarProducto(guardado.getIdProducto()));
	}

	// ---------- CUS34 Registrar Ajuste de Inventario ----------

	/** RF48, RN17: guarda la diferencia con signo (contado − stock); si es 0 no registra nada y responde 422. */
	@Transactional
	public MovimientoDTO registrarAjuste(Long idProducto, AjusteDTO oAjusteDTO) {
		Long idSede = usuarioActual.idSede();
		Producto oProducto = buscarProducto(idProducto);
		if (!oProducto.registrarAjuste(idSede, oAjusteDTO.cantidad(), oAjusteDTO.motivo(), usuarioActual.idUsuario())) {
			throw new ReglaNegocioException("RF48", "Lo contado es igual al stock de la sede: no hay diferencia que ajustar.");
		}
		repositorioProducto.saveAndFlush(oProducto);
		return aDTO(oProducto, idSede);
	}

	// ---------- CUS35 Consultar Kardex ----------

	/** RF50, RN18: lee V_KARDEX. Sin fechas, los últimos 30 días hasta hoy. */
	@Transactional(readOnly = true)
	public List<MovimientoKardexDTO> consultarKardex(Long idProducto, Long idSede, LocalDate fechaInicio, LocalDate fechaFin) {
		buscarProducto(idProducto);
		LocalDate fin = fechaFin == null ? LocalDate.now() : fechaFin;
		LocalDate inicio = fechaInicio == null ? fin.minusDays(30) : fechaInicio;
		if (inicio.isAfter(fin)) {
			throw new ReglaNegocioException("RF50", "La fecha de inicio no puede ser posterior a la fecha de fin.");
		}
		return repositorioProducto.findMovimientos(idProducto, idSede == null ? usuarioActual.idSede() : idSede, inicio, fin).stream()
			.map(ServicioProducto::aDTO)
			.toList();
	}

	// ---------- InventarioApi ----------

	@Override
	@Transactional(readOnly = true)
	public List<ProductoStockDTO> buscar(String nombre, String marca, Long idCategoria, Long idSede) {
		String marcaBuscada = vacioANull(marca);
		return repositorioProducto.findByFiltros(vacioANull(nombre), idCategoria, idSede).stream()
			.filter(oProducto -> oProducto.getTipoUso() == TipoUsoProducto.VENTA && oProducto.getEstado() == EstadoProducto.ACTIVO)
			.filter(oProducto -> marcaBuscada == null
					|| (oProducto.getMarca() != null && oProducto.getMarca().toUpperCase().contains(marcaBuscada.toUpperCase())))
			.map(oProducto -> new ProductoStockDTO(oProducto.getIdProducto(), oProducto.getCodigo(), oProducto.getNombre(), oProducto.getMarca(),
					oProducto.getIdCategoria(), oProducto.getUnidad(), oProducto.getPrecioVenta(), stockEn(oProducto, idSede)))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean verificarStock(Long idProducto, Long idSede, Integer cantidad) {
		Producto oProducto = buscarProducto(idProducto);
		int stock = stockEn(oProducto, idSede);
		if (cantidad == null || cantidad > stock) {
			throw new ReglaNegocioException("RN15",
					"No hay stock suficiente de «" + oProducto.getNombre() + "»: hay " + stock + " " + oProducto.getUnidad() + ".", HttpStatus.CONFLICT);
		}
		return true;
	}

	/**
	 * RN53, RN54: por cada insumo descuenta lo menor entre lo estimado y el stock de la sede (SALIDA con id_atencion; TRG_KARDEX_APLICAR v5.1
	 * la admite para la especialista de esa atención) y deja lo que falta en CONSUMO_PENDIENTE. Nunca impide finalizar la atención.
	 */
	@Override
	@Transactional
	public List<ConsumoPendienteDTO> registrarConsumoAtencion(Long idAtencion, Long idSede, List<InsumoEstimadoDTO> insumos) {
		List<ConsumoPendienteDTO> pendientes = new ArrayList<>();
		for (InsumoEstimadoDTO insumo : insumos) {
			if (insumo.cantidadEstimada() == null || insumo.cantidadEstimada() <= 0) {
				continue;
			}
			Producto oProducto = buscarProducto(insumo.idProducto());
			int descontar = Math.min(insumo.cantidadEstimada(), stockEn(oProducto, idSede));
			if (descontar > 0) {
				oProducto.registrarSalida(idSede, descontar, "Consumo estimado de la atención N° " + idAtencion, idAtencion);
				repositorioProducto.saveAndFlush(oProducto);
			}
			if (descontar < insumo.cantidadEstimada()) {
				ConsumoPendiente oConsumoPendiente = repositorioConsumoPendiente.saveAndFlush(
						new ConsumoPendiente(idAtencion, oProducto.getIdProducto(), idSede, insumo.cantidadEstimada(), descontar));
				pendientes.add(aDTO(oConsumoPendiente, oProducto));
			}
		}
		return pendientes;
	}

	// ---------- AvisoProvider (CUS50) ----------

	/** RF47: productos en su stock mínimo (V_STOCK_ALERTA) y RN53: consumos pendientes de la sede. Solo los que tienen algo. */
	@Override
	@Transactional(readOnly = true)
	public List<AvisoDTO> avisos(Long idSede) {
		List<AvisoDTO> avisos = new ArrayList<>();
		int enMinimo = repositorioProducto.findEnStockMinimo(idSede).size();
		if (enMinimo > 0) {
			avisos.add(new AvisoDTO("STOCK_MINIMO", enMinimo + (enMinimo == 1 ? " producto en stock mínimo" : " productos en stock mínimo"),
					enMinimo, "PRODUCTOS:VER", "/inventario/productos"));
		}
		int pendientes = repositorioConsumoPendiente.findPendientes(idSede).size();
		if (pendientes > 0) {
			avisos.add(new AvisoDTO("CONSUMO_PENDIENTE", pendientes + (pendientes == 1 ? " consumo pendiente" : " consumos pendientes"),
					pendientes, "KARDEX:VER", "/inventario/consumos-pendientes"));
		}
		return avisos;
	}

	// ---------- apoyo ----------

	private Producto buscarProducto(Long idProducto) {
		return repositorioProducto.findById(idProducto)
			.orElseThrow(() -> new ReglaNegocioException("RF44", "El producto " + idProducto + " no existe.", HttpStatus.NOT_FOUND));
	}

	private ConsumoPendiente buscarConsumo(Long idConsumo) {
		return repositorioConsumoPendiente.findById(idConsumo)
			.orElseThrow(() -> new ReglaNegocioException("RN53", "El consumo pendiente " + idConsumo + " no existe.", HttpStatus.NOT_FOUND));
	}

	private Map<Long, Producto> productosPorId(List<Long> idsProducto) {
		return repositorioProducto.findAllById(idsProducto.stream().distinct().toList()).stream()
			.collect(Collectors.toMap(Producto::getIdProducto, Function.identity()));
	}

	private boolean esAdministradora() {
		return usuarioActual.esAdministradora(usuarioActual.idUsuario());
	}

	/** El stock de la sede como lo dejó el último movimiento de esta transacción o, si no hubo, INVENTARIO_SEDE. */
	private static int stockEn(Producto oProducto, Long idSede) {
		return oProducto.ultimoMovimiento(idSede).map(MovimientoKardex::getSaldo)
			.orElseGet(() -> oProducto.getInventarios().stream()
				.filter(inventario -> inventario.getIdSede().equals(idSede))
				.map(InventarioSede::getStockActual)
				.findFirst()
				.orElse(0));
	}

	private static void exigirMotivo(String motivo, String regla, String mensaje) {
		if (motivo == null || motivo.isBlank()) {
			throw new ReglaNegocioException(regla, mensaje);
		}
	}

	private static String vacioANull(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

	/** RN24: el margen solo lo ve la Administradora. */
	private static ProductoDTO aDTO(Producto oProducto, Long idSede, boolean verMargen) {
		InventarioSede inventario = oProducto.getInventarios().stream().filter(i -> i.getIdSede().equals(idSede)).findFirst().orElse(null);
		BigDecimal margen = verMargen && oProducto.getCosto().signum() > 0
				? oProducto.getPrecioVenta().divide(oProducto.getCosto(), 6, RoundingMode.HALF_UP).subtract(BigDecimal.ONE).multiply(CIEN)
					.setScale(2, RoundingMode.HALF_UP)
				: null;
		return new ProductoDTO(oProducto.getIdProducto(), oProducto.getCodigo(), oProducto.getNombre(), oProducto.getMarca(),
				oProducto.getIdCategoria(), oProducto.getUnidad(), oProducto.getTipoUso(), oProducto.getCosto(), margen, oProducto.getPrecioVenta(),
				oProducto.getEsPremium(), oProducto.getEstado(), inventario == null ? 0 : stockEn(oProducto, idSede),
				inventario == null ? null : inventario.getStockMinimo(), inventario == null ? null : inventario.getUbicacion());
	}

	/** El movimiento que acaba de guardar Oracle (con su id, saldo, usuario y fecha) y si quedó en el stock mínimo (RF47). */
	private static MovimientoDTO aDTO(Producto oProducto, Long idSede) {
		MovimientoKardex oMovimiento = oProducto.ultimoMovimiento(idSede).orElseThrow();
		return new MovimientoDTO(oMovimiento.getIdMovimiento(), oProducto.getIdProducto(), idSede, oMovimiento.getTipo(), oMovimiento.getCantidad(),
				oMovimiento.getMotivo(), oMovimiento.getIdAtencion(), oMovimiento.getSaldo(), oMovimiento.getFechaHora(),
				oProducto.estaEnMinimo(idSede));
	}

	private static ConsumoPendienteDTO aDTO(ConsumoPendiente oConsumo, Producto oProducto) {
		return new ConsumoPendienteDTO(oConsumo.getIdConsumo(), oConsumo.getIdAtencion(), oConsumo.getIdProducto(),
				oProducto == null ? null : oProducto.getCodigo(), oProducto == null ? null : oProducto.getNombre(),
				oProducto == null ? null : oProducto.getUnidad(), oConsumo.getIdSede(), oConsumo.getCantidadEstimada(),
				oConsumo.getCantidadDescontada(), oConsumo.getCantidadPendiente(), oConsumo.getEstado().name(), oConsumo.getFechaRegistro(),
				oConsumo.getIdMovimiento(), oConsumo.getObservacion());
	}

	private static MovimientoKardexDTO aDTO(FilaKardex fila) {
		return new MovimientoKardexDTO(fila.getIdMovimiento(), fila.getFechaHora(), fila.getTipo(), fila.getMotivo(), fila.getCantidadConSigno(),
				fila.getSaldo(), fila.getUsuario(), fila.getIdDetalleVenta(), fila.getIdDetalleCompra(), fila.getIdAtencion());
	}

}
