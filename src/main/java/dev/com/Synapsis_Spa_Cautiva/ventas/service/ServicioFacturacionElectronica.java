package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.AlmacenArchivosPort;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.SesionOracle;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.ComprobanteDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.MapperVenta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaDetalle;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.FilaCpePendiente;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioComprobante;
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

	private final String urlPublica;

	private final long vigenciaHoras;

	ServicioFacturacionElectronica(RepositorioComprobante repositorioComprobante, FacturacionElectronicaPort facturacionElectronicaPort,
			AlmacenArchivosPort almacenArchivosPort, GeneradorPdfComprobante generadorPdfComprobante, FirmaEnlaceComprobante firmaEnlaceComprobante,
			PersonaApi personaApi, AtencionApi atencionApi, CatalogoApi catalogoApi, ParametroApi parametroApi, SesionOracle sesionOracle,
			UsuarioActual usuarioActual, TransactionTemplate transacciones, MapperVenta mapperVenta, @Value("${cautiva.comprobantes.url-publica}") String urlPublica,
			@Value("${cautiva.comprobantes.enlace-vigencia-horas}") long vigenciaHoras) {
		this.repositorioComprobante = repositorioComprobante;
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
		RespuestaCpe oRespuestaCpe = enviar(oComprobante);
		String base = "comprobantes/" + oComprobante.getSerie() + "-" + oComprobante.getNumero();
		String rutaXml = oRespuestaCpe.xml() == null ? null : almacenArchivosPort.guardar(base + ".xml", oRespuestaCpe.xml());
		String rutaCdr = oRespuestaCpe.cdr() == null ? null : almacenArchivosPort.guardar(base + "-cdr.xml", oRespuestaCpe.cdr());
		oComprobante.registrarRespuestaSunat(oRespuestaCpe.estadoSunat(), oRespuestaCpe.codigoRespuesta(), oRespuestaCpe.descripcionRespuesta(),
				oRespuestaCpe.hashCpe(), rutaXml, rutaCdr);
		return repositorioComprobante.saveAndFlush(oComprobante);
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

	private DocumentoElectronico aDocumento(Comprobante oComprobante) {
		BigDecimal igvPorcentaje = parametroApi.valorNumero("IGV_PORCENTAJE");
		String codigoSunat = oComprobante.getIdTipoDocComprador() == null ? null : personaApi.obtenerCodigoSunat(oComprobante.getIdTipoDocComprador());
		List<DocumentoElectronico.Linea> lineas = oComprobante.getDetallesVenta().stream().map(this::aLinea).toList();
		return new DocumentoElectronico(oComprobante.getTipo().name(), oComprobante.getSerie(), oComprobante.getNumero(), oComprobante.getFechaEmision(), codigoSunat,
				oComprobante.getNroDocComprador(), oComprobante.getNombreComprador(), oComprobante.getImporteVenta(), igvPorcentaje, lineas);
	}

	private DocumentoElectronico.Linea aLinea(VentaDetalle oDetalle) {
		String descripcion = oDetalle.getIdAtencion() == null ? oDetalle.getTipoItem().name()
				: catalogoApi.obtenerVariante(atencionApi.obtenerIdVariante(oDetalle.getIdAtencion())).nombre();
		return new DocumentoElectronico.Linea(descripcion, oDetalle.getCantidad(), oDetalle.getSubtotal());
	}

	private Comprobante buscar(Long idComprobante) {
		return repositorioComprobante.findById(idComprobante)
			.orElseThrow(() -> new ReglaNegocioException("RF102", "El comprobante " + idComprobante + " no existe.", HttpStatus.NOT_FOUND));
	}

}
