package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.AlmacenArchivosPort;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.SesionOracle;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.DatosProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.InventarioApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.ComprobanteDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.MapperVenta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.NotaCredito;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.SolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaDetalle;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.FilaCpePendiente;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioNotaCredito;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioVenta;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class ServicioFacturacionElectronica {

	private static final String FORMATO_ENLACE = "%s/api/v1/comprobantes/publico/%d?expira=%d&firma=%s";

	/** Días antes de CPE_PLAZO_ENVIO_DIAS desde los que se alerta (U2 §6). */
	private static final long DIAS_DE_ALERTA = 1;

	private final RepositorioComprobante repositorioComprobante;

	private final RepositorioNotaCredito repositorioNotaCredito;

	private final RepositorioVenta repositorioVenta;

	private final FacturacionElectronicaPort facturacionElectronicaPort;

	private final AlmacenArchivosPort almacenArchivosPort;

	private final GeneradorPdfComprobante generadorPdfComprobante;

	private final FirmaEnlaceComprobante firmaEnlaceComprobante;

	private final PersonaApi personaApi;

	private final AtencionApi atencionApi;

	private final CatalogoApi catalogoApi;

	private final ParametroApi parametroApi;

	private final SesionOracle sesionOracle;

	private final UsuarioActual usuarioActual;

	private final TransactionTemplate transacciones;

	private final MapperVenta mapperVenta;

	/** [It. 2.1 · D-33] El nombre del producto en la línea del CPE y del PDF. */
	private final InventarioApi inventarioApi;

	private final String urlPublica;

	private final long vigenciaHoras;

	ServicioFacturacionElectronica(RepositorioComprobante repositorioComprobante, RepositorioNotaCredito repositorioNotaCredito, RepositorioVenta repositorioVenta,
			FacturacionElectronicaPort facturacionElectronicaPort,
			AlmacenArchivosPort almacenArchivosPort, GeneradorPdfComprobante generadorPdfComprobante, FirmaEnlaceComprobante firmaEnlaceComprobante,
			PersonaApi personaApi, AtencionApi atencionApi, CatalogoApi catalogoApi, ParametroApi parametroApi, SesionOracle sesionOracle,
			UsuarioActual usuarioActual, TransactionTemplate transacciones, MapperVenta mapperVenta, InventarioApi inventarioApi,
			@Value("${cautiva.comprobantes.url-publica}") String urlPublica, @Value("${cautiva.comprobantes.enlace-vigencia-horas}") long vigenciaHoras) {
		this.inventarioApi = inventarioApi;
		this.repositorioComprobante = repositorioComprobante;
		this.repositorioNotaCredito = repositorioNotaCredito;
		this.repositorioVenta = repositorioVenta;
		this.facturacionElectronicaPort = facturacionElectronicaPort;
		this.almacenArchivosPort = almacenArchivosPort;
		this.generadorPdfComprobante = generadorPdfComprobante;
		this.firmaEnlaceComprobante = firmaEnlaceComprobante;
		this.personaApi = personaApi;
		this.atencionApi = atencionApi;
		this.catalogoApi = catalogoApi;
		this.parametroApi = parametroApi;
		this.sesionOracle = sesionOracle;
		this.usuarioActual = usuarioActual;
		this.transacciones = transacciones;
		this.mapperVenta = mapperVenta;
		this.urlPublica = urlPublica;
		this.vigenciaHoras = vigenciaHoras;
	}

	/** Si el proveedor no responde, el comprobante queda PENDIENTE y la venta se cierra igual (RN46). */
	@Transactional
	public Comprobante emitirComprobante(Long idComprobante) {
		Comprobante oComprobante = buscar(idComprobante);
		if (usuarioActual.buscarIdUsuario().isEmpty()) {
			sesionOracle.iniciar(oComprobante.getIdUsuarioVenta());
		}
		registrarEnvio(oComprobante);
		return repositorioComprobante.saveAndFlush(oComprobante);
	}

	/**
	 * [It. 2 · CUS23, RF33, RN12, RN46, RN47] La nota de crédito de la venta, con los datos de la solicitud aprobada: NC_BOLETA anula la BOLETA y
	 * NC_FACTURA la FACTURA (UQ_COMPROBANTE_VENTA_CLASE); serie (BC01 o FC01), número y comprador los pone Oracle al insertar (PKG_VENTA,
	 * TRG_COMPROBANTE_REGLAS). id_usuario_autoriza es la Administradora de la sesión; id_usuario_solicita sale de la solicitud, pero
	 * TRG_NOTA_CREDITO_REGLAS lo reemplaza por el usuario de PKG_SESION (D-21). Se envía a APISPERU como el comprobante de CUS19: si no responde
	 * queda PENDIENTE y la reenvía ReenvioCpeJob (RN46). SUNAT solo acepta la nota de un comprobante que ya aceptó (-20076): antes, 409.
	 */
	@Transactional
	public NotaCredito emitirNotaCredito(Long idVenta, SolicitudAnulacion oSolicitud) {
		Venta oVenta = repositorioVenta.findById(idVenta)
			.orElseThrow(() -> new ReglaNegocioException("RF33", "La venta " + idVenta + " no existe.", HttpStatus.NOT_FOUND));
		Comprobante oOriginal = comprobanteDeVenta(oVenta);
		if (oOriginal.getEstadoSunat() != EstadoSunat.ACEPTADO && oOriginal.getEstadoSunat() != EstadoSunat.OBSERVADO) {
			throw new ReglaNegocioException("RN46", "El comprobante " + oOriginal.getSerie() + "-" + oOriginal.getNumero() + " está " + oOriginal.getEstadoSunat()
					+ " en SUNAT: la nota de crédito se emite cuando SUNAT lo acepta.", HttpStatus.CONFLICT);
		}
		TipoComprobante tipo = oOriginal.getTipo() == TipoComprobante.BOLETA ? TipoComprobante.NC_BOLETA : TipoComprobante.NC_FACTURA;
		NotaCredito oNotaCredito = new NotaCredito(oVenta, tipo, oSolicitud.getMotivo(), oSolicitud.getCodigoMotivo(), oSolicitud.getFormaDevolucion(),
				oSolicitud.getIdUsuarioSolicita(), usuarioActual.idUsuario());
		repositorioNotaCredito.saveAndFlush(oNotaCredito);
		registrarEnvio(oNotaCredito);
		return repositorioNotaCredito.saveAndFlush(oNotaCredito);
	}

	/** Envía el comprobante o la nota, guarda el XML y el CDR y registra la respuesta de SUNAT (RN47). */
	private void registrarEnvio(Comprobante oComprobante) {
		RespuestaCpe oRespuestaCpe = enviar(oComprobante);
		String base = "comprobantes/" + oComprobante.getSerie() + "-" + oComprobante.getNumero();
		String rutaXml = oRespuestaCpe.xml() == null ? null : almacenArchivosPort.guardar(base + ".xml", oRespuestaCpe.xml());
		String rutaCdr = oRespuestaCpe.cdr() == null ? null : almacenArchivosPort.guardar(base + "-cdr.xml", oRespuestaCpe.cdr());
		oComprobante.registrarRespuestaSunat(oRespuestaCpe.estadoSunat(), oRespuestaCpe.codigoRespuesta(), oRespuestaCpe.descripcionRespuesta(),
				oRespuestaCpe.hashCpe(), rutaXml, rutaCdr);
	}

	/** La BOLETA o FACTURA de la venta, la que anula la nota de crédito (RN12). */
	private static Comprobante comprobanteDeVenta(Venta oVenta) {
		return oVenta.getComprobantes().stream()
			.filter(comprobante -> comprobante.getTipo() == TipoComprobante.BOLETA || comprobante.getTipo() == TipoComprobante.FACTURA)
			.findFirst()
			.orElseThrow(() -> new ReglaNegocioException("RN12", "La venta " + oVenta.getIdVenta() + " no tiene boleta ni factura que anular."));
	}

	/** Una transacción por comprobante: un fallo no deshace los demás. Devuelve cuántos se reenviaron (RF101, RN46). */
	public int reenviarPendientes() {
		List<FilaCpePendiente> pendientes = repositorioComprobante.findPendientesDeEnvio();
		int reenviados = 0;
		LocalDate hoy = LocalDate.now();
		for (FilaCpePendiente oPendiente : pendientes) {
			alertarSiVence(oPendiente, hoy);
			try {
				transacciones.executeWithoutResult(estado -> emitirComprobante(oPendiente.getIdDocumento()));
				reenviados++;
			}
			catch (RuntimeException ex) {
				log.error("No se pudo reenviar el comprobante {}-{}", oPendiente.getSerie(), oPendiente.getNumero(), ex);
			}
		}
		return reenviados;
	}

	/** [It. 2] CUS20: el comprobante con su estado SUNAT actual, también después del reenvío de ReenvioCpeJob (RN46, Duda 12 del front-end). */
	@Transactional(readOnly = true)
	public ComprobanteDTO obtenerComprobante(Long idComprobante) {
		return mapperVenta.aDTO(buscar(idComprobante));
	}

	@Transactional(readOnly = true)
	public PdfConQr generarRepresentacionImpresa(Long idComprobante) {
		return generadorPdfComprobante.generar(buscar(idComprobante));
	}

	@Transactional(readOnly = true)
	public String generarEnlacePublico(Long idComprobante) {
		Long id = buscar(idComprobante).getIdComprobante();
		long expira = Instant.now().plus(vigenciaHoras, ChronoUnit.HOURS).getEpochSecond();
		return FORMATO_ENLACE.formatted(urlPublica, id, expira, firmaEnlaceComprobante.firmar(id, expira));
	}

	/** Sin sesión: quien abre el enlace no tiene cuenta. Una firma mala o vencida responde 403. */
	@Transactional(readOnly = true)
	public PdfConQr obtenerRepresentacionPublica(Long idComprobante, long expira, String firma) {
		if (!firmaEnlaceComprobante.esValida(idComprobante, expira, firma)) {
			throw new ReglaNegocioException("RF102", "El enlace no es válido o ya venció.", HttpStatus.FORBIDDEN);
		}
		return generarRepresentacionImpresa(idComprobante);
	}

	private void alertarSiVence(FilaCpePendiente oPendiente, LocalDate hoy) {
		long diasRestantes = ChronoUnit.DAYS.between(hoy, oPendiente.getFechaLimite().toLocalDate());
		if (diasRestantes <= DIAS_DE_ALERTA) {
			log.warn("El comprobante {}-{} sigue PENDIENTE y le quedan {} día(s) del plazo de envío a SUNAT (RN46)", oPendiente.getSerie(), oPendiente.getNumero(),
					diasRestantes);
		}
	}

	private RespuestaCpe enviar(Comprobante oComprobante) {
		try {
			return facturacionElectronicaPort.emitir(aDocumento(oComprobante));
		}
		catch (RuntimeException ex) {
			log.warn("El comprobante {}-{} queda PENDIENTE: el proveedor no respondió", oComprobante.getSerie(), oComprobante.getNumero(), ex);
			return new RespuestaCpe(EstadoSunat.PENDIENTE, null, "El proveedor no respondió", null, null, null);
		}
	}

	/**
	 * [It. 2] Una nota de crédito lleva el comprador y las líneas del comprobante que anula (TRG_COMPROBANTE_REGLAS copia el comprador al insertar,
	 * pero la entidad recién guardada no lo vuelve a leer) y la referencia a ese comprobante con el motivo.
	 */
	private DocumentoElectronico aDocumento(Comprobante oComprobante) {
		BigDecimal igvPorcentaje = parametroApi.valorNumero("IGV_PORCENTAJE");
		// [It. 2.1 · D-33] Los nombres de los productos de todas las líneas, en una consulta
		Map<Long, String> productos = inventarioApi.obtenerProductos(oComprobante.getDetallesVenta().stream().map(VentaDetalle::getIdProducto)
			.filter(java.util.Objects::nonNull).toList()).stream().collect(java.util.stream.Collectors.toMap(DatosProducto::idProducto, DatosProducto::nombre));
		List<DocumentoElectronico.Linea> lineas = oComprobante.getDetallesVenta().stream().map(oDetalle -> aLinea(oDetalle, productos)).toList();
		Comprobante oComprador = oComprobante;
		DocumentoElectronico.NotaAfectada oNotaAfectada = null;
		if (oComprobante instanceof NotaCredito oNotaCredito) {
			oComprador = comprobanteDeVenta(repositorioVenta.findById(oNotaCredito.getIdVenta()).orElseThrow());
			oNotaAfectada = new DocumentoElectronico.NotaAfectada(oComprador.getTipo().name(), oComprador.getSerie(), oComprador.getNumero(),
					oNotaCredito.getCodigoMotivo(), oNotaCredito.getMotivo());
		}
		String codigoSunat = oComprador.getIdTipoDocComprador() == null ? null : personaApi.obtenerCodigoSunat(oComprador.getIdTipoDocComprador());
		return new DocumentoElectronico(oComprobante.getTipo().name(), oComprobante.getSerie(), oComprobante.getNumero(), oComprobante.getFechaEmision(), codigoSunat,
				oComprador.getNroDocComprador(), oComprador.getNombreComprador(), oComprobante.getImporteVenta(), igvPorcentaje, lineas, oNotaAfectada);
	}

	/** [It. 2.1 · D-33] Un producto va con su nombre (antes, «PRODUCTO»); el servicio, con el del servicio, como antes. */
	private DocumentoElectronico.Linea aLinea(VentaDetalle oDetalle, Map<Long, String> productos) {
		String descripcion;
		if (oDetalle.getIdProducto() != null && productos.containsKey(oDetalle.getIdProducto())) {
			descripcion = productos.get(oDetalle.getIdProducto());
		}
		else {
			descripcion = oDetalle.getIdAtencion() == null ? oDetalle.getTipoItem().name()
					: catalogoApi.obtenerVariante(atencionApi.obtenerIdVariante(oDetalle.getIdAtencion())).nombre();
		}
		return new DocumentoElectronico.Linea(descripcion, oDetalle.getCantidad(), oDetalle.getSubtotal());
	}

	private Comprobante buscar(Long idComprobante) {
		return repositorioComprobante.findById(idComprobante)
			.orElseThrow(() -> new ReglaNegocioException("RF102", "El comprobante " + idComprobante + " no existe.", HttpStatus.NOT_FOUND));
	}

}
