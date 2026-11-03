package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionCobrable;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.PaqueteClienteApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.SaldoFavorApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.CajaAbiertaProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.ResultadoConsulta;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.CierreVentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.DescuentosDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.MapperVenta;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.PagosDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.ProformaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.VentaDTO;
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
		return mapperVenta.aDTO(repositorioVenta.save(oVenta), 0);
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
		return mapperVenta.aDTO(repositorioVenta.saveAndFlush(oVenta), 0);
	}

	/** Un cobro digital sin aprobar no registra pago (RN42): cuenta en cobrosPendientes y el controlador responde 202. */
	@Transactional
	public VentaDTO registrarPagos(Long idVenta, PagosDTO oPagosDTO) {
		Venta oVenta = buscar(idVenta);
		int cobrosPendientes = 0;
		for (PagosDTO.Pago oPago : oPagosDTO.pagos()) {
			if (pagosApi.usaPasarela(oPago.idMedioPago())) {
				TransaccionDTO oTransaccionDTO = pagosApi.cobrar(idVenta, oPago.idMedioPago(), oPago.monto(), oPago.canal());
				if (oTransaccionDTO.aprobada()) {
					oVenta.registrarPago(oPago.idMedioPago(), oPago.monto(), oTransaccionDTO.idTransaccion());
				}
				else {
					cobrosPendientes++;
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
		return mapperVenta.aDTO(oVenta, 0);
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
