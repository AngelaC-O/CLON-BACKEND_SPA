package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionCobrable;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.PaqueteClienteApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.SaldoFavorApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.CajaAbiertaProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.InventarioApi;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.ProductoStockDTO;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.ResultadoConsulta;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.CierreVentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.CobroPendienteDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.DescuentosDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.MapperVenta;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.PagoVentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.PagosDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.PorCobrarDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.ProformaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.VentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.VentaProductosDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.DocumentoComprador;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoItemVentaDetalle;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaDetalle;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioVenta;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que Oracle ya hace no se repite aquí: al cerrar, TRG_VENTA_EFECTOS marca las atenciones COBRADAS y registra la salida de productos;
 * serie y número los asigna PKG_VENTA.
 */
@Service
@RequiredArgsConstructor
public class ServicioVenta {

	private static final String CODIGO_RUC = "6";

	private final CajaAbiertaProvider cajaAbiertaProvider;

	private final AtencionApi atencionApi;

	private final RepositorioVenta repositorioVenta;

	private final SaldoFavorApi saldoFavorApi;

	private final CalculadoraVenta calculadoraVenta;

	private final PagosApi pagosApi;

	private final PaqueteClienteApi paqueteClienteApi;

	private final ServicioFacturacionElectronica servicioFacturacionElectronica;

	private final PersonaApi personaApi;

	private final CatalogoApi catalogoApi;

	private final UsuarioActual usuarioActual;

	private final ParametroApi parametroApi;

	private final MapperVenta mapperVenta;

	private final InventarioApi inventarioApi;

	/** [It. 2 · CUS25, RF35] Productos de VENTA activos con su precio y su stock en la sede de la sesión; los insumos no aparecen. */
	@Transactional(readOnly = true)
	public List<ProductoStockDTO> buscarProductos(String nombre, String marca, Long idCategoria) {
		return inventarioApi.buscar(nombre, marca, idCategoria, usuarioActual.idSede());
	}

	/**
	 * [It. 2 · CUS26, RF36 a RF39] En el orden del diagrama: stock por línea (409, RN15), caja ABIERTA de la sede (RN30), la venta con sus
	 * líneas (VENTA_DETALLE.id_personal = quien recomendó, sin comisión: RN38), el cobro (RN42), el comprobante (CUS19) y el cierre. La salida
	 * de kardex la registra Oracle al cerrar (TRG_VENTA_EFECTOS): aquí no se repite. [RN52] Si el cobro digital queda PENDIENTE, la venta queda
	 * ABIERTA con cobrosPendientes (el controlador responde 202) y se cierra con POST /ventas/{idVenta}/cierre cuando el webhook lo aprueba.
	 */
	@Transactional
	public VentaDTO venderProductos(VentaProductosDTO oVentaProductosDTO) {
		Long idSede = usuarioActual.idSede();
		for (VentaProductosDTO.Linea oLinea : oVentaProductosDTO.lineas()) {
			inventarioApi.verificarStock(oLinea.idProducto(), idSede, oLinea.cantidad());
		}
		Long idCaja = cajaAbiertaProvider.obtenerCajaAbierta(idSede);
		Venta oVenta = new Venta(idSede, idCaja, oVentaProductosDTO.idCliente(), usuarioActual.idUsuario());
		Map<Long, BigDecimal> precios = preciosDeVenta(idSede);
		for (VentaProductosDTO.Linea oLinea : oVentaProductosDTO.lineas()) {
			if (!precios.containsKey(oLinea.idProducto())) {
				throw new ReglaNegocioException("RN13", "El producto " + oLinea.idProducto() + " es de uso interno o está inactivo: no se vende.");
			}
			oVenta.agregarProducto(oLinea.idProducto(), oLinea.cantidad(), precios.get(oLinea.idProducto()), oLinea.idPersonal());
		}
		// RN11 antes de cobrar: un monto distinto del total no debe llegar a la pasarela
		if (oVentaProductosDTO.monto().compareTo(oVenta.getTotal()) != 0) {
			throw new ReglaNegocioException("RN11", "El pago es de S/ " + oVentaProductosDTO.monto() + " y el total es S/ " + oVenta.getTotal() + ".");
		}
		repositorioVenta.saveAndFlush(oVenta);
		if (pagosApi.usaPasarela(oVentaProductosDTO.idMedioPago())) {
			TransaccionDTO oTransaccionDTO = pagosApi.cobrar(oVenta.getIdVenta(), oVentaProductosDTO.idMedioPago(), oVentaProductosDTO.monto(),
					oVentaProductosDTO.canal());
			if (!oTransaccionDTO.aprobada()) {
				return mapperVenta.aDTO(oVenta, List.of(new CobroPendienteDTO(oTransaccionDTO.idTransaccion(), oTransaccionDTO.enlacePago())));
			}
			oVenta.registrarPago(oVentaProductosDTO.idMedioPago(), oVentaProductosDTO.monto(), oTransaccionDTO.idTransaccion());
		}
		else {
			oVenta.registrarPago(oVentaProductosDTO.idMedioPago(), oVentaProductosDTO.monto(), null);
		}
		CierreVentaDTO oCierreVentaDTO = new CierreVentaDTO(oVentaProductosDTO.tipo(), oVentaProductosDTO.idTipoDocComprador(),
				oVentaProductosDTO.nroDocComprador(), oVentaProductosDTO.nombreComprador());
		validarComprador(oVenta, oCierreVentaDTO);
		oVenta.generarComprobante(oCierreVentaDTO.tipo(), documentoComprador(oCierreVentaDTO));
		oVenta.cerrar();
		repositorioVenta.saveAndFlush(oVenta);
		Comprobante oComprobante = oVenta.getComprobantes().getFirst();
		servicioFacturacionElectronica.emitirComprobante(oComprobante.getIdComprobante());
		return mapperVenta.aDTO(oVenta);
	}

	/**
	 * El diagrama de CUS26 no dice de dónde sale el precio de agregarProducto: es el de PRODUCTO.precio_venta (InventarioApi.buscar, que solo
	 * trae productos de VENTA activos). TRG_VENTA_DETALLE_REGLAS lo vuelve a poner; tomarlo aquí mantiene el total de la venta (CK_VENTA_TOTAL).
	 */
	private Map<Long, BigDecimal> preciosDeVenta(Long idSede) {
		return inventarioApi.buscar(null, null, null, idSede).stream()
			.filter(oProducto -> oProducto.precioVenta() != null)
			.collect(Collectors.toMap(ProductoStockDTO::idProducto, ProductoStockDTO::precioVenta));
	}

	/** [It. 2] CUS18: las atenciones FINALIZADAS sin venta y las ventas ABIERTAS de la sede, para cobrarlas o retomarlas (RN52). */
	@Transactional(readOnly = true)
	public PorCobrarDTO listarPorCobrar(Long idSede) {
		List<Venta> abiertas = repositorioVenta.findAbiertasPorSede(idSede);
		Set<Long> enVenta = abiertas.stream()
			.flatMap(oVenta -> oVenta.getDetalles().stream())
			.map(VentaDetalle::getIdAtencion)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		List<AtencionCobrable> atenciones = atencionApi.listarCobrables(idSede).stream()
			.filter(oAtencion -> !enVenta.contains(oAtencion.idAtencion()))
			.toList();
		return new PorCobrarDTO(atenciones, abiertas.stream().map(mapperVenta::aDTO).toList());
	}

	/** [It. 2] CUS18: la venta con el estado actual de sus cobros digitales en la pasarela (RN42, RN52). */
	@Transactional(readOnly = true)
	public VentaDTO obtenerVenta(Long idVenta) {
		VentaDTO oVentaDTO = mapperVenta.aDTO(buscar(idVenta));
		List<PagoVentaDTO> pagos = oVentaDTO.pagos().stream()
			.map(oPago -> oPago.idTransaccion() == null ? oPago : oPago.conEstadoTransaccion(pagosApi.consultarTransaccion(oPago.idTransaccion()).estado()))
			.toList();
		return new VentaDTO(oVentaDTO.idVenta(), oVentaDTO.idSede(), oVentaDTO.idCaja(), oVentaDTO.idCliente(), oVentaDTO.estado(), oVentaDTO.detalles(),
				oVentaDTO.subtotal(), oVentaDTO.igv(), oVentaDTO.descuento(), oVentaDTO.adelantoAplicado(), oVentaDTO.saldoFavorAplicado(), oVentaDTO.total(),
				pagos, oVentaDTO.pagado(), oVentaDTO.pendiente(), oVentaDTO.cobrosPendientes(), oVentaDTO.comprobante());
	}

	@Transactional
	public VentaDTO crearDesdeAtencion(Long idAtencion) {
		Long idSede = usuarioActual.idSede();
		Long idCaja = cajaAbiertaProvider.obtenerCajaAbierta(idSede);
		AtencionCobrable oAtencionCobrable = atencionApi.obtenerAtencionCobrable(idAtencion);
		Venta oVenta = new Venta(idSede, idCaja, oAtencionCobrable.idCliente(), usuarioActual.idUsuario());
		// Con sesión de paquete la línea va en 0 (RN36); el trigger reescribe el precio de todos modos
		BigDecimal precioUnitario = oAtencionCobrable.idClientePaquete() != null ? BigDecimal.ZERO
				: catalogoApi.precioVigente(oAtencionCobrable.idVariante(), null, null);
		oVenta.agregarServicio(idAtencion, precioUnitario, oAtencionCobrable.idPersonal());
		return mapperVenta.aDTO(repositorioVenta.save(oVenta));
	}

	/** El flush hace que Oracle calcule el descuento de la promoción antes de leer los totales (DF-11). */
	@Transactional
	public VentaDTO aplicarDescuentos(Long idVenta, DescuentosDTO oDescuentosDTO) {
		Venta oVenta = buscar(idVenta);
		if (oDescuentosDTO.idPromocion() != null) {
			oVenta.aplicarPromocion(oDescuentosDTO.idPromocion());
			repositorioVenta.saveAndFlush(oVenta);
		}
		oVenta.aplicarAdelanto(adelantoDe(oVenta));
		BigDecimal saldoFavorAplicado = oDescuentosDTO.saldoFavorAplicado();
		if (saldoFavorAplicado != null && saldoFavorAplicado.signum() > 0) {
			usarSaldo(oVenta, saldoFavorAplicado);
		}
		BigDecimal total = calculadoraVenta.calcularTotal(oVenta);
		if (total.compareTo(oVenta.getTotal()) != 0) {
			throw new IllegalStateException("La venta " + idVenta + " calcula un total distinto del que guarda");
		}
		return mapperVenta.aDTO(repositorioVenta.saveAndFlush(oVenta));
	}

	/**
	 * Un cobro digital sin aprobar no registra pago (RN42): cuenta en cobrosPendientes y el controlador responde 202. [It. 2 · RN52] PagosApi.cobrar
	 * guarda la transacción aparte: si queda PENDIENTE, la venta sigue ABIERTA; si la pasarela la rechaza, la fila RECHAZADA queda aunque esta
	 * transacción haga rollback por el 422.
	 */
	@Transactional
	public VentaDTO registrarPagos(Long idVenta, PagosDTO oPagosDTO) {
		Venta oVenta = buscar(idVenta);
		List<CobroPendienteDTO> cobrosPendientes = new ArrayList<>();
		for (PagosDTO.Pago oPago : oPagosDTO.pagos()) {
			if (oPago.idTransaccion() != null) {
				pagosApi.exigirAprobada(oPago.idTransaccion());
				oVenta.registrarPago(oPago.idMedioPago(), oPago.monto(), oPago.idTransaccion());
			}
			else if (pagosApi.usaPasarela(oPago.idMedioPago())) {
				TransaccionDTO oTransaccionDTO = pagosApi.cobrar(idVenta, oPago.idMedioPago(), oPago.monto(), oPago.canal());
				if (oTransaccionDTO.aprobada()) {
					oVenta.registrarPago(oPago.idMedioPago(), oPago.monto(), oTransaccionDTO.idTransaccion());
				}
				else {
					cobrosPendientes.add(new CobroPendienteDTO(oTransaccionDTO.idTransaccion(), oTransaccionDTO.enlacePago()));
				}
			}
			else {
				oVenta.registrarPago(oPago.idMedioPago(), oPago.monto(), null);
			}
		}
		return mapperVenta.aDTO(repositorioVenta.saveAndFlush(oVenta), cobrosPendientes);
	}

	public ResultadoConsulta consultarRuc(String ruc) {
		return personaApi.consultarDocumento(personaApi.obtenerIdTipoDocumento(CODIGO_RUC), ruc);
	}

	@Transactional
	public VentaDTO cerrar(Long idVenta, CierreVentaDTO oCierreVentaDTO) {
		Venta oVenta = buscar(idVenta);
		validarComprador(oVenta, oCierreVentaDTO);
		oVenta.generarComprobante(oCierreVentaDTO.tipo(), documentoComprador(oCierreVentaDTO));
		oVenta.cerrar();
		for (VentaDetalle oDetalle : oVenta.getDetalles()) {
			if (oDetalle.getTipoItem() == TipoItemVentaDetalle.PAQUETE) {
				paqueteClienteApi.registrarPaquete(oVenta.getIdCliente(), oDetalle.getIdPaquete(), oDetalle.getIdDetalle());
			}
		}
		repositorioVenta.saveAndFlush(oVenta);
		Comprobante oComprobante = oVenta.getComprobantes().getFirst();
		servicioFacturacionElectronica.emitirComprobante(oComprobante.getIdComprobante());
		return mapperVenta.aDTO(oVenta);
	}

	public ProformaDTO armarProforma(ProformaDTO oProformaDTO) {
		List<ProformaDTO.Item> items = oProformaDTO.items().stream().map(this::conPrecio).toList();
		ProformaDTO conPrecios = new ProformaDTO(oProformaDTO.idCliente(), oProformaDTO.nombreCliente(), oProformaDTO.fechaValidez(), items, null);
		return new ProformaDTO(oProformaDTO.idCliente(), oProformaDTO.nombreCliente(), oProformaDTO.fechaValidez(), items,
				calculadoraVenta.calcularTotal(conPrecios));
	}

	private BigDecimal adelantoDe(Venta oVenta) {
		return oVenta.getDetalles().stream()
			.filter(oDetalle -> oDetalle.getIdAtencion() != null)
			.map(oDetalle -> atencionApi.obtenerAtencionCobrable(oDetalle.getIdAtencion()).adelantoMonto())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** SALDO_FAVOR_MOV es inmutable: un segundo uso no coincidiría con saldo_favor_aplicado (-20053, DF-44). */
	private void usarSaldo(Venta oVenta, BigDecimal saldoFavorAplicado) {
		if (oVenta.getIdCliente() == null) {
			throw new ReglaNegocioException("RF25", "El saldo a favor solo lo usa una clienta registrada.");
		}
		if (oVenta.getSaldoFavorAplicado().signum() > 0) {
			throw new ReglaNegocioException("RF25", "La venta ya tiene saldo a favor aplicado.");
		}
		saldoFavorApi.usarSaldo(oVenta.getIdCliente(), saldoFavorAplicado, oVenta.getIdVenta());
		oVenta.aplicarSaldoFavor(saldoFavorAplicado);
	}

	/** RN08 con UMBRAL_BOLETA_DOC; -20056 y -20098 lo repiten en Oracle. */
	private void validarComprador(Venta oVenta, CierreVentaDTO oCierreVentaDTO) {
		BigDecimal importe = oVenta.getSubtotal().add(oVenta.getIgv());
		boolean sinComprador = oCierreVentaDTO.nroDocComprador() == null || oCierreVentaDTO.nombreComprador() == null;
		if (oCierreVentaDTO.tipo() == TipoComprobante.BOLETA && sinComprador
				&& importe.compareTo(parametroApi.valorNumero("UMBRAL_BOLETA_DOC")) > 0) {
			throw new ReglaNegocioException("RN08", "Esta boleta lleva el nombre y el documento del comprador.");
		}
	}

	private DocumentoComprador documentoComprador(CierreVentaDTO oCierreVentaDTO) {
		if (oCierreVentaDTO.idTipoDocComprador() == null && oCierreVentaDTO.nroDocComprador() == null && oCierreVentaDTO.nombreComprador() == null) {
			return null;
		}
		return new DocumentoComprador(oCierreVentaDTO.idTipoDocComprador(), oCierreVentaDTO.nroDocComprador(), oCierreVentaDTO.nombreComprador());
	}

	private ProformaDTO.Item conPrecio(ProformaDTO.Item oItem) {
		if (oItem.tipoItem() == TipoItemVentaDetalle.PRODUCTO) {
			throw new ReglaNegocioException("RF31", "La proforma se arma con variantes y paquetes del catálogo.");
		}
		BigDecimal precioUnitario = catalogoApi.precioVigente(oItem.idVariante(), oItem.idPaquete(), oItem.idPromocion());
		return new ProformaDTO.Item(oItem.tipoItem(), oItem.idVariante(), oItem.idPaquete(), oItem.idPromocion(), oItem.cantidad(), precioUnitario,
				precioUnitario.multiply(BigDecimal.valueOf(oItem.cantidad())));
	}

	private Venta buscar(Long idVenta) {
		return repositorioVenta.findById(idVenta)
			.orElseThrow(() -> new ReglaNegocioException("RF21", "La venta " + idVenta + " no existe.", HttpStatus.NOT_FOUND));
	}

}
